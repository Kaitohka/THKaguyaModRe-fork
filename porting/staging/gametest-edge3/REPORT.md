# gametest-edge3 批次报告（I8 剩余命中扫描：SetLaser / NuclearShot / DanmakuMob；HenyoriLaser 已移除）

范围：为 I8 修复中尚未被 GameTest 覆盖的命中扫描补回归用例。
本批最终只写 `porting/staging/gametest-edge3/`（3 个测试类 + 本报告）；
主树 `test/` 下同名文件为 K3 合并后的同内容副本，**产品文件未做任何修改**。

包名 = 最终包名 `thkaguyamod.test`，目录 `thkaguyamod/test/`（合并即拷贝）。
约定沿用主树 `test/DanmakuDamagePathTest.java`：`@GameTestHolder` + `@PrefixGameTestTemplate(false)`
+ `@GameTest(template = "empty")`，靶一律用火免疫的 `ZombifiedPiglin`（日照ダメージの偽陽性回避）。

---

## 1. 最终交付清单

| 文件（`porting/staging/gametest-edge3/thkaguyamod/test/`） | 行数 | 覆盖的修正点 | 一句话 |
|---|---|---|---|
| `SetLaserDamagePathTest.java` | 113 | `EntityTHSetLaser:152` | 固定レーザーの判定線分上の ZombifiedPiglin が `hitCheck()` 1 回で掉血、設置台（= source）は無傷（負の対照） |
| `NuclearShotDamagePathTest.java` | 125 | `EntityNuclearShot:175` | 核弾の命中が対象を掉血させる（副作用で判定）、線分上の「狙えない弾」は検出されない（負の対照） |
| `DanmakuMobContactDamageTest.java` | 130 | `EntityDanmakuMob:1219` | 弾幕モブの自前接触扫描が対象を拾い `hitCheck(damage)` で掉血、狙えない弾は拾わない（負の対照） |

合計 **4 個 `@GameTest`**（SetLaser 1 / Nuclear 1 / MobContact 2）。
`HenyoriLaserDamagePathTest.java` は**削除**（理由は §3）。

## 2. 断言要点と設計判断

### SetLaserDamagePathTest
- 設置レーザーは `setting` に追従して毎 tick `setPos` し、`setting` が死ぬと自己消滅する。
  **tick を使うと命中 tick が環境でぶれる**ため、`new EntityTHSetLaser(...)`（設置台 = ArmorStand を
  `set` に渡す）→ `updateAngle()` → `hitCheck()` を**直接 1 回**呼ぶ設計。
- 判定線分は実装式どおり yaw=90° から `(x+width → x+length-width)` の水平線分。
  対象は線分中央（設置台 +1.7）に置くので始点が膨張判定箱の外になり `clip` が成立する。
- **負の対照**：設置台（= `source`）が傷つかないことを同時に固定（`!entity1.is(source)` 分岐）。

### NuclearShotDamagePathTest（**実装事実を 1 つ発見**）
- **戻り値では判定できない**：`EntityNuclearShot#hitEntityCheck` は 1.7.10 と同じく、
  ループ内で見つけた相手にその場で `onImpact(...)` を呼び、**最後に受け取った `hitResult` を
  そのまま返す**（見つけた相手を返さない）。初版は戻り値で判定していたため
  `result=null` で落ちた。本版は「対象が実際に掉血したか」という**副作用**で判定する。
  これは移植版の仕様（1.7.10 も同じ構造）であり、製品側の欠陥ではない。
- 座標はハードコードせず、走査箱 `getBoundingBox().expandTowards(motion).inflate(size*0.5)` から
  対象位置と判定線分を式で導く（実装式に追従）。
- 負の対照は「線分上に狙えない弾（遅延フェーズで `isPickable()==false`）だけを置く」方式で、
  弾同士の相殺（`shotDamage` 減算）が起きないことを固定。
- **爆発・地形破壊との隔離**：`tick()` は核制御棒が無いと `ExplosionInteraction.BLOCK` の爆発を
  起こすため、**核弾を世界に追加せず `tick()` を一度も走らせない**。構造内に石を置き、
  テスト後も残っていること（＝爆発していない）を確認。

### DanmakuMobContactDamageTest
- **先に実装を読んで语义を確定**：`EntityDanmakuMob#hitEntityCheck` は使用者の概念を持たない
  **接触扫描**（`canBeHitByProjectile()` な实体のうち移動線分に最も近い 1 体を返す）で、
  `hitCheck(damage)` → `onImpact(...)` が `mobAttack(this)` で `hurt(damage)` する。
  つまり「消弹」ではなく**接触ダメージ／体当たり判定**。
- **重要**：`hitCheck(float)` は移植ツリー内に呼び出し元が無い（1.7.10 でも無かった）＝
  サブクラス実装待ちの死にコード。テストは「呼ばれたときに正しく動くか」を固定する位置づけ。
- 幾何：モブ中心から +1.35 格（扫描箱 `bb+0.9` に入り、対象の膨張箱の外になる帯 `1.2 < dx ≤ 1.5`）。
- 2 つ目は純粋な負の対照（狙えない弾だけを置くと `hitEntityCheck` が `null`）。

## 3. HenyoriLaser 用例を削除した理由（降级）

`EntityTHHenyoriLaser` は **`ModEntities` に登録が無く、生成元も未移植**
（1.7.10 でも `ItemHoutou` の使用箇所がコメントアウト）＝**現状インスタンス化不能な死コード**。
テスト側で `EntityType.Builder.build("")` を作る回避策を試したが、GameTest の実行時には
**レジストリが凍結済み**で `Registry is already frozen` で失敗する（実走で確認）。

- 恒久対処（実体を登録する／テスト専用の登録経路を用意する）は移植判断の範疇なので、
  本批では**用例ごと削除**した（`@GameTest` 1 件減 → 全 69 件のうち 68 件 + 本批 4 件）。
- 代償：`EntityTHHenyoriLaser:126` の `canBeHitByProjectile` 修正点は**未カバー**のまま。
  同クラスの `hitCheck` は 13 セグメントの蛇行線分を回す固有実装なので、
  将来このクラスが登録された時点で（`empty` 構造内で完結する形で）追加すべき。

## 4. 検証結果

### 4.1 实走（授权范围内）

```
cd porting/neoforge-1.21.1 && JAVA_HOME="/c/Program Files/Java/zulu-21" ./gradlew runGameTestServer
```

| 回 | 结果 |
|---|---|
| 初回（合并後） | 69 件中 2 件失败（henyori=Registry frozen / nuclear=result=null） |
| 修正 1（henyori 削除 + nuclear 走査箱から座標導出） | 68 件中 1 件失败（nuclear 仍在） |
| 修正 2（nuclear を副作用判定へ変更） | **69 件全绿：`All 69 required tests passed :)`** |
| 最终确认（产品文件恢复后） | **69 件全绿**（同上） |

### 4.2 变异验证（实走）

3 处产品过滤器を一時的に旧 API へ戻して実走：

| 変异 | 结果 |
|---|---|
| `EntityTHSetLaser.java:152` → `canBeCollidedWith()` | `setlaserdamagesmobinsegment` **赤** |
| `EntityNuclearShot.java:175` → `canBeCollidedWith()` | `nuclearshotdamageslivingtarget` **赤** |
| `EntityDanmakuMob.java:1219` → `canBeCollidedWith()` | `contactscanpickspickablelivingtarget` **赤** |

→ 3 例とも I8 の修正内容そのものを検出している（他の 66 件は緑のまま）。
変异は復元済みで、3 ファイルとも変异前とバイト一致（`diff` 確認済み）。
負の対照側の用例（`nuclearshotignoresunpickableshot` / `contactscanignoresunpickableshot`）は
変异時も緑のまま＝「対照の向きが逆」である設計どおりの挙動。

## 5. 未决 / K3 判断待ち

1. **HenyoriLaser の修正点は未カバー**（§3）。登録 or 削除の判断後にテストを追加するか、
   修正点リストから外すか。
2. **DanmakuMob の `hitCheck(float)` は死にコード**（呼び出し元ゼロ、1.7.10 由来）。
   テストは通るがゲーム内では発火しない。サブクラスで接続するか削除するかは K3 裁定。
   接続する場合は `attackInterval` 無敵期間との関係も決める必要がある。
3. **NuclearShot の爆発経路（`ExplosionInteraction.BLOCK`）は未検証**：本テストは `tick()` を
   走らせないため。専用の隔離構造か、`ModConfig.MasterSparkDestroysBlocks` を切った実行が必要。
4. 前批からの継続：`tools/staging_blocked.txt` と `items3` の重複で `compile_staging.sh` が
   假失败を返し続けている（本批の沙盒コンパイルは `gameTestServerLegacyClasspath.txt` で回避）。
