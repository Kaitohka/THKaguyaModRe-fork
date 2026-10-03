# D4 — 悔悟之棒 `rod_of_remorse` 状态组件化

台账：`porting/integration-notes.md` **D4**「暂用 durability(2) 占位；阶段 5 转组件+模型 override」。

## 1. 现状与原作语义

| | 1.7.10 `thKaguyaMod.item.ItemKaigo` | 移植版现状 `thkaguyamod.item.ItemKaigo` |
|---|---|---|
| 状态载体 | item damage / meta：`0` = 有文字（可用）、`1` = 已使用 | `ItemStack` damage 值，`0`/`1`（`ROD_OF_REMORSE` 注册为 `durability(2)`） |
| 命中效果 | 目标体力 > 75% 且 meta==0 → 设 75%，`setItemDamage(1)` | 同左，`stack.setDamageValue(1)` |
| 补充 | 右键：背包主栏有黑染料且 meta==1 → 消耗 1 个，`setItemDamage(0)` | 同左，`stack.setDamageValue(0)` |
| 外观 | `getIconFromDamage`：`kaigoStick_0` / `kaigoStick_1` | **未实现**：单一模型 `models/item/rod_of_remorse.json` |
| 发光 | `hasEffect(stack,pass)` =（meta==0），且**忽略 pass**（＝两 pass 都发光） | `isFoil` =（damage==0），无 pass 概念 |
| 耐久 | `setMaxDamage(0)`：**不可损**，meta 只是状态位 | `durability(2)`：出现耐久条且 `isDamaged()`；`hurtAndBreak` 会真的减耐久并把物品变空（但当前无处调用） |
| 附魔 | `getItemEnchantability()` = 0、`isBookEnchantable` = false | 已等价 |

**结论**：damage 值在 1.21.1 是「耐久」语义，用它当状态位是错的（会在 tooltip 显示 `1/2` 耐久条、并让 `isDamageableItem()` 为真）。目标＝独立组件 + 模型 override，恢复旧版双外观。

## 2. 新组件定义（草稿）

文件：`d4/src/main/java/thkaguyamod/init/ModDataComponents.java`
→ 合并点：**`porting/neoforge-1.21.1/src/main/java/thkaguyamod/init/ModDataComponents.java`**（在既有 `ORIGINAL_DAMAGE` 之后追加一段 + `register()` 不动）

```java
public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ROD_OF_REMORSE_USED =
        DATA_COMPONENTS.register("rod_of_remorse_used", () -> DataComponentType.<Boolean>builder()
                .persistent(Codec.BOOL)                  // Codec 已 import
                .networkSynchronized(ByteBufCodecs.BOOL) // ByteBufCodecs 已 import，BOOL 是其静态字段
                .build());
```

- 语义：**组件不在 = 未使用（有文字、效果可用）**；`true` = 已使用。
- `Codec.BOOL` 序列化时 `false` 不写字段 → 未使用栈不带组件，与「未使用是默认态」一致。
- 布尔 vs 枚举：旧版只有 2 态，布尔足够；将来若加第 3 态（如「冷却中」）再改枚举，届时需要 `Codec` 变更（**旧存档兼容问题**，见 §6）。
- 命名：现有组件全用 `snake_case`（`danmaku`/`charge`/`gap_color`/`original_damage`），故取 `rod_of_remorse_used`。

## 3. 物品类改动（草稿）

文件：`d4/src/main/java/thkaguyamod/item/ItemKaigo.java`
→ 合并点：**`.../item/ItemKaigo.java`**（整文件替换）

改动点（逐方法对照原作）：
1. 新增私有 `hasWord(ItemStack)`（＝「未使用」判定），替代 3 处 `getDamageValue() == 0`。
2. `hurtEnemy`：`stack.setDamageValue(1)` → `stack.set(ModDataComponents.ROD_OF_REMORSE_USED.get(), true)`（对照原作 `itemStack.setItemDamage(1)`）。
3. `use`：`stack.setDamageValue(0)` → `stack.remove(ModDataComponents.ROD_OF_REMORSE_USED.get())`（对照原作 `itemStack.setItemDamage(0)`）；同时把判定 `getDamageValue() == 1` 改成 `!hasWord(stack)`。
4. `isFoil`：`getDamageValue() == 0` → `hasWord(stack)`（保留原作「忽略 pass、有文字即发光」的行为）。
5. 删除类头「TODO K3-review (blocking)」块（问题已由本项解决）；改写为指向组件与 predicate 的说明。
6. `getEnchantmentValue` / `isBookEnchantable` / `postHurtEnemy`（原作无耐久消耗）**保持不变**。

## 4. 模型 JSON（1.21.1 物品模型覆盖机制）

工程内既有惯例调查结果：
- `resources/assets/thkaguyamod/models/item/` 共 136 个 JSON，**没有任何 `overrides` / `predicate` 先例**（对该目录 `grep -rl overrides` 无命中）。1.21.1 原版惯例见 `assets/minecraft/models/item/bow.json`（`predicates: pulling/pull` + 顺序排列的 overrides）。
- 机制核对（本机 `neoforge-21.1.252-sources.jar`，权威）：
  - `ItemOverride.Deserializer` 读字段 `model` + `predicate`（`ResourceLocation.parse(键)` → **支持命名空间前缀**）。
  - `ItemOverrides.BakedOverride#test` 是「属性值 **>=** 阈值」（`if (f < matcher.value) return false`），**多个 override 从后往前**（`for (int j = list.size()-1; j >= 0; j--)`）匹配，因此 JSON 里**从低阈值到高阈值**排列＝最后写的高阈值优先。
  - 属性名由 `ItemProperties.getProperty` 解析：先查通用表（`damage`/`damaged`/`custom_model_data`/`trim_type`…），再查按 Item 注册的表。
  - **重要**：`ItemPropertyFunction` 接口与 `ItemProperties.register(...)` 重载在 1.21.1 已带 `@Deprecated`（`ClampedItemPropertyFunction` 未弃用，是推荐写法）。本批次仍按此实现，因它是本版本**唯一**的 predicate 注册入口——NeoForge 1.21.1 未提供 `ItemTintSource`/数据驱动 predicate（那是 1.21.2+/1.21.4 的 API，全树 grep 无此类）。**TODO K3-review**：升级版本时需迁移。

产物（合并点＝拷贝到主树同相对路径）：

| staging 文件 | 主树目标 | 说明 |
|---|---|---|
| `d4/.../models/item/rod_of_remorse.json` | `resources/assets/thkaguyamod/models/item/rod_of_remorse.json` | **替换**现有文件：加 `overrides`（保留 `parent` 与 `layer0`） |
| `d4/.../models/item/rod_of_remorse_used.json` | 同目录新增 | 用「已使用」贴图的变体模型 |
| `d4/.../textures/item/rod_of_remorse_used.png` | `.../textures/item/rod_of_remorse_used.png` | **复用原作贴图**：`porting/assets-1.7.10/assets/thkaguyamod/textures/items/kaigoStick_1.png`（md5 `91a453ea3a80316edc533bdf7f8776c0`，32×32），已按本工程 `snake_case` 命名规范改名，未新画 |

- 主树已有的 `textures/item/rod_of_remorse.png` = 原作 `kaigoStick_0.png`（md5 `9301bf4b…` 一致）→ **不要动**，它就是「未使用」层。
- predicate 键用命名空间形式 `thkaguyamod:remorse_used`（避免与其它 MOD 的裸名 predicate 冲突）。

## 5. 客户端注册（草稿）

文件：`d4/src/main/java/thkaguyamod/client/RodOfRemorseClientProperties.java`
→ 合并点：**`.../client/` 新增文件** + **`THKaguyaClient` 构造函数内 1 行接线**

```java
modEventBus.addListener(FMLClientSetupEvent.class, RodOfRemorseClientProperties::register);
```

- 选 `FMLClientSetupEvent` 的理由：`ItemProperties.register` 按 `Item` 键存表，必须在物品**注册之后**调用；`FMLClientSetupEvent` 是「注册完成 + 客户端就绪」的既有惯例点（1.7.10 的 `registerIcons` 也是这种一次性 setup）。事件在 mod 事件总线（`IEventBus`），与 `THKaguyaClient` 里已有的 `RegisterMenuScreensEvent`/`EntityRenderersEvent` 同类。
- 用 `event.enqueueWork(...)` 包裹（该事件是并行分发的，`DeferredWorkQueue` 保证在主线程跑）。
- 值域 0.0 / 1.0，与 1.7.10 meta 值一致。

## 6. 降级与风险

| 风险 | 说明 | 处置 |
|---|---|---|
| `durability(2)` 去留 | 组件化后不再需要它；去掉可消除耐久条、`isDamaged()` 与「`hurtAndBreak` 会把棒子变空」的隐患（1.7.10 不可损）。但它同时决定 `ItemStack` 是否可以叠加与某些原版判定 | **需动 `ModItems.java`**（主树）。建议改为 `new Item.Properties()`（默认 stacksTo=64）。因 `ItemKaigo extends SwordItem`，而 `SwordItem` 构造器会 `component(DataComponents.TOOL, createToolProperties())`；`Item.Properties` 默认不含 `MAX_DAMAGE`，故 `getDestroySpeed` 走 `Tool`（1.0 / 剑效率 1.5）——与旧版 `ToolMaterial.KAIGO`（efficiency 0.0）有细微差异。**TODO K3-review**：是否一并把 KAIGO tier 的 efficiency 对到 `Tool` 规则里。 |
| 叠加数变化 | `durability(2)` 隐式 `stacksTo(1)` → 去掉后变 64。这是**玩家可见变化**（旧版 meta 0/1 两个栈可以合并吗？1.7.10 有 `setHasSubtypes(true)` 且 meta 不同不合并，但同 meta 可叠加 64） | 若要严格保持「一次只带一把」，显式 `.stacksTo(1)`。**需 K3 裁决**，见 §7。 |
| 旧存档 | 组件是新类型；旧存档里 durability(2) 的「已使用」棒（damage=1）载入后组件不在 → 会被当成「未使用」，等于**免费补满一次**。阶段 1 已声明旧存档不兼容，本项按既有前提接受 | 发布说明无需单列（已有总声明）；若 K3 想要 datafix 需另立批次。 |
| predicate API 弃用 | `ItemProperties`/`ItemPropertyFunction` 已 `@Deprecated`，1.21.2+ 迁移面较大 | 已标 TODO；本版本无替代入口，接受。 |
| `isFoil` 与原版 | 组件驱动的 foil 不受 pass 限制，与 1.7.10「忽略 pass 都发光」一致；无需改 | — |
| EntitySukima 依赖 | `entity/item/EntitySukima.java:284` 只做 `itemstack.is(ROD_OF_REMORSE)` 判定，不读 damage → **不受影响** | 已核对，无需改 |

## 7. 需 K3 接线项

1. `ModDataComponents.java` 追加 `ROD_OF_REMORSE_USED`（本目录已给整文件草稿）。
2. `ItemKaigo.java` 整文件替换（本目录已给草稿）。
3. `THKaguyaClient` 构造器加 1 行 `FMLClientSetupEvent` 监听（`RodOfRemorseClientProperties::register`）。
4. **裁决**：`ModItems.ROD_OF_REMORSE` 的属性是 `new Item.Properties()` 还是 `.stacksTo(1)`（§6 第 1、2 行）。
5. 资源合并：3 个模型 JSON + 1 张 PNG 按 §4 表格拷贝。
6. `ModTiers.KAIGO` 的 `efficiency 0.0` 在删掉 `durability(2)` 后是否需要显式 `Tool` 规则（§6 第 1 行）。

## 8. 验证

- 静态：模型 JSON 的 predicate 判定方向、override 顺序、属性名解析已逐条对照本机 `neoforge-21.1.252-sources.jar`（`ItemOverride`/`ItemOverrides`/`ItemProperties`）核实（见 §4 引文）。
- 编译：见 `../REPORT.md` §2（`compile_staging.sh` 不支持本 staging 布局，已改用隔离沙盒逐类 javac，结果记录在那里）。
- 需实机确认（**未做**）：`rod_of_remorse_used` 的 predicate 在 GUI/手持/掉落物三种显示上下文中都切换正常；去掉 `durability(2)` 后 tooltip 不再出现耐久条。
