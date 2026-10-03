# 第三轮严重性普查：1.21.1 有 / 1.7.10 无（或语义大变）的原版机制 × 本模组干涉面

批次：audit-vanilla-mech　　编写：Flash 子代理（基于 verify-fix 批建立的主树认知）
范围：**只读**。不改任何既有文件、不跑 gradlew、不跑 git、不杀进程。
目标：找出「与 I14（1.21.1 姿势自动匍匐，已修）同类」的原版机制变化点 —— 即
「1.21.1 引入了新机制 / 改变了既有语义，而 1.7.10 移植代码按旧语义书写」导致的实际缺陷。

## 判定口径

- **P0**：玩家正常游玩即可触发，表现为功能失效/崩溃/数值错误/资金或物品损失。
- **P1**：需要特定组合或较窄条件触发，或表现为与 1.7.10 行为明显不一致（但不致命）。
- **P2/信息**：语义差异存在但被本模组现有写法天然规避，或影响仅为观感/边界。
- **零发现**：机制存在但与本模组无接触面（grep 无命中），或接触面已正确使用 1.21.1 语义。

## 进度表（逐机制一行，随查随写）

| # | 机制 | 变化点 | 接触点（grep 证据） | 严重度 | 结论 |
|---|---|---|---|---|---|
| 1 | **实体挤压伤害 maxEntityCramming** | 1.7.10 无此机制；MC 1.11 起 `LivingEntity.pushEntities()` 每 tick 统计同 AABB 内 `EntitySelector.pushableBy(this)` 的实体，>23 且 `random.nextInt(4)==0` 时造成 **6 点 cramming 伤害**（`GameRules.maxEntityCramming` 默认 24） | ① 计数条件是 `isPushable()`：`LivingEntity.isPushable()` = `isAlive && !isSpectator && !onClimbable`（`LivingEntity.java:3071`）→ **本模组所有 DanmakuMob 系（妖精/向日葵妖精/幽灵/使魔/琪露诺/露米娅/屠自古/莉格露/花田尖叫者）天然计入**；② 本模组不覆写任何 LivingEntity 的 `isPushable`（grep：`isPushable` 覆写仅 16 处，全部是 `extends Entity` 的道具/效果实体，见进度表下方证据块）；③ 妖精 AI 会朝玩家聚拢，符卡 `THSC_Fafurotskies_no_Kiseki` 还会 `addFreshEntity` 生成苦力怕/鸡（`spellcard/THSC_Fafurotskies_no_Kiseki.java:81,130,140`）；④ 模组弹幕 `EntityTHShot extends Entity` → `isPushable()==false`，**不参与计数**（排除） | **P1** | 真实存在且属 I14 同类：1.7.10 时代一堆妖精贴在一起不会掉血，1.21.1 里 24 只以上叠在一起会每 4 tick 吃 6 点伤害 —— 妖精血量低（原作妖精 20 血以内），密集弹幕战/符卡群刷时会「凭空成片死亡」。**不构成 P0**（需要 24 只 pushable 实体挤进同一 AABB；浸泡 36 实体因坐标间隔 ≥3 格未触发）。建议 K3 裁决：模组怪物是否在 `EntityDanmakuMob` 覆写 `isPushable()` 返回 false（妖精设定上是飞行灵体，不推挤也更贴合原作），或维持原版语义并在文档记录 |
| 2 | **横扫之刃 / 盾牌格挡 / 鞘翅 / 激流三叉戟**（近战与附魔/属性交互） | 1.21.1 用**数据驱动附魔**（`data/minecraft/enchantment/*.json`）+ `ItemAbilities.SWORD_SWEEP` + `isDamageSourceBlocked`；1.7.10 是硬编码 `Enchantment` 类 | ① 模组近战武器全部是 `extends SwordItem`（`ItemDeathScythe:28`、`ItemKaigo:38`、`ItemMikoSword:55`、`ItemOnbashira:20`，以及注册表里 `new SwordItem(ModTiers.X, props)` 的 `LAEVATEINN/HAKUROUKEN/ROUKANKEN`）→ **横扫/格挡/附魔全部走原版通路，无需移植代码**；② 模组主动附魔只有两处，均为数据驱动 ID：`ItemFireRatRobe.java:73` `Enchantments.FIRE_PROTECTION, 5`、`ItemKerobou.java:38` `Enchantments.THORNS, 10`；③ grep `Elytra|Trident|isAutoSpinAttack|Riptide` → **零命中** | **P2** | 无 P0/P1：`ItemKerobou` 的荆棘 10 级是 1.7.10 原样照搬（原版荆棘上限 3，超出部分只是效果按级数线性放大，**不崩溃、不报错**，与旧版一致）；火鼠裘 V→IV 已记录在 `USER_MIGRATION.md §5 #16`。**唯一值得记一句**：`ItemMikoSword` 用 `EntityTypeTags.UNDEAD/ARTHROPOD` 替代旧 `EnumCreatureAttribute`（`ItemMikoSword.java:158,160`），而模组自身的"妖怪/妖精/幽灵"生物**不在**这两个原版标签里 → 模组怪不会被亡灵/节肢杀手加成（与 1.7.10 的"种族系统恒为其他"结果一致，属保持等价） |
| 3 | **弹射物偏转（1.21 旋风人 ProjectileDeflection / canBeDeflected）** | 1.21.1 新增 `Projectile.hitTargetOrDeflectSelf` → 命中实体时先问 `entity.deflection(projectile)`；只有 `Projectile` 子类才走这条路，且有 `EntityTypeTags.REDIRECTABLE_PROJECTILE` 白名单 | grep `deflect\|Deflection\|canBeDeflected` 全树 → **零命中**；模组弹幕 `EntityTHShot extends Entity`（不是 `Projectile`），自己实现 `hitCheck()/hitEntityCheck()`，**完全不经过原版偏转通路** | **P2（零接触）** | 无缺陷。模组的"打拳反弹弹幕"是自实现的 `EntityTHShot.hurt()`（`EntityTHShot.java:1609`），随 I8 补的 `isPickable()` 恢复，与原版偏转机制互不干扰 |
| 4 | **爆炸 BlockInteraction 语义**（TNT/苦力怕 受 mobGriefing，模组配置 Master Spark Destroys Blocks） | 1.21.1 的 `Level.explode` 有 5 种 `ExplosionInteraction`：`NONE/BLOCK/MOB/TNT/TRIGGER`；**只有 `MOB` 会检查 `EventHooks.canEntityGrief`（mobGriefing）**，`BLOCK` 无条件破块（`Level.java:769-773, 797-799`） | **发现不一致**：`EntityTHShot.java:1266-1267, 1323-1324` 与 `THSC_FujiyamaVolcano.java:70` 正确地把配置映射成 `BLOCK/NONE`；但 **`EntityNuclearShot.java:89,99,114,124` 四处硬写 `Level.ExplosionInteraction.BLOCK`，完全不看 `ModConfig.MasterSparkDestroysBlocks`**，而 1.7.10 原作在线爆炸写法相同（`EntityNuclearShot.java:129,139,156,166`: `createExplosion(..., true)` 恒破块）—— 语义确实**与原作一致** | **P2（非缺陷，但配置语义易被误解）** | 结论：`Master Spark Destroys Blocks` 在 1.7.10 里**只覆盖 `EntityTHShot` 的两处爆炸**，核弹（`EntityNuclearShot`）从来不受它控制；`USER_MIGRATION.md §4.3` 把该配置描述为"魔炮 / 爆炸类弹幕是否破坏地形"，读者很容易以为核弹也受控。**属文档措辞可澄清项，非代码缺陷**（另注：`BLOCK` 不受 mobGriefing 影响 → 玩家把 `mobGriefing=false` 也挡不住核弹炸地形，与 1.7.10 相同） |
| 5 | **怪物 griefing / mobGriefing / 袭击·巡逻队卷入模组怪** | 1.21.1：`mobGriefing` 管苦力怕/末影人/村民种地等；袭击由 `Raid` 按 `Raider` 接口组织，巡逻队硬编码 `Pillager/Vindicator/Evoker` | ① 模组实体**没有任何 `setBlock/destroyBlock`**（grep `thkaguyamod/entity/` 零命中）→ 模组怪本来就不 grief；② 模组怪不实现 `Raider`、不继承 `Monster`（基类是 `PathfinderMob`，`EntityDanmakuMob.java:47`）→ **不会被 `Raid`/巡逻队/`PatrolSpawner` 纳入**；③ `spellcard/THSC_Fafurotskies_no_Kiseki` 生成的**原版**苦力怕仍受 mobGriefing 管辖（正确行为） | **P2（零发现）** | 无干涉面 |
| 6 | **亡灵杀手/节肢杀手等附魔 × 模组 BOSS（原作种族系统）** | 1.21.1 用 `EntityTypeTags.UNDEAD/ARTHROPOD/ILLAGER/SENSITIVE_TO_SMITE...` 数据标签 + 数据驱动附魔；1.7.10 用 `getCreatureAttribute()`（原作**没有任何模组实体覆写**它 → 全部返回 `UNDEFINED`） | ① 模组只把旧判定搬到了物品侧：`ItemAjaRedStone.java:111`、`ItemMikoSword.java:158,160` 用 `EntityTypeTags.UNDEAD/ARTHROPOD`（与旧版结果等价，模组怪本就不在标签里）；② 模组怪 `getType()` 不带任何 SENSITIVE_TO_* 标签 → **亡灵杀手对妖精/BOSS 无加成**，与 1.7.10 一致 | **P2（保持等价）** | 无 P0/P1。唯一副作用：玩家给模组 BOSS 起 `EntityTypeTags.UNDEAD` 名字没用（标签是数据包级，不是名字）—— 与旧版同样 |
| 7 | **属性夹紧（armor/toughness/max_health 上限）对模组防具与 BOSS** | 1.21.1：`RangedAttribute` 自带上下限（`MAX_HEALTH` = 1.0~1024.0，`Attributes.java:102-104`）；`armor` 属性 0~30、`armor_toughness` 0~20；`setHealth` 写入时 `Mth.clamp(health, 0, getMaxHealth())`（`LivingEntity.java:1131`） | ① 模组防具材质 `ModArmorMaterials.defense(1,2,2,2)` 等**全部在限内**，无夹紧；② 模组 BOSS 走**属性通路**：`EntityCirno.java:43-44` `setMaxHP(38.0F)` + `setHealth(38.0F)`（`EntityDanmakuMob.java:374` 写的是 `MAX_HEALTH` attribute base value），各阶段 `moveDanmakuAttack(..., 46/32/58/0F, ...)`（`EntityCirno.java:103-112`）→ 峰值 58 ≪ 1024；③ 全树 `MAX_HEALTH` 最大值为 74（`EntityMiko/EntityReimu`），**远低于 1024**；④ 模组实体的 `ARMOR` 属性**零赋值**（默认 0） | **P2（零发现）** | 无夹紧风险。**但发现一个真正的 1.7.10→1.21.1 语义差异（P1，见 #8）**：属性写 base value 会**重置当前血量**，而 1.7.10 的 `setMaxHealth` 不会 |
| 8 | **属性基值写入 × 当前血量（多段 BOSS 阶段切换）** | 1.7.10 `SharedMonsterAttributes.setAttributeModifiers` 只改上限，血量由代码显式 `setHealth(maxHP)`；1.21.1 `LivingEntity.onAttributeUpdated`：**只要 `MAX_HEALTH` 的 base value 变化，就把当前血量按比例重算**（`LivingEntity.java:1101` 附近：`if (attribute.is(Attributes.MAX_HEALTH))` → `setHealth(getHealth() / oldMax * newMax)`） | `EntityDanmakuMob.java:806-807` 连续调用 `getAttribute(MAX_HEALTH).setBaseValue(maxHP); setHealth(maxHP);` —— 第 1 句先按比例缩放当前血量、第 2 句再显式设为目标值，**最终结果仍等于 `maxHP`**（顺序上是对的，无缺陷）；`EntityCirno.java:43-44` 同理 | **P2（写法侥幸正确）** | 逐点核对 `setBaseValue` → `setHealth` 的**调用顺序**后确认无误。**风险点**：若后续有人把 `setHealth(maxHP)` 挪到 `setBaseValue` 之前，或只写 `setBaseValue` 而删掉 `setHealth`，阶段切换就会把血量按比例缩放（例：58→32 阶段只剩 55% 血）→ 这是**易碎写法**，建议 K3 决定是否加一行注释说明「必须后写 setHealth」 |
| 9 | **准星选取的碰撞盒加成 `getPickRadius()`（1.7.10 `getCollisionBorderSize`）** | 1.7.10：`getCollisionBorderSize` 同时用于**弹幕/激光的实体命中判定**（`entity.boundingBox.expand(f2,...)`）与玩家准星；1.21.1 拆成两条独立通路：`getPickRadius()` 只喂 `ProjectileUtil.getEntityHitResult(shooter,...,distance)` 这个 **5 参重载**（= 玩家准星 `GameRenderer.java:828` 与 `Entity.pick`），而**弹幕命中**走 6 参重载（固定 `inflate(0.3F)`），**不看 `getPickRadius`** | ① 只有 `EntitySilverKnife.java:864` 移植了 `getPickRadius()=0.01F`（对应旧 `getCollisionBorderSize`）；② **其余 4 个旧覆写点全部丢失**：`EntityTHShot.java:303-314` 的 `getDimensions` 只返回 `EntityDimensions.scalable(size,size)`（把尺寸与"选取加成"混成一件事，而旧版 `setSize(size,size)` 与 `getCollisionBorderSize()=getShotSize()` 是**两件事**）、`EntityDivineSpirit`（旧 0.1F）、`EntityShotMaterial`（旧 0.1F）、`EntitySanaeWind`（旧 1.0F）均无 `getPickRadius`；③ grep `getPickRadius` 全树仅 1 处 | **P1** | **同类缺陷（I14 级）**：`EntitySanaeWind`（早苗的风起こし）在 1.7.10 有 **1.0 格**的准星加粗，1.21.1 变成 0 → 这枚「帮骑手挡弹/推人」的风弹现在几乎点不中，玩家无法用打拳把它打掉（弹幕自撞仍然有效，因为命中扫描是自实现的）。`EntityTHShot` 丢失的是**准星加粗**（`getDimensions` 已被复用为尺寸，视觉盒 = 选取盒，反而"点了就能中"），影响小于 SanaeWind。**注意与 I8 的分工**：I8 解决的是"能不能选中"（`isPickable`），本条解决的是"选中判定有多宽"（半径）—— 两者独立，I8 补完后本条仍是残留缺口。修法建议：在 `EntitySanaeWind` 加 `getPickRadius(){return 1.0F;}`，其余三处按旧值补 0.1F / `getShotSize()`，或统一在 `EntityTHShot` 覆写后由子类继承 |
| 10 | **爆炸掉落衰减 gameRule（`blockExplosionDropDecay` / `mobExplosionDropDecay`，1.21 新增）** | 1.21 新增两条掉落衰减规则，默认 **true**（= 被炸方块按 1/掉落除数概率不掉落）；`Level.explode` 对 `BLOCK` 用 `blockExplosionDropDecay`、对 `MOB` 用 `mobExplosionDropDecay`（`Level.java:769-773`） | 模组所有爆炸都写死 `ExplosionInteraction.BLOCK`（`EntityTHShot.java:1266,1323`、`EntityNuclearShot.java:89,99,114,124`、`THSC_FujiyamaVolcano.java:70`）→ 受 `blockExplosionDropDecay`（**默认 true**）管辖 | **P2** | 1.7.10 `createExplosion` 无此概念，被炸方块**全部掉落**；1.21.1 默认只会掉一部分 → 用魔炮/核弹/藤原火山炸地形时「掉出的方块比旧版少」。不影响玩家物品栏，属资源产出差异。**建议**：若追求与 1.7.10 等价，可在文档记一行「想全掉落请 `/gamerule blockExplosionDropDecay false`」，或把交互改成 `ExplosionInteraction.TNT`（TNT 规则默认 false）。不建议改代码（会改变原版预期） |
| 11 | **睡觉 / 重生锚 / 铜格栅雷暴 / 避雷针（一句话排除）** | 1.21.1 有 `SleepStatus` 判定、重生锚充能、`WeatheringCopperGrate` 通电、`#minecraft:lightning_rods` 标签 | grep `Totem\|respawn\|SLEEP\|sleeping\|RespawnAnchor\|LightningBolt` → 模组仅 `EntityDragonNeckJewel.java:52,61,91` 出现 `createLightningBoltDanmaku`（是**自定义弹幕名**，不生成 `LightningBolt` 实体）；模组无床/重生锚/避雷针逻辑 | **P2（零发现）** | 无干涉面 |
| 12 | **原版"恐慌"机制 × 模组怪（`panic_causes` 新增）** | 1.21.1 `PanicGoal`/`AnimalPanic` 读取 `DamageTypeTags.PANIC_CAUSES` 标签；`indirect_magic` **在该标签内**（`data/minecraft/tags/damage_type/panic_causes.json`） | 模组怪**不使用任何 PanicGoal**（grep `PanicGoal|HurtByTargetGoal|NearestAttackableTargetGoal` → 零命中），仇恨由自研 AI 管理 | **P2（零发现）** | 无干涉面。附注：模组的爆炸是 `BLOCK` 而非 `MOB`，不会误触盔甲架/末影龙等 `always_*` 标签行为 |
| 13 | **`indirect_magic` 伤害类型标签语义**（与原版 1.21 体系） | 实测 `data/minecraft/tags/damage_type/*.json`：`indirect_magic` ∈ `bypasses_armor` ✓、`witch_resistant_to` ✓、`panic_causes` ✓、`is_magic`(neoforge) ✓；**∉ `bypasses_resistance`**、**∉ `bypasses_shield`**（`bypasses_shield` 只含 `#bypasses_armor` + 2 项 → 因含 armor 标签故**间接命中**）、**∉ `no_knockback`** | 模组全部远程伤害用 `damageSources().indirectMagic(this, user)`，共 **9 处**：`EntityTHShot.java:1139`、`EntityTHLaser.java:246`、`EntityTHSetLaser.java:203`、`EntityTHHenyoriLaser.java:180`、`EntityNuclearShot.java:224`、`EntityKinkakuzi.java:416`、`EntityHakureiShield.java:214`、`EntityHakurouReflecter.java:206`、`THShotLib.java:1433` | **P2** | 与 1.7.10 `causeIndirectMagicDamage` 逐点对应（旧版同类共 13 处，含 `EntitySanaeWind`/`EntityMiniHakkero` 的旧实现分支）；**护甲不生效**（与原作一致，原作魔伤同样无视护甲）；**抗性药水仍生效**、**不死图腾正常触发**（`checkTotemDeathProtection` 只排除 `bypasses_invulnerability`，`indirect_magic` 不在其中）→ 无"图腾失效/抗性失效"缺陷。当前只是**未被任何原版代码消费**（`Tags.DamageTypes.IS_MAGIC` 无读取点），属数据完备性，不是缺陷 |
| 14 | **免伤帧 / 无敌期与高频弹幕（`invulnerableTime` 语义未变）** | 1.21.1 `LivingEntity.hurt` 流程：`invulnerableTime > 10` 且新伤害 ≤ `lastHurt` 时**整次伤害被丢弃**；NeoForge 新增 `DamageContainer.getPostAttackInvulnerabilityTicks()` 允许事件层覆盖（`LivingEntity.java:1190-1202`） | 模组弹幕每 tick 独立结算（`EntityTHShot.tick → hitCheck → onImpact → hurt`），同一只怪会被多枚弹命中；模组怪自身另有 `attackInterval` 无敌期（`EntityDanmakuMob.java:1142`，符卡宣言时 `invincibleTime` 20~60 tick） | **P2** | 语义与 1.7.10 相同（旧版 `hurtResistantTime` 同样是 20 tick、超量部分才穿透），模组未覆写 `invulnerableTime`，**无行为差异**。附注：`applyPotionDamageCalculations` 在 1.7.10 里被**塞在 `hurt` 入口**（`attackEntityFrom(source, applyPotionDamageCalculations(...))`，`EntityDanmakuMob.java:1092`），1.21.1 移植到 `actuallyHurt`（`EntityDanmakuMob.java:1187-1189`）—— 位置合法且效果一致（`actuallyHurt` 在免伤帧判定之后调用），**但会在 `LivingShieldBlockEvent` 等 NeoForge 事件看到的数值上少一次 0.05 缩放**，属事件可见性差异，非玩家可见缺陷 |
| 15 | **商人补货语义（`Merchant.needsRestock` / `MerchantOffer.resetUses`，1.21.1 新增）** | 1.21.1 把"补货"抽成 `Merchant.needsRestock()` + `MerchantOffer.resetUses()`；**原版只有 `Villager` 实现了补货**（`Villager.java:433,445,459`：`WorkAtPoi` 里按"每天 2 次、间隔 2400 tick"重置），`AbstractVillager`/`Merchant` 接口默认**不补货** | ① 模组 `EntitySanae`/`EntitySakuya` 只 `implements Merchant` 并手写 `notifyTrade → increaseUses()`（`EntitySanae.java:391-393`、`EntitySakuya.java:341`），**没有实现 `needsRestock()`**（接口默认返回 false），也没有任何 restock 代码（grep `restock\|resetUses\|needsRestock` → 模组零命中，唯一补货实现是原版 `Villager`）；② `MerchantOffer` 以 `maxUses = 12` 构造（`EntitySanae.java:95-96`）；③ 早苗自定义 GUI 会显示缺货（`MerchantSanaeScreen.java:151`），咲夜走**原版 `MerchantMenu`** | **P1（文档/实现可能不一致）** | `USER_MIGRATION.md §5 #22` 写"用完后该笔交易暂时不可用，需要等待恢复（**约 2 个游戏日**）或换人"。**"约 2 个游戏日恢复"对原版村民成立，对本模组两个商人很可能不成立**：模组没实现 `needsRestock`/`resetUses`，唯一会重置的使用点是重新生成 `MerchantOffers`（重开 GUI 时 `getOffers()` 只在 `buyingList == null` 时新建，**不会重置**）→ 用完 12 次后很可能**永久缺货，只能换人（重进世界/换实体）**。**注意**：早苗 GUI 走自定义 payload（`ModNetworking.sendSanaeOffers`），其缺货态是否随会话重置未在本批次验证范围内。**建议 K3 实测一次"早苗交易 12 次后重开界面/重登"**，并据此修正 §5 #22 的"约 2 个游戏日"表述（属文档措辞，不属代码缺陷；若要恢复旧版"无限次交易"，需在模组商人实现 `needsRestock`） |
| 16 | **MobCategory 与生成上限（`AMBIENT` 语义变化）** | 1.21.1 `MobCategory` 的 `AMBIENT` 只允许**蝙蝠**（`MobCategory.AMBIENT` 的 `max` 极小、`isFriendly=false`、`isPersistent` 特性与 `CREATURE` 不同）；且 `MobCategory.isPersistent()` 为 true 的类别（`CREATURE`/`AMBIENT`）**从不 despawn**（`Mob.removeWhenFarAway` 不会被调用） | `ModEntities` 里 3 个商人用 `AMBIENT`（`ModSpawns.java:66-68` 明确注释"早苗/咲夜/霖之助 = AMBIENT（旧 `EnumCreatureType.ambient`）"） | **P2** | 沿用旧 `EnumCreatureType.ambient` 是**忠实移植**；需注意 `AMBIENT` 是**极低配额**类别，模组商人会与蝙蝠抢配额（旧版 `ambient` 同样是极低配额，行为等价）。**无 despawn 风险**（该类别 persistent）。真正的 despawn 语义见 #17 |
| 17 | **远距 despawn（`removeWhenFarAway` / `requiresCustomPersistence`）** | 1.21.1 `Mob.checkDespawn` 在玩家距离 > 128 格时调 `removeWhenFarAway()`；`Mob.requiresCustomPersistence()` 默认只在骑乘时为 true | 模组 **零覆写** `removeWhenFarAway`/`checkDespawn`/`requiresCustomPersistence`（grep 全树：`thkaguyamod/` 内 `removeWhenFarAway` 零命中；`EntityRinnosuke.java:94` 那个同名方法是 `ModSpawns` 谓词里的**注释与旧文档描述**，不是覆写）→ 全部走 `Mob` 默认（= 可 despawn） | **P2** | 与 1.7.10 一致（旧版 `EntityMob`/`EntityVillager` 同样可 despawn），`USER_MIGRATION.md §6 Q12`"走远后会自然消失"表述**正确**。**附注**：多段 BOSS 打到一半玩家跑远会 despawn（旧版同样），非新缺陷 |
| 18 | **无敌期的击退/受伤标记（`LivingEntity.hurt` 的 early-return）** | 1.21.1 `LivingEntity.hurt` 第一行就是 `if (this.isInvulnerableTo(source)) return false;`（`LivingEntity.java:1143`）→ **无敌期不受伤、不击退、不 `markHurt`**；1.7.10 `EntityLivingBase.attackEntityFrom` 同样是 `isEntityInvulnerable()` 后立即返回 false | `EntityDanmakuMob.java:1142-1148` 用 `attackInterval > 0 → isInvulnerableTo = true` 表达"符卡宣言中的无敌"；同文件 `:1195-1199` 又写了"无敌时只受 5% 伤害"的 `applyPotionDamageCalculations` | **P2（死代码，非回归）** | 逐层核对：`hurt()` 在 `attackInterval > 0` 时**第一步就返回 false**，永远到不了 `actuallyHurt()`，因此 5% 分支**不可达**（`EntityDanmakuMob.java:1195-1199` 是死代码）。**1.7.10 原作同样如此**（`applyPotionDamageCalculations` 作为实参先求值、随后被 `isEntityInvulnerable` 丢弃）→ 行为与旧版一致，**不是新缺陷**。唯一建议：把 `:1193` 的注释「無敵状態のときに受けるダメージを 5% にする」改成「実際には到達しない（旧版も同じ）」以免误导后续维护者 |

## 关键证据块

### E1 挤压伤害的完整判定链（1.21.1 源码）

```
LivingEntity.pushEntities()                       LivingEntity.java:2893-2916
  list = level().getEntities(this, bbox, EntitySelector.pushableBy(this))
  i = gamerules.maxEntityCramming (default 24)
  if (i > 0 && list.size() > i-1 && random.nextInt(4)==0)
      j = 非骑乘者计数;  if (j > i-1) hurt(damageSources().cramming(), 6.0F)

EntitySelector.pushableBy(entity)                 EntitySelector.java:52-78
  → p.isPushable() 为第一道过滤

LivingEntity.isPushable()                         LivingEntity.java:3071
  → isAlive() && !isSpectator() && !onClimbable()   ← 模组怪物全部命中
Entity.isPushable()                               Entity.java:1671  → false（EntityTHShot 等弹幕落在这里）
```

### E2 模组 16 处 `isPushable()` 覆写（**全部是 `extends Entity` 的道具/效果实体，无一 LivingEntity**）

```
EntityCirnoIceBox=true      EntityHakureiShield=false    EntityHakurouReflecter=false
EntityDivineSpirit=false    EntityShotMaterial=false     EntityHisou=false
EntityKinkakuzi=true        EntityMarisaBroom=true       EntityMiniHakkero=false
EntityPendulum=false        EntitySakuyaStopWatch=false  EntitySakuyaWatch=false
EntitySpiritualStrikeTalisman=false  EntitySukima=false  EntityYuukaParasol=false
EntitySpellCard=false
```

### E3 疑似遗漏的旧 API 覆写对（1.7.10 → 1.21.1）

```
1.7.10 entity/item/EntitySilverKnife.java:834,839   canBeCollidedWith=true / getCollisionBorderSize=0.01F
  → port EntitySilverKnife.java:849,864             isPickable=true  / getPickRadius=0.01F        ✔ 对应
1.7.10 entity/shot/EntitySanaeWind.java:335,338      canBeCollidedWith=true / getCollisionBorderSize=1.0F
  → port EntitySanaeWind.java:90,96                  canBeCollidedWith=true / isPickable=true
                                                    ✘ getPickRadius(1.0F) 缺失           ← 见 #9（P1）
1.7.10 entity/shot/EntityTHShot.java:2004,2011       cbw=getAnimationCount()>=1 / border=getShotSize()
  → port EntityTHShot.java:1914,1924,303-314        canBeCollidedWith / isPickable / getDimensions=size
                                                    ✘ getPickRadius(getShotSize()) 缺失   ← 见 #9（P1）
1.7.10 entity/EntityDivineSpirit.java:251,259        cbw=true / border=0.1F
  → port EntityDivineSpirit.java:215,222             canBeCollidedWith / isPickable        ✘ getPickRadius 缺失
1.7.10 entity/EntityShotMaterial.java:305,313        cbw=true / border=0.1F
  → port EntityShotMaterial.java:257,264             canBeCollidedWith / isPickable        ✘ getPickRadius 缺失
```

### E4 `indirect_magic` 的标签归属（实测 `minecraft_1.21.1_client.jar` + neoforge merged）

```
∈ minecraft:bypasses_armor, minecraft:witch_resistant_to, minecraft:panic_causes,
  minecraft:bypasses_wolf_armor, neoforge:is_magic
∉ minecraft:bypasses_resistance, minecraft:no_knockback, minecraft:is_explosion,
  minecraft:bypasses_invulnerability, minecraft:bypasses_enchantments, minecraft:bypasses_effects
→ 护甲不计、抗性药水照减、不死图腾正常触发（checkTotemDeathProtection 只排除 bypasses_invulnerability）
```

### E5 多段 BOSS 阶段切换的血量写法（顺序敏感）

```java
EntityDanmakuMob.java:806-807
  this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHP);   // 1.21.1 onAttributeUpdated 只做
                                                                  // "当前血量 > 新上限则夹到上限"（LivingEntity.java:1101-1107），
                                                                  // 不会按比例缩放
  setHealth(maxHP);                                               // 显式设为目标值 → 最终正确
```

---

## 汇总

**排查机制数：18**（简报列出的 10 项 + 我扩充的 8 项）。
**P0：0 项。**

### P1 清单（建议 K3 裁决）

| 严重度 | # | 机制 | 一句话 |
|---|---|---|---|
| P1 | 1 | maxEntityCramming | 24 只以上 pushable 实体挤同一 AABB → 每 4 tick 6 点伤害；模组全部 DanmakuMob 计入，1.7.10 无此机制 → 密集弹幕战/符卡群刷会成片死妖精 |
| P1 | 9 | `getPickRadius` 缺失 | `EntitySanaeWind` 旧有 1.0 格准星加粗、`EntityTHShot`/`DivineSpirit`/`ShotMaterial` 旧有加粗全部未移植 → 风弹等几乎点不中（I8 补的是"能不能选中"，本条是"选中半径"，两者独立） |
| P1 | 15 | 商人补货 | 模组 Sanae/Sakuya **未实现 `needsRestock`/`resetUses`** → `USER_MIGRATION.md §5 #22` 的"约 2 个游戏日恢复"很可能不成立，需实测并修正文档 |

### 已顺手记录的次级项（P2，非缺陷）

- #4：`EntityNuclearShot` 四处硬写 `BLOCK` 不看配置 —— 与 1.7.10 一致，但 `USER_MIGRATION.md §4.3` 对该配置的描述范围容易被读成"含核弹"。
- #10：`blockExplosionDropDecay` 默认 true → 炸地形掉落比 1.7.10 少（可用 gamerule 或 `ExplosionInteraction.TNT` 对齐）。
- #18：`EntityDanmakuMob.applyPotionDamageCalculations` 的 5% 分支不可达（旧版同样不可达），注释措辞应改以免误导。
- 附：#17 `MobCategory.AMBIENT` 的模组商人属 persistent 类别，无 despawn 风险；真正 despawn 语义与 1.7.10 一致。

### 零发现机制（一句话汇总）

- **#3 弹射物偏转**：模组弹幕是 `extends Entity` 的自实现命中扫描，完全不经过原版 `ProjectileDeflection` 通路（grep 零命中）。
- **#5 怪物 griefing / 袭击·巡逻队**：模组实体无任何 `setBlock/destroyBlock`，不实现 `Raider`、不继承 `Monster`，不会被袭击/巡逻队纳入。
- **#6 亡灵/节肢杀手**：模组怪不在任何 `SENSITIVE_TO_*` 标签内，与 1.7.10"种族恒为其他"等价。
- **#7 属性夹紧**：模组最大 `MAX_HEALTH`=74 ≪ 1024，防具材质防御值远低于上限，无夹紧。
- **#11 睡觉/重生锚/铜格栅雷暴/避雷针**：grep `Totem|respawn|SLEEP|RespawnAnchor|LightningBolt` 零命中（`EntityDragonNeckJewel` 的 `createLightningBoltDanmaku` 只是自定义弹幕名）。
- **#12 原版恐慌机制**：模组怪不使用任何 `PanicGoal`，仇恨由自研 AI 管理。
- **#13 `indirect_magic` 标签**：护甲/抗性/图腾语义与 1.7.10 `causeIndirectMagicDamage` 逐点对应，无失效项。
- **#14 免伤帧**：模组未覆写 `invulnerableTime`，语义与旧版一致。
- **#2 横扫/盾/鞘翅/三叉戟**：模组近战武器全部 `extends SwordItem`，走原版通路；`Elytra|Trident|isRiptide` 零命中。
- **#8、#16、#17**：写法/语义逐点核对后确认无缺陷（见对应行结论）。
