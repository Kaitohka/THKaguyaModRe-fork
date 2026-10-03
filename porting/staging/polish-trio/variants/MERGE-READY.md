# P15 MERGE-READY — 玉枝 6 段充能图标 + 折伞双层染色图标（逐文件合并指令）

> 复核时点：主树 `src/main/java` 共 **260** 个 `.java`（含 13 个 `test/`），
> 较上一批基线（253）**+7**，全部为 `test/**` 与 `entity/**`（I8/I9 修复）——见 §0 冲突核查。
> 本文件给出**可直接照抄的拷贝/插入指令**；草稿文件本身的说明见 `../p15/PREP.md`。

## 0. I8/I9 冲突核查（先做的第一件事）——结论：**无冲突**

| 核查项 | 事实 | 结论 |
|---|---|---|
| 主树近期改动清单 | `find src -newermt "2026-10-03 08:40"` → 19 个文件：`entity/{EntityDivineSpirit,EntityShotMaterial}.java`、`entity/effect/{CirnoIceBox,HakureiShield,HakurouReflecter}.java`、`entity/item/{Kinkakuzi,MarisaBroom,MiniHakkero,SilverKnife,Sukima,YuukaParasol}.java`、`entity/living/EntityDanmakuMob.java`、`entity/shot/{NuclearShot,SanaeWind,THHenyoriLaser,THLaser,THSetLaser,THShot}.java`、`client/renderer/shot/RenderTHShot.java`、`test/` 若干 | 全是 I8/I9（`canBeHitByProjectile`/`isPickable`/扫帚减速）范围 |
| 与 P15 的目标文件交集 | 交集 = **空**。P15 只碰 `item/ItemHouraiJeweledBranch.java`、`item/ItemGapFoldingUmbrella.java`、`client/*`（新增）、`models/item/*.json`、`textures/item/*.png` | ✅ |
| P15 目标文件 mtime | `ItemHouraiJeweledBranch.java` / `ItemGapFoldingUmbrella.java` = **10-02 16:53**；`THKaguyaClient.java` = 10-02 23:59；`models/item/*.json` = 10-02 17:06 | 未被本轮改动 |
| 关键契约是否漂移 | `ModDataComponents.CHARGE`（Integer）/ `GAP_COLOR`（Integer）**签名与语义均未变**（`ModDataComponents.java` mtime 10-02 13:03 未动）；新增的 7 个测试类里 `IntegerDataComponentBoundaryTest` 恰好把 `CHARGE`/`GAP_COLOR` 的边界语义**固定下来**（0 默认值、`has()` 可区分缺失），与本批 predicate/ItemColor 的读法一致 | ✅ 无契约漂移 |
| `THKaguyaClient` 接线锚点 | 文件尾部仍是 `modEventBus.addListener(RegisterClientExtensionsEvent.class, ModClientExtensions::register);` 后接 `}` `}` | ✅ 锚点未变，插入点有效 |
| 主树资源 | `models/item/gap_folding_umbrella.json`（md5 `217bd67b…`）仍是 `layer0`+`layer1` 两层的 `item/generated`；`models/item/jeweled_branch_of_hourai.json`（md5 `1d05a565…`）仍无 `overrides`；两张折伞贴图 md5 与原作一致 | ✅ 前提成立，无需重写模型 |

## 1. 逐文件合并指令

**前置**：以下 `$MAIN` = `porting/neoforge-1.21.1/src`，`$P15` = `porting/staging/polish-trio/p15/src`。
除 `THKaguyaClient.java` 外**全部是拷贝（新增或覆盖）**，没有任何需要手工拼进既有文件的内容。

| # | 源（staging） | 目标（主树） | 动作 |
|---|---|---|---|
| 1 | `$P15/main/java/thkaguyamod/client/HouraiJeweledBranchClientProperties.java` | `$MAIN/main/java/thkaguyamod/client/HouraiJeweledBranchClientProperties.java` | **新增** |
| 2 | `$P15/main/java/thkaguyamod/client/GapFoldingUmbrellaItemColor.java` | `$MAIN/main/java/thkaguyamod/client/GapFoldingUmbrellaItemColor.java` | **新增** |
| 3 | `$P15/main/resources/assets/thkaguyamod/models/item/jeweled_branch_of_hourai.json` | 同相对路径 | **覆盖**（追加 5 条 overrides，`parent`/`layer0` 不变） |
| 4 | `$P15/.../models/item/jeweled_branch_of_hourai_4.json` | 同相对路径 | 新增 |
| 5 | `…_3.json` | 同上 | 新增 |
| 6 | `…_2.json` | 同上 | 新增 |
| 7 | `…_1.json` | 同上 | 新增 |
| 8 | `…_0.json` | 同上 | 新增 |
| 9 | `$P15/.../textures/item/jeweled_branch_of_hourai_4.png` | 同相对路径 | 新增（原作 `houraiEda_4.png`） |
| 10 | `…_3.png` | 同上 | 新增（`houraiEda_3.png`） |
| 11 | `…_2.png` | 同上 | 新增（`houraiEda_2.png`） |
| 12 | `…_1.png` | 同上 | 新增（`houraiEda_1.png`） |
| 13 | `…_0.png` | 同上 | 新增（`houraiEda_0.png`） |

> 折伞**零资源新增**：`models/item/gap_folding_umbrella.json` 与两张贴图主树已有且与原作字节一致（§0 末行），不拷贝。

一条命令版：

```bash
$P15/.   # 以下在仓库根执行
cp -r porting/staging/polish-trio/p15/src/main/java/. \
      porting/neoforge-1.21.1/src/main/java/
cp -r porting/staging/polish-trio/p15/src/main/resources/. \
      porting/neoforge-1.21.1/src/main/resources/
```

## 2. `THKaguyaClient.java` 的插入点与注册行（P15 的 2 行；D4 复用同一事件）

**文件**：`$MAIN/main/java/thkaguyamod/client/THKaguyaClient.java`（211 行；mtime 10-02 23:59）

**插入位置**：构造函数末尾，紧接现有最后一行之后（**第 209 行**），在第 210 行 `    }` 之前：

```java
        modEventBus.addListener(RegisterClientExtensionsEvent.class, ModClientExtensions::register);
        // ↓↓↓ P15 追加（2 行）
        modEventBus.addListener(FMLClientSetupEvent.class, HouraiJeweledBranchClientProperties::register);
        modEventBus.addListener(RegisterColorHandlersEvent.Item.class, GapFoldingUmbrellaItemColor::register);
    }
```

**需要新增的 import**（放在现有 `net.neoforged.neoforge.client.event.*` 群组内，字母序）：

```java
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
```

（同包的 `HouraiJeweledBranchClientProperties` / `GapFoldingUmbrellaItemColor` 无需 import——它们就在 `thkaguyamod.client` 包内。）

**两行为何用不同事件**：
- `FMLClientSetupEvent` —— `ItemProperties.register(Item, …)` 按 `Item` 存入静态表，必须在**物品注册完成之后**执行；该事件是 NeoForge 的「注册完成 + 客户端 setup」标准点（1.7.10 的 `registerIcons` 对应位置）。事件在 mod 事件总线，与隔壁 `RegisterClientExtensionsEvent` 同类。草稿内部已用 `event.enqueueWork(...)` 包住（并行分发 → 投递回主线程）。
- `RegisterColorHandlersEvent.Item` —— 颜色注册的专用事件（原版 `ItemColors.createDefault` 在同一时机填表），比 setup 事件更贴语义，也保证在物品注册之后。

## 2A. D4 合并（`rod_of_remorse`，变体二选一）

**整树拷贝即可**（两个变体都自带 `ModItems.java`，无需手工改主树）：

```bash
X=A   # 或 B
cp -r porting/staging/polish-trio/variants/d4/VARIANT-$X/src/main/java/. \
      porting/neoforge-1.21.1/src/main/java/
cp -r porting/staging/polish-trio/variants/d4/VARIANT-$X/src/main/resources/. \
      porting/neoforge-1.21.1/src/main/resources/
```

| 目标主树路径 | 动作 | A/B 是否相同 |
|---|---|---|
| `src/main/java/thkaguyamod/init/ModDataComponents.java` | 覆盖（尾部追加 `ROD_OF_REMORSE_USED`，`register()` 不动） | 相同 |
| `src/main/java/thkaguyamod/item/ItemKaigo.java` | 覆盖 | 相同 |
| `src/main/java/thkaguyamod/client/RodOfRemorseClientProperties.java` | 新增 | 相同 |
| `src/main/java/thkaguyamod/init/ModItems.java` | 覆盖（**第 114 行**：A = `stacksTo(1)`，B = 主树原样 `durability(2)`） | **不同（唯一差异）** |
| `src/main/resources/assets/thkaguyamod/models/item/rod_of_remorse.json` | 覆盖（加 `overrides`） | 相同 |
| `…/models/item/rod_of_remorse_used.json` | 新增 | 相同 |
| `…/textures/item/rod_of_remorse_used.png` | 新增（原作 `kaigoStick_1.png`） | 相同 |

**依赖的注册行**：`ModDataComponents.register(modEventBus)` 已在 `THKaguyaMod` 调用（无需改）；
`THKaguyaClient` 需加（若 P15 已合并，则 import 已存在）：

```java
modEventBus.addListener(FMLClientSetupEvent.class, RodOfRemorseClientProperties::register);
```

**若主树在合并前又改过 `ModItems.java`**：改用 `variants/d4/VARIANT-A/MODITEMS-CHANGE.md` 的查找串做单行替换。

## 2B. P10 合并（`onbashira`，变体二选一，类名相同不可并存）

```bash
X=A   # 或 B
cp porting/staging/polish-trio/variants/p10/VARIANT-$X/src/main/java/thkaguyamod/event/OnbashiraDigSpeedHandler.java \
   porting/neoforge-1.21.1/src/main/java/thkaguyamod/event/
cp -r porting/staging/polish-trio/variants/p10/VARIANT-$X/src/main/java/thkaguyamod/item/. \
   porting/neoforge-1.21.1/src/main/java/thkaguyamod/item/
```

| 目标主树路径 | 动作 |
|---|---|
| `src/main/java/thkaguyamod/event/OnbashiraDigSpeedHandler.java` | 新增（A/B 内容不同） |
| `src/main/java/thkaguyamod/item/ItemOnbashira.java` | 覆盖（**仅 javadoc**，方法体零改动；A/B 相同） |
| `src/main/java/thkaguyamod/THKaguyaMod.java` | **手改 1 行**（见下） |
| `src/main/java/thkaguyamod/init/ModItems.java` | **不动**（`new Item.Properties()`，无耐久保持原样） |
| `ModTiers.java` | **不动** |

**插入点**（`THKaguyaMod.java` 构造函数内，基线第 **74** 行 `NeoForge.EVENT_BUS.register(new PlayerDeathEventHandler());` 之后）：

```java
        NeoForge.EVENT_BUS.register(new PlayerDeathEventHandler());
        // ↓↓↓ P10 追加（1 行）
        NeoForge.EVENT_BUS.register(new OnbashiraDigSpeedHandler());
```

新增 import（与既有 `thkaguyamod.event.*` 群组同区，`PlayerDeathEventHandler` 那行之后即可）：

```java
import thkaguyamod.event.OnbashiraDigSpeedHandler;
```

## 3. 验证（本轮复核时重新执行）

| 方式 | 命令 | 结果 |
|---|---|---|
| 全树编译（含 P15 两个新类，overlay 到临时副本） | `javac -nowarn -proc:none -encoding UTF-8 -cp <merged.jar;legacy> @files.txt` | **exit=0**，`files=261`，`0 error` |
| 同上，逐变体各一次（D4 A、D4 B、P10 A、P10 B） | 同上（每个变体含其自带 `ModItems.java`） | 四次全部 **exit=0 / errors=0**（§3b 矩阵） |
| 资源 | 6 个 JSON（本体 + 5 变体）`json.load` 通过；5 张 PNG 与原作 md5 逐一一致 | ✅ |
| 契约 | `CHARGE` / `GAP_COLOR` 仍是 `DataComponentType<Integer>`，`getOrDefault(...,0)` 读法未变；`IntegerDataComponentBoundaryTest` 已把该语义固化为测试 | ✅ |

## 3b. 四个变体的编译矩阵（本轮实测）

| 变体 | overlay 文件数 | javac exit | error 数 |
|---|---|---|---|
| `d4/VARIANT-A`（含 `ModItems` 改 `stacksTo(1)`） | 261 | 0 | 0 |
| `d4/VARIANT-B`（含 `ModItems` 原样 `durability(2)`） | 261 | 0 | 0 |
| `p10/VARIANT-A`（恒 3.0F） | 261 | 0 | 0 |
| `p10/VARIANT-B`（`hasCorrectToolForDrops` 分岐） | 261 | 0 | 0 |
| （基线 P15 两新类，见上表首行） | 261 | 0 | 0 |

> 每条 = `neoforge-1.21.1/src/main/java`（260 个 `.java`，含 13 个 test）+ 该变体 1 个文件；
> 每次都在 `porting/staging/polish-trio/.verify` 临时目录内进行，**主树零写入**，跑完即删。

## 4. 与本批其它变体的合并顺序建议

1. 先合并 **P15**（纯客户端 + 资源，不触碰任何 `entity/**`，与 I8/I9 改动零交集）。
2. 再合并 **D4**（选 A 或 B，§2A）——`ModDataComponents` 只追加，不与 P15 冲突。
3. 最后合并 **P10**（选 A 或 B，§2B）——`THKaguyaMod` 1 行。
4. 三处手改行互不重叠（`THKaguyaClient` 2 行、`THKaguyaMod` 1 行），可一次做完。
5. D4、P15 各自的客户端注册都挂在 `FMLClientSetupEvent` 上 —— **同一事件多次 addListener 合法**（两个独立监听器），也可合并为一行 lambda 同时调用两者；本批保持分开以便回退。
6. **`ModItems.java` 的三方交汇**：P15 不动它；D4 两个变体都自带（A 改 1 行、B 与主树相同）；P10 不动它。→ 若同时合并 D4+P10，只有 D4 会写它，无冲突。

## 5. 未决 / 需 K3 注意

- P15 使用 `ItemProperties` / `ItemPropertyFunction`（1.21.1 已 `@Deprecated`，但本版本无替代入口）——详见 `../p15/PREP.md` A2/A6。
- **P10 的 A/B 之决**：本机只能证实 1.12.2 Forge 的 `ForgeHooks.blockStrength` 是「`canHarvest ? 30 : 100` 除法」，1.7.10 Forge 二进制不可得；
  但两代 `PlayerEvent.BreakSpeed` 都位于该除法**之前**（已核对 1.21.1 源码链），故 `BreakSpeed` 在语义上等价于旧版 `Item#getDigSpeed`，
  即 **VARIANT-B（适正 3.0 / 不適正 1.0）更接近 1.7.10 实机**。全部事实见 `p10/VARIANTS.md` §2/§3。
- **D4 的 A/B 之决**：两案玩家可见差异只有「tooltip 是否出现 `0 / 2`」与「潜在可损性」；见 `d4/VARIANTS.md` §0 事实表。
- 玉枝模型是否改 `parent: minecraft:item/bow`（还原 1.7.10 弓形持握）**仍待裁决**。
- 折伞 ribbon 与 base 共用 z 范围（7.5..8.5）是 `item/generated` 的固有行为，原版 potion/tipped_arrow 同样如此；如需真正错位几何，改用手写 `elements`（草稿见 `../p15/PREP.md` B4）。
- 实机 checklist：玉枝 6 段随 `CHARGE` 切换（0/50/100/150/200/201+）；折伞蹲下右键 16 色轮换时 ribbon 变色；两者在 GUI / 手持 / 掉落物三上下文一致。