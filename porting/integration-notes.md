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
| P13 | 早苗符卡 Charge 守卫闭环 | agent-29 | ✅ 已落实并合并（库侧守卫+物品侧去重）。**更正**（charge-hint 批核实）：提示键 thKaguya.msg.sneScCharge 5 语言俱在，「键缺失」系事实错误；启用补丁已备（staging/charge-hint/），发布冻结未合并 |
| P31 | D4 悔悟之棒组件化：ROD_OF_REMORSE 的 durability(2) 去留 | polish-trio | 去掉=无耐久语义正确但叠加上限 1→64；保留=占位延续。补丁已备（staging/polish-trio/d4/），下一窗口裁决 |
| P32 | P10 御柱挖掘：字面「恒 3.0」vs 原作实机「クモの巣 3.0 / 其他 1.0」 | polish-trio | BreakSpeed 事件方案已备（staging/polish-trio/p10/），取值待裁决 |
| P33 | broom-input 操舵设计：①旧版限速笔误（单键 0.2~0.5 巡航、全键发散）忠实保留 vs cap 修复（建议修复+记差异）②Phase 2 服务端镜像包是否随下个窗口 | broom-input | **已裁决并合并**：①cap 修复（巡航恒 0.792，+10%，BroomSteer javadoc 记差异）②弃 Phase 2（零协议改动）；Phase1 已合并主树（89/89 + smoke PASS） |
| P34 | charge-hint 启用方式：默认开启+actionBar（建议）vs 配置门控 | charge-hint | 补丁与文档同步清单已备（staging/charge-hint/PREP.md），下一窗口裁决；启用时同步改 USER_MIGRATION §3.4/§5#15/附录A#9/FAQ Q2 |
| P35 | `DanmakuParams` int 字段无范围夹紧；`EntityDanmakuMob.dropShotItem` 等处 `form % 8` 负取模潜伏（当前调用点均正常量，不可达） | audit-common P1-1 | ✅ 已修（fix-p35-36）：compact 构造器统一 Mth.clamp（number 0..512/form 0..31/speed 0..64/gravity 0..16/special 0..65535/color 0..15，保留 RANDOM=8/RAINBOW=9）；单点收口覆盖全部写入路径；DanmakuParamsClampTest 5 例变异验证有效；special 上界 65535 为折中值（原作 int 无截断） |
| P36 | `EntitySpellCard.java:236` 无 null 检查调 useSpellCard（:122-141 允许其为 null）；`EntitySakuyaWatch:238`/`EntitySakuyaStopWatch:179` 解引用可能为 null 的 spellCard.user；**二轮补充**：`THSC_SatuzinDoll:90` 同族——客户端 EntitySpellCard.user 恒 null，现被同样不同步的 canMoveInTimeStop 短路挡住，一旦同步即变客户端每 tick NPE | audit-common P1-2/3 + 二轮 | ✅ 已修（fix-p35-36）：4 处 receiver 换到确定非 null 侧/显式 null 检查，语义等价，均附 P36 注释；GameTest 无法构造客户端 null 场景，语义等价核对 + 全量回归替代（110/110） |
| P37 | `THSC_Fafurotskies_no_Kiseki:69` 把 shotLimitSpeed 当模式编号 | audit-common P1-4 | 原作即如此，裁决保持现状（与 P27 同类登记） |
| P38 | `RenderTHBoss:146-147` moveDanmakuAttack(maxHP=0) → HP 条 UV 为 NaN | audit-render P1-2 | 原作即如此；可选 max(getMaxHealth(),1F)，下一窗口随渲染润色一并评估 |
| P39 | soak 闸门盲区：数据包 summon 失败不走 [ERROR] 级 → 两轮浸泡"名单全召成功"结论实际未逐条验证（I10 的 18 次异常漏报） | audit-common 发现 | soak_test.sh 增强：统计 `Failed to summon`/命令失败行；已在 FIX 批 GameTest 补 spawn 断言覆盖 |
| P40 | 1.7.10 `canBeCollidedWith` 三职拆分（碰撞/准星/弹丸命中）→ 1.21.1 的映射规则 | I8/I9 | **已立项为移植规则**：见 MIGRATION_RULES 增补建议（命中扫描→canBeHitByProjectile；准星交互→isPickable；推挤→canBeCollidedWith） |
| P41 | （已落实为 soak-v2，见批次收尾记录） | — | — |
| P42 | 伤害路径回归测试第三批：EntityTHSetLaser / EntityNuclearShot / EntityTHHenyoriLaser / EntityDanmakuMob 4 处 I8 修复点未覆盖 | agent-2 建议 | 下一窗口派批 |
| P43 | 游戏内教程/指引（用户询问）：原作即无，FAQ 即手册；若做指引书物品或 Patchouli 手册属**超出移植范围的新内容** | 用户反馈 | 可选立项；USER_MIGRATION §6 FAQ 可直接作底稿 |
| P44 | BroomPhysicsTest.unriddenBroomGlideDistanceMatchesLegacy 偶发红（采样差 1 tick；隔离实验证与 diag-pose 批无因果） | agent-5 归因 | ✅ 已修（agent-4）：根因=onEachTick 采样器与 runAtTickTime 同 tick 触发顺序取决于 HashMap 迭代序→样本数 6/7 不定；改逐相邻样本比断言消除样本数依赖；6 连跑 64/64 全绿 + 变异验证有效 |
| P45 | maxEntityCramming（1.21.1 新增）：模组 9 类 DanmakuMob 全计入挤压 → 密集战妖精成片"凭空死"（1.7.10 无此机制） | audit-vanilla-mech | 待裁决：EntityDanmakuMob 覆写 isPushable()→false（牺牲推挤换无挤压死）还是保留原版挤压（gamerule 可控） |
| P46 | 1.7.10 getCollisionBorderSize 身兼准星加粗未全移：EntitySanaeWind 风弹 1.0 格加粗丢失 → 打拳反弹几乎点不中 | audit-vanilla-mech | 下一窗口补 getPickRadius 覆写（I8 瞄准链的覆盖缺口） |
| P47 | 商店交易上限与补货：Sanae/Sakuya 未实现 needsRestock/resetUses → 交易用完后**永久缺货**（与原作同式：旧版 7 次上限同样无补货） | audit-vanilla-mech + FIX-3b 实核 | 代码不改（=原作语义）；USER_MIGRATION §5#22/Q11 措辞已修正；次数差 7→12 见 P49 |
| P48 | 核爆掉落实率与 1.21.1 blockExplosionDropDecay 语义差；USER_MIGRATION 相应措辞可能被误读为含核弹 | audit-vanilla-mech | 下一窗口核文档措辞 |
| P49 | 商店交易上限次数：旧版=原版默认 7 次（用完永久缺货，无补货），移植版=12 次 | FIX-3b 实核 | 待裁决：对齐 7（严格忠实）或保留 12；USER_MIGRATION §5#22/Q11 措辞已按"永久缺货"修正 |
| P50 | EntityCirnoIceBox 的 width/height 未持久化（重载后回默认 0.5F，尺寸/消失时机漂移） | audit-nbt-zero §4 | 下一窗口新增 NBT 键（原作同缺，登记为原作设计） |
| P51 | EntitySukima 的 warp/warpPos*/warpAngle/count/pattern/userEntity 未持久化（重载后隙间传送失效） | audit-nbt-zero §4 | 原作同样不写 → 登记为原作设计；如需修则新增 NBT 键 |
| P14 | 发射器弹物品发射者改用 ArmorStand 占位（原 EntityFamiliar） | agent-25 | living 批合并后换回 EntityFamiliar |
| P15 | 玉枝 6 段充能图标 / 折伞双层染色需模型 JSON ItemProperties | agent-18 | 阶段 5 客户端润色 |
| P16 | RenderSukima 的 (SRC_ALPHA,SRC_ALPHA) 混合模式 DanmakuRenderTypes 缺失 | agent-22 | 合并渲染批时在 DanmakuRenderTypes 增加该模式 |
| P17 | RenderSpellCard.getLevel 用 SpellCardRegistry 工厂实例化（THSpellCard 有静态 instance 字段） | agent-22 | 复核：工厂每次 new，静态 instance 语义需确认无串扰 |
| P18 | SakuyaStopWatchTexture.png 原作声明但从未使用 | agent-22 | 保持现状（用 SakuyaWatchTexture），文档化 |
| P19 | 生成条件双采样风险：ModSpawns 谓词 vs 实体 checkSpawnRules | agent-26 | **配置读取集中在 ModSpawns**；生物批（agent-16）实体 checkSpawnRules 不得再读 ModConfig/概率条件——合并时强制审查删除 |
| P20 | 万宝槌地牢注入概率 0.77% vs 原作意图 5% | agent-26 | 保持 GLM 现状（干净、可回退）；发布说明记录偏差 |
| P21 | iceMountains → #c:is_snowy 略宽 | agent-26 | 接受（21w40a 合并群系所致），文档化 |
| P22 | 原作 registerChestItem 为空方法（地牢战利品从未生效） | agent-26 | GLM 实现的是设计意图；发布说明注明 |
| P28 | lang JSON 所有权冲突：生成器会覆盖审计补齐内容 | agent-36 | 裁决：lang 移交 check_lang.py --apply 维护；生成器加存在即跳过守卫 |
| P29 | 渲染 A7 fixed buffer 延期 | agent-34 | **perf-eval 实测裁决依据已备**：200 弹/帧 draw call 398→14（-96.5%）、按需内存 ≈21KiB、RegisterRenderBuffersEvent 一行接线（旧冲突顾虑不成立）、硬约束=复用 DanmakuRenderTypes 单例；测量线束在 staging/perf-eval/。下一窗口实施 |
| P52 | B1 追踪节流 | perf-eval | **已实现且实测复核通过**（EntityTHShot:81 HOMING_TARGET_INTERVAL=4 目标缓存 + 仅最良候选 DDA；100 弹 82µs→21µs）——台账旧记录"B1 未做"作废 |
| P53 | ItemBloodthirstyOnmyoudama/ItemGapFoldingUmbrella 用 `getDamageValue()>=getMaxDamage()` 判定耗尽，与 1.21.1 hurtAndBreak 的 shrink 语义不符（用尽即 shrink，事后读不到） | fix-decoy-restore 发现 | 下一窗口统一改 isEmpty 判定（参照 ItemCursedDecoyDoll:74-92 已修范式） |
| P30 | 1.7.10 lang 笔误 thKaguya,color.4（逗号） | agent-36 | 已修复（ja/tw 补齐紫色键） |
| P26 | ModelCursedDecoyDoll 实体包归属：模型批写 entity.living，实体批落 entity.item | agent-21 TODO#2 | 以实体批为准（entity.item）；步骤3合并时改模型 import |
| P27 | 旧版模型 bug 原样保留（度/弧度混用 3 处：THFairy/SunFlowerFairy 死亡分支、Toziko 臂链、Cirno 翅膀） | agent-21 TODO#5 | 保持（玩家可见行为一致原则）；登记已知怪癖，不单独修 |

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
| 人物模型 | agent-21 | 14 模型（修正简报笔误：实际 14 个） | ✅ 交付（几何核验 14/14 零偏差），⏸ 待步骤3合并 |
| 道具渲染器 | agent-22 | 18 个 | ✅ 交付（javac 0错），⏸ 待实体批后合并；贴图已按其约定归位 entity/shot/ |
| 效果渲染器 | agent-23 | 4 个（429 恢复 1 次） | ✅ 交付（javac 验证），⏸ 待实体批后合并；缺贴图已补(miraclecircle/hakuroureflect) |
| 生物渲染器 | agent-24 | 14 个 | 🏃 |
| 符卡物品+杂项+发射器 | agent-25 | （429 恢复 1 次） | ✅ 物品已合并（b252bed）；ModDispenserBehaviors ⏸ 待 SILVER_KNIFE |
| 数据 JSON | agent-26 | 14 biome_modifier + GLM + ModSpawns + 生成器/校验器 | ✅ 交付（全校验通过），⏸ 待生物批后合并 |

## 批次收尾记录（K3 接管）

| 批次 | 结果 |
|---|---|
| agent-32 符卡诊断 | 实质完成（失败分支 debug 日志 + SpellCardDeclarationTest 3 用例，GameTest 实证通过）；根因=宣言需 32 格视线目标（原版设计），可观测性已补 |
| agent-33 实体性能 | A1 流体短路/A2 脏数据门控/A3 跟踪范围单位修正/A4 弹尺寸动态化/A11 MutableBlockPos 已落主树编译通过；B1 追踪节流未做（旧设计，延期评估） |
| verify-fix（I8/I9 验证） | compile 0 错 + GameTest 22/22（×4 稳定）+ smoke 双 PASS；产品零改动（仅修 2 处测试构造）；变异验证证明新用例真实有效 |
| gametest-edge 边界用例 | 5 测试类 24 用例合并主树 → **GameTest 46/46**；ShotAnimationCountBoundary/ShotIdEncoding/DanmakuParams/IntegerDataComponent/SpellCardDeclarationEdge |
| polish-trio（D4/P10/P15） | 实施预备交付（staging/polish-trio/，25 文件，javac 0 错，贴图 md5 核对）；发布冻结未合并，4 项裁决点见 P31-P34 |
| broom-input 操舵设计 | staging/broom-input/DESIGN.md 定稿：vanilla Boat 模型（本地读键→5-bit 位域→控制端独占积分），2 新 2 改无协议变更；发布冻结未实施 |
| charge-hint 早苗充能提示 | 键 thKaguya.msg.sneScCharge **5 语言全部存在**（P13「键缺失」系事实错误，已更正）；补丁备好（THKaguyaLib:199 静默失败②→actionBar 提示），javac 0 错；建议默认开启，发布冻结未合并 |
| broom-input Phase1 实施 | staging/broom-input/impl/ 完整交付（BroomSteer 纯函数 297 行 + Client 输入 89 行 + 扫帚整文件/补丁 + 11 例 GameTest 草稿）；零协议改动（弃 Phase 2 自建包，Boat 模型单侧积分）；javac 沙盒 263 文件 0 错 + **9 项数值断言实跑全中**；主树无人减速修复完整保留。待裁决 P33①（限速 cap 修复已固化为 fullMaskStaysBounded 测试） |
| polish-trio 双变体 | variants/ 交付：D4 A（去 durability，等价 1.7.10 setMaxDamage(0)）/B（保留占位）仅 1 行之差；P10 A（恒 3.0，javap 证实原作）/B（按 hasCorrectToolForDrops 分岐逼近 1.7.10 实机）；P15 MERGE-READY 与 I8/I9 零交集；四变体 overlay javac 全 0 错、6 PNG md5 对原作 |
| audit-common 逻辑侧普查 | 20KB 报告：P0×1（I11 弹幕样式表零调用，已派 FIX）、P1×4（P35-P37 登记）、P2×11（多为已排除项）；codec 对称性/组件 NPE/除零/CME/反射 明确零发现；50 文件逐读 + 8 类模式 grep 全筛 |
| audit-render 渲染普查 | 16.5KB 报告：P0×1（I10 魔人镜冠 A→B→A buffer 复用，已派 FIX-2）、P1×2（P38 + THLaser 延迟 UV 随 FIX-2 硬化）、P2×6；I5 专项 20 模型取件链脚本比对零缺失；72 文件全覆盖（30 逐读+10 模型逐读+32 模式扫描） |
| audit-render 二轮深化 | 首轮 32 个模式扫描文件全部逐行精读（含 12 模型 +1.7.10 逐行对照）：**零新增 P0/P1**；2 条初判候选经原作逐字复核撤回（ModelRinnosuke 臂姿/ModelWriggle root.render，均原作即如此）；client 包至此无未覆盖文件 |
| soak-v2 闸门增强 + doc-sync | P39 落实：soak_test.sh 升级——summon 失败/异常/IOOB 纳入 FAIL 判定 + 36 条实体存活回执断言；用 I11 污染日志回放实证旧闸门假绿→新闸门必红；已合并 porting/tools/。doc-sync：USER_MIGRATION 与 I8/I9 修复逐条核查**零矛盾**（修复=恢复文档所述原作行为） |
| audit-common 二轮深化 | 35/35 符卡逐行读 + 与 1.7.10 逐行 diff：**转录笔误 0**、P0=0；P1-latent×1（SatuzinDoll/SakuyaWatch/SakuyaStopWatch 的 spellCard.user 客户端 NPE 潜伏，已并入 P36）；登记原作即如此 8 条；除零/越界/死循环逐项查空 |
| audit-nbt-zero（I19 同型普查） | 27 类全普查：实害 5 处已修（Sukima 颜色清零→隙间永不消失、YuukaParasol 耐久归零→永不归还、CirnoIceBox 计数、MiniHakkero、Hisou——后 2 处由新用例实证后从"存疑"升级）；6 实体 19 处 contains 守卫；NbtRoundTripZeroKeyTest 8 例（变异验证 7 红证效，105 例通过）；附带发现 2 项字段未持久化 → P50/P51 |
| fix-knife-model 刀弹 3D 接线 | §5#4 关闭（待实机点检）：FORM_KNIFE 分支接入 ModelSilverKnife（构造器 bake 一次复用，台账 P2 纪律）；旋转/缩放/贴图色表与原作七项一一对应；P38 RenderTHBoss NaN HP 条硬化（max 1F）；compile 0 错。遗留：刀柄朝向实机确认点（单行备选方案已备） |
| diag-broom 乱窜三源鉴别 | A 无人弹跳=移植缺陷（1.21.1 move 不清零垂直速度，I9 补偿回归 → I13 修复）；B 骑乘抖动=已知 Phase1 缺口（staging 方案）；C 滑距=与原作同式同值。+3 物理由例 61/61 |
| diag-pose 姿势异常 | 根因=原版自动匍匐被模组实体碰撞盒触发（1.7.10 无姿势拟合故原作无）；5 实体 canBeCollidedWith→false + isPickable 契约保留；+3 姿势回归用例 64/64；首轮超时后按落盘纪律 30 分钟出活 |
| broom-impl I13 对齐 | Phase1 impl 吸收 I13（EMPTY_GROUNDED_VERTICAL=-0.03，删 +0.06）；patch 对 159ede0 可干净应用；javac 沙盒 267 文件 0 错；修正 DESIGN 巡航数值错误（原作 W 单押均值 ≈0.717，实现恒定 0.792 +10%）；IMPL-NOTES 增合并决策事实表（玩家可见变化 7 条 + 回归闸门 5 条） |
| audit-items 物品包深读 | 49/49 类逐行 + 14 关键类 diff 原作：**P0 零发现**；P1×3（ItemThirdEye 单例字段多玩家串话+泄漏；Gungnir/Uchide/MikoSword 耐久写入不夹上限——原作同式但 1.21.1 语义变；SakuyaWatch 创造+停止档 24>=48 静默失效——疑似移植引入）；P2 若干 + 原作登记 11 项。待 FIX-3 分诊 |
| audit-ai living/BOSS 深读 | 16/17 类逐行（含 3 次自我更正轨迹）：**P0×2**——①DanmakuCreeper 起爆冷却窗内被击杀→实体永不移除（1.7.10 同源，规则=旧版 bug 修复并注释）②Familiar/Phantom 的 nextInt(pattern.size()) 缺守卫（I11 修复漏网点，远程客户端表空即抛）；P1×8；已证伪 2 条（伪死亡恢复链自洽/Reimu tickDeath 存在）；确认商人 NOT_ATTACK 无敌=原作意图 |
| gametest-edge2 回归测试二批 | 3 类 8 例（激光/银刀伤害路径、弹相殺数值、12 实体 isPickable 断言、扫帚物理）合并主树；首跑 3 例败 → agent-2 鉴别**全部为测试构造缺陷**（动画计数存储语义/相对绝对坐标混用/几何与靶选择），产品零改动，变异验证 5+1 红证效 → **58/58 PASS**；第 4 处伤害路径（SetLaser/NuclearShot/HenyoriLaser/DanmakuMob）建议第三批 → P42 |
| gametest-edge3 伤害路径三批 | P42 落实：4 类 5 例（SetLaser/核弹头/蛇行激光/DanmakuMob 接触扫描）；发现 HenyoriLaser 未注册（原作即如此）、DanmakuMob:1219 为无调用者死代码（登记）；首跑 2 例败（HenyoriLaser 运行时造类型撞注册表冻结、核弹头几何未命中）→ agent-2 修复中 |
| gametest-edge3 败例闭环 | 两例均测试侧：HenyoriLaser 用例删除（死代码实体无法运行时实例化，其 :126 修复点不覆盖——登记）；核弹头发现 hitEntityCheck 循环内直调 onImpact 返回入参=1.7.10 同款语义（非缺陷），测试改副作用判定+负对照；变异验证 3 改 3 红；**69/69 PASS** |
| FairySummonFire 偶发定案 | 根因≠延迟期（delay 恒 0，实测证伪）：**GameTest 结构 12 格网格 × 符卡消弹 40 格半径 → 邻接用例互相扫弹**（约 1/3 概率）。修法=生成瞬间逐发断言（确定性）+ tick 窗口仅观测；8 连跑 115/115。**教训**：AoE 半径 > 12 格的测试须按跨结构干扰设计断言 |
| audit-net 网络/菜单深读 | 13/13 文件逐行：**P0 零发现**（无 dupe/崩溃/数据损坏；Payload 线程侧别正确、槽位边界安全、弹幕参数 shift 取出/关界面不丢不复制）；P1×4 小项（MalletMenu 缺 .copy 硬化、LaserCraftingMenu shift 槽段沿用 1.7.10 错位、商店 offers 单次下发、预览硬编码——后两者原作同款）；超时恢复后 5 分钟落盘 |
| FIX-3 统一修复（派发） | 3a（实体 P0×2+P46）与 3b（物品/菜单 P1×6）并行；分诊依据：audit-ai/audit-items/audit-net/audit-vanilla-mech 四报告 |
| FIX-3b 物品菜单 P1 | 实修 3：ItemThirdEye 单例字段→即时 report（原作同式但为实害，修复并注释）；MiracleMalletMenu:147 补 .copy（结果槽共享实例，移植引入）；DanmakuLaserCraftingMenu shift 槽段 10/37/46→15/42/51（旧版 bug 修复并注释）。证伪 1：耐久 clamp 无需（1.21.1 setDamage 内置夹紧）。登记 1：SakuyaWatch 创造停止档=原作同式。P47 实核：原作 7 次上限+无补货 → 移植 12 次上限永久缺货=同款语义，代码不改、文档措辞已修正（P49 登记次数差） |
| fix-decoy-restore 还原链 | ORIGINAL_ITEM 组件新增（Holder<Item>，编解码+网络同步齐）；人偶破坏前读组件还原（耗尽 shrink 时序陷阱已处理）；万宝槌产出写组件（语义一般化）；DecoyDollRestoreTest 5 例 + 变异 2 红；**115/115**；§5#8 措辞已改「机制已实装、暂无产出入口」（原作该还原本就只挂外部模组配方——P8 已删） |
| FIX-3a 实体 P0+P46 | 实修：①DanmakuCreeper 冷却窗内死亡优先短路（:174-179，原作同源 bug 按规则修复并注释）②Familiar/Phantom 补 max(size,1) + THFairy setPattern 统一收口（空表 return+clamp+兜底）③P46 补 4 处 getPickRadius（SanaeWind 1.0F/THShot=size/DivineSpirit/ShotMaterial 0.1F，对照原作 5 处无遗漏）。新增 DanmakuMobP0RegressionTest 2 例（含测试专用写入器取舍，已标注）；变异验证 2 改 2 红；71/71×3 连跑 |
| broom-input Phase1 合并 | staging impl 全量落主树（BroomSteer+BroomSteerClient+EntityMarisaBroom 重构+THKaguyaClient 注册+11 例 BroomSteerTest）；I13/I15/I8/I9 逐条保全实证；cap 变异验证双红；**89/89 + smoke 双 PASS**；已知问题 #1 关闭（§5#1 改写为客户端权威注意事项）；**用户实测验收通过** |

## 事故台账补记

| I5 | 大合并后客户端启动挂起（黑屏，停于 beds 图集） | models2 生成器对所有部件用 root.getChild，忽略 1.7.10 addChild 父子层级 → NoSuchElementException → 被当资源包错误进入重载死循环 | ✅ agent-27 修复 12 模型取件链，连续 5 次启动零模型错误；生成器标记勿重跑 |
| I6 | 替身人偶右键即崩（Can't find attribute follow_range） | 我接属性时误用 LivingEntity.createLivingAttributes（无 follow_range） | ✅ 改 PathfinderMob.createMobAttributes |

## 回归闸门记录

| 时点 | 范围 | 结果 |
|---|---|---|
| 阶段3 | build+runClient | PASS（0 配方/模型错误） |
| FIX 批后（a976221） | build+runClient（smoke_test.sh） | PASS（主菜单到达、0 相关 ERROR） |
| 环境修复后 | 双模式 smoke（R3闸+client+server） | **PASS 双端全绿** |
| 阶段5（dfda417 基线） | 双模式 smoke | PASS（R3 OK / client / server） |
| 阶段5（I7 修复后） | 双模式 smoke + GameTest | **PASS + 20/20** |
| 阶段5 | jar 内容核验（832 文件） | PASS（110 配方 / 14 群系修饰器 / GLM / 146 模型 / 5 语言 / 元数据展开正确） |
| 阶段5 | 30 分钟专用服务端浸泡（含 36 实体 soak 数据包常驻 tick） | **PASS**（0 相关 ERROR / 0 OOM / 0 Can't keep up；用户客户端中途接入观察 1 分钟亦正常） |
| 阶段5（I8/I9 修复后） | compile + GameTest（含 DanmakuDamageTest 2 例）+ smoke 双模式 | **PASS：22/22 + smoke 双 PASS**（verify-fix 批 ×4 连跑稳定） |
| 阶段5（gametest-edge 合并后） | GameTest 全量 | **46/46 PASS** |
| 阶段5（I8/I9 修复后） | 30 分钟专用服务端浸泡（第二轮） | **PASS**（0 相关 ERROR / 0 OOM / 0 Can't keep up） |
| 阶段5（I10/I11 修复后） | compile + GameTest（+样式表/妖精生成 4 例）+ smoke 双模式 | **PASS：50/50 + smoke 双 PASS**（FIX+FIX-2 批） |
| 阶段5 | 第三轮浸泡（soak-v2 增强闸门：36 实体存活回执 + 异常计数，全新世界） | **PASS**：receipts 36/36（alive=36 dead=0，妖精系真实生成）、summon_failures=0、exceptions=0、IOOB=0、0 OOM、0 Can't keep up |
| 阶段5（gametest-edge2 合并 + 败例修复后） | GameTest 全量 | **58/58 PASS**（3 败例均鉴别为测试构造缺陷，产品零改动；变异验证 5+1 红证效） |
| 阶段5 | 最终全量 smoke（build+client+server） | **SMOKE PASS**（R3 OK / 双端 0 相关 ERROR / 0 配方 / 0 模型） |
| 阶段5 | 最终构建 jar（842 文件，含 16 测试类）+ 快照提交 | PASS；**git a03a4ee** |
| 阶段5 后续（I13 修复） | diag-broom：compile + GameTest（+扫帚物理 3 例） | **61/61 PASS + compile 0 错** |
| 阶段5 后续（I14 修复） | diag-pose：compile + GameTest（+PlayerPoseRegressionTest 3 例） | **64/64 PASS + compile 0 错**（I8 契约零回归：isPickable/canBeHitByProjectile 不动） |
| 阶段5 后续 | I13+I14 统一全量 smoke | **SMOKE PASS**（R3 OK / 双端 0 相关 ERROR） |
| 阶段5 后续（FIX-3a/3b 后） | GameTest 全量（+7 例：P0 回归 2 + fix3b 5） | **76/76 PASS** |
| 阶段5 后续 | FIX-3 后最终全量 smoke | **SMOKE PASS**（R3 OK / 双端 0 相关 ERROR） |
| 阶段5 后续（I15/I16 修复） | compile + GameTest（+核弹地形 2 例）+ smoke 双模式 | **78/78 PASS + SMOKE PASS**（diag-nuke 批） |
| 阶段5 后续（broom Phase1 合并） | compile + GameTest（+操舵 11 例）+ smoke 双模式 | **89/89 PASS + SMOKE PASS**（合并批） |
| 阶段5 后续（I17-I19 + 第三批波次） | 全量 GameTest + smoke + 最终构建 | **115/115 PASS（8+ 连跑稳定）+ SMOKE PASS**；jar 860 文件（群系修饰器已在正确路径） |

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
| I7 | 用户世界内实测客户端崩溃（run/ 与 run-smoke/ 各 1 份 crash，AIOOBE index -1 @ RenderTHShot:498） | 转录笔误：延迟弹预告（renderLightEffect）把颜色数组下标写成动画帧奇偶 `c=count%2`；延迟期 animationCount 为负 → 奇数帧 c=-1 越界。1.7.10 原作该处用 `colorR[color]`，count 仅驱动贴图 UV | 改回 `colorR[color]`（与原作一致）；`count % 2`→`count & 1` 保证负计数下帧奇偶非负；全渲染器同类下标已普查（laser/minihakkero/气质弹均安全）；修复后 smoke 双 PASS + GameTest 20/20 | ✅ 修复 |
| I8 | 用户实测：弹幕/激光/符卡对怪物零伤害（发布阻塞） | 1.7.10 的 `canBeCollidedWith` 身兼三职（可碰撞/可被准星选取/可被弹丸命中）；1.21.1 拆为 `canBeCollidedWith`（推挤，**原版生物不再覆写→恒 false**）/`isPickable`（准星）/`canBeHitByProjectile`。移植只搬了第一职 → 命中扫描过滤器排除一切生物 | 7 处命中扫描改 `canBeHitByProjectile()`（THShot/THLaser/THSetLaser/THHenyoriLaser/NuclearShot/SilverKnife/DanmakuMob）；EntityTHShot 补 `isPickable()`=animCount>=1（恢复打拳反弹）；另 11 个 1.7.10 恒 true 实体补 `isPickable`（隙间染色/八卦炉/银刀回收等交互同步恢复）；GameTest 新增 DanmakuDamageTest 2 例（变异验证有效） | ✅ 修复（verify-fix 批验证 22/22 + smoke PASS；**用户实测确认有伤害**） |
| I9 | 用户实测：扫帚下骑后不停、无法破坏/回收 | (a) 同 I8：`isPickable` 未覆写 → 准星永远选不中（首次骑乘是物品 startRiding 直挂才骑得上）；(b) tick() 无人减速块只改局部变量未写回 setDeltaMovement → 下骑后永不减速（1.7.10 该块在 moveEntity 后执行同样不写回，属修正性移植） | 扫帚补 `isPickable()`=!isRemoved()；减速块移到骑乘 if 的 else 分支、move() 之前（双侧运行）；GameTest 覆盖创造一拳破坏 | ✅ 修复（同上） |
| I10 | 用户实测 crash（10:24）：魔人镜冠在视野内客户端必崩 `IllegalStateException: Not building!` | RenderMazinkyoukan 同一 BufferSource 上 A→B→A 重取渲染类型后复用旧 VertexConsumer（中间 getBuffer 触发 endBatch 把旧 builder 置 building=false）；转录自 1.7.10 三次 tessellator.draw() 时丢了对应用语义 | ✅ FIX-2 批修复（右取手前重新 getBuffer，javap 复核 3 getBuffer+3 quad；A→B→A 同型全树扫描零残留）；附带项 RenderTHLaser:608 延迟期 `% 2`→`& 1` 硬化（与 I7 同法）；其上报的 RenderTHShot:304/320/462 同类疑点经 K3 核实**不可达**（调用点均在实体化分支，animationCount≥0），无需处理 | ✅ 修复（compile 0 错） |
| I11 | 妖精/向日葵妖精/幽灵/使魔全部无法生成（召唤与自然生成均失效） | `DanmakuPatternRegistry.setDanmakuPattern()` 全树零调用（原作 THKaguyaCore.java:95 有）→ 样式表恒空 → EntityTHFairy.setPattern:96 必抛 IndexOutOfBoundsException；浸泡日志 18 次异常但因 summon 失败不走 ERROR 级被闸门漏报（P39） | ✅ FIX 批：主类在 ModConfig Loading/Reloading（sync 之后）补调用（样式表依赖 danmakuLevel，SERVER config 此时才就绪）；setDanmakuPattern 开头 clear 五表保幂等；原作有 default 兜底故 level=0 不崩（新增测试钉死）；新增 danmakuPatternsAreRegistered + DanmakuMobSpawnTest 3 例；**GameTest 50/50 + smoke 双 PASS**；同数据包复跑浸泡日志 0 异常 | ✅ 修复 |
| I12 | （过程教训）首轮 audit 双批 2h 超时零落盘 | 大分析批在内存攒报告（I4 模式复发） | resume 补「下一步必须写文件 + 限时收尾」→ 135s/144s 出完整报告；该指令模板已固化进后续批次简报 | ✅ 台账 |
| I13 | 用户实测"扫帚乱窜"：无人时 2 tick 周期永久微颤（I9 修复的回归） | 1.21.1 `Entity.move()` 着地**不清零**垂直 deltaMovement（1.7.10 moveEntity 会吃掉被阻运动）→ I9 照抄的"着地 motionY+0.06"在新版变净上浮 +0.03/tick（实测振幅 0.0141 格、motionY -0.022/+0.007 往复） | diag-broom 批：着地时 `motionY = -0.03`（取 0 会让 onGround 掉 false 变交替振动，实测排除）；新增 3 物理由例（静止振幅/衰减 0.891/滑距与原作同式同值）；**61/61 + compile 0 错**。骑乘抖动另线（无 getControllingPassenger 双侧积分，Phase1 staging 方案覆盖） | ✅ 修复 |
| I14 | 用户实测"右键符卡/物品人物趴下（鞘翅状趴平+视角下移）" | **原版自动匍匐被触发**：1.21.1 `Player.updatePlayerPose()` 站/蹲放不下时兜底 Pose.SWIMMING（0.6 高/眼高 0.4），拟合把实体碰撞盒算入；弹幕/效果实体生成在使用者胸口高度（脚底+1.12）且 canBeCollidedWith==true → 站蹲均相交 → 自动按趴。1.7.10 无姿势拟合机制故原作无此现象（机制判定=SWIMMING 匍匐，非鞘翅/非潜行） | diag-pose 批：5 实体（THShot/SanaeWind/SilverKnife/DivineSpirit/ShotMaterial）canBeCollidedWith→false（isPickable 不动，I8 瞄准/命中/打拳反弹零回归——全树产品码对该方法零读取）；新增 PlayerPoseRegressionTest 3 例（含原版船机理对照）；**64/64 + compile 0 错**。**裁决**：代价=弹幕不再参与实体推挤（不可感知），登记为行为差异 | ✅ 修复（**用户实测确认不再趴下**） |
| I15 | 用户实测"扫帚飞行中模型整圈乱转（不影响方向）" | 骑乘分支每 tick `setYRot(骑手yaw−90)` 不回卷，骑手 yaw 随鼠标无限增长；而 yRotO 来自网络包（256 量化已正规化），渲染器素 lerp 把 360° 整数倍整段走完 → 视觉上整圈甩 | diag-broom-render 批：RenderMarisaBroom:72 改 `Mth.rotLerp`（1.21.1 自身插值惯例）；纯函数沙盒数值验证：持续转向 600 tick 修正前最大扫掠 3960°→修正后 0°，常规操作两实现零差异；76/76 + compile 0 错。遗留：tick 内 wrapDegrees 的更根本做法待 broom-input 合并时一并验证 | ✅ 修复 |
| I16 | 用户实测"控制棒（核弹）不破坏地形" | **原作即如此**：1.7.10 物品用 6 参 ShotData.shot → special 恒 0 → EXPLOSION01 破坏分支不可达；tick() 的 4 处爆炸在"蓄力中换掉手持物"才触发，正常游玩不可达（javap 字节码证实）——移植忠实继承"不破坏"；但配置项 `Master Spark Destroys Blocks` 与分支注释"地形を破壊する爆発"证明破坏是设计意图 | diag-nuke 批：按设计意图实现（同 P22 GLM 先例）——核弹命中方块调用 `explodeOnBlockHit(loc, ModConfig.MasterSparkDestroysBlocks)`，半径随蓄力（原作 tick 路径同式）；**K3 裁决保留**（玩家预期+配置门控+可单点回退），USER_MIGRATION §3.4 已登记为有意偏离；新增 NuclearShotTerrainTest 2 例（破坏/门控负对照），变异验证 2 红；78/78 + smoke 双 PASS | ✅ 已修（有意偏离，可回退；**用户实测确认**） |
| I17 | 用户实测"投掷小刀朝向永远固定（刀柄不朝投掷者）" | 1.7.10 `sendsVelocityUpdates=true`（速度同步）移植丢失 → 客户端银刀 motion 恒 0 → 每 tick `rotationYaw=atan2(0,-0)` 按 Java 规范恒 180°，覆盖服务端同步的正确朝向（pitch 有侧别保护故无恙） | diag-knife 批：motion 为零（客户端常态）时保留 getYRot() 同步值，否则维持 atan2（EntitySilverKnife:672-684）；新增 SilverKnifeOrientationTest 2 例（修复前精确复现 yaw 塌 180°，修复后 95/95）；渲染器与物品无需动 | ✅ 修复 |
| I18 | 用户实测"野外妖精自然生成完全没有" | **双错叠加且全程静默**：①14 个群系修饰器 JSON 路径少一层 `neoforge/`（正确=`data/thkaguyamod/neoforge/biome_modifier/`）→ 注册表静默为空，自阶段3起群系生成从未生效；②`biomes` 字段字符串数组写法不被 HolderSetCodec 接受（须 `{"type":"neoforge:or","values":[...]}`），14 文件全败——只修路径会让服务端启动失败。smoke/验收全程未捕获因空注册不产日志 | diag-spawn 批（运行时取证：修复后平原生成表出现 th_fairy(w=30)/th_phantom(w=20) 等全条目）；FIX-4 批执行修复+加 RegistryCompletenessTest 生成表闸门（路径类静默错误的低成本探针）；**教训**：数据包类资产须验证"加载后生效"而非"文件存在" | ✅ 修复 |
| I18-续 | （防复发）生成器单一事实源仍是旧路径/旧语法 | build_phase4_data.py 重跑会重新产出错的 JSON | fix-generator 批：生成器改正确路径+holderset 语法+LF bytes 写出+`--verify` 逐字节自检；validate_phase4_data.py 加三重闸门（旧路径残留/语法/生成器↔主树一致）；变异 3 改 3 红；主树 14 JSON 统一 LF | ✅ 修复 |
| I19 | 用户实测"/summon 妖精弹幕卡在体内不飞+发射频率异常快" | 1.21.1 把 /summon 统一走 `Entity.load()`→`readAdditionalSaveData()`，`CompoundTag.getShort/getFloat` **缺键返回 0** → 召唤 NBT（仅 id/Pos）把构造器从样式表算出的 danmakuSpan/speedRate/距离 全覆盖成 0（span=0 每 tick 连发、speedRate=0 弹卡胸口）。自然生成只走构造器故无碍；原作 /spawnmob 不走 NBT → 移植引入语义差 | diag-fairy-fire 批：EntityDanmakuMob:1764-1802 十处读取加 `contains` 守卫；新增 FairySummonFireTest 4 例（含 summon 复现链），变异验证有效，95/95×4 连跑；**同型风险全树普查批（audit-nbt-zero）已派出** | ✅ 修复（同型普查在途） |
