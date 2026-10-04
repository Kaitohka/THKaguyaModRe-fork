# diag-fairy-flake — `FairySummonFireTest.directDanmakuPatternProducesMovingShot` 偶发红 根因

## 结论（一句话）

不是「delay 让弹不动」，而是**同批次其它用例触发的 `THShotLib.danmakuRemove` 在弹踏出第一 tick 之前
就把它们 `discard` 了**（`tickCount == 0`）。弹速本身正常（`getSpeed() == speedRate`）。

## 证据

诊断日志（临时加在测试内，已随修复保留为 `DIAG-FAIRY` 一行 info）：

```
失败回：
DIAG-FAIRY difficulty=NORMAL fairyAlive=true speedRate=0.2 span=50 shots=8 maxMoved=0.0
           anyTicked=false killedBeforeTick=8
           弹状态: [0: moved=0.0 removed=true anim=0 speed=0.20000000298023224] ... (全 8 发同样)

成功回：
DIAG-FAIRY difficulty=NORMAL fairyAlive=true speedRate=0.2 span=50 shots=8 maxMoved=1.0
           anyTicked=true killedBeforeTick=0
```

- `speed` 列恒为 `speedRate`（0.15〜0.3）→ **弹速链路正常**，`speedRate=0` 的 bug 不存在；
- `anim=0`（`getAnimationCount()`）且 `removed=true` → 弹在**第 1 tick 之前**就被移除；
- `killedBeforeTick == shots.size()` 与失败一一对应。

## 谁消的弹

`grep danmakuRemove`（主树）：

| 调用点 | 参数 | 触发者 |
|---|---|---|
| `THKaguyaLib.java:417`（符卡宣言 `checkSpellCardDeclaration`） | `danmakuRemove(user, 40, "Enemy", true)` | `SpellCardDeclarationTest` / `SpellCardDeclarationEdgeTest` 的 `stack.use(...)` |
| `EntitySpellCard.java:222 / 230`（符卡展开中每 tick） | `danmakuRemove(user, 40, "Other", true)` | 上述用例产出的符卡实体在后续 tick 持续扫 40 格 |
| `EntityDanmakuMob.java:810`（`moveDanmakuAttack`） | `danmakuRemove(this, 40, "Enemy", true)` | 妖精/幽霊等进入攻击模式时 |

`danmakuRemove` 的 `"Enemy"` 分支条件是 `!(entityTHShot.user instanceof Player)`，
`"Other"` 分支是 `entityTHShot.user != centerEntity` —— 本用例的弹属于**非 Player 的妖精**，
两条都命中。

GameTest 的结构是**网格排列**（`StructureGridSpawner`：结构宽 7 + 5 格列间距，行间距 6），
一个 batch 内相邻结构中心距离 ≈ 12 格，**40 格半径会覆盖同批次大量邻接结构**。
所以「本用例的弹被别的用例的符卡/妖精消掉」是常态，只是命中与否取决于批次顺序与结构落点
→ 表现为约 1/3 概率的偶发红。

## 与原假设（delay）的关系

`danmakuPattern` 的样式整型（`d`）在本移植里**不含 delay**：
`ShotData.shot(shotForm, shotColor, 0, 80, special)`（`EntityTHFairy.java:221`）——
`delay = 0`、`end = 80`，与 1.7.10 同（`ShotData.shot(int form, int color, int delay, int end, int special)`）。
因此「抽到 delay ≥ 6 的样式 → 弹在延迟期不动」**不成立**：所有样式的弹生成即实体化
（`getAnimationCount() == 0`，第 1 tick 就能动）。诊断日志里 `anim=0` 是「还没 tick」而非「延迟期」。

## 修复（测试侧，不改产品）

`FairySummonFireTest.directDanmakuPatternProducesMovingShot`：

1. **回归核心改为确定性断言**：生成瞬间对**每一发**弹断言 `getSpeed() > MIN_SHOT_SPEED`。
   不依赖任何 world tick，因此不受同批次弹消し影响；`speedRate` 被强制归 0 时这里必红
   （见 `variation.md` 的变异验证）。
2. **位移判定只对真正踏过 tick 的弹成立**：用 `helper.onEachTick` 逐 tick 记录
   `tickCount >= 1` 的弹的位移；窗口末尾若 `killedBeforeTick == shots.size()`
   （全弹在 tick 前被外部清除），则以第 1 步的弹速断言为准，不再用位移判负。

## 变异验证

把 `EntityTHFairy.java:112` 的 `speedRate = DanmakuPatternRegistry.speed.getOrDefault(...)`
强制改为 `0.0F`：

- `directdanmakupatternproducesmovingshot` **红**（生成瞬间弹速断言触发）；
- 同时 `summonlikepartialnbtkeepspatternstats` / `danmakumobpatternstatssurvivesmissingkey` /
  `summonedfairyshotsmove` 也红（它们本就断言 speedRate>0 / 弹速>0），共 4 例红；
- 复原后全绿。
