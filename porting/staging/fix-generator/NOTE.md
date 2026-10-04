# fix-generator：生成器/验证器与主树对齐（biome_modifier 路径 + HolderSet 语法）

背景：diag-spawn 批修好了主树 14 个 biome_modifier JSON（路径补 `neoforge/` 层 + `biomes`
改 HolderSet 对象形式），但 `porting/tools/build_phase4_data.py` 仍是旧逻辑 —— 重跑一次就会
把主树打回原形（旧路径 + 字符串数组）。本批把**单一事实源**修对。

## 改动清单

### 1. `porting/tools/build_phase4_data.py`

| 位置 | 改动 |
|---|---|
| 模块 docstring | 写明路径必须带 `neoforge/` 层、`biomes` 必须 HolderSet 对象；补 `--out` / `--verify` 用法 |
| `OUT_DIR`（:35） | `staging/data/thkaguyamod/biome_modifier` → `staging/data/thkaguyamod/**neoforge**/biome_modifier` |
| 新增 `MAIN_DIR`（:38） | 指向主树 `porting/neoforge-1.21.1/src/main/resources/data/thkaguyamod/neoforge/biome_modifier`（`--verify` 基准） |
| `write_json` | 改为 **bytes 写入 + 显式 LF**。原来用 `open(..., "w", newline="\n")`，在 Windows 上 text 模式仍可能落成 CRLF，会让「重跑 → 全树 diff」非空 |
| 新增 `write_all(plan, out_dir)` | 抽出写盘逻辑；`biomes` 生成 `{"type":"neoforge:or","values":[...]}`（原来是裸字符串数组，HolderSetCodec 不接受） |
| 新增 `verify_against_main(plan)` | 生成到 `tempfile.TemporaryDirectory()`，与 `MAIN_DIR` **逐字节**比对，返回差异列表 |
| `main()` | 新增 `--out DIR`（临时目录验证）与 `--verify`（只比对不写盘，不一致时 exit 1） |

### 2. `porting/tools/validate_phase4_data.py`

| 位置 | 改动 |
|---|---|
| 新增 `BM_DIR` 常量 | `thkaguyamod/neoforge/biome_modifier`（旧硬编码路径已改） |
| §2 新增路径闸门 | 扫描旧路径 `thkaguyamod/biome_modifier/*.json`，发现即 `FAIL`（原来是静默通过） |
| §2 `biomes` 校验 | 由「非空字符串数组」改为「**必须是 dict 且含 `type` + 非空 `values` 列表**」，错误信息直接点明 HolderSetCodec |
| §5 | `docs[p]['biomes']` → `docs[p]['biomes']['values']` |
| §6 新增 | 调用 `build_phase4_data.py --verify` 做「生成器 ↔ 主树逐字节一致」自检，纳入 `RESULT` |

### 3. `porting/tools/p19_strip_spawn_rules.py`

**无需改动** —— grep `biome` 零命中，它只处理 `entity/living/*.java` 的 `checkSpawnRules` 剥离，
与 biome_modifier 路径无关。其余工具（`build_phase3_assets.py` / `check_lang.py` /
`rebind_weapons.py` / `*.sh`）同样零命中。

## 验证结果

### A. 逐字节对齐（任务要求 1）

```
$ python porting/tools/build_phase4_data.py --verify
VERIFY OK：14 个文件与 ...\data\thkaguyamod\neoforge\biome_modifier 逐字节一致

$ python porting/tools/build_phase4_data.py --out /tmp/genout && diff -r /tmp/genout <主树目录>
DIFF EMPTY (out == main)
```

### B. 验证器（任务要求 2）

```
$ python porting/tools/validate_phase4_data.py
=== 2) biome_modifier structure ===
  OK   路径带 neoforge/ 层，未发现旧路径残留
  cirno.json ... wriggle.json   （14 个全部 OK）
=== 5) biome tag/id resolution ===  unresolved: none
=== 6) 生成器 ↔ 主树 逐字节一致性 ===  VERIFY OK：14 个文件 ...
RESULT: ALL CHECKS PASS
```

### C. 变异验证（3 改 3 红）

| 变异 | 期望 | 实测 |
|---|---|---|
| 在 `staging/data/thkaguyamod/biome_modifier/` 放一个旧路径副本 | 验证器 §2 报错 | `FAIL 检测到旧路径 ...（缺 neoforge/ 层）` → `RESULT: PROBLEMS FOUND` ✓ |
| 把主树 `th_fairy.json` 的 `biomes` 退回字符串数组 | `--verify` 失败 | `VERIFY FAIL / th_fairy.json: 内容不一致（生成 825 字节 / 主树 756 字节）` ✓ |
| 把主树 `th_fairy.json` 转成 CRLF | `--verify` 失败 | `VERIFY FAIL / 内容不一致（生成 825 字节 / 主树 850 字节）` ✓ |

三次变异后均已还原，最终 `--verify` 与 `validate` 双双 PASS。

### D. 运行时回归（本批未改主树数据，属确认性运行）

`./gradlew runGameTestServer` → `105 GAME TESTS COMPLETE`；唯一失败是
`directdanmakupatternproducesmovingshot`（agent-1 在途的 `FairySummonFireTest`，报
"弹速链路疑被归零"），与本批无关；`BiomeSpawnGateTest` 未出现在失败列表 = 绿。

## 遗留

- 本批**只改工具代码**，未重跑生成器覆盖主树（主树 14 个文件已由 diag-spawn 批修好且与生成器一致）。
- 顺带把主树 14 个文件的换行统一为 LF（此前是 CRLF，与仓库内 recipe/loot JSON 的 LF 不一致）；
  该改动是 `--verify` 逐字节一致的前提。
- `validate_phase4_data.py` 的 §6 会 spawn 一个 python 子进程跑生成器自检；若日后要在无 python
  环境跑验证器，需要给该段加 `--skip-generator-check` 开关（当前未加，因为工具链本来就要求 python）。
