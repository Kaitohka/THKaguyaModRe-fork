# 魔法扫帚「手动操舵」1.21.1 实现方案（批次 broom-input）

- 目标：把 1.7.10 的 W/A/S/D/空格 操舵（跳跃悬停上升、S 后退/下降、A/D 横移）恢复到 1.21.1 移植版。
- 分析方式：**纯静态阅读**。未运行 `gradlew`、未 git 操作、未改任何既有文件；本文档是唯一产出。
- 证据基线：
  - 1.7.10 原作：`src/main/java/thKaguyaMod/entity/item/EntityMarisaBroom.java`（下称 **旧箒**）
  - 移植版（只读）：`porting/neoforge-1.21.1/src/main/java/thkaguyamod/entity/item/EntityMarisaBroom.java`（下称 **新箒**）
  - 1.21.1 反编译源（本机只读）：`porting/neoforge-1.21.1/build/moddev/artifacts/neoforge-21.1.252-sources.jar` → `net/minecraft/...`、`net/neoforged/neoforge/...`（行号均指该 jar 内文件）
- 结论标记约定：**[实证]** = 本机源文件可核对；**[推断]** = 依赖 1.7.10 原版内部行为、本机无 1.7.10 客户端反编译源/映射，无法逐行核对；**[决策点]** = 需 K3 拍板。

---

## 1. 1.7.10 原作行为精确摘要

### 1.1 输入读取（客户端键位）

旧箒在实体内部直接读客户端键位（这正是 `MIGRATION_RULES.md` R3 要求修复的「公共代码引用 client-only 类」）：

| 键 | 访问器 | 行号（旧箒） |
|---|---|---|
| W 前进 | `isPushKeyUp()` → `keyBindForward.getIsKeyPressed()` | 222-227 |
| S 后退 | `isPushKeyBack()` → `keyBindBack` | 229-235 |
| D 右移 | `isPushKeyRight()` → `keyBindRight` | 237-243 |
| A 左移 | `isPushKeyLeft()` → `keyBindLeft` | 245-251 |
| 空格 | `isPushKeyJump()` → `keyBindJump` | 253-259 |

读取点：**只在 `worldObj.isRemote`（客户端）**，见 372-379；服务端保留初始值（365-369）：

```
move_up = true;            // 关键：服务端恒为 true
move_right = move_left = move_back = move_jump = false;
if (worldObj.isRemote) { move_up = isPushKeyUp(); ... move_jump = isPushKeyJump(); }
```

**[实证]** 即原作存在服务端/客户端**输入语义不一致**：服务端按「永远按着 W、从不按 S/A/D/空格」模拟，客户端按真实键位模拟；两者靠位置包与 `setPositionAndRotation2` 收敛。这是旧版的抖动手感来源，也是 R3 要求修复的 bug 形态。

### 1.2 数值与运动映射（旧箒 344-688，非客户端-空箒分支）

常量：

| 名称 | 值 | 出处 |
|---|---|---|
| 常规加速度 `acceleration` | `0.04D` | 395 |
| 后退加速度 `acceleration2` | `0.08D` | 396 |
| 悬停模式加速度 | `0.5D`（覆盖 `acceleration`） | 399 |
| `speedMultiplier`（无实际作用） | 0.6（运行时）/ 0.07（构造） | 41、483 |
| 水平基础阻力 | `motionX/Z *= 0.9900000095367432` | 584-586 |
| 垂直基础阻力 | `motionY *= 0.949999988079071` | 585 |
| 空键额外阻尼 | `motion *= 0.97` | 451-453 |
| 水中 | `motion *= 0.5`（三轴） | 556-560 |
| 碰撞盒 / yOffset | `setSize(0.9F, 0.5F)` / `yOffset = 0.8F` | 43-44 |
| 骑乘高度偏移 | `getMountedYOffset() = -0.15D` | 109-113 |

**分支 A：悬停模式（`move_jump == true`，397-444）** —— 先把三轴运动**清零**（403-405），再按 W/S 直接赋垂直速度、按 A/D 直接赋水平速度（**不是累加**）：

- W：`motionY = -sin(-90°) * 0.5 = +0.5`（412）
- S：`motionY = -sin(+90°) * 0.5 = -0.5`（420）
- D：`motionX = +sin(rotationYaw)*0.5`、`motionZ = -cos(rotationYaw)*0.5`（429-430）
- A：同上但 `rotationYaw - 180°`（434-435）
- 随后 `moveSpeed > 0.5` 时 `motion *= moveSpeed`（437-443，见 §1.4 缺陷）

**分支 B：常规模式（445-479）**

1. 俯仰升力（每 tick 无条件）：`motionY += -sin(rotationPitch) * 0.04`（447）
2. 无任何键：`motion *= 0.97`（448-454）
3. W：`motionX += sin(rotationYaw-90°)*0.04`、`motionZ += -cos(rotationYaw-90°)*0.04`（457-461）
4. S：同轴向但 `acceleration2 = 0.08`，且**符号相反**（462-466）——后退是前进的 2 倍加速度
5. D：`sin(rotationYaw+0°)*0.04 / -cos(...)*0.04`（468-472）
6. A：`rotationYaw-180°` 同式（473-477）

> 注意 `rotationYaw` 在此处是**上一 tick 的箒朝向**（= 骑手 yaw − 90，见 507），且 A/D 的 ±90° 偏移量（`+0F` / `-180F`）使横移方向相对「箒正前方（骑手左侧）」成左右 90°——最终效果等价于「沿骑手左右方向横移」。

**分支 C：朝向与骑手同步（481-516）**

- `speedMultiplier = 0.6`（483，赋了但随后无任何读取 → 死代码，与 520-547 被注释的旧速度逻辑配套）
- `rotationPitch = 骑手 pitch`（486-487）；`pitch>90/>-90` 的钳制（489-496）算完**未写回**（死代码）
- `pitch < 0 → pitch *= 0.1`（501-504）在赋值之后、**且从未使用该局部变量** → 死代码（对任何 `rotationPitch < 0` 的骑手，箒的俯仰**不受影响**）
- `rotationYaw = 骑手.rotationYaw - 90F`（507）
- `moveSpeed > 0.8` 时 `motion *= moveSpeed`（510-516，同 §1.4 缺陷）

**分支 D：收尾（555-615）**

1. 入水 `motion *= 0.5`（556-560）
2. `moveEntity(motionX, motionY, motionZ)`（562）
3. 无条件 `motionX/Z *= 0.99`、`motionY *= 0.95`（584-587）
4. `setRotation(rotationYaw, rotationPitch)`（614）→ 渲染用朝向

**分支 E：无骑手（仅服务端，665-687）**：`motionX/Z *= 0.9`、`motionY -= 0.03`、俯仰每 tick 向 0 靠 1°、`onGround` 时 `motionY += 0.06`。随后仍走 D 的入水/move/阻力。

**其他配套行为**

| 行为 | 出处 |
|---|---|
| 骑乘中 `rider.fallDistance = 0`、`fallDistance = 0`（两侧都执行于服务端块） | 665-669 |
| 骑手位置硬贴附：`rider.setPosition(posX, posY - 0.15 + rider.yOffset, posZ)`（d0=d1=0） | 696-705 |
| 空箒客户端插值：`boatPosRotationIncrements` 逐 tick 逼近 `broom*` 目标位姿 | 180-209、305-341 |
| `func_70270_d(boolean)`：写入 `isEmpty`，是本类插值分支的唯一外部开关 | 814-818 |
| 右键上车**只在服务端**执行 `entityPlayer.mountEntity(this)` | 748-763 |
| 攻击 4 点伤害后掉落 `magic_bloom` 并销毁 | 117-154 |
| 水中「半沉」AABB 扫描（283-296）算出的 `d0/d4` 被后续代码完全丢弃 | 283-302 **[实证]** 死代码 |

`isEmpty` 的外部写入者：**[推断]** 旧版原版 `EntityBoat` 同名的 `func_70270_d` 由实体跟踪包路径调用（模组是覆写 vanilla 方法，obf 名不可能自创）；本机无 1.7.10 客户端反编译源与 MCP 映射（`~/.gradle/caches/minecraft/.../mcp_stable/39/methods.csv` 无该条目），**无法给出 vanilla `file:line`**。该点不影响移植结论：1.21.1 的等价机制是 `lerpTo` + `isControlledByLocalInstance()` 守卫（见 §2.2）。

### 1.3 实际手感推演（对源文件手工迭代）

设 `pitch = 0`（平视）、水平只有 W：

- 每 tick `+0.04` → 一旦水平速度 `s > 0.5`，被 `s *= s`（437-443 或 510-516）拉回 → 速度在 0.2~0.5 区间循环；
- 结果：**巡航约 0.3~0.5 格/tick（≈6~10 格/s）**，这是旧版扫帚的常态手感上限；
- 悬停模式：垂直被置 +0.5，随即被同一规则压到 `0.5² = 0.25` → **±0.25 格/tick ≈ 5 格/s 稳定升降**；A/D 只有 0.04 分量，被同一规则按 0.5 比例缩掉 → **悬停模式横移几乎无效**（这是旧版的真实行为，不是笔误）；
- 空键（仅俯仰项 + 0.97 阻尼 + 0.95 垂直阻力）：俯仰 45° 上仰的平衡速度约 `+0.33 格/tick`（≈+6.6 格/s 爬升）；平视时缓慢下沉。
  → **这就是移植版现在「朝视线方向飞」的手感来源**：新箒 174-214 恰好等价于旧版「不按键」那一支（`look * 0.04` + 0.99/0.95 阻力），只丢了按键分支。

### 1.4 旧版缺陷清单（R3/§3 规则要求修复并注释）

| # | 位置（旧箒） | 缺陷 | 后果 |
|---|---|---|---|
| B1 | 437-443、510-516 | 意图是限速，实际写成 `motion *= moveSpeed`（乘的是**速度标量**） | 速度 >1 后**正反馈发散**：同按 W+S+A+D（每 tick 最多 +0.24）约十余 tick 后速度可越过 1 并指数放大；单键时表现却是「软上限」，所以从未被发现 |
| B2 | 489-504 | `pitch` 钳制与 `pitch *= 0.1` 都作用在**局部变量**上且 `rotationPitch` 已赋值 | 低头/抬头的俯仰**没有任何衰减或钳制**，且 `pitch<0` 的 ×0.1 完全无效 |
| B3 | 365-379 | 服务端恒 `move_up = true`；客户端读键位 | 服务端与客户端模拟不一致 → 抖动/rubber-band（R3 类 bug） |
| B4 | 483、520-547 | `speedMultiplier` 写入后无读取，旧速度逻辑整段注释 | 死代码 |
| B5 | 283-302 | 入水 AABB 扫描结果未使用 | 每 tick 5 次 AABB×Material 查询纯浪费 |
| B6 | 412/420/429/434 | 悬停模式用 `0.5 * sin()` 赋值（非累加）后再被 §1.4-B1 压回 0.25 | 悬停升降速度实际只有常数 0.25，与「0.5」的书写意图不符 |

---

## 2. 1.21.1 可行通道对比与选型

### 2.1 通道 a) 原版载具输入包（PlayerInput / `ServerboundPlayerInputPacket`）

**1.21.1 实证链路**：

| 环节 | 位置 | 内容 |
|---|---|---|
| 客户端采集 | `client/player/KeyboardInput.java:24-37` | `up/down/left/right/jumping/shiftKeyDown` 来自 `Options.keyUp/keyDown/keyLeft/keyRight/keyJump/keyShift`（`Options.java:506-511`）；`calculateImpulse` 在**两键同按时返回 0**（15-21） |
| 客户端写入自身 | `client/player/LocalPlayer.java:687-696` | `serverAiStep()` 且 `isControlledCamera()` 时 `xxa = input.leftImpulse; zza = input.forwardImpulse; jumping = input.jumping` |
| 发包 | `LocalPlayer.java:237-243` | **仅当 `isPassenger()`**：发 `ServerboundMovePlayerPacket.Rot` + `ServerboundPlayerInputPacket(xxa, zza, input.jumping, input.shiftKeyDown)`；根载具 != 自身且 `isControlledByLocalInstance()` 时**另发** `ServerboundMoveVehiclePacket` |
| 包体 | `network/protocol/game/ServerboundPlayerInputPacket.java:12-27` | `float xxa`（正=左横移）、`float zza`、bit0=jumping、bit1=shift |
| 服务端落地 | `server/network/ServerGamePacketListenerImpl.java:380-383` | `player.setPlayerInput(xxa, zza, jumping, sneaking)` |
| 落点 | `server/level/ServerPlayer.java:1247-1259` | **`if (this.isPassenger())`** 才写：`xxa/zza` 钳制到 [-1,1]；`jumping`（protected）与 `setShiftKeyDown` |
| 消费 | `world/entity/LivingEntity.java:2806-2810` | 骑手的 `aiStep` 末把 `xxa/zza *= 0.98` 后用于 travel |

**可得性结论 [实证]**：

- W/A/S/D：公共代码可直接读 `LivingEntity.xxa / zza`（`LivingEntity.java:224-226`，**public 字段**）——客户端由 687-696 写、服务端由 1247-1259 写；`javap` 复核为 public。
- **空格：公共代码读不到。** `LivingEntity.jumping` 是 `protected`，且**全树无 public/NeoForge getter**（`javap net.minecraft.world.entity.LivingEntity` 只有 `setJumping(boolean)`；`ILivingEntityExtension` 无相关方法）；实体不是骑手的子类，无法访问。客户端侧的 `LocalPlayer.input`（`LocalPlayer.java:140`）在 `net.minecraft.client.*`，**公共代码禁止引用**（R3 闸门）。
- 副带损失：W+S 或 A+D 同按时原版 `calculateImpulse` 归零（`KeyboardInput.java:15-21`），**丢失旧版「两键都算」的语义**（旧箒 410-425/457-477 是两段独立 `if`）。

**权威性 / 反作弊 [实证]**：

- `ServerboundPlayerInputPacket` 只喂给骑手的 `xxa/zza`，本身**不驱动载具**；载具位置由 `ServerboundMoveVehiclePacket` 驱动：`ServerGamePacketListenerImpl.java:398-401` 要求 `entity.getControllingPassenger() == player` 且 `entity == lastVehicle`；`:420-423` 「moved too quickly」阈值 `d10 - d9 > 100`（≈10 格/tick）→ 回发 `ClientboundMoveVehiclePacket` 强制回拉；`:447` 「moved wrongly」（碰撞修正偏差 >0.0625）同样回拉。
- 载具漂浮踢人：`:289-298` 用 `getMaximumFlyingTicks(entity)`，而 `:321-329` 在 `entity.getGravity() < 1e-5` 时返回 `Integer.MAX_VALUE`；扫帚继承 `Entity`（`Entity.java:1169-1174`：`getDefaultGravity()` 默认 0）→ **永不触发**。玩家漂浮踢人 `:271-275` 有 `!player.isPassenger()` 前置 → 骑乘中同样豁免。
- 结论：走原版载具通道 = **vanilla 船式「客户端权威 + 服务端位置校验」**，不会被踢，允许的最大伪造速度约 10 格/tick。

### 2.2 通道 b) NeoForge KeyMapping + 自建 serverbound Payload

本模组已有完整范例（可 1:1 套用）：

- 包定义 + 编解码 + handler：`porting/neoforge-1.21.1/src/main/java/thkaguyamod/network/SelectSanaeTradePayload.java:44-79`
- 注册：`thkaguyamod/init/ModNetworking.java:20-26`（`event.registrar(THKaguyaMod.MOD_ID)` + `playToServer`），发送端 `PacketDistributor.sendToServer(...)`（`MerchantSanaeScreen.java:132`）
- 框架事实：`PayloadRegistrar.java:31-35` 默认 `HandlerThread.MAIN`（自动包 `MainThreadPayloadHandler`，即 handler 在主线程跑）；`:129` `executesOn`、`:158` `optional`；`IPayloadContext.java:38-40` `enqueueWork`、`:53` `player()`。
- 键位注册若需要：`RegisterKeyMappingsEvent.java:28-31`（`event.register(KeyMapping)`，mod 总线、仅客户端）。

要点：**自建包是唯一能让服务端拿到「跳跃位」的通道**（2.1 已证公共代码无法读取该状态）；同时它天然支持 5 个独立位，能 100% 还原旧版 W+S/A+D 同按语义（`KeyboardInput.calculateImpulse` 归零的损失消失）。

代价与风险：

- 新增协议条目。NeoForge 21.1 里非 optional 载荷集合不一致会导致连接失败；本模组版本号用的是 modid（`ModNetworking.java:21`），客户端/服务端同版本发布即无风险，但与旧版本客户端连服不兼容（可 `optional()` 降级）。
- **不能据此做成「服务端权威」**：只要扫帚仍是本地骑手的控制载具，客户端每 tick 都会发 `ServerboundMoveVehiclePacket`（`LocalPlayer.java:242`），服务端 `:398-470` 会把它 `absMoveTo` 到客户端位置 —— 服务端**自行积分的结果会被客户端位置覆盖**；而真正做服务端权威必须让客户端 `isControlledByLocalInstance()` 为 false，那会同时关掉 `LocalPlayer:242` 的发包与客户端的本地预测，转而依赖 `handleMoveEntity/handleTeleportEntity` 的 `lerpTo`（`ClientPacketListener.java:621-639`、`:570-584`），骑手会吃到约 1 tick 的输入延迟 + 插值拖影（即复刻旧版抖动）。**因此本方案把 b) 定位为「输入镜像/未来权威化预留」，不是权威实现。**

### 2.3 通道 c) 混合（本次选型）

**选型：c) 混合。**

| 输入位 | 通道 | 理由 |
|---|---|---|
| W/A/S/D | 客户端本地读 `Options.keyUp/keyDown/keyLeft/keyRight`（client-only 类内），打包成位域写入箒实体字段，供**本地预测**直接使用 | 动作只在控制端积分（§3.2 Boat 模型），本地读键零延迟；同时保留独立 5 bit（不损失 W+S/A+D 同按语义） |
| 空格 | 同上（`Options.keyJump.isDown()`） | 公共代码无法读取该状态（2.1）；用 `KeyMapping.isDown()` 而非 `Input.jumping`，避开自动跳跃注入（`LocalPlayer.java:756` 在 `autoJumpTime>0` 时强制 `input.jumping = true`）与开界面时的残留（`Minecraft.java:1062` → `KeyMapping.releaseAll()`） |
| 5 bit 位域（镜像） | **自建 serverbound `BroomSteerPayload`**（Phase 2，见 §3.5） | 让服务端持有与客户端一致的输入状态：当前用于 deltaMovement 镜像（加固 `moved too quickly` 阈值，服务端在非控制端把 deltaMovement 归零后 `d9=0`，阈值退化为「10 格/服务器 tick」）、调试可观测性、以及未来服务端权威化的最小前置 |

**运动积分位置：控制端独占（vanilla Boat 模型）。** 门 `isControlledByLocalInstance()`（`Entity.java:3215-3217`）自动给出四种情形的正确分工：

| 情形 | `getControllingPassenger()` | `isControlledByLocalInstance()` | 谁积分 | 位置同步 |
|---|---|---|---|---|
| 本地骑手（自己骑） | 本地玩家 | **true** | 客户端 | 客户端每 tick 发 `MoveVehicle`；服务端 `:398-470` 校验并 `absMoveTo` |
| 服务端上的该骑手 | ServerPlayer | **false** | 不积分（`setDeltaMovement(ZERO)`，同 `Boat.java:299-315`） | 接收客户端 `MoveVehicle` |
| 其他玩家看别人骑 | 远端玩家 | false | 不积分 | 服务端 `ServerEntity` 按 `updateInterval(1)` 广播位姿 → `lerpTo` |
| 无人 | null | `isEffectiveAi()`：服务端 true / 客户端 false | **服务端**积分（旧箒的缓降分支） | 客户端 `lerpTo`（等价旧版空箒插值，296-341） |

**必须新增 `getControllingPassenger()` 覆写**——现状是返回 `null`（`Entity.java:3135-3137` 默认），导致：客户端 `isControlledByLocalInstance()` 为 **false** → 不发 `MoveVehicle`；服务端 `handleMoveVehicle` 的 `getControllingPassenger() == player` 校验永不成立 → **整条 vanilla 载具同步/校验链路处于关闭状态**，而新箒 `tick()` 无侧别门（174-232）→ 客户端与服务端**双重积分**、客户端又被 `lerpTo` 覆盖 → 这就是现在手感差的机制根因。

**不选的方案**：
- `PlayerRideableJumping` + `ServerboundPlayerCommandPacket`（马式跳跃）：客户端只在**松开**跳跃键的边沿发 `START_RIDING_JUMP`（`LocalPlayer.java:866-874`），1.21.1 客户端**从不发** `STOP_RIDING_JUMP`（全树仅在 `ServerGamePacketListenerImpl.java:1507` 被处理）→ 无法表达「按住=悬停」的持续状态；且会牵动客户端 `jumpableVehicle()` 蓄力逻辑（`LocalPlayer.java:857-886`）。
- 纯服务端权威 + 客户端纯插值：见 2.2，等于复刻旧版抖动，且需要自建 clientbound 位置包 + 预测/和解，改动面与风险都不属于发布前夜。

---

## 3. 选定方案的最小实现草图

### 3.1 新增/触碰文件清单

| # | 文件 | 动作 | 内容 |
|---|---|---|---|
| 1 | `thkaguyamod/entity/item/EntityMarisaBroom.java` | **改** | 新增 `getControllingPassenger()`；新增 `steerMask` 字段 + `setSteerMask/getSteerMask`；把 174-232 的双侧积分重构为「控制端独占积分」；按 §1 忠实移植按键分支 |
| 2 | `thkaguyamod/entity/item/BroomSteer.java` | **新（建议）** | 5 个位常量 + `public static Vec3 step(...)` 纯函数（把 §3.3 的运动数学抽出来，供 GameTest 直接测，见 §5） |
| 3 | `thkaguyamod/client/input/BroomSteerClient.java` | **新** | 客户端只读键位 → 位域；写本地箒的 `steerMask`；Phase 2 的边沿发包 |
| 4 | `thkaguyamod/client/THKaguyaClient.java` | **改（1 行）** | 注册 `ClientTickEvent.Pre` 监听（构造函数内 `NeoForge.EVENT_BUS.addListener(...)`） |
| 5 | `thkaguyamod/network/BroomSteerPayload.java` | **新（Phase 2）** | serverbound 5-bit 位域包，结构照抄 `SelectSanaeTradePayload` |
| 6 | `thkaguyamod/init/ModNetworking.java` | **改（1 行，Phase 2）** | `registrar.playToServer(BroomSteerPayload.TYPE, BroomSteerPayload.STREAM_CODEC, BroomSteerPayload::handle)` |
| 7 | `thkaguyamod/config/ModConfig.java` | **不改（推荐）** | 无需配置项，理由见 §4 |
| 8 | `thkaguyamod/item/ItemMarisaBroom.java` | **不改** | 上车仍只在服务端（`ItemMarisaBroom.java:50-60`，与旧箒 748-763 一致）；客户端 1 tick 后由 `ClientboundSetPassengersPacket` 生效，该 tick 内客户端 `isControlledByLocalInstance()` 为 false，**不会误跑无人分支** |

新增的两个 `thkaguyamod.network.*` / `thkaguyamod.client.*` 文件天然满足 R3 闸门（`grep '^import net.minecraft.client'` 只允许出现在 `client` 包）。

### 3.2 关键签名

```java
// 1) EntityMarisaBroom（common）
/** 控制载具语义开关：让 vanilla 的 MoveVehicle 收发/校验/lerp 守卫全部生效。 */
@Override
public LivingEntity getControllingPassenger() {
    return this.getFirstPassenger() instanceof Player p ? p : null;   // 旧箒只接受 EntityPlayer（360）
}

/** 操舵位域。仅客户端（本地骑手）经 BroomSteerClient 写入；服务端由 Phase 2 的包 handler 镜像。 */
private int steerMask;
public void setSteerMask(int mask) { this.steerMask = mask; }
public int getSteerMask()          { return this.steerMask; }

@Override
public void tick() {
    super.tick();                       // damage 动画计数、xo/yo/zo 等，保持现状
    if (this.isControlledByLocalInstance()) {
        this.simulateSteer();           // 唯一积分点：旧箒 360-614 的忠实移植
    } else {
        this.setDeltaMovement(Vec3.ZERO);   // 同 Boat.java:299-315
    }
    if (!this.level().isClientSide) {   // 保持现状：推挤、乘客死亡下车、fallDistance
        ...
    }
}

/** 纯数学入口（BroomSteer.step 的薄包装，便于 GameTest 直接调用）。 */
private void simulateSteer() {
    Player rider = this.getFirstPassenger() instanceof Player p ? p : null;
    if (rider == null) { BroomSteer.stepEmpty(this); return; }        // 旧箒 665-687
    BroomSteer.step(this, this.steerMask, rider.getYRot(), rider.getXRot());  // 旧箒 395-614
}
```

```java
// 2) BroomSteer（common，新）
public static final int FORWARD = 1, BACK = 2, LEFT = 4, RIGHT = 8, JUMP = 16;

/** 输入 + 骑手朝向 → 下一 tick 的 deltaMovement；不做 move()。 */
public static Vec3 step(Vec3 motion, int mask, float riderYaw, float riderPitch, boolean inWater);

/** 空箒缓降（旧箒 665-687）。 */
public static Vec3 stepEmpty(Vec3 motion, float xRot, boolean onGround, boolean inWater);
```

```java
// 3) BroomSteerClient（client-only，新） —— 见 §3.4
```

```java
// 4) BroomSteerPayload（Phase 2，common，新）
public record BroomSteerPayload(int mask) implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, "broom_steer");
    public static final CustomPacketPayload.Type<BroomSteerPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, BroomSteerPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, BroomSteerPayload::mask, BroomSteerPayload::new);

    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BroomSteerPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer sp
                && sp.getVehicle() instanceof EntityMarisaBroom broom) {
            broom.setSteerMask(payload.mask());
            // 可选：把客户端的运动镜像到服务端 deltaMovement，加固 handleMoveVehicle 的
            // "moved too quickly" 阈值（非控制端 deltaMovement 归零时 d9=0）。
        }
    }
}
```

### 3.3 `BroomSteer.step` 伪码（逐条对应旧箒行）

```
mx, my, mz = motion
accel = 0.04 ;  accel2 = 0.08
if (mask & JUMP) {
    accel = 0.5
    (mx,my,mz) = (0,0,0)                                   // 旧 403-405
    if (mask & FORWARD) my = -sin(-90°) * accel            // 旧 412  → +0.5
    if (mask & BACK)    my = -sin(+90°) * accel            // 旧 420  → -0.5
    if (mask & RIGHT) { mx =  sin(yaw)*accel; mz = -cos(yaw)*accel }      // 旧 429-430
    if (mask & LEFT)  { mx =  sin(yaw-180°)*accel; mz = -cos(yaw-180°)*accel } // 旧 434-435
    (mx,my,mz) = capHoriz(|(mx,my,mz)| , 0.5, motion)      // 旧 437-443（见下）
} else {
    my += -sin(pitch) * accel                              // 旧 447（pitch = 上一 tick 骑手俯仰）
    if (!(mask & (FORWARD|BACK|LEFT|RIGHT))) {
        (mx,my,mz) *= 0.97                                 // 旧 448-454
    } else {
        if (mask & FORWARD) { mx +=  sin(yaw-90°)*accel;  mz += -cos(yaw-90°)*accel }   // 旧 457-461
        if (mask & BACK)    { mx += -sin(yaw-90°)*accel2; mz +=  cos(yaw-90°)*accel2 }  // 旧 462-466
    }
    if (mask & RIGHT) { mx +=  sin(yaw)*accel;      mz += -cos(yaw)*accel }      // 旧 468-472
    if (mask & LEFT)  { mx +=  sin(yaw-180°)*accel; mz += -cos(yaw-180°)*accel }// 旧 473-477
}
// 旧 507：箒朝向 = 骑手 yaw - 90（渲染与 A/D 基准）；俯仰 = 骑手俯仰（钳制到 ±90）
yaw' = riderYaw - 90 ;  pitch' = clamp(riderPitch, -90, 90)
(mx,my,mz) = cap(|(mx,my,mz)| , 0.8, motion)               // 旧 510-516
if (inWater) (mx,my,mz) *= 0.5                             // 旧 556-560
// 旧 562 moveEntity → 由调用方执行 move(MoverType.SELF, ...)
(mx,my,mz) *= (0.99, 0.95, 0.99)                           // 旧 584-587
return motion

// cap(len, limit, raw):  len > limit → raw * (limit / len)          ← 建议实现（R3 缺陷修复）
//                      旧实现为 raw * len（旧 437-443 / 510-516）  ← B1 发散源，见 §5 决策点
```

### 3.4 `BroomSteerClient` 伪码

```java
// thkaguyamod/client/input/BroomSteerClient.java  —— 仅客户端
public final class BroomSteerClient {
    private static int lastSent = 0;

    /** ClientTickEvent.Pre：在 level.tickEntities() 之前（Minecraft.java:1798-1800 / 1868）。 */
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !(p.getVehicle() instanceof EntityMarisaBroom broom)) { lastSent = 0; return; }

        Options o = mc.options;
        int mask = (o.keyUp.isDown()    ? BroomSteer.FORWARD : 0)
                 | (o.keyDown.isDown()  ? BroomSteer.BACK    : 0)
                 | (o.keyLeft.isDown()  ? BroomSteer.LEFT    : 0)
                 | (o.keyRight.isDown() ? BroomSteer.RIGHT   : 0)
                 | (o.keyJump.isDown()  ? BroomSteer.JUMP    : 0);

        broom.setSteerMask(mask);                     // 本地预测直接可用
        if (mask != lastSent) {                       // Phase 2：仅变化时发包（+ 可选 10 tick 心跳）
            PacketDistributor.sendToServer(new BroomSteerPayload(mask));
            lastSent = mask;
        }
    }
}
```

注册（`THKaguyaClient` 构造函数内，1 行）：`NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, BroomSteerClient::onClientTickPre);`

要点：
- 用 `p.getVehicle()`（直接载具）而非 `getRootVehicle()`，与旧箒 `riddenByEntity` 语义一致。
- 用 `KeyMapping.isDown()`：自动处理按键重映射、开界面时 `releaseAll`（`Minecraft.java:1062`）、冲突上下文；**不要**用 `Input.jumping`（自动跳跃注入，`LocalPlayer.java:756`）。
- 时序正确性：`ClientTickEvent.Pre` 在 `Minecraft.tick()` 顶部、`level.tickEntities()`（`:1868`）之前触发；实体 tick 顺序恒为「载具 tick → `tickPassenger` 骑手」（`ClientLevel.java:296-308`），故箒在同 tick 内先读到当帧键位，再同步朝向。
- 服务端不需要该掩码即可正确工作（§2.3 表），OTA 兼容性最好；Phase 2 的包只增不改。

### 3.5 交付分期（发布前夜的最小风险切分）

- **Phase 1（发布阻断项，必须做）**：文件 1、2、3、4。运动数学 + 控制端独占积分 + `getControllingPassenger()` + 客户端键位桥。**不改协议**，改动面 2 新 + 2 改。
- **Phase 2（可选，冻结解除后）**：文件 5、6。位域镜像、反作弊阈值加固、未来权威化前置。
- 两期互不依赖：Phase 2 缺席时服务端行为与 Phase 1 完全一致（服务端不积分）。

---

## 4. 与既有「视线前进」的兼容 / 迁移策略

1. **默认行为 = 手动操舵**，直接替换现在的「朝视线前进」；**不提供配置项**。理由：
   - R4「玩家可见行为默认一致」——旧版没有开关，加开关=新行为面；
   - 旧版「视线前进」并不是被丢弃的特性：旧箒 447 的 `motionY += -sin(pitch)*0.04` 就是俯仰升力，忠实移植后**平视/仰视/俯视仍会改变轨迹**，只是不再「不按键也自己往前飞」。玩家从移植版迁移过来时感受到的是「必须按键」，与原版一致。
   - 若发布方坚持要 kill switch，可加 `ModConfig` 布尔 `"Broom Manual Steering"`（默认 true，4 行改动），列在 §6 决策点。
2. **已知问题 #1 需要改写**：`porting/USER_MIGRATION.md:148` 与 `:237` 现在写的是「暂为朝骑手视线方向前进…未完成」；本方案落地后应改为「已恢复手动操舵」，并把 §1.4-B1 的限速实现差异（若采纳修复版）记为新差异条目。**该文件属发布方所有，我未修改，交由 K3 更新。**
3. **旧存档**：箒实体无 NBT（`addAdditionalSaveData` 空，新箒 302-313），新增字段不落盘；无迁移负担。已放置的箒实体在加载后自动获得新行为。
4. **协议**：Phase 1 不改协议 → 与任何已分发客户端/服务端组合均可连；Phase 2 新增 serverbound 载荷会让新旧版本互连失败（NeoForge 非 optional 载荷集合校验），如需容忍则 `registrar.optional()`。
5. **第三人称/摄像机**：无需改动。`positionRider` 已硬贴附 `y - 0.15`（新箒 289-297，对应旧箒 696-705），摄像机取骑手眼部，与原版一致；箒的 `YRot = riderYaw - 90`、`XRot = riderPitch` 供 `RenderMarisaBroom` 使用（现有渲染器已按 `entityYaw`/`getXRot()` 绘制，`RenderMarisaBroom.java:50-75`）。
6. **离鞍/坠落**：保持 vanilla/旧版一致 —— 骑乘中骑手 `fallDistance = 0`（新箒 256-260 / 旧箒 665-669），`LivingEntity.rideTick()` 每 tick `resetFallDistance()`（`LivingEntity.java:2963-2968`）；潜行离鞍（`Player.java:347`、`Player.rideTick:510-515`，服务端 `wantsToStopRiding()` → `stopRiding()`，与旧版潜行下车一致）；离鞍后从当前高度正常坠落受伤，**不新增保护**。旧版未用潜行做升降键，故无键位冲突。

---

## 5. 风险与测试建议

### 5.1 风险清单

| # | 风险 | 依据 | 等级 / 处置 |
|---|---|---|---|
| R1 | 客户端权威 ⇒ 生存模式下扫帚等价「受限飞行」 | `handleMoveVehicle` 的 10 格/tick 上限（`:420`）；0 重力实体免除漂浮踢（`:321-329`） | 中。与原版船同级；发布说明「已知问题」需注明；Phase 2 才能收敛 |
| R2 | 卡顿时 `moved too quickly` 误判回拉 | 服务端非控制端 `deltaMovement` 归零 ⇒ `d9=0`，阈值退化为「10 格/服务器 tick」；一个服务器 tick 内处理多个 `MoveVehicle` 时 `vehicleFirstGoodX` 仍是旧值（`:283-288`、`:404-423`） | 低。自愈（回发 `ClientboundMoveVehiclePacket`）；Phase 2 镜像 deltaMovement 可加固 |
| R3 | 按键读取时序 1 tick 偏差 | `LocalPlayer.serverAiStep`（`:687-696`）在骑手 tick 内、箒 tick 之后；`LivingEntity:2806` 还有 `xxa/zza *= 0.98` | 低。设计上只用**独立位**（`KeyMapping.isDown()`）而非 `xxa/zza` 数值，天然免疫阻尼/缩放 |
| R4 | 自动跳跃 / 开界面导致虚假悬停 | `LocalPlayer.java:756`、`Minecraft.java:1062` | 低。显式规定读 `KeyMapping`，不读 `Input.jumping` |
| R5 | `getControllingPassenger()` 生效后行为面变化超预期 | `Entity.java:3215-3217`、`3844-3850`（`getKnownMovement` 改取骑手）、`LocalPlayer.java:1117`（`vehicleCanSprint` → `canSprint()` 默认 false） | 中。需实测：冲刺、统计（`checkRidingStatistics`，`:461-462`）、载具推挤 |
| R6 | 新旧箒速度分布差异（尤其采纳 B1 修复后） | §1.3 手工推演、§1.4-B1 | 中，**[决策点]**：忠实移植（保留 `motion *= speed`，手感=旧版，含发散隐患）vs 修复（`cap` 形式，手感略慢但安全）。建议：**采修复版 + 注释 + 迁移文档记差异**（R3 规则「发现旧版 bug 修复并注释」），并保留 0.5/0.8 两个数值上限以尽量贴合旧版 |
| R7 | 无人箒现在只在服务端积分 | §2.3 表 | 低，且是修正（现版双侧积分 + `lerpTo` 打架）。需回归确认：空箒缓慢下沉、落地弹跳（`onGround → motionY += 0.06`，旧 683-686） |
| R8 | GameTest 的 mock 玩家是否可作乘客 | `GameTestHelper.java:269-285`（`makeMockPlayer` 的 `isLocalPlayer()` 返回 **true**） | 低但需实现时验证：该 mock 未 `addFreshEntity`，`startRiding` 后可能需要显式 `level().addFreshEntity(player)` |

### 5.2 测试建议

**GameTest 可覆盖（服务端逻辑，纯静态可判定）** —— 建议置于 `porting/staging/gametest/thkaguyamod/test/`，套用现有 `@GameTest` 风格（参考 `DanmakuSpawnTest`、`DanmakuParamsCodecTest`）：

| 测试 | 步骤 | 断言 |
|---|---|---|
| `BroomSteerMathTest`（最高性价比） | 直接调 `BroomSteer.step(motion, mask, yaw, pitch, false)` 迭代 N 次 | ① 仅 FORWARD：速度稳定在 (0, cap] 且方向 = `sin(yaw-90°), -cos(yaw-90°)`；② JUMP：垂直 ≈ +cap 或 ±0.5 分支方向正确；③ JUMP+BACK：`my < 0`；④ JUMP+LEFT/RIGHT：水平 ±；⑤ 无键：`|motion|` 单调下降（俯仰 0）；⑥ **B1 回归**：全键按住 200 tick，`|motion|` 不超过设计的硬上限（修复版）/ 记录发散（忠实版） |
| `BroomGroundSteerTest` | `spawn(MARISA_BROOM)` → `makeMockPlayer(CREATIVE)` → `player.startRiding(broom)` → `broom.setSteerMask(FORWARD)` → tick 20 | 位移方向/量级落在预期区间；无异常；`getControllingPassenger() == player` |
| `BroomEmptySinkTest` | 无乘客，tick 40 | `getY()` 下降、水平速度衰减（服务端 `isControlledByLocalInstance()` 为 true ⇒ 走空箒分支，可测） |
| `BroomHoverTest` | mask = JUMP | `getY()` 上升；mask = JUMP\|BACK → 下降 |
| `BroomSteerPayloadCodecTest`（Phase 2） | `RegistryFriendlyByteBuf` 往返 | mask 原值往返（照 `DanmakuParamsCodecTest` 写法） |
| `BroomControlGateTest` | 断言 `getControllingPassenger()` 在无乘客时为 null、有玩家乘客时为该玩家 | 该门是四条同步链路的总开关，回归价值高 |

**GameTest 覆盖不到、必须人工验收的**（客户端预测、键位、时序、观感）：

1. 单人：上车后分别按住 W / S / A / D / 空格 / 空格+W / 空格+S / W+S / A+D，记录「手感是否符合旧版」（对照 §1.3）；
2. 第三人称视角：相机不被箒模型遮挡、箒朝向随骑手 yaw 旋转（`yaw - 90`）、俯仰随视角；
3. 离鞍与坠落：空中潜行离鞍 → 正常坠落受伤；骑乘中反复上下车不残留速度；
4. 撞墙：贴墙飞行时客户端与服务端位置是否收敛（观察是否出现回拉抖动，验证 R2）；
5. 水中：入水速度减半、可继续操控；
6. 局域网 + 专用服务端：A 客户端骑乘、B 客户端旁观，确认位置/朝向平滑（约 1 tick 滞后属预期）；专用服务端侧确认无 client-only 类加载（`smoke_test.sh` 双模式 + R3 grep 闸门）；
7. 骑乘中重启/重连：位置一致性（`MoveVehicle` 链路在重连后是否重新建立）。

---

## 6. 需 K3 拍板的决策点

1. **§1.4-B1 限速实现**：忠实（保留 `motion *= moveSpeed` 的发散行为）还是修复为 `cap` 形式？建议修复 + 文档记差异。
2. **§5.1-R1 客户端权威**：接受（船同级，推荐，发布说明标注），还是必须服务端权威（工期/风险显著更高）？
3. **Phase 2 是否随发布**：建议不随（Phase 1 已闭环玩家可见缺口，Phase 2 只增加镜像与未来能力）。
4. **是否需要 `ModConfig` kill switch**：建议不要；若要，4 行改动。
5. `USER_MIGRATION.md` §3/§5 与已知问题 #1 的文案更新（发布方所有，我未改）。