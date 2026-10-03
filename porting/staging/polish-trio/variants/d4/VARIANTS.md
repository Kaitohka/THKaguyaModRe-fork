# D4 VARIANTS — 悔悟之棒 `rod_of_remorse`：durability(2) 去留两案

> 两案**共用**同一套组件化实现（`ModDataComponents.ROD_OF_REMORSE_USED` + `ItemKaigo` + predicate + 模型），
> 差异**只在 `ModItems.ROD_OF_REMORSE` 的注册行**（外加 B 案独有的一个小改动）。
> 都是「组件即语义」——damage 值不再参与任何判定（两案的 `ItemKaigo.java` 逐字节相同）。

## 0. 两案速览

| | **VARIANT-A（去 durability）** | **VARIANT-B（保留 durability(2)）** |
|---|---|---|
| 注册行 | `new Item.Properties().stacksTo(1)` | `new Item.Properties().durability(2)`（**主树原样不动**） |
| 未使用态耐久 | 无（`MAX_DAMAGE` 组件不存在） | 有（`MAX_DAMAGE=2`，`DAMAGE=0`） |
| 已使用态耐久 | 无 | 仍是 `0/2`（**组件化后 damage 再也不会被写**） |
| 耐久条 | 不显示 | 不显示（`isDamaged()` = damage>0 = false）→ 仅 tooltip/F3+H 可见 `0/2` 字样 |
| 能否被 `hurtAndBreak` 用坏 | **不会**（`isDamageableItem()` 需 `MAX_DAMAGE`+`DAMAGE` 同时存在，A 案两者都无） | **会**（`isDamageableItem()` = true；一旦有代码调用 `hurtAndBreak`，第 2 次就会把棒子变空） |
| 叠加数 | **1**（显式 `stacksTo(1)`） | 1（`durability()` 隐式设 `MAX_STACK_SIZE=1`） |
| 存档兼容 | 旧存档 `damage=1` 载入 → 组件缺失 → 视为未使用（白送一次） | 同左（组件不存在于旧存档） |
| 创造栏 | 与旧版一致（单栈展示） | 同左 |
| 与 1.7.10 一致度 | **高**：1.7.10 `setMaxDamage(0)` 即不可损，组件化后完全等价；唯一差异是旧版可叠加 64 | **中**：保留了 1.7.10 没有的耐久属性（潜在副作用），行为上不易被玩家察觉，但语义上留了「未来有人调 hurtAndBreak 就坏」的地雷 |

**同一实现的关键事实**（决定取舍）：

1. `ItemStack.isDamageableItem()` = `has(MAX_DAMAGE) && !has(UNBREAKABLE) && has(DAMAGE)`
   —— `Item.Properties#durability(int)` 会一次性写入 `MAX_DAMAGE` / `MAX_STACK_SIZE=1` / `DAMAGE=0`
   （本机 `Item.java:456-461` 核实），因此 B 案一定满足可损坏条件。
2. 耐久条只在 `isDamaged()`（= `isDamageableItem() && damage>0`）时绘制；`durability(2)` 的 damage 恒为 0
   → **玩家在物品栏里看不到耐久条**，B 案的可见代价只有 tooltip / F3+H 的 `0 / 2`。
3. `ItemKaigo extends SwordItem`；`SwordItem` 构造器会 `component(DataComponents.TOOL, createToolProperties())`，
   与 `MAX_DAMAGE` 无关 → **两案的挖掘速度完全相同**（1.0 基础 / `#sword_efficient` 1.5）。

## 1. 逐案文件清单与改动

### VARIANT-A（推荐）

**整套可直接 `cp -r` 覆盖**（含 `ModItems.java`，已按 §2 改好）：

| 文件 | 动作 |
|---|---|
| `VARIANT-A/src/main/java/thkaguyamod/init/ModDataComponents.java` | **覆盖**（追加 `ROD_OF_REMORSE_USED`） |
| `VARIANT-A/src/main/java/thkaguyamod/item/ItemKaigo.java` | **覆盖** |
| `VARIANT-A/src/main/java/thkaguyamod/client/RodOfRemorseClientProperties.java` | 新增 |
| `VARIANT-A/src/main/java/thkaguyamod/init/ModItems.java` | **覆盖**（第 114 行 `durability(2)` → `stacksTo(1)`，全文仅此 1 行不同） |
| `VARIANT-A/src/main/resources/assets/thkaguyamod/models/item/rod_of_remorse.json` | **覆盖**（追加 overrides） |
| `VARIANT-A/src/main/resources/assets/thkaguyamod/models/item/rod_of_remorse_used.json` | 新增 |
| `VARIANT-A/src/main/resources/assets/thkaguyamod/textures/item/rod_of_remorse_used.png` | 新增（原作 `kaigoStick_1.png`） |

### VARIANT-B

与 A 相同的 7 个文件，**唯一差别是 `ModItems.java`**（B 版 = 主树原样，md5 与主树一致）：

| 文件 | 动作 |
|---|---|
| `VARIANT-B/.../init/ModItems.java` | **覆盖**，内容 = 主树当前内容（第 114 行保留 `durability(2)`） |

→ B 案 = 「`ModItems` 语义零改动」的最小侵入路线。两案的其余 6 个文件逐字节相同。

> 两案均**自带** `ModItems.java`，因此套用方式统一为「整树拷贝」，不需要手工改主树文件；
> 若主树在合并前又改过 `ModItems.java`，改用 `VARIANT-A/MODITEMS-CHANGE.md` 里的「查找串」做单行替换。
> 基线 md5：主树 `ModItems.java` = `d895ffc4301e8d2b6668a466e8fbadf1`；
> A 版 = `9791feb4c5eacbb615719f489eb1427c`；B 版 = `d895ffc4301e8d2b6668a466e8fbadf1`（与主树相同）。

## 2. `ModItems.java` 的精确改动点

主树现状（`init/ModItems.java:114`，1 行，行尾无注释）：

```java
    public static final DeferredItem<ItemKaigo> ROD_OF_REMORSE = ITEMS.registerItem("rod_of_remorse", props -> new ItemKaigo(ModTiers.KAIGO, props), new Item.Properties().durability(2));
```

**VARIANT-A** 替换为：

```java
    public static final DeferredItem<ItemKaigo> ROD_OF_REMORSE = ITEMS.registerItem("rod_of_remorse", props -> new ItemKaigo(ModTiers.KAIGO, props), new Item.Properties().stacksTo(1));
```

（`stacksTo(1)` 而非裸 `new Item.Properties()`：1.7.10 实际允许同 meta 叠 64，但悔悟棒一次只需一把，
且 `durability(2)` 原本就是隐式 `stacksTo(1)`，保留 1 是**最小行为变化**；若 K3 想更贴 1.7.10 可改成裸 `new Item.Properties()`，
代价是创造栏/搬运时可叠 64——与 1.7.10 同。）

**VARIANT-B**：`ModItems.java` 内容与主树**逐字节相同**（本目录自带一份便于整树拷贝，实际合并时覆盖也不会产生 diff）。

### 两案共用的 `ModDataComponents.java`（manifests 对照）

两案的 `ModDataComponents.java` 与 `ItemKaigo.java`、`RodOfRemorseClientProperties.java`、两个模型 JSON、PNG
**全部逐字节相同**（已用 `diff -r` 验证）——A/B 之间**只差 `ModItems.java` 的第 114 行**。

## 3. 需要 K3 裁决的全部事实（一表）

| 问题 | 事实 | 出处 |
|---|---|---|
| B 案会不会显示耐久条 | 不会：`isDamaged()` 需 damage>0，组件化后无处写 damage | `ItemStack.java:440-446` |
| B 案会不会被用坏 | 会，但**当前无调用方**：全树 `grep hurtAndBreak` 对 `ROD_OF_REMORSE` 无命中；`SwordItem.postHurtEnemy` 已被 `ItemKaigo` 覆写为空 | 本仓库 grep + `ItemKaigo.postHurtEnemy` |
| A 案会不会被用坏 | 不会：无 `MAX_DAMAGE`/`DAMAGE` → `isDamageableItem()`=false，`hurtAndBreak` 直接跳过 | `ItemStack.java:465` |
| 两案挖掘速度差 | 无。`SwordItem` 固定装 `TOOL`；`MAX_DAMAGE` 不参与 `getDestroySpeed` | `SwordItem.java:24-33`、`Item.java:157` |
| 两案叠加数 | 都是 1 | `Item.java:456-461`（`durability` 隐式 `stacksTo(1)`） |
| 旧存档（durability 棒的 damage=1） | 两案都读不到「已使用」→ 视为未使用 = 白送一次；阶段 1 已声明旧存档不兼容 | `ItemKaigo.hasWord` 契约 |
| 创造栏外观 | 两案一致（单栈、单图标，由组件决定 `_0`/`_1` 贴图） | predicate 实现 |
| 配方 | `data/thkaguyamod/recipe/rod_of_remorse.json` 产出 1 个**新栈**（无组件）→ 两案都是「未使用」态 | 本仓库 |
| `EntitySukima` 兼容 | 只做 `is(ROD_OF_REMORSE)`，不读 damage/组件 → 两案无影响 | `entity/item/EntitySukima.java` |

## 4. 合并指令（变体 → 主树）

```bash
# 选定 VARIANT-X 后（X = A 或 B）——整树拷贝即可，两案都自带 ModItems.java
cp -r porting/staging/polish-trio/variants/d4/VARIANT-X/src/main/java/. \
      porting/neoforge-1.21.1/src/main/java/
cp -r porting/staging/polish-trio/variants/d4/VARIANT-X/src/main/resources/. \
      porting/neoforge-1.21.1/src/main/resources/
# 然后加 THKaguyaClient 接线 1 行（两案相同）:
#   modEventBus.addListener(FMLClientSetupEvent.class, RodOfRemorseClientProperties::register);
#   import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;   ← 若 P15 已合并则无需重复 import
```

## 5. 验证

- 两案**各自**（含各自的 `ModItems.java`）在隔离沙盒做全树 javac（overlay `neoforge-1.21.1/src/main/java` 共 **261** 个 `.java`）：
  `d4/VARIANT-A -> exit=0 / errors=0`、`d4/VARIANT-B -> exit=0 / errors=0`。
- 两案仅差 `ModItems.java` 第 114 行（`stacksTo(1)` vs `durability(2)`），该差异不参与编译判定，故结果必然一致——已各自独立跑一次确认。
- 未实机验证项：predicate 三显示上下文切换、tooltip 是否出现 `0 / 2`（B 案）。