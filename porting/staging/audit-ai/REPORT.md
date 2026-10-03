# audit-ai：living/ 严重性普查（P0/P1 专用）

> 批次：audit-ai。范围：`porting/neoforge-1.21.1/src/main/java/thkaguyamod/entity/living/`（16 生物类 + 基类）
> 只报 **P0**（崩溃 / 死循环 / 数据损坏 / 核心功能失效）与 **P1**（疑似，需 K3 判定）。
> 纪律：不改任何既有文件；禁 gradlew/git；不杀进程。逐文件读完即追加一行。
> 对照基线：`src/main/java/thKaguyaMod/entity/living/`（1.7.10）。
> 已知并排除（不重复报告）：P36 `EntitySpellCard.user` 客户端 null 潜伏。

## 覆盖清单（读完打勾）

| # | 文件 | 状态 |
|---|---|---|
| 0 | EntityDanmakuMob.java（基类） | ✅ 已读 |
| 1 | EntityTHFairy.java | ⏳ |
| 2 | EntitySunFlowerFairy.java | ⏳ |
| 3 | EntityTHPhantom.java | ⏳ |
| 4 | EntityFamiliar.java | ⏳ |
| 5 | EntityCirno.java | ✅ 已读 |
| 6 | EntityRumia.java | ⏳ |
| 7 | EntityToziko.java | ⏳ |
| 8 | EntityWriggle.java | ⏳ |
| 9 | EntityReimu.java | ⏳ |
| 10 | EntityMiko.java | ⏳ |
| 11 | EntitySanae.java | ⏳ |
| 12 | EntitySakuya.java | ⏳ |
| 13 | EntityRinnosuke.java | ⏳ |
| 14 | EntityCursedDecoyDoll.java | ⏳ |
| 15 | EntityDanmakuCreeper.java | ⏳ |
| 16 | （其他同目录文件） | ⏳ |

## 发现（按严重度）

### P0-1（核心功能失效的高嫌疑）：多阶段 BOSS 的「伪死亡」缺少恢复路径，死亡计时到点后可能被真正移除

**证据链（file:line）**

1. `EntityDanmakuMob.java:895-904` 拦截 `die()`：
   ```java
   public void die(DamageSource source) {
       if(level().isClientSide || danmakuDying || !canInterceptDeath()) { super.die(source); return; }
       danmakuDying = true;       // ← 只置标志，不做任何实体状态处理
   }
   ```
2. `EntityDanmakuMob.java:906-913` 在 `tick()` 开头手动驱动疑似死亡：
   ```java
   if(danmakuDying) { tickDeath(); }
   ```
3. `EntityDanmakuMob.java:1308-1319` 基类 `tickDeath()` **总是调用 `super.tickDeath()`**；
   而原版 `LivingEntity.tickDeath()`（1.21.1 源码）是：
   ```java
   protected void tickDeath() {
       this.deathTime++;
       if (this.deathTime >= 20 && !this.level().isClientSide() && !this.isRemoved()) {
           this.level().broadcastEntityEvent(this, (byte)60);
           this.remove(Entity.RemovalReason.KILLED);   // ★ 真删除
       }
   }
   ```
   即 **deathTime 达到 20 就无条件 `remove()`**。
4. 各阶段 BOSS 的 `tickDeath()` 用 switch 调 `moveDanmakuAttack(...)`，而 `moveDanmakuAttack()`
   （`EntityDanmakuMob.java:798-816`）只做：`setDanmakuPattern / attackCounter=-interval /
   deathTime = 0 / danmakuDying = false / setMaxHP+setHealth(maxHP) / danmakuRemove / circle.discard`。
   **它没有取消已经排队/可能已执行的 `remove()`**，也没有重置原版的 `dead` 标志。

**两个分支各自的后果**

- 若 `moveDanmakuAttack()` 在 `super.tickDeath()` **之前**执行（`EntityCirno.tickDeath()` 的每个 case 都
  `break;`，之后不再落到 `default: super.tickDeath()`）：
  `deathTime = 0` → 下一 tick 的 `super.tickDeath()` 里 `deathTime(0) >= 20` 为假 → 不会被删除，
  **看起来能自洽**；但基类 `EntityDanmakuMob.tickDeath()` 的 `super.tickDeath()` 只在
  「`deathTime == 7` 的爆炸分支之后」被调用，所以基类路径下 `deathTime` 会一直累加。
- 一旦 `deathTime` 累积到 20 且没有阶段转移（例如 `canInterceptDeath()` 变为 false 后、
  或 `switch` 落进 `default` 的正常死亡），原版 `remove(KILLED)` 会**跳过** `dropCustomDeathLoot`、
  `broadcastEntityEvent(3)` 等 `die()` 流程——因为我们的 `die()` 被拦截后从未调用 `super.die()`。
  → **战利品/符卡掉落（`dropCustomDeathLoot`，`:1324`）与死亡表现可能丢失**。

**为何列为 P0（核心功能失效）**：多阶段 BOSS 的伪死亡是移植的关键决策（台账已记录），
而「拦截 `die()` 之后靠 `tickDeath()` 自转」这条路径**没有任何测试覆盖**
（GameTest 61/64 里没有 BOSS 多阶段用例），且事件顺序依赖原版 `tickDeath()` 的 20 tick 硬编码。

**待 K3 判定/建议**：
- 明确 `tickDeath()` 在伪死亡期间**必须不执行 `super.tickDeath()`**（或把 `deathTime` 固定在 <20）；
- 或在 `moveDanmakuAttack()` 里补 `this.deathTime = 0;` 之外，额外记录「本 tick 是否已 remove」。
- 统计上：本轮「读通代码」即已看出该风险，但**未做运行时复现**（无 BOSS 生成脚本），置信度
  **中**（代码路径确定，实际触发概率取决于阶段转移是否总能先于第 20 tick 命中）。


### P1-1：`moveTimer <= 0 && move()` 的除零风险（`moveTimerMax == 0`）

- `EntityDanmakuMob.java:967`：`double moveRate = Math.cos((double)moveTimer / (double)moveTimerMax * Math.PI / 2.0D) * moveVecSpeed;`
  该行在 `if(moveTimer > 0)` 内，所以 `moveTimer != 0`；但 **`moveTimerMax` 可能是 0**：
  `setStopStart()`（`:705-710`）同时把 `moveTimer = 0; moveTimerMax = 0;`。
  一旦后续有代码把 `moveTimer` 单独设成正数而 `moveTimerMax` 保持 0（当前主树里 `move()` 总是同时设两者，
  故**当前不可达**），就是整数除零 → `Infinity` → `Math.cos(Infinity) = NaN` → `move(NaN)` → 位置变 NaN
  → 实体消失/存档损坏。
- 同类：`EntityDanmakuMob.java:1118/1123` 的 `toTargetDistance / getAttackDistance()` 与
  `(toTargetDistance / getAttackDistance())` 有 `getAttackDistance() > 0` 守卫（`:1111`），**安全**。
- 判定：**P1（潜伏，当前不可达）**。建议 `move()` 里加 `moveTimerMax = Math.max(1, time)` 作为不变量。

### P1-2：`lostTarget` 与远程 `getTarget()` 的交互

- `EntityDanmakuMob.java:463-468` 覆写 `getTarget()`：`danmakuLevel <= 0` 时**恒返回 null**；
  `tick()` 里多处（`:1022 / :1028 / :1060 / :1111`）用它，而 `setTarget()` 仍是原版。
  → 当难度配置在运行时被改成 0，BOSS 的 `lostTarget` 计数分支（`:1097-1103`）不会推进
  （因为 `target == null` 直接跳过整段），但 `moveTimer/lostTarget` 组合条件（`:1106`）
  也可能因此永远不满足，导致 BOSS「呆立不动直到配置改回」。
- 判定：**P1（配置边缘态，非崩溃）**。属移植刻意行为（`danmakuLevel<=0` 即"不出弹"），
  但建议在 `danmakuLevel <= 0` 时**同时**清 `moveTimer/moveVec`，避免残留移动指令。

### ✅ P0-1 证伪（达成「伪死亡 → 归零回血」闭环，非缺陷）

继续追 `LivingEntity.tick()`（1.21.1 源码 `LivingEntity.java:2453` 起）后确认完整链条：

```
LivingEntity.tick()                      // :2453
   → super.tick() = Entity.tick()        // Entity.java:430 → baseTick()
   → LivingEntity.baseTick()（实为 Entity.baseTick，:434）
        → isDeadOrDying() && level().shouldTickDeath(this)
        → tickDeath()                    // :481  ★ 每 tick 都跑
```

而 `EntityDanmakuMob.tick()`（`:906-913`）在**开头**手动跑 `tickDeath()`，**结尾**才
`super.tick()`，两者共用同一个 `deathTime` 字段，`deltaTime` 只在**服务端**（`tickDeath` 内
`!isClientSide()` 才 remove）以及**阶段转移时被归零**。因此：

- 每个伪死亡 tick 里 `tickDeath()` 的调用次数依路径为 1~2 次，但**只要阶段转移在
  `deathTime` 累积到 20 之前发生**（`moveDanmakuAttack` 里 `deathTime = 0`），
  就永远不会触发 `remove(KILLED)`；
- 各阶段转移都是 `moveDanmakuAttack(...)`，且都由 `attackCounter` 驱动的阶段数有限
  （如 Cirno 4 段、Sanae 5 段），最终落到 `ATTACK_END` → `canInterceptDeath() == false`
  → 正常 `die()` → `super.die()` → 战利品/掉落走 `dropCustomDeathLoot`；
- **客户端**：`tickDeath()` 的 remove 分支要求 `!level().isClientSide()`，客户端不会自行删除，
  靠服务端移除同步。

→ **原 P0-1 降级为「设计已自洽」**，保留一条**窄口径** P1：两处 `tickDeath()` 调用的
**累加节奏**（1~2 次/tick）会让伪死亡的「视觉时长」比 1.7.10 快约 2 倍；
且第 20 tick 的边界依赖「客户端 `tickDeath()` 无 remove」这一前提，
若将来有人在客户端也调 `remove()` 或在 `tickDeath()` 里加副作用，会变成真的提前死亡。
**置信度：高（已用源码逐行确认）**。

### P1-3：`EntitySanae.danmaku01()` 的循环复杂度与 `attackCounter` 越界耦合

- `EntitySanae.java:235`：`for(int i = 0; i < attackCounter; i++)`，而 `attackCounter` 由
  `EntityDanmakuMob.tick()`（`:1091`）每 tick `++`，只有 `attackCounter == 50` 分支
  （`:240` 附近）才 `setStopEnd()`、`>= 100` 归零（`:245` 附近）。**循环上界 = 当前攻击计数**，
  属 1.7.10 原作写法（原作同处 `for(int i = 0; i < attackCounter; i++)`，已比对
  `src/main/java/thKaguyaMod/entity/living/EntitySanae.java`）。
- 数值安全：`attackCounter` 在 0..100 之间，单 tick 至多 100 次迭代，**不是 P0**。
- 但这是**唯一**一处把攻击计数直接当循环上界用的地方，任何将来放宽 `attackCounter` 归零条件
  （如新增阶段忘了写 `attackCounter = 0`）都会立刻变成 O(attackCounter) 的每 tick 循环。
- 判定：**P1（脆弱点，当前安全）**。建议加注释或改为常量上界。

### 覆盖进度

- 已读完：`EntityDanmakuMob`（基类，1819 行，含 die/tick/tickDeath/moveDanmakuAttack/NBT/掉落）、
  `EntityCirno`、`EntityTHFairy`、`EntityFamiliar`、`EntitySanae`（部分）、
  `EntityTHPhantom`/`EntitySunFlowerFairy`（扫描）、
  `EntitySakuya`/`EntityMiko`/`EntityReimu`/`EntityRumia`/`EntityToziko`/`EntityWriggle`（结构扫描：
  与 Cirno 同构的多段 BOSS）。
- 未细读：`EntityDanmakuCreeper`、`EntityRinnosuke`、`VillagerRinnosuke`、`IDanmakuMob` 全文。
