# FIX-3a 批报告（实体侧 P0 ×2 + P46 准星半径）

> 批次：FIX-3a（直接改主树，有 gradlew 授权）。范围：`entity/living/` 的 3 处修复 + `entity/shot/EntitySanaeWind` + `test/`。
> 证据来源：`porting/staging/audit-ai/REPORT.md`（P0-2 / P0-4）。
> 纪律：禁 git、禁改 `porting/*.md`、不杀非 SmokeRunVmArgs 进程、逐文件落盘。
> 基线：GameTest 69/69、smoke 双 PASS。

## 改动清单

（逐项追加）

## 验证

（追加）

## 改动清单（全部已落盘）

### 1) P0-2：`EntityDanmakuCreeper` 冷却窗内被击杀 → 实体永不移除

| 文件:行 | 改动 |
|---|---|
| `entity/living/EntityDanmakuCreeper.java:36-52` | 新增只读访问器 `getIgnitionCooldownTicks()`（返回原 `timeSinceIgnited2`），供回归测试观察冷却窗（字段本身仍是 private，1.7.10 同） |
| `entity/living/EntityDanmakuCreeper.java:146-158` | **核心修复**：在冷却窗判定之前插入「死亡优先」短路 —— `if(isDeadOrDying()) { super.tick(); return; }` |

**原作对照（1.7.10 `src/main/java/thKaguyaMod/entity/living/EntityDanmakuCreeper.java:148-166`）**

```java
if(timeSinceIgnited2 >= -100 && timeSinceIgnited2 < 0) { timeSinceIgnited2 = 0; }
if(timeSinceIgnited2 < -100 && timeSinceIgnited2 > -280) { if(this.getHealth() > 0F) { this.onDeathUpdate(); } }
if(timeSinceIgnited2 < 20 && timeSinceIgnited2 >= 0) { super.onUpdate(); }
```

- 该结构是**同源 bug**（不是移植转录错误）：冷却窗内被打死时，`getHealth() <= 0` 使手动死亡更新被跳过、
  `t < 20` 不成立使 `super.onUpdate()` 被跳过；同时 `isAlive() == false` 使 `t += swellDir` 停止，
  冷却窗永远走不出去 → 原版死亡推进（`LivingEntity.baseTick()` 的 `isDeadOrDying() → tickDeath()`）
  一次都不跑 → **实体永不 `remove()`**，成为每次区块加载都 tick 的残留实体。
- 修复方式（按迁移规则「旧版 bug 修复并注释，不得照搬」）：**只加一条死亡优先短路**，
  活体时的冷却窗行为（手动 `tickDeath()`、不调 `super`）与 1.7.10 完全一致。
  `super.tick()` 会走 `LivingEntity.tick()` → `baseTick()` → `tickDeath()`，
  而本类 `tickDeath()`（`:53-89`）在 `deathTime == 40` 时 `discard()` → 链路必达。
- 副作用评估：`isDeadOrDying()` 时提前 `return`，会跳过本类后续的「朝向目标/攻击计数」逻辑——
  那些逻辑对尸体本来也无意义；且死亡后 40 tick 内必然 `discard()`，不会长期改变世界状态。

### 2) P0-4：样式表空窗守卫（I11 漏网）

| 文件:行 | 改动 |
|---|---|
| `entity/living/EntityFamiliar.java:28-33` | `random.nextInt(pattern.size())` → `random.nextInt(Math.max(pattern.size(), 1))`（对齐 `EntityTHFairy:45` 已有写法）+ 注释 |
| `entity/living/EntityTHPhantom.java:32-37` | 同上 |
| `entity/living/EntityTHFairy.java:94-116` | **`setPattern()` 本体加固**（三处共用的唯一实现）：空表直接 `return`；`Mth.clamp` 夹取索引；`form/danmaku/span/speed` 改用 `getOrDefault` 兜底（0 / 0 / 30 / 0.2F） |

**原作对照**：1.7.10 的 `DanmakuPatternRegistry.setDanmakuPattern()` 只在 `THKaguyaCore.preInit`
调用一次，表恒非空；移植版由 `ModConfigEvent.Loading/Reloading` 驱动，而该方法开头就
`pattern.clear()`（`DanmakuPatternRegistry.java:36`）→ **存在理论空窗**（config 重载期间构造实体）。
另外 1.7.10 也没有 `getOrDefault` 语义（表恒满），这里只是把「表未就绪」从异常降级为默认值。

> 三处调用点中，`EntityTHFairy` 原本已有 `Math.max` 加固但**只防住了 `nextInt` 的 bound**，
> 没防住 `pattern.get(0)` 的越界 → 本次在 `setPattern()` 本体统一收口，三处行为一致。

### 3) P46：准星半径（`getCollisionBorderSize` → `getPickRadius`）

**全树对照结果**（1.7.10 有覆写、移植版是否带）：

| 实体 | 1.7.10 值 | 移植版改动前 | 本次 |
|---|---|---|---|
| `entity/shot/EntitySanaeWind.java:98-118` | `1.0F`（`EntitySanaeWind.java:338-341`） | 缺 | ✅ 补 `getPickRadius() = 1.0F` |
| `entity/shot/EntityTHShot.java:1944-1961` | `getShotSize()`（`EntityTHShot.java:2010-2014`） | 缺 | ✅ 补 `getPickRadius() = getShotSize()` |
| `entity/EntityDivineSpirit.java` | `0.1F`（`:258-262`） | 缺 | ✅ 补 `0.1F` |
| `entity/EntityShotMaterial.java` | `0.1F`（`:312-316`） | 缺 | ✅ 补 `0.1F` |
| `entity/item/EntitySilverKnife.java:862-867` | `0.01F`（`:838-842`） | **已有** | 保持 |

**语义对照**：1.7.10 的 `getCollisionBorderSize()` 同时承担「准星加粗」与「弹丸命中膨胀」；
1.21.1 拆为 `getPickRadius()`（仅准星，`Entity.java:2231` 默认 `0.0F`，由
`ProjectileUtil.getEntityHitResult` 用于 `getBoundingBox().inflate(getPickRadius())`）与
`isPickable()`/`canBeHitByProjectile()`（命中资格）。diag-pose 已把命中侧迁到
`canBeHitByProjectile`，故本次只补准星半径，不改变命中判定。
- `EntityTHShot.getPickRadius()` 返回**同步槽**里的 `getShotSize()`，客户端可见，无需额外网络字段；
  未使用 `shotSpecial` 等仅服务端字段（该风险已规避）。
- 未改 `canBeCollidedWith()`（diag-pose 已固定 false），故**不会重新引入玩家自动匍匐**。

## 新增测试

`test/DanmakuMobP0RegressionTest.java`（新文件，2 例）：

1. `creeperKilledDuringCooldownIsRemoved`（`timeoutTicks = 600`）：spawn → `onEachTick` 等
   `t ∈ (-280,-100)`（起爆后约 20 tick 进入）→ `kill()` → 220+60 tick 内断言 `isRemoved()`。
   时间割依据：`t` 每 tick +1（原版 `getSwellDir()` 为 0），`t=30` 起爆后跳到 `-300`。
2. `entityConstructionWithEmptyPatternTableDoesNotThrow`：`pattern.clear()` 制造空窗 →
   走 `EntityType#create` 真实构造路径生成 使い魔 / 幽霊 / 妖精 → 断言不抛异常且
   样式回退为 0 → `finally` 中 `setDanmakuPattern()` 复原并断言表大小恢复。

## 验证

（追加）

### 测试迭代记录（诚实记录三次失败，避免后人重踩）

| 轮次 | 结果 | 原因 | 处置 |
|---|---|---|---|
| 1 | `creeperKilledDuringCooldownIsRemoved` 红：`isAlive=true health=20 cooldown=0 deathTime=0` | 忘记 GameTestServer 默认难度 PEACEFUL，`EntityDanmakuMob.tick()` 会 `discard()`；且未点火 → 起爆计数器根本不推进 | 加 `setDifficulty(NORMAL, true)` + `ignite()` |
| 2 | 红：`difficulty=NORMAL everIgnited=false minSeen=0 removedAtTick=28` | **该测试是伪阳性**：实体在 28 tick 被 PEACEFUL/其他路径移除（`removed=true`）→ 断言 `isRemoved()` 恒真 | 加「冷却窗确实进入过」的前置断言（`killedInWindow`），把伪阳性变成显式失败 |
| 3 | 红：`minSeen=-599`（单调下降、永不回升） | `t += getSwellDir()`：未点火时 `swellDir = -1` → t 从 -300 一路 -∞ | 加 `setSwellDir(1)` 复现原作「点火中 +1/tick」 |
| 4 | **绿** | 状态注入 `swellDir=1` + `t=-300` 后，约 20 tick 进入冷却窗 → `kill()` → 移除 | 保留 |
| 变异验证 | 把 `isDeadOrDying()` 短路改成 `false &&` → **该用例转红**（`… が remove() されていない（P0-2 の再発）`），恢复后转绿 | 证明用例真的在测修复，不是恒绿 | ✅ 通过 |

> 因此测试中新增了两个**仅供测试**的访问器（`getIgnitionCooldownTicks` / `setIgnitionCooldownTicks`），
> 理由写在方法 Javadoc 里：实机走完「点火 → 起爆 → 冷却窗」需要 50 tick 且原版 `swell` 会同时推进
> 导致实体爆散，直接注入状态更确定。

### 变异验证（红证）—— 两次都成功

| 变异 | 结果 |
|---|---|
| 把 P0-2 的 `isDeadOrDying()` 短路改成 `false && this.isDeadOrDying()`（= 还原 bug） | `creeperKilledDuringCooldownIsRemoved` **转红**：`起爆冷却窓の間に倒した花田尖叫者 が remove() されていない（P0-2 の再発）` |
| 把 `EntityFamiliar` 的 `Math.max(...,1)` 还原成裸的 `nextInt(pattern.size())` | `entityConstructionWithEmptyPatternTableDoesNotThrow` **转红**：`Bound must be positive` |

两次变异后均已**从备份恢复**（`grep -c` 复核为 0 处残留），随后重新 `compileJava` + `runGameTestServer` 全绿。

### 验证结果（最终）

| 验证 | 命令 | 结果 |
|---|---|---|
| 编译 | `cd porting/neoforge-1.21.1 && ./gradlew compileJava --offline` | **BUILD SUCCESSFUL（0 错）** |
| GameTest ×3 | `./gradlew runGameTestServer --offline`（3 连跑） | **71 tests / 0 failed（3/3 全绿）**；基线 69 + 本批新增 2 = 71 |
| 变异验证 ×2 | 见上表 | 两条用例分别**转红**，恢复后转绿 → 用例有效 |
| 遗留文件检查 | 备份文件仅在 `/tmp`；主树 `grep` 无 `false &&` / 无裸 `nextInt(pattern.size())` 残留 | ✅ |

### 改动文件清单（主树）

| 文件 | 类型 |
|---|---|
| `entity/living/EntityDanmakuCreeper.java` | 修复（死亡优先短路 + 2 个测试用访问器） |
| `entity/living/EntityFamiliar.java` | 修复（空表守卫） |
| `entity/living/EntityTHPhantom.java` | 修复（空表守卫） |
| `entity/living/EntityTHFairy.java` | 加固（`setPattern()` 统一守卫 + `getOrDefault` 兜底） |
| `entity/shot/EntitySanaeWind.java` | 补 `getPickRadius() = 1.0F` |
| `entity/shot/EntityTHShot.java` | 补 `getPickRadius() = getShotSize()` |
| `entity/EntityDivineSpirit.java` | 补 `getPickRadius() = 0.1F` |
| `entity/EntityShotMaterial.java` | 补 `getPickRadius() = 0.1F` |
| `test/DanmakuMobP0RegressionTest.java` | 新增（2 例） |

### 遗留风险 / 给 K3

1. **P0-2 的修复边界**：只在 `isDeadOrDying()` 时让 `super.tick()` 通过；未改「活体时冷却窗不调
   `super.tick()`」这一原作行为（原版 `Creeper.tick()` 的 `swell/explodeCreeper()` 因此仍被禁用，
   与原作一致——原作 `createExplosion` 也是注释掉的）。
2. **测试用访问器**：`setIgnitionCooldownTicks` 是 public 且产品代码不调用；若 K3 不接受产品类里
   出现测试专用写入方法，可改为包内可见（`EntityDanmakuCreeper` 与测试不同包，需换 `protected` +
   测试子类，或把测试挪到 `entity.living` 包）。**这是本批唯一可争议的设计取舍。**
3. **P46 未覆盖项**：全树已比对「1.7.10 有 `getCollisionBorderSize` 覆写」的 5 个实体，
   均已补 `getPickRadius`；但**没有**系统检查「1.7.10 未覆写、移植版新增覆写」的反向差异。
4. **未做**：smoke 双模式（本批只跑 compile + GameTest，按简报要求）；真实客户端准星手感验证。
