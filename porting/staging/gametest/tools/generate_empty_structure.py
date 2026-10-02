#!/usr/bin/env python3
"""生成 GameTest 用的空结构模板 ``empty.nbt``（7×7×7，全空气）。

为什么要自己生成
----------------
GameTest 必须先加载一个结构模板（``@GameTest(template = ...)`` 或 TestData#structure）。
原版只在 ``runGameTestServer`` 的临时世界里跑自带结构，成品 jar 里<b>没有</b>可供 mod
复用的空模板；NeoForge 也不提供。所以本仓库自带一个：``thkaguyamod:empty``。

格式
----
1.21.1 的结构管理器（``StructureTemplateManager.loadFromResource`` →
``StructureTemplate.readStructure``）按<b>未压缩布局的 NBT（gzip）</b>读取：
``size``(INT 列表) / ``palette``(COMPOUND 列表) / ``blocks``(COMPOUND 列表) / ``entities``。
注意 ``blocks`` 里不要放任何条目 —— ``StructureTemplate.placeInWorld`` 对空 blocks 会直接
no-op，但 ``loadStructureInfo`` 仍会从 ``size`` 设好结构尺寸，正是我们要的「空房间」。

用法
----
    python porting/staging/gametest/tools/generate_empty_structure.py

默认输出到 ``porting/staging/gametest/resources/data/thkaguyamod/structure/empty.nbt``；
合并到主树时把它拷到 ``src/main/resources/data/thkaguyamod/structure/empty.nbt``。
"""

from __future__ import annotations

import argparse
import gzip
import struct
from pathlib import Path

# NBT tag ids (https://minecraft.wiki/w/NBT_format)
TAG_END = 0
TAG_INT = 3
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11

# 与 SharedConstants.WORLD_VERSION 一致的 1.21.1 数据版本；写进去避免结构 datafixer 跑旧修复。
DATA_VERSION_1_21_1 = 3955

DEFAULT_SIZE = (7, 7, 7)


def _name(text: str) -> bytes:
    raw = text.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def _int(value: int) -> bytes:
    return struct.pack(">i", value)


def empty_structure_nbt(size: tuple[int, int, int] = DEFAULT_SIZE) -> bytes:
    """返回一个空结构模板的未压缩 NBT 字节串。"""
    out = bytearray()

    out += bytes([TAG_COMPOUND]) + _name("")
    out += bytes([TAG_INT]) + _name("DataVersion") + _int(DATA_VERSION_1_21_1)

    # size: INT 列表，三个元素
    out += bytes([TAG_LIST]) + _name("size")
    out += bytes([TAG_INT]) + _int(3)
    for value in size:
        out += _int(value)

    # palette: 只有一个 air 条目，保证 palette 非空（placeInWorld 的空判断看的是 palette）
    out += bytes([TAG_LIST]) + _name("palette") + bytes([TAG_COMPOUND]) + _int(1)
    out += bytes([TAG_STRING]) + _name("Name") + _name("minecraft:air")
    out += bytes([TAG_END])  # palette 列表里这个 compound 的结束标记

    # blocks: 空列表 —— 结构保持全空气
    out += bytes([TAG_LIST]) + _name("blocks") + bytes([TAG_COMPOUND]) + _int(0)

    # entities: 空列表
    out += bytes([TAG_LIST]) + _name("entities") + bytes([TAG_COMPOUND]) + _int(0)

    out += bytes([TAG_END])  # 根 compound 结束
    return bytes(out)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--out",
        type=Path,
        default=Path(__file__).resolve().parent.parent
        / "resources"
        / "data"
        / "thkaguyamod"
        / "structure"
        / "empty.nbt",
        help="输出 .nbt 路径（默认 staging 下的资源目录）",
    )
    parser.add_argument("--size", default="7,7,7", help="结构尺寸，默认 7,7,7")
    args = parser.parse_args()

    size = tuple(int(part) for part in args.size.split(","))
    if len(size) != 3 or min(size) < 3:
        raise SystemExit("--size 需要三个 >=3 的整数，例如 7,7,7")

    payload = gzip.compress(empty_structure_nbt(size), mtime=0)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_bytes(payload)
    print(f"wrote {args.out} ({len(payload)} bytes, size={size})")


if __name__ == "__main__":
    main()