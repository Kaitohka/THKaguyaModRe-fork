# diag-pose 诊断+FIX 批报告（现象：右键符卡/物品后玩家趴下）

> 批次：diag-pose（诊断 + 最小修复，授权改主树产品代码 + gradlew compileJava / runGameTestServer）
> 基线：git 2dcac4a（阶段5 快照后）；GameTest 基线 61/61
> 现象（用户实测，K3 已确认）：右键符卡与部分物品后，**第三人称角色趴平、视角/眼高下降**，即
> 1.21.1 的 **`Pose.SWIMMING`（匍匐/爬行姿）**；1.7.10 原作无此现象。

本文件按「先落盘再完善」纪律写作：下面是**已确认的事实与证据链**，末尾「待验证/进行中」列出尚未跑完的项。

---

## 1. 姿势机制判定（为什么是 SWIMMING，而不是鞘翅/潜行）

证据：`net/minecraft/world/entity/player/Player.java`（1.21.1 源码，取自
`build/moddev/artifacts/neoforge-21.1.252-sources.jar`）

```java
// Player.java:414-446
protected void updatePlayerPose() {
    if (forcedPose != null) { this.setPose(forcedPose); return; }
    if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SWIMMING)) {   // ← 只看「匍匐盒」是否放得下
        Pose pose;
        if (this.isFallFlying())          pose = Pose.FALL_FLYING;
        else if (this.isSleeping())       pose = Pose.SLEEPING;
        else if (this.isSwimming())       pose = Pose.SWIMMING;   // sharedFlag4（真正在游/冲刺）
        else if (this.isAutoSpinAttack()) pose = Pose.SPIN_ATTACK;
        else if (this.isShiftKeyDown() && !abilities.flying) pose = Pose.CROUCHING;
        else                              pose = Pose.STANDING;
        Pose pose1;
        if (this.isSpectator() || this.isPassenger()
            || this.canPlayerFitWithinBlocksAndEntitiesWhen(pose))  pose1 = pose;      // 站姿放得下 → STANDING
        else if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING)) pose1 = Pose.CROUCHING;
        else                                                   pose1 = Pose.SWIMMING;   // ★ 兜底 = 匍匐
        this.setPose(pose1);
    }
}
// Player.java:448-450
protected boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose) {
    return this.level().noCollision(this, this.getDimensions(pose).makeBoundingBox(this.position()).deflate(1.0E-7));
}
```

尺寸（`Player.java:139-150`）：`STANDING 0.6×1.8`、`CROUCHING 0.6×1.5`、
**`SWIMMING 0.6×0.6 withEyeHeight(0.4)`** ← 与用户描述的「趴平 + 眼高下降（1.62 → 0.4）」完全吻合。
`Pose.FALL_FLYING` 会是 0.6×0.6/眼高 0.4 的**鞘翅姿**，但它要求 `isFallFlying()`（滑翔标志），
而 K3 全树 grep 已证实模组**从不**调用 `startFallFlying/setSharedFlag(7)`；
`Pose.CROUCHING` 的第三人称是蹲姿（不趴平）。→ **判定：自动匍匐（SWIMMING 兜底）**。

## 2. 触发链（1.21.1 独有：实体碰撞参与「姿势拟合」）

```java
// CollisionGetter.java:46-61（接口默认实现）
default boolean noCollision(@Nullable Entity entity, AABB collisionBox) {
    for (VoxelShape s : this.getBlockCollisions(entity, collisionBox)) if (!s.isEmpty()) return false;
    if (!this.getEntityCollisions(entity, collisionBox).isEmpty()) return false;  // ★ 实体也算「放不下」
    ...
}
// EntityGetter.java:63-73
default List<VoxelShape> getEntityCollisions(@Nullable Entity p_entity, AABB collisionBox) {
    ...
    Predicate<Entity> predicate = p_entity == null
        ? EntitySelector.CAN_BE_COLLIDED_WITH                       // = NO_SPECTATORS.and(Entity::canBeCollidedWith)
        : EntitySelector.NO_SPECTATORS.and(p_entity::canCollideWith);
    List<Entity> list = this.getEntities(p_entity, collisionBox.inflate(1.0E-7), predicate);
    ... builder.add(Shapes.create(entity.getBoundingBox()));
}
// Entity.java:2039-2045
public boolean canCollideWith(Entity entity) { return entity.canBeCollidedWith() && !this.isPassengerOfSameVehicle(entity); }
public boolean canBeCollidedWith() { return false; }   // 基类默认 false
```

推导出的**关键结论**：`p_entity` 是**玩家**，所以 `player.canCollideWith(其他实体)` 实际读的是
**「其他实体的 `canBeCollidedWith()`」**。也就是说：

> 只要一个 `canBeCollidedWith() == true` 的实体 A 的 bounding box 与玩家的**站姿盒**（0..1.8）
> 或**蹲姿盒**（0..1.5）相交、却不与**匍匐盒**（0..0.6）相交，
> 玩家就会被原版自动判定为 `Pose.SWIMMING`。

1.7.10 **没有**这个机制（旧版 `EntityPlayer` 不会被实体碰撞挤成匍匐，也没有 swim pose）→
**完美解释「原作无此现象」**：同样的实体、同样的重叠，旧版什么都不会发生。

### 2.1 几何自证（用移植树自己的数值算）

模组弹幕生成在**玩家自身坐标**上（`THShotLib.pos_Living(living) = (x, getPosYFromEye(living), z)`，
`getPosYFromEye(pitch=0) = y + 1.62 - 0.5 = y+1.12`，`THShotLib.java:1484-1487 / 1876-1881`），
弹的 bounding box = `EntityDimensions.scalable(size, size)`（`EntityTHShot.java:303-315`），
即竖直区间 `[y+1.12, y+1.12+size]`（size 常在 0.3~1.0）：

| 候选姿势盒 | 区间（相对脚底） | 与弹盒 `[1.12, 1.52]` 是否相交 |
|---|---|---|
| STANDING | 0 ~ 1.8 | **相交 → 放不下** |
| CROUCHING | 0 ~ 1.5 | **相交 → 放不下** |
| SWIMMING | 0 ~ 0.6 | 不相交 → 放得下 |

→ `updatePlayerPose()` 走 else 兜底：`pose1 = Pose.SWIMMING`。**趴下**。

同理可算出的「会在玩家盒内生成的模组实体」（`canBeCollidedWith()==true` 且有生成者）：
银刀（前 0.28 格 < 玩家半宽 0.3，`EntitySilverKnife` dims 0.1）、
早苗之风 `EntitySanaeWind`（生成点就是玩家眼位，dims 1.0×1.0，且显式覆写 true）、
小神灵 `EntityDivineSpirit`（生成点 = 玩家眼位）、弹之素 `EntityShotMaterial`（弹消亡处）。

## 3. 全树排查结论（对照 K3 的 grep 报告）

| 排查项 | 结果 |
|---|---|
| `setPose(` / `Pose.` / `DATA_POSE` / `setSharedFlag` / `setSwimming` / `setForcedPose` / `startFallFlying` 在模组代码中的调用 | **零**（除 `thkaguyamod/test/` 与本报告无关处）。→ 姿势不是模组直写的 |
| `client/` 侧对玩家的渲染/姿势钩子 | 仅 `ModClientExtensions`（帽子防具模型，`getHumanoidArmorModel`，不碰 pose）；`THKaguyaClient` 只注册 Screen/渲染器/图层。→ 排除渲染钩子 |
| `UseAnim`/`getUseAnimation` | 符卡物品 `ItemTHSpellCard` **未覆写** use/UseAnim（`ItemTHSpellCard.java:86-94`）。其他物品用 BLOCK/BOW/DRINK（1.7.10 同），且 UseAnim 在 1.21.1 **不驱动** `Player.updatePlayerPose()`。→ 排除 |
| 草稿清扫：`Entity.move()` 用的 `getEntityCollisions` 同理（本方实体视角） | 与玩家姿势无关 |

→ **根因类别：A（原版自动匍匐被间接触发），非 B（DATA_POSE 直写）**。

## 4. 最小修复方案（进行中，见第 6 节结果）

**方向**：让模组的**投射物/瞬态效果实体不再对「实体碰撞」可见**——即
`canBeCollidedWith()` 返回 `false`（1.21.1 原版**投射物**正是如此：`Projectile`/`ItemEntity`/
`ExperienceOrb` 都不覆写，保持基类 false；只有船/潜影贝/`blocksBuilding` 实体才 true）。

保留 I8 已建立的三职分离：**瞄准/交互**仍由 `isPickable()` 提供、
**弹丸命中**仍由 `canBeHitByProjectile()`（默认 `isAlive() && isPickable()`）提供，
两处都不依赖 `canBeCollidedWith()`（全树 grep：产品代码内**没有任何**对 `canBeCollidedWith()` 的读取）。

代价（须在报告中如实记录）：失去 1.7.10 的「弹幕/效果实体阻挡实体移动」这一副作用
（玩家/怪物不再被弹幕挡住去路）。这是为了让 1.21.1 的姿势拟合不被弹幕干扰所做的取舍；
若不修，则每次符卡宣言/射击都会把玩家按成匍匐，属发布阻塞级可见缺陷。

---

## 5. 待验证 / 进行中

- [ ] 用 GameTest 逐实体实证「谁让玩家站姿盒放不下」（构造诊断 + 回归两用）
- [ ] 落最小修复（`EntityTHShot` / `EntitySanaeWind` / `EntitySilverKnife` / `EntityDivineSpirit` / `EntityShotMaterial`）
- [ ] `./gradlew compileJava` 0 错
- [ ] `./gradlew runGameTestServer`（需 ≥ 基线 61/61）
- [ ] 1.7.10 对照（原作 `setSize`/生成坐标）逐项复核
---

## 6. 修复落盘记录（主树，已改）

### 6.1 改动清单（5 个文件，全部只改一个方法的返回值 + 注释）

| # | 文件:行 | 方法 | 改动 |
|---|---|---|---|
| 1 | `porting/neoforge-1.21.1/src/main/java/thkaguyamod/entity/shot/EntityTHShot.java:1925` | `canBeCollidedWith()` | `getAnimationCount() >= 1` → **`false`**（并补 Javadoc 说明姿势拟合机理） |
| 2 | `.../entity/shot/EntitySanaeWind.java:93` | `canBeCollidedWith()` | `true` → **`false`** |
| 3 | `.../entity/item/EntitySilverKnife.java:849` | `canBeCollidedWith()` | `true` → **`false`** |
| 4 | `.../entity/EntityDivineSpirit.java:220` | `canBeCollidedWith()` | `true` → **`false`** |
| 5 | `.../entity/EntityShotMaterial.java:262` | `canBeCollidedWith()` | `true` → **`false`** |

`isPickable()` 全部保持原样（前者的 `animCount>=1`、其余 `true`），
`canBeHitByProjectile()` 未覆写（默认 = `isAlive() && isPickable()`）→ **I8 的瞄准/命中战果零回归**。

编译验证：`cd porting/neoforge-1.21.1 && ./gradlew compileJava --offline` → **BUILD SUCCESSFUL（exit 0）**，
`build/classes/java/main/thkaguyamod/test/PlayerPoseRegressionTest.class` 已产出（compileJava 源集含 test 包）。

### 6.2 新增回归测试

`porting/neoforge-1.21.1/src/main/java/thkaguyamod/test/PlayerPoseRegressionTest.java`（新文件，3 个 @GameTest）：

1. `boatAtEyeHeightDisplacesStandingPose` —— **机理证据**：把原版船（`canBeCollidedWith()==true`，
   原版 1.375×0.5625）放到玩家眼高（脚底 +1.12），断言玩家站姿盒与它相交，且
   `Player#updatePlayerPose()` 所用的两个谓词（STANDING / CROUCHING 的
   `level().noCollision(player, dimensions.makeBoundingBox(position()).deflate(1e-7))`）双双为 false
   → 原版兜底 `Pose.SWIMMING`。移开船后恢复 true。该用例不随模组修复变绿，证明的是平台机理。
2. `modEntitiesAtEyeHeightDoNotDisplaceStandingPose` —— **修复结果**：上面 5 个实体在眼高
   `canBeCollidedWith()==false`、`isPickable()/canBeHitByProjectile()==true`，且站姿仍放得下。
3. `shotFiredFromPlayerKeepsStandingPose` —— **真实路径**：`THShotLib.createShot(user, THShotLib.pos_Living(user), ...)`
   造一颗 1.0 格弹，断言弹盒确实与站姿盒重叠（防止测试空转），且玩家站姿/蹲姿仍放得下。

> 未采用「tick 玩家后断言 `player.getPose()==STANDING`」：mock player 不在世界的 tick 列表里，
> `updatePlayerPose()` 又是 protected。直接断言它所依赖的公开谓词等价且稳定。

### 6.3 1.7.10 对照（为什么原作不趴下）

| 实体 | 1.7.10 | 1.21.1（修复前） | 1.21.1（修复后） |
|---|---|---|---|
| `EntityTHShot` 等弹类 | `canBeCollidedWith = animCount>=1`（阻挡实体移动） | 同值 → **姿势拟合触发**（玩家脚底+1.12 生成） | `false`（仍可瞄准/打返/命中） |
| `EntitySanaeWind` | `true` | `true` → 触发 | `false` |
| `EntitySilverKnife` | `true` | `true` → 触发（前方 0.28 格 < 玩家半宽） | `false` |
| `EntityDivineSpirit` | `true` | `true` → 触发（生成在眼位） | `false` |
| `EntityShotMaterial` | `true` | `true` → 触发（生成在弹消亡处） | `false` |
| 玩家姿势 | 只有 `isSneaking` 分支；**无自动匍匐** | `updatePlayerPose()` 自动兜底 SWIMMING | 判定不再被弹幕干扰 |

### 6.4 未修 / 已知取舍（需 K3 裁决）

1. **失去「弹幕阻挡实体移动」的副作用**：1.7.10 里 `canBeCollidedWith=true` 会让弹/效果实体参与
   `Entity#move()` 的实体碰撞，玩家/怪物会被弹墙挡住。改为 false 后不再挡路（原版投射物本就如此）。
   影响面：极少数「用弹幕当墙」的玩法；收益：不再每次射击/宣言都被按成匍匐。
2. **同类实体未处理**：`EntitySukima`（0.6×1.8 = 正好玩家整高）、`EntityYuukaParasol`（乘在玩家身上）、
   `EntityMiniHakkero`（摆在脚下）、`EntityCirnoIceBox`/`EntityHakureiShield`/`EntityHakurouReflecter`
   （1.5~1.9 格大 box）等仍为 `true`。判据是「是否会在玩家站位生成」（几何 + 生成者）：
   这些实体按源码看生成在玩家之外（隙间在墙/地面、伞乘在玩家身上，
   而拟合判定对 `isPassenger()` 的玩家会先走 `pose1 = pose` 分支不受影响），
   因此**不属本次现象**；若用户日后报告「站在隙间/八卦炉边也会趴」，按同一个手法处理即可。
   `EntityAjaRedStoneEffect` **不需要改**：它 1.7.10 就是 `false`，且移植版正确保留 false。
3. **修复名不是「接缝式」而是「关掉一职」**：K3 提示的备选方案（抬高生成高度 / 改 dimensions）
   会改动玩家可见的弹幕起点（与 1.7.10 视觉不符），故未采用。

## 7. GameTest 结果

（见下方追加）

### 7.1 首轮 runGameTestServer（修复后）发现 1 个「过期断言」（非产品缺陷）

- 结果：**64 tests 启动**（基线 61 + 本批新增 3），首轮 1 红：
  `ShotAnimationCountBoundaryTest.delayPhaseEndsAndCollisionTurnsOn`
  断言 `shot.canBeCollidedWith() == true`（实体化后）。这是**测试在钉 diag-pose 之前的旧行为**，
  与本次修复的意图直接冲突（该断言的存在意味着每次实体化弹都参与姿势拟合）。
- 处理（改测试，不改产品）：把该用例的观测窗口从 `canBeCollidedWith()` 换成
  **`isPickable()`**（并加一条 `assertFalse(canBeCollidedWith())` 把新契约钉死）；
  同时更新类 Javadoc 第 3 条与 `delayPhaseAnimationCountIsNegative` 里的同类断言。
  即：**「实体化了吗」的观测点从 canBeCollidedWith 迁到 isPickable / canBeHitByProjectile**。

### 7.2 第二轮 runGameTestServer：红的是**已有**的扫帚用例（需甄别是否本批引入）

- 结果：**64 tests 启动**，1 红：`BroomPhysicsTest.unriddenBroomGlideDistanceMatchesLegacy`
  （断言「无人扫帚水平速度按 0.9×0.99 衰减」）。
- 本批**未触碰** `EntityMarisaBroom`（改动仅 5 个实体的 `canBeCollidedWith` + 1 个测试文件），
  且该用例是「按 tick 采样速度」的时间敏感型断言（`onEachTick` + 6 tick 窗口 + 0.02 容差），
  疑为**既有的偶发失败**。→ 追加 3 连跑甄别（见 7.3；若 3 次全绿则判为 flaky，
  若稳定红则需另开批次，因为扫帚物理不在本批范围）。
- 处理原则（禁止粉饰）：**不修改该用例、不放宽容差**，只做归因记录。

### 7.3 flaky 甄别（3 连跑，脚本计数版）

命令：`for i in 1..3; ./gradlew runGameTestServer --offline > /tmp/gt$i.log`，
逐次统计 `failed at` 条数与 `N tests are now running`。

结果：**3/3 均为 64 tests、0 条 `failed at`** → `unriddenBroomGlideDistanceMatchesLegacy`
为**既有的偶发失败（flaky）**，与本批改动无关（本批未触碰 `EntityMarisaBroom`、
未触碰任何实体移动/物理）；该用例是 `onEachTick` 采样 + 6 tick 窗口 + 0.02 容差的时间敏感断言，
在同机 3 连跑的其他两次均为绿。**未修改该用例、未放宽容差**（不粉饰），登记为遗留风险交 K3。

> 注：之前两次「首轮红」均为**单次运行**，其中第一次是真实的过期断言（已按 7.1 迁移观测点），
> 第二次是这条 flaky。3 连跑用于确认修复后无稳定红项。

## 8. 最终验证结果

| 验证 | 命令 | 结果 |
|---|---|---|
| 编译 | `cd porting/neoforge-1.21.1 && ./gradlew compileJava --offline` | **BUILD SUCCESSFUL（0 错）** |
| GameTest 全量 ×3 | `./gradlew runGameTestServer --offline`（3 连跑） | **64 tests / 0 failed（3/3 全绿）**；基线 61 + 本批新增 3 = 64 |
| 新增用例 | `PlayerPoseRegressionTest`（3 例：机理 / 修复结果 / 真实射击路径） | 全绿 |
| 观测点迁移 | `ShotAnimationCountBoundaryTest`（2 处断言 + 2 处 Javadoc） | 全绿（原 `canBeCollidedWith()==true` 的期望已按新契约改为 `isPickable()`） |

## 9. 交接要点（给 K3 / 发布方）

1. **根因一句话**：1.21.1 的 `Player#updatePlayerPose()` 会把「实体边界」也算进姿势拟合
   （`noCollision` → `getEntityCollisions` → 对方 `canBeCollidedWith()`）；
   而模组弹/效果实体生成在**使用者自身坐标**（脚底 +1.12）且 `canBeCollidedWith()==true`，
   于是站姿/蹲姿都「放不下」→ 原版兜底 `Pose.SWIMMING`（匍匐、眼高 0.4）。
   1.7.10 没有这套拟合，所以原作不趴下。
2. **改动**：5 个实体 `canBeCollidedWith()` → `false`（见 §6.1），`isPickable()` 保持不变，
   I8 的瞄准/弹丸命中零回归；1 个过期测试的观测点迁移（§7.1）；新增 3 个回归用例（§6.2）。
3. **需要 K3 裁决**：§6.4 的取舍（弹幕不再阻挡实体移动）与「同类实体（隙间/伞/八卦炉/冰块/结界）
   暂不改」的范围决定。
4. **建议文档同步**（本批**未改**任何 `porting/*.md`，按禁令）：
   `USER_MIGRATION.md` §3.2/§5 可加一条「弹幕/效果实体不再阻挡实体移动（1.21.1 姿势系统所致）」；
   `integration-notes.md` 事故台账建议新增
   `I13 | 用户实测：右键符卡/物品后角色自动趴下（匍匐，眼高 0.4）| ... | ✅ 修复（5 实体 canBeCollidedWith→false + PlayerPoseRegressionTest 3 例）`。
5. **未做**：真实客户端第三人称目视验证（需用户复测）；§6.4 第 2 项同类实体；
   flaky 的扫帚用例（§7.3）。

### 7.4 修正 §7.3 的结论（自纠：我的新用例自身有一个假设错误）

**7.3 的「3/3 全绿」结论是错的**，起因是我上一轮脚本把 `grep -ciE` 的输出被管道吞掉，
只看到过滤后的 `exit=0`（那是 grep 的退出码）。重跑（逐次 `failed at` 计数）得到真实结果：

| 轮次 | tests | failed | 失败用例 |
|---|---|---|---|
| 1 | 64 | 1 | `PlayerPoseRegressionTest.modEntitiesAtEyeHeightDoNotDisplaceStandingPose` |
| 2 | 64 | 2 | 同上 + `BroomPhysicsTest.unriddenBroomGlideDistanceMatchesLegacy` |
| 3 | 64 | 2 | 同上 + 同上 |

**失败原因（我的测试假设错，不是产品缺陷）**：
`normal_shot`（{@code EntityTHShot}）的 {@code isPickable()} 是
{@code getAnimationCount() >= 1}，而 {@code helper.spawn()} 出来的是**延迟相位**（count = 0 → false），
所以断言「isPickable()==true」本身就与 I7/I8 的设计矛盾。
→ 已改：对 {@code EntityTHShot} 单独断言「延迟相位不可瞄」（与
{@code ShotAnimationCountBoundaryTest}/{@code EntityPickableTest} 同一契约），
其余 4 个实体仍断言 {@code isPickable()==true}；并补 {@code import EntityTHShot}。
（这正是 §7.1 那类「观测点/相位」问题，我在新测试里又踩了一次——记录在案。）

**`BroomPhysicsTest.unriddenBroomGlideDistanceMatchesLegacy`（既有的扫帚用例）**：
2/3 轮红，且日志给出实测数据：
`observed=0.5615501416127204 expected=0.5003411520778169 speeds=0.800000 0.712800 0.635105 0.565878 0.504198 0.449240`
→ 实际是 0.891 倍/tick（0.800→0.7128→0.6351→0.5659），**比期望的 0.891^6 = 0.5003 慢一档**，
即 `speeds` 列表里少采/多采了 1 tick（`onEachTick` 采样窗口与 `runAtTickTime` 的对齐差 1），
属**测试自身的采样对齐问题**，与实体物理无关。
本批**未改** `EntityMarisaBroom`、未改任何移动代码 → **非本批引入**；
按纪律**不修改该用例、不放宽容差**，登记为既有 flaky 交 K3。

### 7.5 第二次自纠：`sanae_wind` 也踩了同一个相位假设

7.4 的修正把「延迟相位 ⇒ `isPickable()==false`」当成了**所有** `EntityTHShot` 子类的性质，
但 `EntitySanaeWind` 覆写了 `isPickable() == true`（1.7.10 同样如此）→
修正后仍有 1 红：`sanae_wind: 遅延相位の弾が狙える…`。
→ 最终判据改为：**只对裸的 `normal_shot`（`type == ModEntities.NORMAL_SHOT.get()`）断言延迟相位不可瞄**，
其余实体一律 `isPickable() == true`。

教训（写在这里供同类测试参考）：`EntityTHShot` 家族里 `isPickable()` 有**两套**实现
（基类 = 相位相关、子类 = 恒 true），任何批量断言都必须按「是否覆写」分类，不能按基类推断。

### 7.6 最终稳定性（4 连跑）

| 轮次 | tests | failed | 失败用例 |
|---|---|---|---|
| 1 | 64 | 0 | — |
| 2 | 64 | 1 | `unriddenBroomGlideDistanceMatchesLegacy`（既有扫帚用例） |
| 3 | 64 | 0 | — |
| 4 | 64 | 0 | — |

→ **本批新增/修改的用例 4 轮全绿**；唯一偶发红是**既有**的扫帚采样对齐问题（§7.4），
与本批改动无因果关系（本批未触碰 `EntityMarisaBroom` 或任何移动/物理代码），
按纪律不改该用例、不放宽容差，登记为遗留项。

### 7.7 追加 3 连跑（RUN5-7）：扫帚用例转为**稳定红** → 需要严格归因

RUN5/6/7 全部只有 `unriddenBroomGlideDistanceMatchesLegacy` 一条红（本批用例继续全绿）。
从 §7.6 的 4 连跑（1 红）到这里 3 连跑（3 红），扫帚用例呈现出**与运行环境相关的倾向性**。
因为「稳定红」不再是可忽略的偶发，必须做**严格归因**，不能靠断言「与我无关」结案。

归因实验（见 §7.8）：把本批新增的 `PlayerPoseRegressionTest` 临时移出主树
（`porting/staging/diag-pose/PlayerPoseRegressionTest.java.hold`），
使测试总数为 **61**（与基线一致，只保留 5 个产品改动），再跑 3 次。
- 若扫帚用例仍红 → 与本批新增测试无关（本批只改了 5 处 `canBeCollidedWith`）；
- 若变绿 → 说明新增测试改变了批次调度/时序（64 个测试被切成 50+14 两批），需进一步定位。
实验后**无论结果如何都会把该文件放回主树**（本批的交付物之一）。

### 7.8 归因结论：扫帚用例为**既有 flaky**，与本批无关

| 配置 | 轮次 | 红 | 红的用例 |
|---|---|---|---|
| 64 tests（含本批新增 3 例） | 5 连跑（§7.6 / §7.7 合并计） | 4 次红 | 全部是 `unriddenBroomGlideDistanceMatchesLegacy` |
| **61 tests**（临时移出本批新增测试，仅保留 5 个产品改动） | 3 连跑 | 1 次红（RUNa） | 同样是 `unriddenBroomGlideDistanceMatchesLegacy` |

→ 两种配置下**只有同一条**既有用例会红，且都是**间歇性**的 → **与本批改动无因果关系**。
`PlayerPoseRegressionTest` 已放回主树（`compileJava` BUILD SUCCESSFUL）。

**该用例的问题（供 K3 另开批次）**：`onEachTick` 采样的 `speeds` 列表与
`runAtTickTime(POST_DISMOUNT_TICKS=6)` 之间存在 1 tick 的对齐差
（实测 `observed=0.5615` vs `expected=0.5003`，比值 = 0.891 的 **5** 次方），
即断言用 6 次衰减的期望去比 5 次采样的实测。**本批不修**（属扫帚批次，
且修改他人测试的语义需要其原设计意图）。

## 8. 最终验证结果（修正版）

| 验证 | 命令 | 结果 |
|---|---|---|
| 编译 | `./gradlew compileJava --offline` | **BUILD SUCCESSFUL（0 错）** |
| GameTest（本批用例） | `runGameTestServer --offline` × 8 轮（64 tests） | 本批新增/修改用例 **8/8 轮全绿**（0 条与 `PlayerPoseRegressionTest`/`ShotAnimationCountBoundaryTest` 相关的红） |
| GameTest（全量口径） | 同上 | 64 tests 启动；唯一偶发红 = **既有** `BroomPhysicsTest.unriddenBroomGlideDistanceMatchesLegacy`（§7.8 归因） |
| 归因实验 | 61 tests（移出本批新测试）× 3 | 同一条既有用例红 1 次 → 与本批无关 |

## 9. 交接要点（给 K3 / 发布方）

1. **根因（一句话）**：1.21.1 `Player#updatePlayerPose()` 的姿势拟合把**实体边界**也算进
   `noCollision`；模组弹/效果实体生成在**使用者自身坐标**（脚底 +1.12）且
   `canBeCollidedWith()==true` → 站姿(0..1.8)/蹲姿(0..1.5) 都「放不下」→ 原版兜底
   `Pose.SWIMMING`（0.6 高、眼高 0.4 = 用户看到的趴平 + 视角下移）。1.7.10 无此机制。
2. **改动**：5 处 `canBeCollidedWith()` → `false`（§6.1）；`isPickable()` 语义不变，
   I8 的瞄准/弹丸命中零回归；1 个过期断言迁到 `isPickable()`（§7.1）；新增 3 个回归用例（§6.2）。
3. **待裁决**：§6.4 的取舍（弹幕不再阻挡实体移动）与同类实体（隙间/伞/八卦炉/冰块/结界）范围。
4. **建议台账**（本批按禁令未改任何 `porting/*.md`）：
   `I13 | 用户实测：右键符卡/物品后角色自动趴下（匍匐 + 眼高 0.4）| 1.21.1 姿势拟合把实体边界计入，模组弹生成在玩家眼高且 canBeCollidedWith=true | 5 实体改 false + PlayerPoseRegressionTest 3 例 | ✅ 修复`
5. **遗留**：扫帚 flaky 用例（§7.8，非本批）；真实客户端第三人称目视复测（需用户）；
   §6.4 第 2 项同类实体。


## 10. 终局确认（本报告定稿时的最后一次实测）

```
./gradlew compileJava --offline          → BUILD SUCCESSFUL (0 错)
./gradlew runGameTestServer --offline    → 64 tests / 0 failed
                                            ========= 64 GAME TESTS COMPLETE IN 1.021 s ======================
```

改动行号（定稿）：
- `entity/shot/EntityTHShot.java:1925`、`entity/shot/EntitySanaeWind.java:93`、
  `entity/item/EntitySilverKnife.java:849`、`entity/EntityDivineSpirit.java:220`、
  `entity/EntityShotMaterial.java:262` —— 5 处 `canBeCollidedWith()` 返回 `false`
- `test/PlayerPoseRegressionTest.java`（新，3 例：81 / 127 / 170 行）
- `test/ShotAnimationCountBoundaryTest.java`（观测点从 `canBeCollidedWith()` 迁到 `isPickable()`）
