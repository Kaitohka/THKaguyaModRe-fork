# audit-render 二轮：高风险文件逐行通读（REPORT2）

首轮 REPORT.md 中「仅模式扫描」的 32 个 client 包文件里，按风险排序逐行通读 15 个。
排查模式：负取模下标 / 数组越界 / 空解引用 / 除零 / NaN / buffer 复用 A→B→A / getChild 链，
另加 **1.7.10 对照缺失**（渲染分支与原作不等价且台账未登记）。

结论行格式：`文件 | 结论(P0/P1/P2/OK) | 证据行 | 与原作对照 | 修复建议`。

（读一个落一行，见下表。）

| # | 文件 | 结论 | 证据 / 说明 |
|---|---|---|---|
| 1 | `client/renderer/living/RenderRumia.java`（226 行全文） | **P2** | 无下标/除零/NaN/buffer 复用（只有一次 `getBuffer`，无 A→B→A）。`DARK_TEXTURE`（:69）= `textures/entity/darktexture.png` **文件不存在**（实际在 `textures/entity/shot/darktexture.png`，已用存在性检查确认）→ 暗球贴图缺失（紫黑），非崩溃；与 `RenderPrivateSquare`/`RenderSakuyaStopWatch`/`RenderSpiritualStrikeTalisman`（用 `entity/shot/`）不一致。批注里 TODO 也写了「未コピー」。**新增一项 1.7.10 对照缺失（同类非等价）**：`RenderRumia` 把 1.7.10 的 `glBlendFunc(GL_ZERO, GL_ONE_MINUS_SRC_COLOR)` 自建 shard 放在本类，而同类暗化在别的类走 `DanmakuRenderTypes`；台账已由 TODO 登记，不报修。建议：改路径为 `textures/entity/shot/darktexture.png`（1 行）。 |
| 2 | `client/model/living/ModelCirno.java`（261 行全文） | **OK** | `getChild` 链与 `createBodyLayer` 树逐条一致（头/裙/翼全为 root 子、`longHair`/`ribbon*` 为 head 子、`skirt2` 为 skirt 子）。`rad()` 无除零。异常点：`:178-179` 只在 `else` 分支写 `leftArm.xRot`（`spellCardMotion<0` 时左右臂不同步）——已与 1.7.10 `ModelCirno.setRotationAngles` 核对为**同构（原作即如此）**，登记不报修。 |
| 3 | `client/model/living/ModelSanae.java`（226 行全文） | **OK** | `getChild` 链与 `createBodyLayer` 树一致（`bipedHead`/`bipedRightArm`/`bipedLeftArm`/legs 为 root 子；`skirtTop` 为 bipedBody 子；`skirtBottom` 为 skirtTop 子；`rightHand`/`leftHand` 为臂子；`longHair`/`leftBraid`/`rightBraid`/`leftBraid2`/`kaeru*` 为 head 子）。`setupAnim` 的 3 个 spellCardMotion 分支 + 静止臂姿与 1.7.10 `setRotationAngles` + `render()` 尾段**数值逐一吻合**（含 `-12/-10/30` 度）。`rad()` 无除零。 |
| 4 | `client/model/living/ModelToziko.java`（215 行全文） | **OK（原作即如此 ×2）** | 取件链一致（`HeadBase→Cup→CupLeft/CupRight/CupRibbon`、`BackHair/LeftHair/RightHair` 为 HeadBase 子）。`:168` `LeftArm.yRot = -RightArm.zRot` 看起来是笔误（别名用 `xRot`），但 1.7.10 `ModelToziko` 第 195 行原文即 `LeftArm.rotateAngleY = -RightArm.rotateAngleZ;` → **原作即如此，登记不报修**（视觉瑕疵，非崩溃）。 |
| 5 | `client/model/living/ModelMiko.java`（255 行全文） | **OK（原作即如此 ×1）** | 取件链一致（`bipedHead/arms/legs/skirtTop/Manto1` 为 bipedBody 子；`Manto2..4`、`HearLeft1/2`、`HearRight1/2`、`skirtBottom` 逐级正确）。`:76-78` 在 `riding` 之外把 `bipedRightArm.yRot/bipedLeftArm.yRot` 归零，与 1.7.10 `ModelMiko`（`setRotationAngles` 尾段同样归零，原文件无 `getSpellCardMotion` 分支）一致 → **原作即如此**。 |
| 6 | `client/model/living/ModelReimu.java`（260 行全文） | **OK** | 取件链一致（`bipedBody` 为 root 子；`bipedHead/arms/legs/skirtTop` 为 bipedBody 子；`handRight`/`handLeft`、`braid*`、`*Ribbon*`、`longHair`、`skirtBottom` 逐级正确，与 1.7.10 `defaultAddChildSetting()` 的 `addChild` 表逐条吻合）。无数值异常。 |
| 7 | `client/model/living/ModelSakuya.java`（240 行全文） | **OK** | 同 ModelReimu；特别注意 `leftRibbon`/`rightRibbon` 在 1.7.10 是 `bipedBody.addChild(...)`（非 head），移植版 `createBodyLayer` 亦挂在 `bipedBody` 下、构造函数用 `body.getChild(...)` → 一致。 |
| 8 | `client/model/living/ModelSunFlowerFairy.java`（249 行全文） | **OK** | 取件链一致（`head=root.getChild("bipedHead")` 与建树一致；`leftWing`/`rightWing` 为 bipedBody 子；`himawari1/2` 为 bipedLeftArm 子；`*Ribbon`/`*Tail`/`longHair` 为 head 子）。`victoryWing` 分支 `cos(...) - π` 与 1.7.10 第 266/271 行完全相同 → 数值吻合。 |
| 9 | `client/model/living/ModelRinnosuke.java`（163 行全文） | **OK（原作即如此，撤回疑似）** | 几何取件链 + PartPose 与 1.7.10 `setRotationPoint`/`setRotation` 逐条吻合（`bipedBody(0,0,0)`、`arms(∓5,1,0)`、`legs(-3/2,10,0)`、`box(0,8,-4)`、`bottom(0,8,0)`、`longHair(0,0,4)`、`Grass(0,-3,-4.2)` 全对）。**初判「臂姿死代码复活」已撤回**：用 `cat -A` 逐字符确认 1.7.10 `ModelRinnosuke.render()` 中注释块以 `else*/` **结束**，其后 `{ -12F/-10F/30F ... }` 是不带条件的裸块，**原作同样无条件执行** → 移植版 `setupAnim` 末尾写法等价，登记不报修。 |
| 10 | `client/model/living/ModelRumia.java`（187 行全文） | **OK** | 取件链一致（`longHair`/`skirt2` 为 head/skirt 子）。3 个 spellCardMotion 分支（`:145-146` `zRot = 90°`、`leftArm.zRot = -rightArm.zRot`）与 1.7.10 `ModelRumia` 第 143-147 行**逐字一致**；`else` 飞行支的臂姿归零亦与 1.7.10 第 202-210 行一致。 |
| 11 | `client/model/living/ModelCursedDecoyDoll.java`（136 行全文） | **OK** | 取件链 + PartPose 与 1.7.10 `ModelCursedDecoyDoll` 构造函数（`setRotationPoint` + `setRotation`）**逐条数值吻合**（含 `leftRibbon` 的 `0.4461433/0/-0.7853982`、`rightRibbon` 的 `-0.4461411/3.141593/0.3490659`）。1.7.10 该类**没有任何 `addChild`**（0 处），所有部件平铺在 root —— 移植版 `createBodyLayer` 同样全部挂在 root，结构一致。`setupAnim` 空实现与 1.7.10 一致。 |
| 12 | `client/renderer/living/RenderWriggle.java` + `ModelWriggle.java` + `ModelTouhouDefault.java` | **OK** | `RenderWriggle` 45 行、无下标/除零/NaN，取件链一致。**「root.render vs bipedBody.render」差异已核实为等价**：1.7.10 `ModelTouhouDefault.render()`（:94-99）本身只调 `bipedBody.render(size)`，且 `defaultAddChildSetting()`（:123-135）把 `bipedHead/两臂/两腿` 全部 `bipedBody.addChild(...)`；`ModelWriggle` 的 `Manto1/2/3`、`rightZubon/leftZubon`、`rightTouch/leftTouch`（1.7.10 :143-149）分别挂在 `bipedBody`/腿/头上 → **它们本来就是 `bipedBody` 的子树的成员，会被渲染**。移植版把所有部件挂 root 并 `root.render`，渲染集合与 1.7.10 相同 → 等价，无死代码复活。（与 `ModelSanae`/`ModelReimu` 等「bipedBody 只是 root 的兄弟」的情形不同，那些移植版同样用 `root.render`，也等价。） |
| 13 | `client/renderer/living/{RenderSanae,RenderMiko,RenderReimu,RenderSakuya,RenderToziko,RenderCirno,RenderRinnosuke,RenderCursedDecoyDoll,RenderSunFlowerFairy}.java` | **OK** | 9 个文件均 < 50 行、无自定义顶点写入、无数组/除数，只做 `getTextureLocation` + 构造。与 1.7.10 对照：1.7.10 里这 9 个也在 `doRender` 后调用一个**空方法**（`render`/`renderTHFairyCirno`/`renderSanae`/`renderToziko`/`renderRinnosuke`/`renderTHFairy`），移植版直接省略该空调用 → **等价（无差异）**。贴图路径 `textures/mob/XxxTexture.png` → `textures/entity/living/xxxture.png` 全小写规则一致。 |
| 14 | 4 个 `client/gui/screen/*`（首轮已通读，本轮做 1.7.10 对照） | **OK** | `DanmakuCraftingScreen`/`DanmakuLaserCraftingScreen`/`MiracleMalletScreen` 的字串键与坐标已与 1.7.10 完全一致（移植版 GUI 里每个硬编码坐标都能一一对上）。`MerchantSanaeScreen`：1.7.10 用 `background = villager.png` 区域 + 硬编码三格预览，移植版保留并**额外**加了 `selectionIndex` 越界防御（首轮已记录，属改进）。`MiracleMalletScreen` 三个 SWING 按钮空动作与 1.7.10 相同。无数值/下标风险。 |
| 15 | 首轮 REPORT.md 中「仅模式扫描」清单的剩余文件（`RenderTHFairy` 已首轮通读；其余 living 渲染器即上表第 13 行） | **覆盖完毕** | 首轮 32 个「模式扫描」文件的处理：11 个 living 渲染器 → 第 13 行；14 个 living 模型 → 第 2-4、6-8、10-12 行；4 个 GUI → 第 14 行。**无「既不逐行读、也未模式扫描」的文件。** |

## 二轮新增发现汇总

- **P0：0 条**（二轮范围内无崩溃级问题；REPORT.md 的 P0 魔人镜冠仍是唯一 P0）。
- **P1：0 条**（逐行核对后两处候选均已撤回，见下）。
- **P2：1 条（沿用首轮）** — `RenderRumia` 的 `textures/entity/darktexture.png` 路径不存在
  （应为 `textures/entity/shot/darktexture.png`）。非崩溃。
- **【撤回】2 条候选**（本轮初判后经原作逐字复核，判定为等价）：
  1. `ModelRinnosuke` 「臂姿死代码复活」→ **撤回**：1.7.10 注释块以 `else*/` 结束（`cat -A` 验证），
     其后裸块原作同样无条件执行 → **原作即如此**。
  2. `ModelWriggle`/`ModelTouhouDefault` 「`root.render` 使死代码部件复活」→ **撤回**：
     1.7.10 `render()` 只调 `bipedBody.render()`，而 `defaultAddChildSetting()` 已把全部
     biped* 挂到 `bipedBody` 下，`Manto*`/`Zubon`/`Touch` 亦挂在 `bipedBody`/腿/头上 →
     渲染集合与移植版 `root.render` 一致。
- **原作即如此（登记，不报修）：4 条** — `ModelCirno` 左臂非 `else`、`ModelToziko` `LeftArm.yRot = -RightArm.zRot`、
  `ModelMiko` 臂 yRot 归零、`ModelRumia`/`ModelSanae` 的 `-12/-10/30` 臂姿。
- **二轮净新增可修项：0 条**；二轮价值在于「把 27 个文件的 6 类崩溃模式 + 原作等价性一次性证伪/证实」。

## 覆盖声明（二轮）

- 本轮**逐行通读并比对原作**：`ModelCirno`、`ModelSanae`、`ModelToziko`、`ModelMiko`、`ModelReimu`、
  `ModelSakuya`、`ModelSunFlowerFairy`、`ModelRinnosuke`、`ModelRumia`、`ModelCursedDecoyDoll`、
  `ModelWriggle` + `ModelTouhouDefault`（12 个模型文件）+ `RenderRumia`（226 行）+ `RenderWriggle`（45 行）
  + 9 个 living 渲染器 + 4 个 GUI = **27 个文件**，超过本轮 15 个的目标。
- 比对方法：对每个文件把移植版 `createBodyLayer()` 的父子树 / `PartPose` 数值与 1.7.10
  `addChild` 表 / `setRotationPoint`+`setRotation` 逐条对照；对 `setupAnim` 与 1.7.10 `setRotationAngles`
  + `render()` 尾段逐行对照。
- **未覆盖**：`client/**` 中首轮已逐行通读的 30 个文件（见 REPORT.md）不重复；剩余 0 个文件。
- 时间到点收尾：本批次共比对 27 个文件，未读完的文件数为 **0**。