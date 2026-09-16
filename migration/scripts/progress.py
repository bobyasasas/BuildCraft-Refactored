#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""BuildCraft 迁移进度追踪脚本（Python 3 标准库，无第三方依赖）。

用法:
  python3 progress.py
      读取 migration/tasks.json，重新生成 migration/PROGRESS.md，并打印一行摘要。
  python3 progress.py --check
      校验 migration/tasks.json（不写任何文件）：JSON 可解析、顶层字段存在、
      status 全部在枚举内、id 唯一且符合 M<数字>.<数字> 格式、done 任务 evidence 非空。
      全部通过 exit 0；任何失败打印原因并 exit 1。
  python3 progress.py --set <ID> <STATUS> [EVIDENCE...]
      更新指定任务的 status / evidence（若给出）与 updated 日期，然后重新生成 PROGRESS.md。
  python3 progress.py --trend
      采集一条趋势记录（时间戳、git commit、任务完成度、Forge import 等代码指标），
      追加到 migration/trends/metrics-history.jsonl 并刷新 metrics-latest.json，打印记录内容。
      不修改 tasks.json 与 PROGRESS.md。供 CI 定期执行形成趋势（M3.8）。
"""

import json
import re
import subprocess
import sys
from datetime import datetime
from pathlib import Path

MIGRATION_DIR = Path(__file__).resolve().parents[1]
REPO_ROOT = MIGRATION_DIR.parent
TASKS_FILE = MIGRATION_DIR / "tasks.json"
PROGRESS_FILE = MIGRATION_DIR / "PROGRESS.md"
TREND_DIR = MIGRATION_DIR / "trends"
TREND_HISTORY_FILE = TREND_DIR / "metrics-history.jsonl"
TREND_LATEST_FILE = TREND_DIR / "metrics-latest.json"

VALID_STATUS = ("pending", "in_progress", "partial", "unverified", "done")
STATUS_LABEL = {
    "pending": "未完成",
    "in_progress": "进行中",
    "partial": "部分完成",
    "unverified": "未验证",
    "done": "已完成",
}
STATUS_WEIGHT = {
    "done": 1.0,
    "in_progress": 0.5,
    "partial": 0.5,
    "unverified": 0.5,
    "pending": 0.0,
}
ID_RE = re.compile(r"^M\d+\.\d+$")
TOP_KEYS = ("schema", "updated", "target", "baseline_2026_09", "tasks")
TASK_KEYS = ("id", "phase", "title", "status", "acceptance", "evidence", "notes")

EXCLUDE_DIRS = (".git", ".gradle", "build", "buildcraft_resources_generated")
CMD_TIMEOUT = 120  # 单条子进程超时（秒）

USAGE = (
    "用法:\n"
    "  python3 progress.py                          # 生成 PROGRESS.md 并打印摘要\n"
    "  python3 progress.py --check                  # 校验 tasks.json（不写文件）\n"
    "  python3 progress.py --set <ID> <STATUS> [EVIDENCE...]  # 更新任务并重新生成\n"
    "  python3 progress.py --trend                  # 追加一条趋势记录（M3.8）\n"
    f"  STATUS 枚举: {', '.join(VALID_STATUS)}"
)


# ---------------------------------------------------------------- tasks.json

def load_tasks():
    """读取 tasks.json，返回 (data, error_message)。"""
    if not TASKS_FILE.is_file():
        return None, f"文件不存在: {TASKS_FILE}"
    try:
        text = TASKS_FILE.read_text(encoding="utf-8")
    except OSError as exc:
        return None, f"无法读取 {TASKS_FILE}: {exc}"
    try:
        return json.loads(text), None
    except json.JSONDecodeError as exc:
        return None, f"{TASKS_FILE} 不是合法 JSON: {exc}"


def validate_tasks(data):
    """校验 tasks.json 数据结构，返回错误列表（空列表 = 通过）。"""
    errors = []
    if not isinstance(data, dict):
        return [f"顶层必须是 JSON object，实际为 {type(data).__name__}"]
    for key in TOP_KEYS:
        if key not in data:
            errors.append(f"缺少顶层字段 {key}")
    tasks = data.get("tasks")
    if not isinstance(tasks, list) or not tasks:
        errors.append("tasks 必须是非空数组")
        return errors
    seen_ids = set()
    for index, task in enumerate(tasks):
        where = f"tasks[{index}]"
        if not isinstance(task, dict):
            errors.append(f"{where} 不是 object")
            continue
        tid = task.get("id")
        if not isinstance(tid, str) or not ID_RE.match(tid):
            errors.append(f"{where} 的 id 非法: {tid!r}（需匹配 M<数字>.<数字>）")
        elif tid in seen_ids:
            errors.append(f"id 重复: {tid}")
        else:
            seen_ids.add(tid)
            where = tid
        status = task.get("status")
        if status not in VALID_STATUS:
            errors.append(
                f"{where} 的 status 非法: {status!r}（枚举: {', '.join(VALID_STATUS)}）"
            )
        for key in TASK_KEYS:
            if key not in task:
                errors.append(f"{where} 缺少字段 {key}")
        if status == "done" and not str(task.get("evidence") or "").strip():
            errors.append(f"{where} status=done 但 evidence 为空")
    return errors


def summarize(tasks):
    """返回 (各状态计数, 总数, 完成率%)。权重: done=1, in_progress/partial/unverified=0.5, pending=0。"""
    counts = {status: 0 for status in VALID_STATUS}
    for task in tasks:
        status = task.get("status")
        counts[status] = counts.get(status, 0) + 1
    total = len(tasks)
    score = sum(STATUS_WEIGHT.get(task.get("status"), 0.0) for task in tasks)
    rate = int(round(100.0 * score / total)) if total else 0
    return counts, total, rate


def group_by_phase(tasks):
    """按 phase 字段分组（保持首次出现顺序），返回 (阶段名列表, 阶段名->任务列表)。"""
    names = []
    mapping = {}
    for task in tasks:
        name = str(task.get("phase") or "(未分阶段)")
        if name not in mapping:
            mapping[name] = []
            names.append(name)
        mapping[name].append(task)
    return names, mapping


# ------------------------------------------------------- 代码实时指标采集

def _find_argv(print0=False):
    """find 命令：列出仓库内全部 .java 文件（排除 EXCLUDE_DIRS 目录）。"""
    argv = ["find", "."]
    if EXCLUDE_DIRS:
        argv += ["(", "-type", "d", "("]
        for i, name in enumerate(EXCLUDE_DIRS):
            if i:
                argv.append("-o")
            argv += ["-name", name]
        argv += [")", "-prune", ")", "-o"]
    argv += ["-type", "f", "-name", "*.java", "-print0" if print0 else "-print"]
    return argv


def _grep_argv(patterns, list_files=False, fixed=False):
    """grep 命令：在 .java 文件里统计（排除 EXCLUDE_DIRS 目录）。"""
    if isinstance(patterns, str):
        patterns = [patterns]
    argv = ["grep", "-r", "--include=*.java"]
    argv += [f"--exclude-dir={name}" for name in EXCLUDE_DIRS]
    argv.append("-l" if list_files else "-h")
    if fixed:
        argv.append("-F")
    for pattern in patterns:
        argv += ["-e", pattern]
    argv.append(".")
    return argv


def _pipe(argv_lists):
    """把多段命令串成管道执行，返回最后一段命令的 stdout（bytes）。"""
    procs = []
    prev_stdout = None
    try:
        for i, argv in enumerate(argv_lists):
            proc = subprocess.Popen(
                argv,
                cwd=REPO_ROOT,
                stdin=prev_stdout if i > 0 else subprocess.DEVNULL,
                stdout=subprocess.PIPE,
                stderr=subprocess.DEVNULL,
            )
            procs.append(proc)
            if prev_stdout is not None:
                prev_stdout.close()
            prev_stdout = proc.stdout
        out, _ = procs[-1].communicate(timeout=CMD_TIMEOUT)
        return out or b""
    finally:
        for proc in procs:
            if proc.poll() is None:
                proc.kill()


def _wc(out_bytes):
    text = out_bytes.decode("utf-8", "replace").strip()
    return int(text.split()[0]) if text else 0


def collect_metrics():
    """实时采集代码指标；任何一项失败记为 None（渲染时显示 N/A），绝不让脚本崩溃。"""
    metrics = {}

    def safe(key, fn):
        try:
            metrics[key] = fn()
        except Exception:
            metrics[key] = None

    # a) .java 文件总数与总行数
    safe("java_files", lambda: _wc(_pipe([_find_argv(), ["wc", "-l"]])))
    safe(
        "java_lines",
        lambda: _wc(_pipe([_find_argv(print0=True), ["xargs", "-0", "cat"], ["wc", "-l"]])),
    )
    # b) import net.minecraftforge 的文件数与出现次数
    safe(
        "forge_import_files",
        lambda: _wc(_pipe([_grep_argv("import net.minecraftforge", list_files=True), ["wc", "-l"]])),
    )
    safe(
        "forge_import_occurrences",
        lambda: _wc(_pipe([_grep_argv("import net.minecraftforge"), ["wc", "-l"]])),
    )
    # c) TODO / FIXME 出现次数
    safe("todo", lambda: _wc(_pipe([_grep_argv("TODO"), ["wc", "-l"]])))
    safe("fixme", lambda: _wc(_pipe([_grep_argv("FIXME"), ["wc", "-l"]])))
    # d) @OnlyIn(Dist.CLIENT) 出现次数
    safe("onlyin_client", lambda: _wc(_pipe([_grep_argv("@OnlyIn(Dist.CLIENT)", fixed=True), ["wc", "-l"]])))
    # e) RegistryObject 涉及的文件数
    safe(
        "registryobject_files",
        lambda: _wc(_pipe([_grep_argv("RegistryObject", list_files=True, fixed=True), ["wc", "-l"]])),
    )
    return metrics


# ------------------------------------------------------------ PROGRESS.md

def fmt(value):
    if value is None:
        return "N/A"
    if isinstance(value, int):
        return f"{value:,}"
    return str(value)


def esc(text):
    """转义 Markdown 表格单元格内容。"""
    return str(text).replace("|", "\\|").replace("\r", " ").replace("\n", " ").strip()


def render_progress(data, now_str):
    tasks = data["tasks"]
    counts, total, rate = summarize(tasks)
    baseline = data.get("baseline_2026_09", {}) or {}

    lines = []
    add = lines.append

    add("# BuildCraft 迁移进度仪表盘")
    add("")
    add("> **本文件由 migration/scripts/progress.py 自动生成，禁止手改；更新任务请编辑 migration/tasks.json 或用 `--set` 命令。**")
    add(">")
    add(f"> 生成时间：{now_str} ｜ 数据源：migration/tasks.json（schema={data.get('schema')}，updated={data.get('updated')}）")
    add("")
    add("## 总览")
    add("")
    status_part = "、".join(f"{s}({STATUS_LABEL[s]})={counts[s]}" for s in VALID_STATUS)
    add(
        f"**总任务数 {total}** ｜ {status_part} ｜ **完成率 {rate}%**"
        "（权重：done=1，in_progress/partial/unverified=0.5，pending=0）"
    )
    add("")

    phase_names, phase_map = group_by_phase(tasks)
    add("## 阶段汇总")
    add("")
    add("| 阶段 | 任务数 | done | 完成率 |")
    add("|---|---:|---:|---:|")
    for name in phase_names:
        p_counts, p_total, p_rate = summarize(phase_map[name])
        add(f"| {esc(name)} | {p_total} | {p_counts['done']} | {p_rate}% |")
    add("")

    add("## 任务明细")
    add("")
    add("| ID | 任务 | 状态 | 验收标准 | evidence / notes |")
    add("|---|---|---|---|---|")
    for task in tasks:
        status = task["status"]
        extra = []
        if str(task.get("evidence") or "").strip():
            extra.append(esc(task["evidence"]))
        if str(task.get("notes") or "").strip():
            extra.append("notes: " + esc(task["notes"]))
        extra_cell = "<br>".join(extra) if extra else "—"
        add(
            f"| {esc(task['id'])} | {esc(task['title'])} "
            f"| {status}（{STATUS_LABEL[status]}） | {esc(task['acceptance'])} | {extra_cell} |"
        )
    add("")

    metrics = collect_metrics()
    add("## 代码实时指标")
    add("")
    add(
        f"采集时间：{now_str}；采集范围：仓库根目录（排除 .git、.gradle、build、buildcraft_resources_generated）。"
    )
    add("")
    add("| 指标 | 当前值 | 调研基线(2026-09) | 目标 |")
    add("|---|---:|---:|---|")
    java_base = "—"
    if isinstance(baseline.get("java_files_main"), int) and isinstance(
        baseline.get("java_files_api_submodule"), int
    ):
        java_base = (
            f"{baseline['java_files_main'] + baseline['java_files_api_submodule']}"
            f"（main {baseline['java_files_main']} + API 子模块 {baseline['java_files_api_submodule']}）"
        )
    rows = [
        (".java 文件总数", fmt(metrics.get("java_files")), java_base, "无目标(参考)"),
        (".java 总行数", fmt(metrics.get("java_lines")), "—", "无目标(参考)"),
        ("Forge import 文件数", fmt(metrics.get("forge_import_files")), fmt(baseline.get("forge_import_files")), "0"),
        ("Forge import 出现次数", fmt(metrics.get("forge_import_occurrences")), "—", "0"),
        ("TODO 出现次数", fmt(metrics.get("todo")), fmt(baseline.get("todo")), "随 M1.4 下降"),
        ("FIXME 出现次数", fmt(metrics.get("fixme")), fmt(baseline.get("fixme")), "随 M1.4 下降"),
        ("@OnlyIn(Dist.CLIENT) 出现次数", fmt(metrics.get("onlyin_client")), fmt(baseline.get("onlyin_client")), "随 M1.2 下降"),
        ("RegistryObject 涉及文件数", fmt(metrics.get("registryobject_files")), fmt(baseline.get("registryobject_files")), "0"),
    ]
    for name, current, base, goal in rows:
        add(f"| {name} | {current} | {base} | {goal} |")
    add("")

    add("## 维护方式")
    add("")
    add("- 更新任务状态：`python3 migration/scripts/progress.py --set M0.2 in_progress \"证据文本\"`")
    add("- 校验数据：`python3 migration/scripts/progress.py --check`")
    add("- 重新生成本文件：`python3 migration/scripts/progress.py`")
    add("")
    return "\n".join(lines)


def regenerate(data):
    """生成 PROGRESS.md 并打印一行摘要，返回退出码。"""
    errors = validate_tasks(data)
    if errors:
        for err in errors:
            print(f"[错误] {err}", file=sys.stderr)
        print(f"[错误] tasks.json 校验失败（{len(errors)} 处），未生成 PROGRESS.md", file=sys.stderr)
        return 1
    now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    PROGRESS_FILE.write_text(render_progress(data, now_str), encoding="utf-8")
    counts, total, rate = summarize(data["tasks"])
    status_part = "、".join(f"{STATUS_LABEL[s]}={counts[s]}" for s in VALID_STATUS)
    print(f"已生成 migration/PROGRESS.md：总任务 {total}，{status_part}，完成率 {rate}%")
    return 0


# ---------------------------------------------------------------- 子命令

def cmd_check():
    data, err = load_tasks()
    if err:
        print(f"[错误] {err}", file=sys.stderr)
        return 1
    errors = validate_tasks(data)
    if errors:
        for e in errors:
            print(f"[错误] {e}", file=sys.stderr)
        print(f"tasks.json 校验失败：共 {len(errors)} 处问题", file=sys.stderr)
        return 1
    total = len(data["tasks"])
    done = sum(1 for t in data["tasks"] if t["status"] == "done")
    print(f"tasks.json 校验通过：{total} 个任务（done={done}），id 唯一且格式合法，status 全部在枚举内，done 任务 evidence 非空")
    return 0


def cmd_set(tid, status, evidence):
    if not ID_RE.match(tid):
        print(f"[错误] id 格式非法: {tid!r}（需匹配 M<数字>.<数字>，如 M0.2）", file=sys.stderr)
        return 1
    if status not in VALID_STATUS:
        print(f"[错误] status 非法: {status!r}（枚举: {', '.join(VALID_STATUS)}）", file=sys.stderr)
        return 1
    data, err = load_tasks()
    if err:
        print(f"[错误] {err}", file=sys.stderr)
        return 1
    task = next((t for t in data.get("tasks", []) if t.get("id") == tid), None)
    if task is None:
        print(f"[错误] 任务 id 不存在: {tid}（tasks.json 中没有该任务，未做任何修改）", file=sys.stderr)
        return 1
    old_status = task.get("status")
    task["status"] = status
    evidence_updated = False
    if evidence is not None:
        task["evidence"] = evidence
        evidence_updated = True
    data["updated"] = datetime.now().strftime("%Y-%m-%d")
    errors = validate_tasks(data)
    if errors:
        for e in errors:
            print(f"[错误] {e}", file=sys.stderr)
        print("[错误] 更新后的 tasks.json 无法通过校验，未写入任何文件", file=sys.stderr)
        return 1
    TASKS_FILE.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    changed = [f"status: {old_status} -> {status}"]
    if evidence_updated:
        changed.append("evidence 已更新")
    print(f"已更新 {tid}（{', '.join(changed)}）")
    return regenerate(data)


def cmd_trend():
    """M3.8 趋势记录：采集一条指标快照追加进 JSONL 历史，并刷新 latest 快照。"""
    data, err = load_tasks()
    if err:
        print(f"[错误] {err}", file=sys.stderr)
        return 1
    counts, total, rate = summarize(data["tasks"])
    record = {
        "timestamp": datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
        "commit": None,
        "total": total,
        "done": counts["done"],
        "in_progress": counts["in_progress"],
        "partial": counts["partial"],
        "unverified": counts["unverified"],
        "pending": counts["pending"],
        "rate_pct": rate,
    }
    try:
        commit = subprocess.run(
            ["git", "rev-parse", "--short", "HEAD"],
            cwd=REPO_ROOT, capture_output=True, text=True, timeout=CMD_TIMEOUT,
        )
        if commit.returncode == 0:
            record["commit"] = commit.stdout.strip() or None
    except Exception:
        pass
    record.update(collect_metrics())
    TREND_DIR.mkdir(parents=True, exist_ok=True)
    with TREND_HISTORY_FILE.open("a", encoding="utf-8") as fh:
        fh.write(json.dumps(record, ensure_ascii=False, sort_keys=True) + "\n")
    TREND_LATEST_FILE.write_text(
        json.dumps(record, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    print("趋势记录已追加: " + json.dumps(record, ensure_ascii=False, sort_keys=True))
    print(f"历史文件: {TREND_HISTORY_FILE}（latest 快照: {TREND_LATEST_FILE}）")
    return 0


def main(argv):
    if not argv:
        data, err = load_tasks()
        if err:
            print(f"[错误] {err}", file=sys.stderr)
            return 1
        return regenerate(data)
    if argv[0] == "--check":
        if len(argv) != 1:
            print("用法: progress.py --check", file=sys.stderr)
            return 2
        return cmd_check()
    if argv[0] == "--set":
        if len(argv) < 3:
            print(USAGE, file=sys.stderr)
            return 2
        evidence = " ".join(argv[3:]) if len(argv) > 3 else None
        return cmd_set(argv[1], argv[2], evidence)
    if argv[0] == "--trend":
        if len(argv) != 1:
            print("用法: progress.py --trend", file=sys.stderr)
            return 2
        return cmd_trend()
    print(f"未知参数: {' '.join(argv)}\n{USAGE}", file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
