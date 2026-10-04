# diag-knife-orientation：银刀投掷朝向固定

## 现象
各类小刀右键投掷后，刀的朝向永远固定在一个方向（刀柄不朝向投掷者/不随投掷方向旋转）。

## 根因（已定位，证据链）

**一句话：客户端侧的 `EntitySilverKnife` 永远拿不到投掷速度（motionX/Y/Z 恒为 0），
于是每 tick 的 `rotationYaw = atan2(0.0, -0.0)` 退化为恒定 **180°**，把服务器同步过来的
正确朝向每帧覆盖掉；1.7.10 之所以正常，是因为旧版把速度同步给了客户端。**

证据链：

1. `porting/.../entity/item/EntitySilverKnife.java:132-135`（投掷构造器）
   ```java
   float rotationYaw = (float)Math.atan2(motionX, -motionZ) / 3.141593F * 180F;
   float rotationPitch = (float)Math.atan2(motionY, Math.sqrt(...)) / 3.141593F * 180F;
   moveTo(pos.x + angle.x * speed, ..., rotationYaw, rotationPitch);
   ```
   朝向完全由 **本实体自己的 motion 字段** 推导。与 1.7.10 原作
   `src/main/java/thKaguyaMod/entity/item/EntitySilverKnife.java:97-100` 逐字一致（原作即如此）。

2. `EntitySilverKnife.java:122-130`：`motionX/Y/Z`、`lastShotMotionX/Y/Z`、`accelerationX/Y/Z`
   只在这个**投掷构造器**里初始化。

3. 客户端永远不跑这个构造器：`ClientPacketListener.handleAddEntity` → `createEntityFromPacket`
   用的是 `entitytype.create(level)`（无参构造器，`EntitySilverKnife.java:89-100`），
   它**不设置任何 motion**。`ClientboundAddEntityPacket` 里虽然带速度字段，但
   （a）服务端侧 `ServerEntity` 取的是 `entity.getDeltaMovement()`，而银刀从不调用
   `setDeltaMovement`（全仓库仅 `EntityTHShot.java:1271` 用到 deltaMovement，与刀无关），
   恒为 `Vec3.ZERO`；
   （b）1.21.1 的 `Entity.recreateFromPacket(ClientboundAddEntityPacket)`（已核对反编译源码
   `Entity.java:3562-3573`）只写位置/朝向/id/uuid，**不写速度**。
   即：客户端刀子的 motion 恒为 0。

4. 客户端每 tick 重算朝向并 `setRot`：
   `EntitySilverKnife.java:669-709`
   ```java
   float rotationYaw = (float)((Math.atan2(motionX, -motionZ) * 180D) / 3.1415927410125732D); // 0.0, -0.0 -> 180°
   ...
   setRot(rotationYaw, rotationPitch);   // 覆盖掉 lerp 同步来的朝向
   ```
   `Math.atan2(+0.0, -0.0)` 在 Java 规范里恒为 **+π（180°）**，与投掷方向无关 →
   客户端每 tick 把 yaw 钉死在 180°，正是用户看到的「永远固定一个方向」。
   （pitch 因为 `:672-675` 的 `if(!level().isClientSide)` 保护而在客户端保留，所以只有 yaw 固定。）

5. 1.7.10 对照：`CommonProxy.java:162` 注册时
   `initEntity(EntitySilverKnife.class, "SilverKnife", id, mod, 40, 1, true)` —— 最后一个 `true`
   是 **sendsVelocityUpdates**，旧版 `EntityTrackerEntry` 会把 `motionX/Y/Z` 同步给客户端
   （生成包 + S12PacketEntityVelocity），所以旧版客户端算 `atan2` 用的是**真实速度**，
   朝向正确。移植版丢了这条速度同步（`EntityType.Builder` 的 `trackDeltas` 依赖
   `getDeltaMovement()`，而本实体用的是自有 motion 字段），故退化。

6. 弹幕 `FORM_KNIFE`（`RenderTHShot` 的降级程序化实现，台账 §5#4）**不是同一数据链**：
   弹幕朝向来自 `EntityTHShot.getYRot()/getXRot()`（`RenderTHShot:312` 用 `yaw/pitch` 参数 +
   `thShot.getAngleZ()`），与银刀实体无关 → 同因不成立，且属已登记未完成项，不修。

## 修复（最小，忠于原作语义：刀柄朝向投掷者/随投掷方向）

`porting/.../entity/item/EntitySilverKnife.java`（tick 内，:672-684）：
motion 水平成分全为 0 时（＝客户端常态）不再用 `atan2` 覆盖，改为保留当前（服务器同步来的）朝向；
非退化情形（服务器飞行中、垂直弹道等）计算路径一字不改。

```java
float rotationYaw = (motionX == 0.0D && motionZ == 0.0D)
        ? getYRot()
        : (float)((Math.atan2(motionX, -motionZ) * 180D) / 3.1415927410125732D);
```

补记：`rotationPitch` 不受本 bug 影响——它的重算被 `if(!level().isClientSide)`（:686-689）保护，
客户端永远用同步来的 pitch，所以用户只看到 yaw 固定。

## 改动清单

| 文件:行 | 改动 |
|---|---|
| `porting/neoforge-1.21.1/src/main/java/thkaguyamod/entity/item/EntitySilverKnife.java:672-684` | tick 内的 yaw 重算：motion 全 0（＝客户端常态）时保留当前朝向（服务器同步值），否则维持原 `atan2` 计算。附 8 行注释说明 1.7.10 的速度同步为何在此失效。 |
| `porting/neoforge-1.21.1/src/main/java/thkaguyamod/test/SilverKnifeOrientationTest.java`（新增） | 2 个 GameTest（见下）。 |

未改：`RenderSilverKnife`（`Axis.YP.rotationDegrees(180F - entityYaw)` 与原作 `glRotatef(180F - yaw, ...)`
逐字等价，用 `entityYaw` 是 1.21.1 的正确写法）；`ItemSilverKnife`；弹幕 FORM_KNIFE（不同数据链，已登记）。

## 测试与验证（实测）

新增 `porting/neoforge-1.21.1/src/main/java/thkaguyamod/test/SilverKnifeOrientationTest.java`：

1. `thrownKnifeKeepsThrowDirection`：按 `ItemSilverKnife#use` 的路径投掷（yaw = 90/0/-90/180/45°+pitch30°），
   断言生成瞬间 `getYRot()/getXRot()` 等于**原作定义**
   `atan2(motionX, -motionZ)` / `atan2(motionY, |motion_xz|)`，并再跑 2 tick 确认不被覆盖。
2. `zeroMotionDoesNotCollapseYawTo180`：直接复现**客户端生成路径**
   （无参构造器 + `setYRot(期望值)` + motion 全 0 + `setKnifeCount(3)`），跑 2 tick 后断言
   yaw 仍是期望值、**没有**被钉到 180°。

实测结果：

| 步骤 | 命令 | 结果 |
|---|---|---|
| 编译 | `./gradlew compileJava --offline` | **0 错误**（仅既有 unchecked/deprecation 注记） |
| 回归测试（**修复前**，临时回退修复以验证测试有效性） | `./gradlew runGameTestServer --offline` | `zeromotiondoesnotcollapseyawto180` **FAIL**：`motion が 0 でも yaw は 180° に潰れず 0.0 を保つはず。実際: 180.0` ← **精确复现用户现象** |
| 回归测试（**修复后**） | 同上 | **All 95 required tests passed :)**（基线 89 + 本次新增 2 + 其它批次新增；两次全绿运行均为 95/95） |

- 首次运行时 `thrownKnifeKeepsThrowDirection` 因**测试自身期望值写错**（写成「等于玩家 yaw」，
  而原作定义含 `atan2` 的 180° 折返）失败并报 `期望 90.0 实际 -89.99999`；改为原作定义后通过，
  反向印证了「朝向由 motion 的 atan2 定义」这一语义。
- 静态/数值验证补充：`Entity.recreateFromPacket`（1.21.1 反编译源码 `Entity.java:3562-3573`）
  只写 pos/rot/id/uuid **不写速度**，且 `ServerEntity` 取的是 `getDeltaMovement()`（银刀恒为 `Vec3.ZERO`）
  → 客户端 motion 必为 0 的结论有源码级证据。

### 基线无关的偶发失败（非本次改动引入）
`FairySummonFireTest` 的两条（`summonedFairyShotsMove` / `summonedFairyFireIntervalMatchesSpan`）
与 `directDanmakuPatternProducesMovingShot` 在多次运行间**随机**失败（同一提交下两次跑失败集合不同：
`{fairy×2}`、`{fairy×1, direct}`、`{none}`）。它们与银刀无调用关系，且
`FairySummonFireTest.java` 的 mtime 落在本批次运行期间（其他批次正在改），本次改动只触及
`EntitySilverKnife.java` + 新增测试文件。修复后的全绿运行（95/95）即为交付证据。
