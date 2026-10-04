# HANDOFF — THKaguyaMod 移植工程交接文档

> 面向：**新接手的主 Agent / 维护者**。目标：读完本文即可理解项目全貌并继续开发。
> 生成时点：2026-10-04。工作区根：`D:\AI构建插件源代码\THKaguyaModRe-fork-master`（Windows + Git Bash）。
>
> **本文是导航与现状快照，不是事实源。** 唯一事实源是 `porting/integration-notes.md`（台账：已裁决 / 待办 P 项 / 批次状态板 / 事故台账 I 项 / 回归闸门记录）。任何数字冲突以台账与源码实测为准。

## 目录

1. [项目当前进度与已完成工作](#1-项目当前进度与已完成工作)
2. [各模块功能说明与代码结构](#2-各模块功能说明与代码结构)
3. [待办事项与已知问题](#3-待办事项与已知问题)
4. [环境配置与依赖](#4-环境配置与依赖)
5. [关键决策与设计思路](#5-关键决策与设计思路)

<!-- 以下各节逐节落盘 -->

---

## 1. 项目当前进度与已完成工作

### 1.1 五阶段规划执行状态

| 阶段 | 内容 | 状态 | 验收记录 |
|---|---|---|---|
| 阶段 1 | 源码盘点与可行性评估（266 个 Java 文件；资源全缺） | ✅ 完成 | `phase1-inventory.md` |
| 阶段 2 | NeoForge 1.21.1 工程骨架（ModDevGradle + 1.7.10 资源恢复） | ✅ 完成 | `phase2-skeleton.md` |
| 阶段 3 | 核心注册迁移（126 物品 / 10 方块 / 2 标签页 / 110 配方 / 5 语言） | ✅ 完成 | `phase3-registration.md` |
| 阶段 4 | 全量实体/渲染/物品/GUI/网络/数据合并 | ✅ 完成 | `phase4-execution-plan.md` + 台账批次状态板 |
| 阶段 5 | 发布质量基建（GameTest / 冒烟 / 浸泡 / 四轮用户实测修复 / 发布文档） | ✅ **完成**（`8ce39ff`） | `RELEASE_NOTES.md` + 台账回归闸门记录 |

阶段 5 的实际形态：**以用户实测反馈驱动的迭代修复**（事故台账 I1-I19），期间补齐了 GameTest 套件
（89 → **115 例**）、双模式冒烟、浸泡闸门与玩家迁移文档。发布物已打包（见 §1.3）。

### 1.2 git 基线与快照链

```
2501a9c  baseline: 1.7.10 THKaguyaMod source (pre-port snapshot)
a2826d0  phase1: inventory & feasibility assessment
380b472  phase2: NeoForge 1.21.1 skeleton + recovered 1.7.10 assets
09a7597  phase3: core registration migration
a976221  phase4 wip: danmaku core + spellcards + GUI + behavior items
72c3838  phase4: big merge — 34 entities + 26 models + 36 renderers + 28 weapon items
dfda417  phase4 complete: perf fixes + migration guide + all green
2dcac4a  phase5: release gates green — playtest fixes (I7-I11) + 58 GameTests + soak-v2
159ede0  phase5 follow-up: playtest round 3 fixes (I13/I14)
d0dcf87  phase5 follow-up 2: audit wave 3 + FIX-3 (2 P0 + 6 P1) + 76 GameTests
73b247b  phase5 follow-up 3: broom model spin (I15) + nuclear terrain destruction (I16)
1d75d9e  feat: broom manual steering (Phase1 merge) — W/S/A/D/space control
ccd558b  docs: user playtest sign-off — broom steering + nuke terrain confirmed
8ce39ff  phase5 final wave: I17-I19 fixes + audit wave 4 batches + HANDOFF.md   ← **当前 HEAD**
```

**终态：34 个提交，无 tag，工作区干净（`git status` 零改动），在途批次零。**
最后一个提交 `8ce39ff` 一次性落地了此前在途的全部内容：I17 银刀朝向、I18 群系修饰器路径+语法
（含生成器修复与验证器三重闸门）、I19 NBT 缺键归零同型普查、刀弹 3D 接线、替身人偶还原链、
P35/P36 硬化、89→115 测试扩充，以及本文件。

> **接手第一件事**：无需清理，直接跑一遍 §4.3 的闸门命令确认环境可用即可开工。

### 1.3 质量闸门现状

| 闸门 | 命令 | 当前状态（2026-10-04 终态，台账 + 实测） |
|---|---|---|
| 静态闸 R3（common 不得 import `net.minecraft.client`） | `smoke_test.sh` 步骤 [0] | PASS |
| 编译 | `./gradlew compileJava` | 0 错 |
| GameTest | `./gradlew runGameTestServer` | **115/115 PASS**（**8+ 次连跑稳定，偶发已根除**）。主树 **30 个测试类 / 115 个 `@GameTest`** |
| 双模式冒烟 | `bash porting/tools/smoke_test.sh` | **SMOKE PASS**（R3 OK / 客户端到主菜单 / 专用服务端启动 / 双端 0 相关 ERROR、0 配方错误、0 模型错误） |
| 30 分钟浸泡 | `bash porting/tools/soak_test.sh 30` | **×3 PASS**（soak-v2 增强闸门）：36 实体存活回执 36/36、summon 失败 0、异常 0、IOOB 0、0 OOM、0 "Can't keep up" |
| 数据资产校验 | `PYTHONIOENCODING=utf-8 python porting/tools/validate_phase4_data.py` | **RESULT: ALL CHECKS PASS**（含生成器↔主树逐字节自检；I18 后已加固为三重闸门） |
| 语言文件校验 | `python porting/tools/check_lang.py` | **OK**（274 键 × 5 语言键集一致；46 实体 / 126 物品 / 10 方块 / 35 符卡全覆盖） |
| 发布 jar | `porting/neoforge-1.21.1/build/libs/thkaguyamod-2.90-1.21.1.jar` | **860 文件**（1.85 MB；110 配方 / 14 群系修饰器 / GLM / 146 模型 / 5 语言） |

**用户实测验收状态**

| 已验收通过 | 待用户复测 |
|---|---|
| 弹幕/激光/符卡对怪物有伤害（I8）、扫帚可破坏与下骑停止（I9）、扫帚操舵与不抖动（I13/P33）、人物不再自动趴下（I14）、扫帚模型不再乱转（I15）、核弹破坏地形（I16，有意偏离） | 野外妖精自然生成（**I18**）、投掷小刀朝向（**I17**）、魔人镜冠不再崩溃（**I10**）、刀弹 3D 外观（§5#4） |

> ⚠️ **闸门盲区教训（P39）**：数据包 `/summon` 失败**不走 `[ERROR]` 级**，所以"I11 妖精无法生成"
> 在连续两轮浸泡里都是"名单全召成功"的假绿。soak-v2 已把 `Failed to summon`/异常/IOOB 纳入 FAIL 判定
> 并加了存活回执，但**新写的验收脚本仍要遵循同一原则：验证"加载后生效"，而不是"文件存在"或"没报错"**（见 I18）。

### 1.4 事故台账 I1-I19 摘要（完整根因见 `integration-notes.md`）

| # | 一句话 | 状态 |
|---|---|---|
| I1 | 冒烟脚本按目录名宽泛匹配杀 java 进程 → 误杀用户游戏窗口 | ✅ 改 `SmokeRunVmArgs` 精确匹配 |
| I2 | 本机 TLS 拦截代理（Steamcommunity302）→ 专用服务端下载依赖 PKIX 失败 | ✅ 自建信任库 `thkaguya-truststore.jks` |
| I3 | 429 限流 ×3（13 路并发超每分钟 21 次） | ✅ 并发上限降到 10 |
| I4 | 2h 超时 ×6（大文件批次分析期过长，部分零落盘） | ✅ 简报加"立即逐文件落盘" |
| I5 | 大合并后客户端黑屏挂起（模型 `root.getChild` 忽略 1.7.10 父子层级） | ✅ 修复 12 模型取件链 |
| I6 | 替身人偶右键即崩（误用 `createLivingAttributes`，缺 `follow_range`） | ✅ 改 `createMobAttributes` |
| I7 | 延迟弹渲染 `colorR[count%2]` → 负计数下标 -1 客户端必崩 | ✅ 改回 `colorR[color]` |
| I8 | **弹幕/激光/符卡对怪物零伤害**（`canBeCollidedWith` 三职拆分只搬一职） | ✅ 7 处命中扫描 + 12 实体 `isPickable` |
| I9 | 扫帚下骑不停/无法破坏（同 I8 + 减速块未写回 `setDeltaMovement`） | ✅ 修复 |
| I10 | 魔人镜冠客户端必崩（`Not building!`，缓冲源 A→B→A 复用失效写入器） | ✅ FIX-2 修复 |
| I11 | **妖精/幽灵/使魔全部无法生成**（样式表注册调用遗漏，`pattern.get(0)` 越界） | ✅ FIX 修复 + 幂等 clear |
| I12 | （过程教训）审计批 2h 超时零落盘 | ✅ 指令模板固化 |
| I13 | 扫帚无人时永久微颤（1.21.1 `move()` 着地不清零垂直速度） | ✅ 着地 `motionY=-0.03` |
| I14 | 右键物品时人物自动趴下（原版自动匍匐把弹幕碰撞盒算入站立空间） | ✅ 5 实体退出推挤碰撞 |
| I15 | 扫帚飞行模型整圈乱转（角度不回卷 + 渲染 lerp 走完 360° 整数倍） | ✅ 改 `Mth.rotLerp` |
| I16 | 控制棒（核弹）不破坏地形（原作从未接线，属设计意图未实现） | ✅ 按意图实现（**有意偏离**，配置门控） |
| I17 | 银刀投掷朝向恒 180°（`sendsVelocityUpdates` 移植丢失 → 客户端 motion 恒 0） | ✅ 修复（保留同步 yaw；**待用户复测**） |
| I18 | **野外妖精完全不自然生成**（群系修饰器 JSON 少一层 `neoforge/` + `biomes` 写法不合 HolderSetCodec，双错全程静默） | ✅ 修复 + 生成器同步修 + 验证器三重闸门（**待用户复测**） |
| I19 | `/summon` 妖精弹幕卡体内且射速异常（`getShort/getFloat` 缺键返回 0 覆盖构造器值） | ✅ 修复 + 同型普查（6 实体 19 处 `contains` 守卫） |

**规律（对新手最重要的一条）**：这 19 起事故里，**超过一半是"移植丢了一行调用/一个语义"，而不是算法写错**。
`canBeCollidedWith` 三职、样式表注册调用、`sendsVelocityUpdates`、群系修饰器目录层级都属于这一类。
接手后遇到"功能完全失效但没报错"，优先怀疑**接线缺失或静默失败**，而不是逻辑错误。

---

## 2. 各模块功能说明与代码结构

### 2.1 源码总览

根包 `thkaguyamod`（279 个 Java 文件，约 50,700 行）：

```
porting/neoforge-1.21.1/src/main/java/thkaguyamod/
├── THKaguyaMod.java          @Mod 主入口（122 行）
├── danmaku/      7 文件  2,895 行   弹幕核心算法与数据
├── entity/      50 文件 15,950 行   实体（4 个子包）
├── item/        49 文件  5,512 行   物品行为
├── spellcard/   37 文件  3,867 行   35 张符卡 + 基类
├── client/      73 文件 11,294 行   渲染/模型/输入/界面（纯客户端）
├── menu/         8 文件  1,809 行   4 个容器菜单 + 4 个槽位
├── network/      2 文件    126 行   2 个自定义包
├── registry/     3 文件    381 行   样式表/特殊弹/符卡注册表
├── init/        14 文件  1,638 行   全部 DeferredRegister 与数据注册
├── config/       1 文件    141 行   ModConfig（SERVER 侧 TOML）
├── block/        2 文件            工作台 / 小神灵方块
├── blockentity/  1 文件            DivineSpirit 方块实体（空壳）
├── event/        1 文件            玩家死亡（残机复活）
└── test/        30 文件  5,965 行   115 个 GameTest
```

### 2.2 逐包职责

**`danmaku/`（弹幕核心，2,895 行）**
整个模组的算法心脏，与 1.7.10 同名类一一对应。

| 类 | 行数 | 职责 |
|---|---|---|
| `THShotLib.java` | 2,027 | 弹幕生成库：`createShot` 系列 / `createWideShot` / `createCircleShot` / `createRingShot` / 激光系列 / 角度与向量工具 / 形状-颜色-伤害-尺寸常量数组（`SMALL/TINY/.../LASER`、`DAMAGE[]`、`SIZE[]`）。**任何弹幕行为改动先看这里** |
| `THKaguyaLib.java` | 516 | 高阶库：符卡宣言校验（`checkSpellCardDeclaration`）、`itemEffectFollowUser`、特殊弹派发 |
| `DanmakuConstants.java` | — | 形状/颜色/难度/反弹（`BOUND01`）/激光类型常量。1.7.10 里 `THShotLib extends DanmakuConstants`，移植保留该继承 |
| `ShotData.java` / `LaserData.java` | — | 弹/激光参数 record（尺寸、伤害、寿命、重力、特殊 ID） |
| `DanmakuParams.java` | — | 弹物品的 `DataComponentType` 载荷：`(number, form, speed, gravity, special, color, infinity)`，含 `CODEC` + `STREAM_CODEC`；**compact 构造器统一夹紧各字段（P35 修复点）** |
| `ISpecialShot.java` | — | 特殊弹接口（配合 `SpecialShotRegistry` 做无反射派发） |

**`entity/shot/`（10 类，3,896 行）** — 弹幕/激光实体。
`EntityTHShot`（1,962 行，普通弹，含延迟弹机制 `animationCount`/`lastTime`）、`EntityTHLaser`、
`EntityTHSetLaser`（固定激光）、`EntityTHHenyoriLaser`（蛇行激光，**原作未注册**，属死代码）、
`EntityNuclearShot`（核弹）、`EntitySanaeWind`、`EntityHomingAmulet`、`EntityOnmyoudama`、
`EntityDragonNeckJewel`、`EntityMusouFuuin`。

**`entity/living/`（17 类，5,727 行）** — 生物。
- `EntityDanmakuMob`（1,862 行）：**所有弹幕生物的抽象基类**，自研 AI（`registerGoals()` 空实现，
  移动/攻击在 `tick()` 里手写）、`defineSynchedData` 同步槽、NBT 读写（I19 修复点在此）。
- 派生：`EntityTHFairy`（妖精基类，样式表 `setPattern` 在此）→ `EntitySunFlowerFairy` /
  `EntityTHPhantom` / `EntityFamiliar`；`EntityCirno` / `EntityRumia` / `EntityToziko` /
  `EntityWriggle` / `EntityReimu` / `EntityMiko` / `EntitySanae` / `EntitySakuya`（后两者**同时实现
  `Merchant`**，见 §3 的 P47/P49）/ `EntityRinnosuke`（村民型商人）/ `EntityDanmakuCreeper`。
- `IDanmakuMob`：弹幕生物接口。

**`entity/item/`（14 类，4,911 行）** — 道具/效果实体。
`EntitySakuyaWatch`（431 行，时停空间，含位置回卷 + `connection.teleport`）、`EntitySakuyaStopWatch`、
`EntityMarisaBroom`（425 行，扫帚）+ `BroomSteer`（320 行，**操舵纯函数，可单测**）、
`EntitySukima`（隙间）、`EntitySilverKnife`、`EntityMiniHakkero`、`EntityMazinkyoukan`（魔人镜冠）、
`EntityKinkakuzi`、`EntityPendulum`、`EntityHisou`、`EntitySpiritualStrikeTalisman`、
`EntityYuukaParasol`、`EntityCursedDecoyDoll`。

**`entity/effect/`（6 类，1,405 行）** — 纯视觉/范围效果：`EntitySpellCardCircle`（符卡魔法阵）、
`EntityMiracleCircle`、`EntityHakureiShield`、`EntityHakurouReflecter`、`EntityAjaRedStoneEffect`、
`EntityCirnoIceBox`。

**`item/`（49 类，5,512 行）** — 物品行为。三类：
- 弹幕物品：`ItemTHShot`（325 行，19 弹形共用，持 `DANMAKU` 组件）、`ItemTHLaser`、`ItemShotMaterial`。
- 符卡物品：`ItemTHSpellCard`（35 张符卡共用，构造参数为符卡编号）。
- 行为物品：约 40 个（`ItemUchide` 万宝槌、`ItemSpearTheGungnir`、`ItemMikoSword`、`ItemThirdEye`、
  `ItemSakuyaWatch`、`ItemGapFoldingUmbrella`、`ItemBloodthirstyOnmyoudama` …）。多数含
  `// TODO K3-review` 标注的"降级/外部依赖删除"说明。

**`spellcard/`（37 类，3,867 行）** — `THSpellCard`（597 行，符卡基类，弹幕脚本）、
`EntitySpellCard`（符卡实体/生命周期）、`SpellCardRegistry` 在 `registry/`，
其余 35 个 `THSC_*` 为具体符卡（每张一个类）。

**`registry/`（3 类，381 行）**
- `DanmakuPatternRegistry`：妖精系弹幕样式表（难度 1-4 各 9 条 + level 0 的 default 3 条）。
  **注意**：`setDanmakuPattern()` 必须在 config 加载后调用（`THKaguyaMod` 的 `ModConfigEvent.Loading`
  监听里），且方法开头 `clear()` 五张表以保证重载幂等 —— 这是 I11 的修复点。
- `SpellCardRegistry`：符卡编号 → 工厂的注册表（替代原作的静态 `instance` 字段）。
- `SpecialShotRegistry`：特殊弹 ID → 实例的注册表（替代原作的每 tick 反射 `newInstance`，R5 性能项）。

**`client/`（73 类，11,294 行）— 纯客户端，禁止被 common 代码引用（R3 闸门）**
- `THKaguyaClient.java`（219 行）：`@Mod(dist=CLIENT)` 入口，注册所有渲染器/模型层/菜单屏幕/客户端扩展。
- `client/renderer/shot/`：`RenderTHShot` / `RenderTHLaser` / `DanmakuRenderTypes`（自定义 RenderType，
  取代 1.7.10 的 GL11 即时模式）。
- `client/renderer/living/`（15 个）、`client/renderer/effect/`（4 个）、`client/renderer/blockentity/`（1 个）、
  以及根下 19 个道具/效果渲染器。
- `client/model/living/`（14 个 `Model*`）：1.7.10 的 `ModelBase` 几何体改写为
  `EntityModel` + `ModelPart`，几何用生成器（`tools/models*`）转写；**旧版的度/弧度混用 bug 原样保留（P27）**。
- `client/gui/screen/`（4 个 Screen）：弹幕工作台 / 激光工作台 / 早苗商店 / 万宝槌。
- `client/input/`（1 个）：扫帚操舵的按键采集（客户端权威）。
- `ModClientExtensions.java`：防具自定义模型钩子（`IClientItemExtensions`）。

**`menu/`（8 类，1,809 行）** — 4 个 `AbstractContainerMenu`（弹幕工作台 / 激光工作台 / 早苗商店 /
万宝槌）+ 4 个 `Slot`。弹幕工作台的两个菜单持有各自的 `SimpleContainer` 参数槽与 3×3 矩阵，
`updateResult()` 把结果写进 `ResultContainer`；`removed()` 负责退还所有槽位内容。**没有 dataSlots**
（早苗/万宝槌都没有同步进度条）。

**`network/`（2 类，126 行）** — 早苗商店专用：
`SelectSanaeTradePayload`（客户端→服务端，选择交易索引；服务端 `menu.stillValid` + `instanceof` 双校验）、
`UpdateSanaeOffersPayload`（服务端→客户端，下发 `MerchantOffers`）。注册在 `init/ModNetworking`。
两者都走 NeoForge 默认的 `HandlerThread.MAIN`（自动 `enqueueWork`），线程安全。

**`init/`（14 类，1,614 行）** — 全部注册与数据，是"接线"的唯一集中地：
`ModItems`（336 行，126 项）、`ModEntities`（282 行，46 项）、`ModBlocks`、`ModBlockEntities`、
`ModMenus`、`ModSounds`、`ModCreativeTabs`、`ModTiers`、`ModArmorMaterials`、`ModDataComponents`（53 行，
全部自定义组件）、`ModSpellCards`（35 张符卡接线）、`ModSpawns`（305 行，生成谓词 = 1.7.10
`getCanSpawnHere` 的等价实现，**配置门控集中在此**）、`ModNetworking`、`ModDispenserBehaviors`。

**`config/ModConfig.java`（141 行）** — `ModConfigSpec`（SERVER 类型）+ 同名静态字段 + `sync()` 单点写入。
字段名与 1.7.10 `THKaguyaConfig` 保持一致（`danmakuLevel`、`fairySpawnRate`、`shotMaxNumber` …）。
配置文件为 `config/thkaguyamod-server.toml`。

**`block/` + `blockentity/`** — `DanmakuCraftingTableBlock`（一个类，`laser` 布尔区分两种工作台）、
`DivineSpiritBlock`、`DivineSpiritBlockEntity`（1.7.10 为空壳，保持最小实现）。

**`test/`（30 类，115 个 `@GameTest`）** — 全部用 `@GameTestHolder(THKaguyaMod.MOD_ID)` +
`@PrefixGameTestTemplate(false)` + `@GameTest(template = "empty")`；模板是
`src/main/resources/data/thkaguyamod/structure/empty.nbt`（7×7×7 空结构）。
覆盖：注册完整性、弹幕生成/命中/伤害路径（4 条）、组件与参数编解码边界（含 `DanmakuParams` 夹紧）、
NBT 往返与缺键归零、符卡宣言、扫帚物理与操舵、姿势回归、群系生成闸门、残机复活、
人偶还原、银刀朝向、核弹地形。
**新增测试时照抄 `DanmakuSpawnTest` 的写法**；注意测试间会互相干扰（结构坐标/世界状态），
`8ce39ff` 已把偶发红根因定位并消除——**新用例不要依赖执行顺序**。

### 2.3 resources 资产布局

```
porting/neoforge-1.21.1/src/main/resources/
├── assets/thkaguyamod/
│   ├── blockstates/   10 个 JSON
│   ├── models/block/  10 个、models/item/ 136 个 JSON（共 146）
│   ├── textures/      block/10、entity/70、gui/2、item/133、models/3
│   ├── lang/          en_us / ja_jp / ko_kr / zh_cn / zh_tw（5 份）
│   └── sounds.json
└── data/thkaguyamod/
    ├── neoforge/biome_modifier/   14 个 JSON  ← **路径必须带 neoforge/ 一层**（I18）
    ├── loot_modifiers/            1 个（万宝槌地牢注入 GLM）
    ├── loot_table/                1 个（地牢战利品表）
    ├── recipe/                    110 个 JSON（单数目录名）
    └── structure/                 empty.nbt（GameTest 空模板）
```

**生成器所有权规则（重要，违反会互相覆盖）**

| 资产 | 生成器 | 规则 |
|---|---|---|
| 物品/方块/贴图/配方（阶段 3） | `porting/tools/build_phase3_assets.py` | 单一事实源，可重复执行 |
| 群系修饰器 + GLM（阶段 4） | `porting/tools/build_phase4_data.py` | **已修好 `neoforge/` 路径与 HolderSet 写法**；重跑前先确认输出目录 |
| 语言文件 | `porting/tools/check_lang.py --apply` | **lang 已移交本工具维护**；`build_phase3_assets.py` 加了"存在即跳过"守卫（P28） |
| 模型几何 | `porting/tools/build_phase3_assets.py` 内的模型段 | **I5 后标记勿重跑**：生成器曾对所有部件用 `root.getChild` 忽略父子层级，修复是手工的 12 个模型 |

> 校验器：`porting/tools/validate_phase4_data.py`（数据 JSON 结构）、`check_lang.py`（5 语言键集一致性 +
> 实体/物品/方块键全覆盖）。**改完数据资产跑一遍这两个脚本**。

### 2.4 1.7.10 源码的定位

原树保留在 `src/main/java/thKaguyaMod/`（266 个文件，git 基线 `2501a9c`），**只读参考**，
不参与构建（`build.gradle` 只编译 `porting/neoforge-1.21.1`）。对照类时按同名文件找：
`thKaguyaMod/entity/living/X.java` ↔ `thkaguyamod/entity/living/X.java`（包名小写化、类名不变）。
1.7.10 的资源在 `porting/assets-1.7.10/`。

---

## 3. 待办事项与已知问题

> **完整台账见 `porting/integration-notes.md`**（P1-P53 + I1-I19）。本节只列**未关闭**项。
> 已关闭（✅）的 P 项：P8、P13、P23、P30、P33、P40（已固化为移植规则）、P41、P44、**P52**（B1 已实现复核通过）。
> 玩家可见的问题另有一份面向玩家的表述：`USER_MIGRATION.md` §5（22 条）+ 附录 A（21 条待发布方确认）。
>
> **终态提示**：全部在途批次已合并进 `8ce39ff`，**staging 里剩余的都是"已交付但未合并"的可选补丁**（见 §3.1）。

### 3.1 staging 已就绪、等待合并的批次（均为可选，非阻塞）

| 批次 | 位置 | 内容 | 待办 |
|---|---|---|---|
| **D4** 悔悟之棒组件化 | `porting/staging/polish-trio/d4/` | A 变体（去掉 `durability(2)`，等价 1.7.10 `setMaxDamage(0)`）/ B 变体（保留占位）仅 1 行之差 | 裁决 A/B（P31） |
| **P10** 御柱挖掘速度 | `porting/staging/polish-trio/p10/` | A 变体（恒 3.0，`javap` 证实是原作字面值）/ B 变体（按 `hasCorrectToolForDrops` 分岐，逼近原作实机表现） | 裁决 A/B（P32） |
| **P15** 玉枝充能图标 / 折伞染色 | `porting/staging/polish-trio/` | 模型 JSON `ItemProperties` 覆盖层，`MERGE-READY`，与 I8/I9 零交集 | 裁决是否做（纯外观） |
| **charge-hint** 早苗充能提示 | `porting/staging/charge-hint/` | `THKaguyaLib:199` 的静默失败改为 actionBar 提示（键 `thKaguya.msg.sneScCharge` **5 语言已存在**） | 裁决"默认开启 vs 配置门控"（P34）；启用时同步改 `USER_MIGRATION.md` §3.4/§5#15/附录A#9/FAQ Q2 |
| **P29 / A7** 渲染 fixed buffer | `porting/staging/perf-eval/` | 200 弹/帧 draw call 398→14（-96.5%）、按需内存 ≈21KiB、`RegisterRenderBuffersEvent` 一行接线（旧"与 THKaguyaClient 冲突"的顾虑已实测排除） | **建议下一窗口实施**；硬约束=复用 `DanmakuRenderTypes` 单例 |
| **broom-input Phase 2** | `porting/staging/broom-input/` | 服务端镜像包 | **已裁决放弃**（Phase 1 已合并，零协议改动更优），无需行动 |

### 3.2 代码侧未关闭项（按优先级）

**可能影响玩法，建议优先**

| P | 问题 | 建议动作 |
|---|---|---|
| **P45** | `maxEntityCramming`（1.21.1 新增，默认 24）：模组 9 类 `EntityDanmakuMob` 全部计入挤压 → 密集战斗中妖精会成片"凭空死"（1.7.10 无此机制）。**当前 `entity/living/` 无任何 `isPushable` 覆写 → 问题仍然存在** | 二选一：`EntityDanmakuMob` 覆写 `isPushable()→false`（牺牲推挤换无挤压死），或保留原版行为（`/gamerule maxEntityCramming` 可控）。**这是目前最可能被玩家报告的问题** |
| **P42** | 伤害路径回归测试第三批：4 处 I8 修复点（`EntityTHSetLaser` / `EntityNuclearShot` / `EntityTHHenyoriLaser` / `EntityDanmakuMob` 接触扫描） | **已覆盖**：`SetLaserDamagePathTest`(1) + `NuclearShotDamagePathTest`(2) + `DanmakuMobContactDamageTest`(2) 已合并；`EntityTHHenyoriLaser` 因**原作未注册**（运行时无法实例化）而用例删除，其 `:126` 修复点**不覆盖**——登记为已知缺口 |
| **P49** | 商店交易次数：原作 = 1.7.10 原版默认 **7 次**，移植版 = **12 次**（`MerchantOffer` 构造参数） | 裁决"对齐 7（严格忠实）"或"保留 12"。两版**都不补货**（P47 已实核：`canRestock()` 取接口默认 false，全树无 `resetUses`），`USER_MIGRATION.md` §5#22 措辞已按"永久缺货"修正 |
| **P46** | 1.7.10 `getCollisionBorderSize` 身兼"准星加粗"，`EntitySanaeWind` 风弹 1.0 格加粗丢失 → 打拳反弹几乎点不中 | **已修**（FIX-3a）：4 处 `getPickRadius`（SanaeWind 1.0F / THShot = size / DivineSpirit / ShotMaterial 0.1F），对照原作 5 处无遗漏 |
| **P36** | `EntitySpellCard.java:236` 无 null 检查调 `useSpellCard`；`EntitySakuyaWatch:238` / `EntitySakuyaStopWatch:179` 解引用可能为 null 的 `spellCard.user`；`THSC_SatuzinDoll:90` 同族 | **已修**（`8ce39ff`）：4 处 null 守卫 |
| **P35** | `DanmakuParams` int 字段无范围夹紧；`dropShotItem` 等处 `form % 8` 负取模潜伏 | **已修**（`8ce39ff`）：`DanmakuParams` 补 compact 构造器统一夹紧（单点覆盖所有写入路径）+ `DanmakuParamsClampTest`(5 例) |
| **P50** | `EntityCirnoIceBox` 的 `width`/`height` 未持久化（重载后回默认 0.5F，尺寸/消失时机漂移） | **原作同缺** → 登记为原作设计；如需修则下一窗口补 NBT 键 |
| **P51** | `EntitySukima` 的 `warp`/`warpPos*`/`warpAngle`/`count`/`pattern`/`userEntity` 未持久化（重载后隙间传送失效） | **原作同样不写** → 登记为原作设计；若要修需成组补 NBT |
| **P53** | `ItemBloodthirstyOnmyoudama` / `ItemGapFoldingUmbrella` 用 `getDamageValue() >= getMaxDamage()` 判定耗尽，与 1.21.1 `hurtAndBreak` 的 shrink 语义不符（用尽即 shrink，事后读不到） | 下一窗口统一改 `isEmpty()` 判定（参照 `ItemCursedDecoyDoll:74` 的写法） |
| **P48** | 核爆掉落实率与 1.21.1 `blockExplosionDropDecay` 语义差；`USER_MIGRATION.md` 相应措辞可能被误读为"含核弹" | 下一窗口修文档措辞（属文档，不是代码） |

**低优先 / 外观与体验**

| P | 问题 |
|---|---|
| **P29**（= 遗留评估项 **A7**） | 渲染 fixed buffer 延期。**裁决依据已备**（`staging/perf-eval/`）：200 弹/帧 draw call 398→14（-96.5%）、按需内存 ≈21KiB、旧冲突顾虑已实测排除 → **建议下一窗口实施** |
| **P52**（= 遗留评估项 **B1**） | `homing()` 追踪节流 **已实现且实测复核通过**（`EntityTHShot:81` `HOMING_TARGET_INTERVAL=4` 目标缓存 + 仅最良候选 DDA；100 弹 82µs→21µs）——台账旧记录"B1 未做"作废 |
| **P2** | `RenderTHShot` 的 FORM_KNIFE 分支每帧 `new ModelSilverKnife`（R5 性能待办）——**已修**（构造器 bake 一次复用）；§5#4 的 3D 外观待用户实机点检 |
| **P15 / P16 / P18 / P25 / P26 / P27 / P37** | 外观与既有怪癖：玉枝充能图标、折伞染色、`RenderSukima` 混合模式近似值（`SRC_ALPHA,ONE` 代替 `SRC_ALPHA,SRC_ALPHA`）、商店箭头贴图缺失、模型包归属、**旧版模型度/弧度混用 bug 3 处原样保留**、`THSC_Fafurotskies_no_Kiseki` 把 `shotLimitSpeed` 当模式编号（原作同） |
| **P38** | `RenderTHBoss` HP 条 UV 为 NaN（原作同）——**已硬化**（`max(getMaxHealth(),1F)`） |
| **P6** | 火鼠裘无限耐久是否真的无限（`ArmorItem` 构造器是否会强制材料耐久）——需游戏内核对 |
| **P20 / P21 / P22** | GLM 地牢注入 0.77%（原设计意图 5%）、`#c:is_snowy` 群系范围略宽、原作 `registerChestItem` 是空方法（地牢战利品从未生效，GLM 实现的是设计意图）——均为**有意的文档化偏差** |
| **P43** | 游戏内教程/指引：原作即无，`USER_MIGRATION.md` §6 FAQ 即手册；若做指引书/Patchouli 手册属**超出移植范围的新内容**，需单独立项 |
| **P11** | 诅咒替身人偶的 `ORIGINAL_ITEM` 组件 —— **已实装**（`8ce39ff`，还原链可用）；`USER_MIGRATION.md` §5#8 已更新为"机制已修复，仅缺获取途径" |
| **P14** | 发射器弹幕发射者仍用 ArmorStand 占位（原 `EntityFamiliar`，玩家不可见、不影响弹道） |

### 3.3 发布前必须由发布方拍板的事项

见 `USER_MIGRATION.md` **附录 A（21 条）**，其中**发布阻塞级**的 3 条：

1. **作者署名与授权文本**：当前是模板占位 `All Rights Reserved`（原模组未附许可文件）。
2. **反馈渠道**：`issueTrackerURL` / `displayURL` 未填写。
3. **万宝槌地牢刷新率**：0.77% vs 原设计意图 5%，确认最终取值。

另：`USER_MIGRATION.md` §5 有 **22 条玩家可见的已知问题**（扫帚客户端权威、时停天空暗化降级、
4 类音效原版占位、无刷怪蛋、商店预览硬编码、隙间/玉枝图标不变等），全部**不致命**，
但接手后若要改代码，改完必须**同步更新 §5 与附录 A 的对应条目**。

### 3.4 已完成批次（留档索引，供追溯根因）

这些批次的代码**已全部合并进 `8ce39ff`**，报告留在 `staging/` 下作为证据链。排查同类问题时先读对应报告：

| 批次 | 位置 | 内容 |
|---|---|---|
| `diag-spawn` | `staging/diag-spawn/REPORT.md` + `fix/` | **I18** 群系修饰器路径修复（14 JSON 迁到 `neoforge/` 子目录，`biomes` 改 HolderSet 写法）+ `BiomeSpawnGateTest` |
| `audit-nbt-zero` | `staging/audit-nbt-zero/REPORT.md` | **I19 同型普查**：27 类全查，实害 5 处已修（Sukima 颜色、YuukaParasol 耐久、CirnoIceBox 计数、MiniHakkero 三字段、Hisou 三字段）+ `NbtRoundTripZeroKeyTest`(8 例)；存疑 2 处 → P50/P51 |
| `fix-p35-36` | `staging/fix-p35-36/REPORT.md` | P35 `DanmakuParams` 夹紧 + P36 `spellCard.user` null 守卫（4 处） |
| `diag-knife` / `fix-knife-model` | `staging/diag-knife/`、`staging/fix-knife-model/` | **I17** 银刀朝向修复 + `SilverKnifeOrientationTest`；刀弹 3D 接线（§5#4） |
| `diag-fairy-fire` | `staging/diag-fairy-fire/REPORT.md` | **I19** 妖精弹幕卡体内修复 + `FairySummonFireTest`(4 例) |
| `fix-decoy-restore` | `staging/fix-decoy-restore/` | 替身人偶还原链（`ORIGINAL_ITEM` 组件，§5#8 机制实装） |
| `perf-eval` | `staging/perf-eval/` | **P52** B1 追踪节流实现与实测；**P29** A7 fixed buffer 的裁决依据（建议下一窗口实施） |
| `fix-3a` / `fix3b-tests` | `staging/fix-3a/`、`staging/fix3b-tests/` | FIX-3 实体/物品修复报告 |
| `audit-*`（common / render / net / ai / items / vanilla-mech） | `staging/audit-*/REPORT.md` | 五轮深读普查报告，是 P 项与 I 项的原始证据来源 |

> **接手动作**：无需清理。直接跑一次 `compileJava` → `runGameTestServer` → `smoke_test.sh` 确认环境可用。

---

## 4. 环境配置与依赖

### 4.1 工具链

| 项 | 值 | 说明 |
|---|---|---|
| JDK | **Zulu 21.0.4**，路径 `/c/Program Files/Java/zulu-21`（Windows：`C:\Program Files\Java\zulu-21`） | **必需**。本机还有 jdk-1.8 / 11 / 18 / 22 / 25，**不要用它们**——`build.gradle` 声明 `java.toolchain.languageVersion = JavaLanguageVersion.of(21)` |
| Gradle | **9.3.0**（wrapper） | `porting/neoforge-1.21.1/gradle/wrapper/gradle-wrapper.properties` → `gradle-9.3.0-bin.zip`。本机 `~/.gradle/wrapper/dists/` 已有完整发行版 |
| 构建插件 | **ModDevGradle 2.0.148** | `build.gradle` plugins 块。NeoGradle 为备选，**未采用** |
| NeoForge | **21.1.252** | `gradle.properties` → `neo_version=21.1.252`；依赖范围 `[21.1.252,)` |
| Minecraft | 1.21.1 | `minecraft_version=1.21.1` |
| Mappings | Parchment `2024.11.17` @ 1.21.1 | `parchment_mappings_version` |
| modid / 版本 | `thkaguyamod` / `2.90-1.21.1` | `mod_id` / `mod_version` |

**JVM 参数（`gradle.properties`，含 I2 的信任库修复）**

```properties
org.gradle.jvmargs=-Xmx3G -Djavax.net.ssl.trustStore=C:\\Users\\Kaitohka\\.gradle\\thkaguya-truststore.jks -Djavax.net.ssl.trustStorePassword=changeit
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
```

### 4.2 信任库（重要，环境相关）

**背景（事故 I2）**：本机装有 TLS 拦截代理 **Steamcommunity302**，Java 默认信任库不认
`libraries.minecraft.net` 的证书 → 专用服务端首次启动时下载依赖报 `PKIX path building failed`。

**解法**：自建信任库 = JDK 默认 `cacerts` + 代理 CA 链：

- 仓库内副本：`porting/tools/thkaguya-truststore.jks`
- 运行时位置：`~/.gradle/thkaguya-truststore.jks`（`gradle.properties` 指向这里）
- 口令：`changeit`
- 代理根证书：`porting/tools/sc302-root.cer`
- **代理证书轮换后需要重新生成**（`keytool` 把新 CA 导入 `cacerts` 副本）

> 若换机器/换代理，症状是"专用服务端起不来 + PKIX 报错"，按上面重建即可。

### 4.3 构建与测试命令（精确）

所有命令都在**仓库根** `D:\AI构建插件源代码\THKaguyaModRe-fork-master` 下执行（Git Bash）。

```bash
# ① 编译（最快的反馈环）
cd porting/neoforge-1.21.1 && JAVA_HOME="/c/Program Files/Java/zulu-21" ./gradlew compileJava

# ② 功能回归（GameTest；全量跑完自动退出）
cd porting/neoforge-1.21.1 && JAVA_HOME="/c/Program Files/Java/zulu-21" ./gradlew runGameTestServer
#    结果看日志尾部： "========= N GAME TESTS COMPLETE" + "All N required tests passed :)"
#    日志： porting/neoforge-1.21.1/run/logs/latest.log

# ③ 双模式冒烟（R3 静态闸 + 客户端到主菜单 + 专用服务端启动 + 0 相关 ERROR）
bash porting/tools/smoke_test.sh
#    可选参数： --skip-build（跳过 gradlew build）、 --client-only（只跑客户端）
#    日志： porting/neoforge-1.21.1/run-smoke/logs/latest.log
#    通过标志： 末行 "SMOKE PASS"，且两行 "[client]/[server] ... PASS"

# ④ 30 分钟专用服务端浸泡（发布闸门）
bash porting/tools/soak_test.sh 30
#    会注入 run-smoke/world/datapacks/thkaguya_soak 数据包，召唤 36 个实体并逐条断言存活
#    通过标志： receipts 36/36、summon_failures=0、exceptions=0、IOOB=0、0 OOM、0 "Can't keep up"

# ⑤ 数据资产校验（改过 data/ 或 lang/ 后必跑；必须在仓库根执行）
PYTHONIOENCODING=utf-8 python porting/tools/validate_phase4_data.py
python porting/tools/check_lang.py            # 加 --apply 才写文件
#    validate_phase4_data.py 期望 "RESULT: ALL CHECKS PASS"，并会与主树逐字节比对
#    （单一事实源自检）；**不加 PYTHONIOENCODING=utf-8 会在 GBK 控制台报 UnicodeEncodeError**
#    （脚本第 128 行的 "↔" 字符，属显示问题，不是校验失败）
#    check_lang.py 期望 "OK: 5 language files parse; ..."，当前输出 274 键 × 5 语言
```

**沙盒编译（不跑 gradlew 时的等价验证，FIX 批常用）**：用
`porting/neoforge-1.21.1/build/moddev/artifacts/neoforge-21.1.252-merged.jar` +
`porting/neoforge-1.21.1/build/moddev/gameTestServerLegacyClasspath.txt`
（**注意：原 `clientLegacyClasspath.txt` 已改名**）拼 classpath 直接 `javac`：

```bash
CP="$(cygpath -w build/moddev/artifacts/neoforge-21.1.252-merged.jar);$(cat build/moddev/gameTestServerLegacyClasspath.txt | tr -d '\r' | paste -sd';' -)"
find src/main/java -name "*.java" | tr '/' '\\' > files.txt
javac -nowarn -proc:none -encoding UTF-8 -cp "$CP" -d /tmp/out @files.txt
```

### 4.4 进程管理（血的教训，务必遵守）

**`runClient` / `runServer` 是"用户自己在玩"的窗口 —— 不要碰。**
事故 I1：早期冒烟脚本按 `neoforge-1.21.1` 宽泛匹配杀 `java.exe`，**误杀了用户的游戏窗口**。

**冒烟/浸泡进程的精确杀法**（脚本已内置，手工清理时照抄）：

```bash
# 只杀冒烟/浸泡专用的 JVM（-D 标记经 argfile 传入，故按 argfile 文件名匹配）
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'SmokeRunVmArgs' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }"

# 以及包装它们的 gradle 启动进程
powershell -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { \$_.CommandLine -match 'gradlew(.bat)?\"? run(Client|Server)Smoke' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force -ErrorAction SilentlyContinue }"
```

**安全设计（不要改回去）**：

- 冒烟用**专用 run 配置** `clientSmoke` / `serverSmoke`（`build.gradle` 内），
  工作目录是独立的 `run-smoke/`（用户手动玩的是 `run/`），并带 `thkaguyamod.smoke=true` 标记。
- 杀进程只匹配 `SmokeRunVmArgs`（ModDevGradle 用 argfile 传参，`-D` 标记不在命令行上，
  这是 I1 修复的关键发现）。
- **禁止**用 `pkill java`、`taskkill /IM java.exe`、按目录名匹配等宽泛方式。

### 4.5 子代理并发纪律（若继续用多代理编排）

- **稳态并发上限 10 路**（13 路触发 429 限流，事故 I3）。
- 单批次规模：**≤30 个文件或 ≤4000 行**，超出先拆批。
- 每路超时 2h；超时/429/520 一律**无损恢复**（resume 保留上下文）。
- 简报必须包含「**立即逐文件落盘**」指令（事故 I4/I12：大分析批攒报告到超时 → 零产出）。
- 权限边界：flash 禁改需求/注册名/`porting/*.md`、禁 git 提交、默认禁 gradlew
  （仅 FIX 批明确授权 `compileJava`）。
- 完整协议见 `porting/ORCHESTRATION.md`。

---

## 5. 关键决策与设计思路

> 这一节是**为什么这么做**的记录。改代码前先读这里，避免推翻已经裁决过的取舍。

### 5.1 命名与注册

| 决策 | 理由 |
|---|---|
| **modid 全小写 `thkaguyamod`** | 1.7.10 的 `THKaguyaMod` 含大写，不是合法的现代 modid（FML 要求 `[a-z][a-z0-9_]{1,63}`）。选 `thkaguyamod` 是因为它**与原模组的资源域 `assets/thkaguyamod/` 完全一致**，迁移成本最低 |
| **注册名用干净 snake_case** | 1.7.10 的 `stringHumpToUnderline` 有 bug（`SilverKnife`→`silverknife`、`YinYangOrb`→`yin_yangorb`，只有重复大写字母才插下划线）。因 modid 已变、**存档不可迁移**，索性用干净命名（`silver_knife_blue`、`yin_yang_orb`）。新旧对照表：`porting/items_phase3.json` |
| **metadata 变体 → 独立物品/方块** | 1.21.1 惯例。1.7.10 的 19 个 `shot_item` meta、8 个珍珠 meta、4 个银刀 meta、8 个小神灵颜色 meta 等全部拆成独立注册项 → 126 物品 + 10 方块物品 |
| **`DeferredRegister` + `Supplier<T>` 工厂** | R2 规则：禁用 `cpw.mods.fml` 与已移除 API。所有注册集中在 `init/` 包，`ModItems` 等类只声明、不初始化 |
| **符卡/特殊弹注册表取代静态字段** | 1.7.10 的 `THSpellCard` 有静态 `instance` 字段（每 tick 反射 `newInstance`）。改为 `SpellCardRegistry`（编号 → 工厂）与 `SpecialShotRegistry`（特殊弹 ID → 实例），消除反射与串扰风险（R5 性能项；`P17` 仍在复核该静态字段的残留影响） |
| **NBT → DataComponent** | 弹幕参数（`Number/DanmakuForm/Speed/Gravity/Special/Color/Infinity`）→ `ModDataComponents.DANMAKU`（`DanmakuParams` record，含 `CODEC` + `STREAM_CODEC`）；另有 `CHARGE` / `GAP_COLOR` / `ORIGINAL_DAMAGE`。**1.21.1 没有数字物品 ID**，所以依赖数字 ID 的还原链（诅咒替身人偶）无法等价实现（P11） |

### 5.2 行为等价与现代化（`MIGRATION_RULES.md` 的核心）

**总原则：行为等价 + 实现现代化优先。** 与"逐行忠实"冲突时以规则为准。四条要点：

1. 「忠实移植」指**行为等价**，不阻碍现代化替换（`Optional`、enhanced-for、`try-with-resources` 等）。
2. 反射 hack / 已移除 API / 公共代码引用客户端类 → 一律换现代机制并注释。
3. **发现旧版 bug（自递归、off-by-one、服务端引用客户端类）→ 修复并注释，不得照搬。**
4. 每 tick 分配热点、无缓存反射、字符串查表 → 顺手修复并在报告列出。

### 5.3 事故沉淀出的五条硬规则（最容易踩的坑）

#### 规则 1：`canBeCollidedWith` 三职拆分（I8/I9，P40 已固化为规则）

1.7.10 的 `canBeCollidedWith` **身兼三职**：可推挤碰撞 / 可被准星选取 / 可被弹丸命中。
1.21.1 拆成三个方法，且**原版生物不再覆写 `canBeCollidedWith`（恒 false）**。映射规则：

| 1.7.10 用途 | 1.21.1 方法 | 后果（若漏搬） |
|---|---|---|
| 命中扫描过滤器 | `canBeHitByProjectile()` | 弹幕/激光/符卡对怪物**零伤害** |
| 准星选取（打拳反弹、攻击破坏、回收） | `isPickable()` | 准星永远选不中，交互全废 |
| 实体推挤 | `canBeCollidedWith()` | 注意：设为 true 会触发 **I14 自动匍匐** |

**接手提示**：新增任何"可被击中/可被瞄准"的实体，**三个方法都要想一遍**。

#### 规则 2：NBT 缺键归零（I19）

1.21.1 的 `/summon` 统一走 `Entity.load()` → `readAdditionalSaveData()`，而
**`CompoundTag.getShort/getFloat/getInt` 缺键返回 0**（1.7.10 的 `/spawnmob` 不走 NBT，所以原作无此问题）。
后果：召唤时构造器从样式表算出的 `danmakuSpan`/`speedRate` 等被 **0 全覆盖**（射速爆炸 + 弹卡体内）。

**规则**：`readAdditionalSaveData` 里每个读取都要 `contains(key)` 守卫。
普查结论（`staging/audit-nbt-zero/`）：27 个覆写类中实害 5 处、安全 8 处、存疑 2 处（P50/P51）、空实现 9 处。

#### 规则 3：数据包资产要验证"加载后生效"（I18）

群系修饰器 JSON 少一层 `neoforge/` 目录 + `biomes` 写成字符串数组 → **注册表静默为空，不报错不告警**，
自阶段 3 起自然生成**从未生效**，而连续多轮 smoke/浸泡全程"0 相关 ERROR"。

**规则**：NeoForge 数据包注册表路径由 `CommonHooks.prefixNamespace("neoforge:biome_modifier")` 推出
→ `data/<ns>/neoforge/biome_modifier/`。`biomes` 必须是 HolderSet 对象形式
（`{"type":"neoforge:or","values":[...]}`），字符串数组会被 `HolderSetCodec` 拒绝（服务端启动即抛异常）。
**验证方式**：用 GameTest 断言生成表内容（`BiomeSpawnGateTest`），而不是"文件存在 + 没报错"。

#### 规则 4：原版机制差异（I13/I14/I15）

| 机制 | 1.7.10 | 1.21.1 | 应对 |
|---|---|---|---|
| `move()` 着地（I13） | 吃掉被阻运动，垂直速度清零 | **不清零** → 照抄"着地补偿 +0.06"会变成净上浮（永久微颤） | 着地时显式 `motionY = -0.03`（取 0 会让 `onGround` 掉 false 变交替振动） |
| 自动匍匐（I14） | 无此机制 | `Player.updatePlayerPose()` 站/蹲放不下时兜底 `Pose.SWIMMING`，会把实体碰撞盒算入 | 弹幕/效果实体 `canBeCollidedWith → false`（瞄准/命中不受影响） |
| 渲染插值（I15） | 无插值 | `yRotO` 来自网络包（已正规化），素 lerp 会把 360° 整数倍整段走完 | 用 `Mth.rotLerp`（1.21.1 自身惯例） |
| 速度同步（I17） | `sendsVelocityUpdates=true` | 默认不发送 → 客户端 `motion` 恒 0，`atan2(0,-0)` 恒 180° | 保留服务端同步的 yaw；或恢复速度同步 |
| `maxEntityCramming`（P45，未决） | 无 | 默认 24，密集怪群会互相挤压致死 | **待裁决**（见 §3.2） |

#### 规则 5：BOSS 伪死亡 / `die()` 拦截

多段 BOSS（早苗、咲夜、神子等）在 1.7.10 里 `die()` 被拦截以推进阶段。
1.21.1 的 vanilla 只在 `dead == true` 时跑 `tickDeath()`，因此移植用
`canInterceptDeath()` + `danmakuDying` 标志在 `tick()` 里手动回滚 `tickDeath()`。
**改 BOSS 死亡逻辑前先读 `EntityDanmakuMob` 的这段注释**。

### 5.4 有意偏离旧版的清单（改之前先确认，全部有文档登记）

| 项 | 偏离内容 | 登记位置 | 可回退性 |
|---|---|---|---|
| **核弹地形破坏**（I16） | 原作**从未接线**（爆炸破坏分支因 `special` 恒 0 不可达，有字节码证据）→ 新版按设计意图实现，受 `Master Spark Destroys Blocks` 门控（默认开） | `USER_MIGRATION.md` §3.4；台账 I16 | 高（单点回退 + 配置开关） |
| **扫帚操舵限速 cap**（P33） | 原作限速是笔误（单键 0.2~0.5 抖动、全键同按发散）→ 修复为恒定 0.792（比旧版均值约 +10%） | `USER_MIGRATION.md` §3.4 + `BroomSteer` javadoc；台账 P33 | 高（常量） |
| **扫帚操舵客户端权威** | 移动计算在客户端（与原版船同级），非旧版的"视线前进" | `USER_MIGRATION.md` §5#1 | 中（需 Phase 2 服务端镜像包，已裁决放弃） |
| **万宝槌地牢战利品**（P20/P22） | 原作 `registerChestItem` 是**空方法**（地牢战利品从未生效）→ GLM 实现设计意图，概率 0.77%（原设计 5%） | 台账 P20/P22；`USER_MIGRATION.md` §5#18 | 高（数据 JSON） |
| **万宝槌 GUI 入口**（P8） | 原作潜行右键开 GUI 的代码被**注释掉**（死代码）→ 移植恢复该入口 | `ItemUchide` javadoc | 高（删 4 行） |
| **万宝槌外部 MOD 配方删除**（P8） | `item.alicedoll.bare`（外部 MOD）无 1.21.1 版 → 整条转换删除；`roukanSenpuuzin` 用 `ROUKANKEN` 占位。**替身人偶的"还原成原素材"链已实装**（`ORIGINAL_ITEM` 组件，`8ce39ff`） | `USER_MIGRATION.md` §5#8/#9 | — |
| **`recipelessItemsRecipe` 的 8 条配方** | 原作默认关闭 → 转成常规 `_bonus` 配方（保证物品可获取） | `phase3-registration.md` | 高（JSON） |
| **四类音效** | 原作 `.ogg` 缺失 → 用原版音效占位 | `USER_MIGRATION.md` §5#17 | — |
| **保留的原作 bug（不修）** | 模型度/弧度混用 3 处（P27）、`THSC_Fafurotskies_no_Kiseki` 参数误用（P37）、隙间 `warp*` 不持久化（P51）、`EntityCirnoIceBox` 尺寸不持久化（P50） | 各 P 项 | — |
| **已硬化的原作 bug（修复）** | `RenderTHBoss` HP 条 UV NaN → `max(getMaxHealth(),1F)`（P38）；`ItemThirdEye` 单例字段多玩家串话 → 无状态即时显示；`DanmakuLaserCraftingMenu` shift 槽段错位 → 按真实槽数修正 | FIX-3 / `8ce39ff` | — |

### 5.5 子代理编排协议要点（`ORCHESTRATION.md`）

- **K3（主控）** 负责：需求拆解、接口契约冻结、依赖排序、裁决、合并、验收、git 提交。
- **flash（子代理）** 负责：明确范围内的移植/修复，**不做架构决策**；冲突或接口不清 →
  留 `// TODO K3-review` + 报告上报。
- **派发前提**：主树处于稳定可编译态、与在途批次**无文件重叠**、依赖件已冻结为契约、并发 < 10。
- **落盘约定**：移植批写 `porting/staging/<批次>/`（**包名即最终包名，合并 = 拷贝**）；
  FIX 批直接改主树。
- **报告六要素**：变更清单（文件 + 行数 + 一句话）/ 验证方式与结果 / 假定 API 清单 /
  TODO 与降级点 / 需 K3 接线项 / 未决风险。
- **纪律（血的教训）**：
  - 「**立即逐文件落盘**」—— 不要攒到最后（I4/I12：多个分析批 2h 超时零产出）。
  - 保留既有 `TODO K3-review` 标记与**日文注释原文**。
  - 禁改 `porting/*.md`、禁改注册名、禁 git、默认禁 `gradlew`。
  - 超时/429/520 → **无损恢复**（resume 保留上下文），不要重头再来。

### 5.6 接手后的建议动作顺序

1. 读 `porting/integration-notes.md`（台账，唯一事实源）+ 本文。
2. 跑一次 `compileJava` → `runGameTestServer`（应 **115/115**）→ `smoke_test.sh`（应 **SMOKE PASS**），
   确认环境可用。工作区是干净的，**无需清理**。
3. 跟进 §1.3 的"待用户复测"清单（I18 野外生成 / I17 小刀朝向 / I10 魔人镜冠 / §5#4 刀弹外观）
   —— 这四项是发布前最后的验证缺口。
4. 从 §3.2 的 **P45（挤压致死）** 与 §3.1 的可选补丁裁决点入手 —— 这两处最影响玩家体验；
   **P29/A7** 已有实测裁决依据，建议下一窗口实施。
5. 任何代码改动：先看 §5.3 的五条硬规则，改完跑 §4.3 的 ⑤（数据校验）与 ③（冒烟）。
6. 改到玩家可见行为时，**同步更新 `USER_MIGRATION.md` 的 §3/§5/附录 A**。

---

## 附录：编写时点的核对记录（**已全部闭合**）

本文件最初撰写时仓库里有多批在途，当时记录过 7 处"文档 ↔ 仓库"不一致。**这些现已全部闭合**
（`8ce39ff` 一并修正了 `RELEASE_NOTES.md` / `integration-notes.md` / `USER_MIGRATION.md`），
下表只作追溯留档，**不再需要任何行动**：

| # | 当时的差异 | 现状 |
|---|---|---|
| 1 | `RELEASE_NOTES.md` 的 GameTest 计数（58）落后于实测 | ✅ 已改为 **115/115** |
| 2 | `RELEASE_NOTES.md` 文档索引写 "P1-P42 / I1-I12" | ✅ 已改为 **P1-P53 / I1-I19** |
| 3 | `USER_MIGRATION.md` §5#22 与台账 P47/P49 的"无限次/12 次"矛盾 | ✅ 已按"永久缺货 + 移植版 12 次 / 旧版 7 次"统一；次数差异保留为 P49 待裁决 |
| 4 | 4 个 staging 批次"待裁决" | ✅ 状态已在 §3.1 明确（均为**可选补丁**，非阻塞） |
| 5 | 台账 P47 措辞"原作交易本就无限次" | ✅ 已由 P49 修正（原作也是有限次） |
| 6 | `RELEASE_NOTES.md` "46 个实体类型" | ✅ 核对一致，无需改动 |
| 7 | `ORCHESTRATION.md` §8 "无在途代理" | ✅ 已符合事实（在途批次零） |

**仍有效的 4 条环境/文档提醒**（文档未写、但接手会踩）：

1. `porting/tools/validate_phase4_data.py` 必须在**仓库根**执行（脚本按相对路径定位
   `porting/staging/data`），且 GBK 控制台需 `PYTHONIOENCODING=utf-8`，否则在第 128 行因
   `↔` 字符抛 `UnicodeEncodeError`（**不是校验失败**，加环境变量后 `RESULT: ALL CHECKS PASS`）。
2. `porting/neoforge-1.21.1/build/moddev/` 下的 classpath 文件**只有 `gameTestServerLegacyClasspath.txt`**
   （`clientLegacyClasspath.txt` 已改名；另有 `gameTestServerRunProgramArgs.txt` / `RunVmArgs.txt`）。
3. `porting/neoforge-1.21.1/build.gradle` 里 `clientSmoke` / `serverSmoke` 在 `runs {}` 块内
   （约 63-76 行），`gameTestServer` 在约 86 行 —— 查 run 配置时两处都要看。
4. **jar 文件数的两种口径**：`unzip -l` 汇总行的 `860 files` **含目录条目**，实际文件条目是
   **803**。`RELEASE_NOTES.md` §4 仍写 "832 文件"（更早一版的核验值）—— 属于历史记录口径差异，
   不影响发布，接手若更新该行建议统一为"860 条目 / 803 文件（含 115 个测试类）"。
