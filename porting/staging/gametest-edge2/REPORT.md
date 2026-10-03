# gametest-edge2 批次报告（I8 / I9 回归族 · 第二批）

范围：为刚修复的 I8（`canBeCollidedWith` 三职拆分未跟随）与 I9（扫帚无人减速写回）补 GameTest。
只写 `porting/staging/gametest-edge2/`，主树（含 `test/DanmakuDamageTest.java`）**未改一字**。
包名 = 最终包名 `thkaguyamod.test`，目录 `thkaguyamod/test/`（合并即拷贝）。

约定沿用既有测试：`@GameTestHolder(THKaguyaMod.MOD_ID)` + `@PrefixGameTestTemplate(false)` +
`@GameTest(template = "empty")`（模板 `thkaguyamod:empty`）。`DanmakuDamageTest` 的
「用户弹直射 NoAi 僵尸 + 固定 tick 断言」结构被复用为基线。

---

## 1. 变更清单（新增，无修改）

| 文件（`porting/staging/gametest-edge2/thkaguyamod/test/`） | 行数 | 一句话 |
|---|---|---|
| `DanmakuDamagePathTest.java` | 181 | 3 test：EntityTHLaser 直射僵尸掉血 / EntitySilverKnife 掷出命中掉血（`ticksInAir>=5` 门）/ 不同 user 的 THShot 对撞相殺（0.1 系数，值 9.4 与 5.0）|
| `EntityPickableTest.java` | 146 | 3 test：12 个补 isPickable 的实体逐一 spawn 断言 `isPickable()` 与 `canBeHitByProjectile()` / 丢弃后回到 false / THShot 延迟期 false、实体化后 true（含 count=0 边界）|
| `BroomPhysicsTest.java` | 134 | 2 test：载人后下车 → 速度模长 6 tick 内降到 0.8 倍以下（下骑不停回归）/ 石上无人扫帚 30 tick 不沉（`motionY += 0.06` 补偿）|

合计 **8 个 `@GameTest`**（全部默认 `required`）。三个类名与主树现有测试类不重名。

## 2. 断言要点（含为稳定性做的几何/时序设计）

### DanmakuDamagePathTest

- **laserDamagesMob**：`LaserData.laser(LASER_NORMAL, RED, 0.3F(宽), 3.0F(长), 4.0F(伤害), 0, 200)`，
  速度 0.5、从僵尸侧面 2.5 格直射。40 tick 后断言僵尸掉血/死亡。**激光走的是
  `EntityTHLaser#hitCheck` 独立扫描代码**（不是 THShot 的），是 `canBeHitByProjectile` 修复的 7 处之一。
- **silverKnifeDamagesMob**：直接用 `new EntitySilverKnife(type, level, user, pos, angle, 0.5D, KNIFE_COLOR_BLUE)`
  （= `ItemSilverKnife#use` 的生成路径）并 `addFreshEntity`。银刀有
  「`getKnifeCount() > 2` 才前进（≈4 tick）」+「`ticksInAir >= 5`」两道门，实测命中发生在第 5 tick 之后，
  故判定放在 40 tick；测试注释里写明了这一点。
- **shotsFromDifferentUsersCancelOut**：`EntityTHShot#isPickable()` 是 `getAnimationCount() >= 1`，
  实体化即刻（tick 1）计数为 0 所以**两弹在 tick 1 互相看不见**，首次命中只能发生在 tick 2。
  于是把两弹初始间距设为 4.3 格、双方速度 2.0 格/tick：
  tick 2 开始时相距 0.3 格（进入彼此扫描箱），tick 3 时已相距 1.7 格（脱离 1.4 格扫描箱），
  **无论哪一发先 tick，相殺都恰好发生一次**，数值因而确定：
  A = 10 − 6×0.1 = **9.4**、B = 6 − 10×0.1 = **5.0**（1.7.10 的 Player 使用者系数 0.1）。
  另有「两者相殺后仍为正」的副断言，防把系数写成全额相减。

### EntityPickableTest

- 12 实体（扫帚 / 隙间 / 迷你八卦炉 / 银刀 / 小神灵 / 子弹之素 / 金阁寺 / 冰棺 / 白霊盾 / 白狼反射 / 折伞 / SanaeWind）
  用 `helper.spawn(type, CENTER)` 逐一生成，检查 `isPickable()` 与 `canBeHitByProjectile()`
  （后者默认实现是 `isAlive() && isPickable()`，即 isPickable 漏掉的**实际后果**）。
  失败项聚合成一条断言消息，便于定位漏掉的实体。
- 扫帚 `discard()` 后 `isPickable()` 回到 false（该实现是 `!isRemoved()`，不是恒 true）。
- THShot：延迟 10 tick 弹生成即 `-10` → `isPickable()==false`；`setAnimationCount(1)` → true；
  **边界 `0` 仍为 false**（`>= 1` 条件）；再设 `-80` 回 false。

### BroomPhysicsTest

- **unriddenBroomDeceleratesAfterDismount**：`Zombie`（NoAi、NoGravity）用
  `rider.startRiding(broom, true)` 上扫帚（`Entity` 默认 `canAddPassenger` 允许空载乘客，force 再兜底），
  断言确实载上了；`stopRiding()` 后给 `setDeltaMovement(0.3, 0, 0)`，6 tick 后断言
  速度模长 < 初始值与 < 0.8×初始值、水平分量 < 0.8×初始值（0.9⁶ ≈ 0.53，余量充足）。
- **groundedBroomDoesNotSink**：在 (3,1,3) 放石（顶面 y=2.0），扫帚在 y=2.4 生成（避免包围盒与石重叠），
  30 tick 后断言：仍在、y > 1.5（不沉）、`deltaMovement.y > -0.15`（下向移动量未堆积）、y < 3.5（未持续上浮）。
  无人分支每 tick `motionY -= 0.03`，着地时 `+0.06` — 补偿移除后该值会持续向负累积。

## 3. 验证方式与结果

沿用上一批 REPORT §2.2 的等价 javac 沙盒（`compile_staging.sh` 已确认对 overlay staging 不适用）：

```bash
# 主树 260 + 本批 3 = 263 个 java 文件，一次全量编译
javac -nowarn -proc:none -encoding UTF-8 \
  -cp "<neoforge-21.1.252-merged.jar>;<clientLegacyClasspath 经 cygpath -w 拼接>" \
  -d porting/.edge2-build/out @files.txt      # → 0 error（仅既有类 deprecation/unchecked 提示）
```

- **全量结果：0 error**；三个类的 `.class` 均正常产出
  （`DanmakuDamagePathTest.class` / `EntityPickableTest.class` / `BroomPhysicsTest.class`）。
- 三个类另用 `-Xlint:deprecation,unchecked` 单独编译：**0 警告 0 错误**。
- 临时目录 `porting/.edge2-build/` 用完即删（已删除）；未运行 gradlew、未 git 操作。

**运行时 GameTest 未执行**（铁律禁止 gradlew）。合并后应在主树跑
`./gradlew runGameTestServer`，期望总数 **58**（主树现有 50 + 本批 8）。

## 4. 假定 API 清单（均经 javap / 主树源码 + 沙盒编译核对）

| 符号 | 用途 |
|---|---|
| `THShotLib.createLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double speed, LaserData)` | 生成真实激光实体 |
| `LaserData.laser(int form,int color,float width,float length,float damage,int delay,int end)` | 激光参数 |
| `new EntitySilverKnife(EntityType,Level,LivingEntity,Vec3,Vec3,double,int)` + `KNIFE_COLOR_BLUE` | 掷出银刀 |
| `EntityTHShot#shotDamage`（public）、`THShotLib.createShot` | 弹对弹相殺 |
| `ModEntities.{MARISA_BROOM,SUKIMA,MINI_HAKKERO,SILVER_KNIFE,DIVINE_SPIRIT,SHOT_MATERIAL_ENTITY,KINKAKUZI,CIRNO_ICE_BOX,HAKUREI_SHIELD,HAKUROU_REFLECTER,YUUKA_PARASOL,SANAE_WIND}` | 12 实体 spawn |
| `Entity#isPickable()` / `canBeHitByProjectile()`（默认 `isAlive() && isPickable()`） | I8 断言 |
| `EntityType.getKey(EntityType<?>)` | 失败消息里的实体 id |
| `EntityMarisaBroom#setDeltaMovement/getDeltaMovement/getFirstPassenger`、`Entity#startRiding(Entity,boolean)/stopRiding()` | I9 断言 |
| `GameTestHelper#spawn/setBlock/absoluteVec/makeMockPlayer/runAtTickTime/assertTrue/assertFalse/assertValueEqual/succeed` | 脚手架 |

## 5. TODO / 降级点 / 未决风险

1. **相殺用例的时序是「推导 + 余量」，不是实测**：依靠「tick1 计数 0 → 互不可见、tick2 才可见」
   与「1 tick 内 1.7 格位移脱离 1.4 格扫描箱」两条推理。若某次运行出现「命中两次」或「一次都没有」，
   数值断言会直接报出实际值，便于判断是否需要把两弹初始间距再拉开。
2. **银刀的命中 tick 未精确固定**（只在 40 tick 后断言）：银刀的 `knifeCount`/`ticksInAir` 门
   使命中时刻依赖前几 tick 的状态，写死 tick 反而脆。若要更强断言，可加
   `helper.onEachTick` 记录首次掉血 tick。
3. **扫帚「无人减速」测的是速度模长与水平分量**，未区分「0.9 阻尼」与「其他减速」来源；
   `motionY += 0.06` 的补偿只在着地用例里以「不沉 + 下向分量不堆积」间接验证
   （未直接测「恰好 +0.06」，因为着地判定 `onGround()` 的时序受 `move()` 影响，直测会脆）。
4. **12 实体是否就是全部**：本批按简报给的 12 个名字（与主树 grep `isPickable` 的 13 处吻合，
   第 13 处是 THShot 本身，单独测）。若后续还有实体补 `isPickable`，`types` 列表需同步。
5. 未覆盖：`EntityTHSetLaser` / `EntityNuclearShot` / `EntityTHHenyoriLaser` / `EntityDanmakuMob`
   这 4 处命中扫描（同为 I8 修复点）。它们的命中几何更复杂（多段激光 / 核弹 / 海nyori / 怪物 AI），
   需要各自设计场景；建议作为第三批或合并进现有弹幕测试。
6. 上一批报告提到的 `tools/staging_blocked.txt` / `items3` 重复问题仍未处理，
   `compile_staging.sh` 对 `gametest-edge2` 同样返回假失败（本批因此仍用 §3 的沙盒法）。