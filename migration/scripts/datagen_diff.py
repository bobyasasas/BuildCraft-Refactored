#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""M3.4 datagen 对拍脚本（Python 3 标准库，无第三方依赖）。

用法:
  python3 datagen_diff.py <datagen_assets> <shipped_assets> <baseline_assets> [--strict]
      datagen_assets   26.1.2 runData 产物 assets 根（neo/run/datagen/assets）
      shipped_assets   随包资源 assets 根（neo/src/main/resources/assets）
      baseline_assets  1.20.1 legacy datagen 产物 assets 根（buildcraft_resources_generated/assets，只读）
      --strict         物品/方块 id 对拍按白名单外 diff=0 判定（默认仅量化报告，不判 fail）

三个判定层（S1/S3 悬空引用/S4 可达性恒定 fail-closed，不受 --strict 影响）:
  S1 无漂移: datagen 产物 vs 随包资源逐字节 diff。除白名单（1 项手工模型，见下）与手工贴图
     规则（assets/<ns>/textures/**，M4.1 全量入库，见下）外必须 diff=0。这是 M3.4 的核心
     门禁：runData 必须能逐字节再生随包 assets。贴图规则放行是单向的（只允许"随包有、产物无"；
     datagen 侧产出 textures/ 文件、或双树同路径内容不同，仍按漂移 FAIL），并附带收紧的
     fail-closed 卫生校验 texture_hygiene_failures：PNG 魔数/非空、.png.mcmeta 必须是合法
     JSON object 且有对应 PNG（孤儿 mcmeta 恒 fail）、textures/ 下不允许异物文件。
  S2 物品 id 对拍（归一化 N1/N2 后）: 白名单外 diff=0（--strict 判 fail，lenient 仅量化）。
  S3 方块 id 对拍（blockstate 层）: base-only 必须 0；cur-only 白名单外必须 0；悬空模型引用恒 fail。
  S4 方块模型账目（归一化 N3）: 双方"多出"的 models/block 文件必须能被本方某个 blockstate
     经直接引用或 parent 链可达；孤儿模型恒 fail。

1.20.1 -> 26.1.2 归一化规则（显式、可审计；命中项不是"放行漂移"，而是断代差异的登记）:
  N1 items/ 定义新格式: 1.21.4 起物品外观拆为 assets/<ns>/items/<id>.json（当前 977 个），
     1.20.1 基线无此层（0 个）。物品 id 空间归一化为 items/*.json ∪ models/item/*.json(顶层)。
  N2 覆盖子模型: 基线 models/item/<item>/<variant>.json 是 1.20.1 override 谓词子模型（32 个），
     26.1.2 占位资产为单模型。归一化规则：子模型归属于父物品 id，父 id 必须仍存在于当前物品
     id 空间（脚本强制校验，父 id 丢失即 fail）。
  N3 方块模型占位化: M3.2 资产裁决已将 legacy 手工方块模型（quarry/flood_gate 连接状态变体等）
     替换为每方块单一张占位模型（vanilla parent）。因此 models/block 文件级不逐一对拍，改为
     S3 的 blockstate id 对拍 + S4 的可达性账目（任何不可达的孤儿模型都会 fail）。
  N4 数据侧与 lang 不在本脚本范围（登记，不做文件级对拍）:
     - recipes/tags: JSON 格式断代由确定性转换器 neo/tools/convert_recipes_m210.py 处理，
       语义对拍由 registry_diff.py --strict（1561/1561）覆盖；
     - lang: buildcraft_resources_generated 基线不含 lang（legacy datagen 不生成），
       857 键冻结集对拍由 lang_diff.py 覆盖（datagen 侧由 BcLangData.KEY_COUNT==857 守卫）；
     - 纹理 PNG: 非 datagen 产物（基线 240 个为手工放置资产），基线侧不做文件级对拍；
       随包侧自 M4.1 起全量入库（1015 PNG + 24 .png.mcmeta），在 S1 按手工贴图规则
       单向放行并做卫生校验（见 S1 段与 texture_hygiene_failures）。

白名单与规则放行（逐项枚举/单一前缀规则，路径 + 原因；整目录兜底一律不做）:
  S1（随包独有）:
    - buildcraftcore/models/block/pipe_kinesis_wood_inventory.json —
      手写紧凑 elements/from/to/UV 数组模型，stock gson pretty-print 无法产出该字节形态；
      属于手工资产，datagen 不再生它（drift gate 对它单向豁免：只允许"随包有、产物无"）。
    - 规则放行 assets/<ns>/textures/**（M4.1 裁决入库、M4.12 门禁适配）: 手工美术资产，
      1.20.1 基线 1015 PNG + 24 .png.mcmeta 全量移植（md5 一致）。单向豁免（只允许
      "随包有、产物无"），附带收紧的卫生校验 texture_hygiene_failures（PNG 魔数、
      mcmeta 合法 JSON、无孤儿 mcmeta、无异物文件），任何卫生违规恒 fail。
  S2/S3（id 级，两侧各自枚举）:
    - 基线独有 31 项物品 id:
        buildcraftenergy:<fuel|oil|...>_bucket_christmas ×30 — legacy 圣诞彩蛋 override 模型，
          26.1.2 注册表基线无对应物品 id（registry_diff.py --strict 1561/1561 佐证两侧注册表一致）；
        buildcraftrobotics:robot — legacy RobotEntity 的基础模型文件（被 robot_<type>.json parent
          链引用），不是注册物品 id。
    - 当前独有 4 项物品 id（与 3 项 blockstate id 同源）:
        buildcraftcore:energy_meter / marker / pipe_kinesis_wood — M2.2 垂直切片 id，
          与 RegistryParityTest.EXTRA_WHITELIST 同源；legacy datagen 未为它们产出资产；
        buildcraftrobotics:robot_googles — 1.20.1 注册表基线含此物品（registry 基线 977 items
          一致），但 legacy datagen 漏生成其模型；26.1.2 datagen 覆盖每个注册物品（fail-closed）。
    - minecraft:atlases/blocks.json — legacy 管道精灵图集（textures/pipes/* 的 single 源清单）；
       26.1.2 树已无 pipes/ 纹理（M3.2 占位化后无引用方），datagen 无对应产物。

退出码: 0 通过；1 有 fail；2 对拍工具自身错误（目录缺失/JSON 非法/结构异常，fail-closed）。
"""

import json
import os
import re
import sys

USAGE = ("用法: python3 datagen_diff.py <datagen_assets> <shipped_assets> <baseline_assets> [--strict]")

# ---------------------------------------------------------------------------
# 白名单（逐项枚举；改动任何一项都必须同步更新模块 docstring 的登记）
# ---------------------------------------------------------------------------

# S1: 随包独有、datagen 不再生的手工资产（relpath, 原因）。
SHIPPED_ONLY_WHITELIST = {
    "buildcraftcore/models/block/pipe_kinesis_wood_inventory.json":
        "手写紧凑 elements/UV 数组模型，gson pretty-print 无法产出该字节形态（手工资产）",
}


# S1 规则放行（M4.1/M4.12 裁决）：assets/<ns>/textures/** 为手工放置的美术资产
# （1.20.1 基线全量入库，md5 一致），本质不由 datagen 再生。放行单向：只允许
# "随包有、产物无"；datagen 侧产出 textures/ 文件、或双树同路径内容不同，仍按漂移 FAIL。
def is_handplaced_texture(rel):
    parts = rel.split("/")
    return len(parts) >= 3 and parts[1] == "textures"


def texture_hygiene_failures(shipped):
    """随包 textures/ 资产的 fail-closed 卫生校验，返回 [(kind, rel)]。

    收紧项（M4.12）：放行不等于免检——
      - .png 必须 8 字节 PNG 魔数开头（空文件/文本伪装恒 fail）；
      - .png.mcmeta 必须是合法 JSON object；
      - .png.mcmeta 必须有同路径对应 .png（孤儿 mcmeta 恒 fail）；
      - textures/ 下不允许 .png/.png.mcmeta 之外的异物文件。
    """
    bad = []
    pngs = set()
    metas = set()
    for rel, path in sorted(shipped.items()):
        if not is_handplaced_texture(rel):
            continue
        if rel.endswith(".png"):
            pngs.add(rel)
            with open(path, "rb") as f:
                head = f.read(8)
            if head != b"\x89PNG\r\n\x1a\n":
                bad.append(("贴图损坏(非PNG或空文件)", rel))
        elif rel.endswith(".png.mcmeta"):
            metas.add(rel)
            try:
                with open(path, encoding="utf-8") as f:
                    meta = json.load(f)
                if not isinstance(meta, dict):
                    raise ValueError("顶层必须是 JSON object")
            except (ValueError, OSError, UnicodeDecodeError) as exc:
                bad.append(("贴图mcmeta非法(%s)" % exc, rel))
        else:
            bad.append(("贴图目录异物", rel))
    for rel in sorted(metas):
        if rel[: -len(".mcmeta")] not in pngs:
            bad.append(("贴图mcmeta孤儿(无对应PNG)", rel))
    return bad

# 与 RegistryParityTest.EXTRA_WHITELIST 同源的 M2.2 垂直切片 id（按 id path 匹配）。
SLICE_IDS = frozenset({"marker", "engine_stone", "pipe_kinesis_wood", "energy_meter"})

# S2 基线独有物品 id（ns, path）：30 项 legacy 圣诞彩蛋 override 模型
# （fuel/oil 各热度档位的 *_bucket_christmas，逐项枚举，不做前缀兜底）+ robotics 实体基础模型。
CHRISTMAS_WHITELIST = frozenset((
    "buildcraftenergy:fuel_dense_heat_0_bucket_christmas",
    "buildcraftenergy:fuel_dense_heat_1_bucket_christmas",
    "buildcraftenergy:fuel_dense_heat_2_bucket_christmas",
    "buildcraftenergy:fuel_gaseous_heat_0_bucket_christmas",
    "buildcraftenergy:fuel_gaseous_heat_1_bucket_christmas",
    "buildcraftenergy:fuel_gaseous_heat_2_bucket_christmas",
    "buildcraftenergy:fuel_light_heat_0_bucket_christmas",
    "buildcraftenergy:fuel_light_heat_1_bucket_christmas",
    "buildcraftenergy:fuel_light_heat_2_bucket_christmas",
    "buildcraftenergy:fuel_mixed_heavy_heat_0_bucket_christmas",
    "buildcraftenergy:fuel_mixed_heavy_heat_1_bucket_christmas",
    "buildcraftenergy:fuel_mixed_heavy_heat_2_bucket_christmas",
    "buildcraftenergy:fuel_mixed_light_heat_0_bucket_christmas",
    "buildcraftenergy:fuel_mixed_light_heat_1_bucket_christmas",
    "buildcraftenergy:fuel_mixed_light_heat_2_bucket_christmas",
    "buildcraftenergy:oil_dense_heat_0_bucket_christmas",
    "buildcraftenergy:oil_dense_heat_1_bucket_christmas",
    "buildcraftenergy:oil_dense_heat_2_bucket_christmas",
    "buildcraftenergy:oil_distilled_heat_0_bucket_christmas",
    "buildcraftenergy:oil_distilled_heat_1_bucket_christmas",
    "buildcraftenergy:oil_distilled_heat_2_bucket_christmas",
    "buildcraftenergy:oil_heat_0_bucket_christmas",
    "buildcraftenergy:oil_heat_1_bucket_christmas",
    "buildcraftenergy:oil_heat_2_bucket_christmas",
    "buildcraftenergy:oil_heavy_heat_0_bucket_christmas",
    "buildcraftenergy:oil_heavy_heat_1_bucket_christmas",
    "buildcraftenergy:oil_heavy_heat_2_bucket_christmas",
    "buildcraftenergy:oil_residue_heat_0_bucket_christmas",
    "buildcraftenergy:oil_residue_heat_1_bucket_christmas",
    "buildcraftenergy:oil_residue_heat_2_bucket_christmas",
))
BASE_ONLY_ITEM_WHITELIST = CHRISTMAS_WHITELIST | {
    "buildcraftrobotics:robot",  # legacy RobotEntity 基础模型，非注册物品 id
}

# S2/S3 当前独有 id 白名单：3 个切片 id + robot_googles（legacy datagen 漏生成）。
CUR_ONLY_WHITELIST = {
    "buildcraftcore:energy_meter",
    "buildcraftcore:marker",
    "buildcraftcore:pipe_kinesis_wood",
    "buildcraftrobotics:robot_googles",
}
# S3 blockstate 层当前独有（robot_googles 是纯物品，无 blockstate）。
CUR_ONLY_BLOCKSTATE_WHITELIST = {
    "buildcraftcore:energy_meter",
    "buildcraftcore:marker",
    "buildcraftcore:pipe_kinesis_wood",
}

# minecraft:atlases/blocks.json（legacy 管道精灵图集）。
BASE_ONLY_ATLAS_WHITELIST = {"minecraft:atlases/blocks.json"}

# blockstate/block 模型里合法的资源引用形如 "ns:block/xxx" / "ns:item/xxx"。
REF_RE = re.compile(r'"([a-z0-9_]+):(block|item)/([a-z0-9_/]+)"')

MOD_NAMESPACES = frozenset(
    "buildcraft" + s for s in ("lib", "core", "builders", "energy", "factory", "silicon", "transport", "robotics")
)


def fail_tool(msg):
    print(f"[错误] {msg}（fail-closed）", file=sys.stderr)
    sys.exit(2)


def scan_tree(root, label):
    """返回 {relpath(unix / 分隔): 绝对路径}；根目录必须存在。"""
    if not os.path.isdir(root):
        fail_tool(f"{label} 目录不存在: {root}")
    out = {}
    for dirpath, _dirnames, filenames in os.walk(root):
        for fn in sorted(filenames):
            full = os.path.join(dirpath, fn)
            out[os.path.relpath(full, root).replace(os.sep, "/")] = full
    return out


def load_json(path, label):
    try:
        with open(path, encoding="utf-8") as fh:
            return json.load(fh)
    except (OSError, json.JSONDecodeError, UnicodeDecodeError) as exc:
        fail_tool(f"{label} {path} 无法读取/非法 JSON: {exc}")


def namespaces(tree):
    return {rel.split("/", 1)[0] for rel in tree}


def item_ids(tree, root):
    """归一化 N1 后的物品 id 空间: items/*.json ∪ models/item/*.json(顶层)。
    返回 (id 集合, 覆盖子模型 [(ns, '<item>/<variant>')]，子模型按 N2 归属父 id)。"""
    ids, subs = set(), []
    for rel in tree:
        parts = rel.split("/")
        if len(parts) == 3 and parts[1] == "items" and parts[2].endswith(".json"):
            ids.add((parts[0], parts[2][:-5]))
        elif len(parts) == 4 and parts[1] == "models" and parts[2] == "item" and parts[3].endswith(".json"):
            ids.add((parts[0], parts[3][:-5]))
        elif len(parts) == 5 and parts[1] == "models" and parts[2] == "item" and parts[4].endswith(".json"):
            subs.append((parts[0], parts[3] + "/" + parts[4][:-5]))
    return ids, subs


def blockstate_ids(tree):
    return {(rel.split("/")[0], rel.split("/")[2][:-5])
            for rel in tree if len(rel.split("/")) == 3 and rel.split("/")[1] == "blockstates"}


def block_model_ids(tree):
    """models/block 文件 id（ns, path；path 含子目录段）。"""
    out = set()
    for rel in tree:
        parts = rel.split("/")
        if len(parts) >= 4 and parts[1] == "models" and parts[2] == "block" and parts[-1].endswith(".json"):
            out.add((parts[0], "/".join(parts[3:])[:-5]))
    return out


def blockstate_refs(tree, root):
    """解析全部 blockstate 的直接模型引用，归一化为文件 id 形态 (ns, path)（剥掉 'block/' 段）。"""
    direct = set()
    for rel, path in tree.items():
        parts = rel.split("/")
        if len(parts) != 3 or parts[1] != "blockstates":
            continue
        raw = json.dumps(load_json(path, "引用解析"))
        for ns, folder, name in REF_RE.findall(raw):
            if folder == "block":
                direct.add((ns, name))
    return direct


def build_reachability(tree, root):
    """blockstate 直接引用 -> BFS 沿 block 模型出边；只在本命名空间树内走。
    返回 (可达模型 id 集, 悬空引用列表)。悬空 = 引用了树内不存在的 buildcraft 模型文件。"""
    direct = blockstate_refs(tree, root)
    model_files = block_model_ids(tree)
    dangling = sorted(
        f"{ns}:block/{path}" for ns, path in direct
        if ns in MOD_NAMESPACES and (ns, path) not in model_files
    )
    # 出边表：模型文件 id -> 该文件 JSON 内引用到的本树模型 id
    out_edges = {}
    for rel, path in tree.items():
        parts = rel.split("/")
        if len(parts) >= 4 and parts[1] == "models" and parts[2] == "block" and parts[-1].endswith(".json"):
            fid = (parts[0], "/".join(parts[3:])[:-5])
            raw = json.dumps(load_json(path, "出边解析"))
            targets = set()
            for ns, _folder, name in REF_RE.findall(raw):
                if (ns, name) in model_files:
                    targets.add((ns, name))
            out_edges[fid] = targets
    seen, stack = set(), [t for t in direct if t in model_files]
    while stack:
        cur = stack.pop()
        if cur in seen:
            continue
        seen.add(cur)
        for nxt in out_edges.get(cur, ()):
            if nxt not in seen:
                stack.append(nxt)
    return seen, dangling, direct


def diff_id_set(section, base, cur, base_whitelist, cur_whitelist, fatal):
    """registry_diff.py 同款对拍输出：白名单外 missing/extra。返回 (报告行, ok)。"""
    missing_all = sorted(base - cur)
    extra_all = sorted(cur - base)
    missing = [x for x in missing_all if f"{x[0]}:{x[1]}" not in base_whitelist]
    extra = [x for x in extra_all if f"{x[0]}:{x[1]}" not in cur_whitelist]
    hit_base = [x for x in missing_all if f"{x[0]}:{x[1]}" in base_whitelist]
    hit_cur = [x for x in extra_all if f"{x[0]}:{x[1]}" in cur_whitelist]
    ok = not missing and not extra
    verdict = "OK" if ok else ("FAIL" if fatal else "GAP(quantify-only)")
    lines = [
        f"{section}: baseline {len(base)}, current {len(cur)},"
        f" missing(白名单外) {len(missing)}, extra(白名单外) {len(extra)},"
        f" 白名单放行 {len(hit_base) + len(hit_cur)}  -> {verdict}"
    ]
    if missing:
        lines.append(f"    missing 白名单外 ({len(missing)}): " + ", ".join(f"{n}:{p}" for n, p in missing))
    if extra:
        lines.append(f"    extra 白名单外 ({len(extra)}): " + ", ".join(f"{n}:{p}" for n, p in extra))
    if hit_base:
        lines.append(f"    missing 白名单放行 ({len(hit_base)}): " + ", ".join(f"{n}:{p}" for n, p in hit_base))
    if hit_cur:
        lines.append(f"    extra 白名单放行 ({len(hit_cur)}): " + ", ".join(f"{n}:{p}" for n, p in hit_cur))
    return lines, ok


def main(argv):
    paths = [a for a in argv if not a.startswith("--")]
    strict = "--strict" in argv
    unknown = [a for a in argv if a.startswith("--") and a != "--strict"]
    if len(paths) != 3 or unknown:
        if unknown:
            print(f"未知参数: {' '.join(unknown)}", file=sys.stderr)
        print(USAGE + "  [--strict]", file=sys.stderr)
        return 2
    datagen_dir, shipped_dir, baseline_dir = paths

    datagen = scan_tree(datagen_dir, "datagen 产物")
    shipped = scan_tree(shipped_dir, "随包资源")
    baseline = scan_tree(baseline_dir, "基线")
    if not datagen:
        fail_tool("datagen 产物目录为空: " + datagen_dir)
    if not shipped:
        fail_tool("随包资源目录为空: " + shipped_dir)

    print("=== M3.4 datagen 对拍 ===")
    print(f"datagen:  {datagen_dir} ({len(datagen)} files)")
    print(f"shipped:  {shipped_dir} ({len(shipped)} files)")
    print(f"baseline: {baseline_dir} ({len(baseline)} files)")
    print(f"模式: {'--strict（id 对拍白名单外 diff=0 判 fail）' if strict else 'lenient（id 对拍仅量化报告）'}")
    print("")

    failures = []

    # ---- S1 无漂移（恒定 fail-closed） ----
    print("-- S1 无漂移: datagen 产物 vs 随包资源（逐字节，恒定 fail-closed）--")
    drift = []
    for rel in sorted(set(shipped) | set(datagen)):
        if rel in shipped and rel in datagen:
            with open(shipped[rel], "rb") as fa, open(datagen[rel], "rb") as fb:
                if fa.read() != fb.read():
                    drift.append(("内容不同", rel))
        elif rel in shipped:
            # 产物缺失侧：白名单手工模型与手工贴图规则（单向）允许"随包有、产物无"
            if rel not in SHIPPED_ONLY_WHITELIST and not is_handplaced_texture(rel):
                drift.append(("随包独有(白名单外)", rel))
        else:
            drift.append(("产物独有", rel))
    allowed = [rel for rel in shipped if rel in SHIPPED_ONLY_WHITELIST and rel not in datagen]
    tex_exempt = [rel for rel in shipped if is_handplaced_texture(rel) and rel not in datagen]
    hygiene = texture_hygiene_failures(shipped)
    for kind, rel in drift:
        print(f"    DRIFT [{kind}] {rel}")
    for kind, rel in hygiene:
        print(f"    BADTEX [{kind}] {rel}")
    s1_ok = not drift and not hygiene
    print(f"S1: 比对 {len(set(shipped) | set(datagen))} 路径, drift {len(drift)},"
          f" 白名单放行 {len(allowed)}, 贴图规则放行 {len(tex_exempt)}(卫生违规 {len(hygiene)})"
          f"  -> {'OK' if s1_ok else 'FAIL'}")
    if not s1_ok:
        failures.append("S1 无漂移" if drift else "S1 贴图卫生")
    else:
        print(f"    白名单放行: {', '.join(allowed)}")
        print(f"    贴图规则放行: {len(tex_exempt)} 项手工贴图（卫生校验通过，不逐项罗列）")
    print("")

    # ---- S2 物品 id 对拍（N1/N2 归一化） ----
    print("-- S2 物品 id 对拍（归一化 N1: items/∪models/item；N2: 子模型归属父 id）--")
    cur_ids, cur_subs = item_ids(datagen, datagen_dir)
    base_ids, base_subs = item_ids(baseline, baseline_dir)
    # N2 强制校验：每个基线子模型的父 id 必须仍在当前物品 id 空间
    lost_parents = sorted(
        f"{ns}:{sub.split('/')[0]}" for ns, sub in base_subs
        if (ns, sub.split("/")[0]) not in cur_ids
    )
    print(f"    基线覆盖子模型 {len(base_subs)} 个（N2 归属父 id 后不参与 id diff），"
          f"父 id 丢失 {len(lost_parents)}  -> {'OK' if not lost_parents else 'FAIL'}")
    for x in lost_parents:
        print(f"    LOST PARENT {x}")
    if lost_parents:
        failures.append("S2 覆盖子模型父 id")
    # 内部一致性：当前每个 models/item 顶层模型必须有对应 items/ 定义
    model_files = sum(1 for rel in datagen
                      if len(rel.split("/")) == 4 and rel.split("/")[1] == "models" and rel.split("/")[2] == "item")
    defs = {(rel.split("/")[0], rel.split("/")[2][:-5])
            for rel in datagen if len(rel.split("/")) == 3 and rel.split("/")[1] == "items"}
    orphan_models = sorted(f"{n}:{p}" for n, p in cur_ids if (n, p) not in defs)
    print(f"    当前 items/ 定义 {len(defs)}, models/item 顶层文件 {model_files}"
          f", 无定义的模型 {len(orphan_models)}  -> {'OK' if not orphan_models else 'FAIL'}")
    for x in orphan_models:
        print(f"    ORPHAN MODEL {x}")
    if orphan_models:
        failures.append("S2 items/定义覆盖")
    lines, ok = diff_id_set("S2 物品 id", base_ids, cur_ids,
                            BASE_ONLY_ITEM_WHITELIST, CUR_ONLY_WHITELIST, fatal=strict)
    print("\n".join(lines))
    if not ok:
        failures.append("S2 物品 id")
    print("")

    # ---- S3 方块 id 对拍（blockstate 层）+ 悬空引用 ----
    print("-- S3 方块 id 对拍（blockstate 层）+ 悬空模型引用（恒定 fail-closed）--")
    for label, tree, root in (("datagen", datagen, datagen_dir), ("baseline", baseline, baseline_dir)):
        _reach, dangling, _direct = build_reachability(tree, root)
        if dangling:
            print(f"    DANGLING [{label}] 引用了不存在的模型文件:")
            for x in dangling:
                print(f"      {x}")
            failures.append(f"S3 悬空引用({label})")
        else:
            print(f"    {label}: blockstate 模型引用无悬空  -> OK")
    base_bs = blockstate_ids(baseline)
    cur_bs = blockstate_ids(datagen)
    lines, ok = diff_id_set("S3 blockstate id", base_bs, cur_bs,
                            frozenset(), CUR_ONLY_BLOCKSTATE_WHITELIST, fatal=strict)
    print("\n".join(lines))
    if not ok:
        failures.append("S3 blockstate id")
    print("")

    # ---- S4 方块模型账目（N3 归一化：可达性，恒定 fail-closed） ----
    print("-- S4 方块模型账目（归一化 N3: 文件级差异必须被 blockstate 引用图可达，孤儿恒 fail）--")
    for label, tree, root in (("datagen", datagen, datagen_dir), ("baseline", baseline, baseline_dir)):
        reach, _dangling, direct = build_reachability(tree, root)
        files = block_model_ids(tree)
        others = {"baseline": datagen, "datagen": baseline}[label]
        other_files = block_model_ids(others)
        only = sorted(files - other_files)
        orphans = sorted(x for x in only if x not in reach)
        print(f"    {label}: 方块模型文件 {len(files)}, 本侧独有 {len(only)},"
              f" blockstate 引用图不可达的孤儿 {len(orphans)}  -> {'OK' if not orphans else 'FAIL'}")
        for x in orphans:
            print(f"      ORPHAN {x[0]}:{x[1]}")
        if orphans:
            failures.append(f"S4 孤儿模型({label})")
    print("")
    print("    N3 说明: legacy 多变体手模（quarry/flood_gate 连接态等）已按 M3.2 裁决占位化，"
          "文件级不做逐一 id 对拍，以上可达性账目保证没有不可解释的模型文件。")
    print("")

    # ---- S5 覆盖面登记（恒过，仅显式列出断代差异的去向） ----
    print("-- S5 覆盖面登记（归一化 N1/N4：不在本脚本对拍范围的断代项及去向）--")
    base_textures = sum(1 for rel in baseline if "/textures/" in rel)
    cur_textures = sum(1 for rel in datagen if "/textures/" in rel)
    base_lang = sum(1 for rel in baseline if "/lang/" in rel)
    cur_lang = sum(1 for rel in datagen if "/lang/" in rel)
    base_items_json = sum(1 for rel in baseline if len(rel.split("/")) == 3 and rel.split("/")[1] == "items")
    cur_items_json = sum(1 for rel in datagen if len(rel.split("/")) == 3 and rel.split("/")[1] == "items")
    print(f"    items/ 定义层 (N1 新格式): baseline {base_items_json}, current {cur_items_json}"
          f" — 1.21.4+ 新增层，已并入 S2 物品 id 空间")
    print(f"    lang (N4): baseline {base_lang}, current {cur_lang}"
          f" — 基线不含 lang，857 键冻结集由 lang_diff.py 覆盖")
    print(f"    纹理 PNG (N4): baseline {base_textures}, current {cur_textures}"
          f" — 非 datagen 产物（手工放置资产）")
    print(f"    minecraft 图集: baseline {len(BASE_ONLY_ATLAS_WHITELIST)} 项"
          f"（legacy pipes 精灵图集，M3.2 占位化后无引用方）— S2 白名单登记")
    print("    数据侧 recipes/tags (N4): 格式断代由 convert_recipes_m210.py 处理，"
          "语义对拍由 registry_diff.py --strict 覆盖")
    print("")

    # ---- 结论 ----
    if failures:
        print(f"RESULT: FAIL — fail 项: {', '.join(dict.fromkeys(failures))}")
        return 1
    if strict:
        wl = len(SHIPPED_ONLY_WHITELIST) + len(BASE_ONLY_ITEM_WHITELIST) + len(CUR_ONLY_WHITELIST) \
            + len(CUR_ONLY_BLOCKSTATE_WHITELIST) + len(BASE_ONLY_ATLAS_WHITELIST)
        tex_n = sum(1 for rel in shipped if is_handplaced_texture(rel))
        print(f"RESULT: PASS — 无漂移 diff=0（白名单 {len(SHIPPED_ONLY_WHITELIST)} 项 +"
              f"贴图规则放行 {tex_n} 项，卫生校验通过）；物品/方块 id 对拍白名单外 diff=0"
              f"（白名单合计 {wl} 项，逐项枚举见脚本头注）")
    else:
        print("RESULT: PASS — 无漂移 diff=0；id 对拍白名单外 diff=0（lenient 模式）")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
