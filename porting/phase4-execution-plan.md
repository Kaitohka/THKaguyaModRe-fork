# 阶段 4 执行规划（K3 主控派发基准）

> 用途：引导 deepseek-v4.1-flash 子代理完成具体编程实现。每一步给出输入、预期输出、技术要点、验收标准。
> 纪律：flash 只执行分配批次，不做架构决策；接口不清/冲突 → 留 `// TODO K3-review` 并上报，由 K3 裁决。
> 统一约束：Mojmap 映射；NeoForge 21.1.252；Java 21；旧行为/数值/接口忠实保留；不改注册名。
> 构建：`cd porting/neoforge-1.21.1 && JAVA_HOME="/c/Program Files/Java/zulu-21" ./gradlew compileJava`
> 回归：`bash porting/tools/smoke_test.sh`（PASS=构建通过+主菜单到达+0 相关 ERROR）
> 裁决台账：`porting/integration-notes.md`（每次合并前必读 P 项）。

## 步骤 0：在途批次收尾（进行中，无需新派发）

10 个在途批次（agent-16/17/18/19/21/22/23/24/25/26）产出至 `porting/staging/`。K3 负责按本规划顺序合并。

## 步骤 1：道具/效果实体合并（agent-17 交付后）

- **输入**：`staging/entities2/thkaguyamod/entity/{item,effect}/*.java`（18 类）；台账 P1/P5。
- **输出**：主树合并 + `ModEntities` 追加 18 条目（注册名/tracking/updateInterval 按 1.7.10 CommonProxy.java:158-216）+ 编译 0 错。
- **技术要点**：`EntitySakuyaStopWatch extends EntitySakuyaWatch`（P1 泛型约束，若 agent-17 未这么做则调整）；掉落物类不加 noSave，效果类加；时停协议 `tickCount`/`lastTime` 原语义；`connection.teleport` 玩家回传。
- **验收**：compileJava 0 错；`EntitySakuyaWatch` 的时停逻辑与 1.7.10 逐行等价。
- **执行者**：K3 合并；编译错误若多 → flash 修复批（FIX-3）。

## 步骤 2：生物实体合并（agent-16 交付后）

- **输入**：`staging/living/`（16 类 + DanmakuPatternRegistry）。
- **输出**：合并 + `ModEntities` 追加 living 条目（带刷怪蛋颜色记录于注释）+ 属性注册（`EntityAttributeCreationEvent`：`EntityDanmakuMob` 系需注册 Attributes：maxHealth/movementSpeed/followRange——1.7.10 用 `registerAttributes` 旧式，1.21.1 必须事件注册否则崩溃）。
- **技术要点**：PathfinderMob/Monster/Villager 基类映射；自研 tick 移动保留；BOSS tickDeath 阶段切换；村民职业占位 NONE + TODO；`getCanSpawnHere`→`checkSpawnRules`。
- **验收**：compileJava 0 错；属性事件注册覆盖全部 living 实体（漏注册=生成即崩，必须逐一核对）。
- **执行者**：K3 合并 + 接线；flash 修复批兜底。

## 步骤 3：模型与渲染器合并（agent-20/21/22/23/24 交付后）

- **输入**：`staging/models1/`（10）、`staging/models2/`（16）、`staging/render_items/`（18）、`staging/render_effect/`（4）、`staging/render_living/`（14）。
- **输出**：
  1. 合并至 `client/model/`、`client/model/living/`、`client/renderer/`（含 living/effect 子包）。
  2. `THKaguyaClient` 追加：`EntityRenderersEvent.RegisterLayerDefinitions` 注册全部 26 个 LayerDefinition（ModelLayerLocation 名单见两批报告）；`RegisterRenderers` 追加 36 个实体渲染器（替换占位 NoopRenderer）；`RenderSpellCardCircle` 替换 SPELL_CARD_CIRCLE 的 Noop。
  3. 小神灵方块 `BlockEntityRenderer`（发光面片 TESR 移植）——若渲染批未含，K3 补写（参考旧 TileEntityDivineSpiritRenderer，三层加色混合面片 + worldTime 正弦浮动）。
  4. 台账 P2：RenderTHShot 的 FORM_KNIFE 改用 bake 后的 ModelSilverKnife 部件。
- **技术要点**：渲染器构造器 `(EntityRendererProvider.Context)`；`context.bakeLayer(location)`；贴图路径全小写（已就位）；`LivingEntityRenderer` 泛型 `<EntityXxx, ModelXxx>`。
- **验收**：compileJava 0 错 + smoke PASS；游戏内 summon 各实体无渲染崩溃（缺失渲染器会崩——注册表逐一核对覆盖率）。
- **执行者**：K3 接线；flash 修编译。

## 步骤 4：武器/符卡/杂项物品合并（agent-18/25 交付后）

- **输入**：`staging/items2/`（28 类）、`staging/items3/`（ItemTHSpellCard + ItemHisyaku + ItemBuddhaStoneBowl + ModDispenserBehaviors）。
- **输出**：
  1. 合并至 `item/`、`init/ModDispenserBehaviors.java`。
  2. 生成器 `build_phase3_assets.py` 表追加 cls 绑定（28+2 类），重新生成 ModItems。
  3. 符卡物品绑定：35 个 `spell_card_*` → `ItemTHSpellCard(props, spellCardId)`（生成器 spellcard kind 处理：card_entries 顺序 = 旧数字 ID）。
  4. `ModDispenserBehaviors.register()` 接入 `THKaguyaMod` 构造器（FMLCommonSetupEvent 后或直接构造末尾——DispenserBlock.registerBehavior 线程安全，直接调用即可，加注释）。
  5. 台账 P3/P4：符卡宣言处 Charge 组件对接 + 符名键改新注册名。
- **验收**：compileJava 0 错 + smoke PASS + 游戏内右键小弹/激光/符卡可见弹幕（K3 或用户实测）。
- **执行者**：K3 生成器+接线；flash 修编译。

## 步骤 5：商店/万宝槌 GUI + 网络注册（agent-19 交付后）

- **输入**：`staging/gui2/`（2 Menu + 2 Screen + SelectSanaeTradePayload + Slot）。
- **输出**：
  1. 合并 `menu/`、`client/gui/screen/`、`network/`。
  2. K3 新建 `init/ModNetworking.java`：`RegisterPayloadHandlersEvent` → `PayloadRegistrar` 注册 SelectSanaeTradePayload（SERVER 向，play 阶段），处理函数接菜单 `setSelectionHint`。
  3. `ModMenus` 追加 MERCHANT_SANAE / MIRACLE_MALLET；`THKaguyaClient` 追加两个 Screen 注册。
  4. 万宝槌方块打开方式：1.7.10 由 ItemUchide 右键打开（guiMiracleMalletID=3）——物品行为在 agent-18 批；对接确认。
- **验收**：compileJava 0 错 + smoke PASS + 商店翻页按钮发包实测（K3 实测或 GameTest）。
- **执行者**：K3 网络/注册接线；flash 修编译。

## 步骤 6：数据 JSON 合并（agent-26 交付后）

- **输入**：`staging/data/`（biome_modifier JSON ×11 + GLM 战利品 + ModSpawns 建议）。
- **输出**：JSON 入 `src/main/resources/data/`；K3 按报告决定 SpawnPlacements 接入（`RegisterSpawnPlacementsEvent`）；GLM 若方案存疑 → 改战利品表注入事件或延后，记台账。
- **验收**：smoke PASS + 游戏内对应群系可见妖精自然生成（实测）。
- **执行者**：K3。

## 步骤 7：符卡降级恢复（依赖步骤 1/2）

- **输入**：台账 P7；THSC_HikouchuuNest/MasterSpark/SatuzinDoll/Fafurotskies/GagoujiCyclone 中被注释的实体调用；实体已合并。
- **输出**（flash 批 FIX-4）：恢复 5 处注释调用，构造器按实体实际签名；删对应 TODO。
- **验收**：compileJava 0 错；逐卡核对恢复点与 1.7.10 原代码一致。
- **执行者**：flash。

## 步骤 8：客户端润色项（flash 批，步骤 3 之后）

- **输入**：台账 P9；旧 `ItemMarisaHat/ItemKerobou` 的 getArmorModel/getArmorTexture；旧 `TileEntityDivineSpiritRenderer`。
- **输出**：`client/ModClientExtensions.java`（`IClientItemExtensions.getHumanoidArmorModel` 返回 bake 的帽子模型）+ 防具层贴图路径修正 + 小神灵 BER + `EntitySpellCardCircle`/`EntityMiracleCircle` 渲染器接线确认。
- **验收**：戴帽游戏内可见模型（实测）；compileJava 0 错。
- **执行者**：flash。

## 步骤 9：弹幕垂直切片世界内冒烟（K3 + 用户）

- **输入**：步骤 1-6 全部合并后的构建。
- **验收清单**（逐项打勾）：
  1. 创造栏取小弹 → 右键发射 → 可见白色小弹飞出（渲染管线验证）。
  2. 弹幕命中生物有伤害（命中判定验证）。
  3. 弹幕合成台右键打开 GUI → 放素材→出成品→参数写入组件（GUI+组件验证）。
  4. 符卡右键宣言 → 魔法阵+符卡实体+音效（符卡链路验证）。
  5. 咲夜怀表 → 周围实体静止（时停验证，C 级风险项）。
  6. 激光物品 → 激光渲染。
- **执行者**：K3 引导，用户实测（或 GameTest 自动化，视可行性）。

## 步骤 10：阶段 4 验收文档 + 提交

- **输出**：`porting/phase4-interactions.md`（功能清单逐项验收表 + 已知问题 + 网络/GUI/事件/配置核对）；git commit。
- **执行者**：K3。

## 阶段 5 预告（不在本规划派发范围）

DataFixer 评估（结论大概率"不支持旧存档"并文档化）、全量回归、性能（弹幕实体 tick 开销、Spark 或日志计时）、发布构建、已知问题清零或登记。
