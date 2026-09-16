#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""M2.10 内容资产迁移：把 1.20.1 legacy datagen 的 recipe/tag JSON 一次性转换成 26.1.2 格式。

用法（仓库根目录执行，或传显式路径）:
  python3 neo/tools/convert_recipes_m210.py [SRC] [DST]
      SRC  legacy datagen 根（默认 buildcraft_resources_generated/data，只读红线，绝不写入）
      DST  neo 资源根（默认 neo/src/main/resources/data）

脚本可重跑、幂等：先清空自己拥有的输出目录（8 个模块命名空间的 recipe/、buildcraft/tags/、
c/tags/），再全量重新生成；绝不触碰 DST 下其它内容（如 buildcraftenergy/tags/worldgen、
buildcraftcore/function、minecraft/tags/function 等 M2.8/M3.1 已落地资产）。

格式映射（断代点，来源：26.1.2 反编译源码 decompile_*_output.jar 与
minecraft_26.1.2_client.jar 里的 vanilla 数据样例，非凭记忆）:
  - 目录名：recipes/ -> recipe/（RecipeManager 用 Registries.elementsDirPath(RECIPE)= "recipe"），
    tags/items -> tags/item、tags/blocks -> tags/block（vanilla 客户端数据实际布局）；
  - vanilla 合成配方（crafting_shaped/shapeless，1377 个）：
      ingredient {"item": x} -> "x"，{"tag": t} -> "#t'"（t 经 forgeTagMap 映射），
      数组 -> 字符串数组；result {"item": x, "count": n} -> {"id": x}（count>1 才带 count；
      ItemStackTemplate.MAP_CODEC 的字段是 id/count，无 item）；
      type/category/group/show_notification/pattern 原样保留（26.1.2 codec 同名字段）；
  - BC 自定义类型（184 个）JSON 字段名逐字保留（serializer Java 一并移植，见
    neo/src/main/java/buildcraft/{silicon,factory,energy}/recipe/），仅两处变化：
      ingredient 对象 {"item"|"tag"} -> "#"-前缀字符串/裸字符串（与 vanilla 同一
      Ingredient.CODEC 惯用法）；output.nbt 丢弃（1.20.1 NBT -> 26.1.2 数据组件断代，
      50 个 assembly 配方受影响，输出的 wire/lens/paintbrush 均为占位物品，语义随
      后续机器里程碑 M2.11-M2.13 恢复）；
      heat_exchange/heatable、heat_exchange/coolable、distillation、fuel 四类纯
      流体+数字，无任何 item 引用 -> 原样字节级复制；
  - forge: 标签引用：NeoForge 26.1.2 无 forge: 命名空间（通用标签已改名 c:），全部经
    forgeTagMap 重写到 c:（改名直接映射）或本 mod 自带的 c: vendor 标签（见 vendorTags）；
    未知 forge: 引用直接报错退出（fail-closed）；
  - tag 值格式无断代：{"values": [{id, required}]} 与 "#ns:path" 引用 26.1.2 仍支持
    （TagEntry.FULL_CODEC），legacy 的 buildcraft: 标签仅目录改名 + 内容里 forge: 引用重写；
  - legacy 的 forge:/minecraft: 纯 tag 基建文件（8+4 个）不迁移：基线 tags 口径只有
    buildcraft* + buildcraft 命名空间（tags.item 66 / block 1 / fluid 1），它们不在口径内；
    26.1.2 侧需要的等价物由 forgeTagMap 的改名映射 + vendorTags 提供。
"""

import glob
import json
import os
import shutil
import sys
from collections import Counter

SRC_DEFAULT = "buildcraft_resources_generated/data"
DST_DEFAULT = "neo/src/main/resources/data"

MODULE_NS = ("buildcraftbuilders", "buildcraftcore", "buildcraftenergy", "buildcraftfactory",
             "buildcraftlib", "buildcraftrobotics", "buildcraftsilicon", "buildcrafttransport")

# 26.1.2 运行时实际存在的 c: 通用标签（neoforge-26.1.2.109-universal.jar data/c/tags）；
# 改名映射逐项核对过 jar 内容，1:1 语义等价（个别为复数化改名）。
FORGE_RENAMES = {
    "forge:bookshelves": "c:bookshelves",
    "forge:chests/wooden": "c:chests/wooden",
    "forge:cobblestone": "c:cobblestones",
    "forge:gravel": "c:gravels",
    "forge:ingots/gold": "c:ingots/gold",
    "forge:ingots/iron": "c:ingots/iron",
    "forge:obsidian": "c:obsidians",
    "forge:rods/wooden": "c:rods/wooden",
    "forge:sand": "c:sands",
    "forge:sandstone": "c:sandstone/blocks",
    "forge:stone": "c:stones",
    "forge:storage_blocks/lapis": "c:storage_blocks/lapis",
    "forge:storage_blocks/redstone": "c:storage_blocks/redstone",
    "forge:string": "c:strings",
    "forge:tools/wrench": "c:tools/wrench",
    "forge:slimeballs": "c:slime_balls",
    "forge:gems/diamond": "c:gems/diamond",
    "forge:gems/emerald": "c:gems/emerald",
    "forge:gems/lapis": "c:gems/lapis",
    "forge:gems/quartz": "c:gems/quartz",
    "forge:dusts/redstone": "c:dusts/redstone",
    "forge:glass/colorless": "c:glass_blocks/colorless",
}
# 16 种染料：forge:dyes/<color> -> c:dyes/<color>（c: 全 16 色齐全）
FORGE_RENAMES.update({f"forge:dyes/{c}": f"c:dyes/{c}" for c in (
    "black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray",
    "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow")})

# 26.1.2 运行时缺失、由本 mod 自建的 c: vendor 标签（成员取自 legacy datagen 的
# forge: 同名 tag 或 1.20.1 forge: 标签语义）。
VENDOR_COLORS = ("black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray",
                 "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow")
VENDOR_TAGS = {
    "clay": ["minecraft:clay"],
    "workbenches": ["minecraft:crafting_table"],
    "storage_blocks/quartz": ["minecraft:quartz_block"],
    "ingots/brick": ["minecraft:brick"],
    "ingots/nether_brick": ["minecraft:nether_brick"],
    "gears/wood": ["buildcraftcore:gear_wood"],
    "gears/stone": ["buildcraftcore:gear_stone"],
    "gears/iron": ["buildcraftcore:gear_iron"],
    "gears/gold": ["buildcraftcore:gear_gold"],
    "gears/diamond": ["buildcraftcore:gear_diamond"],
    "gears": ["#c:gears/wood", "#c:gears/stone", "#c:gears/iron", "#c:gears/gold",
              "#c:gears/diamond"],
}
VENDOR_TAGS.update({f"glass/{c}": [f"minecraft:{c}_stained_glass"] for c in VENDOR_COLORS})
# 与 forgeTagMap 合并成完整引用映射
FORGE_TAG_MAP = dict(FORGE_RENAMES)
FORGE_TAG_MAP.update({f"forge:{path}": f"c:{path}" for path in VENDOR_TAGS})

# 纯流体/数字自定义类型：字节级复制（26.1.2 侧 codec 字段名与其逐字一致）
VERBATIM_COPY_TYPES = {
    "buildcraftfactory:heat_exchange/heatable",
    "buildcraftfactory:heat_exchange/coolable",
    "buildcraftfactory:distillation",
    "buildcraftenergy:fuel",
}
VANILLA_TYPES = {"minecraft:crafting_shaped", "minecraft:crafting_shapeless"}

owned_dirs = []  # DST 下由本脚本清空重建的目录（相对 DST）


def map_tag(tag_id):
    """forge: 引用 -> c: 映射；其余（minecraft:/buildcraft:/c:）原样。"""
    if tag_id.startswith("forge:"):
        mapped = FORGE_TAG_MAP.get(tag_id)
        if mapped is None:
            raise SystemExit(f"[fail-closed] 未知 forge: 标签引用（映射表缺失）: {tag_id}")
        return mapped
    return tag_id


def map_ingredient(obj):
    """{"item": x} -> "x"；{"tag": t} -> "#t'"; 数组逐项递归；其余原样。"""
    if isinstance(obj, list):
        return [map_ingredient(o) for o in obj]
    if isinstance(obj, dict):
        if "item" in obj and "tag" not in obj:
            return obj["item"]
        if "tag" in obj and "item" not in obj:
            return "#" + map_tag(obj["tag"])
    raise SystemExit(f"[fail-closed] 无法识别的 1.20.1 ingredient 形状: {obj!r}")


def map_result(obj):
    """{"item": x[, "count": n]} -> {"id": x[, "count": n]}；26.1.2 ItemStackTemplate 断代。"""
    out = {"id": obj["item"]}
    if obj.get("count", 1) != 1:
        out["count"] = obj["count"]
    return out


def convert_vanilla_crafting(doc):
    doc = dict(doc)
    if "key" in doc:
        doc["key"] = {sym: map_ingredient(ing) for sym, ing in doc["key"].items()}
    if "ingredients" in doc:
        doc["ingredients"] = [map_ingredient(ing) for ing in doc["ingredients"]]
    if "result" in doc:
        doc["result"] = map_result(doc["result"])
    return doc


def convert_stack(obj):
    """BC 自定义类型的输入栈 {"count": n, "ingredient": {...}} -> 字符串或 {"ingredient","count"}。"""
    ingredient = map_ingredient(obj["ingredient"])
    count = obj.get("count", 1)
    if count == 1:
        return ingredient
    return {"ingredient": ingredient, "count": count}


def convert_output(obj):
    """BC 自定义类型的输出栈 {"item", "count"?, "nbt"?} -> {"id"[, "count"]}；nbt 丢弃（断代）。"""
    out = {"id": obj["item"]}
    if obj.get("count", 1) != 1:
        out["count"] = obj["count"]
    return out


def convert_custom(doc):
    t = doc["type"]
    doc = dict(doc)
    if t == "buildcraftsilicon:assembly":
        if doc.get("subType") == "BASIC":
            doc["requiredStacks"] = [convert_stack(s) for s in doc["requiredStacks"]]
            doc["output"] = convert_output(doc["output"])
        # FACADE：只有 type+subType，原样保留
    elif t == "buildcraftsilicon:programming":
        doc["input"] = convert_stack(doc["input"])
        doc["output"] = convert_output(doc["output"])
    elif t == "buildcraftsilicon:integration":
        doc["centerStack"] = convert_stack(doc["centerStack"])
        doc["requirements"] = [convert_stack(s) for s in doc["requirements"]]
        doc["output"] = convert_output(doc["output"])
    elif t == "buildcraftsilicon:facade_swap":
        pass  # {"type": ...} 本来就无字段
    elif t == "buildcraftenergy:coolant":
        if doc.get("coolantType") == "solid":
            doc["solid"] = map_ingredient(doc["solid"])
        # fluid 冷却剂的 degreesCoolingPerMb 纯数字，原样
    else:
        raise SystemExit(f"[fail-closed] 未规划的自定义类型: {t}")
    return doc


def write_json(path, doc):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(doc, fh, ensure_ascii=False, indent=2)
        fh.write("\n")


def raw_copy(src, dst):
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(src, dst)


def main(argv):
    src = os.path.normpath(argv[1]) if len(argv) > 1 else SRC_DEFAULT
    dst = os.path.normpath(argv[2]) if len(argv) > 2 else DST_DEFAULT
    if not os.path.isdir(src):
        raise SystemExit(f"legacy datagen 目录不存在: {src}")

    counts = Counter()

    # 1) 清空本脚本拥有的输出目录（8 个模块 ns 的 recipe/ + buildcraft/tags/ + c/tags/）
    for ns in MODULE_NS:
        owned_dirs.append(os.path.join(dst, ns, "recipe"))
    owned_dirs.append(os.path.join(dst, "buildcraft", "tags"))
    owned_dirs.append(os.path.join(dst, "c", "tags"))
    for d in owned_dirs:
        if os.path.isdir(d):
            shutil.rmtree(d)

    # 2) recipes：8 个模块命名空间 recipes/ -> recipe/，按 type 分派转换
    for src_ns_dir in sorted(glob.glob(os.path.join(src, "*", "recipes"))):
        ns = os.path.basename(os.path.dirname(src_ns_dir))
        if ns not in MODULE_NS:
            continue
        for src_file in sorted(glob.glob(os.path.join(src_ns_dir, "**", "*.json"), recursive=True)):
            rel = os.path.relpath(src_file, src_ns_dir)
            dst_file = os.path.join(dst, ns, "recipe", rel)
            with open(src_file, encoding="utf-8") as fh:
                doc = json.load(fh)
            rtype = doc.get("type")
            if rtype in VERBATIM_COPY_TYPES:
                raw_copy(src_file, dst_file)
                counts[f"{rtype} (原样复制)"] += 1
            elif rtype in VANILLA_TYPES:
                write_json(dst_file, convert_vanilla_crafting(doc))
                counts[rtype] += 1
            elif isinstance(rtype, str) and rtype.startswith("buildcraft"):
                write_json(dst_file, convert_custom(doc))
                counts[rtype] += 1
            else:
                raise SystemExit(f"[fail-closed] 未规划的 recipe type {rtype!r}: {src_file}")
            counts[f"ns:{ns}"] += 1

    # 3) tags：buildcraft 命名空间 items/blocks/fluids -> item/block/fluid，内容里 forge: 引用重写
    for legacy_cat, new_cat in (("items", "item"), ("blocks", "block"), ("fluids", "fluid")):
        for src_file in sorted(glob.glob(os.path.join(src, "buildcraft", "tags", legacy_cat, "**", "*.json"),
                                         recursive=True)):
            rel = os.path.relpath(src_file, os.path.join(src, "buildcraft", "tags", legacy_cat))
            dst_file = os.path.join(dst, "buildcraft", "tags", new_cat, rel)
            with open(src_file, encoding="utf-8") as fh:
                doc = json.load(fh)
            values = []
            for value in doc.get("values", []):
                if isinstance(value, dict) and isinstance(value.get("id"), str) and value["id"].startswith("#"):
                    value = dict(value, id="#" + map_tag(value["id"][1:]))
                values.append(value)
            write_json(dst_file, {"values": values})
            counts[f"tag:buildcraft:{new_cat}"] += 1

    # 4) c: vendor 标签（26.1.2 运行时缺失的 forge: 等价物；放 c: 命名空间以保持
    #    基线 tags 口径（buildcraft* + buildcraft）不被额外 id 污染）
    for path, members in sorted(VENDOR_TAGS.items()):
        write_json(os.path.join(dst, "c", "tags", "item", f"{path}.json"), {"values": members})
        counts["tag:c:vendored"] += 1

    total_recipes = sum(v for k, v in counts.items() if k.startswith("ns:"))
    print("M2.10 转换完成：")
    print(f"  recipes 共 {total_recipes} 个（目标基线 1561）")
    for key, value in sorted(counts.items()):
        if not key.startswith("ns:"):
            print(f"    {key}: {value}")
    for ns in MODULE_NS:
        print(f"    ns:{ns}: {counts[f'ns:{ns}']}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
