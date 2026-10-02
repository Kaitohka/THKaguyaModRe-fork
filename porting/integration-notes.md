# 阶段 4 集成分类账（K3 裁决记录 / 跨上下文持久化）

> 规范：合并评审一律对照 `porting/MIGRATION_RULES.md`（现代化优先 + 八条验收）。

## 已裁决

| # | 事项 | 裁决 | 状态 |
|---|---|---|---|
| D1 | `THSpellCard.shot(int,int,int)` 自递归（1.7.10 崩溃级 bug） | 修复为 `shot(form,color,delay,80)` | ✅ 已修（a976221） |
| D2 | `getCircleColor()` vs `getSpellCardCircleColor()` | 双入口委派，保留 | ✅ |
| D3 | `THSpellCard.rand` 类型 Random→RandomSource | 保留 RandomSource | ✅ |
| D4 | `rod_of_remorse` 的 damage 0/1 状态标记 | 暂用 durability(2) 占位；阶段 5 转组件+模型 override | ⏳ 阶段5 |
| D5 | 音效 .ogg 缺失 | 4 事件注册，原版音效占位 | ✅ |

## 待裁决 / 待办（合并各批时核对）

| # | 事项 | 来源 | 处置 |
|---|---|---|---|
| P1 | `EntitySakuyaStopWatch` 必须 `extends EntitySakuyaWatch`（共用 ModelPrivateSquare 泛型约束） | agent-20 | 合并 agent-17 实体批时核对/调整继承 |
| P2 | `RenderTHShot` FORM_KNIFE 分支需独立 bake 的 `ModelSilverKnife` 部件（不能每帧 new） | agent-20 #2 | 渲染器注册时处理（ModelLayerLocation silver_knife 注入） |
| P3 | 符卡「Charge」校验组件化（早苗卡 meta14-18） | agent-12 TODO#3 | 符卡物品批（agent-25）合并时对接 CHARGE 组件 |
| P4 | 符名聊天展示键：旧 `item.thSpellCard.<num>.name` vs 新 `item.thkaguyamod.spell_card_<name>` | agent-12 TODO#4 | 用新键（THKaguyaLib 宣言处改为按注册名查键） |
| P5 | `EntityPendulum` 按本地化名称搜方块 → 改注册名比对 | agent-17 批内已要求 | 合并时确认 |
| P6 | 火鼠裘无限耐久 → 当前无 durability 属性即无限（ArmorItem 构造器会强制材料耐久？需实测） | 阶段3遗留 | 游戏内核对 |
| P7 | THSC 降级 5 处（EntitySukima/MiniHakkero/SakuyaWatch/DanmakuCreeper/DanmakuMob 依赖） | agent-13 | B4/B5 批合并后恢复注释掉的调用 |
| P8 | 万宝槌外部 MOD 配方删除、`roukanSenpuuzin` 暂用 ROUKANKEN | agent-19 | ✅ 已落实（外部配方删除并注释；ROUKANKEN 占位） |
| P23 | 商店交易列表下发：vanilla 只认 MerchantMenu | agent-19 阻塞上报 | 裁决方案(a)：新增 clientbound UpdateSanaeOffersPayload（MerchantOffers.STREAM_CODEC），K3 已实现并注册 |
| P24 | 商店交易预览硬编码（aja_red_stone/护身符×5 与真实交易表不符） | agent-19 风险#4 | 保持原行为（1.7.10 即硬编码），发布说明注明；若判定 bug 后续改 offer.getResult() |
| P25 | 商店箭头按钮贴图在 1.21.1 villager.png 已不存在 | agent-19 风险#5 | 用默认按钮+文本；阶段 5 美术还原可选 |
| P9 | 模型层注册（RegisterLayerDefinitions）+ 防具自定义模型钩子（IClientItemExtensions） | agent-20 #5 | 渲染批合并时统一做 |
| P10 | `ItemOnbashira` 挖掘速度 3.0 恒值在 1.21.1 无 item 覆写点 | agent-10 TODO | 用属性组件或 Tool 规则补，阶段 5 |
| P11 | `ItemCursedDecoyDoll` 的 OriginalItemID 还原需新组件 ORIGINAL_ITEM（物品注册名字符串） | agent-18 | 合并道具实体批时加组件并对接万宝槌还原 |
| P12 | `sakuyas_pocket_watch` damage 当模式位 → 需 durability(2)（同 kaigo 处理） | agent-18 | 生成器表改，合并物品批时 |
| P13 | 早苗符卡 Charge 守卫在 THKaguyaLib.checkSpellCardDeclaration 里恒通过 | agent-18/25 | K3 在库侧补 CHARGE 组件判定（物品侧已有重复判定，库侧补全后删物品侧） |
| P14 | 发射器弹物品发射者改用 ArmorStand 占位（原 EntityFamiliar） | agent-25 | living 批合并后换回 EntityFamiliar |
| P15 | 玉枝 6 段充能图标 / 折伞双层染色需模型 JSON ItemProperties | agent-18 | 阶段 5 客户端润色 |
| P16 | RenderSukima 的 (SRC_ALPHA,SRC_ALPHA) 混合模式 DanmakuRenderTypes 缺失 | agent-22 | 合并渲染批时在 DanmakuRenderTypes 增加该模式 |
| P17 | RenderSpellCard.getLevel 用 SpellCardRegistry 工厂实例化（THSpellCard 有静态 instance 字段） | agent-22 | 复核：工厂每次 new，静态 instance 语义需确认无串扰 |
| P18 | SakuyaStopWatchTexture.png 原作声明但从未使用 | agent-22 | 保持现状（用 SakuyaWatchTexture），文档化 |
| P19 | 生成条件双采样风险：ModSpawns 谓词 vs 实体 checkSpawnRules | agent-26 | **配置读取集中在 ModSpawns**；生物批（agent-16）实体 checkSpawnRules 不得再读 ModConfig/概率条件——合并时强制审查删除 |
| P20 | 万宝槌地牢注入概率 0.77% vs 原作意图 5% | agent-26 | 保持 GLM 现状（干净、可回退）；发布说明记录偏差 |
| P21 | iceMountains → #c:is_snowy 略宽 | agent-26 | 接受（21w40a 合并群系所致），文档化 |
| P22 | 原作 registerChestItem 为空方法（地牢战利品从未生效） | agent-26 | GLM 实现的是设计意图；发布说明注明 |

## 批次状态板

| 批次 | 代理 | 范围 | 状态 |
|---|---|---|---|
| 弹幕核心 | agent-7(K3) | entity/shot 11 + THShotLib + 3 掉落物 | ✅ 合并 |
| 渲染器(弹/激光) | agent-8 | RenderTHShot/RenderTHLaser/DanmakuRenderTypes | ✅ 合并 |
| 合成台 GUI | agent-9 | 2 Menu + 3 Slot + 2 Screen | ✅ 合并 |
| 简单物品 | agent-10 | 14 行为物品 | ✅ 合并 |
| 射击物品 | agent-11 | ItemTHShot/Laser/ShotMaterial | ✅ 合并 |
| 符卡基础 | agent-12 | THSpellCard/EntitySpellCard/Registry/THKaguyaLib/魔法阵 | ✅ 合并 |
| 35 符卡 | agent-13 | THSC_* ×35 | ✅ 合并 |
| FIX-1 | agent-14 | 物品编译修复 | ✅ 收敛 |
| FIX-2 | agent-15 | 符卡编译对齐 | ✅ 收敛（全树编译归零） |
| 道具模型 | agent-20 | 10 模型（几何已运行时核验） | ✅ 交付，⏸ 待实体批后合并 |
| 生物实体 | agent-16 | living 16 + DanmakuPatternRegistry | 🏃 |
| 道具/效果实体 | agent-17 | item 13 + effect 5（含时停） | 🏃 |
| 武器物品 | agent-18 | 28 类（429 恢复 1 次） | ✅ 交付（stub javac 0错），⏸ 待 agent-17 实体后合并 |
| 商店/万宝槌 GUI | agent-19 | 2 Menu + 2 Screen + Payload ×2 | ✅ 合并（编译0错）；打开入口待 EntitySanae/ItemUchide |
| 人物模型 | agent-21 | 16 模型 | 🏃 |
| 道具渲染器 | agent-22 | 18 个 | ✅ 交付（javac 0错），⏸ 待实体批后合并；贴图已按其约定归位 entity/shot/ |
| 效果渲染器 | agent-23 | 4 个（429 恢复 1 次） | ✅ 交付（javac 验证），⏸ 待实体批后合并；缺贴图已补(miraclecircle/hakuroureflect) |
| 生物渲染器 | agent-24 | 14 个 | 🏃 |
| 符卡物品+杂项+发射器 | agent-25 | （429 恢复 1 次） | ✅ 物品已合并（b252bed）；ModDispenserBehaviors ⏸ 待 SILVER_KNIFE |
| 数据 JSON | agent-26 | 14 biome_modifier + GLM + ModSpawns + 生成器/校验器 | ✅ 交付（全校验通过），⏸ 待生物批后合并 |

## 回归闸门记录

| 时点 | 范围 | 结果 |
|---|---|---|
| 阶段3 | build+runClient | PASS（0 配方/模型错误） |
| FIX 批后（a976221） | build+runClient（smoke_test.sh） | PASS（主菜单到达、0 相关 ERROR） |
| 环境修复后 | 双模式 smoke（R3闸+client+server） | **PASS 双端全绿** |

## 代理协调文件

- `porting/tools/staging_modentities_extra.txt`：agent 自建的 ModEntities 追加条目暂存（compile_staging.sh 沙盒用）。**合并时以各代理报告为准，此文件仅供参考**
- `porting/tools/staging_blocked.txt`：跨批依赖阻塞清单
- `porting/.staging-build/`：agent 编译验证沙盒（已 .gitignore）

## 事故台账

| # | 事故 | 根因 | 处置 | 状态 |
|---|---|---|---|---|
| I1 | 用户客户端"进世界崩溃" | smoke_test.sh 清理步骤按 'neoforge-1.21.1' 宽泛匹配杀进程，误杀用户窗口 | 专用 run 配置 clientSmoke/serverSmoke + 独立 run-smoke/ 目录；MDG 经 argfile 传参致 -D 标记不上命令行 → 改按 argfile 文件名 `SmokeRunVmArgs` 精确匹配（已实测验证） | ✅ 修复 |
| I2 | 专用服务端无法启动 | 本机 Steamcommunity302 代理 TLS 拦截，Java 信任库不认 libraries.minecraft.net（netty epoll 下载 PKIX 失败） | 构建工程信任库（cacerts+代理CA链）`porting/tools/thkaguya-truststore.jks`，复制到 `~/.gradle/` 并配 `org.gradle.jvmargs` trustStore | ✅ 服务端 Done(5.4s) |
| I3 | 429 限流 ×3（agent-18/23/25） | 13 路并发超每分钟 21 次请求上限 | 无损恢复；稳态并发 ≤10；批次空位补入 | ✅ 台账 |
| I4 | 2h 超时 ×6（agent-16/17/19/21/24/26） | 大文件批次分析阶段过长（agent-16/24 超时前零落盘） | 全部无损恢复；简报补"立即逐文件落盘"指令；超大批次今后先拆小 | ✅ 恢复中 |
