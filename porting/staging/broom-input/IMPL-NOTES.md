# broom-input 实现草稿（Phase 1）— 接线与验证说明

本目录是**草稿**，不参与编译：`porting/staging/broom-input/impl/` 下所有内容都未进入主树。
设计与依据见同目录上级的 `../DESIGN.md`（1.7.10 行为摘要、通道对比、选型结论）。

---

## 1. 产出物清单

| 文件（相对 `porting/staging/broom-input/impl/`） | 类型 | 最终路径 | 行数 | 说明 |
|---|---|---|---|---|
| `thkaguyamod/entity/item/BroomSteer.java` | 新 | 同左 | 311 | 操舵演算の純関数。原作 395-687 の数値・分岐を関数化。速度上限と無人接地補正のみ意図的に修正（`// TODO K3-review` 付き） |
| `thkaguyamod/client/input/BroomSteerClient.java` | 新 | 同左 | 89 | `ClientTickEvent.Pre` で `Options.keyUp/Down/Left/Right/Jump` を読み、5 bit を騎乗中の箒へ書き込む |
| `thkaguyamod/entity/item/EntityMarisaBroom.java` | 改（**完全ファイル**） | 同左 | 425 | `getControllingPassenger()` 追加／`steerMask` 追加／`tick()` を「制御側だけ積分」へ再構成（主樹 `159ede0` の I13 を吸収済み） |
| `EntityMarisaBroom.java.patch` | 改（unified diff，`-p1`） | — | 268 | 上記作業を主樹 **`159ede0`** に当てた差分。`patch` で適用検証済み |
| `THKaguyaClient.java.patch` | 改（unified diff，`-p1`） | — | 33 | import 2 行 + コンストラクタ 1 行（`NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, BroomSteerClient::onClientTickPre)`） |
| `thkaguyamod/test/BroomSteerTest.java` | 新 | 同左 | 353 | GameTest 11 件。純関数 9 件 + 制御権 1 件 + 実体結合 1 件 |

作業ファイルは「最終パッケージ名＝そのままコピーできる形」で置いてある（`thkaguyamod/...`）。
`EntityMarisaBroom.java` は**完全ファイル**と**パッチ**の両方を置いた。どちらか一方だけを使うこと
（両方やると二重適用になる）。パッチは次の 2 通りで検証済み:

```bash
# 完全ファイル方式（推奨）
cp -r porting/staging/broom-input/impl/thkaguyamod/. porting/neoforge-1.21.1/src/main/java/thkaguyamod/
# パッチ方式（リポジトリルート基準 -p1）
patch -p1 -d porting/neoforge-1.21.1 < porting/staging/broom-input/impl/EntityMarisaBroom.java.patch
patch -p1 -d porting/neoforge-1.21.1 < porting/staging/broom-input/impl/THKaguyaClient.java.patch
```

> `EntityMarisaBroom.java.patch` は**主樹 HEAD 版に対する差分**である。主樹は他批次が触っているため、
> `patch --dry-run` で当たることを確認してから本適用すること（後述 §4 の無人減速修復と競合し得る）。

---

## 2. 接线顺序（合并手順）

1. `impl/thkaguyamod/entity/item/BroomSteer.java` → `.../thkaguyamod/entity/item/BroomSteer.java`（新規）。
2. `impl/thkaguyamod/client/input/BroomSteerClient.java` → `.../thkaguyamod/client/input/BroomSteerClient.java`（新規、ディレクトリも新規）。
3. `impl/thkaguyamod/entity/item/EntityMarisaBroom.java` を主樹の同パスへ上書き（**先に §3 を読むこと**）。
4. `THKaguyaClient.java.patch` を適用（import 2 行 + リスナー 1 行）。
5. `impl/thkaguyamod/test/BroomSteerTest.java` → `.../thkaguyamod/test/BroomSteerTest.java`（新規）。
   テストは `@GameTestHolder` で自動登録されるので追加の結線は不要。

新規クラスはいずれも新しい依存を持たない（`BroomSteer` は `Vec3` のみ、`BroomSteerClient` は
`thkaguyamod.client.input` 内で client-only クラスだけを参照 → R3 ゲート（common から client を import しない）に抵触しない）。

---

## 3. 主樹の無人ブロックとの統合（**回退禁止**）

### 3.1 統合の時点（2026-10-03 時点で再確認）

| 項目 | 値 |
|---|---|
| 主樹 HEAD（本節の基準） | **`159ede0`** "phase5 follow-up: playtest round 3 fixes (I13 broom jitter, I14 pose crawl)" |
| 主樹 `EntityMarisaBroom.java` | 422 行（`getControllingPassenger` 無し・`steerMask` 無し・騎乗/無人を**両側**で積分） |
| 本批 `impl/thkaguyamod/entity/item/EntityMarisaBroom.java` | 428 行（＋制御端分岐・＋`steerMask`・＋`getControllingPassenger`） |
| 差分パッチ | `EntityMarisaBroom.java.patch`（`-p1`、**159ede0 に対して適用検証済み**） |

### 3.2 I9 → I13 の変遷と本批の対応

1. **I9**（主樹 `2dcac4a` 以前）: 無人ブロックがローカル変数にしか書かれておらず
   「下骑不停」だった → 騎乗 if の else へ移動し `setDeltaMovement` に反映。
2. **I13**（主樹 `159ede0`）: 接地中に `motionY += 0.06D`（原作 683-686）を残すと、
   1.21.1 の `Entity#move()` が着地時に垂直成分を 0 にしないため
   **2 tick 周期の上下動（実測 振幅 0.0141 ブロック）と運動量パケットの連続発火**が起きる。
   → 接地中は `motionY = -0.03` に固定する（`0` にすると衝突判定が動きを報告せず
   `onGround` が落ちて 1 tick おきの振動に変わる、という実測結論つき）。

**本批は I13 をそのまま吸収している**（`motionY += 0.06` は存在しない）。対応関係:

| 主樹 `159ede0` の現行コード | 本草稿での位置 |
|---|---|
| 無人の `motionX *= 0.9` / `motionZ *= 0.9` | `BroomSteer.stepEmpty(...)`（原作 672/674 のまま、`EMPTY_DRAG`） |
| 無人の俯仰 1°/tick 減衰 | `BroomSteer.decayEmptyPitch(...)`（値は同じ、`float` を返す純関数化） |
| 接地中 `motionY = -0.03`（I13） | `BroomSteer.EMPTY_GROUNDED_VERTICAL`（= `-EMPTY_SINK`）、`stepEmpty` 内で `onGround ? 固定 : motion.y - EMPTY_SINK` |
| 空中 `motionY -= 0.03` | `stepEmpty` の `motion.y - EMPTY_SINK` |
| 「`move()` より前に移動量へ反映」のコメントと意図 | 本草稿は `setDeltaMovement(stepEmpty(...))` を `move()` の**前**に置いており同義 |
| 入水 `*0.5` が無人の分岐にも掛かっていた | `stepEmpty` の末尾に同じ `WATER_FACTOR` を入れて保存（原作 555-560 は両分岐共通） |

**`BroomSteerTest` も同調済み**: 旧 `emptyBroomLiftsWhenOnGround`（+0.06 を期待）は削除し、
代わりに `emptyBroomGroundedVerticalIsFixed`（① 入力の垂直成分に依らず `-0.03` に固定、
② damp 後も tick ごとに変化しない＝運動量パケットが出ない条件、③ 空中は従来どおり `-0.03`）
を追加。期待値は §4.1 の実測表に記載。

**差分（意図的なもの）は「どの側で走るか」だけ**:

- 主樹: 乗員がいれば誰でも（両側で）積分、いなければ両側で積分 → 二重積分＋通信路が閉じたまま。
- 本草稿: `isControlledByLocalInstance()` が **true の側だけ**積分し、他方は `setDeltaMovement(ZERO)`。
  無人側の式は I13 のまま不変なので、「無人箒が沈んで静止する」挙動は**保存される**
  （GameTest `mountedBroomMovesServerSide` がサーバー側で沈むことを固定している）。

**統合時の注意**: `EntityMarisaBroom.java.patch` は `159ede0` に当たることを `patch` で検証済みだが、
主樹がさらに動いた場合は目視マージに切り替え、下記の 3 点だけ手で反映すれば十分:
1. `getControllingPassenger()` の追加、
2. `steerMask` フィールド + `setSteerMask`/`getSteerMask`、
3. `tick()` の移動ブロックを `if (isControlledByLocalInstance()) { ...積分... } else { setDeltaMovement(ZERO); }` で囲み、
   中の計算を `BroomSteer.step/stepEmpty` に置き換える（**主樹の I13 の `motionY = -0.03` は
   `BroomSteer.stepEmpty` に取り込み済みなので、置き換えても失われない**）。

---

## 4. 编译验证（等价 javac 沙盒／未跑 gradlew）

`staging/gametest-edge/REPORT.md` §2.2 と同じ組み立て（`neoforge-21.1.252-merged.jar` +
moddev の legacy classpath，路径经 `cygpath -w` 归一化、源文件清单用 Windows 绝对路径）：

> 注意：本批第一次编译时 `build/moddev/clientLegacyClasspath.txt` 已不存在
> （gradle 运行配置名变动后只剩 `clientSmoke*` / `gameTestServer*` / `serverSmoke*` 三份）。
> 改用 `gameTestServerLegacyClasspath.txt`（含 GameTest 类，本批测试需要），结果同样 0 错。

```bash
W=/tmp/bi-check
cp -r porting/neoforge-1.21.1/src/main/java/. $W/src/main/java/           # 主树（当前 264 文件）
cp -r porting/staging/broom-input/impl/thkaguyamod/. $W/src/main/java/thkaguyamod/   # 本批 3 新文件
patch -p1 -d $W < porting/staging/broom-input/impl/THKaguyaClient.java.patch         # +1 行接线
# 源文件清单必须是 Windows 绝对路径（cygpath -w），否则 Windows 版 javac 会把 /tmp/... 当相对路径
find $W/src/main/java -name '*.java' | while IFS= read -r p; do cygpath -w "$p"; done > $W/files.txt
javac -nowarn -proc:none -encoding UTF-8 -cp "$MERGED;$LEGACY" -d "$(cygpath -w $W/out)" @"$(cygpath -w $W/files.txt)"
```

**结果：`javac exit: 0`，267 个源文件（主树 264 + 本批 3），仅 `deprecation` / `unchecked` 两条 note
（与主树基线同源，非本批引入）。**
产出确认：`thkaguyamod/entity/item/BroomSteer.class`、`EntityMarisaBroom.class`、
`thkaguyamod/client/input/BroomSteerClient.class`、`thkaguyamod/test/BroomSteerTest.class`
（主树自带的 `BroomPhysicsTest.class` 也一并编译通过）。

未做的验证（铁律禁止 / 超出批次）:
- **未跑 `./gradlew runGameTestServer`** → `BroomSteerTest` 的 11 件只是「编译通过 + 数学推导自洽」，运行期未执行。
- 未跑客户端/服务端冒烟 → 键位、手感、通信收敛全部未实测（见 §6）。

### 4.1 追加：数值実測（编译沙盒内直接跑 `BroomSteer`）

`EntityMarisaBroom` の実体 tick は「`step()` → `move()` → `dampAfterMove()`」で、
数学的には `out = damp(step(in))`（`move()` は移動量を書き換えない）。
`BroomSteer` は世界に触れない純関数なので、**编译沙盒のクラスパスでそのまま JVM 実行**して数値を実測した
（`javac` のみ／一時ファイルは `/tmp`、リポジトリ外。主樹・staging には何も追加していない）:

| 入力 | 実測結果（`step()` 出力） | `GameTest` の対応 |
|---|---|---|
| W 1000 tick（俯仰 0） | `(-0.8, 0, -2.4e-18)` len=0.8 | `stepForwardAcceleratesAlongBroomYaw` |
| JUMP+W 1 tick / damp 後 | `(0, 0.5, 0)` → `(0, 0.4749999940, 0)` | `stepHoverAssignsVelocity` |
| JUMP+S / JUMP+D / JUMP+A | `(0,-0.5,0)` / `(0,0,-0.5)` / `(0,0,0.5)` | `stepHoverAssignsVelocity` / `stepHoverForcesHorizontalCap` |
| 無入力 `(0.4,0.2,-0.1)` | `(0.388, 0.194, -0.097)`（= ×0.97） | `stepIdleDecaysAndFalls` |
| 俯仰 −90° / +90°（無入力、入力 0 から） | `+0.0388` / `−0.0388` | `stepIdleDecaysAndFalls` |
| W 1 tick / S 1 tick | `len 0.04` / `len 0.08` | `stepBackwardIsTwiceForwardAcceleration` |
| W の dry / wet | `-0.04` / `-0.02` | `stepWaterHalvesMotion` |
| 全 4 キー 1000 tick / +JUMP | len **0.8** / len **0.5**（発散しない） | `fullMaskStaysBounded` |
| 無人の空中 `(0.5,0.1,-0.25)` | `(0.45, 0.07, -0.225)` | `emptyBroomSinksAndDecays` |
| 無人の接地（入力 `(0,0,0)` / `(0,-0.5,0)` / `(0,+0.4,0)`） | いずれも `(0, -0.03, 0)`（固定） | `emptyBroomGroundedVerticalIsFixed` |
| 無人の接地 + damp を 2 tick | `-0.0284999996 → -0.0284999996`（**相等**＝運動量パケットが出ない条件） | `emptyBroomGroundedVerticalIsFixed` |
| `decayEmptyPitch(30 / −30 / 0.5)` | `29.0 / −29.0 / 0.5` | `emptyBroomSinksAndDecays` |
| `maskOf(W,−,−,−,JUMP)` | `17`（= 1\|16） | —（ビット定義の確認） |

**全 10 件の純関数テストの期待値が実測と一致**（`EPS = 1e-9` 内）。
`W` の Z 成分のような ±1e-18 の残差は `Math.toRadians(−90°)` の丸め由来で、テストの許容差内。
「無人の接地」行は主樹 I13 の修正後の値（旧版は `−0.03 + 0.06 = +0.03`、接地ごとに上下動した）。

`-Xlint:deprecation` / `-Xlint:unchecked` 全開でも **`broom` を含む警告は 0 件**
（警告はすべて主樹既存の他クラス由来）。

---

## 5. 设计中修正的两处判断（与 `../DESIGN.md` 的差异，请以此为准）

1. **不加 `BroomSteerPayload`（Phase 2 也不必）**。
   - 设计稿计划「客户端发 5 bit，服务端镜像」。实现时复核标准流程后判断**多余**：
     `LocalPlayer#tick` 在骑乘中**每 tick** 发送 `ServerboundPlayerInputPacket`（`xxa/zza/jumping/shiftKeyDown`），
     服务端把它写进骑手的 `xxa/zza/jumping`；箒作为载具又每 tick 发送 `ServerboundMoveVehiclePacket`。
   - 若再加自建位域包，**每个客户端 tick 会多出一个自定义包**（输入变化才发也不足以避免长按 W 时的持续发送），
     与服务端权威无关（权威仍由 `MoveVehicle` 的位置校验承担，见 DESIGN §2.2 的结论）。
   - 因此本批**零协议改动**：改动的只有 3 个新类 + 2 处既有文件的改动。这是发布前夜风险最小的形态。
     若 K3 仍要「服务端镜像位域以便未来权威化」，`../DESIGN.md` §3.5 的文件 5/6 可直接加上，与本批互不冲突。

2. **需要 `isControlledByLocalInstance()`，而不是「客户端读键 + 服务端读 `xxa/zza`」**。
   - 设计稿曾列「服务端用 `xxa/zza` 还原按键」。实现时放弃，原因：
     `EntityMarisaBroom` 离线/无人时 `getControllingPassenger() == null` →
     `isControlledByLocalInstance()` 回落到 `isEffectiveAi()`；若按侧别（`level().isClientSide`）分工会更脆弱，
     而 `isControlledByLocalInstance()` 一次同时覆盖「本地骑乘 / 服务端骑乘 / 旁观者 / 无人」四种情形，
     正是 `Boat` 的既有形态（`Boat.java:299-315`）。服务端因此不需要读任何按键。

3. **「前 tick 的箒朝向」是移植正确性的关键**（新增发现，写入 `BroomSteer` 的类 javadoc）。
   原作在**输入演算（395-479）之后**才把箒的 yaw/pitch 更新为骑手值（481-507）。因此 `step()` 必须收到
   「前 tick 的箒 yaw/pitch」。本批在 `tick()` 开头用 `prevYaw/prevPitch` 读取，
   **不是** `rider.getYRot()`。若直接传骑手 yaw，等价于把 `R` 与 `R−90°` 混用，左右横移方向会错 90°。
   （顺带修掉：主樹旧代码里 `motionXZ`、以及 `pitch *= 0.1F` 后 `pitch` 未被使用的死代码，本批不再复制。）

---

## 5.1 已知的「実体側がまだ実測できていない」点

純関数は §4.1 で数値まで確認したが、**実体の tick 経路**（`move()` の衝突、`isControlledByLocalInstance()` の
実機での真偽、`LocalPlayer` が送る `MoveVehicle` の収束）は GameTest 実行／実クライアントでしか確認できない。
特に `controlGateFollowsPlayerPassenger` は **`Player#startRiding(vehicle, true)`** を使っている:
GameTest の `makeMockPlayer` はログインしていないモックなので `canRide()`（`isShiftKeyDown`／`boardingCooldown`）と
`canAddPassenger()` の通常経路は通るが、`force = true` にしておく方が実行時のノイズ（ボード遅延、ポーズ）に強い。
本番の乗車は `EntityMarisaBroom#interact`（サービス側で `startRiding(this)`）なので、テストの `force` とは経路が違う点に注意。

### 5.2 主樹 `BroomPhysicsTest`（I13 回帰 / 計 5 件）との関係

主樹には `thkaguyamod/test/BroomPhysicsTest.java`（`groundedBroomStaysStill` を含む）が既にあり、
**本批の `BroomSteerTest` と責務が一部重なる**。統合時の扱いは次のとおり:

| 主樹 `BroomPhysicsTest` のテスト | 本批 `BroomSteerTest` の対応 | 重複の扱い |
|---|---|---|
| `unriddenBroomDeceleratesAfterDismount` | （純関数ではないので対応なし） | そのまま残す |
| `groundedBroomDoesNotSink` | `emptyBroomGroundedVerticalIsFixed` | 主樹版は実体の位置、本批版は純関数の値。**両方残す**（層が違う） |
| `groundedBroomStaysStill`（I13） | `emptyBroomGroundedVerticalIsFixed` | 同上。I13 の実体側の回帰は主樹版が担う |
| `unriddenBroomGlideDistanceMatchesLegacy` | （純関数版は無し） | そのまま残す |
| `unriddenBroomTravelDistanceMatchesLegacy` | （純関数版は無し） | そのまま残す |

→ **削除が必要なテストは無い**。本批を足すと GameTest は 61 + 11 = 72 件になる見込み（`BroomSteerTest` が
11 件で、うち既存と同名のものは無い）。合計数はマージ時点の実測で確定させること。

---

## 6. 未验证项与风险（交给 K3 / 人工验收）

| # | 项 | 说明 |
|---|---|---|
| V1 | **运行时 GameTest 未跑** | `BroomSteerTest` 的 11 件断言仅经推导 + 编译。「原始速度が 0.5²=0.25 になる」等の導出値は計算で合っているが、実行時に `Vec3.length()` の丸めで `EPS=1e-9` を外す可能性は低い（全て 1 tick 分の厳密な乗除算）。 |
| V2 | **`mountedBroomMovesServerSide` の沈下量** | 構造は 7³。15 tick で `−0.03` ずつ沈む想定。着地後は I13 により `motionY` が `−0.03` に固定される（位置は動かない）ので、空中の 15 tick 内に床へ着くかどうかで目減りする。実行時に赤なら 5 tick へ減らすか、`assertTrue` を「移動量が非ゼロ」へ緩める。 |
| V3 | **手動マージ競合** | 主樹は他批次が進行中（本作業中にもファイル数が 259 → 260 → 264 と変化、HEAD も `2dcac4a` → `159ede0`）。パッチは `159ede0` に対して適用検証済みだが、`EntityMarisaBroom.java` は競合しやすい。§3.2 の「3 点だけ手で反映」を保険に。 |
| V4 | **手感（人間の目視）** | 巡航 ≈0.5 格/tick、ホバー ±0.25 格/tick、俯仰 45° 上昇 ≈+0.33 格/tick（`../DESIGN.md` §1.3 の導出）。**旧版と比べて「速い/遅い」は実測でしか決められない**。 |
| V5 | **速度上限の実装修正（発散 vs クランプ）** | 原作の `motion *= moveSpeed` を `limit/speed` へ修正した。単キー時の実効値は両者一致（|v| = limit）だが、**多キー同時押しで旧版と挙動が変わる**（旧版は発散）。`../DESIGN.md` §6 の第 1 決定点。回帰テストは `fullMaskStaysBounded`。 |
| V6 | **カメラ/視線の追従** | サーバー側で箒 yaw を `riderYaw − 90°` に固定している（原作 507 と同じ）。騎手が頭を振ったときの「体の向き」は原版の body-rotation 補間（`LivingEntity.yBodyRot`）が別途担当するため、旧版の見た目と一致するかは目視確認が要る。 |
| V7 | **`steerMask` の同期** | 非同期フィールドなので、**他プレイヤーから見た箒の描画**には影響しない（位置・向きはパケットで届く）。影響するのは「ローカル騎乗者を持つ側」の演算だけ。問題なし。 |

---

## 7. 需要 K3 拍板 / 追加作業

1. `../DESIGN.md` §6 の 5 決定点（特に**速度上限を修正するか**、**Phase 2 の Payload を足すか**）。
2. `porting/USER_MIGRATION.md` §3 / §5 の「魔法の箒的操舵＝未完成」記述の更新（发布方所有，本批未改）。
3. 合并后跑 `./gradlew runGameTestServer`（合并时点的主树为 **61/61**，本批 +11 ⇒ 预计 **72**；以合并时点的实际计数为准），
   再跑 `porting/tools/smoke_test.sh` 双模式 + R3 grep 闸门。
4. 人工验收清单见 `../DESIGN.md` §5.2 的第 1-7 条（单人按键手感、第三人称、离鞍坠落、撞墙收敛、局域网/专用服务端）。

---

## 8. 合并 Phase 1 后的玩家可见变化 / 回归闸门 / 风险（决策用事实）

### 8.1 玩家可见变化

| # | 变化 | 依据 |
|---|---|---|
| 1 | **操舵恢复**: W 前进 / S 后退（速度为前进的 2 倍）/ A・D 横移 / 空格＝悬停模式（W 上升、S 下降、A・D 横移） | `BroomSteer.step`（原作 395-560） |
| 2 | **「必须按键」**: 「朝骑手视线方向自动前进」被替换。**不按任何键时**保留原作行为（俯仰升力 + 0.97/0.95 双阻尼），因此抬头/低头仍改变轨迹，但不再自动前飞 | 原作 447-454；`../DESIGN.md` §1.3・§4.1 |
| 3 | **抖动/橡皮筋消失**: `getControllingPassenger()` 生效后，本地骑乘＝客户端独占积分，服务端只校验位置；服务端不再「恒按 W」地跑另一套模拟，`lerpTo` 也不再覆盖客户端位置 | 主树现在的骑乘分支无侧别门 → 双侧积分被服务端位置覆盖（`../DESIGN.md` §2.3 表） |
| 4 | **无人箒**: 由「客户端+服务端两侧都积分」变为「服务端独占积分」。I13 的缓降/静止语义不变（`stepEmpty` 吸收） | §3.2 对照表 |
| 5 | **多键同按**: 原作的 `motion *= moveSpeed` 发散（W+S+A+D 长时间按住可指数加速）改为正确的上限钳制（0.8 / 悬停 0.5）。这是行为差异，需在迁移文档记一条 | `BroomSteer.capFactor`（`// TODO K3-review`） |
| 6 | **键位可重映射**: 通过 `Options` 读取，因此受玩家自己的按键设置控制（旧版同样如此）；开界面时 `KeyMapping.releaseAll()` 会正确清空输入 | `BroomSteerClient` |
| 7 | **已知问题 #1 可关闭**: `porting/USER_MIGRATION.md` §3「操舵暂为朝视线前进」与 §5 第 1 条「魔法扫帚不能手动驾驶」应改写为「已恢复手动操舵」，并把第 5 条（多键发散修复）作为新差异条目补入。**该文档属发布方所有，本批未改** | `USER_MIGRATION.md:148`・`:237` |

### 8.2 必须回归的闸门

1. `./gradlew compileJava`（0 错）→ `./gradlew runGameTestServer`（主树 61 + 本批 11 = 72，全绿）；
2. `porting/tools/smoke_test.sh` 双模式（client / server）；
3. R3 闸门：`grep -r '^import net.minecraft.client' porting/neoforge-1.21.1/src/main/java/thkaguyamod --include=*.java` 只允许 `client/` 包。
   本批的 `BroomSteerClient` 落在 `thkaguyamod/client/input/`，`BroomSteer` 只依赖 `Vec3` —— 已满足；
4. **人工**（GameTest 覆盖不到）: 单人按键手感（W/S/A/D/空格/空格+W/空格+S/W+S/A+D）、第三人称视角、离鞍与坠落、
   撞墙时的位置收敛（服务端 `moved wrongly` 回拉）、入水减速、局域网观察者视角、专用服务端；
5. **网络回归**: 主树 `BroomPhysicsTest` 的 I13 断言（`groundedBroomStaysStill`）必须仍绿——本批把
   `motionY = -0.03` 搬进 `BroomSteer.EMPTY_GROUNDED_VERTICAL`，语义等价但代码位置变了。

### 8.3 风险（决策时需权衡）

| 风险 | 说明 | 缓解 |
|---|---|---|
| 客户端权威 | 扫帚等价「受限飞行」（服务端 `handleMoveVehicle` 的 10 格/tick 上限，0 重力实体免漂浮踢）。与原版船同级 | 发布说明标注；要服务端权威需 Phase 2 + 预测/和解，成本高 |
| 手感调参 | **悬停模式（空格）与旧版逐位一致**（`0.5 > 0.5` 为 false，两侧都不触发上限分支）；差异只在常规模式的巡航：旧版因 `motion *= moveSpeed` 在 `0.6355〜0.7871` 之间循环（均值 ≈0.717），本实现稳定在 `0.8 × 0.99 = 0.792` | 见 §9 的数値修正；常量集中在 `BroomSteer`，调参成本 1 行 |
| 协议 | 本批**零协议改动**（不加自建包），任何已分发客户端/服务端组合都可连 | §5.1 |
| 合并冲突 | `EntityMarisaBroom.java` 是热点文件（本批期间主树从 `2dcac4a` 走到 `159ede0`）。补丁已对 `159ede0` 验证，但仍有再次变化的风险 | §3.2 的「3 点手改」保险 |
| 未实测 | `BroomSteerTest` 11 件只经编译 + 数值实算，未在 `runGameTestServer` 跑过 | 合并后第一件事就是跑闸门 1 |

---

## 9. 决策用数値修正（重算，2026-10-03；`../DESIGN.md` §1.3 の一部を上書き）

本節は `../DESIGN.md` §1.3 の「巡航 ≈0.3〜0.5 格/tick」を**誤り**として訂正する。
原作の上限分支は {@code if (moveSpeed > 0.8D) motion *= moveSpeed}（スカラー乗算）であり、
スカラー再帰で回すと次のようになる（pitch 0、W 単押し、実測ではなく式の直接計算）:

| 状態 | 原作の 1 tick あたり速度 | 本実装（正しいクランプ） |
|---|---|---|
| 通常モード W 単押し 巡航 | `0.6355 〜 0.7871` を循環（平均 **≈0.717**） | 一定 **`0.8 × 0.99 = 0.792`** |
| 懸停（空格）+ W/S | `0.5 > 0.5` が false ⇒ 上限分支**不発** ⇒ `0.5 × 0.99 = 0.495` | 同じく **0.495**（逐位一致） |
| 懸停 + A/D | 同上 **0.495**（水平 1 軸） | 同上 **0.495**（逐位一致） |
| 懸停 + W + D | ベクトル長 `√2 × 0.5 = 0.7071 > 0.5` ⇒ 発火 ⇒ `0.3536`/軸（= 0.5 に正規化） | 同じく `0.3536`/軸 |

つまり:

1. **懸停操舵は旧版と数値一致**（この分支だけは原作のスカラー乗算が結果的に正しいクランプと同値になる）。
2. 差が出るのは通常モードの巡航だけで、幅は **0.717 → 0.792（+10%）**、かつ旧版の 4 tick 周期の脈動が消える。
   `../DESIGN.md` §1.3 の 0.3〜0.5 は誤り（0.8 の上限をソフトリミットと読み違えたもの）。
3. 発散（多キー同押しで速度 >1 から指数的増加）は旧版のみ。`BroomSteerTest.fullMaskStaysBounded` が本実装側を固定する。