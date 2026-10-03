# diag-broom-render — 魔法扫帚飞行中模型自转/乱摆（诊断 + FIX）

基线 `d7e4ccc`（GameTest 76/76）。工作区 `D:\AI构建插件源代码\THKaguyaModRe-fork-master`。
只改了 1 个文件：`porting/neoforge-1.21.1/src/main/java/thkaguyamod/client/renderer/RenderMarisaBroom.java`。

---

## 1. 结论（TL;DR）

**根因 = 角度回卷（简报方向 3）。** 骑乘分支每 tick 把箒的 yaw 设成
「骑手 yaw − 90°」（非正规化、可以无限增长），而渲染用的 `entityYaw` 是
`LevelRenderer` 用**素 lerp** 把 `yRotO`（网络包写入的正规化值）与 `yRot`（非正规化值）插出来的。
两者相差 360° 的整数倍时，素 lerp 会「多转整数圈」，且中点可以偏离真实朝向最多 180°。

- 判定「乱转」的精确语义：**绕 Y 轴整圈甩**（不是受击摇晃、不是 roll 错误）。
- 不是受击摇晃：`timeSinceHit` 只在 `hurt()` 里置 10，骑乘飞行中不会触发；
  该动画用 `getTimeSinceHit()/getDamageTaken()/getForwardDirection()`（同步字段）且是 Z 轴小幅摆，
  与「飞行中乱转」不符。**已排除**。
- 不是 I9/I13 无人分支俯仰震荡：那些只在**无人**时执行（用户明确说飞行中）。**已排除**（但见 §6 遗留）。
- pitch（X 轴）不受影响：`Entity#turn` 把 xRot 夹在 ±90°，不会出现 360° 倍数差。**未改动**。

---

## 2. 证据链

### 2.1 代码位置

| # | 位置 | 事实 |
|---|---|---|
| E1 | `entity/item/EntityMarisaBroom.java:213` | 骑乘分支每 tick `this.setYRot(yaw - 90F)`（`yaw = living.getYRot()`） |
| E2 | `Entity#setYRot` | 只做 `Float.isFinite` 检查，**不做 `wrapDegrees`**（1.21.1 源码） |
| E3 | `MouseHandler#turnPlayer` → `Entity#turn` | 鼠标每帧 `setYRot(getYRot() + f1)`，**同样不正规化** → 客户端玩家的 yaw 随持续转向无限增长 |
| E4 | `LevelRenderer#renderEntity`（1.21.1 `LevelRenderer.java:1265`） | `float f = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());` ← **素 lerp**，这是渲染器第 2 参 `entityYaw` 的来源 |
| E5 | `Entity#baseTick`（`Entity.java:446-447`） | 每 tick 开头 `xRotO = getXRot(); yRotO = getYRot();` |
| E6 | `ClientPacketListener#handleMoveEntity` → `Entity#lerpTo` | 服务端位置/旋转包写入客户端实体：`lerpTo` 在 `Entity` 里就是 `setPos + setRot`（直接覆盖，无插值） |
| E7 | `ServerEntity.sendChanges`（骑乘分支） | 只发 `ClientboundMoveEntityPacket.Rot`（yaw 按 256 等分量子化 → 隐式正规化到 [-180,180]） |
| E8 | `ServerGamePacketListenerImpl#handleMovePlayer`（骑乘分支） | `player.absMoveTo(..., f, f1)`，`f = Mth.wrapDegrees(packet.getYRot(...))` → **服务端骑手 yaw 被正规化** |
| E9 | `Entity#lerpPositionAndRotationStep`（`Entity.java:3830-3841`） | 1.21.1 自己插值时用的是 `Mth.rotLerp`（不是素 lerp），说明 rotLerp 是本版本「正确的角度插值」 |

### 2.2 推导（E1+E3+E5+E6 → 360° 倍数差）

1. 客户端玩家 yaw 连续增长（E3）；箒的 yaw 每 tick 被设为「该值 − 90」（E1）→ 也是连续增长、非正规化。
2. 服务端每 tick 收到 `ServerboundMovePlayerPacket.Rot`（`LocalPlayer.tick`，骑乘时必发），
   正规化后写入**服务端骑手**（E8）；服务端箒的 yaw 被 `ServerEntity` 以 256 等分量化后广播（E7）。
3. 客户端把该包写入箒（E6）→ `yRot` 变成 [-180,180] 区间的小值。
4. **下一个 tick**：`baseTick` 把 `yRotO` 设为这个**正规化值**（E5），随后 `tick()` 又把 `yRot`
   设回「骑手 yaw − 90」的**非正规化值**（E1）。
5. 于是渲染时 `yRotO` 与 `yRot` 恰好相差 **360° 的整数倍**（两值 mod 360 相等）。
6. 素 lerp（E4）不知道这件事，按数值线性插值 → **整个 360° 倍数被"走"了一遍**。

### 2.3 数值验证（纯函数ハーネス，`javac` 沙盒实算）

`/tmp/brv/CongruentCheck.java` 复刻了 E5+E6+E1+E4 的完整序列（鼠标 7°/tick 持续转向 600 tick）：

```
yRot = echo + 360k のケース（マウス 7°/tick、600 tick）
  修正前: フレーム掃引 最大  3960.00 deg (11.00 回転) / 真の向きとの誤差 最大  180.00 deg
  修正後: フレーム掃引 最大     0.00 deg ( 0.00 回転) / 真の向きとの誤差 最大    0.00 deg
```

- **修正前**: 1 フレームで最大 **11 回転**ぶんの角度を掃引し、補間中点は真の向きから最大 **180°** ずれる
  → まさに「乱回転」。回転速度が速いほど回転数が増える（1°/tick でも 1 回転/フレーム）ので、
  「ときどき乱れる」ではなく**速く回すほど派手に回る**。
- **修正後**: 掃引 0（同一フレーム内では静止）、真の向きとの誤差 0。

---

## 3. FIX（最小・原作視覚セマンティクス維持）

`client/renderer/RenderMarisaBroom.java`

```java
// before
poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
// after
float renderYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - renderYaw));
```

- `Mth.rotLerp` は内部で `wrapDegrees(end - start)` を通すので**必ず短い弧を回る**
  （`Mth.java` の実装を確認）。1.21.1 自身もエンティティ補間（E9）でこれを使っている。
- **視覚セマンティクスは変わらない**: 通常操作（前後・微調整）では `yRotO` と `yRot` の差が小さいため
  `rotLerp` と素 lerp は**完全に一致**する（下の表）。差が出るのは 180° 差のときだけで、
  そのとき素 lerp は逆方向に回る（バニラの lerpTo も rotLerp で短い方を選ぶので、rotLerp が正しい）。
- pitch は 1.7.10 と同じく**現在値**（`-entity.getXRot()`）のまま。理由をコメントで明記
  （±90° クランプにより 360° 倍数差が起きない）。

一致確認（`/tmp/brv/FixCheck.java`）:

| `yRotO` | `yRot` | 素 lerp(0.5) | rotLerp(0.5) | 差 |
|---|---|---|---|---|
| 0 | −90 | −45.00 | −45.00 | 0 |
| 45 | −45 | 0.00 | 0.00 | 0 |
| 90 | 0 | 45.00 | 45.00 | 0 |
| −179 | 91 | −44.00 | −224.00 | 180（素 lerp が逆回り＝誤り） |
| 179 | −91 | 44.00 | 224.00 | 180（同上） |

---

## 4. 検証

| 項 | 結果 |
|---|---|
| `./gradlew compileJava` | **BUILD SUCCESSFUL（0 錯）** |
| `./gradlew runGameTestServer` | **All 76 required tests passed**（変更前と同じ 76/76、回帰なし） |
| 純関数ハーネス（`/tmp/brv/`、リポジトリ外） | 修正前 掃引 3960°/frame・誤差 180° → 修正後 0°/0°（§2.3） |
| 変更行数 | 1 ファイル / +27 −1（コメント含む。実コードは 2 行） |

> レンダ経路は GameTest のカバー外なので、数値検証は上記ハーネス（実装と同一の式を再現）で行った。

---

## 5. 未改动・明示的に排除した候補

| 候補 | 判定 | 根拠 |
|---|---|---|
| 受击摇晃动画（`timeSinceHit`） | **否** | `hurt()` でのみ 10 に設定。`tick()` が毎 tick 減算。Z 軸の小幅揺れで、騎乘飛行中は 0 のまま |
| roll（側傾）計算誤り | **否** | 箒のレンダラに roll 相当の回転は `timeSinceHit` の Z 回転のみ |
| I9/I13 无人分支の俯仰回正震荡 | **否（骑乘中は無関係）** | `else` 分支（无人時）のみ。骑乘時は `setXRot(rider.getXRot())` の 1 行のみ |
| `ModelMarisaBroom` | **否** | 移植版は `createBodyLayer()` を提供するだけで、`RenderMarisaBroom` は使っていない（手続き的クワッド描画）。回転源ではない |

---

## 6. 遗留（K3 判断待ち・本批未改）

1. **同一の 360° 倍数問題は「実体側の補間」にも残っている**。
   主樹には `getControllingPassenger()` が無いため、この箒はまだバニラの載具同期経路に乗っておらず、
   クライアントとサーバーが**両側で独立に積分**している。`Entity#lerpPositionAndRotationStep`（E9）は
   `rotLerp` を使うので角度は安全だが、位置は二重積分＋lerp で競合する。
   → `porting/staging/broom-input/impl/`（Phase 1 実装、`getControllingPassenger()` + 控制端独占積分）
   のマージで解消する。本批のレンダラ修正はそのマージ後も**そのまま必要**（マージ後も
   `yRotO` はネットワーク値、`yRot` は騎手由来の非正規化値になるため）。
2. **根本的には yaw を非正規化のまま持つのが原因**。`EntityMarisaBroom#tick()` の
   `setYRot(yaw - 90F)` を `setYRot(Mth.wrapDegrees(yaw - 90F))` にすれば
   `yRotO`/`yRot` の倍数差自体が消えるが、
   (a) 1.7.10 原作も非正規化のまま保持しており、
   (b) `wrapDegrees` を入れると `yRotO` との差が 180° 近辺で符号反転して
   1 フレームだけ大きく回る可能性がある（要検証）、
   (c) レンダラ側で `rotLerp` にすれば同コストで確実に直る、
   の理由で**レンダラ側のみの修正に留めた**。実体側も直す場合は `broom-input` の Phase 1 と
   まとめて 1 回の検証で行うのが安全。
3. **実機での最終確認は未実施**（`gradlew runClient` は本批で起動していない）。確認手順:
   騎乗 → マウスを 1 秒以上連続で回す → モデルが滑らかに追従すること
   （修正前はフレームごとに数回転していた）。
