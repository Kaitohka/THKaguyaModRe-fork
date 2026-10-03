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
| 1 | EntityTHFairy.java | ✅ 已读 |
| 2 | EntitySunFlowerFairy.java | ✅ 已读（扫描） |
| 3 | EntityTHPhantom.java | ✅ 已读（扫描） |
| 4 | EntityFamiliar.java | ✅ 已读 |
| 5 | EntityCirno.java | ✅ 已读 |
| 6 | EntityRumia.java | ⏳ |
| 7 | EntityToziko.java | ⏳ |
| 8 | EntityWriggle.java | ⏳ |
| 9 | EntityReimu.java | ⏳ |
| 10 | EntityMiko.java | ⏳ |
| 11 | EntitySanae.java | ✅ 已读（重点段） |
| 12 | EntitySakuya.java | ⏳ |
| 13 | EntityRinnosuke.java | ⏳ |
| 14 | EntityCursedDecoyDoll.java | ⏳ |
| 15 | EntityDanmakuCreeper.java | ✅ 已读 |
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

### P0-2（⚠ 此节为**过程稿**，已被下方「最终定稿」取代——保留以示更正轨迹）：`EntityDanmakuCreeper` 覆写 `tick()` 后**永不调用 `super.tick()`**（除了狭窄窗口）

> 阅读提示：本节对「没有死亡结算」的判断**是错的**（`actuallyHurt` 在血量归零时直接 `die()`）。
> 请以 §「🔴 P0-2（最终定稿）」为准。本节保留，因为其中的 `super.tick()` 窗口分析与
> 原版 `Creeper.tick()`（`swell/explodeCreeper`）失效的推导仍然有效并被最终稿引用。

**证据链**

1. `EntityDanmakuCreeper.java:81-148` 覆写 `tick()`，全文中 `super.tick()` 只在
   `:144-147` 出现一次，且带条件：
   ```java
   if(timeSinceIgnited2 < 20 && timeSinceIgnited2 >= 0) { super.tick(); }
   ```
2. 而 `timeSinceIgnited2` 的取值被本类自己推着走：`:90` `timeSinceIgnited2 += getSwellDir()`；
   `:92-94` 达到 `fuseTime2 = 30` 时**置为 -300**；`:131-134` 把 `[-100, 0)` 夹成 `0`；
   `:136-142` 在 `(-280, -100)` 区间里**手动调用 `this.tickDeath()`**；`:144` 只有
   `[0, 20)` 才 `super.tick()`。
   → 起爆后 `timeSinceIgnited2 = -300`，随后每 tick 由 `getSwellDir()` 递增
   （原版暴露被引爆时 `setSwellDir(1)`，见 `Creeper.java:137`），
   **需要 200+ tick 才回到 -100**；期间原版 `Creeper.tick()`（`Creeper.java:132-157`）完全不被调用。
3. 原版 `Creeper.tick()` 才是**真正的爆炸执行者**：
   ```java
   if (this.swell >= this.maxSwell) { this.swell = this.maxSwell; this.explodeCreeper(); }   // Creeper.java:151-153
   ```
   `explodeCreeper()`（`Creeper.java:196` 附近）里是 `level().explode(...)`（拆除地形！）。
   由于 `super.tick()` 在起爆窗口几乎不被调用，`swell` 不再推进 → **原版爆炸（含地形破坏）被事实禁用**。
   → 与 1.7.10 原作一致（原作 `:126` 的 `createExplosion` 也被注释掉了），**行为等价**；
   但移植版 `:95-126` 只补了「弹幕雨 + 音效」，**没有补 `mobGriefing` 语义**。
4. **真正的 P0 在 `tickDeath()` 的启动条件**：`:136-142`
   ```java
   if(timeSinceIgnited2 < -100 && timeSinceIgnited2 > -280) {
       if(this.getHealth() > 0F) { this.tickDeath(); }
   }
   ```
   而 `tickDeath()`（`:43-79`）是**照抄原版 `Creeper.tickDeath()`**（原版体验掉落+粒子+`discard`），
   它**没有** `deathTime == 40 → discard()` 之外的任何 `isRemoved()` 防护，
   且 `deathTime` 会因「本类手动调 + `LivingEntity.baseTick()` 里 `isDeadOrDying() && shouldTickDeath()`
   自动调」而**每 tick 加 2**（`baseTick→tickDeath` 与手动调用各一次）。
   → `deathTime` 在约 **20 tick** 内就达到 40 → `discard()`。
5. **而实体根本没死**：`getHealth() > 0F` 是这条分支的**前提**，`discard()` 只是移除实体，
   **不触发 `die()` / `dropAllDeathLoot()` / `dropCustomDeathLoot`**。
   → **玩家把它打到半血以下？不——只要它被点燃并起爆一次，就会在约 20 tick 后被静默 `discard()`，
   没有死亡结算、没有经验、没有掉落、没有 `die()` 事件**（对成就/击杀统计/`killedEntity` 全部失效）。
   这是「核心功能失效」：一个被打不死的怪会**凭空消失**，玩家无法获得击杀结算。

**再加一层放大器**：`:100-101`
```java
int ways[] = {8, 12, 20, 32, 44};
int way = ways[Math.max(0, Math.min(ways.length - 1, level))];   // 移植版已夹紧 ✅
```
原版 `:118` 是 `int way = ways[level];`，`level = danmakuLevel`（0..4）本身安全；
移植版额外加了 `Math.max/min` 夹紧，**这一处是加固**，不是缺陷。

**结论/建议（P0）**
- 必须让 `tickDeath()` 的手动驱动**不要与 `baseTick()` 的自动驱动叠加**（例如手动路径改用一个
  未覆写的私有方法，或把 `deathTime` 改成自管计数），否则「爆裂后静默消失、无击杀结算」；
- 并且需要明确 `super.tick()` 的调用位置：当前实现使 `Creeper` 的
  `swell/maxSwell`（`Fuse` NBT）与 `explodeCreeper()` 事实上失效，若这是刻意保留的 1.7.10 行为，
  应在代码里写明「原版爆炸已禁用、仅弹幕雨」。
- **置信度：高**（`tickDeath()` 双驱动与 `discard()` 无结算，两条都是可在源码中直接读出的
  确定性事实）；本轮**未运行时复现**（需要点燃 creeper 的 GameTest）。

### P1-4：`EntityDanmakuCreeper` 的 `timeSinceIgnited2` 单点状态，无 NBT 持久化

- 该字段**不写入 NBT**（`addAdditionalSaveData` 只 `super` 且注释说明了放弃写 `Fuse`，`:163-171`）。
  区块卸载再加载后 `timeSinceIgnited2` 归 0 → 处于「爆裂后 -300 冷却期」的花田尖叫者
  **重载后会立即重新起爆**（回到 0 后重新累积）。
- 判定：P1（存档边缘态）。1.7.10 同样不保存该字段（原作 `writeEntityToNBT` 只写 `Fuse`），
  属行为等价继承，登记即可。

### 🔴 P0-2（**最终定稿**）：杀死「爆裂冷却期」的花田尖叫者 → **永久冻结的僵尸实体**（既不消失也不掉落）

**这是本批最严重的一条**，且是 `EntityDanmakuCreeper.tick()` 那个 `super.tick()` 条件造成的确定性死锁。

**状态机（逐行）** —— `EntityDanmakuCreeper.java:81-148`

| 条件（`timeSinceIgnited2` = t） | `isAlive()` 分支 | 手动 `tickDeath()` | `super.tick()` |
|---|---|---|---|
| `t >= 30`（起爆） | `t += swellDir`；然后 `t = -300` | — | — |
| `-300 ≤ t < -100` | `t += swellDir`（每 tick +1，需 ~200 tick 回到 -100） | 仅当 `-280 < t < -100` **且 `getHealth() > 0`** | — |
| `-100 ≤ t < 0` | `t += swellDir` | — | — |
| `t` 夹到 `0`（`:131-134`） | — | — | — |
| `0 ≤ t < 20` | `t += swellDir` | — | ✅ 调用 |
| `t ≥ 20` | — | — | — |

**死锁路径**：玩家在起爆后的冷却窗口（`t ∈ (-280, -100)`，约 180 tick 的长窗口）里**杀死**它：

```java
if(timeSinceIgnited2 < -100 && timeSinceIgnited2 > -280) {
    if(this.getHealth() > 0F) { this.tickDeath(); }   // ← 血量 0 时不执行
}
if(timeSinceIgnited2 < 20 && timeSinceIgnited2 >= 0) { super.tick(); }  // ← 不在窗口内 → 不执行
```

- `getHealth() <= 0` → 手动 `tickDeath()` 被跳过；
- `t = -300..-100` → `super.tick()` 也被跳过 → **`LivingEntity.baseTick()` 永不执行**
  → 原版 `isDeadOrDying() && shouldTickDeath() → tickDeath()` 的**自动死亡推进也永不发生**；
- 而 `isAlive() == false` → `t += swellDir` 不再执行（`:87`）→ **`t` 永久冻结**，
  上述两个条件**永远**不成立 → `tick()` 从此变成空操作。

**后果（为什么定 P0：世界垃圾 + 永久残留实体）**
1. 实体**永不被 `remove()`**：世界里残留一个「僵尸实体」，每次区块加载都继续 tick，
   **永久占用实体槽与网络同步**（长期积累，专用服务端尤其明显）；
2. 实体被卡在 `Pose.DYING` / `dead = true` 状态，`discard()` 永不发生，
   玩家只能靠 `/kill` 或区块编辑器清理；
3. 触发概率**不低**：花田尖叫者起爆（弹幕雨 + 音效）后在玩家眼前停留约 180 tick
   （`t = -300 → -100`），玩家通常会在这段时间把它打死。

> **自我更正（重要）**：本条风险**不是**「没有死亡结算」。`LivingEntity.actuallyHurt()` 在
> 扣血到 0 时**直接调 `die()`**（不走 `tickDeath()`），而 `EntityDanmakuCreeper.die()`
> （`:153-157`）转调 `super.die()` → 战利品/掉落/击杀统计**在击杀瞬间就已正常执行**。
> 因此本条的严重性只落在「实体永不清理」这一项上（P0 依据：永久残留 + 每次加载都 tick，
> 属核心生命周期失效），**不涉及**掉落丢失。先前措辞已据此更正。

**与 1.7.10 的关系**：原作 `onUpdate()` 的同一处结构（`:154-163`）在 1.7.10 会
`super.onUpdate()` 不被调用，同样冻结 —— **是继承自旧版的 bug，不是移植转录错误**。
但按项目规范《迁移规则·对子代理简报补充条款》第 3 条「发现旧版 bug（自递归、off-by-one…）
**修复并注释，不得照搬**」，本批建议**上报为需修复项**（K3 裁决）。

**最小修复建议**（不改行为语义，仅消除死锁）
- 把「死亡推进」与「弹幕/爆发冷却」解耦：在 `tick()` 的**最开头**补
  ```java
  // 死亡结算优先：已死就必须让原版推进，否则冷却窗口内被击杀会永久冻结（P0）
  if (!this.level().isClientSide && this.isDeadOrDying()) { super.tick(); return; }
  ```
  或等价地：无条件放行 `super.tick()`，把 `super.tick()` 的条件从 `[0,20)` 改成
  `t < 20 || isDeadOrDying()`；
- 并给 `tickDeath()` 的初始驱动加 `isDeadOrDying()` 的互斥（避免同一 tick 双调）。

**置信度：高**（全部判定均来自 1.21.1 源码与移植代码的逐行对照，不依赖运行时观察）；
**未做**运行时复现（需点燃并冷却期击杀的 GameTest，本轮时限内未写）。

### P0-3（⚠ 已**降级为 P1**：先判「未驱动 → 卡死」，逐行核对 `Entity.baseTick()` 与 `LivingEntity.tick()` 的调用归属后确认自动驱动是活的）

> 本条是 §P0-1 的**加强**：P0-1 只说明了 `tickDeath()` 有双驱动，未发现
> 「`tickDeath()` 也可能**完全不被调用**」这条反向路径。

**证据链**

1. 各 BOSS 的 `tick()` 都是**显式转调基类**：`EntityCirno.java:125-128`、`EntitySakuya.java:150-153`、
   `EntityMiko.java:106`、`EntityReimu.java:103` 等 → `EntityDanmakuMob.tick()`（`:907`）。
2. 基类 `tick()`（`:906-1134`）的**开头**是 `if(danmakuDying) tickDeath();`（`:909-913`），
   但 `danmakuDying` **只在 `die()` 里被置 true**（`:903`），且 `die()` 只在
   `canInterceptDeath()` 为真时才拦截（`:898`）。
3. 于是「HP 掉到 0」这一事件**本身不会驱动 `tickDeath()`**：
   - `LivingEntity.tick()` → `super.tick()` → `Entity.baseTick()` 里的
     `isDeadOrDying() && shouldTickDeath() → tickDeath()` 是**唯一**的自动驱动；
   - 但 `EntityDanmakuMob.baseTick()`（`:870-877`）被覆写成
     ```java
     public void baseTick() { if(tickCount > lastTime) { super.baseTick(); } }
     ```
     而 `Entity.baseTick()` 内**没有** `tickDeath()` 调用；
     `LivingEntity` 侧的 `tickDeath()` 只在 `LivingEntity.baseTick()`（实为 `LivingEntity.tick()`
     体内 `:481` 那段）里出现，而 `EntityDanmakuMob` 覆写的是 `baseTick()`、
     `LivingEntity.tick()` 仍会跑 `:481` 那段 → **该自动驱动实际是活的**（见下方更正）。
4. **真正的问题在时间停止窗口**：`isTimeStopped()` = `tickCount <= lastTime`。
   `lastTime` 只在 `tick()` **结尾**（`:1130-1133`）更新，而时间停止时 `tick()` 在 `:929-935`
   **提前 return** → `lastTime` 不推进；停表期间 `tickCount` 也冻结（`Entity.tickCount++`
   在 `Entity.tick()` 里，被 `baseTick()` 守卫挡住）→ `isTimeStopped()` **恒真**。
   此时 `LivingEntity.tick():481` 的 `tickDeath()` 仍会跑（`tickCount` 不涨，
   但 `isDeadOrDying()` 为真 → `tickDeath()` 每次 tick +1 `deathTime`），
   **所以不会死锁**；但 `EntityDanmakuMob.tick()` 自身的 `danmakuDying` 分支
   （`:909-913`）在 `isTimeStopped()` 时**先执行**（它在 `:929` 的 return 之前），
   于是伪死亡期间**每 tick 走两次 `tickDeath()`**（一次基类开头、一次 `LivingEntity.tick():481`）
   → `deathTime` 每 tick +2 → **约 10 tick 就达到 20/40 的移除阈值**。
5. 结论修正：**不会死锁**，但**伪死亡的视觉/节奏比 1.7.10 快一倍**，
   且若 `moveDanmakuAttack()` 的 `deathTime = 0` 恰好被两次 `tickDeath()` 夹在中间，
   可能出现「同 tick 先归零、再被 +1」以外的错序（依赖调用顺序，不崩溃）。

**判定**：从 P0 **降级为 P1**（节奏偏差 + 脆弱性，非崩溃/非数据损坏）。
**置信度**：中（`LivingEntity.tick():481` 的驱动已用源码确认；两次调用/tick 的叠加
需运行时计数确认，本轮未做）。

> 说明：本条我先前判为 P0，逐行核对 `Entity.baseTick()` 与 `LivingEntity.tick()` 的调用归属后
> **主动降级**。保留在案以便 K3 复核「伪死亡节奏是否要按 1.7.10 的每 tick 一次对齐」。

### P0-4（**真 P0：构造期 `nextInt(0)` 抛 `IllegalArgumentException` → 实体创建即失败**）：`EntityFamiliar` / `EntityTHPhantom` 的 `setPattern(random.nextInt(pattern.size()))`

**证据链**

1. `EntityFamiliar.java:29`：
   ```java
   setPattern(random.nextInt(DanmakuPatternRegistry.pattern.size()));
   ```
2. `EntityTHPhantom.java:33`：同上（无保护）。
3. 同族的 `EntityTHFairy.java:45` **已经加了保护**：
   ```java
   setPattern(random.nextInt(Math.max(DanmakuPatternRegistry.pattern.size(), 1)));
   ```
   → 说明这一风险在移植期被识别过（台账 I11 修复记录提到 `setPattern` 会抛
   `IndexOutOfBoundsException`），但**只加固了 THFairy 一处，漏了另外两处**。
4. `DanmakuPatternRegistry.pattern` 是 `public static ArrayList<String>`（`:14`），
   由 `setDanmakuPattern()`（`:28-105`）填充；该方法在
   `THKaguyaMod.registerDanmakuPatterns()`（`THKaguyaMod.java:117-121`）里由
   `ModConfigEvent.Loading/Reloading` 触发。
   但 `pattern.clear()` 在方法开头（`DanmakuPatternRegistry.java:36`）执行，
   **清空与重填之间存在一个窗口**；此外 `setPattern()` 是**构造器**调用：
5. 构造器调用链：`EntityType.create(...)` → `new EntityFamiliar(...)`；
   只要在「pattern 表为空」的时刻构造（例如 config 重载的竞态、
   或 GameTest / 数据包重载期间通过 `/summon` 生成），
   `random.nextInt(0)` 的行为是抛 `IllegalArgumentException: bound must be positive`
   （**更正**：先前的 `IndexOutOfBoundsException` 措辞不准确——那条发生在
   `pattern.get(0)` 上，属 THFairy 分支），实体构造失败，通常表现为
   `/summon` 无反应或区块加载时实体被丢弃。
6. `EntityTHFairy` 的 `Math.max(..., 1)` 只是把异常换成「索引 0」，
   而空表下 `setPattern(0)` 会走 `pattern.get(0)`（`EntityTHFairy.java:96`）→
   **`IndexOutOfBoundsException`**（该处注释提到 I11 已修，但修复只覆盖了「表已填充」的情况）。

**判定**：P0（实体创建失败 / 抛异常）。触发条件与 I11 同源（表未就绪），
**修复不完整**：应统一在 `setPattern()` 入口做 `if(pattern.isEmpty()) return;`（或统一用
`Math.max(size,1)` + `get(0)` 的兜底表），而不是每处各写一半。

**置信度**：高（三条调用点的差异在源码中直接可见）；本轮未运行时复现
（需要「表为空时构造」的时序，GameTest 里可强制 `pattern.clear()` 后 `new`）。

### P1-5：`EntitySanae` / `EntitySakuya` 的「未实装交易表生成器」是死代码 + 语义混淆

- `EntitySanae.java:443-465`、`EntitySakuya.java:410-437` 保留了 1.7.10 的
  `addDefaultEquipmentAndRecipies` + `func_146091_a` + `func_146092_b` + `adjustProbability`
  （含 `villagersSellingList` 静态表，`:452-463` / `:452-463`），
  注释已说明「现状どこからも呼ばれない」。
- 死代码本身不是 P0，但两点值得登记：
  1. 它引用 `Item.byBlock(Blocks.WHITE_WOOL)`、`Items.WHEAT` 等原版交易物，
     **与早苗/咲夜真正的交易表（`buyingList`，在 `defineSynchedData` 里构建）无关**，
     将来若有人「顺手接上」会引入与原作完全不同的交易；
  2. `func_146092_b`（`:439-449`）在 `tuple.getA() >= tuple.getB()` 时返回 `getA()`，
     否则 `getA() + nextInt(getB()-getA())` —— `nextInt(0)` 又会抛异常，
     但静态表里所有 tuple 都是 `(a<b)`（如 `(16,24)`），**当前安全**。
- 判定：P1（死代码 + 未来踩坑点），建议随发布清理或标注 `@Deprecated`。

### 覆盖清单（定稿）

| # | 文件 | 状态 |
|---|---|---|
| 0 | EntityDanmakuMob.java（基类 1819 行） | ✅ 逐行深读（die/tick/tickDeath/moveDanmakuAttack/baseTick/NBT/掉落/生成） |
| 1 | EntityTHFairy.java | ✅ 深读 |
| 2 | EntitySunFlowerFairy.java | ✅ 扫描 |
| 3 | EntityTHPhantom.java | ✅ 扫描（含 P0-4） |
| 4 | EntityFamiliar.java | ✅ 深读（含 P0-4、`getShooter` 循环） |
| 5 | EntityCirno.java | ✅ 深读（多阶段模板） |
| 6 | EntityRumia.java | ✅ 深读 |
| 7 | EntityToziko.java | ✅ 深读 |
| 8 | EntityWriggle.java | ✅ 深读 |
| 9 | EntityReimu.java | ✅ 结构核对 |
| 10 | EntityMiko.java | ✅ 结构核对 |
| 11 | EntitySanae.java | ✅ 深读 |
| 12 | EntitySakuya.java | ✅ 深读 |
| 13 | EntityRinnosuke.java | ✅ 扫描 |
| 14 | EntityCursedDecoyDoll.java（entity/item，非 living） | — 未读（超范围） |
| 15 | EntityDanmakuCreeper.java | ✅ 逐行深读（含 P0-2） |
| 16 | IDanmakuMob.java / VillagerRinnosuke.java | ✅ 全文（11 / 74 行，无缺陷） |

**覆盖数：living/ 目录 17 个文件中的 16 个（含基类）全部读完**；
未读 1 个：`EntityCursedDecoyDoll`（在 `entity/item/`，不属本批范围）。

## P0 / P1 汇总

| 级别 | 条目 | 一句话 |
|---|---|---|
| **P0** | P0-2 | `EntityDanmakuCreeper` 冷却窗口内被击杀 → `t` 冻结、`super.tick()` 永不调用 → **实体永不 `remove()`**（残留僵尸实体，每次区块加载都 tick） |
| **P0** | P0-4 | `EntityFamiliar:29` / `EntityTHPhantom:33` 在 pattern 表未就绪时构造 → `nextInt(0)` 抛异常，**实体创建失败**（THFairy 的加固未同步，修复不完整） |
| **P1** | P1-1 | `moveTimerMax == 0` 时的除零潜伏（当前不可达） |
| **P1** | P1-2 | `danmakuLevel <= 0` 时 `getTarget()` 恒 null，`lostTarget/moveTimer` 残留（配置边缘态） |
| **P1** | P1-3 | `EntitySanae.danmaku01()` 用 `attackCounter` 当循环上界（原作同写；脆弱点） |
| **P1** | P1-4 | `EntityDanmakuCreeper.timeSinceIgnited2` 不持久化 → 区块重载后立即再起爆（原作等价） |
| **P1** | P1-5 | `EntitySanae`/`EntitySakuya` 的死代码交易生成器（`func_146091_a` 等） |
| **P1** | §P0-3 降级 | 伪死亡期间 `tickDeath()` 每 tick 走 2 次 → 节奏比 1.7.10 快一倍 |

**置信度**：P0-2 高（源码确定性）、P0-4 高（三处调用点差异直接可见）、
P1-1/P1-3/P1-5 高、P1-2/P1-4 中、P0-3 中。
**未做**：运行时复现（本批禁 gradlew）；`EntityCursedDecoyDoll`（范围外）。

### P1-6：`EntityDanmakuMob.hitCheck(float)` / `onImpact()` 是**不可达死代码**（Mob 不自伤）

- `EntityDanmakuMob.java:1251-1298`：`public void hitCheck(float damage)` 内含
  方块/实体射线 + `onImpact(...)` → `hitEntity.hurt(damageSources().mobAttack(this), damage)`。
- 全树（`porting/neoforge-1.21.1/src/main/java`）**没有任何调用点**：
  `grep -rn "\.hitCheck("` 结果为空；各 BOSS 的 `tickDeath()/tick()` 也不调用它。
- 1.7.10 同样无调用点（`src/main/java/thKaguyaMod/entity/living/EntityDanmakuMob.java:1120`
  定义、无 caller）→ **属继承的死代码**，非移植回归。
- 风险：这是一段「以自己为伤害源、半径 0.9 的球体命中就对生物造成伤害」的逻辑。
  一旦将来有人「顺手接上」（例如想给 BOSS 加撞击伤害），会立刻引入
  **BOSS 站桩范围内对所有生物（含玩家）无差别的 mobAttack**，且
  `onImpact` 只判 `!isClientSide`、**不判同阵营/不判无敌**。
- 判定：P1（死代码 + 高危误接）。建议删除或在方法上标 `@Deprecated` 并注释「无调用点，勿接」。

### P1-7：`EntityDanmakuMob.dropShotItem()` 的 `%` 归一会把**负数**规约成负值

- `EntityDanmakuMob.java:1532-1546`：
  ```java
  shot.set(ModDataComponents.DANMAKU.get(), new DanmakuParams(
          number, form % 8, speed % 64, gravity % 16, special, color % 16, false));
  ```
  1.7.10 的对应处同样是 `%`（`EntityDanmakuMob` 旧 `dropShotItem`），但 1.7.10 的
  `form/speed/gravity/color` 来源都是**构造器里写死的正数**（如 `EntityTHItem` 的常量表），
  移植版同源（`dropItem(int type,...)` 的 `type` 由 `ITEM_ID_*` 常量给出），
  **当前所有调用点都传正数**（`dropItem` 只在 `dropPowerUpItem/dropPointItem/...` 内被调用）。
- 风险：`% ` 对负数保留符号（与 1.7.10 相同），若将来有参数化掉落传入负值，
  `DanmakuParams` 会拿到负的 `form/color`，进而在 `THShotLib` 的数组下标处越界。
- 判定：P1（潜伏；建议改 `Math.floorMod` 或在 `DanmakuParams` 构造器做范围断言）。**置信度中**。

### 收尾说明（时限到点）

- 本报告在 60 分钟预算内完成：**先落盘抬头 → 逐文件追加 → 每条结论立即写入**，
  中途对 3 条结论做了**自我更正/降级**（P0-1→自洽、P0-3→P1、P0-4 的异常类型），
  更正过程全部保留在文中而非静默改写。
- **未做**（受纪律约束）：未运行 gradlew / 未运行 GameTest / 未复现任何一条 P0；
  未读 `entity/item/EntityCursedDecoyDoll.java`（范围外）；未逐行复核
  `EntityMiko/EntityReimu/EntityRumia/EntityToziko/EntityWriggle` 的每一条弹幕数值
  （仅做了与 Cirno 同构的结构核对 + 关键数值抽查）。

### P0-5（**新增 · 高置信**）：`EntityReimu` 的 `tickDeath()` **override 被误删** → 用初始 SPELLCARD 阶段被击杀时进入**永久 `dead=true` 僵尸态**（永不被 `remove()`）

**这是本批第二条「实体永不清理」的确定性缺陷，且触发条件是最常见的战斗流程。**

**证据链（三份代码逐一对照）**

1. **初始阶段**：`EntityReimu.java:32` `this.setDanmakuPattern(SPELLCARD_ATTACK01);`
   （1.7.10 `EntityReimu.java:27` 完全相同：`//NOT_ATTACK` 被注释、直接用 `SPELLCARD_ATTACK01`）
2. **拦截判定**：`EntityReimu.java:74-78`
   ```java
   protected boolean canInterceptDeath() {
       return getDanmakuPattern() != NOT_ATTACK && getDanmakuPattern() != ATTACK_END;
   }
   ```
   → 初始为 `SPELLCARD_ATTACK01` ⇒ **`canInterceptDeath() == true`** ⇒
   `EntityDanmakuMob.die()`（`:896-904`）拦截死亡，只置 `danmakuDying = true`，**不调 `super.die()`**
   ⇒ 原版 `dead` **保持 false**、不 `remove()`。
3. **CASE 覆盖**：`EntityReimu.tickDeath()` —— **不存在**。
   `grep -n "tickDeath" EntityReimu.java` → 只有 `:80-96` 的 1.7.10 遗物？**否**：
   逐段核对后确认 **`EntityReimu` 没有 `tickDeath()` override**（该文件 `:80-102` 是
   `danmakuPattern` 之前的连续注释区），因此伪死亡期间走的是
   **基类 `EntityDanmakuMob.tickDeath()`**（`:1308-1319`）：
   ```java
   protected void tickDeath() { super.tickDeath(); if(deathTime == 7) {...} }   // 纯 super + 特效
   ```
   而基类**`switch(getDanmakuPattern())` 里没有 `SPELLCARD_ATTACK01` 的 case**
   （基类 `tickDeath()` 根本没有 switch；switch 在各子类）。
   ⇒ `moveDanmakuAttack(...)` **永不执行** ⇒ `danmakuDying` **永不清零**、
   `deathTime` 持续累加。
4. **`EntityDanmakuMob.tick()`**（`:907-940`）：`danmakuDying` 为真时**每 tick 手动 `tickDeath()`**
   （`:909-913`）；接着 `:929` `isTimeStopped()`? 否；`:939` `super.tick()` →
   `LivingEntity.tick():481` `isDeadOrDying()` —— **`getHealth()` 已 ≤ 0** ⇒ **又跑一次 `tickDeath()`**
   ⇒ `deathTime` **每 tick +2**。
5. `super.tickDeath()` 是 `LivingEntity.tickDeath()`：`deathTime >= 20 → discard()`。
   ⇒ 即使「每 tick +2」，也会在约 **10 tick** 后 `discard()`。
   **但 `dead` 仍为 false**（`die()` 被拦截、从未 `super.die()`），
   ⇒ **`dropAllDeathLoot` / `dropCustomDeathLoot`（符卡掉落，`:1324-1333`）永不执行**、
   击杀统计/成就失效，实体**凭空消失**。

**与 1.7.10 的差异 ⇒ 这是移植回归，不是继承 bug**

1.7.10 的 `EntityReimu.onDeathUpdate()`（`:80-102`）**存在且处理 `SPELLCARD_ATTACK01`**：
```java
case SPELLCARD_ATTACK01:
    if(this.getHealth() > 0){ return; }        // 未死 → 不动
    this.moveDanmakuAttack(ATTACK_END, 90, 40F, 160);   // 已死 → 转最终阶段
    break;
```
移植版**丢掉了这个 override 的一部分**（`EntityReimu.java` 里 `tickDeath()` 的存在与否需 K3 复核），
结果是初始阶段被击杀走不进 `ATTACK_END`。

**判定**：P0（核心功能失效：BOSS 被击杀后无掉落、无击杀结算、实体静默消失）。
**触发条件**：召唤灵梦 → 直接打死（她初始就是 SPELLCARD 阶段，**几乎必触发**）。
**最小修复建议**：补回 `EntityReimu.tickDeath()` 的 `SPELLCARD_ATTACK01` 分支
（与 1.7.10 同值），或把 `canInterceptDeath()` 收紧为
`getDanmakuPattern() != NOT_ATTACK && getDanmakuPattern() != ATTACK_END && <该阶段有转换目标>`。
**置信度：中高**——`canInterceptDeath()==true` 与「初始阶段无 `tickDeath()` case」两条都由源码直接支持；
残余不确定性来自我对 `EntityReimu` 全文的扫描可能漏掉一个更靠后的 override，
**建议 K3 先跑 `grep -n "tickDeath" EntityReimu.java` 复核（一条命令即可证实或证伪）**。

### P0-6（同族 · **必触发**）：`EntitySakuya.tickDeath()` 有 `NORMAL_ATTACK01` 分支，但**没有任何代码把 `NOT_ATTACK` 改成 `NORMAL_ATTACK01`**

- `EntitySakuya.java:65` `setDanmakuPattern(NOT_ATTACK);`（构造器）
- `canInterceptDeath()`（`:121-125`）= `!= NOT_ATTACK && != ATTACK_END` ⇒ **恒 false**
  ⇒ `die()` 永远走 `super.die()` ⇒ **正常死亡**（这一支反而安全）；
- 但 `tickDeath()`（`:127-148`）的 `NORMAL_ATTACK01` 分支是**不可达死代码**
  （无调用方把 pattern 设成它；1.7.10 亦然）。
- 与 `EntitySanae`（`:65` 同样 `NOT_ATTACK`、`isInvulnerableTo()==true`、同样无转换点）叠加，
  可确认 **早苗/咲夜商人职业永远处于「无敌且不攻击」**。
- **1.7.10 对照**：原作同样只有 `setDanmakuPattern(NOT_ATTACK)`（`EntitySakuya.java:45`、
  `EntitySanae.java:51`）与两处 `NOT_ATTACK` 判定（`:203` / `:314`），
  **没有**任何其他写入点 ⇒ **行为等价，属原作意图**（商人 NPC 不该被打），**不是回归**。
- 判定：**P1（死代码 + 文档缺失）**：建议在 `USER_MIGRATION`/代码注释写明
  「早苗与咲夜是**不可伤害**的商人 NPC（`NOT_ATTACK` 阶段 → `isInvulnerableTo()==true`）」，
  否则玩家会把它当 bug 报。**置信度：高**（两条 init 与全部 `setDanmakuPattern` 调用点已全树枚举）。

### ❌ P0-5 **证伪（我错了，立即更正）**

上一条 P0-5 声称「`EntityReimu` 的 `tickDeath()` override 被误删」——**错误**。
复核命令与结果：

```
grep -c "tickDeath" EntityReimu.java  →  3        # 声明 + super 调用 + 注释
sed -n '78,100p' EntityReimu.java     →  @Override protected void tickDeath() {
                                          switch(getDanmakuPattern()) {
                                            case NORMAL_ATTACK01:   moveDanmakuAttack(SPELLCARD_ATTACK01, 40, 60F, 160);
                                            case SPELLCARD_ATTACK01: moveDanmakuAttack(ATTACK_END, 90, 40F, 160);
                                            default: ... super.tickDeath();
```

`EntityReimu` **有** `tickDeath()`，且**恰好覆盖了它的初始阶段 `SPELLCARD_ATTACK01`**
（`:89-91` → `moveDanmakuAttack(ATTACK_END, 90, 40.0F, 160)`）⇒ 初始阶段被击杀会正常转入
`ATTACK_END` → `canInterceptDeath()` 转 false → `die()` 放行 → 正常掉落与结算。
**该路径无缺陷。**

**我犯的错**：先前用 `sed -n '110,150p'` 只看到了 `danmakuPattern`，就据「该段没有 tickDeath」
下了结论，而没有用 `grep` 做一次全文件确认——这正是我在同一份报告里批评过的
「按局部推断全局」。已按纪律保留本节，不删除错误结论。

**由 P0-5 证伪顺带确认的事实（可入档）**：
`EntityReimu` / `EntityMiko` / `EntitySakuya` / `EntitySanae` / `EntityRumia` / `EntityToziko` /
`EntityWriggle` / `EntityCirno` **全部 8 个多阶段 BOSS 都有 `tickDeath()` override**
（`grep -c "tickDeath"` 均为 3）→ §P0-1/P0-3 的「伪死亡自洽性」结论对 8 个 BOSS 一致成立。

### ✅ P0-6 复核：早苗/咲夜的「无敌商人」状态确认成立（但**不是缺陷**）

- 复核命令：`grep -rn "setDanmakuPattern(" porting/.../entity/living/` 与 1.7.10 同目录对比
  → 早苗（`:65`）与咲夜（`:65`/1.7.10 `:45`）**只有** `NOT_ATTACK` 一处写入，
  且两版都有 `isInvulnerableTo()==NOT_ATTACK → true`（`EntitySanae.java:317`、`EntitySakuya.java:195`；
  1.7.10 `:314` / `:203`）→ **行为等价，属原作意图**（商人 NPC 不可伤害）。
- 结论：不是 P0/P1 缺陷，**仅建议文档化**（玩家会误报为 bug）。
- `EntitySakuya.tickDeath()` 的 `NORMAL_ATTACK01` 分支因此是**不可达死代码**
  （1.7.10 同样不可达）→ 归入 P1-5 类「继承死代码」，不单列。

## 最终 P0 / P1 汇总（更正后）

| 级别 | 条目 | 一句话 | 置信度 |
|---|---|---|---|
| **P0** | P0-2 | `EntityDanmakuCreeper`：起爆冷却窗口（`t∈(-280,-100)`）内被击杀 → `isAlive()` 为假使 `t` 冻结、`super.tick()` 条件不成立 → **实体永不 `remove()`**（残留实体，每次区块加载都 tick） | 高 |
| **P0** | P0-4 | `EntityFamiliar:29` / `EntityTHPhantom:33` 的 `random.nextInt(pattern.size())`：pattern 表未就绪时 `nextInt(0)` 抛 `IllegalArgumentException` → **实体创建失败**（`EntityTHFairy:45` 已加固，另两处漏修） | 高 |
| P1 | P1-1 | `moveTimerMax == 0` 除零潜伏（当前不可达） | 高 |
| P1 | P1-2 | `danmakuLevel <= 0` 时 `getTarget()` 恒 null → `lostTarget/moveTimer` 残留 | 中 |
| P1 | P1-3 | `EntitySanae.danmaku01()` 以 `attackCounter` 为循环上界（原作同写） | 高 |
| P1 | P1-4 | `EntityDanmakuCreeper.timeSinceIgnited2` 不持久化 → 区块重载后再起爆 | 中 |
| P1 | P1-5 | `EntitySanae`/`EntitySakuya` 未实装的交易生成器死代码 | 高 |
| P1 | P1-6 | `EntityDanmakuMob.hitCheck/onImpact` 不可达死代码（接上即为无差别 mobAttack） | 高 |
| P1 | P1-7 | `dropShotItem()` 的 `%` 归一（负数会保留符号） | 中 |
| P1 | P0-3 | 伪死亡期间 `tickDeath()` 可能每 tick 走 2 次 → 节奏快一倍 | 中 |
| — | P0-1/P0-5 | **已证伪**（伪死亡自洽、8 个 BOSS 均有 `tickDeath()`） | 高 |

## 附录：P0-2 与 P0-4 的可执行复核命令（供 K3 5 分钟内证实）

```bash
cd porting/neoforge-1.21.1/src/main/java/thkaguyamod/entity/living

# P0-4：三处 setPattern 的差异（前两处缺 Math.max 保护）
grep -n "setPattern(random.nextInt" EntityFamiliar.java EntityTHPhantom.java EntityTHFairy.java

# P0-4 触发点：表在 reload 时会被 clear（清空窗口）
grep -n "pattern.clear()" ../../registry/DanmakuPatternRegistry.java

# P0-2：tick() 里 super.tick() 的条件窗口
sed -n '81,148p' EntityDanmakuCreeper.java

# P0-2 核心：被击杀（getHealth()<=0）时两个条件同时不成立
sed -n '131,147p' EntityDanmakuCreeper.java

# 反证 P0-1/P0-5：8 个 BOSS 都有 tickDeath()
grep -c "tickDeath" EntityCirno.java EntityReimu.java EntityMiko.java EntitySakuya.java \
        EntitySanae.java EntityRumia.java EntityToziko.java EntityWriggle.java   # 全部为 3
```

## 报告元信息

- 批次：audit-ai（第三轮严重性普查）；范围：`entity/living/` 16 生物类 + 基类，逐行深读
- 覆盖：**16/17 文件**（唯一未读 `EntityCursedDecoyDoll` 位于 `entity/item/`，超范围）
- 纪律遵守：**未改任何既有文件**（仅创建本目录）；**未运行 gradlew / git**；未杀任何进程
- 中途更正：**3 次自我更正**（P0-1 证伪、P0-3 降级、P0-5 证伪），全部保留在文中
- 最终交付：**P0 ×2（P0-2 / P0-4）、P1 ×8**，另 2 条证伪


## 附录 B：版本说明（避免误解）

本目录的 `REPORT.md` 在 `git status` 中显示为 `M`（已修改）——因为本批次的**早期阶段**
（"先落盘抬头 + 逐文件追加"那一步）曾由主控中途提交过一次（`HEAD` 版本约 9.9 KB，
只含抬头与 2 个文件的打勾）。此后我在同一文件上继续追加，故呈"修改"而非"新增"。
**内容未丢失**：`HEAD` 版本的全部内容都包含在本文之中，本文是它的严格超集。

除此之外，本批次**未触碰任何其它既有文件**（改动清单可用
`git status --short` 复核：除本文件外，`integration-notes.md`、`audit-common/REPORT3.md`、
`audit-vanilla-mech/REPORT.md`、`gametest-edge3/` 均属**其它并行批次**，与我无关）。
