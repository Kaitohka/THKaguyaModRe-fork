# P10 — 御柱 `onbashira` 挖掘速度恒 3.0

台账：`porting/integration-notes.md` **P10**「`ItemOnbashira` 挖掘速度 3.0 恒值在 1.21.1 无 item 覆写点 → 用属性组件或 Tool 规则补，阶段 5」。

## 1. 原作语义

`src/main/java/thKaguyaMod/item/ItemOnbashira.java:30-34`（只读参考）：

```java
//ブロックに対する強さ
@Override
public float getDigSpeed(ItemStack itemStack, Block block, int metadata)
{
    return 3F;
}
```

注册为 `new ItemOnbashira(ToolMaterial.WOOD)`（`THKaguyaItems.java:451`），WOOD 的 efficiency 是 2.0，**这个覆写把它替换成常数 3.0**（比木剑快一点点，但远慢于铁 6.0/钻石 8.0）。注释里写的是「とてつもなく強力な武器 しかし重くて…」——即「强力武器但有惩罚」的设计。移植版现状（`ItemOnbashira.java`）**没有实现**，类 javadoc 已挂 TODO。

> 补充事实（本地可核）：`ModItems.ONBASHIRA` = `new ItemOnbashira(ModTiers.WOOD, props)` + `new Item.Properties()`（**无耐久**，与 1.7.10 `ToolMaterial.WOOD` 的 maxUses 无关）。所以「无耐久」这一条移植版已与 1.7.10 行为一致，P10 只差速度。

## 2. 1.21.1 可行实现的三条路径（逐条核对本机反编译源）

| 方案 | 机制 | 能否表达「所有方块恒 3.0」 | 结论 |
|---|---|---|---|
| A. `Tool` 组件规则 | `Item#getDestroySpeed(stack,state)` → `stack.get(DataComponents.TOOL).getMiningSpeed(state)`（`Item.java:157-160`），`Tool.getMiningSpeed` 顺序查 rules、命中即返回，否则 `defaultMiningSpeed`。可用 `Tool.Rule.overrideSpeed(TagKey,float)` / `overrideSpeed(HolderSet,float)` | **能**，但只能按「方块集合」枚举。要覆盖全部方块需要一个含所有方块的自定义 tag（不可能穷举/维护）；`SimpleTier` 的 `deniesDrops(INCORRECT_FOR_WOODEN_TOOL)` 只覆盖木镐挖不动的部分 | ✗ 拒绝（无法表达全量，且会与挖掘等级耦合） |
| B. 属性修饰符（`Attributes.MINING_EFFICIENCY`） | `Player#getDigSpeed`：`if (f > 1.0F) f += MINING_EFFICIENCY`（`Player.java:775-779`）。御柱当前 `Tool` 走 `SwordItem.createToolProperties()`：`defaultMiningSpeed = 1.0F`，只有 `#sword_efficient` 命中 1.5 | **不能**：`f > 1.0F` 是前置条件，且这是**加法**（得不到恒值语义），另需 `ItemAttributeModifiers` + 每 tick 维护（当前 `ModItems` 全树无 `.attributes(...)` 用例） | ✗ 拒绝（不是恒值、还引入状态维护） |
| C. **`PlayerEvent.BreakSpeed` 事件** | `Player#getDigSpeed` 的最后一步：`f = EventHooks.getBreakSpeed(this, state, f, pos)`（`Player.java:803`），NeoForge 用这个值 fill `PlayerEvent.BreakSpeed`（可取消、有 `setNewSpeed`）。**这是 1.21.1 唯一的「按物品、全方块一律」覆写点** | **能**，直接 `setNewSpeed(3.0F)` | ✅ **采用** |

凭证（`net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed`，本机 `neoforge-21.1.252-sources.jar`）：
`getEntity()`（继承自 `PlayerEvent`）、`getState()`、`getOriginalSpeed()`、`getNewSpeed()`、`setNewSpeed(float)`、`getPosition()`（`Optional<BlockPos>`，可能为空）。事件在 `NeoForge.EVENT_BUS`（游戏总线）上，客户端 `LocalPlayer` 的挖掘预测与服务端实破坏**都会**发火。

## 3. 产出

文件：`p10/src/main/java/thkaguyamod/event/OnbashiraDigSpeedHandler.java`（新增类）
→ 合并点：**`.../thkaguyamod/event/OnbashiraDigSpeedHandler.java`** 新建 + **`THKaguyaMod.java` 构造函数 1 行接线**

```java
NeoForge.EVENT_BUS.register(new OnbashiraDigSpeedHandler());   // 紧随 PlayerDeathEventHandler 之后
```

放 `thkaguyamod.event`（**不是** `thkaguyamod.client`）的原因：事件在客户端与专用服务端都会触发，逻辑纯服务端兼容；且 `MIGRATION_RULES.md` R3 的审查闸门是「`import net.minecraft.client` 只允许出现在 client 包」——本类不含任何 client 引用，放在公共包最合适。已在专用服务端 smoke 场景下也成立（与 `PlayerDeathEventHandler` 同构）。

另附 `p10/src/main/java/thkaguyamod/item/ItemOnbashira.java`：**只改 javadoc**（把「未实现」TODO 换成指向事件处理器），方法体一行不动——避免合并时误改行为。

## 4. 与原作语义的差异（需 K3 裁决）

1. **「恒 3.0」的字面 vs 1.7.10 实机行为**：1.7.10 `EntityPlayer#getPlayerRelativeBlockHardness` 对「不是适正工具」的方块走的是常数
   `1.0F / hardness / 100.0F`（即基准速度 1.0），只有 `canHarvestBlock(block)` 为真时才会用 `getDigSpeed = 3.0`。
   `ItemSword#canHarvestBlock` 只在クモの巣（`Blocks.web`）为 true、其余看材质 `isToolNotRequired`（土/砂/砂利等）。
   所以 1.7.10 的**实际**手感更接近「软方块 3.0、石材/矿石 1.0」。本实现按 `getDigSpeed` 的字面（全方块 3.0）移植，比 1.7.10 挖石头更快。
   - 代码里已写等价替代方案（注释内一行即可切换）：
     ```java
     event.setNewSpeed(player.hasCorrectToolForDrops(event.getState(), player.level(),
             event.getPosition().orElse(player.blockPosition())) ? 3.0F : 1.0F);
     ```
     这样会还原成「クモの巣だけ 3.0、他 1.0」。
   - **该 1.7.10 vanilla 分支属还原记忆，仓库内无 1.7.10 反编译源可直接引证**（本机 gradle 缓存只有 1.12.2/1.21 产物）→ 标为待 K3 确认项。
2. **生效顺序**：事件在 `Player#getDigSpeed` 的所有系数（`MINING_EFFICIENCY`、ポーション、`BLOCK_BREAK_SPEED`、水中/空中惩罚）**之后**执行，所以本实现最终是
   `3.0 × 采掘加速/减速系数`（若中途不打折；`setNewSpeed` 直接覆盖前面所有项，包括空中惩罚）。1.7.10 也是「先取 `getDigSpeed`，再乘 potion 系数」→ **顺序一致**。
3. **副手**：1.7.10 只看主手（`getCurrentItem`）。本实现 **主手或副手** 都认。差异仅在「主手空、副手御柱」时；`TODO K3-review` 已标，若要严格对齐可去掉 `getOffhandItem` 判定。
4. **创造模式/瞬间破坏**：`PlayerEvent.BreakSpeed` 在创造模式也会发火，但原版创造模式走另一条路径（`isCreative()` → 直接破坏），实际无影响。
5. **不涉及**：`SwordItem.canAttackBlock`（`!player.isCreative()`）未改，与原作 `ItemSword` 的「创造模式不能左键砍方块」一致。

## 5. 需 K3 接线项

1. 新建 `thkaguyamod/event/OnbashiraDigSpeedHandler.java`（本目录草稿）。
2. `THKaguyaMod.java` 加 1 行 `NeoForge.EVENT_BUS.register(new OnbashiraDigSpeedHandler());`（`import thkaguyamod.event.OnbashiraDigSpeedHandler;`）。
3. 可选：合并 `ItemOnbashira.java` 的 javadoc 变更。
4. **裁决** §4-1（字面 3.0 / 复古 1.0）与 §4-3（是否认副手）。
5. `ModItems.ONBASHIRA` **不需要改**（无耐久保持原样）；`ModTiers.WOOD` 不需要改。

## 6. 风险与降级

| 风险 | 说明 | 处置 |
|---|---|---|
| 与服务端不同步 | 客户端预测用同样的事件（`LocalPlayer.getDigSpeed` 也经 `EventHooks`），因此双端一致；若出现橡皮筋，检查是否被其它 MOD 的 `BreakSpeed` 监听覆盖 | 事件非取消、只改速度；排后（`setNewSpeed` 后注册的监听会再覆盖）→ 已用 `NeoForge.EVENT_BUS` 顺序默认，未做优先级声明 |
| 影响面 | 只在「手持/副手御柱」时改速度，其它物品零影响；`event.getEntity()` 恒非空 | 无需额外守卫 |
| 性能 | 每次破方块判定 1 次，两次 `ItemStack#is`（identity 比较），无分配（除 `getPosition()` 的 `Optional`，且不调用） | 可忽略 |
| 降级 | 若事件方案被否决，退路是方案 A（自定义 `Tool` 规则 + 一个 `#thkaguyamod:onbashira_mineable` 方块 tag），但表达不了「全部方块」 | 记录于此 |

## 7. 验证

- 静态：`Player#getDigSpeed` → `EventHooks.getBreakSpeed` 的调用链、`BreakSpeed` 的 API 签名（`getEntity/getState/getNewSpeed/setNewSpeed/getPosition`）逐条对照本机 `neoforge-21.1.252-sources.jar` 已核实。
- 编译：见 `../REPORT.md` §2（隔离沙盒全树 javac，`exit=0`）。
- 需实机确认（**未做**）：手持御柱挖土/石/矿石的破坏时间；与采掘加速药水叠加；主手/副手切换。