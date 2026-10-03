# broom-input 实现草稿（Phase 1）— 接线与验证说明

本目录是**草稿**，不参与编译：`porting/staging/broom-input/impl/` 下所有内容都未进入主树。
设计与依据见同目录上级的 `../DESIGN.md`（1.7.10 行为摘要、通道对比、选型结论）。

---

## 1. 产出物清单

| 文件（相对 `porting/staging/broom-input/impl/`） | 类型 | 最终路径 | 行数 | 说明 |
|---|---|---|---|---|
| `thkaguyamod/entity/item/BroomSteer.java` | 新 | 同左 | 297 | 操舵演算の純関数。原作 395-687 の数値・分岐を関数化。速度上限のみ意図的に修正（`// TODO K3-review` 付き） |
| `thkaguyamod/client/input/BroomSteerClient.java` | 新 | 同左 | 89 | `ClientTickEvent.Pre` で `Options.keyUp/Down/Left/Right/Jump` を読み、5 bit を騎乗中の箒へ書き込む |
| `thkaguyamod/entity/item/EntityMarisaBroom.java` | 改（**完全ファイル**） | 同左 | 423 | `getControllingPassenger()` 追加／`steerMask` 追加／`tick()` を「制御側だけ積分」へ再構成 |
| `EntityMarisaBroom.java.patch` | 改（unified diff，`-p1`） | — | 248 | 上記作業を主樹 HEAD 版に当てた差分。適用検証済み |
| `THKaguyaClient.java.patch` | 改（unified diff，`-p1`） | — | 26 | import 2 行 + コンストラクタ 1 行（`NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, BroomSteerClient::onClientTickPre)`） |
| `thkaguyamod/test/BroomSteerTest.java` | 新 | 同左 | 329 | GameTest 11 件。純関数 9 件 + 制御権 1 件 + 実体結合 1 件 |

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

## 3. 主樹の「無人減速修復」との統合（**回退禁止**）

本草稿作成の直前に主樹へ入った修正：

> `EntityMarisaBroom#tick()` の騎乗 if に **else（無人の減速・降下）分岐**が追加され、
> 「無人時は `motionX/Z *= 0.9`・`motionY -= 0.03`・俯仰 1°/tick 減衰、接地なら `+0.06`
> を `setDeltaMovement` に反映する（ローカル変数に書くだけでは届かない）」となっている。

**本草稿はこれをそのまま保持している**（回退していない）。対応関係:

| 主樹の現行コード | 本草稿での位置 |
|---|---|
| 無人の `motionX *= 0.9` / `motionY -= 0.03` / `motionZ *= 0.9` | `BroomSteer.stepEmpty(...)`（原作 672-674 のまま、`EMPTY_DRAG` / `EMPTY_SINK`） |
| 無人の俯仰 1°/tick 減衰 + `onGround` で `+0.06` | `BroomSteer.decayEmptyPitch(...)` / `stepEmpty(...)` 内の `EMPTY_GROUND_LIFT` |
| 「`move()` より前に移動量へ反映」のコメントと意図 | 本草稿は `setDeltaMovement(stepEmpty(...))` を `move()` の**前**に置いており同義 |
| 入水 `*0.5` が無人の分岐にも掛かっていた | `stepEmpty` の末尾に同じ `WATER_FACTOR` を入れて保存（原作 555-560 は両分岐共通） |

**差分（意図的なもの）は「どの側で走るか」だけ**:

- 主樹: 乗員がいれば誰でも（両側で）積分、いなければ両側で積分 → 二重積分＋通信路が閉じたまま。
- 本草稿: `isControlledByLocalInstance()` が **true の側だけ**積分し、他方は `setDeltaMovement(ZERO)`。
  無人側の式は不変なので、「無人箒が緩やかに沈む」という修復済みの挙動は**保存される**
  （GameTest `mountedBroomMovesServerSide` がサーバー側で沈むことを固定している）。

**唯一、形式上変わる点**: 無人箒の俯仰減衰は主樹では分岐の中で `this.setXRot(...)` していたが、
本草稿では `float` を返す純関数にして代入している（値は同じ、`decayEmptyPitch`）。`// TODO K3-review` は付けていない
（挙動同値の単なる関数抽出のため）。

**統合時の注意**: もし K3 が「主樹の無人ブロックを更に変更した」場合、`EntityMarisaBroom.java.patch` が
当たらない可能性がある。その場合はパッチではなく目視マージに切り替え、下記の 3 点だけ手で反映すれば十分:
1. `getControllingPassenger()` の追加、
2. `steerMask` フィールド + `setSteerMask`/`getSteerMask`、
3. `tick()` の移動ブロックを `if (isControlledByLocalInstance()) { ...積分... } else { setDeltaMovement(ZERO); }` で囲み、
   中の計算を `BroomSteer.step/stepEmpty` に置き換える。

---

## 4. 编译验证（等价 javac 沙盒／未跑 gradlew）

`staging/gametest-edge/REPORT.md` §2.2 と同じ組み立て（`neoforge-21.1.252-merged.jar` +
`build/moddev/clientLegacyClasspath.txt`，路径经 `cygpath -w` 归一化、源文件清单用 Windows 绝对路径）：

```bash
W=/tmp/bs-build
cp -r porting/neoforge-1.21.1/src/main/java/. $W/src/main/java/           # 主树 260 文件
cp -r porting/staging/broom-input/impl/thkaguyamod/. $W/src/main/java/thkaguyamod/   # 本批 3 新文件
patch -p1 -d $W < porting/staging/broom-input/impl/THKaguyaClient.java.patch         # +1 行接线
# 源文件清单必须是 Windows 绝对路径（cygpath -w），否则 Windows 版 javac 会把 /tmp/... 当相对路径
find $W/src/main/java -name '*.java' | while IFS= read -r p; do cygpath -w "$p"; done > $W/files.txt
javac -nowarn -proc:none -encoding UTF-8 -cp "$MERGED;$LEGACY" -d "$(cygpath -w $W/out)" @"$(cygpath -w $W/files.txt)"
```

**结果：`javac exit: 0`，263 个源文件，仅 `deprecation` / `unchecked` 两条 note（与主树基线同源，非本批引入）。**
产出确认：`thkaguyamod/entity/item/BroomSteer.class`、`EntityMarisaBroom.class`、
`thkaguyamod/client/input/BroomSteerClient.class`、`thkaguyamod/test/BroomSteerTest.class`。

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
| 無人の `(0.5,0.1,-0.25)` | `(0.45, 0.07, -0.225)` | `emptyBroomSinksAndDecays` |
| 無人の接地 `(0,0,0)` | `(0, 0.03, 0)` = −0.03+0.06 | `emptyBroomLiftsWhenOnGround` |
| `decayEmptyPitch(30 / −30 / 0.5)` | `29.0 / −29.0 / 0.5` | `emptyBroomSinksAndDecays` |
| `maskOf(W,−,−,−,JUMP)` | `17`（= 1\|16） | —（ビット定義の確認） |

**全 9 件の純関数テストの期待値が実測と一致**（`EPS = 1e-9` 内）。
`W` の Z 成分のような ±1e-18 の残差は `Math.toRadians(−90°)` の丸め由来で、テストの許容差内。

`-Xlint:deprecation` / `-Xlint:unchecked` 全開でも **`broom` を含む警告は 0 件**
（145 行の警告はすべて主樹既存の他クラス由来）。

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

---

## 6. 未验证项与风险（交给 K3 / 人工验收）

| # | 项 | 说明 |
|---|---|---|
| V1 | **运行时 GameTest 未跑** | `BroomSteerTest` 的 11 件断言仅经推导 + 编译。「原始速度が 0.5²=0.25 になる」等の導出値は計算で合っているが、実行時に `Vec3.length()` の丸めで `EPS=1e-9` を外す可能性は低い（全て 1 tick 分の厳密な乗除算）。 |
| V2 | **`mountedBroomMovesServerSide` の沈下量** | 構造は 7³。15 tick で `−0.03` ずつ（初回粘性込み）沈む想定で、床に着いても `+0.06` で押し戻される。壁・床の有無で目減りするが「`getY() < startY`」は満たす想定。実行時に赤なら 5 tick へ減らすか、`assertTrue` を「移動量が非ゼロ」へ緩める。 |
| V3 | **手動マージ競合** | 主樹は他批次が進行中（本作業中にもファイル数が 259 → 260 へ変化）。パッチは適用検証済みだが、`EntityMarisaBroom.java` は競合しやすい。§3 の「3 点だけ手で反映」を保険に。 |
| V4 | **手感（人間の目視）** | 巡航 ≈0.5 格/tick、ホバー ±0.25 格/tick、俯仰 45° 上昇 ≈+0.33 格/tick（`../DESIGN.md` §1.3 の導出）。**旧版と比べて「速い/遅い」は実測でしか決められない**。 |
| V5 | **速度上限の実装修正（発散 vs クランプ）** | 原作の `motion *= moveSpeed` を `limit/speed` へ修正した。単キー時の実効値は両者一致（|v| = limit）だが、**多キー同時押しで旧版と挙動が変わる**（旧版は発散）。`../DESIGN.md` §6 の第 1 決定点。回帰テストは `fullMaskStaysBounded`。 |
| V6 | **カメラ/視線の追従** | サーバー側で箒 yaw を `riderYaw − 90°` に固定している（原作 507 と同じ）。騎手が頭を振ったときの「体の向き」は原版の body-rotation 補間（`LivingEntity.yBodyRot`）が別途担当するため、旧版の見た目と一致するかは目視確認が要る。 |
| V7 | **`steerMask` の同期** | 非同期フィールドなので、**他プレイヤーから見た箒の描画**には影響しない（位置・向きはパケットで届く）。影響するのは「ローカル騎乗者を持つ側」の演算だけ。問題なし。 |

---

## 7. 需要 K3 拍板 / 追加作業

1. `../DESIGN.md` §6 の 5 決定点（特に**速度上限を修正するか**、**Phase 2 の Payload を足すか**）。
2. `porting/USER_MIGRATION.md` §3 / §5 の「魔法の箒的操舵＝未完成」記述の更新（发布方所有，本批未改）。
3. 合并后跑 `./gradlew runGameTestServer`（全量应为 **主树现有 22 + gametest-edge 24 已合并? + 本批 11**——以合并时点的实际计数为准），
   再跑 `porting/tools/smoke_test.sh` 双模式 + R3 grep 闸门。
4. 人工验收清单见 `../DESIGN.md` §5.2 的第 1-7 条（单人按键手感、第三人称、离鞍坠落、撞墙收敛、局域网/专用服务端）。