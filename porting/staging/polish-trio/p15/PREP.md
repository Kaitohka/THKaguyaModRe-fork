# P15 — 蓬莱玉枝 6 段充能图标 + 隙间折叠伞双层染色图标

台账：`porting/integration-notes.md` **P15**「玉枝 6 段充能图标 / 折伞双层染色需模型 JSON ItemProperties → 阶段 5 客户端润色」。

---

## A. 蓬莱の玉の枝 `jeweled_branch_of_hourai`

### A1. 原作语义（`src/main/java/thKaguyaMod/item/ItemHouraiJeweledBranch.java`）

`Charge` 是**消费量**（0 = 满，50/tick 恢复、每次射击 +50，射击需 `getCharge() <= 200`），并直接决定图标：

| 1.7.10 `getIconFromDamage(damage)` | 图标 | 贴图 |
|---|---|---|
| `damage <= 0` | `icon[0]` | `houraiEda` |
| `<= 50` | `icon[1]` | `houraiEda_4` |
| `<= 100` | `icon[2]` | `houraiEda_3` |
| `<= 150` | `icon[3]` | `houraiEda_2` |
| `<= 200` | `icon[4]` | `houraiEda_1` |
| 其余（201..301） | `icon[5]` | `houraiEda_0` |

`getIcon(stack, renderPass, player, usingItem, useRemaining)` 也返回同一张（**使用中无独立 pull 图**）。
移植版现状：`ModDataComponents.CHARGE`（Integer）已正确承载消费量，但模型是单一
`models/item/jeweled_branch_of_hourai.json`（只有 `houraiEda` 一张），**6 段未实现**。

### A2. 1.21.1 API 调查结论（逐条对照本机 `neoforge-21.1.252-sources.jar`）

| 项 | 本版本事实 | 出处 |
|---|---|---|
| predicate 注册入口 | `ItemProperties.register(Item, ResourceLocation, ItemPropertyFunction)`（public static），内部 `PROPERTIES.computeIfAbsent(item)` | `client/renderer/item/ItemProperties.java` |
| 弃用状态 | `ItemPropertyFunction` 接口与 `ItemProperties.register(...)` 重载均带 **`@Deprecated`**；`ClampedItemPropertyFunction`（返回 0..1，自动 clamp）未弃用，是推荐写法 | 同上 / `ClampedItemPropertyFunction.java` |
| 模型 JSON | `overrides[].predicate` 的键经 `ResourceLocation.parse` → **支持 `namespace:path` 形式** | `client/renderer/block/model/ItemOverride.java` |
| 判定方向 | `BakedOverride#test`：`if (f < matcher.value) return false` → **「属性值 >= 阈值」** | `ItemOverrides.java` |
| 匹配顺序 | 构造时 `for (int j = size-1; j >= 0; j--)`，`resolve` 里第一个 `test` 通过者生效 → **JSON 从低阈值到高阈值排列**（最后写的优先） | 同上 |
| 属性名解析顺序 | 先通用表（`damage`/`damaged`/`custom_model_data`/`trim_type`…），再按 Item 的表；未注册的名字返回 `null` → 该属性视为 `-Infinity` | `ItemProperties.getProperty` |
| 原版先例 | `assets/minecraft/models/item/bow.json`（`pulling`/`pull`） | 原版资源 jar |
| **本工程先例** | `resources/assets/thkaguyamod/models/item/` 136 个 JSON 中 **0 个** 用 `overrides`/`predicate`（`grep -rl overrides` 为空）→ 本项是**首例**，按原版 `bow.json` 的写法建立惯例 | 本仓库 |
| NeoForge 新式替代 | 1.21.1 **不存在** `ItemTintSource`/`ItemModel` 数据驱动 predicate（那是 1.21.2+ / 1.21.4 的 API）。`ItemProperties` 是本版本唯一入口 | 反编译源全树 grep 无 `ItemTintSource` |

### A3. 产出（合并点＝拷贝到主树同相对路径）

| staging 文件 | 主树目标 | 动作 |
|---|---|---|
| `p15/.../client/HouraiJeweledBranchClientProperties.java` | `.../thkaguyamod/client/HouraiJeweledBranchClientProperties.java` | 新增 |
| `p15/.../models/item/jeweled_branch_of_hourai.json` | 同路径 | **替换**（保留 `parent`/`layer0`，追加 5 条 overrides） |
| `p15/.../models/item/jeweled_branch_of_hourai_{0,1,2,3,4}.json` | 同目录 | 新增 5 个 |
| `p15/.../textures/item/jeweled_branch_of_hourai_{0,1,2,3,4}.png` | 同目录 | 新增 5 张（**复用原作贴图**，见 A4） |

predicate 键：`thkaguyamod:hourai_charge_stage`，取值 = **段阶号 / 5**（clamp 后即 `0.0 / 0.2 / 0.4 / 0.6 / 0.8 / 1.0`）。

overrides 表（顺序＝低→高，含义见 A1）：

| 阈值 | 模型 | 对应 1.7.10 |
|---|---|---|
| （基准，无 predicate） | `jeweled_branch_of_hourai` | `houraiEda`（Charge 0） |
| `0.2` | `jeweled_branch_of_hourai_4` | `houraiEda_4`（1..50） |
| `0.4` | `jeweled_branch_of_hourai_3` | `houraiEda_3`（51..100） |
| `0.6` | `jeweled_branch_of_hourai_2` | `houraiEda_2`（101..150） |
| `0.8` | `jeweled_branch_of_hourai_1` | `houraiEda_1`（151..200） |
| `1.0` | `jeweled_branch_of_hourai_0` | `houraiEda_0`（201..301） |

### A4. 贴图复用（**不新画**，全部来自原作）

| staging 贴图 | 原作来源（`porting/assets-1.7.10/assets/thkaguyamod/textures/items/`） | md5 校验 |
|---|---|---|
| `jeweled_branch_of_hourai_4.png` | `houraiEda_4.png` | `3744b8b6ad573e9555199bb2e983eaf5` ✅ |
| `jeweled_branch_of_hourai_3.png` | `houraiEda_3.png` | `2b1c3d0c36eca8b18e73e50abe81768b` ✅ |
| `jeweled_branch_of_hourai_2.png` | `houraiEda_2.png` | `54410b1bfd5b1fba84a8e99dd450bd68` ✅ |
| `jeweled_branch_of_hourai_1.png` | `houraiEda_1.png` | `044b313caf76ee496756ff1ff6e4e4e1` ✅ |
| `jeweled_branch_of_hourai_0.png` | `houraiEda_0.png` | `6cc9fecb157e5ed2426a2e1f09f0eafa` ✅ |
| （**不动**）`jeweled_branch_of_hourai.png` | 主树现有文件 = 原作 `houraiEda.png` | `3c705282bb0e7c9b7caf64704b128d0d` ✅ |

全部 32×32 PNG，命名改为本工程 `snake_case` 规范（对应注册名 `jeweled_branch_of_hourai`）。

### A5. 客户端注册

```java
// THKaguyaClient 构造函数内（与 D4 的注册可合并到同一事件）
modEventBus.addListener(FMLClientSetupEvent.class, HouraiJeweledBranchClientProperties::register);
```

`ItemProperties.register` 按 `Item` 键写静态表，必须在物品注册完成后调用 → `FMLClientSetupEvent`（`event.enqueueWork` 包裹）。类头 javadoc 已写接线说明。

### A6. 风险 / 降级

| 风险 | 说明 | 处置 |
|---|---|---|
| `@Deprecated` API | 见 A2 第 2 行。1.21.1 无替代 | 接受；`// TODO K3-review` 已标 |
| 图标不随 CHARGE 变化 | 若 predicate 注册遗漏或键名不一致，模型会**静默**停在基准图（不报错） | 键名在 Java 与 JSON 两处硬编码同一常量语义，已逐字对齐；实机需确认 6 段都能看到 |
| Charge 语义方向 | 组件存的是**消费量**（0 = 满），若将来有人把语义改成「充能量」则整表反向 | 已在此文档冻结语义；`ItemHouraiJeweledBranch` 类 javadoc 已有说明 |
| 恢复计时不同步 | `inventoryTick` 只在服务端/客户端各自跑，图标切换可能滞后 1 tick | 与 1.7.10 同级（旧版也靠 NBT 同步），接受 |
| 无 `bow` 姿态 | 1.7.10 继承 `ItemBow` → 第三人称弓形持握；移植版 `parent: item/generated` 无该 display | **不在 P15 范围**（图标项）。若要还原，把 6 个模型的 `parent` 改为 `minecraft:item/bow` 即可（或复制 `bow.json` 的 `display` 段）。**TODO K3-review** |

---

## B. 隙間の折りたたみ傘 `gap_folding_umbrella`

### B1. 原作语义（`src/main/java/thKaguyaMod/item/ItemGapFoldingUmbrella.java`）

| 1.7.10 | 行为 |
|---|---|
| `requiresMultipleRenderPasses()` = `true` | 2 层描画 |
| `getIconFromDamageForRenderPass(damage, layer)` | layer 0 → `GapFoldingUmbrella_base`；layer 1 → `GapFoldingUmbrella_ribbon` |
| `getColorFromItemStack(stack, pass)` | pass 0 → `0xFFFFFF`；pass 1 → `colorR/G/B[GapColor]`（16 色 + 1 个备用第 17 项） |
| 副作用 | 该方法是**写**操作：`GapColor >= 16` → 写回 0；NBT 不存在 → 新建 NBT 写 0 |

调色板（`colorName[]` 顺序 = `black, red, green, brown, blue, purple, cyan, lightGray, gray, pink, lime, yellow, lightBlue, magenta, orange, white`）已原样搬入 `GapFoldingUmbrellaItemColor`（**去掉 1.7.10 的第 17 个备用元素**，因为调用侧 `if (color >= 16) color = 0` 使其永不可达）。

### B2. 1.21.1 机制调查结论

| 项 | 本版本事实 | 出处 |
|---|---|---|
| 物品着色入口 | `RegisterColorHandlersEvent.Item`（mod 事件总线、**仅客户端**）：`event.register(ItemColor, ItemLike...)` | `net/neoforged/neoforge/client/event/RegisterColorHandlersEvent.java` |
| `ItemColor` 签名 | `int getColor(ItemStack stack, int tintIndex)` | `client/color/item/ItemColor.java` |
| 颜色如何进顶点 | `ItemRenderer.renderQuadList`：`if (flag && bakedquad.isTinted()) i = itemColors.getColor(stack, bakedquad.getTintIndex());` 然后按 ARGB 分量乘到顶点 | `client/renderer/entity/ItemRenderer.java:195-210` |
| `-1` 的含义 | 无着色（`0xFFFFFFFF`：alpha/R/G/B 全 1） | 同上 |
| 生成模型的 tintindex | `ItemModelGenerator#processFrames(int tintIndex, String texture, …)`：**layer 下标直接作为 `tintIndex`** 写进每个生成面（正面 + 侧面） | `client/renderer/block/model/ItemModelGenerator.java:41-59` |
| 生成模型的触发 | `ModelBakery` 对 `getRootModel() == GENERATION_MARKER`（即 `parent` 链到 `builtin/generated`）的模型调用 `ITEM_MODEL_GENERATOR.generateBlockModel(...)` | `client/resources/model/ModelBakery.java:289-296` |
| 手写 elements 的 tintindex | 每个 face 可显式写 `"tintindex": n`（默认 `-1`） | `BlockElementFace.Deserializer#getTintIndex` |
| 覆盖的显示上下文 | GUI / 手持 / 掉落物都走 `ItemRenderer` → `ItemColors` | `ItemRenderer`、`GuiGraphics` |

### B3. 结论：**现有模型 JSON 已足够，无需改模型**

主树 `models/item/gap_folding_umbrella.json` 已经是：

```json
{ "parent": "minecraft:item/generated",
  "textures": { "layer0": "thkaguyamod:item/gap_folding_umbrella",
                "layer1": "thkaguyamod:item/gap_folding_umbrella_ribbon" } }
```

经 `builtin/generated` 生成后，layer0 的面 tintindex = 0、layer1 的面 tintindex = 1，
于是 **1.7.10 的 pass 0/pass 1 直接映射成 tintindex 0/1**，只需注册一个 `ItemColor`：

- `tintIndex == 0`（base）→ `-1`（= 1.7.10 的 `0xFFFFFF`，但用 `-1` 以保留 alpha=1）
- `tintIndex == 1`（ribbon）→ 调色板颜色

两张贴图主树已有且与原作字节一致（`gap_folding_umbrella.png` ← `GapFoldingUmbrella_base.png` md5 `d2724935…`；`gap_folding_umbrella_ribbon.png` ← `GapFoldingUmbrella_ribbon.png` md5 `8a1f9a40…`）→ **本项零资源新增**。

### B4. 若将来需要「真正分离的双层模型」（备用方案）

现有生成模型的 2 层是**同一 z 范围（7.5..8.5）共面**叠加，靠绘制顺序（layer0 先、layer1 后）+ `LEQUAL` 深度测试取胜；原版 potion/tipped_arrow/firework_star 都是这么做的（已被原版验证可行，NeoForge 另有 `fixItemModelSeams` 处理接缝）。
如果某张贴图需要**不同的几何**（例如 ribbon 要偏移 0.05 或用不同 display），改用手写 elements 模型：

```json
{
  "parent": "minecraft:item/generated",
  "textures": { "base": "thkaguyamod:item/gap_folding_umbrella",
                "ribbon": "thkaguyamod:item/gap_folding_umbrella_ribbon" },
  "elements": [
    { "from": [0,0,7.5], "to": [16,16,8.5],
      "faces": { "south": { "uv": [0,0,16,16], "texture": "#base", "tintindex": 0 },
                 "north": { "uv": [16,0,0,16], "texture": "#base", "tintindex": 0 } } },
    { "from": [0,0,7.45], "to": [16,16,7.55],
      "faces": { "south": { "uv": [0,0,16,16], "texture": "#ribbon", "tintindex": 1 } } }
  ]
}
```

（`tintindex` 逐面显式指定，见 B2 第 7 行；注意此时 `parent` 的 `layer0` 纹理仍会参与 `particle` 解析，保持 `textures` 命名一致即可。）**当前不需要**，仅作降级/演进路径记录。

### B5. 产出（合并点＝拷贝）

| staging 文件 | 主树目标 | 动作 |
|---|---|---|
| `p15/.../client/GapFoldingUmbrellaItemColor.java` | `.../thkaguyamod/client/GapFoldingUmbrellaItemColor.java` | 新增 |

接线（1 行，可与 A5/D4 的 `FMLClientSetupEvent` 并列）：

```java
modEventBus.addListener(RegisterColorHandlersEvent.Item.class, GapFoldingUmbrellaItemColor::register);
```

`import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;`。
选这个事件而非 `FMLClientSetupEvent`：它是颜色注册的专用事件（原版 `ItemColors.createDefault` 也在同一时机填表），且保证在物品注册之后。

### B6. 与 1.7.10 的**有意差异**（依 `MIGRATION_RULES.md` §3「旧版 bug 修而不搬」）

1. **去掉渲染期 NBT 写入**：1.7.10 的 `getColorFromItemStack` 会改 `ItemStack` 的 NBT（把越界 `GapColor` 归零、缺 NBT 就新建）。1.21.1 的 `ItemColor#getColor` 在渲染线程被任意栈调用，写入会破坏线程安全与同步契约。移植版**只读 + 就地钳制**（值越界当 0 用），画面结果相同。
2. **`0xFFFFFF` → `-1`**：1.7.10 返回 RGB（alpha 位为 0），而 1.21.1 会把返回值 alpha 当乘数 → 若照抄 `0xFFFFFF` 会**整层全透明**。这是必须的移植修正，已注释说明。
3. **越界不再写回**：`GapColor >= 16` 时不再修改栈，只在本次着色按 0 处理；`ItemGapFoldingUmbrella.use` 里本来就会把 `>= 16` 归一（`if (color >= 16) color = 0;`），因此实际不可达。

### B7. 风险 / 降级

| 风险 | 说明 | 处置 |
|---|---|---|
| 静默无着色 | 忘记接线或 tintindex 假设不成立时，伞面照常显示但 ribbon 不变色（不报错） | 实机需确认 16 色轮换（蹲下右键）时 ribbon 颜色随之变化 |
| 共面叠加 | 见 B4 | 原版先例充分，接受；必要时切手写 elements |
| `GapColor` 组件缺失 | `getOrDefault(...,0)` → 黑色（1.7.10 缺 NBT 也是黑色） | 一致 |
| 与 `ItemSukima`/`GAP` 的关系 | 伞耐久耗尽后换成 `gap` 物品（`ItemGapFoldingUmbrella.use` 已实现），与着色无关 | 无需改 |

---

## C. P15 需 K3 接线项（汇总）

1. 新增 3 个客户端类（`HouraiJeweledBranchClientProperties`、`GapFoldingUmbrellaItemColor`；D4 的 `RodOfRemorseClientProperties` 见 `../d4/PREP.md`）。
2. `THKaguyaClient` 构造函数加 2 行监听（`FMLClientSetupEvent` ×1 用于玉枝 predicate；`RegisterColorHandlersEvent.Item` ×1 用于折伞着色）。
3. 资源合并：`jeweled_branch_of_hourai.json` **替换** + 5 个变体 JSON + 5 张 PNG（A3/A4 表）。
4. **裁决** A6 最后一行（是否把玉枝模型 `parent` 改为 `item/bow` 以还原 1.7.10 的弓形持握）。
5. 无 `ModItems` / `ModDataComponents` 改动（`CHARGE`、`GAP_COLOR` 组件均已存在且语义匹配）。

## D. 验证

- 静态：predicate 阈值方向/override 顺序/属性名解析/生成模型 tintindex/`ItemColor` 调用点，全部逐条对照本机 `neoforge-21.1.252-sources.jar` 核实（见 A2、B2 的出处列）。
- 资源：6 个 JSON 全部 `json.load` 通过；5 张 PNG 与原作 md5 逐一比对一致（A4）。
- 编译：见 `../REPORT.md` §2（隔离沙盒全树 javac，`exit=0`）。
- 需实机确认（**未做**）：玉枝 6 段随 Charge 切换；折伞 16 色 ribbon 变色；两者在 GUI/手持/掉落物三种上下文一致。