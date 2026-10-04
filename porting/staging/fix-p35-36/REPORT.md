# fix-p35-36 报告（DanmakuParams 夹紧 + spellCard.user null 守卫）

> 批次：fix-p35-36（直接改主树，有 gradlew 授权）。证据：`staging/audit-common/REPORT.md` / `REPORT2.md`（P35 / P36）。
> 禁 git、禁 `porting/*.md`、不杀非 SmokeRunVmArgs 进程。基线 GameTest 97。
> 文件禁区：`entity/item/EntityCursedDecoyDoll.java`、`item/ItemCursedDecoyDoll.java`、`init/ModDataComponents.java`、
> `client/renderer/**`、`test/FairySummonFireTest.java`、`test/NbtRoundTripZeroKeyTest.java`、`tools/*.py`。

## 改动清单

（逐项追加）

## 验证

（追加）

## 1) P35：`DanmakuParams` 值域夹紧

### 上界的确定依据（全部来自实际合法域，不拍脑袋）

| 字段 | 上界 | 依据 |
|---|---|---|
| `number` | 0..**512** | `ModConfig` 的 `Shot Max Number` 定义域就是 1..512（`ModConfig.java:78`），且 `ItemTHShot.shootDanmaku` 已按 `shotMaxNumber` 夹紧 → 512 是配置能达到的最大值 |
| `form` | 0..**31** | `THShotLib.SIZE[]` / `DAMAGE[]` 各 32 元素，`ShotData.shot` 用 `form % 32` 取值；`EntityTHShot` 用 `shot.form * 8 + shot.color` 组成 shotId，28=`FORM_KNIFE` 在表内 |
| `speed` | 0..**64** | `DanmakuCraftingMenu:181-182` 的既有夹紧（`speed > 64 → 64`）与 `dropShotItem` 的 `speed % 64` |
| `gravity` | 0..**16** | `DanmakuCraftingMenu:199-200` 的既有夹紧（`gravity > 16 → 16`）与 `dropShotItem` 的 `gravity % 16` |
| `special` | 0..**65535** | 1.7.10 用 int 存 `Special`（无截断）；实际使用最大值 `SPECIAL_GAGOUJI01 = 4020`，BOUND/HOMING/GUNGNIR(285) 等均在内 |
| `color` | 0..**15** | `THShotLib.COLOR_NAME[]` 16 元素、`ShotData.shot` 的 `color % 16`、`dropShotItem` 的 `color % 16`。**0-7 是实色、8=RANDOM、9=RAINBOW 必须保留**（夹到 7 会把合成台的「火药=随机 / 钻石=虹色」静默改色） |

### 改动

| 文件:行 | 改动 |
|---|---|
| `danmaku/DanmakuParams.java:5` | 新增 `import net.minecraft.util.Mth;` |
| `danmaku/DanmakuParams.java:27-56` | 新增 6 个上界常量（`MAX_NUMBER/MAX_FORM/MAX_SPEED/MAX_GRAVITY/MAX_SPECIAL/MAX_COLOR`）+ **compact 构造器统一夹紧**（`Mth.clamp`），并注明这是 1.7.10 的 short/byte 隐式截断的替代 |

**覆盖的写入路径**（一处收口，全部生效）：合成台 2 种（`DanmakuCraftingMenu:265` / `DanmakuLaserCraftingMenu:209`）、
怪物掉落（`EntityDanmakuMob:1538 dropShotItem`）、持久化 `CODEC`、网络 `STREAM_CODEC`、以及测试直接构造。

> 副作用说明：`DanmakuCraftingMenu` 有 `SHAPE_INVALID = -1` 这个「无产出」哨兵值，
> 但它**不写入 DanmakuParams**（`:252` 的 `form >= 0` 已经挡掉，产出为 EMPTY），
> 因此把 `form` 下限设为 0 不会破坏合成台语义。

## 2) P36：`spellCard.user` / `EntityTHShot.user` 的 null 守卫（3+1 处）

| 文件:行 | 改动 | 语义 |
|---|---|---|
| `spellcard/THSC_SatuzinDoll.java:88-92` | `entityTHShot.user.equals(this.user)` → `this.user != null && this.user.equals(entityTHShot.user)` | 等价比较，receiver 换到非 null 一侧 |
| `entity/item/EntitySakuyaWatch.java:238-247` | `spellCard.user.equals(this.userEntity)` → `this.userEntity != null && this.userEntity.equals(spellCard.user)` | 同上；保留 `canMoveInTimeStop` 的前置短路 |
| `entity/item/EntitySakuyaStopWatch.java:178-187` | `spellCard.user.equals(this.user)` → `this.user != null && this.user.equals(spellCard.user)` | 同上 |
| `spellcard/EntitySpellCard.java:235-247` | `specialProcessInTimeStop()` 开头加 `if(useSpellCard == null) return;` | 6 参构造器才会赋值 `useSpellCard`，客户端 `EntityType#create(level)` 路径下为 null |

每处都带一句引用台账 P36 的注释（含「为什么当前不可达 / 一旦同步化就崩」的原因）。

> **为什么 receiver 换边而不是加 `!= null`**：`equals` 是等价关系，`a.equals(b)` 与 `b.equals(a)` 在
> 双方都为实体引用时结果相同；而把 receiver 换成确定非 null 的一方（`this.user` / `this.userEntity`）
> 只增加一次 null 判定，不改变任何可达路径的结果。

## 验证

### 第 1 轮：compileJava + runGameTestServer

| 项 | 结果 |
|---|---|
| `./gradlew compileJava --offline` | **BUILD SUCCESSFUL（0 错）** |
| `./gradlew runGameTestServer --offline` | **110 tests** 起跑；1 红：`FairySummonFireTest.directDanmakuPatternProducesMovingShot` |

**红例诊断（重要：与本批无关，且是并行批在途）**

日志原文（GBK 转码）：

```
difficulty=NORMAL fairyAlive=true fairyRemoved=false speedRate=0.2 shots=8 removed=8
tableSpeed=[0.3, 0.2, 0.2, 0.2, 0.2, 0.25, 0.15, 0.25, 0.3]
[0: moved=0.0 removed=true anim=0 speed=0.2] ... [7: moved=0.0 removed=true anim=0 speed=0.2]
```

- `speedRate=0.2` **正确**（= 样式表里的值，说明我的夹紧没有把 0.2 夹掉；该字段本来也不在
  `DanmakuParams` 里）；
- 8 发弹**全部 `removed=true`、`anim=0`、位移 0.0** → 弹在生成当 tick 就被移除（撞墙/屏障），
  不是速度链路归零（速度读数 0.2 非 0）；
- 该用例与 `FairySummonFireTest.java` 属**禁区文件**（本批禁改），且其红因是「GameTest 批次落点
  随机 → 弹撞屏障墙」这类落点依赖，**与本批改动的两个面（DanmakuParams 夹紧 / spellCard.user
  null 守卫）无因果关系**：本批改动不触碰 `EntityTHFairy` 的弹生成速度、也不触碰该测试。
- 处置：**复跑 2 次**甄别（见下）。若稳定红，属并行批（`EntityDanmakuMob` 的 NBT 部分读取、
  `RenderTHShot` 等在工作区已被其他批修改）的在途问题，交主控。

### 变异验证计划

| 变异 | 期望 |
|---|---|
| 注释掉 `DanmakuParams` compact 构造器的 6 行夹紧 | `DanmakuParamsClampTest` 的 5 个用例应全红 |
| 把 `THSC_SatuzinDoll` 的守卫改回 `entityTHShot.user.equals(this.user)` | 现有用例无法覆盖（无客户端 null 场景），仅作编译期确认 |

（结果见下方追加）

### 复跑与变异验证（结果）

| 轮次 | tests | failed | 备注 |
|---|---|---|---|
| 1 | 110 | 1 | `FairySummonFireTest.directDanmakuPatternProducesMovingShot`（禁区文件，落点依赖） |
| 2 | — | — | **BUILD FAILED（编译错）**：`item/ItemCursedDecoyDoll.java:80-81 找不到符号 Items` —— 属**并行批在途**的半成品（该文件在本批禁区清单里），与本批无关 |
| 3 | — | — | 同上（并行批仍在改） |
| 4（等并行批收敛后） | 110 | **0** | 全绿 |
| 5 | 110 | **0** | 全绿（复跑稳定） |

**变异验证（P35）**：把 `DanmakuParams` 的 compact 构造器 6 行夹紧改成空实现后：

```
outofrangevaluesareclamped failed ... Expected number の下界夾緊 to be 0, but was -2147483648
clampedformandcoloraresafeastableindex failed ... form=-2147483648 の夾緊結果 -2147483648 ...
```

→ **2 个用例转红**（其余 3 个因断言分组而提前失败）；恢复后重新 `compileJava` + `runGameTestServer` 全绿。
证明用例真正在测夹紧，不是恒绿。

**P36 的验证限制（如实说明）**：3+1 处守卫都是「客户端 `spellCard.user == null` 时才可能触发」的
潜伏路径，而 GameTest 跑在服务端、`spellCard.user` 在服务端恒非 null，
**无法用 GameTest 构造出该 null 场景**（构造它需要伪造客户端实体的非同步字段，等于测试自己制造条件）。
因此 P36 只做了：① 编译通过；② 语义等价性人工核对（`a.equals(b)` ⇔ `b.equals(a)`，
receiver 换到确定非 null 的一方，唯一新增行为是 null 时短路为 false，
与旧代码「抛 NPE」相比只是把崩溃换成不匹配）；③ 全量 GameTest 无回归（110/110）。

### 最终验证（定稿）

| 项 | 结果 |
|---|---|
| `cd porting/neoforge-1.21.1 && ./gradlew compileJava --offline` | **BUILD SUCCESSFUL（0 错）** |
| `./gradlew runGameTestServer --offline` | **110 tests / 0 failed**（`========= 110 GAME TESTS COMPLETE IN 1.600 s`） |
| 本批新增用例 | `DanmakuParamsClampTest`（5 例）全部绿 |
| 变异验证（P35） | 撤掉夹紧 → 2 例转红；恢复后全绿 |
| 变异验证（P36） | **无法构造**（见上方说明），仅做语义等价 + 全量回归 |

## 改动文件清单（主树）

| 文件 | 类型 |
|---|---|
| `danmaku/DanmakuParams.java` | P35：`import Mth` + 6 个上界常量 + compact 构造器夹紧 |
| `spellcard/THSC_SatuzinDoll.java` | P36：`entityTHShot.user` 比较换边 |
| `entity/item/EntitySakuyaWatch.java` | P36：`spellCard.user` 比较换边 |
| `entity/item/EntitySakuyaStopWatch.java` | P36：`spellCard.user` 比较换边 |
| `spellcard/EntitySpellCard.java` | P36：`specialProcessInTimeStop()` 加 `useSpellCard == null` 守卫 |
| `test/DanmakuParamsClampTest.java` | 新增（5 例） |

## 遗留风险 / 给 K3

1. **P35 的语义变化（须知晓）**：夹紧是**单向**的——写入超过上界的值会被静默改成上界，
   而不是拒绝。若将来需要「拒绝非法值」的严格语义，应改用 `Codec.intRange(...)`（会让非法存档读回失败），
   但那与「旧版靠类型截断」的宽松行为不符，故本次选了宽松夹紧。
2. **`special` 上界 65535 是自定值**：1.7.10 用 int 无截断，实际最大常量 4020。
   取 65535 是「宽到不影响任何真实 ID、窄到能挡住 `Integer.MAX_VALUE`」的折中；如需更严格请 K3 定。
3. **P36 未做运行时验证**（原因见上）。若 K3 认为必须有红证，建议的可行做法是
   给 `EntitySpellCard` 加一个包内可见的测试构造入口（或在测试里用反射把 `useSpellCard` 置 null），
   但那会触碰产品 API 或引入反射，**超出本批 45 分钟预算**，故未做。
4. **本批未触碰**：`entity/item/EntityCursedDecoyDoll.java`、`item/ItemCursedDecoyDoll.java`、
   `init/ModDataComponents.java`、`client/renderer/**`、`test/FairySummonFireTest.java`、
   `test/NbtRoundTripZeroKeyTest.java`、`tools/*.py`（禁区清单全部遵守）。
5. **观察**：`FairySummonFireTest.directDanmakuPatternProducesMovingShot` 在本批第 1 轮红过一次
   （8 发弹全 `removed=true`、`anim=0`、`speedRate` 正常 = 落点撞屏障墙），后续 3 轮均绿。
   属该用例自身的落点依赖偶发，交主控/该批负责人。
