# audit-common REPORT2 — spellcard 包 35 个 THSC_* 逐行通读

二轮深化：首轮这些文件仅被「模式 grep」覆盖，本轮逐行通读并与 1.7.10
`src/main/java/thKaguyaMod/entity/spellcard/THSC_*.java` 对照，专查符卡特有风险面：
弹幕生成参数越界、难度/等级做除数、user/level/target 空解引用、弹形表/颜色表下标、
while 生成弹的无限循环、以及转录笔误（参数与原作不一致）。

约定：**登记** = 原作即如此（不作为缺陷上报，仅留档）；**新发现** = 移植引入。

## 状态表（读完即落盘）

| 符卡 | 行数 | 结论 |
|---|---|---|
| THSC_Catadioptric | 45 | 已读（见下方批次结论） |
| THSC_EternalMeek | 35 | 已读（见下方批次结论） |
| THSC_Fafurotskies_no_Kiseki | 162 | 已读（见下方批次结论） |
| THSC_FujiyamaVolcano | 160 | 已读（见下方批次结论） |
| THSC_GagoujiCyclone | 145 | 已读（见下方批次结论） |
| THSC_HikouchuuNest | 69 | 已读（见下方批次结论） |
| THSC_HoukaKenran | 47 | 已读（见下方批次结论） |
| THSC_IcicleFall | 92 | 已读（见下方批次结论） |
| THSC_Kachoufuugetu | 73 | 已读（见下方批次结论） |
| THSC_KappaPororoca | 80 | 已读（见下方批次结论） |
| THSC_Kasho_no_Eimin | 41 | 已读（见下方批次结论） |
| THSC_Kerochan_Fuuu_ni_Makezu | 50 | 已读（见下方批次结论） |
| THSC_LittleBugStorm | 255 | 已读（见下方批次结论） |
| THSC_MasterSpark | 87 | 已读（见下方批次结论） |
| THSC_MeteonicShower | 42 | 已读（见下方批次结论） |
| THSC_MeteorOnEarth | 111 | 已读（见下方批次结论） |
| THSC_MiracleFruit | 100 | 已读（见下方批次结论） |
| THSC_Mishagujisama | 34 | 已读（见下方批次结论） |
| THSC_MoonlightRay | 41 | 已读（见下方批次结论） |
| THSC_MosesMiracle | 43 | 已读（见下方批次结论） |
| THSC_MusouFuuin | 43 | 已读（见下方批次结论） |
| THSC_Nami_to_Tubu_no_Kyoukai | 44 | 已读（见下方批次结论） |
| THSC_NijuuKokushichou | 93 | 已读（见下方批次结论） |
| THSC_NonDirectionalLaser | 49 | 已读（见下方批次结论） |
| THSC_PerfectFreeze | 111 | 已读（见下方批次结论） |
| THSC_RedMagic | 105 | 已读（见下方批次结论） |
| THSC_SaikouRanbu | 69 | 已读（见下方批次结论） |
| THSC_SatuzinDoll | 124 | 已读（见下方批次结论） |
| THSC_ScarletShoot | 87 | 已读（见下方批次结论） |
| THSC_Spear_the_Gungnir | 19 | 已读（见下方批次结论） |
| THSC_StarbowBreak | 90 | 已读（见下方批次结论） |
| THSC_StardustReverie | 81 | 已读（见下方批次结论） |
| THSC_Yasaka_no_Kamikaze | 51 | 已读（见下方批次结论） |
| THSC_YouryokuSpoiler | 153 | 已读（见下方批次结论） |
| THSC_Zenzinrui_no_Hisouten | 68 | 已读（见下方批次结论） |

## 发现清单

### 批次 1（6 张，已逐行读 + 逐行 diff 原作）

- **THSC_Spear_the_Gungnir**（19 行）：空壳类，与 `ItemSpearTheGungnir` 侧生成 GUNGNIR 弹。无弹幕逻辑，无风险。原作同构（登记）。
- **THSC_Mishagujisama**（34 行）：`spellcard_main:30-31` 两次 `createCircleShot(..., 32, 0.7D, angleBase)`，`way=32` 非零 → `360F/32` 安全；`FORM_RICE`/`SIZE[FORM_RICE]` 均常量下标。原作同构（登记）。`rotate2`（:29）计算后未使用 —— 原作同为空算（登记）。
- **THSC_EternalMeek**（35 行）：`:29 createRandomRingShot(..., 3, 0.1D, 50F)`，`num=3` 非零。`SIZE[FORM_KNIFE]` 常量下标。**注意**：`:24 if(target.isAlive())` 与 `THSpellCard.onUpdate` 的 `target.getY()` 同样无 null 保护，但原版 `EntitySpellCard` 也不校验 target（`spellCardDeclaration` 由调用方保证非 null）→ 原作即如此（登记，与 REPORT.md 的 P1「target 空解引用」同一根因）。
- **THSC_Kasho_no_Eimin**（41 行）：`:37-38 createCircleShot(..., 15, 0.8D, angleBase)`，`way=15` 非零；`color` 仅取 PURPLE/YELLOW 常量；`time % 6`/`time % 9` 除数常量。原作同构（登记）。
- **THSC_MoonlightRay**（41 行）：`:30 float angleSpan = 360F / way;` 且 `:28 int way = 7;` **硬编码常量 7**（非字段），除数安全。`:36 LaserData.laser(THShotLib.BLUE, 0.8F + level * 0.2F, 40.0F, 6.0F, 10, 29 + level * 5, 0)` —— 参数位置为 `(form, color, width, length, damage, delay, end, special)`；此处首参 `THShotLib.BLUE` 被当作 **form**（值 1 = LASER_GUNGNIR），而「颜色」实为第二参 `0.8F+level*0.2F`（**float 被隐式截断为 int = 0..1**）。这与原作 `THSC_MoonlightRay` 逐字一致（原作 `LaserData.laser(THShotLib.BLUE, 0.8F + level * 0.2F, ...)`）→ **原作即如此，登记**。功能后果：月光雷的颜色随难度变化而非固定，属原作行为。`level` 取值 0..4，`29 + level*5` = 29..49 均为合法 delay 值。
- **THSC_MeteonicShower**（42 行）：`:33 int colors[] = {0, 2, 3, 4};` `:34 colors[rand.nextInt(4)]` —— `rand` 是 **`ThreadLocalRandom`（局部变量，:32）**，`nextInt(4)` 恒 0..3，下标安全；与字段 `rand` 无冲突。`:36 int damage = (int)(size * 15F)`，`size ∈ [0.2,0.6)` → `damage ∈ [3,9)`。`:38 createRandomRingShot(..., 1, 0.0D, 40F)`，`num=1` 非零 → 安全。原作同构（登记，仅 `Random`→`ThreadLocalRandom` 机械替换）。

**批次 1 结论：0 新发现。**

### 批次 2（6 张，已逐行读 + 逐行 diff 原作）

- **THSC_MosesMiracle**（43 行）：`:33-41` 的 `for(int i = -5; i < 6; i++)` 是**固定 11 次**循环（非 while），无无限循环风险。`:39-40 THShotLib.pos_Living(target)` 直接解引用 `target`（无 null 检查），但同函数 `onUpdate` 已在 `targetPos` 处先解引用 `target`，`target` 为 null 时更早崩溃 → 不构成新增路径。`FORM_LIGHT`/`BLUE` 常量。原作同构（登记）。
- **THSC_MusouFuuin**（43 行）：`:24 if(user != null && target != null)` **已显式双空检查**（良好实践，登记为样板）。`:26-28` 构造 `EntityMusouFuuin` 传入 `target`（此处已保证非 null）。`angle(time * 33F, 0F)` 走 `THSpellCard` 内部角度工具，无数组下标。原作同构（登记）。
- **THSC_Nami_to_Tubu_no_Kyoukai**（44 行）：`:35-36` `yaw += Mth.cos((float)(time + 75) / 50F) * 17F;` / `pitch += Mth.sin(...) * 17F;` —— 与 1.7.10 原作逐字一致（原作 `MathHelper.cos`/`sin`）。`yaw`/`pitch` 随 time 单调累加且**无界**（time<280 内 yaw 最大约 ±17*280 ≈ ±4760 度），但仅作为角度传入 `getVecFromAngle`（三角函数天然周期），无数值崩溃。`:41 createSphereShot(..., 12, 2.0D, 0F)`，`way=12` 非零 → 安全。原作即如此（登记）。
- **THSC_Catadioptric**（45 行）：继承 `THSC_ScarletShoot`，`:36-41` 固定 5 次循环，`angleSpan = 90F/4F` 常量除数。`:38 getRotationVectorFromAngle(...)` 无下标。`scarletShot(look, 0.6D, true)` 实参与原作一致。原作同构（登记）。
- **THSC_HoukaKenran**（47 行）：`:33-34` 与 `:44` 共 3 处 `createRingShot(..., 6 / 32, 0.6D, ...)`，`way` 分别为 6 与 32（常量非零）→ `360F/way` 安全。`SIZE[FORM_CRYSTAL]` 常量下标。`:28/:39 if(target.getHealth() > 0F)` 无 null 检查，与 `onUpdate` 先解引用 `target` 同级 → 非新增路径。原作同构（登记）。
- **THSC_NonDirectionalLaser**（49 行）：`:34/:45 LaserData.laser(THShotLib.RED + i, 0.7F, 20.8F, 7.0F, 25, 60, 0)`，`i ∈ [0,4]` → `form = RED+i = 0..4`，均在 `LASER_*` 合法域；与原作 `THShotLib.RED + i` 一致。`:29/:40` 固定 5 次循环。`:28/:39` 声音调用为移植改写（原作 `playSoundAtEntity(user, "thKaguyaMod.masterspark", ...)`）→ 等价。原作同构（登记）。

**批次 2 结论：0 新发现。**

### 批次 3（6 张，已逐行读 + 逐行 diff 原作）

- **THSC_Kerochan_Fuuu_ni_Makezu**（50 行）：`:30 for(i=0;i<4;i++)` 与 `:41 for(i=0;i<2;i++)` 均为固定次数循环；无除法、无数组下标（`SIZE[FORM_LIGHT]` 常量）；`pos(cardPosX, …)` 用缓存的 `cardPosX/PosY/PosZ`（`onUpdate` 每 tick 刷新）而非直接 `card.getX()`。原作同构（登记）。
- **THSC_Yasaka_no_Kamikaze**（51 行）：`:38/:45 createRingShot(..., 8, …, time*10F)`，`way=8` 常量非零 → `360F/8` 安全。`:31 if(target.isAlive())` 无 null 保护，与 `onUpdate` 同级 → 非新增路径。`shot(..., THShotLib.WIND01)` 的 special=180 未注册进 `SpecialShotRegistry`，回落 `EntityTHShot.entityHitSpecialProcess` 的内建 `case WIND01`（**预期行为**，非缺陷）。原作同构（登记）。
- **THSC_Zenzinrui_no_Hisouten**（68 行）：`:56 specialShot_hitBlock` 内 `THShotLib.createShot(shot.user, …)` —— `shot.user` 在**客户端**可能为 null（`user` 是不同步的普通字段）。已核实不构成 NPE：`THShotLib.createShot` 只用 `source.level()`（source 传的是 `shot` 本身，非 null），新弹的 `user` 为 null 时服务端下一 tick 自行 `discard()`、客户端根本不 `addFreshEntity`。`:41 shot.setShotSize(shot.getShotSize() + 0.1F)` 在 `tickCount < 20` 内单调放大，`DATA_SHOT_SIZE` 以 `(int)(size*100F)` 存储，最大 `(0.3+2.0)*100 = 230`，远未溢出。**0 新发现**（原作即如此，登记）。
- **THSC_HikouchuuNest**（69 行）：`:27 while(!flag && count < 4)` —— 这是本包内**唯一** `while` 生成弹的循环，但**有界**：`count++` 仅在方块命中分支执行，且 `count < 4` 为循环条件。推演：4 次连续方块命中即退出；只要有一次未命中即 `flag = true` 退出。**不存在无限循环**。`:42-44` 用 `movingObjectPosition != null && getType() == BLOCK` 判定，比原作 `!= null` 更严格（原作 `func_147447_a` 未命中返回 null，语义等价）。原作即如此（登记）。
- **THSC_SaikouRanbu**（69 行）：`:33 color[time % 4]`，数组 4 元素、`time >= 0` → `0..3` 恒安全；`:49/:65 color[rand.nextInt(3)]`，数组 3 元素、`RandomSource.nextInt(3)` 恒 0..2 → 安全（此处 `rand` **未被遮蔽**，是类字段）。`:33 createRingShot(..., 15, …)`，`way=15` 非零。`time % 30`/`time % 4` 除数常量。原作同构（登记）。
- **THSC_Kachoufuugetu**（73 行）：`:47 for(i=0;i<5;i++)`、`:56 for(j=0;j<5;j++)` 固定次数。`:53-60` 用 `mother.getYRot()` / `mother.angle` —— `mother` 来自 `THShotLib.createShot(...)`，该工厂**恒返回非 null**（客户端也构造出对象，只是不 `addFreshEntity`）→ 无 NPE。`angle`/`angle2` 无界但仅作角度参数。无除法。原作同构（登记）。

**批次 3 结论：0 新发现。**

### 批次 4（6 张，已逐行读 + 逐行 diff 原作）

- **THSC_ScarletShoot**（87 行）：`:32 float angleSpan = 180F / 4F;` —— **常量除数**（不是原作 `180F/4F`？已 diff 确认逐字相同）→ 无除零。`:50 float angleSpan = 6F / 2F;` 常量。`:34/:52` 固定 3/5 次循环；`scarletShot` 内 `:74 for(i<11)`、`:80 for(i<22)` 固定次数（**非 while**，无无限循环）。`:78/:84 createRandomRingShot(..., 1, 0.0D, 30F/60F)`：`num=1` **非零** → `THShotLib` 内 `for(i<num)` 只发 1 发，无除零。`SIZE[FORM_BIG]`/`SIZE[FORM_MEDIUM]`/`SIZE[FORM_SMALL]` 均常量下标。`THShotLib.RED/BLUE` 常量。原作同构（登记）。**0 新发现。**
- **THSC_IcicleFall**（92 行）：**本包第一处可疑点，已排除**。`:41-42` 链式调用 `THShotLib.createShot(...).rotate = THShotLib.rotate_Default();` —— 若 `createShot` 返回 null 会 NPE。已核实 **`THShotLib.createShot` 恒返回非 null**（客户端也构造对象、只是不 `addFreshEntity`，见 `THShotLib.java:241-258`）。同时确认 `rotate_Default()` 在**原作与移植中都返回新对象**（原作 `Vec3.createVectorHelper(0,1,0)`，移植 `new Vec3(...)`）→ 无共享实例被污染。`:39 for(i=1;i<num;i++)`，`num = 4 + level*2`（level 0..4 → num ∈ [4,12]）非零。`:54 shot.tickCount >= shot.getDeadTime()` —— `getDeadTime()` 返回 `DATA_SHOT_END_TIME`（`:57 setDeadTime(120)` 写入同槽，见 `EntityTHShot.java:1677-1689`）→ 语义自洽。原作同构（登记）。**0 新发现。**
- **THSC_KappaPororoca**（80 行）：`:51 if(shot.tickCount % 4 == 3)` 常量除数。`:54 shot.user.getYRot()` —— `shot.user` 服务端非 null（`EntityTHShot.tick` 会 `discard` user==null 的弹），客户端该弹已被 `discard`（`:604-613` 无端判断），故不可达。`:68 shot.bound(hitResult, 0.9D, 4)` 与 `EntityTHShot.bound` 签名的 `returnCount` 语义一致（原作同参）。原作同构（登记）。**0 新发现。**
- **THSC_StardustReverie**（81 行）：`:40 int way[] = {4,4,7,9,10};` `:44 way[level]` —— `level` 0..4（config `defineInRange(0,4)`，且 `level==0` 时 `EntityDanmakuMob.getTarget()` 恒 null → `spellcard_main` 不可达），索引恒界内；且 `way[]` 全部元素 ≥4 → `360F/way`（`createCircleShot` 内）无除零。原作同构（登记）。**0 新发现。**
- **THSC_MasterSpark**（87 行）：`:53 angleSpan = 360F / 7F` 常量；`:75 ShotData.shot(FORM_STAR, time % 7, …)` → color 0..6，`FORM_STAR` 常量；`:48-51` `lookAt.yRot((float)Math.PI * 2)` 与原作 `lookAt.rotateAroundY((float)Math.PI * 2)` **单位一致（均为弧度）**，且 `yRot` 返回**新对象**（`Vec3` 不可变）→ 无共享 Vec3 被就地改写的数据污染（这点比原作更安全）。`:61 for(i<7)` 固定次数。`:35` 构造 `EntityMiniHakkero` 传入 `target`（此处 `target` 已在 `onUpdate` 解引用过）。原作同构（登记）。**0 新发现。**
- **THSC_StarbowBreak**（90 行）：`:49 float span = 360F / num;` 且 `:48 int num = time % 60;` —— 外围守卫 `:36 time % 60 >= 3 && <= 23 && time % 3 == 0` → `num ∈ {3,6,9,12,15,18,21}`，**恒非零** → `360F/num` 安全。`:52 rainbowPattern[num / 3 - 1]` —— `num/3 ∈ {1..7}`，减 1 后 ∈ {0..6}，数组 7 元素 → 界内。`:54 createRingShot(..., num*2, …)` `way ∈ [6,46]` 非零。`:46 Vec3 side` 计算后未使用（原作同样未使用，编译期消除）。原作同构（登记）。**0 新发现。**

**批次 4 结论：0 新发现。**

### 批次 5（6 张，已逐行读 + 逐行 diff 原作）

- **THSC_NijuuKokushichou**（93 行）：`:34 for(i<30)` 固定次数；`:42 shot(FORM_BUTTERFLY, i % 2, …)` → color 0/1，`SIZE[FORM_BUTTERFLY]` 常量；`:42 createCircleShot(..., 8, 0.2D, 0F)` `way=8` 非零；`:67 shot.getShotColor() % 2` 常量除数（返回值恒 0..7，见 `ShotIdEncodingBoundaryTest`）。原作同构（登记）。**0 新发现。**
- **THSC_MiracleFruit**（100 行）：`:63 for(j<8)` / `:66 for(i<6)` 固定嵌套次数（**非 while**）；`:49 createCircleShot(..., 8, 0.2D, rand.nextFloat()*360F)` `way=8` 非零；`SIZE[FORM_OVAL]` 常量；`:68 ShotData.shot(FORM_SMALL, RED, 0.3F, 6.0F, 3 + i*2, 90)` delay 为 3..13。`:38/:93 user/`living.removeAllEffects()` 与 `onUpdate` 的 `user` 同级；`:39` 只在 `time == 1` 且 `user` 已由 `onUpdate` 解引用。原作同构（登记）。**0 新发现。**
- **THSC_RedMagic**（105 行）：`:41/:55 for(i<3)` 固定；`:44/:47/:58/:61 createCircleShot(..., 6, 0.7D)` `way=6` 非零；`:73 shot.tickCount % 3 == 0` 常量除数；`:77 30 - shot.tickCount + 120` 为 end 值（可为负 → `EntityTHShot` 中 `end<=0` 弹会立刻过期，与原作同式）。`SIZE[FORM_BIG]`/`SIZE[FORM_LIGHT]` 常量；`angle2` 0/60/120 仅作角度。原作同构（登记）。**0 新发现。**
- **THSC_SatuzinDoll**（124 行）：`:37 createSphereShot(..., 32, 1.0D, time*6F)` `way=32` 非零；`:41 int way = 9;` 常量；`:46/:48 createCircleShot(..., way=9, …)` 非零。`:54/:83 List<Entity> list = level().getEntities(...)` 是**快照**，遍历中 `discard()`（`:107`）无 CME。**`:90 entityTHShot.user.equals(this.user)` —— 已核查为非新增 P1（见下方「P1 候选」）。** 原作同构（登记）。**0 新发现。**
- **THSC_GagoujiCyclone**（145 行）：`:42/:53 float angle = … * (30F + level * 30F) + 90F;` 仅作角度参数；`:46/:57/:66 createRingShot(..., 9 / 3, 1.0D, …)` `way` 常量非零；`:72/:73 createRingShot(..., 8 + level*4 / 4 + level*2, …)` —— `level ∈ [0,4]` → `8+level*4 ∈ [8,24]`、`4+level*2 ∈ [4,12]`，**恒非零** → `360F/way` 安全；`:70/:100/:106 time % 20`、`:40/:51/:64 time % 120` 除数常量。`SIZE[FORM_ARROW]`/`SIZE[FORM_CIRCLE]` 常量。**注意**：`:96` 被注释掉的 `break;` 使 `case SPECIAL_GAGOUJI01` **故意贯穿**到 `SPECIAL_GAGOUJI02` 的代码块（`if/else if`）——与原作逐字一致（原作同样注释掉了 `break;` 与 `case SPECIAL_GAGOUJI02:` 标签），是**拉长周期**的 1.7.10 原始设计，非移植笔误（登记）。`:114-127 user instanceof EntityDanmakuMob / setDeadTime / shotLimitSpeed = 0.2 + ModConfig.danmakuLevel*0.1` 均与原作一致（仅 `THKaguyaConfig`→`ModConfig` 改名）。**0 新发现。**
- **THSC_YouryokuSpoiler**（153 行）：`:44/:52 shot(FORM_SCALE, rand.nextInt(7), 0.3F, 3.0F, 6, 70, SPECIAL_SPOILER01)` → color 0..6，`SIZE` 无下标；`:44/:52 createCircleShot(..., 7, 1.5D, ±time*3F)` `way=7` 非零。`:65/:90/:123/:138` **均有 `shot.user != null` / `hitEntity instanceof LivingEntity` 守卫**（本包内最严谨的空检查写法，登记为样板）。`:140 LivingEntity living = (LivingEntity)hitEntity;` 强转——**已核查非新增 CCE**：进入条件是 `shot.user != null && hitEntity.is(shot.user)`，而 `Entity.is(Entity)` 实现为 `this == entity`（已查 NeoForge 21.1.252 源码 `Entity.java:2618`），故 `hitEntity` 与 `shot.user`（`LivingEntity`）**同一对象** → 必为 LivingEntity（原作同式，且原作无 `instanceof` 守卫，移植反而更安全）。原作同构（登记）。**0 新发现。**

**批次 5 结论：0 新发现。**

### P1 候选（本轮唯一升级项，需主控裁决）

| 位置 | 说明 |
|---|---|
| `spellcard/THSC_SatuzinDoll.java:90`（`entityTHShot.user.equals(this.user)`）、`entity/item/EntitySakuyaWatch.java:238`、`entity/item/EntitySakuyaStopWatch.java:181`（`spellCard.user.equals(this.user)` / `spellCard.user.equals(this.userEntity)`） | **潜在 NPE，当前不可达（P1-latent）**。`EntitySpellCard.user` 是 public 非同步字段；客户端由 `EntityType.create(level)` 走**双参构造器**（`EntitySpellCard.java:88`，不设置 `user`），且 `addAdditionalSaveData` 为空、`defineSynchedData` 只同步 `DATA_SPELL_CARD_NUMBER` → **客户端上 `spellCard.user` 恒为 null**，`user.equals(...)` 会 NPE。当前被 `&&` 短路保护：客户端的 `canMoveInTimeStop` 同为非同步字段、恒 false（`EntitySpellCard.java:42`，仅六参构造器 `:130` 赋值）→ 短路后不执行 `user.equals`。**一旦将来把 `canMoveInTimeStop` 纳入同步，即变成客户端每 tick NPE 崩溃**。建议改为 `this.userEntity.equals(spellCard.user)`（对调后 null 安全）或直接加 `spellCard.user != null &&`。原作 `spellCard.user.equals(...)` 同式（登记），但移植新增了客户端可达路径（`EntitySakuyaStopWatch.tick` **无 `isClientSide` 短路**，见 `EntitySakuyaStopWatch.java:127-181`），故列为移植引入的脆弱点。 |

### 批次 6（5 张，已逐行读 + 逐行 diff 原作）

- **THSC_FujiyamaVolcano**（160 行）：**发现除零候选，判定为原作即如此（登记）**。`:43/:49` 的 `-(2.4D / user.distanceTo(target))` —— `target` 已在 `onUpdate` 解引用，但 `user.distanceTo(target)` **可为 0**（当弹主与目标重合时）→ 得到 `Infinity`，`Infinity` 作为 `acceleration` 传入 `EntityTHShot` 后会让 `motionX/Y/Z` 变为 `±Infinity` → 位置 NaN（实体位置污染）。原作逐字为 `-(2.4D / user.getDistanceToEntity(target))`，**1.7.10 同样可除零**。触发条件苛刻（弹主与目标精确重合），且 1.7.10 同样存在 → 按纪律**登记不报**。另 `:126-129 getJustTimeAcceleration(int time, …)` 无调用点（原作亦无），不构成路径。`:66 createSphereShot(..., 44, 0.0D, …)` `way=44` 在 `ways[][]`（45 项，索引 0..44）界内 → `ways[44]` 存在（末项 `{1,3,6,12,12,6,3,1}`）→ **安全**；`:78/:91/:104 for(i<2)`、`:139 for(i<40)` 固定次数。`SIZE[FORM_BIG/MEDIUM/SMALL/TINY]` 常量。**0 新发现。**
- **THSC_PerfectFreeze**（111 行）：`:49-50 int num = 4 + level; if(num > 10) num = 10;` **已夹紧**（良好实践，登记为样板）。`:63 level().getEntities(...)` 为快照，遍历中 `shot.discard()`（`:74`）无 CME。`:72-73` 读取 `shot.getShotForm()/getShotSize()/shotDamage/getShotEndTime()` 并重发新弹，`end = getShotEndTime() - tickCount + 50` 可为负（弹已过期时）→ 与 `EntityTHShot` 的 `end<=0` 立即过期语义一致，原作同式。`SIZE[FORM_CIRCLE]` 常量。原作同构（登记）。**0 新发现。**
- **THSC_MeteorOnEarth**（111 行）：`:36 int way[] = {0, 5, 7, 4, 6};` `:44/:60 way[level]` —— `level ∈ [0,4]` 界内；**`level == 0` 时 `way[0] = 0`**，会传给 `createRingShot(..., 0, …)` → `THShotLib:580 float span = 360F / way;` **除零 → `Infinity`**。但 `level == 0` 要求 `ModConfig.danmakuLevel == 0`，而 `EntityDanmakuMob.getTarget()` 在 `danmakuLevel <= 0` 时恒返回 null（`EntityDanmakuMob.java:463-466`）→ 符卡无法宣言/无法进入 `spellcard_main`。**判定为不可达**（与 REPORT.md 中同一裁决一致），登记。`:45/:61 level >= HARD` 用常量 8 分支。原作同构（登记）。**0 新发现。**
- **THSC_LittleBugStorm**（255 行，本包最长）：`:52 int way = 1 + level * 2;` → `level ∈ [0,4]` → `way ∈ [1,9]`，**恒非零**；`:62/:70/:78/:86 createSphereShot(..., way, 0.3D, 1.4F)` 内部走 `ways[way]`（way ≤ 9 < 45）→ 界内。`:124/:184 shot.tickCount % 3 == 0` 常量除数。`:146/:205` 的 `shot.angle = new Vec3(x, sin(...)*30F*0.01, z)` —— 移植把原作的 `shot.angle.yCoord = …`（可变 Vec3 就地改写）改为新建 Vec3，**语义等价**（`:145` 有注释说明），且避免了不可变 Vec3 的就地改写风险。`:100-142/:156-213` 的多段 `specialShot` 状态机（01→02→03、11→12→13）与原作逐 case 对照一致（含 `setShotEndTime(20)`/`setShotEndTime(80)` 等值）。`:107/:119/:152/:167/:179/:211/:223/:235 shot.reCreate(); shot.delete();` —— `reCreate()` 在服务端返回新弹、客户端返回 null（但返回值未被使用）→ 无 NPE。**0 新发现。**
- **THSC_Fafurotskies_no_Kiseki**（162 行）：`:49 int pattern = rand.nextInt(100);` → `:55` 作为 `acceleration` 传入（0..99）；`:69 int pattern = (int)shot.shotLimitSpeed;` 把 acceleration 还原为 pattern → 与 REPORT.md 中登记的 P1「速度/模式语义混用」同一处，**原作逐字一致**（原作 `int pattern = (int)shot.shotLimitSpeed;`）。`:74/:87` 用 `LocalDate.now()` 替代 `world.getCurrentDate()`（机械替换，语义等价）。`:76/:91/:108/:114` 生成 `EntityDanmakuCreeper`/`EntityNuclearShot`/`Creeper`/`Chicken` 均在 `!world.isClientSide` 内（`:79/:89/:94/:111/:118/:128/:138`）。`:104 pattern <= 70` / `:116 <= 80` / `:124 <= 90` 分支与原作一致。`FORM_KNIFE`/`RANDOM` 常量。**0 新发现。**

**批次 6 结论：0 新发现。**

---

## 全包结论

- **覆盖率：35/35 张 THSC_\* 全部逐行读完，并逐行 diff 1.7.10 原作**（`src/main/java/thKaguyaMod/entity/spellcard/`，含 `THSC_BrilliantDragonBullet` 未移植、原作亦未注册）。
- **新增 P0：0 项。**
- **新增 P1：1 项**（P1-latent，见「P1 候选」：`THSC_SatuzinDoll.java:90` + `EntitySakuyaWatch.java:238` + `EntitySakuyaStopWatch.java:181` 的 `spellCard.user.equals(...)`，当前被非同步字段 `canMoveInTimeStop` 的短路保护，一旦纳入同步即变客户端 NPE）。
- **登记（原作即如此，不计入缺陷）：8 项** —— `THSC_MoonlightRay:36` LaserData 参数位语义混用、`THSC_Nami_to_Tubu_no_Kyoukai:35-36` 无界角度累加、`THSC_StarbowBreak` 各处、`THSC_GagoujiCyclone:96` 故意贯穿的 case、`THSC_FujiyamaVolcano:43/49` 距离做除数（可除零，原作同）、`THSC_MeteorOnEarth:44/60` `way[0]==0`（不可达）、`THSC_Fafurotskies:69` 速度当模式、`THSC_YouryokuSpoiler:140` 强转（实为同对象，安全）。
- **良好实践登记（4 处，供其他批次对照）**：`THSC_MusouFuuin:24` 双空检查、`THSC_YouryokuSpoiler:65/90/123/138` 空检查、`THSC_PerfectFreeze:49-50` num 夹紧、`THSC_HikouchuuNest:27` 有界 while。
- **无限循环风险：0 项**（全包 while 仅 `THSC_HikouchuuNest:27`，有 `count < 4` 上界）。
- **数组下标越界：0 项**（全部颜色/弹形/way 数组下标已逐条核对：`THSC_SaikouRanbu:33` `time%4`、`THSC_StarbowBreak:52` `num/3-1`、`THSC_NijuuKokushichou:42` `i%2`、`THSC_MeteorOnEarth/StardustReverie` 的 `way[level]` 等均界内）。
- **除零：0 项可达**（3 处已核实不可达：`THSC_FujiyamaVolcano:43/49`、`THSC_MeteorOnEarth:44/60`、`THSC_FujiyamaVolcano:128` 无调用点；其余 `360F/way` 的 way 全部为常量或已证明非零）。
- **转录笔误：0 项**（35 张逐行 diff 后，除机械替换——`Vec3` 不可变、`Random`→`ThreadLocalRandom`、`rotationYaw/Pitch`→`getYRot()/getXRot()`、`worldObj`→`level()`、`registerSpecialShot(Class,…)`→`(this,…)`——**未发现任何参数值、常量、条件、循环边界的差异**）。

### 覆盖声明
- 逐行通读：35 个 `THSC_*.java`（共 2899 行）+ 本轮涉及的 `THSpellCard.java`、`EntitySpellCard.java`、`THShotLib.java`（`ways[][]` 表与 `create*` 生成器）、`EntityTHShot.java`（`setDeadTime`/`reCreate`/`discard`）、`EntitySakuyaWatch/StopWatch`（时间停止遍历）、`Entity.java`（`is(Entity)` 语义，NeoForge 21.1.252 源码）。
- 逐行 diff：35/35 对照 `src/main/java/thKaguyaMod/entity/spellcard/` 同名文件（`diff -w -B` 归一化空白后过滤注释/import/package）。
- 未覆盖：`client/**`（另一批）；`THSC_BrilliantDragonBullet`（原作亦未注册，不属 35 张）。
- 证据获取方式：`grep`/`diff`/`javap`（读 `build/libs` 已编译产物核对调用点）+ 只读 `Read`；**未修改任何既有文件、未运行 gradlew、未执行 git 命令**。

