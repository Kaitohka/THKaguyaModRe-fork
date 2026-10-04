# diag-spawn：妖精が自然生成されない 根因診断（**只读**，不改主树）

基线：git ccd558b。冲突警戒：`entity/living/EntityTHFairy.java` / `EntityDanmakuMob.java` 由 agent-1 在改，本批**只读**。
现象：`/summon thkaguyamod:th_fairy` 正常（I11 已修），但野外无自然生成的妖精。
配置：`Spawn Fairy=true`、`Fairy Spawn Rate(%)=50`（语义反向：内部 `100 - 数值` 作拒绝概率）。

## 排查链（逐环取证，随查随写）

| 环 | 检查项 | 文件:行 | 结论 | 证据 |
|---|---|---|---|---|
| 1 | SpawnPlacements 注册 | `init/ModSpawns.java:83-121`；主类 `THKaguyaMod.java:101` `ModSpawns.register(modEventBus)` | **通** | 11 个实体全部 `event.register(..., ON_GROUND, MOTION_BLOCKING_NO_LEAVES, 谓词, REPLACE)`；NeoForge 要求新注册必须有非空 placement+heightmap，已满足（`RegisterSpawnPlacementsEvent.register` 否则抛 NPE）。`Operation.REPLACE` 对首次注册等价于 put。 |
| 2 | 群系修饰器 JSON 路径 | `src/main/resources/data/thkaguyamod/biome_modifier/*.json`（14 个） | **断（头号嫌疑）** | NeoForge 的 biome modifier 是**数据包注册表**，其目录由 `RegistryDataLoader` 用 `Registries.elementsDirPath()` 推出：`CommonHooks.prefixNamespace("neoforge:biome_modifier")` = **`neoforge/biome_modifier`**（`CommonHooks.java:1340-1342`），再按**命名空间**拼接 → 正确路径是 **`data/thkaguyamod/neoforge/biome_modifier/*.json`**。现有文件少了 `neoforge/` 一层。对照实机同目录的 Mekanism（`data/mekanism/neoforge/biome_modifier/`）、environmentaltech 均带该层；全盘扫描 mods/ 目录**无任何一个 mod** 用 `data/<ns>/biome_modifier/` 这种浅路径。 |
| 2b | JSON 内容语法 | 同上 | **通** | `SpawnerData.CODEC` 要求 `type/weight/minCount/maxCount`（`MobSpawnSettings.java:137-147`）——全部命中；`biomes` 用 `Biome.LIST_CODEC`，接受 tag 列表；`type: neoforge:add_spawns` 与 `NeoForgeMod.java:251` 注册的 `add_spawns` 一致。`#c:is_*` / `#minecraft:is_*` tag 在 `data/c/tags/worldgen/biome/` 与 `data/minecraft/tags/worldgen/biome/` 均存在（已抽查 is_plains/is_desert/is_swamp/is_snowy/is_snowy_plains）。`_comment` 字段被 RecordCodecBuilder 忽略，无害。 |
| 2c | 是否被加载 / 有无报错 | `run-smoke/logs/latest.log` | **无报错（正是问题所在）** | 路径不匹配 → `listResources("neoforge/biome_modifier", …)` 返回**空表**，`RegistryDataLoader.loadContentsFromManager` 只是"没有元素可读"，**不会报错、不会告警**。所以 smoke 的"0 recipe/model/spawn 错"完全掩盖了这一层缺失。 |
| 3 | 生成谓词（亮度方向/地表/水） | `ModSpawns.java:139-159`（fairySpawnRules）vs 1.7.10 `EntityTHFairy.getCanSpawnHere`（`:375-411`） | **通** | 逐条对照：概率 `nextInt(100) < fairySpawnRate` ✓；`y<64 && y<nextInt(64)` ✓；**亮度方向正确**：`getBrightness(SKY,pos) - getSkyDarken() <= 7 → false`（要求地表白天）✓ 与旧 `getSavedLightValue(Sky) - skylightSubtracted <= 7` 同向；脚下 `grass/dirt/sand` ✓。无写反。 |
| 4 | 双采样残留 | `EntityTHFairy.java:379-384`、`EntityDanmakuMob.java:1858-1861` | **通** | 实体侧 `checkSpawnRules` 直接 `return true;`，与 P19 裁决一致（条件集中在 ModSpawns）。 |
| 5 | 生成类别与上限 | `ModEntities.java:221-275` | **通** | 妖精/花妖精/幽灵/琪露诺/露米娅/屠自古/莉格露/花火 = `MONSTER`，早苗/咲夜/霖之助 = `AMBIENT`，与 1.7.10 `EnumCreatureType` 对齐；无 `noSummon()`；`MobCategory.MONSTER` 的 charge 未设（= 无额外 spawn cost 限制）。 |

## 实测取证（临时诊断用例，已删除）

在 GameTest 里打印「结构所在群系（plains）的模组 spawn 列表」+「资源管理器能否找到 JSON」：

```
（修复前）
DIAGBIOME biome=minecraft:plains modSpawns=[]                       ← 群系里没有任何模组 spawn
DIAGBIOME hasPlacement(th_fairy)=true                               ← 环 1 正常
DIAGPATH neoforge/biome_modifier -> []                              ← 期望路径下 0 个文件
DIAGPATH biome_modifier -> [thkaguyamod:biome_modifier/*.json ×14]  ← 文件实际在这里（少了 neoforge/ 一层）
```

**验证性修复实验**（只改 `build/resources/main`，不动 `src/`）：把 14 个 JSON 复制到
`data/thkaguyamod/neoforge/biome_modifier/` → 立刻暴露出**第二个断点**：

```
Registry loading errors:
>> Errors in element thkaguyamod:cirno:
Caused by: IllegalStateException: Not a JSON object: "#c:is_snowy_plains"; ...
（14 个文件全部解析失败 → 注册表加载直接抛异常）
```

把 `biomes` 从「字符串数组」改成 NeoForge 自定义 HolderSet 或单个 tag 字符串后：

```
DIAGBIOME biome=minecraft:plains modSpawns=[MONSTER:th_fairy(w=30) MONSTER:th_phantom(w=20)
  MONSTER:sunflower_fairy(w=3) MONSTER:rumia(w=4) MONSTER:toziko(w=7)
  AMBIENT:rinnosuke(w=1) AMBIENT:sakuya(w=1) AMBIENT:sanae(w=1)]   ← 全部生效
```

四种写法的实测结果：

| `biomes` 写法 | 结果 |
|---|---|
| `["#tagA", "#tagB", ...]`（**当前仓库的写法**） | ✗ `Not a JSON object: "#tagA"` |
| `"#tag"`（单个字符串） | ✓ |
| `["minecraft:plains", "minecraft:forest"]`（具体群系 id 数组） | ✓ |
| `{"type":"neoforge:or","values":["#tagA","#tagB"]}` | ✓ |

原因：`Biome.LIST_CODEC` 走 `HolderSetCodec`，它只接受 **字符串（单个 id/tag）**、
**元素数组**、或 **`ICustomHolderSet` 对象**；**字符串数组**不在接受范围内。

---

## 结论

**断点环：2（群系修饰器 JSON）** —— 两处叠加，缺一不可：

1. **路径错**：`data/thkaguyamod/biome_modifier/*.json` 少了 NeoForge 要求的一层 `neoforge/`
   （正确：`data/thkaguyamod/neoforge/biome_modifier/*.json`）。
   证据：`CommonHooks.prefixNamespace`（`CommonHooks.java:1340-1342`）→ `RegistryDataLoader.loadContentsFromManager`（`:227`）
   → 运行时实测 `listResources("neoforge/biome_modifier")` 返回空、`listResources("biome_modifier")` 返回 14 个文件。
   对照：同机 Mekanism / environmentaltech 都用 `data/<ns>/neoforge/biome_modifier/`；全盘扫描 mods/ 无第二家用浅路径。
2. **`biomes` 语法错**：字符串数组 `["#tagA","#tagB"]` 不被 `HolderSetCodec` 接受（只接受 单字符串 / 元素数组 /
   `ICustomHolderSet` 对象）。实测报错 `Not a JSON object: "#c:is_snowy_plains"`，14 个文件全部解析失败。

**根因链**：路径错 → 注册表为空（静默、无日志）→ 群系 spawn 列表里没有任何模组条目 →
`NaturalSpawner` 永远抽不到妖精；`/summon` 不受影响（不经 biome_modifier），与实测现象完全吻合。
语法错被路径错掩盖：**即使把文件挪到正确路径，若不改 `biomes` 写法，服务端会直接启动失败**（更严重）。

**建议修法**（K3 执行，本批未改主树）：

| # | 改动 | 位置 |
|---|---|---|
| 1 | 14 个 JSON 从 `src/main/resources/data/thkaguyamod/biome_modifier/` **移动到** `src/main/resources/data/thkaguyamod/neoforge/biome_modifier/` | `porting/neoforge-1.21.1/src/main/resources/data/thkaguyamod/` |
| 2 | 每个文件的 `biomes` 改为 `{"type":"neoforge:or","values":[ ...原数组... ]}` | 同上 14 个文件 |
| 3 | 同步 `porting/staging/data/thkaguyamod/biome_modifier/` 的暂存副本（同两处改动） | `porting/staging/data/` |

**修正后的 14 个 JSON 草稿已放在 `porting/staging/diag-spawn/fix/data/thkaguyamod/neoforge/biome_modifier/`**（整目录可直接覆盖）。

## 建议加一道闸门（防同类回归）

路径类错误是**静默**的：注册表为空不会产生任何 WARN/ERROR，smoke 的"0 错"闸门抓不到。
建议在 `RegistryCompletenessTest` 里加一条断言（本批未改主树）：

```java
// 自然生成名单必须非空（biome_modifier 路径/语法错的唯一低成本探针）
var plains = level.registryAccess().registryOrThrow(Registries.BIOME)
        .getHolderOrThrow(Biomes.PLAINS).value().getMobSettings();
boolean any = java.util.stream.Stream.of(MobCategory.values())
        .flatMap(c -> plains.getMobs(c).unwrap().stream())
        .anyMatch(d -> d.type.toString().startsWith("thkaguyamod:"));
assertTrue(any, "biome_modifier 未生效：平原群系的 spawn 列表里没有模组实体");
```

## 遗留 / 注意

- **本批为只读诊断**：`src/` 未改动；验证实验只写 `build/resources/main/`（已删除），临时诊断用例已删除。
- 修好之后妖精的实际出现率仍受三重随机压制，属正常：`weight 30`（同群系还有原版怪竞争）→
  `fairySpawnRate=50` 拒一半 → 地表白天光照 + 草/土/沙脚下。1.7.10 同样如此。
- `danmaku_creeper.json` 的 `Spawn Hanabeeper` 默认 false（配置门控在谓词里），修好路径后仍需开配置才生成，属预期。
- `rumia_2.json`（roofedForest 权重 7）与 `rumia.json` 现在会**叠加**（同一实体两条 spawn 条目）——
  与 1.7.10 `addSpawn` 两次调用的效果一致，属预期。
