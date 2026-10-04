# diag-fairy-fire — `/summon thkaguyamod:th_fairy` 弹幕卡在身体里 + 连发

基线：git ccd558b（GameTest 89/89、smoke PASS）。
现象：召出的妖精每 tick 连发，弹速 0，弹停在妖精胸口不动。

## 根因（已定位）

**`EntityDanmakuMob.readAdditionalSaveData` 用「缺键即 0」的读取覆盖了构造函数里由样式表算出的正确值。**

`/summon` 走的是 NBT 加载路径（`EntityType.loadEntityRecursive` → `Entity.load(tag)` → `readAdditionalSaveData(tag)`），
而该 tag 只含 `id`（+`Pos`），**不含** `Span`/`Speed`/`ShotForm`/`ShotColor` 等键。
`CompoundTag.getShort/getFloat` 对缺失键返回 **0**，于是：

| 字段 | 构造器（样式表"000"，danmakuLevel=2） | /summon 后实际值 | 后果 |
|---|---|---|---|
| `danmakuSpan` | 30 | **0** | `EntityTHFairy.tick()` 的 `attackCounter > danmakuSpan → 0` 每 tick 复位 → **每 tick 开火** |
| `speedRate` | 0.3F | **0.0F** | `createSphereShot(..., speed=0, ...)` → firstSpeed=limitSpeed=acceleration=0 → **弹停在生成点**（生成点=妖精 `pos()` → 胸口） |
| `shotForm`/`shotColor` | 依样式表 | **0 / 0** | 弹型与颜色被重置 |
| `detectionDistance`/`attackDistance` | 8 / 8 | **0 / 0** | 索敌范围被清零 |
| `danmakuPattern` | 依样式表 | **0** | d=0 → 走 `d<=999` 分支：way=1, num=1 |

d=0 + speedRate=0 ⇒ `createSphereShot(..., speed=0, ..., way=1, ...)` 每 tick 生成 1 发**零速**弹，
位置恒等于妖精坐标 ⇒ 截图里「发光弹卡在胸口 + 连发」。

**对照组（自然生成 / 刷怪蛋）**：走 `EntityType.create(level)` → 只跑构造函数 → **不调用** `readAdditionalSaveData`
→ `speedRate=0.3F`、`danmakuSpan=30` 正确。所以只有 `/summon`（及任何带部分 NBT 的加载）复现。

## 证据

### 1. NBT 读取把构造函数的值覆盖成 0（根因）

`porting/neoforge-1.21.1/src/main/java/thkaguyamod/entity/living/EntityDanmakuMob.java:1744-1785`
（修复前）：

```java
public void readAdditionalSaveData(CompoundTag nbtTagCompound) {
    super.readAdditionalSaveData(nbtTagCompound);
    if (nbtTagCompound != null && !nbtTagCompound.isEmpty()) {
        setForm(nbtTagCompound.getByte("Form"));                 // 缺键 → 0
        setDanmakuPattern(nbtTagCompound.getInt("Danmaku"));     // 缺键 → 0
        danmakuSpan = nbtTagCompound.getShort("Span");           // 缺键 → 0   ★
        shotForm  = nbtTagCompound.getByte("ShotForm");          // 缺键 → 0
        shotColor = nbtTagCompound.getByte("ShotColor");         // 缺键 → 0
        speedRate = nbtTagCompound.getFloat("Speed");            // 缺键 → 0.0 ★
        setAttackDistance(nbtTagCompound.getByte("AttackRange"));   // 缺键 → 0
        setDetectionDistance(nbtTagCompound.getByte("DetectRange"));// 缺键 → 0
        ...
    }
}
```

`/summon` 的 NBT 只有 `id`（+`Pos`），于是走进 `!isEmpty()` 分支但**每个键都缺**。
`CompoundTag.getShort/getFloat` 在缺键时返回 0（NeoForge 21.1.252 `CompoundTag.java:344-354`：
`if (this.contains(key, 99)) {...} return 0;`）。

### 2. 两个 0 分别造成「连发」和「卡住」

- `danmakuSpan = 0` → `EntityTHFairy.java:187-190`（tick）：
  ```java
  if (this.attackCounter > danmakuSpan) { attackCounter = 0; }
  ```
  `attackCounter` 每次开火后 `+1`（`EntityDanmakuMob.java:1091`），`> 0` 立刻为真 → 每 tick 归零 →
  `danmakuPattern()` 的 `if (attackCounter > 0) return;` 从不拦住 → **每 tick 开火**。
- `speedRate = 0.0F` → `EntityTHFairy.java:219-229`（d=0 分支）：
  ```java
  double speed = speedRate;            // 0.0
  double slowDownRate = speed / num;   // 0.0
  THShotLib.createSphereShot(getShooter(), pos(), look, speed, shotData, way, 0F);
  ```
  `createSphereShot(..., firstSpeed=0, limitSpeed=0, acceleration=0, ...)` →
  `EntityTHShot` 的 `motionX/Y/Z = angle * 0 = 0`，且 `shotAcceleration()` 两个分支都不成立
  （`0 > 0` 与 `0 < 0` 均假）→ **速度恒为 0，位置恒等于 `pos()`（妖精眼睛下方一点 = 胸口）**。
- 另外 `detectionDistance = 0` / `attackDistance = 0` 会让 `getNearestPlayer(this, 0)` 找不到人
  （自然索敌失效），`danmakuPattern = 0` 让样式固定成 "000"。

### 3. 键值对照（danmakuLevel=2 时，`DanmakuPatternRegistry` case 2）

| 键 | 样式表值（`DanmakuPatternRegistry.java:56-64`） | 构造函数读到 | /summon 后（修复前） | /summon 后（修复后） |
|---|---|---|---|---|
| `"000"` danmaku/span/speed/form | `0 / 30 / 0.3F / SMALL[BLUE]=1` | `danmakuSpan=30`, `speedRate=0.3F` | **0 / 0.0F** | 30 / 0.3F |
| 1.7.10 原作同值 | `CommonProxy.registerDanmakuPattern()` 内 `("000", 0, 30, 0.3F, ...)` | — | — | — |

对照 1.7.10 `EntityDanmakuMob.readEntityFromNBT`（`src/main/java/thKaguyaMod/entity/living/EntityDanmakuMob.java:1717-1759`）：
代码**逐字相同**，但 1.7.10 的 `readEntityFromNBT` 只在「从存档加载完整实体」时被调用，
`/summon`（`/spawnmob`）走的是另一条不读 NBT 的路径；1.21.1 把两者统一到 `Entity.load(tag)`，
于是缺键即 0 的行为被暴露出来。**因此这是移植引入的语义差，不是原作 bug。**

## 修复

**文件：`porting/neoforge-1.21.1/src/main/java/thkaguyamod/entity/living/EntityDanmakuMob.java:1744-1810`**

把 9 个 `getXxx("key")` 调用逐个包进 `nbtTagCompound.contains("key")` 判定：键存在才覆盖，
不存在则保留构造函数（样式表）设定的值。这样：
- `/summon`（部分 NBT）→ 保留样式表值；
- 存档加载（完整 NBT）→ 行为完全不变（既有键全部存在）。

`isFlyingMode` 的更新仍按原逻辑放在后面（它依赖 `getFlyingHeight()`，本身不受缺键影响）。

## 测试

新增 `porting/neoforge-1.21.1/src/main/java/thkaguyamod/test/FairySummonFireTest.java`（4 个用例）：

1. `summonLikePartialNbtKeepsPatternStats` —— 用「只有 id 的 tag」走 `Entity.load`，断言
   `danmakuSpan` / `speedRate` / `attackDistance` 仍等于构造函数从样式表读到的值；
   并断言**完整 NBT 往返仍生效**（防止修复把存档加载一起废掉）。
2. `summonedFairyShotsMove` —— 真实 spawn + 僵尸目标 + 真实 tick，断言发射的弹
   `getSpeed() > 0.05` 且相对生成点位移 > 0.25 格。
3. `summonedFairyFireIntervalMatchesSpan` —— 断言两次开火间隔落在 `[span/2, span*3]`。
4. `directDanmakuPatternProducesMovingShot` —— 绕过索敌直调 `danmakuPattern`，解耦定位。

（结果见下）

### 结果

`./gradlew compileJava --offline` → **0 错误**。

`./gradlew runGameTestServer --offline` → **All 95 required tests passed**（连续 3 次通过；本批新增 4 个用例，
基线 91 + 本批 4 = 95）。新增用例：

```
FairySummonFireTest.summonLikePartialNbtKeepsPatternStats   PASS
FairySummonFireTest.directDanmakuPatternProducesMovingShot  PASS
FairySummonFireTest.summonedFairyShotsMove                  PASS
FairySummonFireTest.summonedFairyFireIntervalMatchesSpan    PASS
```

**修复前反证（已实测）**：临时去掉 `EntityDanmakuMob.readAdditionalSaveData` 的 contains 守卫后重跑：

```
summonlikepartialnbtkeepspatternstats failed ...
  summon 后 danmakuSpan 被 NBT 缺键覆盖成 0（0 表示每 tick 连发）——readAdditionalSaveData 未做 contains 守卫
directdanmakupatternproducesmovingshot failed ...
  直调路径的弹速度为 0（卡在生成点）: 0.0（speedRate=0.0）
2 required tests failed :(
```

恢复守卫后连续 2 次 `All 95 required tests passed`。即：新用例能真实抓住本 bug，
不是「恒绿」的假阳性。

## 遗留 / 注意

1. **测试用例踩过的坑（已修，供后续参考）**：
   - GameTest 会给结构四周加屏障墙，目标放在 `FAIRY_POS + (0,0,7)`（墙外）会让
     `hasLineOfSight` 恒为 false → 妖精不开火。目标必须放在 7×7×7 内部（现用 2.5 格）。
   - `directDanmakuPatternProducesMovingShot` 必须显式设难度为 NORMAL：
     `GameTestServer` 的难度会被前序用例改成 PEACEFUL，此时 `EntityDanmakuMob.tick()`
     会 discard 妖精，其弹走 `shotFinishBonus` 消失 → 随机失败。
   - 7×7×7 结构太小，弹撞墙即 `discard`，观察窗口取 6 tick 即可。
2. `EntityTHFairy.setPattern` 的 `Mth.clamp` + `getOrDefault` 兜底（FIX-3a）保留，未改。
3. 本修复只覆盖 `EntityDanmakuMob` 的 NBT 读取。同一模式（缺键即 0 覆盖构造器默认值）
   在 `EntitySakuyaWatch`/`EntityHisou` 等自带 `readAdditionalSaveData` 的实体上未逐一排查，
   建议后续按同一手法扫一遍（`/summon` 这些实体是否有类似现象）。
4. 本次未改动任何 `porting/*.md`、未执行 git、未杀非本任务进程。
