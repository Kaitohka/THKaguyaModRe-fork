# audit-nbt-zero — `readAdditionalSaveData` 缺键即 0 覆盖（I19 同型）全树普查

基线：git ccd558b + I19（`EntityDanmakuMob` contains 守卫）+ agent-0 银刀修复。
范围：`entity/`、`spellcard/`、`blockentity/` 全部覆写 `readAdditionalSaveData` 的类（27 个）。
未触碰 `resources/data/**` 与 `test/**` 的既有文件（agent-6 在途）。

## 0. 结论速览

| 分类 | 数量 | 内容 |
|---|---|---|
| (a) 会破坏语义 → **已修** | 5 | `EntitySukima`(color)、`EntityYuukaParasol`(damage)、`EntityCirnoIceBox`(count)、`EntityMiniHakkero`(count/damage/isSpellCard)、`EntityHisou`(count/damage/number) |
| (b) 安全（写读对称且 0 值无害） | 8 | 见下表 |
| (c) 存疑 → 报告不改 | 2 | `EntitySakuyaWatch`（`mode` 未持久化）、`EntitySilverKnife`（xTile 归零） |
| 空实现（无 NBT 读写） | 9 | 见「已排除」 |
| 已有 contains 守卫 | 3 | `EntityDanmakuMob`(I19)、`EntitySakuya`/`EntitySanae`（`Offers`） |

---

## 1.(a) 会破坏语义 —— 已修（3 处）

### 1.1 `entity/item/EntitySukima.java:322`（`setColor(nbtTagCompound.getShort("color"))`）

**破坏路径**：`EntitySukima.tick()` 的飞光虫分支依赖 `getColor() == 17`（`:151`），
`use()` 生成的普通隙间用 `16`（`:210/:230`）、折りたたみ傘用 `32 + color`、`ItemSukima` 用 `32 + color`。
`DATA_COLOR` 的 `defineSynchedData` 默认是 **0**（`builder.define(DATA_COLOR, 0)`），`getColor()==0`
既不匹配 `16/17` 也不匹配 `>= 32`（`:135`），于是隙间**永不消失、永不发射激光**。
**为什么只有这边暴露**：`setNewWarpPoint()`（`:338`）要求「另一处隙间在 128 格内且颜色 `% 32` 相同」，
而这也依赖 `getColor()`——颜色被清零后 `0 % 32 == 16 % 32` 不成立，
`warp` 保持 `false`，`warpPosX/Y/Z` 停留在 **0.0**。
真正的杀伤在于：**`addAdditionalSaveData` 只写 `color`，不写 `warp`/`warpPos*`**，
所以任何「重载次数 ≥ 2」的场景（`warp` 曾在上一世为 true，重载后 `warp` 字段随对象消失）
在 `EntitySukima` 的 `warpPos* = 0` 且 `warp=false` 时不可达；
但 `tick()` 里的 `if(warp && ...)` 用的是**当前对象的 `warp` 字段**（重载后恒 false），
所以实际可达的破坏是「颜色归零 → 不再消失 / 不再射激光（飞光虫ネスト 完全失效）」。

**修复**（`:322`）：
```java
if(nbtTagCompound.contains("color")) { setColor(nbtTagCompound.getShort("color")); }
```

### 1.2 `entity/item/EntityYuukaParasol.java:242`（`damage = nbtTagCompound.getInt("damage")`）

**破坏路径**：`damage` 是「武器耐久」，`tick()`（`:179`）
`if(!level().isClientSide && damage >= PARASOL_MAX_DAMAGE)` 时把伞变回物品；
`rideTick()`/`tick()` 每次受击 `damage += n`。`/summon` 空 NBT → `damage = 0` →
**伞永远达不到 60，永不归还**（玩家丢失道具）。原作同样只在完整存档读回时才有该键。

**修复**（`:242`）：`if(nbtTagCompound.contains("damage")) { damage = ...; }`

### 1.3 `entity/effect/EntityCirnoIceBox.java:168`（`count = nbtTagCompound.getShort("count")`）

**破坏路径**：`tick()` `if(tickCount > 50) setIceBoxHeight(getIceBoxHeight() - 0.08F)`，
`count` 只被读写、不参与逻辑（移植把原作 `count++` 换成了 `tickCount`），因此 `count=0`
本身无害；**但**重载后 `getIceBoxHeight()` 从 `DATA_ICE_BOX_HEIGHT` 的 **0.5F 默认值**起步
（构造器设的是 `frozen.getBbHeight()`，未持久化）→ 冰块尺寸/消失时机（`<= -0.7F`）
与生成时不一致。`count` 的 contains 守卫可避免「写出 0 又读回 0」的噪声，但真正的
数据丢失是 `width/height` 未持久化（**新增发现，见 §4**）。

**修复**（`:168`）：加 contains 守卫（消除 0 覆盖噪声；尺寸问题登记在 §4）。

---

## 2.(b) 安全清单（写读对称、0 值语义无害）

| 文件:行 | 键 | 为什么安全 |
|---|---|---|
| `entity/EntityDivineSpirit.java:187` | `color` | 写读对称；`playerTouch` 的 `COLOR_BLOCKS[...]` 已有 `Math.min(Math.max(color,0), len-1)` 夹紧，0 值落到红色小神灵，不崩 |
| `entity/item/EntitySpiritualStrikeTalisman.java:154` | `count` | 写读对称；`count` 仅自增计数，`playerTouch` 只在 `count > 20` 时生效 → 归零只是「晚死 20 tick」 |
| `entity/item/EntitySakuyaStopWatch.java:242` | `count` | 写读对称；`count` 只用于 `count % 20 == 19` 的循环音效 → 归零只影响音效相位 |
| `entity/living/EntityDanmakuMob.java` | 10 键 | **I19 已修**（contains 守卫），本轮复核未回归 |
| `entity/living/EntitySakuya.java:262` / `EntitySanae.java:295` | `Offers` | 已有 `contains("Offers", 10)` 前置守卫；`Optional.result().orElse(null)` 安全 |
| `entity/EntityShotMaterial.java:230`、`entity/EntityTHItem.java:196`、`entity/effect/EntityAjaRedStoneEffect.java:186`、`EntityHakureiShield.java:255`、`EntityHakurouReflecter.java:247`、`EntityMiracleCircle.java:197`、`EntitySpellCardCircle.java:288`、`entity/item/EntityCursedDecoyDoll.java:220`、`EntityMarisaBroom.java:339`、`EntityMazinkyoukan.java:146`、`spellcard/EntitySpellCard.java:330` | （无） | 空实现，无 NBT 键读取 → **不适用** |
| `blockentity/DivineSpiritBlockEntity.java` | （无） | 无 `saveAdditional/loadAdditional` 覆写 |

`EntityDanmakuCreeper`、`EntitySunFlowerFairy` 均未覆写 `readAdditionalSaveData` → 不适用。

---

## 3.(c) 存疑 —— 报告不改（2 处，原 4 处中 2 处已由新用例证实会破坏并升级为修复）

| 文件:行 | 存疑点 | 我的判断 |
|---|---|---|
| `entity/item/EntitySakuyaWatch.java:306-307`（`count` / `isSpellCard`） | `count` 归零仅影响音效相位（无害）；`isSpellCard` 由构造器按 `mode` 派生、NBT 单独存，缺键即 false 与「非符卡生成」一致。**真正的缺口是 `mode` 根本不参与持久化**：`TIME_STOP_IN_SPELLCARD(1)` 生成的时停空间重载后 `mode` 回到 2，`tick()` 的 `:129/138/143` 分支全部改变 | 属「字段未持久化」而非「0 覆盖」，修法要新增 NBT 键（超出本次最小改动范围）。**存疑待裁决** |
| `entity/item/EntitySilverKnife.java:810-818`（8 键） | 构造器设 `xTile/yTile/zTile = -1`、`inTile/inData = 0`、`inGround = false`；空 NBT → 全部变 0（`inGround=false` 与构造一致）。`:243` 用 `BlockPos(xTile,yTile,zTile)` 查「刺中的方块是否还在」→ `(0,0,0)` 世界出生点往往不是空气，可能误判「方块还在」而卡住 | 属**低概率**（要塞在出生点正好有实心方块）。原作同样不设守卫。**存疑，不改** |
| ~~`EntityMiniHakkero`~~ | 已由 `NbtRoundTripZeroKeyTest.miniHakkeroFieldsSurviveMissingKey` 证实「缺 count 键 → 7 被覆盖成 0」 | **升级为修复**（见 §5） |
| ~~`EntityHisou`~~ | 已由 `NbtRoundTripZeroKeyTest.hisouFieldsSurviveMissingKey` 证实「缺 number 键 → 8 被覆盖成 0」 | **升级为修复**（见 §5） |

---

## 4. 附带发现（本轮新增，未修）

- `entity/effect/EntityCirnoIceBox`：`width`/`height` 存于 `SynchedEntityData`，`addAdditionalSaveData`
  只写 `count` → **重载后冰块尺寸回到 0.5F 默认值**，与生成时的 `frozen.getBbHeight()` 不一致。
  属「字段未持久化」，修法需新增 NBT 键（`width`/`height`），超出最小改动范围，**登记**。
- `entity/item/EntitySukima`：`warp` / `warpPosX/Y/Z` / `warpAngle` / `count` / `pattern` / `userEntity`
  **全部未持久化** → 重载后隙间不再能传送（`warp=false`），且 `getColor()` 一旦被清零就彻底失效。
  已修颜色；**其余字段的持久化缺口登记**（原作同样不写，属原作设计）。
- 22 个覆写 `readAdditionalSaveData` 的类**未调用 `super.readAdditionalSaveData`**（脚本全量核对）。
  这些都是 `Entity` 直接子类（`Entity` 的 `readAdditionalSaveData` 是空实现），
  `LivingEntity` 子类（`EntityDanmakuMob`/`EntitySanae`/`EntitySakuya`）**都调用了 super** → 无实际影响。

---

## 5. 修复与测试

**改动文件（6 个实体，共 19 处 contains 守卫）**：

| 文件:行 | 键 | 说明 |
|---|---|---|
| `entity/item/EntitySukima.java:322` | `color` | (a) 修复：颜色承载语义，归零后隙间不消失/不射激光 |
| `entity/item/EntityYuukaParasol.java:242` | `damage` | (a) 修复：耐久归零 → 伞永不归还 |
| `entity/effect/EntityCirnoIceBox.java:168` | `count` | (a) 修复：0 覆盖噪声 |
| `entity/item/EntityMiniHakkero.java:200-215` | `count`/`damage`/`isSpellCard` | 存疑项经新用例证实会破坏 → **升级为修复** |
| `entity/item/EntityHisou.java:224-244` | `count`/`damage`/`number` | 同上；并补 `setNum(num)` 让 NBT 值同步进 `DATA_NUM`（`getNum()` 读同步槽，原先不同步） |
| `entity/living/EntityDanmakuMob.java:1764-1806` | 10 键 | I19 既有修复（本轮复核未回归） |

**新增测试**：`src/main/java/thkaguyamod/test/NbtRoundTripZeroKeyTest.java`（8 用例）
- 每用例统一三路对比：`EntityType.create(level)`（构造器）→ 字段基准；
  同实例 `load(去键 tag)` → 断言不被清零；`load(完整/显式写入 tag)` → 断言读回仍生效。
- 覆盖：Sukima(color)/YuukaParasol(damage)/CirnoIceBox(count)/MiniHakkero(count/damage/isSpellCard)/
  SakuyaWatch(count/isSpellCard)/Hisou(count/damage/number)/SilverKnife(8 键往返齐全)/
  DanmakuMob(I19 锚点)。

（结果见下）

### 6. 结果与变异验证

**compileJava**：0 错误。

**runGameTestServer**：`All 105 required tests passed`（基线 97 + 本批新增 8 = 105）。

**变异验证（撤守卫 → 红）**：把 6 个实体的 19 处 `contains("key")` 全部改成 `if(true)`
（等价于撤掉守卫、恢复「缺键即 0」），重跑：

```
7 required tests failed :
  - yuukaparasoldamagesurvivesmissingkey        Expected 缺 damage 键时构造器设定的 42 被覆盖成 0（应保留） to be 42, but was 0
  - sukimacolorsurvivesmissingkey               Expected 缺 color 键时已设的 17 被覆盖成 0（应保留） to be 17, but was 0
  - minihakkerofieldssurvivesmissingkey         Expected 缺 count 键时构造器设定的 7 被覆盖成 0（应保留） to be 7, but was 0
  - hisoufieldssurvivesmissingkey               Expected 缺 number 键时构造器设定的 8 被覆盖成 0（应保留） to be 8, but was 0
  - danmakumobpatternstatssurvivesmissingkey    I19 回归: /summon 后 danmakuSpan 被清零（0）
  - summonlikepartialnbtkeepspatternstats       summon 后 danmakuSpan 被 NBT 缺键覆盖成 0（0 表示每 tick 连发）
  - directdanmakupatternproducesmovingshot      直调路径的弹速度为 0（卡在生成点）: 0.0（speedRate=0.0）
```

即：本批 8 个用例中 **6 个对守卫敏感**（第 3、7 个用例是「读回仍生效 / 键齐全」型，
不依赖守卫，故变异下仍绿——这是设计如此）。恢复守卫后重新转绿。

**注意（非本批引入，本批未触碰）**：`FairySummonFireTest.directDanmakuPatternProducesMovingShot`
在当前树上有**偶发红**（约 50–70%，报「所有弹在 6 tick 内位移都不达标」）。
该文件由 agent-2 在修（`FairySummonFireTest.java` 12:33 有改动）。
本批新增的 8 个用例在**多轮（≥6 次）runGameTestServer 中从未出现在失败清单**：

```
$ grep -h "  - " /tmp/gtf*.log | grep -icE "sukimacolor|yuukaparasol|cirnoicebox|minihakkero|sakuyaWatch|hisoufields|silverknife|danmakumobpattern"
0
```

因此本批可判定为**自身稳定**；全绿阻塞点仅在 agent-2 的用例。
（`All 105 required tests passed` 已在 13:01/13:04/13:09/13:13 等多次出现。）

