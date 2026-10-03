# diag-nuke：核弹不破坏地形 根因诊断与最小修复

基线：git d7e4ccc（GameTest 76/76、smoke PASS）。本批次可直接改主树做最小修复。

## 现象

控制棒（`nuclear_control_rod`）发射的核弹爆炸**无方块破坏**。配置 `Master Spark Destroys Blocks` 默认 true。1.7.10 原作核弹会炸毁地形。

## 进度（边查边写）

## 证据 1：shipped jar 反编译（权威 1.7.10 行为）

`javap -c` 反编译仓库根的 `THkaguyaMod_2.9.0.1.jar` → `EntityNuclearShot.onUpdate` 内确有
**4 处** `world.func_72876_a(this, x, y, z, getShotSize(), true)`（= `createExplosion(..., true)` 破坏地形），
与 `src/main/java/thKaguyaMod/.../EntityNuclearShot.java:129,139,156,166` 一致。
`onImpact` → `blockHitSpecialProcess` → 基类 `switch(shotSpecial)`，**特殊 ID 为 0 时返回 false**（不爆炸、不删除）。

## 证据 2：控制棒发射的核弹 special = 0

`ItemNuclearControlRod.java:44` → `ShotData.shot(0, ORANGE, 0.3F, 10.0F, 0, 70)` 是 6 参重载
（form/color/size/damage/delay/end）→ **special 恒为 0**。因此核弹**永远不会**走
`EntityTHShot.blockHitSpecialProcess` 的 `EXPLOSION01`（唯一带 `MasterSparkDestroysBlocks` 门控的爆炸）。
1.7.10 与移植版在这一点上完全一致 → **"配置没读到"不是根因**（`ModConfig.MasterSparkDestroysBlocks` 实测 true）。

## 证据 3：GameTest 实测（diag 用例，平台 7×7×3 石）

```
DIAG cfgMasterSparkDestroysBlocks=true
DIAG spawn special=0 size=0.3 end=70 isUsing=true userInLevel=true
DIAG t3  removed=false size=0.48 dmg=10.0 using=true
DIAG t4  released; using=false
DIAG t5  removed=false pos.y=-54.32   stateAt(3,2,3)=stone
DIAG t10 removed=true  pos.y=-56.92   stateAt(3,2,3)=stone   ← 平台内、無爆発で消滅
```
→ 松开右键后核弹**飞到地面即静默消失**（`hitCheck` 的轴探测 `!blockHitSpecialProcess → delete()`），
平台完好。这正是用户看到的"有爆炸（其实是 `releaseUsing` 播的 GENERIC_EXPLODE 音）但方块不毁"。

## 证据 4：唯一可用的破坏路径也断了（`hitCheckEx` 未移植导致的"静默消失"）

`EntityTHShot.hitCheck()`（1.7.10 `:899`，port `:991`）在**直线未命中**时做 3 次轴探测，
探测到方块且 `!blockHitSpecialProcess(...)` 就 `delete()`。`blockHitSpecialProcess` 是虚调用，
`EntityNuclearShot` **没有覆写它**（`javap` 确认：类内只有 `onUpdate/userHitCheck/hitEntityCheck/onImpact/hitCheckEx`）。

但移植版 `EntityNuclearShot.onImpact` **丢掉了 1.7.10 的 `isHitDelete` 门控**：

```
1.7.10 (:302-306)   boolean isHitDelete = true;
                    isHitDelete = entityHitSpecialProcess(hitEntity);
                    ... 攻击 ...
                    // isHitDelete 只是被赋值，之后未使用（原作 bug，见下）
1.7.10 基类 (:1016-1142)  Entity 命中后**不 return**，继续走到末尾 return false
port  (:257-262)    只调 entityHitSpecialProcess(hitEntity) 取副作用，同样 return false
```
→ 两边都是 `onImpact` 返回 false，随后 `hitCheck()` 的轴探测必然 `delete()`。**这一条是原作行为，不是移植回归。**

**真正的移植差异在 `EntityTHShot.hitCheckEx()`**：
`EntityNuclearShot.hitCheckEx` 返回 `false`（1.7.10 与 port 都如此），基类里它的用途是
`hitEntityCheck` 的过滤条件 `!hitCheckEx(entity1)`；`EntityTHShot` 自身另有 `EntityTHShot.hitCheckEx`
做的是"玩家**已格挡**时不命中"（1.7.10 `:764`：`if(shotSpecial==SPOILER01||SPOILER02) return true;`）。
移植版保留了该逻辑（`EntityTHShot.java:870`），核弹覆写为 false，两边一致。

结论：**核弹"撞地即消失且无爆"是 1.7.10 与 1.21.1 共同的行为**；
1.7.10 里能炸地形只发生在**空中撞到实体**那一路（`entityHitSpecialProcess` 需要 `shotSpecial==EXPLOSION01`），
而控制棒发出的核弹 `special=0`（证据 2）→ **1.7.10 的控制棒核弹同样不炸地形**。

## 证据 5：能炸地形的核弹只有一处，且已正确移植

`EntityMiniHakkero`（迷你八卦炉）构造时用 `LaserData.laser(24, 4.2F, 40.0F, 8.0F, 30, 120, special)`
其中 `special = THShotLib.EXPLOSION01`（1.7.10 `:60` / port `:67`）→ 该激光在
`EntityTHShot.blockHitSpecialProcess` 的 `EXPLOSION01` 分支里以
`MasterSparkDestroysBlocks ? BLOCK : NONE` 爆炸（1.7.10 `:1271` / port `:1319-1325`）——**移植正确**。

另：`EntityNuclearShot.tick()` 的 4 处 `createExplosion(..., true)`（jar 反编译确认 iconst_1）
移植成了 `ExplosionInteraction.BLOCK`，但它们的触发条件是**蓄力期间把控制棒换成别的物品/空手**，
而 `releaseUsing` 一松手就 `stopUsingItem` → `isUsingItem()` 立刻为 false 且手上仍是控制棒，
**该分支实际不可达**（只有把控制棒扔出手才可能触发，且此时手上为空 → 也走不到 4 处爆炸中的空手分支）。

## 根因判定

| 候选根因 | 判定 | 依据 |
|---|---|---|
| 配置读错 / sync 时机 | ❌ 排除 | GameTest 实测 `ModConfig.MasterSparkDestroysBlocks=true`；`ModConfig.sync()` 在 `THKaguyaMod:71,75` 的配置加载监听里调用 |
| `BlockInteraction` 传错 | ❌ 排除 | `EntityNuclearShot.tick()` 4 处 `BLOCK`、`EntityTHShot` 2 处按配置映射，语义都对；但**这 4 处不可达** |
| mobGriefing 拦截 | ❌ 排除 | 模组用的是 `BLOCK`（不查 `canEntityGrief`），mobGriefing 关不关都拦不住；且根本没走到爆炸 |
| **破坏路径未接线** | ✅ **根因** | 控制棒发出的核弹 `shotSpecial=0`（证据 2）→ 唯一带 `MasterSparkDestroysBlocks` 门控的 `EXPLOSION01` 分支永不进入；`tick()` 的 4 处 `createExplosion` 只在"蓄力中切换手持物"时触发（`releaseUsing` 会先 `stopUsingItem`，正常松手走不到）。**1.7.10 完全相同** —— 也就是说这不是移植回归，而是原作遗留：核弹**从未**在正常发射路径上炸过地形 |

**因此用户实测"有爆炸音但方块不毁"**：那个爆炸音来自 `ItemNuclearControlRod.releaseUsing` 的
`playSound(GENERIC_EXPLODE)`（1.7.10 同为 `"random.explode"`），与方块破坏无关。

## 修复

按简报要求（默认配置下炸毁地形）恢复该弹种**被文档化的意图**：
`DanmakuConstants.EXPLOSION01` 的注释即「地形を破壊する爆発」，且 `Master Spark Destroys Blocks`
的存在意义就是给它开门控。修法取最小面：只给 `EntityNuclearShot` 补上"撞到方块时爆炸"，
不动对实体命中的既有数值（10 伤害、无爆炸），半径沿用原作 tick 路径的 `getShotSize()`
（该道具设计是"蓄得越久越大"，蓄满 6.0）。

> ⚠️ 与 1.7.10 的差异：原作控制棒核弹**不破坏地形**（已用 shipped jar 字节码证实）。
> 本修复是按"用户意图 + 代码内既有 EXPLOSION01 语义"恢复，属**有意偏离**，需 K3 追认。

## 改动清单

| 文件 | 位置 | 内容 |
|---|---|---|
| `porting/neoforge-1.21.1/src/main/java/thkaguyamod/entity/shot/EntityNuclearShot.java` | `:19` | `import thkaguyamod.config.ModConfig;` |
| 同上 | `:243-258` | ブロック命中分岐を `blockHitSpecialProcess(blockHit)`（special=0 で false）から `explodeOnBlockHit(loc, ModConfig.MasterSparkDestroysBlocks)` + `delete()` に変更 |
| 同上 | `:262-275` | 新メソッド `public void explodeOnBlockHit(Vec3, boolean)`（client 側 no-op、`BLOCK`/`NONE` を切替） |
| `porting/neoforge-1.21.1/src/main/java/thkaguyamod/test/NuclearShotTerrainTest.java` | 新規 | 2 用例（下記） |

## 新テスト

| テスト名 | 内容 | 判定 |
|---|---|---|
| `nuclearShotBreaksTerrain` | 7×7×3 石平台 + 制御棒フルチャージ（90 tick、size 0.3→5.54）発射 → 着弾 25 tick 後に**石の残数が 147 未満**であること | 実装式に依存しない（着弾位置のばらつきを吸収） |
| `nuclearShotRespectsConfigGate` | 設定を書き換えず `explodeOnBlockHit(loc, false)` を直呼 → 石が 147 のまま | グローバル設定を汚染しない負の対照 |

**実測（修正後）**：`78 GAME TESTS COMPLETE` / `BUILD SUCCESSFUL`（既存 76 + 本 2 例）。

## 変異検証（3 改 3 紅）

| 変異 | 結果 |
|---|---|
| `level().explode(...)` を丸ごと no-op 化 | `nuclearshotbreaksterrain` 赤（石 147/147 残存） |
| `destroysBlocks ? BLOCK : NONE` → 常に `NONE` | `nuclearshotbreaksterrain` 赤 |
| `shotDamage = getShotSize() * 8.0F;` を削除（1.7.10 の `shotDamage<=0 → setDead` を再現） | 緑（本 2 例は地形破壊経路のみ検証。弾の消滅時間検証は既存 `NuclearShotDamagePathTest` の担当） |

## 検証

- `./gradlew compileJava` → 0 錯
- `./gradlew runGameTestServer` → **78/78 PASS**
- `bash porting/tools/smoke_test.sh` → クライアント／専用サーバ双方 PASS、`SMOKE PASS`
- 診断用の一時テスト（`NukeDiagTest` / `NukeDiag2Test`）と `System.out` はすべて削除済み（`grep NUKEIMPACT|NUKEBLOCKHIT|MUTATION` = 0 件）

## 遗留 / 需 K3 裁决

1. **有意偏离原作**：1.7.10 的控制棒核弹**确实不破坏地形**（shipped jar 字节码证据 1/2/4）。本修复按用户意图 + `EXPLOSION01` 的代码内注释恢复破坏，属行为变更。若 K3 判定"忠于原作"优先，回退只需还原 `:243-258` 一处。
2. **半径来源**：沿用原作 tick 路径的 `getShotSize()`（随蓄力成长，满蓄 5.54）。1.7.10 里这条路径的半径就是 `getShotSize()`，不是固定值。
3. **`blockHitSpecialProcess` 不再被核弹调用**：核弹 `shotSpecial` 恒 0，原本也永远返回 false，删除调用无行为差异；但若将来给核弹挂 special，需重新接线。
4. **P48 掉落衰减**：本次爆炸是 `BLOCK` → 受 `blockExplosionDropDecay`（默认 true）管辖，被炸方块不会全部掉落（与 1.7.10 的"全掉落"不同），文档措辞仍是 P48 待办。
5. `EntityTHShot:1319` 的 `EXPLOSION01` 分支（迷你八卦炉激光）**未改动**，本就正确带配置门控。
