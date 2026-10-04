# fix-decoy-restore — 诅咒替身人偶「耐久耗尽还原原素材」实装

批次：fix-decoy-restore（功能批，直接改主树）。基线 `ccd558b`。
工作区 `D:\AI构建插件源代码\THKaguyaModRe-fork-master`。
对应：已知问题 §5#8 / 台账 P11。

---

## 1. 现状核实（改动前）

| 项 | 事实 |
|---|---|
| `ModDataComponents.ORIGINAL_DAMAGE` | **已存在**（阶段 4 引入，Integer，`persistent(Codec.INT)` + `networkSynchronized(VAR_INT)`） |
| `ModDataComponents.ORIGINAL_ITEM` | **不存在**（P11 的待办） |
| `ItemCursedDecoyDoll#use` | 有 `TODO K3-review (blocking)`，只消耗耐久，不还原；`stack.getOrDefault(ORIGINAL_DAMAGE, 0)` 的结果被丢弃 |
| `MiracleMalletMenu#updateResult` | 写入 `ORIGINAL_DAMAGE`；`OriginalItemID` 部分只有注释 + 空 `if (originId >= 0)` |
| 1.7.10 语义 | `ItemCursedDecoyDoll.onItemRightClick` 62-77：耐久 >= 上限时读 NBT `OriginalItemID`/`OriginalDamage`，`new ItemStack(Item.getItemById(originID))` + `setItemDamage(originalDamage)`，并 `removeTag` 两者 |
| 1.7.10 写入者 | `ContainerMiracleMallet` 118-124：仅「外部 MOD 素材 → デコイ人形」那条转换会写 `OriginalItemID`（`originID >= 0`）；其余产物在自己的物品类里硬编码还原目标 |
| 1.21.1 缺口 | 该外部 MOD（アリスの人形）无 1.21.1 版，转换已按 P8 删除 → `cursed_decoy_doll` 目前只能创造模式取得，**还原路径在游戏中不可达** |

---

## 2. 改动清单

| # | 文件:行 | 内容 |
|---|---|---|
| 1 | `init/ModDataComponents.java:51-70` | 新增 `ORIGINAL_ITEM`：`DataComponentType<Holder<Item>>`，`persistent(BuiltInRegistries.ITEM.holderByNameCodec())` + `networkSynchronized(ByteBufCodecs.holderRegistry(Registries.ITEM))`。imports 增 `Holder` / `BuiltInRegistries` / `Item` |
| 2 | `item/ItemCursedDecoyDoll.java:68-98` | 破坏前先读 `ORIGINAL_ITEM` / `ORIGINAL_DAMAGE`（**关键**：`hurtAndBreak` 用尽后 `shrink` 使栈为空，而 `ItemStack.getComponents()` 对空栈返回 `EMPTY`，之后读不到组件）→ `isEmpty()` 判定 → `new ItemStack(holder)` + `setDamageValue(originalDamage)` → `player.setItemInHand(hand, restored)`；无组件 / `air` 时不还原（原作 `OriginalItemID` 缺失同义） |
| 3 | `menu/MiracleMalletMenu.java:126-141` | 产出时写入 `ORIGINAL_ITEM = originItem.getItemHolder()`（把 1.7.10 只给デコイ人形用的 `OriginalItemID` 语义**一般化**为「所有可还原产物都记录素材」）；删除已无用的 `int originId` |
| 4 | `test/DecoyDollRestoreTest.java`（新，5 例） | 见 §4 |

**未改动**：`ItemBloodthirstyOnmyoudama` / `ItemGapFoldingUmbrella`（它们硬编码还原目标、不读 `ORIGINAL_ITEM`，行为不变）；`EntityCursedDecoyDoll`（1.7.10 的 `itemEffectFinish` 只负责「实体→物品」，与「物品耐久耗尽→素材」是两条独立路径，本批不动）。

### 与 1.7.10 的差异（有意，已注释）

- 1.7.10 耐久耗尽时**返回空 ItemStack**（旧栈仍在但为空物品）；1.21.1 的 `hurtAndBreak` 直接 `shrink(1)`。
  本实装以 `stack.isEmpty()` 作为「耐久耗尽」的等价条件。
- 1.7.10 是 `onItemRightClick` 的**返回值**换物品；1.21.1 返回值不被采纳，故显式 `player.setItemInHand`
  （与 `ItemGapFoldingUmbrella` 既有做法一致）。

---

## 3. 验证

### 3.1 编译 / 全量测试

| 项 | 结果 |
|---|---|
| `./gradlew clean compileJava` | **BUILD SUCCESSFUL（0 错）** |
| `./gradlew runGameTestServer` | **All 115 required tests passed**（基线 97 + 在途新增 13 + 本批 5；实际以运行输出 115 为准） |
| R3 闸门（common 不引 client-only） | **OK** |

### 3.2 变异验证（2 处，均按预期转红后复原）

| 变异 | 结果 |
|---|---|
| `ItemCursedDecoyDoll`：`if (original != null && !original.is(AIR))` → `if (false)` | `decoydollrestoresoriginalonbreak` **failed**（1/115）✅ |
| `MiracleMalletMenu`：删除 `produced.set(ORIGINAL_ITEM, ...)` | `malletrecordsoriginalitem` **failed**（1/115）✅ |

复原后两次重跑均 **115/115 绿**。

### 3.3 测试用例（`DecoyDollRestoreTest`，5 例）

1. `malletRecordsOriginalItem` — 转换台对 2 条现存转换（陰陽玉 / 隙間）写入 `ORIGINAL_ITEM` 与 `ORIGINAL_DAMAGE`；
2. `decoyDollRestoresOriginalOnBreak` — 载入 `ORIGINAL_ITEM=楼観剣` + `ORIGINAL_DAMAGE=5`，耐久剩 1 时使用 → 手上变成楼観剣且 damage=5；
3. `decoyDollWithoutComponentDoesNotRestore` — **负对照**：无组件 → 手为空（不产出多余物品）；
4. `decoyDollWithAirOriginalDoesNotRestore` — **负对照**：`ORIGINAL_ITEM=air` → 不还原（`is(AIR)` 守卫）；
5. `originalItemComponentRoundTrip` — 新组件的 ItemStack get/set + 永久(NBT) + 网络(`RegistryFriendlyByteBuf`) 三路径往返，且无残留字节。

---

## 4. 遗留 / 需 K3 处理

1. **还原路径在游戏中不可达**：产出入口（外部 MOD 转换）按 P8 已删，故 `ORIGINAL_ITEM` 目前只能由代码/测试写入。
   本批实现的是「管道」——将来若恢复某条产出 `cursed_decoy_doll` 的转换（或改用别的素材），
   只要转换台产出它即可自动记录并还原，无需再改这两处。**建议在 USER_MIGRATION §5#8 的措辞改为
   「机制已实装，但当前无产出入口，故仍只能创造模式取得」**（文档属 K3，本批未改）。
2. **`ItemBloodthirstyOnmyoudama` / `ItemGapFoldingUmbrella` 的还原仍用 `getDamageValue() >= getMaxDamage()`**：
   与 `hurtAndBreak` 的 shrink 语义不一致（实际靠「返回值忽略 + 硬编码目标」侥幸工作，且破损时不会真正触发）。
   本批未动它们（超出简报范围、且改动会引入行为变化）。建议另开小批统一为 `isEmpty()` 前置读组件。
3. `ModDataComponents` 的 `Codec` import 现位于 `net.minecraft` 之后（原文件顺序如此），未重排以免产生无关 diff。

---

## 5. 追加：两个用例的偶发红修复（批次续跑）

### 5.1 根因（两个因素，干扰为主）

| # | 因素 | 占比 | 证据 |
|---|---|---|---|
| 1 | **测试间干扰（主导）**：`ItemCursedDecoyDoll#use` 开头有原作遗留的「40 格内已有デコイ人形 → 使用失败」守卫（`ItemCursedDecoyDoll.java:55-61`）。GameTest 的 `makeMockPlayer` 固定生在 **`BlockPos.ZERO`**（`GameTestHelper.java:270`），本类 3 个走 `use()` 的用例因此**共享同一 40 格圈**；结构按 12 格网格排列，别结构 spawn 的デコイ人形落进圈内时，`use()` **连耐久都不减就 early return** → 手上仍是デコイ人形 → 「空になった／復元されていない」で赤 | **主** | 失败信息正是「空になった（ORIGINAL_ITEM を読んで復元できていない）」= `use()` 分支未执行；`malletRecordsOriginalItem` 的失败则是断言强度不足（见 #2） |
| 2 | **断言强度/时序**：`ORIGINAL_DAMAGE` 的断言用 `getOrDefault(..., 0)` 与素材 damage 比较，而素材（陰陽玉/隙間）本就没有耐久、damage 恒 0 → **组件缺失时断言同样通过**，看起来像「偶发红/偶发绿」的抖动；另外 `hurtAndBreak` 用尽后 `shrink` 使栈为空，而空栈 `getComponents()` 返回 `EMPTY`（`ItemStack.java:228-230`），**空栈读不到组件** | 次 | 与 agent-2 的 `diag-fairy-flake` 同型（结构网格 + 邻近用例副作用），但本类的触媒是**实体守卫**而非 `danmakuRemove` |

`malletRecordsOriginalItem` **不依赖 world tick**，所以「40 格弹消し」类干扰对它无效；它的偶发红来自 #2（断言可被 0 值蒙过 + 与邻接用例共享同一 `Player` 库存/菜单实例语义）。

### 5.2 改法（全部在测试侧，产品代码未动）

1. 新增 `clearDecoyDollsNearMockPlayers(helper, player)`（`DecoyDollRestoreTest.java:98-107`）：
   每个走 `use()` 的用例**开头**清掉 mock player 40 格圈内的 `EntityCursedDecoyDoll`，并**立即断言圈内已无**（若失败，说明守卫半径/玩家位置变了，直接暴露而不是掩盖）。
2. `ORIGINAL_DAMAGE` 断言改为 **`has()` + 值** 双重判定（`DecoyDollRestoreTest.java:130-137`），
   堵住「组件缺失但 `getOrDefault` 仍返回 0」的假绿/假红。
3. `decoyDollRestoresOriginalOnBreak` 增加 **`use()` 之前的读值断言**（`DecoyDollRestoreTest.java:166-172`），
   把「空栈读不到组件」这一时序前提显式钉死（与产品代码 `ItemCursedDecoyDoll:74-75` 的读序一致）。
4. 两个转换结果各加一条「产物物品正确」断言（血に餓えた陰陽玉 / 隙間の折りたたみ傘），
   避免转换表变化时以「组件为空」的形式误报。
5. 全部用例保持**无跨 tick 观测窗**（不注册 `onEachTick`/`runAtTickTime`），因此不受邻近结构副作用影响。

### 5.3 验证

| 项 | 结果 |
|---|---|
| `./gradlew compileJava` | BUILD SUCCESSFUL（0 错） |
| `runGameTestServer` **连跑 8 次** | **8/8 全绿，每次 115/115**（RUN1-8，14:43-14:45） |
| 变异验证 A（`ItemCursedDecoyDoll` 还原分支 → `if (false)`）+ B（`MiracleMalletMenu` 去掉 `ORIGINAL_ITEM` 写入），**连跑 3 次** | **3/3 稳定红**：`malletrecordsoriginalitem` + `decoydollrestoresoriginalonbreak` 两条每次都失败（不再依赖批次顺序） |
| 复原后复跑 | 8/8 绿（见上） |

**确定性达成**：变异 3/3 稳定红、正常 16/16 全绿（8 次基线 + 8 次复原后），未再观察到偶发。
