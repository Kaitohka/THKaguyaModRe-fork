# 客户端渲染代码同族崩溃隐患普查（批次 audit-render）

范围：`porting/neoforge-1.21.1/src/main/java/thkaguyamod/client/**`（72 个 .java），
重点是 I7（颜色数组下标 = 动画帧奇偶）同族的崩溃模式：数组/List 下标、`getChild` 取件链、
null 解引用、除零、NaN、RenderType/Buffer 使用错误。

**总结（3 行）**

- **P0：1 条** — `client/renderer/RenderMazinkyoukan.java:109` 复用已被 `endBatch` 关闭的
  `opaqueBuffer`（渲染类型 A → B → A），必定抛 `IllegalStateException: Not building!` **客户端必崩**，
  已有实物崩溃报告（`run/crash-reports/crash-2026-10-03_10.24.31-client.txt`）。
- **P1：2 条** — （a）`RenderTHLaser.renderLightEffect` 的 `count % 2` 在延迟期为负 → 负 UV（I7 同族，但不越界）；
  （b）`RenderTHBoss` 用 `moveDanmakuAttack(..., 0F, ...)` 造成的 `0/0 = NaN` 流入 HP 条 UV。
- **P2：6 条** — 均是「原作即如此」的有界/OOB 无崩溃观察项 + 1 条贴图路径缺失（见 P2 表）。

---

## P0（崩溃级，必须修）

| 文件:行 | 模式 | 可达路径 | 严重度 | 置信度 | 最小修复建议 | 对照原作 |
|---|---|---|---|---|---|---|
| `client/renderer/RenderMazinkyoukan.java:71, 83, 109`（崩溃栈落点 `quad` = 同文件 129） | 模式 6：RenderType/Buffer 使用错误 —— **同一 `MultiBufferSource` 上 A→B→A** 重取渲染类型后继续用旧 `VertexConsumer` | 魔人经巻（`EntityMazinkyoukan`）被渲染即触发：71 行取 `MODE_OPAQUE`（左取手）→ 83 行取 `MODE_ADD_ONE_MINUS_SRC_COLOR`（正文，`BufferSource.getBuffer` 内部 `endBatch(lastSharedType)` 会 `builder.build()` 把前一个 builder 置 `building=false`）→ 109 行**复用旧变量** `opaqueBuffer` 画右取手 → `BufferBuilder.ensureBuilding()` 抛 `IllegalStateException: Not building!` | **P0** | **确认（有崩溃报告实物）** | 忠实于原作的顺序（取手 → 正文 → 取手），只需在 109 行前**重新取一次**同类型 buffer：`VertexConsumer opaqueBuffer2 = bufferSource.getBuffer(DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_OPAQUE));`，然后 `quad(opaqueBuffer2, ...)`（注意 `opaqueBuffer` 不可复用；重取会先 flush 正文批次，绘制顺序与 1.7.10 的 3 次 `tessellator.draw()` 完全一致）。 | 1.7.10 用 `Tessellator.instance` + 每段 `startDrawingQuads()/draw()` 立即模式，三次绘制互不干扰 → **原作无此问题，是移植引入缺陷**（同族于 I7） |

崩溃报告证据（原文）：

```
java.lang.IllegalStateException: Not building!
  at com.mojang.blaze3d.vertex.BufferBuilder.ensureBuilding(BufferBuilder.java:68)
  at com.mojang.blaze3d.vertex.BufferBuilder.addVertex(BufferBuilder.java:144)
  at thkaguyamod.client.renderer.RenderMazinkyoukan.quad(RenderMazinkyoukan.java:129)
  at thkaguyamod.client.renderer.RenderMazinkyoukan.render(RenderMazinkyoukan.java:109)   <- 复用 opaqueBuffer
  at thkaguyamod.client.renderer.RenderMazinkyoukan.render(RenderMazinkyoukan.java:30)
Entity Type: thkaguyamod:mazinkyoukan (thkaguyamod.entity.item.EntityMazinkyoukan)
```

机制旁证（1.21.1 源码，`~/.gradle/caches/neoformruntime/.../sourcesAndCompiledWithNeoForge_*.jar`
内 `net/minecraft/client/renderer/MultiBufferSource.java`）：

```java
public VertexConsumer getBuffer(RenderType renderType) {
    ...
    if (this.lastSharedType != null) { this.endBatch(this.lastSharedType); }   // 关掉上一个
    bufferbuilder = new BufferBuilder(this.sharedBuffer, ...);
    this.lastSharedType = renderType; ...
}
private void endBatch(RenderType renderType, BufferBuilder builder) { MeshData meshdata = builder.build(); ... }  // building=false
```

**全树同型扫描结果**：对 `client/**` + 整个 `thkaguyamod/**` 做了「同一方法内，变量在后续
`getBuffer()`（参数不同）之后仍被使用」的模式扫描，**只有 `RenderMazinkyoukan` 命中**，无其它同类残留。

---

## P1（疑似，建议修）

| 文件:行 | 模式 | 可达路径 | 严重度 | 置信度 | 最小修复建议 | 对照原作 |
|---|---|---|---|---|---|---|
| `client/renderer/shot/RenderTHLaser.java:608`（调用点 `:281`，延迟分支 `:265`） | 模式 1：`%` 作用于可能为负的 `getAnimationCount()` | 延迟期（`getAnimationCount() < 0` 且非 `EntityTHSetLaser`）→ `renderLightEffect(ctx, color % 8, size2, thLaser)`：`color % 8` 安全（`getShotId()` 经 `%8` 得 `0..7`，且调用前已 `%8`），但 `int count = thShot.getAnimationCount() % 2` 得 `-1`（Java 负取模），随后 `count * 32 / 64F = -0.5` → **UV 为负**。**不越界、不崩溃**（`count` 只进 UV，未用于数组下标）。 | P1 | 高（代码路径确定）/ 影响仅为贴图采样错误 | 与 I7 同族，忠实原作语义的正确写法是保留 UV 用途但消除负值：`int count = Math.abs(thShot.getAnimationCount()) % 2;`（1.7.10 原作此处 `count = count % 2` 同样会得负值，但 1.7.10 的 `getAnimationCount()` 在延迟期与移植版负值语义一致——**原作即如此**，故只是"照抄原来就不干净"，若要消除负 UV 需明确改动，属于可选项）。 | 原作 `RenderTHLaser.java:688` 同样是 `int count = thShot.getAnimationCount() % 2;` → **原作即如此**（不报为缺陷，仅记录） |
| `client/renderer/living/RenderTHBoss.java:146-147` | 模式 5：NaN 源流入顶点 | `renderTHBossStatus` 在 `getDanmakuPattern() != NOT_ATTACK` 时渲染；`EntityDanmakuMob.moveDanmakuAttack()` 多处以 `maxHP = 0F` 调用（`EntityCirno.java:112`、`EntityRumia.java:95`、`EntityToziko.java:96`、`EntityWriggle.java:106`、`EntityReika/Miko/Sakuya/Sanae` 的 `ATTACK_END`），`setBaseValue(0)` + `setHealth(0)` → `hp = 0/0 = NaN`、`hp2 = NaN`，NaN 作为 UV（`hp` 直接当 u 坐标）与顶点参数；随后 `poseStack.scale(size,...)` 中 `size` 正常。**不崩溃**，表现为 HP 条整条异常/不可见。 | P1 | 中（`maxHP=0F` 调用点确定；NaN 不是崩溃，属渲染污染） | 忠于原作：1.7.10 同样传 `0F`，**原作即如此**；若要稳妥，加 `float max = Math.max(danmakuMob.getMaxHealth(), 1.0F);` 再除（会轻微改变原行为，建议仅在确认 HP 条异常后再改）。 | 1.7.10 `RenderTHBoss` 用同样的 `getHealth()/getMaxHealth()` 且同样有 `0F` 调用 → **原作即如此** |

---

## P2（观察项，一句话）

| 文件:行 | 观察 | 严重度 | 置信度 | 结论 |
|---|---|---|---|---|
| `client/renderer/shot/RenderTHShot.java:304,320`（`getAnimationCount() % 2` → `renderLightShot` 的 `count`） | 与 I7 同族写法，但这些调用点在 `else`（`animationCount >= 0`）分支内，`count` 仅进 UV；`renderLightShot` 内 `int c = color % 8`（`:430`）用的是**颜色**而非 `count`，不会 AIOOBE | P2 | 高 | 无崩溃；I7 已修同类 |
| `client/renderer/shot/RenderTHShot.java:462`（`renderKishituShot` `int c = count % 2`）、`:809-811`（风弹 `count % 4`）、`:1115-1121`（幽霊 `count % 20`） | `count` 均来自非负分支，且只进 UV/缩放 | P2 | 高 | 无崩溃 |
| `client/renderer/living/RenderDivineSpirit.java:71-73`、`client/renderer/RenderSukima.java:116-118`、`client/renderer/effect/RenderMiracleCircle.java:124`、`client/renderer/effect/RenderSpellCardCircle.java:132-134`、`client/renderer/living/RenderTHPhantom.java:115-117` | `colorR[color]` 的 `color` 来自同步数据/NBT。可达值域实际有界（`RenderDivineSpirit` 0..7、`RenderSukima` `%32` 且构造用 `32+0..15`/16/17、`RenderMiracleCircle` 0..4、`RenderSpellCardCircle` 经 `%8`），但 `EntityDivineSpirit.readAdditionalSaveData` 的 `tag.getShort("color")`、`EntitySukima` 的 `tag.getShort("color")` 未做 clamp（同实体内 `playerTouch` 反而 clamp 了）→ 只有**被篡改/损坏的存档 NBT** 才越界 | P2 | 中 | **原作即如此**（1.7.10 同样从 dataWatcher/NBT 直取），不报为移植缺陷 |
| `client/renderer/living/RenderTHFairy.java:107`（`getDanmakuPattern() % 3`） | 若 `form >= 0` 而 `danmakuPattern < 0`，纹理名成 `fairytexture_-1.png` → 仅缺纹理（紫黑），非崩溃；本树中 `form=-1` 的僵尸妖精会直接走 `ZombieFairyTexture` 分支 | P2 | 中 | **原作即如此** |
| `client/renderer/living/RenderRumia.java:69`（`thkaguyamod:textures/entity/darktexture.png`） | 该路径在资源里**不存在**（实际文件在 `textures/entity/shot/darktexture.png`）；`RenderRumia` 的 TODO 已声明。表现是暗球贴图缺失（紫黑），非崩溃 | P2 | 高（已用文件存在性检查确认） | 资源问题，非崩溃；`RenderPrivateSquare`/`RenderSakuyaStopWatch`/`RenderSpiritualStrikeTalisman` 用的是 `textures/entity/shot/darktexture.png`（存在），只有 `RenderRumia` 不一致 |
| `client/renderer/RenderAjaRedStoneEffect.java:71`（`360F / (float)lightNum`） | `lightNum = getLightPower()^2`，若为 0 则 `angleSpan = NaN`，但 `for (i < 0)` 一次都不进循环（无顶点写入）；`lightPower` 来自 `getMaxLocalRawBrightness` ≤15 | P2 | 高 | 无实际 NaN 流入 |

---

## I5（getChild 取件链）专项：已修 12 个模型的残留复核

用脚本对 `client/model/**`（20 个文件）机械重建 `createBodyLayer()`/`createMesh()` 的父子树，再把
每个 `parent.getChild("name")` / `this.X.getChild("name")` 逐条比对（含循环生成的 `"side"+i` /
`"stick"+i` / `"handle"+i` 展开、以及 `HumanoidModel.createMesh` 注入的
`head/hat/body/right_arm/left_arm/right_leg/left_leg`）。**结果：0 条缺失取件**，包括
`ModelMarisaHat`/`ModelSuwakoHat` 的 `head→hat_base→{brim,hat_base2..4,ribbon_*}`、
`ModelSanae`/`ModelSunFlowerFairy` 的 `head=root.getChild("bipedHead")`（与 `createBodyLayer` 中
`root.addOrReplaceChild("bipedHead")` 一致）、`ModelToziko` 的 `HeadBase→Cup→Cup*`、
`ModelWriggle` 的 `Manto1..3` 链（均与树一致）。**I5 类无残留。**

注：`ModelMarisaHat#renderToBuffer` 只渲染 `head` 子树（帽子是 head 的子），
这与 1.7.10 的 `bipedHead.render()` 语义一致；`ModelTouhouDefault#part()` 保留了 1.7.10 的
null 宽容（未生成部件返回 null），但 `ModelTouhouDefault` 的 `setRotationAnglesTHKaguya` 里
`this.bipedBody.xRot` 等**未做 null 检查**（`bipedHead` 用了 `part()`，`bipedBody` 没有）——
只有 `ModelWriggle` 继承它，而 `ModelWriggle.createBodyLayer` 确实生成了所有 biped* 部件，
故当前不可达（P2，不报）。

## 其它已排除项（明确不报）

- `DanmakuRenderTypes`：`MODE_*` 4 值 / `getCulled` / `Key` record 缓存 / 自建
  `ONE_MINUS_SRC_COLOR_TRANSPARENCY` —— 无下标、无除零、无 null；所有 RenderType 用
  `POSITION_TEX_COLOR` + `getPositionTexColorShader`，`quad()` 里 `.setLight()` 是 no-op 但不报错。
- `ClientProxy` 侧 46 个 `ModEntities` **全部**有 `registerEntityRenderer`（脚本核对：缺失 0）；
  所有被使用的 `ModelLayerLocation` 都在 `THKaguyaClient` 注册（脚本核对：未注册 0）。
  `RenderSilverKnife` 指向 `ModelSilverKnife`，`RenderPose` 一致。
- 除零：`RenderCircular...` 类 `angleSpan = PI / zDivNum`（`zDivNum` 常量 8/9/13，`zDivNum-1` 亦非 0），
  `RenderMasterSparkLaser` 的 `zAngleDivNum` 16/64，`RenderRiceShotPart` 的 `zDivNum-1`（7/5）——均常量，无 0。
- 顶点表下标：`GUNGNIR_ORDER` max 29 ≤ `GUNGNIR_VERTEX` 29 行；`LAEVATEINN_ORDER` 1..52 ≤ 52 行；
  `LAEVATEINN_TIME_X_ROW = {6,8,39,44,50}` 全部 < 52，与 1.7.10 的 `sin` 行号逐一核对一致（B2 抽取无越界）。
- `RenderCirnoIceBox` 的 `p[i][j]` 是常量 6×4 表，`px/py/pz` 长 8 → 无越界。
- `Optional`/`orElse(null)`：客户端包里没有 `Optional` 解引用链；`RenderSpellCard` 的
  `SpellCardRegistry.getSpellCardModDomain(int)` 可能返回 `null`（未注册 ID），但
  `EntitySpellCard` 的编号来自已注册表（31 = PrincessUndine 在 1.7.10 也未注册、也不生成），
  且 1.7.10 同写法 → **原作即如此**，当前不可达（P2）。
- `PoseStack` push/pop 配平：脚本对 `client/renderer/**` 全部方法做 `pushPose/popPose` 计数比对，**0 条不配平**。
- `RenderMazinkyoukan` 的 `count` 经实体侧 `count = Math.min(count, 114)` 有界（`EntityMazinkyoukan.tick`），
  故 `i % 63 / i / 63` 的 UV 无越界（同 I7 的「帧计数进 UV」写法，但不进数组下标）。
- `NoopRenderer`、4 个 GUI 屏（`MerchantSanaeScreen` 已含越界防御 `selectionIndex` 检查）、
  `THKaguyaClient`、`ModClientExtensions`（`getHumanoidArmorModel` 只在 `HEAD` 槽 bake layer，
  layer 已注册）—— 无数组下标/除零/崩溃路径。
- `ModConfig.useTimeStopEffect` 在所有使用点都是布尔门，未作除数。

---

## 覆盖声明（普查完整性证据）

包内文件总数 **72**（`find client -name '*.java'`）。

**逐行通读（30 个）**：
`THKaguyaClient`、`ModClientExtensions`、
`client/renderer/NoopRenderer`、`RenderAjaRedStoneEffect`、`RenderCirnoIceBox`、`RenderDivineSpirit`、
`RenderHisou`、`RenderKinkakuzi`、`RenderMarisaBroom`、`RenderMazinkyoukan`、`RenderMiniHakkero`、
`RenderPendulum`、`RenderPrivateSquare`、`RenderSakuyaStopWatch`、`RenderShotMaterial`、`RenderSilverKnife`、
`RenderSpellCard`、`RenderSpiritualStrikeTalisman`、`RenderSukima`、`RenderTHItem`、`RenderYuukaParasol`、
`client/renderer/blockentity/DivineSpiritBlockEntityRenderer`、
`client/renderer/effect/{RenderHakureiShield,RenderHakurouReflecter,RenderMiracleCircle,RenderSpellCardCircle}`、
`client/renderer/living/{RenderTHBoss,RenderTHFairy,RenderFamiliar,RenderTHPhantom}`、
`client/renderer/shot/{DanmakuRenderTypes,RenderTHShot,RenderTHLaser}`（后三者含 1264/710/221 行全文）、
4 个 `gui/screen/*`。

**逐行通读的 model（10 个）**：`ModelHisou`、`ModelKinkakuzi`、`ModelMarisaBroom`、`ModelMarisaHat`、
`ModelMiniHakkero`、`ModelMiniHakkero2`、`ModelPrivateSquare`、`ModelSilverKnife`、`ModelSuwakoHat`、
`ModelYuukaParasol`。

**模式扫描覆盖（32 个，未逐行读但已过 6 类模式 grep / 结构脚本）**：
`client/renderer/living/{RenderCirno,RenderCursedDecoyDoll,RenderMiko,RenderReimu,RenderRinnosuke,
RenderRumia,RenderSakuya,RenderSanae,RenderSunFlowerFairy,RenderToziko,RenderWriggle}`、
`client/model/living/{ModelCirno,ModelCursedDecoyDoll,ModelMiko,ModelReimu,ModelRinnosuke,ModelRumia,
ModelSakuya,ModelSanae,ModelSunFlowerFairy,ModelTHFairy,ModelTHFairyCirno2,ModelTouhouDefault,
ModelToziko,ModelWriggle}` —— 这 14 个 living 模型 + 11 个 living 渲染器
**已用脚本重建父子树并逐条比对 getChild**（0 缺失），
并用 grep 覆盖：`%` 取模、`[变量]` 数组下标、`getChild`、`getAnimationCount`、`acos|asin|sqrt`、
`/ 变量` 除法、`Optional|orElse`、`getBuffer`/`pushPose|popPose` 配平。

**未逐行读、仅模式扫描的 0 个**：上表已含全部非通读文件。即「模式扫描覆盖」=
32 个文件（结构脚本 + 6 类模式 grep），无完全未扫描文件。

扫描所用关键命令（可复现）：
```bash
# I5 getChild 树比对（0 缺失）
python <script>   # 重建 createBodyLayer/addOrReplaceChild 树 vs 每个 getChild
# Buffer A→B→A 复用（全树只有 RenderMazinkyoukan 命中）
python <script>   # "var = ...getBuffer(...)" 后是否在另一次 getBuffer(...) 后仍被使用
# 模型图层注册完整性 / 实体渲染器完整性（0 缺失）
python <script>   # THKaguyaClient.layer(...) vs renderer/** 中的 ModelLayerLocation；ModEntities vs registerEntityRenderer
# push/pop 配平（0 不配平）
python <script>
```

（脚本为一次性分析脚本，未落盘到 `porting/staging/audit-render/` 之外；本批次只允许写本目录，
故脚本正文以命令说明形式记录，未修改任何既有文件。）

## 交接建议（给主控）

1. **必修**：`RenderMazinkyoukan.java:109` 重取 `VertexConsumer`（见 P0 行修复建议），修完再跑一次
   魔人経巻渲染即可复验（原崩溃报告：`run/crash-reports/crash-2026-10-03_10.24.31-client.txt`）。
   这是本批次唯一 P0，且与 I7 同属「数组/资源句柄类」移植引入缺陷。
2. **可选**：`RenderTHLaser:608` 的负 UV（同族但无害）、`RenderTHBoss` 的 NaN HP 条（原作即如此）。
3. **建议另立批次**：`RenderRumia` 的 `textures/entity/darktexture.png` 路径与其它 3 个使用
   `textures/entity/shot/darktexture.png` 的渲染器不一致（资源问题，非崩溃）。