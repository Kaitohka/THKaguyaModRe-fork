# 阶段 1：源码盘点与可行性评估 — 验收记录

模组：**五つの難題MOD+ (THKaguyaMod)**，modid `THKaguyaMod`，版本 `2.90-1.7.10`，东方 Project 弹幕模组。
源码状态：完整 Java 源码 266 个文件（git 基线快照 `2501a9c`）。**资源文件全部缺失**（无 assets/、无 mcmod.info、无贴图/模型/语言/音效文件）。

难度评级定义（按规划约定：**C = 高难度**）：

| 等级 | 含义 |
|---|---|
| S | 近乎机械映射，逻辑可原样保留 |
| A | 适配级：API 改名/换签名，逻辑不变 |
| B | 模块重写级：机制需按新体系重构，设计可保留 |
| C | 高难度：重写核心或存在阻断风险，必须有替代方案 |

---

## 1. 依赖清单及替代方案

| 依赖 | 1.7.10 中的用途 | 1.21.1 状态 | 替代方案 |
|---|---|---|---|
| 第三方模组 API | **无编译期依赖**（确认无 IC2/Baubles 等 import） | — | 无需处理 |
| `item.alicedoll.bare`（外部 MOD 物品，字符串软引用） | 万宝槌转换配方 1 条（→诅咒替身人偶） | 该 MOD 无 1.21.1 版 | 删除该转换配方，或改为可选的数据驱动条件配方 |
| LWJGL GL11/GL12 即时模式 | 全部渲染（约 55 个文件直接调用） | 1.21.1 为 GL32 + RenderSystem，即时模式不可用 | 全部重写为 `BufferBuilder` + 自定义 `RenderType`（加法混合用 `RenderStateShard`）；billboard 改 `Quaternionf` |
| Forge `Configuration` (.cfg) | THKaguyaConfig 全部配置项 | 已移除 | NeoForge `ModConfigSpec`（TOML），配置项 1:1 映射 |
| 音效文件探测（硬编码 `bin/assets/.../sukima.ogg` 路径） | 隙间音效切换 | 坏味道，必须删除 | 标准 `sounds.json` + 音效事件注册，配置项仅保留开关 |
| 数字药水 ID（5/9/10/11/14/18/20 硬编码） | 食物/道具/符卡的药水效果 | 已移除 | `MobEffects` 注册表引用 |
| 硬编码物品/方块 ID（染料 351、花 37/38 等） | 激光染色、开花特殊弹 | 已移除 | `Items.DYE`→`DyeColor` 体系、`Blocks` 引用 + `BlockTags` |
| netty `ByteBuf`（`C17PacketCustomPayload("MC|TrSel")`） | 早苗商店翻页按钮 | 原版协议已变 | NeoForge `CustomPacketPayload` + `PayloadRegistrar`，1 个包即可 |
| `EnumHelper` | 6 个工具材质 + 3 个防具材质 | 已移除 | `Tier`/`ArmorMaterial` 自定义实现（1.21 为 record + 注册表） |

---

## 2. 功能清单表（覆盖 @Mod 入口全部注册链）

### 2.1 注册与基础设施

| 功能 | 1.7.10 实现 | 1.21.1 对应 API | 难度 |
|---|---|---|---|
| mod 入口/代理 | `@Mod` + `@SidedProxy`（CommonProxy/ClientProxy） | `@Mod` + `DistExecutor`/入口分离 | A |
| 物品注册（54 个字段，含 19 弹形/8 色珍珠/4 色刀/符卡 ID 等 metadata 变体） | `GameRegistry.registerItem` + damage 值子类型 | `DeferredRegister<Item>`；metadata 变体拆分独立物品或 `DataComponentType` | B |
| 方块注册（弹幕合成台 2 meta、小神灵 8 色 meta） | `GameRegistry.registerBlock` + ItemBlockWithMetadata | `DeferredRegister<Block>` + `BlockBehaviour.Properties`；meta → 拆方块或 `BlockState` 属性 | A |
| TileEntity（TileEntityDivineSpirit，**空壳**，逻辑全在注释里） | `GameRegistry.registerTileEntity` | `BlockEntityType` + `DeferredRegister`；原计划"100 tick 自毁"用 `BlockEntityTicker` 实现 | S |
| 实体注册（registerModEntity 约 45 个 + registerGlobalEntityID 15 个） | `EntityRegistry`，trackingRange=40、updateFrequency=1 | `EntityType.Builder` + `DeferredRegister` + `AttributeSupplier` 事件 | A |
| 自写刷怪蛋 ItemSpawnEgg（巨型 if 链 hack） | 内嵌类逐个 new 实体 | 原版 `SpawnEggItem` 或干脆不做（创造栏用 `/summon`） | A |
| 自然生成（妖精/幽灵/BOSS/商人，生物群系+权重+条件） | `EntityRegistry.addSpawn` + `BiomeGenBase` 静态数组 | `BiomeModifier` JSON（`neoforge:add_spawns`）+ `SpawnPlacements` | A |
| 创造模式物品栏（符卡页） | `CreativeTabs` 子类 | `CreativeModeTab` + `BuildCreativeModeTabContentsEvent` | S |
| 配置系统（EntityID/Limit/Effect/SE vol/Danmaku/Other 共 30+ 项） | Forge `Configuration` | `ModConfigSpec`；EntityID 类别整体废弃（新版实体 ID 自动）；硬编码路径探测删除 | S |
| 合成配方（有序约 75 + 无序 1，含 26 个符卡配方） | `GameRegistry.addRecipe` 硬编码 | JSON 配方（数据驱动）；可写脚本批量生成 | A |
| 发射器行为（银刀/弹物品，识别 NBT `Infinity`） | `BlockDispenser.dispenseBehaviorRegistry` | `DispenseItemBehavior` 注册 | A |
| 地牢战利品（打出小槌） | `ChestGenHooks` | 战利品表 JSON + `LootTable` 注入 | A |
| 创造栏双命名体系（unlocalizedName 与注册名两套） | `stringHumpToUnderline` 正则 | 统一为注册名，直接废弃 | S |
| 村民霖之助（职业 932 + 7 条交易） | `VillagerRegistry.IVillageTradeHandler` | `VillagerTradesEvent`（或 `VillagerProfession`+`PoiType`） | B |

### 2.2 弹幕核心系统（本模组的心脏）

| 功能 | 1.7.10 实现 | 1.21.1 对应 API | 难度 |
|---|---|---|---|
| 弹幕数据结构 ShotData/LaserData（form/color/damage/size/delay/end/special） | 纯 POJO + 工厂 | 原样保留（可改 record） | S |
| 弹幕常量（25 弹形 × 8 色、特殊弹 ID、难度） | DanmakuConstants 常量类 | 原样保留 | S |
| 弹幕生成库 THShotLib（2388 行：createShot/Wide/Circle/Ring/RandomRing/Sphere + 激光系列 + ways 球面分带表 + Vec3 数学） | `Vec3.createVectorHelper`、直接 new 实体 spawn | 逻辑原样保留；`Vec3`→`net.minecraft.world.phys.Vec3`、`spawnEntityInWorld`→`addFreshEntity`；**附带修复每 tick `new Random()` 等分配热点** | A |
| 弹幕实体 EntityTHShot（2033 行：移动/加速/旋转/重力/hitCheck 扫掠射线/弹弹相杀/难度补正/时停协议） + 11 个子类（激光/追踪符/核弹/阴阳玉等） | 直接继承 Entity + DataWatcher 5 槽位（定点小数角度） | `SynchedEntityData`；碰撞改 `Level.clip`+`ProjectileUtil`；手动位移逻辑保留 | **C** |
| 特殊弹体系 ISpecialShot（约 30 项，散布自注册） | `SpecialShotRegistry` + **每 tick 每弹 `Class.newInstance()`** | 注册表保留，实例缓存/枚举化（顺带修掉反射热点） | B |
| 杂鱼弹幕模式表（"000"~"064"，int 编码 + 按难度 4 档） | `DanmakuPatternRegistry` HashMap | 数据结构保留，编码逻辑保留 | B |
| 符卡宣言（视线选目标/经验+B弹消耗/宣言演出/40 格消弹） | THKaguyaLib + EntitySpellCard + SpellCardRegistry **反射** | 注册表改 `Supplier` 工厂；宣言流程逻辑保留 | B |
| 36 个 THSC_ 符卡类（35 张注册 + 1 个注释掉的） | THSpellCard 模板方法 + 纯弹幕脚本 | 基类重写后，各卡弹幕脚本近乎纯数学，逐个搬运 | B |

### 2.3 实体系统

| 功能 | 1.7.10 实现 | 1.21.1 对应 API | 难度 |
|---|---|---|---|
| EntityDanmakuMob 基类（**完全禁用原版 AI**，自研 move/余弦缓动/多阶段 BOSS 血条/符卡宣言/掉落辅助） | `isAIEnabled()=false` + 重写 onUpdate | 自研 tick 逻辑保留是优势；`DataWatcher`→`SynchedEntityData`；属性改 `AttributeSupplier` | B |
| 杂兵 5 种（妖精/向日葵妖精/幽灵/使魔/弹幕苦力怕，模式解码发射、僵尸妖精复活彩蛋） | EntityCreature 子类 | 逻辑保留，基类映射 | B |
| BOSS 8 种（琪露诺/露米娅/屠自古/莉格露/灵梦/神子/咲夜/早苗，多阶段 HP 重置 + 符卡阶段） | `onDeathUpdate` 里重置 HP 切阶段 | 逻辑保留；`onDeathUpdate`→`tickDeath` | B |
| 商人实体（咲夜/早苗 IMerchant + 仿制村民 GUI） | `IMerchant`/`MerchantRecipeList`/`displayGUIMerchant` | `Merchant` 接口 + `SimpleMenuProvider` + 原版 MerchantMenu | B |
| 道具效果实体 13 个（时停怀表/秒表、替身人偶、银飞刀、金阁寺、扫帚、八卦炉、绯想剑、探宝棒、隙间传送门、灵击札、魔人经卷、阳伞） | Entity 子类 + DataWatcher | 逐一重写，逻辑保留 | B |
| **时间停止协议**（`ticksExisted--`+位置回滚+玩家 setPlayerLocation 回传） | EntitySakuyaWatch 每 tick 扫 40 格实体直接改内部状态 | `tickCount` 已私有化 → Accessor/AT 或改自维护冻结字段；玩家回传 `ServerPlayer.connection.teleport`；替代方案：NeoForge `EntityTickEvent`/`EntityJoinLevelEvent` 拦截 + Attachment 存 prev 状态 | **C** |
| 效果实体 6 个（符卡魔法阵/五芒星/结界盾/反射盾/冰冻箱/吸光石） | Entity 子类（非粒子） | 逻辑保留；冰冻箱同时停协议 | B |
| 死代码：EntityAIDanmakuPosition（无引用） | EntityAIBase 复制 | **不移植，直接丢弃** | — |

### 2.4 物品系统（63 类）

| 功能 | 1.7.10 实现 | 1.21.1 对应 API | 难度 |
|---|---|---|---|
| ItemTHShot（539 行核心：6 种阵型发射 + 7 项 NBT 参数 + 双层图标染色） | NBT `Number/DanmakuForm/Speed/Gravity/Special/Color/Infinity` | **NBT → 自定义 `DataComponentType`**；图标染色 → `ItemColor`+模型层 | B |
| ItemTHLaser / ItemTHSpellCard（统一符卡入口，damage=卡 ID，Charge 充能） | 同上 | 符卡 ID → DataComponent 或拆物品 | B |
| 射击武器 13 件（八卦炉/祓棒×2/天狗扇/制御棒/水枪/龙颈玉/玉枝弓/幽々子扇/幽香伞/艾哲红石/灵魂火把/冈格尼尔） | onItemRightClick/onPlayerStoppedUsing 蓄力 | `use`/`releaseUsing`/`UseAnim`；耐久当充能槽的用法保留 | B |
| 近战武器 12 件（楼观剑系/白楼剑/冰剑/莱瓦汀/绯想剑/神子剑/悔悟棒/死神镰/御柱/佛御石钵等） | ItemSword + hitEntity/onUpdate | `hurtEnemy`/`inventoryTick`；`EnumHelper` 材质 → 自定义 `Tier` | B |
| 防具 5 件（火鼠裘/魔理沙帽/诹访子帽/河童帽/阴阳玉） | ItemArmor + onArmorTick + getArmorModel | `ArmorMaterial` record + `IClientItemExtensions.getHumanoidArmorModel`；tick 逻辑→`inventoryTick` 检槽位 | B |
| 食物 3 件（子安贝/天界桃/伊吹瓢） | ItemFood + 数字药水 ID | `FoodProperties` + `MobEffects` | A |
| 移动/辅助道具 12 件（时停表/秒表/银刀/隙间/折伞/血阴阳玉/穿墙凿/柄杓/第三眼/闭眼/探宝棒/替身人偶/扫帚/金阁寺/灵击札/小槌等） | 射线 + setPlayerLocation | `Level.clip` + `ServerPlayer.connection.teleport` | B |
| 未注册/未完成物品：roukanSenpuuzin（声明未注册）、ItemHoutou（功能注释光） | — | 移植时决定：实装或删除（建议先列入缺陷清单） | — |

### 2.5 渲染系统（最大重写面）

| 功能 | 1.7.10 实现 | 1.21.1 对应 API | 难度 |
|---|---|---|---|
| RenderTHShot（1595 行，23 弹形手写顶点 + 8 色调色板） | Tessellator + GL11 加法混合 + billboard | `BufferBuilder`+自定义 `RenderType`（`Additive`）+`Quaternionf` billboard；**顶点数学可原样搬运** | **C** |
| RenderTHLaser（721 行：参数化圆柱 + 冈格尼尔/莱瓦汀硬编码顶点表 + 母子激光） | 同上 | 同上 | **C** |
| 其余约 50 个渲染器（道具/效果/符卡/魔法阵等，手法统一） | Tessellator + GL11 | 同一套重写模式，量大但模式固定 | B |
| 26 个手写 Techne 盒体模型（ModelBase/ModelRenderer，含 2 个 ModelBiped 帽） | addBox + 旋转点 | `LayerDefinition`/`ModelPart`（盒体坐标可直接映射） | B |
| BOSS 头顶血条 RenderTHBoss | FontRenderer+Tessellator 世界内绘制 | `EntityRenderer` 内 `PoseStack`+`Font` 绘制 | A |
| TileEntityDivineSpiritRenderer（TESR 三层发光面片）+ ISimpleBlockRenderingHandler（空壳） | TESR + 自定义 renderId | `BlockEntityRenderer`；ISBRH 体系已删除，直接去掉 | A |
| 未注册渲染器 4 个（RenderMasterSpark/RenderTHHenyoriLaser/RenderSphereShot/RenderHakureiShield） | — | 不移植或按需启用 | — |

### 2.6 GUI / 网络 / 事件

| 功能 | 1.7.10 实现 | 1.21.1 对应 API | 难度 |
|---|---|---|---|
| GuiHandler（4 个数字 ID 映射） | `IGuiHandler` + `player.openGui` | `MenuType`+`NetworkHooks.openScreen`/`SimpleMenuProvider` | A |
| 弹幕合成台（Container 522 行：8 功能背包 + 3×3 占用模式→弹形判定 + 数值累加 + 复制逻辑） | `Container`+`InventoryCrafting`+手写判定 | `AbstractContainerMenu`+`CraftingContainer`；**判定逻辑纯 Java 可原样搬运**；刷新机制改 `slotsChanged` | B |
| 激光合成台（同构简化版） | 同上 | 同上 | B |
| 早苗商店 GUI（仿村民交易 + 唯一自定义包） | 仿 ContainerMerchant + `C17PacketCustomPayload` | 1.21.1 原版 `MerchantMenu` 体系 + 1 个 `CustomPacketPayload` | B |
| 万宝槌转换台（4 条硬编码转换 + 经验门槛） | Container + NBT 还原 | 逻辑保留；含外部 MOD 物品配方 1 条（删除或数据驱动） | B |
| 残机复活事件（死亡→消耗残机原地复活+无敌圈） | `LivingDeathEvent` | NeoForge `LivingDeathEvent`（取消事件），近乎照搬 | S |
| 时停事件处理器 | **空壳，从未注册** | 不移植 | — |
| GuiSpellCardMode（死亡界面复制品） | **未接入 GuiHandler** | 不移植 | — |

---

## 3. 风险与回退（阶段 1 结论）

| # | 风险 | 等级 | 回退/对策 |
|---|---|---|---|
| R1 | **资源文件全部缺失**（贴图 23+ 弹形、符卡卡面、实体材质、语言文件、音效 spellcard/masterspark/down/sukima 等） | 阻断 | 本仓库是 fork（THKaguyaModRe-fork），从上游官方仓库或发布版 `THKaguyaMod 2.90-1.7.10.jar` 提取 `assets/`；模型是手写代码不需资源，贴图 PNG 可直接迁移，模型 JSON 按 1.21.1 规范重建 |
| R2 | 弹幕渲染即时模式 GL 重写量（约 2500 行核心顶点代码 + 50 渲染器） | C | 顶点数学原样搬运；先实现 RenderTHShot 一个形状打通管线作模板，再批量推广；加法混合用自定义 `RenderType.CompositeState` |
| R3 | 时停系统依赖实体内部状态改写（`ticksExisted--`、`setPlayerLocation`） | C | 替代方案：`tickCount` 用 NeoForge `EntityTickEvent.Pre` 取消 tick + Attachment 存回滚快照；玩家用 `connection.teleport`；若不可行，时停降级为"强缓慢+弹幕静止" |
| R4 | 1.7.10 存档/NBT 数据（弹幕参数 NBT 7 字段、符卡 Charge 等）无直接映射 | B | 全部转 `DataComponentType`；旧存档迁移评估后大概率声明"不支持"，提供补偿说明 |
| R5 | 已知源码缺陷（移植时顺带修复，不照搬）：CommonProxy 服务端引用 `Minecraft.getMinecraft()`（专用服务器崩溃）、特殊弹每 tick 反射 newInstance、HenyoriLaser 每段 new dummy 实体、EntityPendulum 按本地化名称比对方块、ItemSpawnEgg hack | B | 移植即修复，逐一登记 |
| R6 | 死代码/半成品（EntityAIDanmakuPosition、GuiSpellCardMode、时停事件、4 个未注册渲染器、THSC_BrilliantDragonBullet、EntityMiko 占位、roukanSenpuuzin、ItemHoutou、SC_31） | — | 不移植清单，避免无效工作量 |

## 4. 环境确认

- 目标版本：**NeoForge 21.1.252**（MC 1.21.1 最新，经 maven API 确认，共 249 个 21.1.x 版本）
- 需要：JDK 21、Gradle 8+、NeoGradle 7.x 或 ModDevGradle 2.x（阶段 2 选型）
- 网络：NeoForge maven API 可达 ✔

## 5. 验收自检

- [x] 功能清单覆盖 `@Mod` 入口（THKaguyaCore preInit/init 全注册链：物品/方块/TE/GUI/实体/弹幕模式/符卡/生成/发射器/战利品/渲染/事件/配方），无遗漏
- [x] 每个 C 级条目（R2 渲染核心、R3 时停系统、弹幕实体基类）均有替代实现方案
- [x] 依赖清单完整（无编译期第三方依赖；软引用 1 条已登记替代方案）
- [x] 基线快照已提交（git `2501a9c`）
