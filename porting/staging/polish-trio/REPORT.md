# polish-trio 批次报告（D4 / P10 / P15 实施预备）

- 批次：`polish-trio`（阶段 5 发布前夜的三个"阶段5转"润色项）
- 产出目录：`porting/staging/polish-trio/`（`d4/` `p10/` `p15/`，各含 `PREP.md`）
- 交付性质：**设计 + staging 补丁草稿 + 合并点说明**（未触碰任何主树文件）
- 对照只读参考：`src/main/java/thKaguyaMod/`（1.7.10 原作）
- API 事实来源：`porting/neoforge-1.21.1/build/moddev/artifacts/neoforge-21.1.252-sources.jar`（本机反编译源）+ 原版资源 jar + 主树现状 grep

---

## 1. 变更清单

### D4 悔悟之棒组件化（`d4/`）

| 文件 | 行数 | 动作 | 一句话 |
|---|---|---|---|
| `d4/src/main/java/thkaguyamod/init/ModDataComponents.java` | 74 | 整文件草稿（主树同文件 +11 行） | 追加 `ROD_OF_REMORSE_USED`（`Codec.BOOL` + `ByteBufCodecs.BOOL`）布尔组件，取代 durability 占位 |
| `d4/src/main/java/thkaguyamod/item/ItemKaigo.java` | 103 | 整文件替换 | 4 处 damage 读写改组件读写；删掉类头 blocking TODO；`isFoil`/`use`/`hurtEnemy` 语义与 1.7.10 meta 0/1 一一对应 |
| `d4/src/main/java/thkaguyamod/client/RodOfRemorseClientProperties.java` | 54 | 新增 | 注册 predicate `thkaguyamod:remorse_used`（0.0/1.0），`FMLClientSetupEvent` 接线 |
| `d4/.../models/item/rod_of_remorse.json` | 14 | 替换 | 保留 `parent: item/handheld` + `layer0`，追加 1 条 override（`remorse_used >= 1` → `rod_of_remorse_used`） |
| `d4/.../models/item/rod_of_remorse_used.json` | 6 | 新增 | 已使用态变体模型（`layer0 = item/rod_of_remorse_used`） |
| `d4/.../textures/item/rod_of_remorse_used.png` | 32×32 | 新增（**复用原作**） | 复制自 `porting/assets-1.7.10/.../items/kaigoStick_1.png`（md5 `91a453ea…`，改名合规） |

### P10 御柱挖掘速度（`p10/`）

| 文件 | 行数 | 动作 | 一句话 |
|---|---|---|---|
| `p10/src/main/java/thkaguyamod/event/OnbashiraDigSpeedHandler.java` | 74 | 新增 | `PlayerEvent.BreakSpeed` 中对手持/副手御柱的玩家 `setNewSpeed(3.0F)`（1.7.10 `getDigSpeed` 恒 3.0F 的字面移植） |
| `p10/src/main/java/thkaguyamod/item/ItemOnbashira.java` | 35 | 仅 javadoc 替换 | 把"未实现"TODO 换成指向事件处理器；方法体零改动 |

### P15 玉枝 6 段图标 + 折伞双层染色（`p15/`）

| 文件 | 行数 | 动作 | 一句话 |
|---|---|---|---|
| `p15/src/main/java/thkaguyamod/client/HouraiJeweledBranchClientProperties.java` | 66 | 新增 | predicate `thkaguyamod:hourai_charge_stage`（段阶 0..5 → 0.0..1.0），读 `CHARGE` 组件，6 段 |
| `p15/src/main/java/thkaguyamod/client/GapFoldingUmbrellaItemColor.java` | 84 | 新增 | `ItemColor`：tintIndex 0 → `-1`，tintIndex 1 → `GAP_COLOR` 调色板（去渲染期 NBT 写入，修 alpha 语义） |
| `p15/.../models/item/jeweled_branch_of_hourai.json` | 13 | 替换 | 保留 `parent: item/generated` + `layer0`，追加 5 条 overrides（0.2/0.4/0.6/0.8/1.0，低→高排列） |
| `p15/.../models/item/jeweled_branch_of_hourai_{0..4}.json` | 6×5 | 新增 | 5 个变体模型 |
| `p15/.../textures/item/jeweled_branch_of_hourai_{0..4}.png` | 32×32 | 新增（**复用原作**） | 复制自 `houraiEda_0..4.png`（md5 逐一核对一致，改名合规） |

**主树本次零改动**；合计 staging 25 个文件（Java 6 / 资源 13 / PREP 3 / 本报告 1 + 2 目录说明）。

---

## 2. 验证方式与结果

| # | 方式 | 命令/方法 | 结果 |
|---|---|---|---|
| V1 | **全树编译（隔离沙盒）** | 把 `neoforge-1.21.1/src/main/java`（253 类）overlay 上 `d4/`+`p10/`+`p15/` 的 6 个草稿类 → `javac -nowarn -proc:none -encoding UTF-8 -cp <merged.jar;legacy>` 共 **258 文件** | **exit=0，0 error**（客户端类路径含全部 client-only 类，因此 predicate/ItemColor/事件处理器都实际参与编译） |
| V1b | 复跑（主树同期被其它批次改动后） | 同上；期间主树有 19 个 `entity/**` + `test/DanmakuDamageTest.java` 被并行批次改动（mtime 09:48，非本批） | 仍 **exit=0**；本批 6 个草稿与那些改动无冲突 |
| V2 | 资源 JSON 合法性 | `python -c "json.load(...)"` 对 8 个 JSON | 全部通过 |
| V3 | 贴图字节一致性 | `md5sum`（复制的 PNG vs 原作 PNG） | 6/6 一致（`rod_of_remorse_used` + `hourai_0..4`） |
| V4 | 主树现状核对 | 对 models/item 目录 `grep`、对 `ItemKaigo`/`ItemOnbashira`/`ItemHouraiJeweledBranch`/`ItemGapFoldingUmbrella` 全树 grep 引用方 | ① 该目录无任何 `overrides` 先例（本项为首例）② `ROD_OF_REMORSE` 唯一外部引用 `EntitySukima.java:284` 只做 `is(...)` 判定，不读 damage → 组件化不影响 |
| V5 | API 事实核对 | 逐条读 `neoforge-21.1.252-sources.jar`：`ItemProperties`/`ClampedItemPropertyFunction`/`ItemOverride`/`ItemOverrides`/`ItemModelGenerator`/`ModelBakery`/`ItemColor`/`ItemColors`/`ItemRenderer`/`RegisterColorHandlersEvent`/`PlayerEvent.BreakSpeed`/`Player#getDigSpeed`/`Item#getDestroySpeed`/`Tool`/`Item.Properties#durability`/`ItemStack#isDamageableItem` | 全部结论有出处（见各 PREP 的出处表） |
| V6 | 编译工具用法探查 | `bash porting/tools/compile_staging.sh items2` | 见下方"工具局限"，**未用于本批验收** |

### ⚠ `compile_staging.sh` 的布局局限（须知）

`porting/tools/compile_staging.sh <sub>` 假设 staging 是**扁平包名树**（`staging/<sub>/thkaguyamod/...java`），且**不做去重**——
它把主树与 staging 的 `.java` 一起列进 `files.txt`。实测用 `items2`（含 27 个与主树**同名同包**的类）跑：

```
staging/.../ItemAjaRedStone.java:39: 错误: 重复类: thkaguyamod.item.ItemAjaRedStone   (27 处同类错误)
--- javac exit: 1   compiled: 280 files
```

即「补丁覆盖主树同名文件」这种 staging 形态会被判为重复类。本批次的 staging 是 `src/main/java` 完整布局（合并＝覆盖/新增）且含**同名文件替换**（`ModDataComponents`/`ItemKaigo`/两个模型会覆盖主树），
所以改用**overlay 到临时副本再全树编译**（V1）——不触碰主树、不跑 gradlew，等价于该脚本的意图。已按简报要求"staging 布局不匹配就静态自查并注明"。

**未做的验证**（本地不可得，需实机）：模型 predicate 实际切换、`ItemColor` 实际着色、BreakSpeed 手感、去掉 `durability(2)` 后的 tooltip 表现。均已在各 PREP 的"需实机确认"列出。

---

## 3. 假定 API 清单（全部已在本机 21.1.252 反编译源核对签名）

| API | 用途 | 核对结论 |
|---|---|---|
| `ItemProperties.register(Item, ResourceLocation, ItemPropertyFunction)` | D4/P15 predicate 注册 | 存在；`@Deprecated`（`ClampedItemPropertyFunction` 未弃用，本批使用之） |
| `ClampedItemPropertyFunction#unclampedCall(stack, level, entity, seed)` | predicate 实现接口 | 返回值自动 clamp 到 0..1 |
| `ItemOverride` 的 `predicate` 支持 `namespace:path` 键 | `thkaguyamod:remorse_used` / `thkaguyamod:hourai_charge_stage` | `ResourceLocation.parse(entry.getKey())` |
| `ItemOverrides.BakedOverride#test`：`>=` 且从后往前匹配 | override 排列顺序 | `if (f < value) return false`；`for (j = size-1; j >= 0; j--)` |
| `ItemModelGenerator#processFrames`：layer 下标 = tintIndex | 折伞 tintindex 0/1 假设 | 显式确认 |
| `RegisterColorHandlersEvent.Item#register(ItemColor, ItemLike...)` | 折伞着色 | mod 总线、仅客户端 |
| `ItemColor#getColor(stack, tintIndex)`，`-1` = 不着色 | 折伞 | `ItemRenderer` 按 ARGB 乘算 |
| `PlayerEvent.BreakSpeed#setNewSpeed(float)` / `#getEntity()` | P10 | `EventHooks.getBreakSpeed` 在 `Player#getDigSpeed` 末段 |
| `FMLClientSetupEvent#enqueueWork(Runnable)` | 客户端注册时机 | `ParallelDispatchEvent` 提供，签名已 `javap` |
| `DataComponentType.Builder#persistent(Codec).networkSynchronized(StreamCodec)` | 新组件 | 与既有 `CHARGE`/`GAP_COLOR` 同构造 |
| `Item.Properties#durability(int)` 会同时设 `MAX_DAMAGE` + `MAX_STACK_SIZE=1` + `DAMAGE=0` | D4 §6 的叠加数分析 | 源码确认 |

---

## 4. TODO 与降级点

| 项 | 位置 | 说明 |
|---|---|---|
| `// TODO K3-review` ×14 | 6 个 Java 草稿内 | 逐条列出接口/语义不确定处（`grep -rn "TODO K3-review" porting/staging/polish-trio`） |
| D4 降级 | `d4/PREP.md` §6 | ① 是否保留 `durability(2)`（保留＝仍有耐久条+`isDamaged()`；去掉＝叠加数变 64、需动 `ModItems`）② 旧存档 `damage=1` 的棒会"免费补满一次" |
| P10 降级 | `p10/PREP.md` §4-1 | 若判定"字面 3.0"偏离手感，切到注释内给的 `hasCorrectToolForDrops ? 3.0F : 1.0F`（还原 1.7.10 的"クモの巣だけ 3.0"） |
| P15 降级 | `p15/PREP.md` B4 | 折伞若需真正分离几何，切换到手写 `elements` + 逐面 `tintindex`（草稿已给） |
| P15 弃用 API | `p15/PREP.md` A6 | `ItemProperties` 系列已 `@Deprecated`，1.21.2+ 需迁移到 `ItemModel`/`ItemTintSource`；1.21.1 无替代入口 |
| 编译工具 | 本报告 §2 | `compile_staging.sh` 对"覆盖式"staging 会误判重复类，未使用；已用等价 overlay 编译 |

---

## 5. 需 K3 接线项（合并动作清单）

| # | 动作 | 文件 |
|---|---|---|
| 1 | 覆盖 `init/ModDataComponents.java`（+`ROD_OF_REMORSE_USED`） | 主树同路径 |
| 2 | 覆盖 `item/ItemKaigo.java` | 同上 |
| 3 | 覆盖 `item/ItemOnbashira.java`（仅 javadoc） | 同上 |
| 4 | 新增 3 个客户端类 | `client/RodOfRemorseClientProperties.java`、`client/HouraiJeweledBranchClientProperties.java`、`client/GapFoldingUmbrellaItemColor.java` |
| 5 | `client/THKaguyaClient.java` 构造函数 **+3 行**监听 | `FMLClientSetupEvent` ×2（D4、P15-A）、`RegisterColorHandlersEvent.Item` ×1（P15-B）；需 +3 个 import |
| 6 | `THKaguyaMod.java` 构造函数 **+1 行** | `NeoForge.EVENT_BUS.register(new OnbashiraDigSpeedHandler());`；需 +1 import |
| 7 | 资源覆盖/新增 | `models/item/rod_of_remorse.json`（覆盖）、`rod_of_remorse_used.json`（新）、`models/item/jeweled_branch_of_hourai.json`（覆盖）、`jeweled_branch_of_hourai_{0..4}.json`（新）、`textures/item/rod_of_remorse_used.png`（新）、`textures/item/jeweled_branch_of_hourai_{0..4}.png`（新） |
| 8 | **裁决 4 项** | ① `ModItems.ROD_OF_REMORSE` 是否改 `new Item.Properties()` / `.stacksTo(1)` ② `ModTiers.KAIGO` efficiency 是否需要显式 `Tool` 规则 ③ P10 用"字面 3.0"还是"复古 1.0"、是否认副手 ④ 玉枝模型是否改 `parent: item/bow` 以还原弓形持握 |
| 9 | 台账更新 | `integration-notes.md` D4 / P10 / P15 状态；发布说明补"旧存档 `rod_of_remorse` 状态位迁移"（已有总声明可覆盖） |

---

## 6. 未决风险

| 风险 | 级别 | 说明 | 缓解 |
|---|---|---|---|
| 组件化后旧存档语义漂移 | 中 | `damage=1`（已使用）→ 组件缺失 → 被当作未使用，等于白送一次 | 阶段 1 已声明旧存档不兼容；如要 datafix 需另立批次 |
| `durability(2)` 去留引出的二阶效应 | 中 | 去掉后叠加数 1→64（玩家可见）、`SwordItem` 的自带 `TOOL` 规则生效（挖掘速度 1.0/1.5 而非 KAIGO tier 的 0.0）；保留则耐久条与"用坏即消失"隐患仍在 | 已列 2 项 K3 裁决；本批**不改** `ModItems`，把决定权留给合并 |
| predicate/着色"静默失效" | 中 | 键名不一致或忘接线时**不报错**，只是图标/颜色不切换（Java 与 JSON 两处硬编码同一语义，靠人眼对齐） | 键名已逐字核对；实机 checklist 已给（玉枝 6 段、折伞 16 色） |
| P10 与其它 MOD 的 BreakSpeed 监听互相覆盖 | 低 | 事件非取消，注册顺序决定胜负 | 未声明优先级；如冲突可用 `addListener(priority, ...)` |
| P10 客户端/服务端预测不一致 | 低 | 双端都会发火；若客户端预测与实际不同步会出现破坏延迟或橡皮筋 | 待实机；`setNewSpeed` 在两端用同一判据 |
| `@Deprecated` predicate API | 低（长期） | 版本升级需重写 | 已标 TODO |
| 大量模型新增对图集/启动的影响 | 极低 | +6 模型 +6 贴图（32×32），与既有 136 模型同量级 | 无需处理 |

---

## 7. 自检：简报要求覆盖情况

- D4：新组件定义 ✅ / 物品类改动（对照 `setDamage` 语义）✅ / 模型 JSON（含 override 机制调查 + 工程内惯例检查 + 1.21.1 原版约定）✅
- P10：三条可行路径（Tool 组件规则 / 属性修饰符 / BreakSpeed 事件）逐条评估并给出否决理由 ✅ / 选定最忠于原作者 ✅ / 处理器草稿 + 注册点 ✅
- P15：predicate 与着色在本版本的**确切 API**（含弃用状态、判定方向、匹配顺序、tintindex 机制）✅ / 玉枝谓词 + 6 个模型 JSON + 客户端注册类 ✅ / 折伞 ItemColor + 双层方案（含备用 elements 草稿）✅ / 贴图全部指名复用原作、未新画 ✅
- 每项三件套：补丁草稿（最终包名/最终相对路径）+ `PREP.md`（改动点/语义对照/降级与风险/需 K3 接线）+ 不确定处 `// TODO K3-review` ✅
- 铁律遵守：**未修改任何既有文件**（主树与 staging 既有目录均只读；临时编译目录 `.verify` 已删除）✅ / 未跑 gradlew ✅ / 未用 git ✅ / 未杀 java 进程 ✅ / 全部产出只写在 `porting/staging/polish-trio/` ✅
  - 说明：编译沙盒期间检测到主树 19 个 `entity/**` + `test/DanmakuDamageTest.java` 文件 mtime 为 09:48（并行批次所为），**非本批改动**；本批只读取主树，所有写入均在 `polish-trio/` 内。

---

# 追加轮次（双变体 + 合并就绪）

> 在首轮 3 份 PREP 的基础上，把待裁决项做成**即取即用的双变体**，并对 P15 做了主树最新状态复核。
> 新增目录：`variants/`（顶层 `MERGE-READY.md` + `d4/` + `p10/`）。**主树依旧零写入。**

## 8. 追加产出清单

| 文件 | 说明 |
|---|---|
| `variants/d4/VARIANTS.md` | A/B 取舍对照（玩家可见差异、存档/创造栏影响、与 1.7.10 一致度、K3 裁决所需全部事实表） |
| `variants/d4/VARIANT-A/`（7 文件） | A 案：**自带 `ModItems.java`**（`durability(2)` → `stacksTo(1)`）+ 组件类 + 物品类 + client + 2 模型 + PNG |
| `variants/d4/VARIANT-B/`（7 文件） | B 案：同上，`ModItems.java` = 主树原样（md5 `d895ffc4…`，与主树一致） |
| `variants/d4/VARIANT-{A,B}/MODITEMS-CHANGE.md` | 各自的单行改动说明 / 「无改动」说明 |
| `variants/p10/VARIANTS.md` | A/B 对照 + **1.7.10 原作确认 + 1.12.2 Forge 反编译实证**（见 §9） |
| `variants/p10/VARIANT-A/src/.../OnbashiraDigSpeedHandler.java` | A 案：字面恒 3.0F |
| `variants/p10/VARIANT-B/src/.../OnbashiraDigSpeedHandler.java` | B 案：`hasCorrectToolForDrops ? 3.0F : 1.0F` |
| `variants/p10/VARIANT-{A,B}/src/.../ItemOnbashira.java` | 两案相同（仅 javadoc 变更） |
| `variants/MERGE-READY.md` | **逐文件合并指令**：目标路径、插入点、依赖的注册行、编译矩阵、I8/I9 冲突核查、合并顺序 |

合计新增 **25 个文件**（`polish-trio` 现有 48 个）。

## 9. 追加验证

| # | 方式 | 结果 |
|---|---|---|
| W1 | **四变体逐个全树 javac**（overlay 主树 260 个 `.java` + 变体文件 = 261 文件/次，临时目录内，跑完即删） | `d4/A exit=0`、`d4/B exit=0`、`p10/A exit=0`、`p10/B exit=0`，**全部 0 error** |
| W2 | 变体差异核对 | `diff -r`：D4 两案仅 `ModItems.java` 第 114 行不同，其余逐字节相同；P10 两案仅 `OnbashiraDigSpeedHandler.java` 内容不同，`ItemOnbashira.java` 相同 |
| W3 | `ModItems.java` 基线生成 | 用 Python 从主树原文派生（A = 单行替换，B = 原文），基线 md5 `d895ffc4301e8d2b6668a466e8fbadf1`；生成后已对主树**只读**（未回写） |
| W4 | P15 主树最新状态复核 | 目标文件 mtime 全部 ≤ 10-02 23:59，近 1h 的 I8/I9 改动（19 文件）与 P15 目标**零交集**；`CHARGE`/`GAP_COLOR` 契约未漂移；`THKaguyaClient` 尾部锚点行未变 |
| W5 | 1.7.10 实机语义取证 | ① 原作 `ItemOnbashira.java` 源码 = 恒 3F ✅ ② `THkaguyaMod_2.9.0.1.jar` 内 `ItemOnbashira.class` `javap -c` = `ldc float 3.0f; freturn`（**与源码一致**）✅ ③ 1.12.2 Forge 反编译：`ItemSword.canHarvestBlock` 只认 `Blocks.WEB`；`ForgeHooks.blockStrength` = `digSpeed/hardness/(canHarvest ? 30.0 : 100.0)` ✅ ④ 1.21.1 侧 `Player#getDigSpeed` → `EventHooks.getBreakSpeed`、`BlockBehaviour#getDestroyProgress` 的 30/100 分除 ✅ |

## 10. 追加的未决与降级

| 项 | 说明 |
|---|---|
| P10 A/B 的最终定案 | 本机无法取得 1.7.10 Forge 二进制；已用「同代 1.12.2 Forge」+「两代 BreakSpeed 都在 30/100 分除之前」两条证据链推出 **B 更贴实机**，但仍建议实机计时 4 组（石头/土/クモの巣/草）终审 |
| P10 第三取向 | 若只想压蜘蛛网到 3.0、不想让草叶变慢：把 B 案的 else 分支从 `1.0F` 改成 `event.getOriginalSpeed()`（`p10/VARIANTS.md` §3-3） |
| D4 A/B 的最终定案 | 差异只有 tooltip `0 / 2` 与潜在可损性；建议 A（与 1.7.10 `setMaxDamage(0)` 等价），B 作零风险回退位 |
| 未实机项 | predicate 三显示上下文、折伞 16 色、P10 破坏时间、D4 组件读写的游戏内表现——全部已在各文档列出 checklist |