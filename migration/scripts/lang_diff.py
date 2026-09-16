#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""M3.5 语言键对拍脚本（Python 3 标准库，无第三方依赖）。

用法:
  python3 lang_diff.py [--baseline PATH] [--assets-dir PATH]
      --baseline PATH   BuildCraft-Localization 基线 en_us.json（JSONC：允许 // 与 /* */ 注释）。
                        默认 <仓库根>/BuildCraft-Localization/assets/buildcraft/lang/en_us.json（子模块只读）。
      --assets-dir PATH neo 侧资产根目录。默认 <仓库根>/neo/src/main/resources/assets。
                        脚本扫描 <assets-dir>/*/lang/en_us.json 全部 modid 并取键集合并集。

判定（任务 M3.5 验收 "BuildCraft-Localization 语言键集合与基线 diff=0"）:
  - 基线键集合 = 子模块 en_us.json 的全部键（子模块是单一 "buildcraft" 命名空间的扁平 KV，
    未按 mod 预分文件，故 neo 侧取 8 个 modid lang 文件的键集合并集与之对拍；键名原样保留，
    含 tile.xxx/item.xxx/gui.xxx 等老式前缀——运行时 lang 是全局平面 KV，跨命名空间查找，老键可照常引用）。
  - missing（基线有、neo 无）白名单外必须为 0；extra（neo 有、基线无）白名单外必须为 0。
  - 只对拍键集合，不对拍文案内容（占位文案可接受；英文文案已机械复用子模块原文）。
  - 白名单机制：个别确有依据的键可列入下方 MISSING_WHITELIST / EXTRA_WHITELIST 并注明理由，
    目标是白名单保持为空。改动白名单必须同步本文件 docstring 与 migration/tasks.json 的 notes。

格式依据: 子模块 assets/buildcraft/lang/*.json 为 1.13+ JSON lang（小写 locale 文件名 en_us.json），
但文件含 // 注释行（JSONC，文件头自称 "Master language file"），解析时按状态机剥离字符串字面量
之外的 // 与 /* */ 注释；剥离后必须是严格 JSON（尾逗号等按工具错误处理，fail-closed）。
json.loads 对重复键静默保留末值，故用 object_pairs_hook 检测重复键并按工具错误拒绝。

退出码: 0 通过（白名单外 diff=0）；1 有 missing/extra（fail）；2 对拍工具自身错误
（文件缺失/JSON 非法/重复键/找不到任何 lang 文件，fail-closed）。
"""

import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_BASELINE = REPO_ROOT / "BuildCraft-Localization" / "assets" / "buildcraft" / "lang" / "en_us.json"
DEFAULT_ASSETS_DIR = REPO_ROOT / "neo" / "src" / "main" / "resources" / "assets"

# 白名单（当前为空，理想目标：保持为空）。
# MISSING_WHITELIST: 基线有、neo 侧确实不落地的键（逐字键名，注明理由）。
# EXTRA_WHITELIST:   neo 侧有、基线没有的键（逐字键名，注明理由；如未来注册内容产生
#                    block.<ns>.<path> 现代自动键而基线只有老式键时，经主 agent 裁决后在此登记）。
MISSING_WHITELIST = frozenset()
EXTRA_WHITELIST = frozenset()

USAGE = "用法: python3 lang_diff.py [--baseline PATH] [--assets-dir PATH]"


def strip_jsonc_comments(text):
    """按状态机剥离 JSONC 注释（// 与 /* */）；字符串字面量与转义序列内的注释符原样保留。"""
    out = []
    i, n = 0, len(text)
    in_string = False
    while i < n:
        ch = text[i]
        if in_string:
            out.append(ch)
            if ch == "\\" and i + 1 < n:
                out.append(text[i + 1])
                i += 2
                continue
            if ch == '"':
                in_string = False
            i += 1
            continue
        if ch == '"':
            in_string = True
            out.append(ch)
            i += 1
        elif ch == "/" and text.startswith("//", i):
            while i < n and text[i] != "\n":
                i += 1
            continue
        elif ch == "/" and text.startswith("/*", i):
            i = text.find("*/", i + 2)
            if i < 0:
                raise ValueError("unterminated /* comment")
            i += 2
            continue
        else:
            out.append(ch)
            i += 1
    return "".join(out)


def load_lang_keys(path, label):
    """读取单个 lang 文件，返回键集合；任何结构问题按工具错误（exit 2）处理。"""
    try:
        text = path.read_text(encoding="utf-8")
    except OSError as exc:
        print(f"[错误] 无法读取 {label} {path}: {exc}", file=sys.stderr)
        sys.exit(2)
    try:
        clean = strip_jsonc_comments(text)
        data = json.loads(clean)
    except (json.JSONDecodeError, ValueError) as exc:
        print(f"[错误] {label} {path} 剥离注释后不是合法 JSON: {exc}", file=sys.stderr)
        sys.exit(2)
    if not isinstance(data, dict):
        print(f"[错误] {label} {path} 顶层必须是 JSON object", file=sys.stderr)
        sys.exit(2)
    seen = []

    def collect(pairs):
        seen.extend(k for k, _ in pairs)
        return dict(pairs)

    json.loads(clean, object_pairs_hook=collect)
    counts = {}
    for key in seen:
        counts[key] = counts.get(key, 0) + 1
    dupes = sorted(k for k, c in counts.items() if c > 1)
    if dupes:
        print(f"[错误] {label} {path} 存在重复键（json 会静默吞键，拒绝处理）: {', '.join(dupes)}", file=sys.stderr)
        sys.exit(2)
    return set(data)


def main(argv):
    baseline_path = DEFAULT_BASELINE
    assets_dir = DEFAULT_ASSETS_DIR
    rest = list(argv)
    while rest:
        flag = rest.pop(0)
        if flag in ("--baseline", "--assets-dir") and rest:
            value = Path(rest.pop(0))
            if flag == "--baseline":
                baseline_path = value
            else:
                assets_dir = value
        else:
            print(f"未知或不完整参数: {flag}\n{USAGE}", file=sys.stderr)
            return 2

    if not baseline_path.is_file():
        print(f"[错误] 基线文件不存在: {baseline_path}", file=sys.stderr)
        return 2
    if not assets_dir.is_dir():
        print(f"[错误] neo 资产目录不存在: {assets_dir}", file=sys.stderr)
        return 2

    lang_files = sorted(assets_dir.glob("*/lang/en_us.json"))
    if not lang_files:
        print(f"[错误] {assets_dir} 下找不到任何 */lang/en_us.json（fail-closed）", file=sys.stderr)
        return 2

    print("=== M3.5 语言键对拍 ===")
    print(f"baseline: {baseline_path}")
    print(f"assets:   {assets_dir}")
    print("")

    base_keys = load_lang_keys(baseline_path, "baseline")

    union = set()
    print("-- neo 侧各 modid lang/en_us.json 键数 --")
    for path in lang_files:
        keys = load_lang_keys(path, "neo")
        modid = path.relative_to(assets_dir).parts[0]
        print(f"  {modid:20s} {len(keys):5d}")
        union |= keys

    missing_all = sorted(base_keys - union)
    extra_all = sorted(union - base_keys)
    missing = [k for k in missing_all if k not in MISSING_WHITELIST]
    extra = [k for k in extra_all if k not in EXTRA_WHITELIST]
    miss_wl = [k for k in missing_all if k in MISSING_WHITELIST]
    extra_wl = [k for k in extra_all if k in EXTRA_WHITELIST]
    ok = not missing and not extra

    print("")
    print(f"基线键集合: {len(base_keys)} ｜ neo 并集: {len(union)} ｜ missing(白名单外) {len(missing)}"
          f" ｜ extra(白名单外) {len(extra)} ｜ 白名单放行 missing {len(miss_wl)} / extra {len(extra_wl)}")
    if missing:
        print(f"missing 白名单外 ({len(missing)}):")
        for key in missing:
            print(f"  - {key}")
    if extra:
        print(f"extra 白名单外 ({len(extra)}):")
        for key in extra:
            print(f"  + {key}")
    if miss_wl:
        print(f"missing 白名单放行 ({len(miss_wl)}): " + ", ".join(miss_wl))
    if extra_wl:
        print(f"extra 白名单放行 ({len(extra_wl)}): " + ", ".join(extra_wl))

    print("")
    if ok:
        print("RESULT: PASS — 语言键集合白名单外 diff=0（只对拍键集合，不比文案）")
        return 0
    print(f"RESULT: FAIL — missing {len(missing)} / extra {len(extra)}（白名单外）")
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
