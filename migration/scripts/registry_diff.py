#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""M3.3 注册表对拍脚本（Python 3 标准库，无第三方依赖）。

用法:
  python3 registry_diff.py <dump.json> <baseline.json> [--strict]
      dump.json     26.1.2 运行时 dump（RegistryDumpProbe 生成，与基线同 schema）
      baseline.json 冻结基线 migration/snapshots/registry-baseline.json（只读）
      --strict      recipes/tags 分节也按 fail 处理（默认 quantify-only：只量化计数，不算 fail；
                    内容移植完成后启用 --strict 收紧为全量对拍）

判定（与 gametest buildcraftcore:registry_parity / RegistryParityTest 的语义一致）:
  - 五类注册表 blocks/items/block_entities/entities/fluids：
      missing（基线有、当前无）必须为 0；
      extra 必须为 0 —— 先按 RegistryParityTest.EXTRA_WHITELIST 的 path 白名单扣除
      （4 个 M2.2 垂直切片 id：marker / engine_stone / pipe_kinesis_wood / energy_meter，
      按 path 匹配、跨 8 个 buildcraft* 命名空间与五类注册表共用，与该类逐字一致），
      扣除后仍有 extra 即 fail。
  - recipes / tags.*：默认仅输出精确计数（missing/extra 清单 + 计数），不影响退出码；
    --strict 时同五类口径参与判定。
退出码: 0 通过；1 有 fail；2 对拍工具自身错误（文件缺失/JSON 非法/分节缺失，fail-closed）。
"""

import json
import sys

FIVE_SECTIONS = ("blocks", "items", "block_entities", "entities", "fluids")
TAG_SECTIONS = ("tags.biome", "tags.block", "tags.fluid", "tags.item")
QUANTIFY_SECTIONS = ("recipes",) + TAG_SECTIONS

# 白名单出处：neo/src/main/java/buildcraft/core/gametest/RegistryParityTest.java 的
# EXTRA_WHITELIST（M3.3 要求与该 gametest 完全一致，改动必须两处同步）。
# 语义照抄该类 checkRegistry：按 id 的 path 匹配（不含命名空间），五类注册表共用。
EXTRA_WHITELIST = frozenset({"marker", "engine_stone", "pipe_kinesis_wood", "energy_meter"})

USAGE = "用法: python3 registry_diff.py <dump.json> <baseline.json> [--strict]"


def load_snapshot(path, label):
    """读取快照 JSON 并校验全部分节存在；任何结构问题按工具错误（exit 2）处理。"""
    try:
        with open(path, encoding="utf-8") as fh:
            data = json.load(fh)
    except OSError as exc:
        print(f"[错误] 无法读取 {label} {path}: {exc}", file=sys.stderr)
        sys.exit(2)
    except json.JSONDecodeError as exc:
        print(f"[错误] {label} {path} 不是合法 JSON: {exc}", file=sys.stderr)
        sys.exit(2)
    if not isinstance(data, dict):
        print(f"[错误] {label} {path} 顶层必须是 JSON object", file=sys.stderr)
        sys.exit(2)
    required = list(FIVE_SECTIONS) + ["recipes", "tags"]
    for section in required:
        if section not in data:
            print(f"[错误] {label} {path} 缺少分节 {section}（fail-closed）", file=sys.stderr)
            sys.exit(2)
    tags = data["tags"]
    if not isinstance(tags, dict) or any(k not in tags for k in ("biome", "block", "fluid", "item")):
        print(f"[错误] {label} {path} 的 tags 分节缺少 biome/block/fluid/item 之一", file=sys.stderr)
        sys.exit(2)
    return data


def id_path(entry_id):
    return entry_id.split(":", 1)[1] if ":" in entry_id else entry_id


def as_id_list(value, section, label, path):
    if not isinstance(value, list) or any(not isinstance(x, str) for x in value):
        print(f"[错误] {label} {path} 的分节 {section} 必须是字符串 id 列表", file=sys.stderr)
        sys.exit(2)
    return value


def compare(section, baseline_ids, current_ids, fatal):
    """返回 (报告行列表, 是否 fail)。白名单只作用于 extra，missing 无白名单（同 RegistryParityTest）。"""
    base, cur = set(baseline_ids), set(current_ids)
    missing = sorted(base - cur)
    extra_all = sorted(cur - base)
    allowed = [x for x in extra_all if id_path(x) in EXTRA_WHITELIST]
    extra = [x for x in extra_all if id_path(x) not in EXTRA_WHITELIST]
    ok = not missing and not extra

    verdict = "OK" if ok else ("FAIL" if fatal else "GAP(quantify-only)")
    lines = [
        f"{section}: baseline {len(base)}, current {len(cur)}, missing {len(missing)},"
        f" extra(白名单外) {len(extra)}, extra(白名单放行) {len(allowed)}  -> {verdict}"
    ]
    if missing:
        lines.append(f"    missing ({len(missing)}): " + ", ".join(missing))
    if extra:
        lines.append(f"    extra 白名单外 ({len(extra)}): " + ", ".join(extra))
    if allowed:
        lines.append(f"    extra 白名单放行 ({len(allowed)}): " + ", ".join(allowed))
    return lines, ok


def main(argv):
    paths = [a for a in argv if not a.startswith("--")]
    strict = "--strict" in argv
    unknown = [a for a in argv if a.startswith("--") and a != "--strict"]
    if len(paths) != 2 or unknown:
        if unknown:
            print(f"未知参数: {' '.join(unknown)}", file=sys.stderr)
        print(USAGE + "  [--strict]", file=sys.stderr)
        return 2
    dump_path, baseline_path = paths

    dump = load_snapshot(dump_path, "dump")
    baseline = load_snapshot(baseline_path, "baseline")

    print("=== M3.3 注册表对拍 ===")
    dump_meta = dump.get("metadata", {})
    base_meta = baseline.get("metadata", {})
    print(f"dump:     {dump_path} (generator {dump_meta.get('generator', '?')},"
          f" minecraft_version {dump_meta.get('minecraft_version', '?')})")
    print(f"baseline: {baseline_path} (generator {base_meta.get('generator', '?')},"
          f" minecraft_version {base_meta.get('minecraft_version', '?')})")
    print(f"模式: {'--strict（全部分节 fail-closed）' if strict else 'lenient（recipes/tags 仅量化计数，不算 fail）'}")
    print("")

    failed = []
    gap_counts = []

    print("-- 五类注册表（missing=0 且白名单外 extra=0 才通过）--")
    for section in FIVE_SECTIONS:
        lines, ok = compare(section,
                            as_id_list(baseline[section], section, "baseline", baseline_path),
                            as_id_list(dump[section], section, "dump", dump_path),
                            fatal=True)
        print("\n".join(lines))
        if not ok:
            failed.append(section)

    print("")
    print("-- recipes / tags（默认 quantify-only：缺口只记数；--strict 时同口径判定）--")
    for section in QUANTIFY_SECTIONS:
        if section.startswith("tags."):
            baseline_ids = as_id_list(baseline["tags"][section.split(".", 1)[1]], section, "baseline", baseline_path)
            dump_ids = as_id_list(dump["tags"][section.split(".", 1)[1]], section, "dump", dump_path)
        else:
            baseline_ids = as_id_list(baseline[section], section, "baseline", baseline_path)
            dump_ids = as_id_list(dump[section], section, "dump", dump_path)
        lines, ok = compare(section, baseline_ids, dump_ids, fatal=strict)
        print("\n".join(lines))
        if not ok:
            if strict:
                failed.append(section)
            else:
                # 量化口径：missing 为内容移植缺口（基线有、当前无），extra 为当前多出的 id
                gap_counts.append(f"{section} missing {len(set(baseline_ids) - set(dump_ids))}"
                                  f" / extra {len(set(dump_ids) - set(baseline_ids))}")

    print("")
    print("-- 结论 --")
    if failed:
        print(f"RESULT: FAIL — fail 分节: {', '.join(failed)}")
        return 1
    if strict:
        print("RESULT: PASS — 全部分节（含 recipes/tags）白名单外 diff=0")
    else:
        print("RESULT: PASS — 五类注册表白名单外 diff=0")
        if gap_counts:
            print("recipes/tags 缺口（quantify-only，待内容移植任务处理）: " + "; ".join(gap_counts))
        else:
            print("recipes/tags: 无缺口")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
