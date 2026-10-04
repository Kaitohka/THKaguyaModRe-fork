#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""阶段 4 数据批次：自然生成（BiomeModifier）JSON 生成器。

单一事实源 = 1.7.10 源码 `src/main/java/thKaguyaMod/CommonProxy.java` 的
`registerEntitySpawn()`。本脚本解析其中未被注释的 `EntityRegistry.addSpawn(...)`
调用，按下面的映射表把 1.7.10 `BiomeGenBase` 静态群系折叠为 1.21.1 群系 tag，
产出 `porting/staging/data/thkaguyamod/neoforge/biome_modifier/*.json`。

**路径必须带 `neoforge/` 一层**：NeoForge 的 biome modifier 是数据包注册表，
目录名由 `CommonHooks.prefixNamespace("neoforge:biome_modifier")` 推出
（见 `RegistryDataLoader#loadContentsFromManager`），缺这层时注册表会静默为空
（不报错、不告警），自然生成直接失效。**`biomes` 必须是 HolderSet 对象形式**：
`HolderSetCodec` 只接受「单个 id/tag 字符串 / 元素数组 / ICustomHolderSet 对象」，
`["#tagA", "#tagB"]` 这种字符串数组会解析失败（服务端启动即抛异常）。

用法（仓库根目录）：
    python porting/tools/build_phase4_data.py            # 生成 JSON 到 staging
    python porting/tools/build_phase4_data.py --check    # 只打印对照表，不写文件
    python porting/tools/build_phase4_data.py --out DIR  # 生成到指定目录（临时目录验证用）
    python porting/tools/build_phase4_data.py --verify   # 生成到临时目录并与主树逐字节比对
"""

import argparse
import json
import os
import re
import sys
import tempfile
from collections import OrderedDict

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
COMMON_PROXY = os.path.join(ROOT, "src", "main", "java", "thKaguyaMod", "CommonProxy.java")
# 交付路径：staging 暂存区（与主树 porting/neoforge-1.21.1/src/main/resources/data 同结构）。
OUT_DIR = os.path.join(ROOT, "porting", "staging", "data", "thkaguyamod", "neoforge", "biome_modifier")
# 主树现行文件（--verify 的比对基准）。
MAIN_DIR = os.path.join(ROOT, "porting", "neoforge-1.21.1", "src", "main", "resources", "data",
                        "thkaguyamod", "neoforge", "biome_modifier")

MODID = "thkaguyamod"

# 1.7.10 实体类 → 1.21.1 注册名（阶段 4 实体批的命名约定，见报告）
ENTITY_IDS = OrderedDict([
    ("EntityTHFairy", "th_fairy"),
    ("EntitySunFlowerFairy", "sunflower_fairy"),
    ("EntityTHPhantom", "th_phantom"),
    ("EntityDanmakuCreeper", "danmaku_creeper"),
    ("EntityCirno", "cirno"),
    ("EntityRumia", "rumia"),
    ("EntityToziko", "toziko"),
    ("EntityWriggle", "wriggle"),
    ("EntitySanae", "sanae"),
    ("EntitySakuya", "sakuya"),
    ("EntityRinnosuke", "rinnosuke"),
])

# 1.7.10 BiomeGenBase 静态字段 → 1.21.1 群系 tag / 群系 id
# 说明：1.18（21w40a）删除了全部“高度变体”子群系（*Hills / *Edge / megaTaiga 等），
# 因此 xxxHills 一律折叠回其母群系所属 tag。
BIOME_MAP = {
    "plains":           ["#c:is_plains"],
    "birchForest":      ["#minecraft:is_forest"],
    "birchForestHills": ["#minecraft:is_forest"],
    "desert":           ["#c:is_desert"],
    "desertHills":      ["#c:is_desert"],
    "extremeHills":     ["#minecraft:is_hill", "#minecraft:is_mountain"],
    "extremeHillsEdge": ["#minecraft:is_hill", "#minecraft:is_mountain"],
    "extremeHillsPlus": ["#minecraft:is_hill", "#minecraft:is_mountain"],
    "forest":           ["#minecraft:is_forest"],
    "forestHills":      ["#minecraft:is_forest"],
    "taiga":            ["#minecraft:is_taiga"],
    "taigaHills":       ["#minecraft:is_taiga"],
    "swampland":        ["#c:is_swamp"],
    "icePlains":        ["#c:is_snowy_plains"],
    # Ice Mountains = snowy_plains 的 hills 变体，21w40a 合并进 snowy_plains。
    # 用 c:is_snowy 保留“雪原/雪山”语义，避免与 icePlains 完全重复。
    "iceMountains":     ["#c:is_snowy"],
    "jungle":           ["#minecraft:is_jungle"],
    "jungleHills":      ["#minecraft:is_jungle"],
    "jungleEdge":       ["#minecraft:is_jungle"],
    "roofedForest":     ["#minecraft:is_forest"],
    "beach":            ["#minecraft:is_beach"],
    "coldBeach":        ["#c:is_snowy"],
    "coldTaiga":        ["#minecraft:is_taiga"],
    "coldTaigaHills":   ["#minecraft:is_taiga"],
    "deepOcean":        ["#minecraft:is_deep_ocean"],
    "frozenOcean":      ["#minecraft:is_ocean"],
    "frozenRiver":      ["#minecraft:is_river"],
    "megaTaiga":        ["#minecraft:is_taiga"],
    "megaTaigaHills":   ["#minecraft:is_taiga"],
    "mesa":             ["#minecraft:is_badlands"],
    "mesaPlateau":      ["#minecraft:is_badlands"],
    "mesaPlateau_F":    ["#minecraft:is_badlands"],
    "ocean":            ["#minecraft:is_ocean"],
    "river":            ["#minecraft:is_river"],
    "savanna":          ["#minecraft:is_savanna"],
    "savannaPlateau":   ["#minecraft:is_savanna"],
    # 1.21.1 没有“stony shore”群系 tag，直接写群系 id。
    "stoneBeach":       ["minecraft:stony_shore"],
    # 旧代码里被注释掉的 hell（下界）不参与映射。
}

ADD_SPAWN_RE = re.compile(
    r"^\s*(?P<commented>//)?\s*EntityRegistry\.addSpawn\(\s*"
    r"(?P<cls>\w+)\.class\s*,\s*"
    r"(?P<weight>\d+)\s*,\s*(?P<min>\d+)\s*,\s*(?P<max>\d+)\s*,\s*"
    r"EnumCreatureType\.(?P<category>\w+)\s*,\s*"
    r"BiomeGenBase\.(?P<biome>\w+)\s*\)"
)


def parse_spawns(path):
    """返回 [(entity_class, weight, min, max, category, biome_field)]，跳过注释行。"""
    spawns = []
    skipped = []
    with open(path, "r", encoding="utf-8") as fh:
        for lineno, line in enumerate(fh, 1):
            m = ADD_SPAWN_RE.match(line)
            if not m:
                continue
            if m.group("commented"):
                skipped.append((lineno, m.group("cls"), m.group("biome")))
                continue
            spawns.append((
                m.group("cls"),
                int(m.group("weight")),
                # 1.21.1 的 SpawnerData.minCount 是 ExtraCodecs.POSITIVE_INT（>=1），
                # 而旧代码里 sanae/sakuya/rinnosuke 是 0。JSON 里只能写 1，
                # 与 maxCount 组合后与原 0~1 语义等价（原本受 98% 早退，实际也只会出 1 只）。
                max(1, int(m.group("min"))),
                int(m.group("max")),
                m.group("category"),
                m.group("biome"),
            ))
    return spawns, skipped


def group(spawns):
    """按 (实体, 权重, min, max) 聚合，保留源码出现顺序。

    1.7.10 对同一实体可以有不同权重的 addSpawn（如 EntityRumia 在 roofedForest 权重 7、
    其余群系权重 4）。一个 biome_modifier 文件的 biomes 只有一组，因此这种情形拆成多个文件。
    返回 [(cls, weight, min, max, category, [biome_field, ...]), ...]
    """
    order = []
    buckets = {}
    for cls, weight, mn, mx, category, biome in spawns:
        if cls not in ENTITY_IDS:
            raise SystemExit("未知实体类 %s：请先补 ENTITY_IDS" % cls)
        key = (cls, weight, mn, mx, category)
        if key not in buckets:
            buckets[key] = []
            order.append(key)
        if biome not in buckets[key]:
            buckets[key].append(biome)
    return [(k[0], k[1], k[2], k[3], k[4], buckets[k]) for k in order]


def to_tags(biomes):
    tags = []
    for biome in biomes:
        if biome not in BIOME_MAP:
            raise SystemExit("BIOME_MAP 缺少 1.7.10 群系字段 %s" % biome)
        for tag in BIOME_MAP[biome]:
            if tag not in tags:
                tags.append(tag)
    return tags


def build():
    spawns, skipped = parse_spawns(COMMON_PROXY)
    groups = group(spawns)

    plan = []
    used = {}
    for cls, weight, mn, mx, category, old_biomes in groups:
        entity_id = ENTITY_IDS[cls]
        used[entity_id] = used.get(entity_id, 0) + 1
        name = entity_id if used[entity_id] == 1 else "%s_%d" % (entity_id, used[entity_id])
        plan.append({
            "name": name,
            "entity": entity_id,
            "old_class": cls,
            "category": category,
            "weight": weight,
            "min": mn,
            "max": mx,
            "old_biomes": old_biomes,
            "biomes": to_tags(old_biomes),
        })
    return plan, skipped


def write_json(path, payload):
    """写出 JSON。

    换行必须固定为 LF：主树里 14 个文件是 LF + 末尾换行，且 `json.dump` 在
    Windows 上用 text 模式写 `\\n` 会被翻译成 CRLF，导致「生成器重跑 → 全树 diff 非空」。
    这里显式用 newline="\\n" + bytes 写入，保证跨平台逐字节一致。
    """
    os.makedirs(os.path.dirname(path), exist_ok=True)
    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    with open(path, "wb") as fh:
        fh.write(text.encode("utf-8"))


def write_all(plan, out_dir):
    written = []
    for p in plan:
        payload = OrderedDict()
        payload["type"] = "neoforge:add_spawns"
        # HolderSet 对象形式：字符串数组（["#tagA", "#tagB"]）不被 HolderSetCodec 接受。
        payload["biomes"] = OrderedDict([
            ("type", "neoforge:or"),
            ("values", p["biomes"]),
        ])
        payload["spawners"] = OrderedDict([
            ("type", "%s:%s" % (MODID, p["entity"])),
            ("weight", p["weight"]),
            ("minCount", p["min"]),
            ("maxCount", p["max"]),
        ])
        # 群系与参数的来源可追溯：1.7.10 的 addSpawn 调用列表（行号 + 类 + 群系字段）。
        payload["_comment"] = (
            "1.7.10 CommonProxy.registerEntitySpawn(): %s, weight=%d, min=%d, max=%d, "
            "EnumCreatureType=%s, BiomeGenBase=[%s]"
            % (p["old_class"], p["weight"], p["min"], p["max"], p["category"],
               ", ".join(p["old_biomes"]))
        )
        path = os.path.join(out_dir, p["name"] + ".json")
        write_json(path, payload)
        written.append(path)
    return written


def verify_against_main(plan):
    """生成到临时目录，并与主树 `data/thkaguyamod/neoforge/biome_modifier/` 逐字节比对。

    这是「生成器是否仍是单一事实源」的自检：任何路径、语法、缩进、换行、
    字段顺序的偏差都会让本函数返回非空差异列表。
    """
    diffs = []
    with tempfile.TemporaryDirectory() as tmp:
        write_all(plan, tmp)
        expected = sorted(p["name"] + ".json" for p in plan)
        actual = sorted(os.listdir(MAIN_DIR)) if os.path.isdir(MAIN_DIR) else []
        for name in sorted(set(expected) | set(actual)):
            tmp_path = os.path.join(tmp, name)
            main_path = os.path.join(MAIN_DIR, name)
            if not os.path.exists(tmp_path):
                diffs.append("%s: 主树多出（生成器不再产出）" % name)
                continue
            if not os.path.exists(main_path):
                diffs.append("%s: 主树缺失（生成器有产出）" % name)
                continue
            gen = open(tmp_path, "rb").read()
            cur = open(main_path, "rb").read()
            if gen != cur:
                diffs.append("%s: 内容不一致（生成 %d 字节 / 主树 %d 字节）" % (name, len(gen), len(cur)))
    return diffs


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true", help="只打印对照表")
    ap.add_argument("--out", default=None, help="输出目录（默认 staging/data/thkaguyamod/neoforge/biome_modifier）")
    ap.add_argument("--verify", action="store_true",
                    help="生成到临时目录并与主树逐字节比对（不写任何文件）")
    args = ap.parse_args()

    plan, skipped = build()

    print("%-18s %-24s %-6s %-5s %-5s %s" % ("modifier", "1.7.10 class", "w", "min", "max", "modern biomes"))
    for p in plan:
        print("%-18s %-24s %-6d %-5d %-5d %s" % (
            p["name"], p["old_class"], p["weight"], p["min"], p["max"], " ".join(p["biomes"])))
    print()
    print("跳过（源码中被注释的 addSpawn 行）：")
    for lineno, cls, biome in skipped:
        print("  CommonProxy.java:%d  %s / %s" % (lineno, cls, biome))

    if args.check:
        return 0

    if args.verify:
        diffs = verify_against_main(plan)
        if diffs:
            print("\nVERIFY FAIL（生成器产物与主树不一致）：")
            for d in diffs:
                print("  " + d)
            return 1
        print("\nVERIFY OK：%d 个文件与 %s 逐字节一致" % (len(plan), MAIN_DIR))
        return 0

    out_dir = args.out or OUT_DIR
    write_all(plan, out_dir)
    print("\n写出 %d 个文件到 %s" % (len(plan), out_dir))
    return 0


if __name__ == "__main__":
    sys.exit(main())
