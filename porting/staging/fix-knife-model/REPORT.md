# fix-knife-model：FORM_KNIFE 刀弹接入 3D 模型（§5#4）+ P38 NaN 硬化

## 目标
1. 弹幕 `FORM_KNIFE`（女仆秘技等）由「程序化贴图弹降级渲染」改为原作语义的 **3D 银色小刀模型**
   （`ModelSilverKnife`，几何已核验、layer 已注册）。
2. P38：`client/renderer/living/RenderTHBoss.java:146-147` 的 `maxHP=0` → NaN 流入 HP 条 UV，加一行硬化。

## 1. 原作对照（1.7.10 → 1.21.1 逐行）

原作 `src/main/java/thKaguyaMod/client/render/shot/RenderTHShot.java:219-228`：

```java
case THShotLib.FORM_KNIFE:
    size2 = size * 3.0F;
    GL11.glScalef(size2, size2, size2);
    ModelBase modelSilverKnife;
    modelSilverKnife = new ModelSilverKnife();
    GL11.glRotatef( yaw,   0F, 1F, 0F);
    GL11.glRotatef(-thShot.rotationPitch, 1F, 0F, 0F);
    GL11.glRotatef(thShot.getAngleZ(), 0F, 0F, 1F);
    modelSilverKnife.render(thShot, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
    break;
```

写像（顺序与数值一字不改）：

| 原作 GL 调用 | 移植版 | 备注 |
|---|---|---|
| `size2 = size * 3.0F` | `float size2 = size * 3.0F;`（`renderKnifeShot` 首行） | 只有刀弹是 3 倍缩放 |
| `glScalef(size2,size2,size2)` | `ctx.pose.scale(size2, size2, size2)` | — |
| `glRotatef(yaw, 0,1,0)` | `ctx.pose.mulPose(Axis.YP.rotationDegrees(yaw))` | `yaw` = `renderTHShot(ctx, thShot, entityYaw, entityYaw, partialTick)` 的 `entityYaw`（原作 `doRender` 的 yaw） |
| `glRotatef(-rotationPitch, 1,0,0)` | `ctx.pose.mulPose(Axis.XP.rotationDegrees(-pitch))` | `pitch` = `thShot.getXRot()`（`renderTHShot` 内 `float pitch = thShot.getXRot();`，原作 `rotationPitch`） |
| `glRotatef(getAngleZ(), 0,0,1)` | `ctx.pose.mulPose(Axis.ZP.rotationDegrees(slope))` | `slope` = `thShot.getAngleZ()` |
| `render(..., 0,0,-0.1F, ...)` 的平移 | `ctx.pose.translate(0,0,-0.1F)` | 1.21.1 的 `ModelPart.render` 不接受偏移，故显式给 |
| `bindEntityTexture(thShot)`（`renderTHShot` 开头） | `RenderType.entityCutoutNoCull(ctx.texture)` | `ctx.texture` = `getTextureLocation(entity)`；FORM_KNIFE 分支返回 `SilverKnife_<色>.png`（与原作 1564-1587 行的色→表**逐 case 一致**，已核对 8 色全同） |
| `GL11.glDisable(GL_LIGHTING)`（renderTHShot 开头） | `LightTexture.FULL_BRIGHT` | 自发光，与其它弹一致 |

旋转顺序（Y → X → Z）与原作固定功能矩阵一致；`Axis.YP/XP/ZP.rotationDegrees` 与
`glRotatef(angle, axis)` 同语义（右手系、角度制）。

## 2. 改动清单

| 文件:行 | 改动 |
|---|---|
| `client/renderer/shot/RenderTHShot.java:8-20` | 新增 import：`RenderType`、`OverlayTexture`、`ModelSilverKnife`、`RenderSilverKnife` |
| `…/RenderTHShot.java:114-127` | 构造器内 **bake 一次**：`this.knifeModel = new ModelSilverKnife(context.bakeLayer(RenderSilverKnife.LAYER_LOCATION));` + `private final ModelSilverKnife knifeModel;`（台账 P2：禁止每帧 new；1.7.10 原作每帧 new 是为了旧 ModelBase，1.21.1 的 `ModelPart` bake 昂贵） |
| `…/RenderTHShot.java:357-366` | `case FORM_KNIFE` 由 `renderNormalShot(...)` 降级改为 `renderKnifeShot(ctx, size, yaw, pitch, thShot.getAngleZ())` |
| `…/RenderTHShot.java:439-482`（新增方法） | `public void renderKnifeShot(Ctx, float size, float yaw, float pitch, float slope)`：按上表写像，含完整 Javadoc 对照 |
| `client/renderer/living/RenderTHBoss.java:146-153` | P38：`float maxHealth = Math.max(danmakuMob.getMaxHealth(), 1.0F);`，`hp/hp2` 改用 `maxHealth`；注释登记「原作即如此的 0 除算，NaN UV 在批渲染有风险」 |

**未改**：`getTextureLocation` 的 FORM_KNIFE 色表（已与原作一致）、
`ModelSilverKnife`（几何已核验）、`RenderSilverKnife`（银刀实体渲染器，另一条链路）、
`THKaguyaClient`（layer 已注册，无需改）。

## 3. 编译结果

```
./gradlew compileJava --offline   →  BUILD SUCCESSFUL（0 错误）
（仅既有的 unchecked/deprecation 注记）
```

过程记录：期间有一次 `BUILD FAILED`，错误为
`init/ModDataComponents.java:68: 程序包 BuiltInRegistries 不存在`——该文件 mtime 13:25:22
（其他批次正在编辑，缺失 import），与本批次改动无关；约 1 分钟后该文件被修好
（mtime 13:25:38），复跑即 `BUILD SUCCESSFUL`。本批次只触及
`client/renderer/shot/RenderTHShot.java` 与 `client/renderer/living/RenderTHBoss.java`。

## 4. 实机目视点检项（渲染无 GameTest 覆盖）

1. 女仆秘技（`THSC_LittleBugStorm` 的 `setShotForm(FORM_LIGHT/FORM_RICE)` 之外，
   刀弹来源为 `FORM_KNIFE` 的符卡/道具）→ 应看到**立体的银色小刀**，不再是平面贴图弹。
2. 刀柄朝向：小刀应沿弹道指向，刀柄在**靠近使用者/发射点**的一侧
   （Y 旋转 = 弹道 yaw、X 旋转 = -pitch；若实机看到刀尖反向，需核对 `yaw` 是否该用
   `entityYaw` 还是 `thShot.getYRot()`——原作 `doRender` 的 `yaw` 参数即 1.21.1 的
   `entityYaw`，本实现按原作取 `entityYaw`）。
3. 尺寸：刀弹应约为普通小弹的 3 倍（`size * 3.0F`）。
4. 颜色：8 色刀（红/蓝/绿/黄/紫/水/橙/白）应各自贴对应贴图（`silverknife_*.png` 64×32）。
5. 弹道倾斜（`angleZ`/slope）应可见：侧倾的刀弹其刀刃绕自身 Z 轴倾斜。
6. 与银刀**实体**（右键投掷）同款外观（同一 `ModelSilverKnife` + 同族贴图）。

## 5. 遗留

- P38 只硬化了 `RenderTHBoss` 一处；`EntityDanmakuMob.moveDanmakuAttack(..., 0F, ...)` 的调用点
  （`EntityCirno:112` / `EntityRumia:95` / `EntityToziko:96` / `EntityWriggle:106` 等）本身未动
  （在 `entity/`，本批次不得触碰）。若要彻底消除 `maxHealth = 0`，需另立批次在
  `moveDanmakuAttack` 内 `Math.max(maxHP, 0F)` 语义化处理（会改变实体数值，需主控决定）。
- `RenderTHShot` 的 `Ctx` 复用字段（`# perf: B6`）现在同时被 `renderKnifeShot` 使用；
  该路径只读 `ctx.pose/buffers/texture`，不新增可变状态，与既有约定一致。
- 未跑 `runGameTestServer`（按批次指示留给主控统一回归）。
