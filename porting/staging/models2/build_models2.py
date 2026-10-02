"""models2 batch builder — 1.7.10 living-entity models -> NeoForge 1.21.1 (Mojmap).

Geometry (box coords / sizes / texture offsets / rotation points / part hierarchy) is
extracted mechanically from the 1.7.10 sources by _gen_geometry.py, so the numbers cannot
drift. Animation code (setupAnim and friends) is transcribed by hand below, one entry per
model, and lives next to the geometry so the whole file can be re-generated:

    python build_models2.py

1.7.10 -> 1.21.1 mapping used for the animation bodies:
    render(...)+setRotationAngles(...)   -> setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)
    moveCounter / movement               -> limbSwing
    moveCycle / far                      -> limbSwingAmount
    ticksExisted / tick / danmakuMob.ticksExisted -> ageInTicks
    pheadYaw / yaw / f3                  -> netHeadYaw
    pheadPitch / pitch / f4              -> headPitch
    MathHelper.<x>                       -> Mth.<x>
    rotateAngleX/rotateAngleY/rotateAngleZ -> xRot/yRot/zRot
    rotationPointX/Y/Z                   -> x/y/z
    ModelBase.onGround (swing progress)  -> this.attackTime
    ModelBase.isRiding                   -> this.riding
    entity.isSneaking()                  -> entity.isShiftKeyDown()
    entity.isRiding()                    -> this.riding

Every setupAnim starts with root.getAllParts().forEach(ModelPart::resetPose) because the
1.7.10 animation mutates rotation *points* (riding / sneaking poses) and relies on the
renderer re-applying the default pose every frame; resetPose() is the modern equivalent.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _gen_geometry as G

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "thkaguyamod", "client", "model", "living")

BASE_FIELDS = ["bipedHead", "bipedBody", "bipedRightArm", "bipedLeftArm",
               "bipedRightLeg", "bipedLeftLeg", "skirtTop", "skirtBottom",
               "longHair", "rightBraid", "leftBraid"]

IMPORTS_COMMON = """import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
"""

RAD = """    /** 度数をラジアンに変換（1.7.10 の rad() と同じ） */
    protected static float rad(float degree)
    {
        return degree / 180F * (float)Math.PI;
    }
"""

RESET = "        this.root.getAllParts().forEach(ModelPart::resetPose);"

MODELS = []


def M(name, **kw):
    kw["name"] = name
    MODELS.append(kw)


# =====================================================================================
# ModelTouhouDefault — 東方キャラモデル作成用のベースモデル
# =====================================================================================
M(
    name="ModelTouhouDefault",
    entity="EntityDanmakuMob",
    extends="EntityModel<T>",
    type_params="<T extends EntityDanmakuMob>",
    nullable=True,
    fields=BASE_FIELDS,
    doc="""/**
 * 東方キャラモデルの基底（1.7.10 {@code ModelTouhouDefault} の移植）。
 * 自身は箱を持たない抽象アンカーで、{@link ModelWriggle} がこの上に自分の
 * Techne データ（{@code getTechneModelData()} 相当）を組み立てる。
 * {@code T} は 1.7.10 の {@code EntityDanmakuMob} 引数に対応する。
 *
 * <p>モデルレイヤー名（呼び出し側）: 未登録。{@link ModelWriggle} が
 * {@code thkaguyamod:wriggle} を使う。</p>
 */
""",
    extra_fields="""    /** float型のPI（1.7.10 と同じ値） */
    protected static final float fPI = 3.141593F;

    /** 肩幅（身体の中心から腕までの長さ） */
    protected float shoulderWidth = 3.0F;
    /** 身体の高さ（座高） */
    protected float bodyHeight = 0.0F;
    /** 足の長さ */
    protected float legHeight = 0.0F;
""",
    skip_create_body_layer=True,
    setup="""    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        this.setRotationAnglesTHKaguya(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.setRotationAnglesAfterTHKaguya(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    /** スペルカード宣言時の右腕（1.7.10 setRotationAnglesAfterTHKaguya） */
    public void setRotationAnglesAfterTHKaguya(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if (entity.getSpellCardMotion() < -15)
        {
            this.bipedRightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin(rad(30F + entity.getSpellCardMotion())) * 4F;
        }
        else if (entity.getSpellCardMotion() < 0)
        {
            this.bipedRightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin(rad(-entity.getSpellCardMotion())) * 4F;
        }
    }

    /** パーツの回転（1.7.10 setRotationAnglesTHKaguya） */
    public void setRotationAnglesTHKaguya(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.setDefaultPause();

        this.bipedBody.xRot = 0.0F;
        this.bipedHead.yRot = netHeadYaw / 57.29578F;
        this.bipedHead.xRot = headPitch / 57.29578F;

        this.bipedRightArm.xRot = Mth.cos(limbSwing * 0.6662F + fPI) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedRightArm.yRot = rad(90F);
        this.bipedLeftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedLeftArm.yRot = rad(90F);

        this.bipedRightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 0.5F * limbSwingAmount;
        this.bipedLeftLeg.xRot = Mth.cos(limbSwing * 0.6662F + fPI) * 0.5F * limbSwingAmount;
        this.bipedRightLeg.yRot = 0.0F;
        this.bipedLeftLeg.yRot = 0.0F;
        this.bipedRightLeg.zRot = 0.0F;
        this.bipedLeftLeg.zRot = 0.0F;

        if (this.skirtTop != null)
        {
            this.skirtTop.xRot = 0.0F;
            if (this.skirtBottom != null)
            {
                this.skirtBottom.xRot = 0.0F;
            }
        }

        if (this.riding)
        {
            this.bipedBody.y = -1.0F;

            this.bipedRightArm.xRot += -rad(36F);
            this.bipedLeftArm.xRot += -rad(36F);
            this.bipedRightLeg.xRot = -rad(72F);
            this.bipedLeftLeg.xRot = -rad(72F);
            this.bipedRightLeg.yRot = rad(13F);
            this.bipedLeftLeg.yRot = -rad(13F);
            this.bipedRightLeg.zRot = rad(13F);
            this.bipedLeftLeg.zRot = -rad(13F);

            if (this.skirtTop != null)
            {
                this.skirtTop.xRot = -rad(20F);
                if (this.skirtBottom != null)
                {
                    this.skirtBottom.xRot = -rad(30F);
                }
            }

            this.InRidingMotion();
        }

        this.bipedRightArm.yRot = 0.0F;
        this.bipedLeftArm.yRot = 0.0F;
    }

    /** 座っているときに呼ばれるモーション処理（1.7.10 は空実装） */
    protected void InRidingMotion()
    {
    }

    /** パーツのデフォルト位置（1.7.10 setDefaultPause、位置のみ毎フレーム再適用） */
    protected void setDefaultPause()
    {
        if (this.bipedHead != null) this.bipedHead.setPos(0.0F, 0.0F, 0.0F);
        if (this.bipedBody != null) this.bipedBody.setPos(0.0F, -(this.bodyHeight + this.legHeight - 25F), 0.0F);
        if (this.bipedRightArm != null) this.bipedRightArm.setPos(-this.shoulderWidth, 2.0F, 0.0F);
        if (this.bipedLeftArm != null) this.bipedLeftArm.setPos(this.shoulderWidth, 2.0F, 0.0F);
        if (this.bipedRightLeg != null) this.bipedRightLeg.setPos(-2.0F, this.bodyHeight, 0.0F);
        if (this.bipedLeftLeg != null) this.bipedLeftLeg.setPos(2.0F, this.bodyHeight, 0.0F);
    }

    /** 東方キャラモデルの独自パラメータ（1.7.10 setTouhouModelData） */
    protected void setTouhouModelData()
    {
        this.shoulderWidth = 3.0F;
    }
""",
)

# =====================================================================================
# ModelWriggle — リグル
# =====================================================================================
M(
    name="ModelWriggle",
    entity="EntityWriggle",
    extends="ModelTouhouDefault<EntityWriggle>",
    skip_fields=BASE_FIELDS,
    extra_links=[("bipedBody", "bipedHead"), ("bipedBody", "bipedRightArm"),
                 ("bipedBody", "bipedLeftArm"), ("bipedBody", "bipedRightLeg"),
                 ("bipedBody", "bipedLeftLeg"), ("bipedHead", "longHair"),
                 ("bipedHead", "rightBraid"), ("bipedHead", "leftBraid")],
    doc="""/**
 * リグル（{@code EntityWriggle}）のモデル。1.7.10 {@code ModelWriggle} の移植。
 * 基底（{@link ModelTouhouDefault}）の {@code getTechneModelData()} を差し替えて
 * 自前の Techne データを組み立てる構造で、そのため 1.7.10 と同じく
 * biped 系パーツもこのクラスの createBodyLayer で定義する。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:wriggle}</p>
 */
""",
    ctor_super="        super(root);",
    ctor_tail="        this.setTouhouModelData();",
    setup="""    @Override
    protected void setTouhouModelData()
    {
        this.shoulderWidth = 2.4F;
        this.bodyHeight = 7F;
        this.legHeight = 12F;
    }

    @Override
    public void setRotationAnglesTHKaguya(EntityWriggle entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        super.setRotationAnglesTHKaguya(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // マント
        this.Manto1.xRot = (float)Math.sin((double)(limbSwing + ageInTicks) / 10.0D) * 0.1F + 0.3F;
        this.Manto2.xRot = (float)Math.sin((double)(limbSwing * 1.2F + ageInTicks) / 7.8D) * 0.25F + 0.25F;
        this.Manto3.xRot = (float)Math.sin((double)(limbSwing * 1.5F + ageInTicks) / 5.3D) * 0.3F + 0.20F;

        if (this.riding)
        {
        }
        else if (entity.getFlyingHeight() == 0)
        {
            this.bipedRightLeg.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount;
            this.bipedLeftLeg.xRot = Mth.cos(limbSwing + fPI) * 0.7F * limbSwingAmount;
            this.bipedRightLeg.zRot = 0F;
            this.bipedLeftLeg.zRot = 0F;

            if (limbSwing > 0F)
            {
                this.bipedRightArm.xRot = Mth.cos(limbSwing + fPI) * 2.0F * limbSwingAmount * 0.5F;
                this.bipedLeftArm.xRot = Mth.cos(limbSwing) * 2.0F * limbSwingAmount * 0.5F;
                this.bipedRightArm.yRot = rad(-10F);
                this.bipedRightArm.zRot = rad(20F);
                this.bipedLeftArm.yRot = -this.bipedRightArm.yRot;
                this.bipedLeftArm.zRot = -this.bipedRightArm.zRot;
            }
            else
            {
                this.bipedRightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F;
                this.bipedRightArm.yRot = 0.0F;
                this.bipedRightArm.zRot = -0.6457718F;
                this.bipedLeftArm.xRot = this.bipedRightArm.xRot;
                this.bipedLeftArm.yRot = 0.0F;
                this.bipedLeftArm.zRot = 0.6457718F;
            }
        }
        else
        {
            this.bipedRightLeg.zRot = Math.abs(Mth.sin(ageInTicks / 10F) * 0.1F);
            this.bipedLeftLeg.zRot = -this.bipedRightLeg.zRot;
            this.bipedRightLeg.xRot = Math.abs(Mth.sin(ageInTicks / 10F) * 0.2F);
            this.bipedLeftLeg.xRot = this.bipedRightLeg.zRot;

            this.bipedRightArm.xRot = rad(0F);
            this.bipedRightArm.zRot = rad(20F + Mth.sin(ageInTicks / 10F) * 5F);
            this.bipedLeftArm.xRot = rad(0F);
            this.bipedLeftArm.zRot = rad(-20F - Mth.sin(ageInTicks / 10F) * 5F);
        }
    }
""",
)

# =====================================================================================
# ModelCirno — チルノ
# =====================================================================================
M(
    name="ModelCirno",
    entity="EntityCirno",
    extends="EntityModel<EntityCirno>",
    doc="""/**
 * チルノ（{@code EntityCirno}）のモデル。1.7.10 {@code ModelCirno} の移植。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:cirno}</p>
 */
""",
    setup="""    @Override
    public void setupAnim(EntityCirno entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        this.head.yRot = netHeadYaw / (180F / (float)Math.PI);
        this.head.xRot = headPitch / (180F / (float)Math.PI);
        this.body.yRot = Mth.sin(Mth.sqrt(this.attackTime) * (float)Math.PI * 2.0F) * 0.2F;
        this.skirt.xRot = 0F;

        // スペルカードの宣言をする
        if (entity.getSpellCardMotion() < -15)
        {
            this.rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin((float)(30F + entity.getSpellCardMotion()) / 180F * 3.141593F) * 4F;
        }
        else if (entity.getSpellCardMotion() < 0)
        {
            this.rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin((float)(-entity.getSpellCardMotion()) / 180F * 3.141593F) * 4F;
        }
        else
        {
            this.rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F;
            this.leftArm.xRot = this.rightArm.xRot;
        }

        if (this.riding)
        {
            this.rightArm.xRot = -((float)Math.PI / 5F);
            this.leftArm.xRot = -((float)Math.PI / 5F);
            this.rightLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.leftLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.rightLeg.yRot = ((float)Math.PI / 14F);
            this.leftLeg.yRot = -((float)Math.PI / 14F);
            this.rightLeg.zRot = ((float)Math.PI / 14F);
            this.leftLeg.zRot = -((float)Math.PI / 14F);

            this.skirt.xRot += -((float)Math.PI / 5F);
        }
        else if (entity.getFlyingHeight() == 0)
        {
            this.rightLeg.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount;
            this.leftLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount;
            this.rightLeg.zRot = 0F;
            this.leftLeg.zRot = 0F;

            if (limbSwing > 0F)
            {
                this.rightArm.xRot = Mth.cos(limbSwing + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
                this.leftArm.xRot = Mth.cos(limbSwing) * 2.0F * limbSwingAmount * 0.5F;
                this.rightArm.yRot = -10F / 180F * (float)Math.PI;
                this.rightArm.zRot = 20F / 180F * (float)Math.PI;
                this.leftArm.yRot = -this.rightArm.yRot;
                this.leftArm.zRot = -this.rightArm.zRot;
            }
            else
            {
                this.rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F;
                this.rightArm.yRot = 0.0F;
                this.rightArm.zRot = -0.6457718F;
                this.leftArm.xRot = this.rightArm.xRot;
                this.leftArm.yRot = 0.0F;
                this.leftArm.zRot = 0.6457718F;
            }
        }
        else
        {
            this.rightLeg.zRot = Math.abs(Mth.sin(ageInTicks / 10F) * 0.1F);
            this.leftLeg.zRot = -this.rightLeg.zRot;
            this.rightLeg.xRot = Math.abs(Mth.sin(ageInTicks / 10F) * 0.2F);
            this.leftLeg.xRot = this.rightLeg.zRot;

            this.rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F;
            this.rightArm.yRot = 0.0F;
            this.rightArm.zRot = -0.6457718F;
            this.leftArm.xRot = this.rightArm.xRot;
            this.leftArm.yRot = 0.0F;
            this.leftArm.zRot = 0.6457718F;
        }

        this.body.xRot = 0F;
        if (entity.getFlyingHeight() > 0)
        {
            this.rightWing.yRot = Mth.cos(ageInTicks * 0.4F) * (float)Math.PI * 0.25F;
            this.leftWing.yRot = -this.rightWing.yRot;
            this.rightWing2.yRot = Mth.cos(ageInTicks * 0.4F) * (float)Math.PI * 0.25F;
            this.leftWing2.yRot = -this.rightWing2.yRot;
            this.rightWing3.yRot = Mth.cos(ageInTicks * 0.4F) * (float)Math.PI * 0.25F;
            this.leftWing3.yRot = -this.rightWing2.yRot;
        }
        else
        {
            this.rightWing.yRot = Mth.cos(ageInTicks * 0.1F) * (float)Math.PI * 0.1F + (float)Math.PI * 0.15F;
            this.leftWing.yRot = -this.rightWing.yRot;
            this.rightWing2.yRot = Mth.cos(ageInTicks * 0.1F) * (float)Math.PI * 0.1F + (float)Math.PI * 0.15F;
            this.leftWing2.yRot = -this.rightWing2.yRot;
            this.rightWing3.yRot = Mth.cos(ageInTicks * 0.1F) * (float)Math.PI * 0.1F + (float)Math.PI * 0.15F;
            this.leftWing3.yRot = -this.rightWing2.yRot;
        }
    }
""",
)

# =====================================================================================
# ModelRumia — ルーミア
# =====================================================================================
M(
    name="ModelRumia",
    entity="EntityRumia",
    extends="EntityModel<EntityRumia>",
    doc="""/**
 * ルーミア（{@code EntityRumia}）のモデル。1.7.10 {@code ModelRumia} の移植。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:rumia}</p>
 */
""",
    setup="""    @Override
    public void setupAnim(EntityRumia entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        this.head.yRot = netHeadYaw / (180F / (float)Math.PI);
        this.head.xRot = headPitch / (180F / (float)Math.PI);
        this.body.yRot = Mth.sin(Mth.sqrt(this.attackTime) * (float)Math.PI * 2.0F) * 0.2F;
        this.skirt.xRot = 0F;

        // スペルカードの宣言をする
        if (entity.getSpellCardMotion() < -15)
        {
            this.rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin((float)(30F + entity.getSpellCardMotion()) / 180F * 3.141593F) * 4F;
        }
        else if (entity.getSpellCardMotion() < 0)
        {
            this.rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin((float)(-entity.getSpellCardMotion()) / 180F * 3.141593F) * 4F;
        }
        else
        {
            this.rightArm.zRot = 90F / 180F * (float)Math.PI;
            this.leftArm.zRot = -this.rightArm.zRot;
        }

        if (this.riding)
        {
            this.rightArm.xRot = -((float)Math.PI / 5F);
            this.leftArm.xRot = -((float)Math.PI / 5F);
            this.rightLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.leftLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.rightLeg.yRot = ((float)Math.PI / 14F);
            this.leftLeg.yRot = -((float)Math.PI / 14F);
            this.rightLeg.zRot = ((float)Math.PI / 14F);
            this.leftLeg.zRot = -((float)Math.PI / 14F);

            this.skirt.xRot += -((float)Math.PI / 5F);
        }
        else if (entity.getFlyingHeight() == 0)
        {
            this.rightLeg.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount;
            this.leftLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount;
            this.rightLeg.zRot = 0F;
            this.leftLeg.zRot = 0F;

            if (limbSwing > 0F)
            {
                this.rightArm.xRot = Mth.cos(limbSwing + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
                this.leftArm.xRot = Mth.cos(limbSwing) * 2.0F * limbSwingAmount * 0.5F;
                this.rightArm.yRot = -10F / 180F * (float)Math.PI;
                this.rightArm.zRot = 20F / 180F * (float)Math.PI;
                this.leftArm.yRot = -this.rightArm.yRot;
                this.leftArm.zRot = -this.rightArm.zRot;
            }
            else
            {
                this.rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F;
                this.rightArm.yRot = 0.0F;
                this.rightArm.zRot = -0.6457718F;
                this.leftArm.xRot = this.rightArm.xRot;
                this.leftArm.yRot = 0.0F;
                this.leftArm.zRot = 0.6457718F;
            }
        }
        else
        {
            this.rightLeg.zRot = Math.abs(Mth.sin(ageInTicks / 10F) * 0.1F);
            this.leftLeg.zRot = -this.rightLeg.zRot;
            this.rightLeg.xRot = Math.abs(Mth.sin(ageInTicks / 10F) * 0.2F);
            this.leftLeg.xRot = this.rightLeg.zRot;

            this.rightArm.zRot = 90F / 180F * (float)Math.PI;
            this.rightArm.yRot = 0.0F;
            this.rightArm.xRot = 0.0F;
            this.leftArm.zRot = -this.rightArm.zRot;
            this.leftArm.yRot = 0.0F;
            this.leftArm.xRot = 0.0F;
        }
    }
""",
)

# =====================================================================================
# ModelReimu — 霊夢
# =====================================================================================
M(
    name="ModelReimu",
    entity="EntityReimu",
    extends="EntityModel<EntityReimu>",
    extra_fields="""    protected float legHeight = 12F;
    protected float bodyHeight = 9F;
""",
    doc="""/**
 * 霊夢（{@code EntityReimu}）のモデル。1.7.10 {@code ModelReimu} の移植。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:reimu}</p>
 */
""",
    setup="""    @Override
    public void setupAnim(EntityReimu entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        this.setDefaultPause();
        this.bipedBody.y = 5F;

        this.bipedRightLeg.x = -2.0F;
        this.bipedLeftLeg.x = 2.0F;
        this.bipedHead.y = -1.0F;
        this.bipedHead.yRot = netHeadYaw / 57.29578F;
        this.bipedHead.xRot = headPitch / 57.29578F;

        this.bipedRightArm.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedLeftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedRightArm.zRot = 30F / 180F * 3.141593F;
        this.bipedLeftArm.zRot = -30F / 180F * 3.141593F;
        this.skirtTop.xRot = 0.0F;
        this.skirtBottom.z = 0.0F;
        this.skirtBottom.xRot = 0.0F;
        this.bipedLeftArm.y = -1F;
        this.bipedRightArm.y = -1F;

        if (entity.isShiftKeyDown())
        {
            this.bipedBody.yRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.4F * limbSwingAmount;
            this.skirtTop.yRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.4F * limbSwingAmount;
            this.bipedRightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
            this.bipedLeftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        }

        this.bipedRightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 0.5F * limbSwingAmount;
        this.bipedLeftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 0.5F * limbSwingAmount;
        this.bipedRightLeg.yRot = 0.0F;
        this.bipedLeftLeg.yRot = 0.0F;
        this.bipedRightLeg.zRot = 0.0F;
        this.bipedLeftLeg.zRot = 0.0F;

        if (this.riding)
        {
            this.bipedBody.y = -1F;
            this.bipedRightLeg.y = 5.0F;
            this.bipedLeftLeg.y = 5.0F;
            this.bipedRightArm.xRot += -((float)Math.PI / 5F);
            this.bipedLeftArm.xRot += -((float)Math.PI / 5F);
            this.bipedRightLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.bipedLeftLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.bipedRightLeg.yRot = ((float)Math.PI / 14F);
            this.bipedLeftLeg.yRot = -((float)Math.PI / 14F);
            this.bipedRightLeg.zRot = ((float)Math.PI / 14F);
            this.bipedLeftLeg.zRot = -((float)Math.PI / 14F);

            this.skirtTop.y = 5F;
            this.skirtTop.z = 0F;
            this.skirtTop.xRot = -((float)Math.PI / 5F);
            this.skirtBottom.z = 0.0F;
            this.skirtBottom.y = 4F;
            this.skirtBottom.xRot = -((float)Math.PI / 4F);
        }
        else
        {
            this.skirtTop.y = 5F;
            this.skirtTop.z = 0F;
            this.skirtBottom.z = 0.0F;
            this.skirtBottom.y = 4.0F;
            this.bipedRightLeg.y = 7.0F;
            this.bipedLeftLeg.y = 7.0F;
        }

        this.bipedRightArm.yRot = 0.0F;
        this.bipedLeftArm.yRot = 0.0F;

        this.bipedRightLeg.z = 0.0F;
        this.bipedLeftLeg.z = 0.0F;
        this.bipedBody.xRot = 0.0F;
        this.bipedBody.zRot = 0.0F;

        if (entity.isShiftKeyDown())
        {
            this.bipedHead.xRot -= 0.5F;
        }
    }

    /** 1.7.10 setDefaultPause（位置のみ毎フレーム再適用） */
    public void setDefaultPause()
    {
        if (this.bipedHead != null) this.bipedHead.setPos(0.0F, 5.0F, 0.0F);
        if (this.bipedBody != null) this.bipedBody.setPos(0.0F, -5.0F, 0.0F);
        if (this.bipedRightArm != null) this.bipedRightArm.setPos(-3.0F, 6.0F, 0.0F);
        if (this.bipedLeftArm != null) this.bipedLeftArm.setPos(3.0F, 6.0F, 0.0F);
        if (this.bipedRightLeg != null) this.bipedRightLeg.setPos(-2.0F, this.bodyHeight + 5F, 0.0F);
        if (this.bipedLeftLeg != null) this.bipedLeftLeg.setPos(2.0F, this.bodyHeight + 5F, 0.0F);
    }
""",
)

# =====================================================================================
# ModelSakuya — 咲夜
# =====================================================================================
M(
    name="ModelSakuya",
    entity="EntitySakuya",
    extends="EntityModel<EntitySakuya>",
    extra_fields="""    protected float legHeight = 14F;
    protected float bodyHeight = 9F;
""",
    doc="""/**
 * 咲夜（{@code EntitySakuya}）のモデル。1.7.10 {@code ModelSakuya} の移植。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:sakuya}</p>
 */
""",
    setup="""    @Override
    public void setupAnim(EntitySakuya entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        this.setDefaultPause();

        this.bipedRightLeg.x = -2.0F;
        this.bipedLeftLeg.x = 2.0F;
        this.bipedHead.yRot = netHeadYaw / 57.29578F;
        this.bipedHead.xRot = headPitch / 57.29578F;

        this.bipedRightArm.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedLeftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedRightArm.zRot = 30F / 180F * 3.141593F;
        this.bipedLeftArm.zRot = -30F / 180F * 3.141593F;
        this.skirtTop.xRot = 0.0F;
        this.skirtBottom.z = 0.0F;
        this.skirtBottom.xRot = 0.0F;

        if (entity.isShiftKeyDown())
        {
            this.bipedBody.yRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.4F * limbSwingAmount;
            this.skirtTop.yRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.4F * limbSwingAmount;
            this.bipedRightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
            this.bipedLeftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        }

        this.bipedRightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 0.5F * limbSwingAmount;
        this.bipedLeftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 0.5F * limbSwingAmount;
        this.bipedRightLeg.yRot = 0.0F;
        this.bipedLeftLeg.yRot = 0.0F;
        this.bipedRightLeg.zRot = 0.0F;
        this.bipedLeftLeg.zRot = 0.0F;

        if (this.riding)
        {
            this.bipedBody.y = -1F;
            this.bipedRightLeg.y = 5.0F;
            this.bipedLeftLeg.y = 5.0F;
            this.bipedRightArm.xRot += -((float)Math.PI / 5F);
            this.bipedLeftArm.xRot += -((float)Math.PI / 5F);
            this.bipedRightLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.bipedLeftLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.bipedRightLeg.yRot = ((float)Math.PI / 14F);
            this.bipedLeftLeg.yRot = -((float)Math.PI / 14F);
            this.bipedRightLeg.zRot = ((float)Math.PI / 14F);
            this.bipedLeftLeg.zRot = -((float)Math.PI / 14F);

            this.skirtTop.y = 5F;
            this.skirtTop.z = 0F;
            this.skirtTop.xRot = -((float)Math.PI / 5F);
            this.skirtBottom.z = 0.0F;
            this.skirtBottom.y = 4F;
            this.skirtBottom.xRot = -((float)Math.PI / 4F);
        }
        else
        {
            this.skirtTop.y = 5F;
            this.skirtTop.z = 0F;
            this.skirtBottom.z = 0.0F;
            this.skirtBottom.y = 4.0F;
            this.bipedRightLeg.y = this.bodyHeight;
            this.bipedLeftLeg.y = this.bodyHeight;
        }

        this.bipedRightArm.yRot = 0.0F;
        this.bipedLeftArm.yRot = 0.0F;

        this.bipedRightLeg.z = 0.0F;
        this.bipedLeftLeg.z = 0.0F;
        this.bipedBody.xRot = 0.0F;
        this.bipedBody.zRot = 0.0F;

        if (entity.isShiftKeyDown())
        {
            this.bipedHead.xRot -= 0.5F;
        }
    }

    /** 1.7.10 setDefaultPause（位置のみ毎フレーム再適用） */
    public void setDefaultPause()
    {
        if (this.bipedHead != null) this.bipedHead.setPos(0.0F, 0.0F, 0.0F);
        if (this.bipedBody != null) this.bipedBody.setPos(0.0F, 1.0F, 0.0F);
        if (this.bipedRightArm != null) this.bipedRightArm.setPos(-3.0F, 1.0F, 0.0F);
        if (this.bipedLeftArm != null) this.bipedLeftArm.setPos(3.0F, 1.0F, 0.0F);
        if (this.bipedRightLeg != null) this.bipedRightLeg.setPos(-2.0F, this.bodyHeight, 0.0F);
        if (this.bipedLeftLeg != null) this.bipedLeftLeg.setPos(2.0F, this.bodyHeight, 0.0F);
    }
""",
)

# =====================================================================================
# ModelMiko — 神子
# =====================================================================================
M(
    name="ModelMiko",
    entity="EntityMiko",
    extends="EntityModel<EntityMiko>",
    extra_fields="""    protected float legHeight = 12F;
    protected float bodyHeight = 9F;
""",
    doc="""/**
 * 神子（{@code EntityMiko}）のモデル。1.7.10 {@code ModelMiko} の移植。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:miko}</p>
 */
""",
    setup="""    @Override
    public void setupAnim(EntityMiko entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        this.setDefaultPause();
        this.bipedBody.xRot = 0.0F;
        this.bipedHead.yRot = netHeadYaw / 57.29578F;
        this.bipedHead.xRot = headPitch / 57.29578F;

        this.bipedRightArm.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedRightArm.yRot = 90F / 180F * 3.141593F;
        this.bipedLeftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedLeftArm.yRot = 90F / 180F * 3.141593F;
        this.bipedRightArm.zRot = 20F / 180F * 3.141593F;
        this.bipedLeftArm.zRot = -20F / 180F * 3.141593F;

        this.bipedRightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 0.5F * limbSwingAmount;
        this.bipedLeftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 0.5F * limbSwingAmount;
        this.bipedRightLeg.yRot = 0.0F;
        this.bipedLeftLeg.yRot = 0.0F;
        this.bipedRightLeg.zRot = 0.0F;
        this.bipedLeftLeg.zRot = 0.0F;

        this.skirtTop.xRot = 0.0F;
        this.skirtBottom.xRot = 0.0F;

        // マント
        this.Manto1.xRot = (float)Math.sin((double)(limbSwing + ageInTicks) / 10.0D) * 0.1F + 0.3F;
        this.Manto2.xRot = (float)Math.sin((double)(limbSwing * 1.2F + ageInTicks) / 7.8D) * 0.25F + 0.25F;
        this.Manto3.xRot = (float)Math.sin((double)(limbSwing * 1.5F + ageInTicks) / 5.3D) * 0.3F + 0.20F;
        this.Manto4.xRot = (float)Math.sin((double)(limbSwing * 2.0F + ageInTicks) / 4.1D) * 0.34F + 0.0F;

        if (this.riding)
        {
            this.bipedBody.y = -1.0F;
            this.bipedRightArm.xRot += -((float)Math.PI / 5F);
            this.bipedLeftArm.xRot += -((float)Math.PI / 5F);
            this.bipedRightLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.bipedLeftLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.bipedRightLeg.yRot = ((float)Math.PI / 14F);
            this.bipedLeftLeg.yRot = -((float)Math.PI / 14F);
            this.bipedRightLeg.zRot = ((float)Math.PI / 14F);
            this.bipedLeftLeg.zRot = -((float)Math.PI / 14F);

            this.skirtTop.xRot = -(20F / 180F * (float)Math.PI);
            this.skirtBottom.xRot = -(30F / 180F * (float)Math.PI);
        }

        this.bipedRightArm.yRot = 0.0F;
        this.bipedLeftArm.yRot = 0.0F;
    }

    /** 1.7.10 setDefaultPause（位置のみ毎フレーム再適用） */
    public void setDefaultPause()
    {
        if (this.bipedHead != null) this.bipedHead.setPos(0.0F, 0.0F, 0.0F);
        if (this.bipedBody != null) this.bipedBody.setPos(0.0F, 3.0F, 0.0F);
        if (this.bipedRightArm != null) this.bipedRightArm.setPos(-3.0F, 1.0F, 0.0F);
        if (this.bipedLeftArm != null) this.bipedLeftArm.setPos(3.0F, 1.0F, 0.0F);
        if (this.bipedRightLeg != null) this.bipedRightLeg.setPos(-2.0F, this.bodyHeight, 0.0F);
        if (this.bipedLeftLeg != null) this.bipedLeftLeg.setPos(2.0F, this.bodyHeight, 0.0F);
    }
""",
)

# =====================================================================================
# ModelSanae — 早苗
# =====================================================================================
M(
    name="ModelSanae",
    entity="EntitySanae",
    extends="EntityModel<EntitySanae>",
    doc="""/**
 * 早苗（{@code EntitySanae}）のモデル。1.7.10 {@code ModelSanae} の移植。
 * 1.7.10 は render() の中で setRotationAngles() の後にスペルカード宣言ポーズを
 * 上書きしていたため、この setupAnim も同じ順序で適用する。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:sanae}</p>
 */
""",
    setup="""    @Override
    public void setupAnim(EntitySanae entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        this.skirtTop.xRot = 0.0F;
        this.bipedHead.yRot = netHeadYaw / (180F / (float)Math.PI);
        this.bipedHead.xRot = headPitch / (180F / (float)Math.PI);
        this.bipedBody.yRot = Mth.sin(Mth.sqrt(this.attackTime) * (float)Math.PI * 2.0F) * 0.2F;

        this.bipedRightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 0.7F * limbSwingAmount;
        this.bipedLeftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 0.7F * limbSwingAmount;
        this.bipedRightLeg.yRot = 0.0F;
        this.bipedLeftLeg.yRot = 0.0F;
        this.bipedRightLeg.zRot = 0.0F;
        this.bipedLeftLeg.zRot = 0.0F;

        this.bipedRightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedLeftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedRightArm.yRot = -10F / 180F * (float)Math.PI;
        this.bipedRightArm.zRot = 30F / 180F * (float)Math.PI;
        this.bipedLeftArm.yRot = -this.bipedRightArm.yRot;
        this.bipedLeftArm.zRot = -this.bipedRightArm.zRot;

        if (this.riding)
        {
            this.bipedRightArm.xRot = -((float)Math.PI / 5F);
            this.bipedLeftArm.xRot = -((float)Math.PI / 5F);
            this.bipedRightLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.bipedLeftLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.bipedRightLeg.yRot = ((float)Math.PI / 14F);
            this.bipedLeftLeg.yRot = -((float)Math.PI / 14F);
            this.bipedRightLeg.zRot = ((float)Math.PI / 14F);
            this.bipedLeftLeg.zRot = -((float)Math.PI / 14F);

            this.skirtTop.xRot += -((float)Math.PI / 5F);
        }

        this.bipedRightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.bipedLeftArm.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.bipedRightArm.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.bipedLeftArm.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;

        // スペルカードの宣言をする（1.7.10 は render() 内で setRotationAngles の後に実行）
        if (entity.getSpellCardMotion() < -15)
        {
            this.bipedRightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin((float)(30F + entity.getSpellCardMotion()) / 180F * 3.141593F) * 4F;
        }
        else if (entity.getSpellCardMotion() < 0)
        {
            this.bipedRightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin((float)(-entity.getSpellCardMotion()) / 180F * 3.141593F) * 4F;
        }
        else
        {
            this.bipedRightArm.xRot = -12F / 180F * (float)Math.PI;
            this.bipedRightArm.yRot = -10F / 180F * (float)Math.PI;
            this.bipedRightArm.zRot = 30F / 180F * (float)Math.PI;

            this.bipedLeftArm.xRot = this.bipedRightArm.xRot;
            this.bipedLeftArm.yRot = -this.bipedRightArm.yRot;
            this.bipedLeftArm.zRot = -this.bipedRightArm.zRot;
        }
    }
""",
)

# =====================================================================================
# ModelToziko — 屠自古
# =====================================================================================
M(
    name="ModelToziko",
    entity="EntityToziko",
    extends="EntityModel<EntityToziko>",
    doc="""/**
 * 屠自古（{@code EntityToziko}）のモデル。1.7.10 {@code ModelToziko} の移植。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:toziko}</p>
 */
""",
    setup="""    @Override
    public void setupAnim(EntityToziko entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        this.HeadBase.yRot = netHeadYaw / (180F / (float)Math.PI);
        this.HeadBase.xRot = headPitch / (180F / (float)Math.PI);
        this.Body.yRot = Mth.sin(Mth.sqrt(this.attackTime) * (float)Math.PI * 2.0F) * 0.2F;
        this.SkirtTop.xRot = 0F;

        // スペルカードの宣言をする
        if (entity.getSpellCardMotion() < -15)
        {
            this.RightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin((30F + entity.getSpellCardMotion()) / 180F * 3.141593F) * 4F;
        }
        else if (entity.getSpellCardMotion() < 0)
        {
            this.RightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F - Mth.sin((float)(-entity.getSpellCardMotion()) / 180F * 3.141593F) * 4F;
        }
        else
        {
            this.RightArm.yRot = 90F / 180F * (float)Math.PI;
            this.LeftArm.yRot = -this.RightArm.zRot;
        }

        if (this.riding)
        {
            this.RightArm.xRot = -((float)Math.PI / 5F);
            this.LeftArm.xRot = -((float)Math.PI / 5F);

            this.SkirtTop.xRot -= ((float)Math.PI / 5F);
        }
        else if (entity.getFlyingHeight() == 0)
        {
            if (limbSwing > 0F)
            {
                this.RightArm.xRot = Mth.cos(limbSwing + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
                this.LeftArm.xRot = Mth.cos(limbSwing) * 2.0F * limbSwingAmount * 0.5F;
                this.RightArm.yRot = -90F / 180F * (float)Math.PI;
                this.RightArm.zRot = 20F / 180F * (float)Math.PI;
                this.LeftArm.yRot = -this.RightArm.yRot;
                this.LeftArm.zRot = -this.RightArm.zRot;
            }
            else
            {
                this.RightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F;
                this.RightArm.yRot = 0.0F;
                this.RightArm.zRot = -0.6457718F;
                this.LeftArm.xRot = this.RightArm.xRot;
                this.LeftArm.yRot = 0.0F;
                this.LeftArm.zRot = 0.6457718F;
            }
        }
        else
        {
            this.RightArm.zRot = 0.0F;
            this.RightArm.yRot = Mth.sin(ageInTicks / 13F) * 0.1F;
            this.RightArm.xRot = -80F / 180F * (float)Math.PI + Mth.sin(ageInTicks / 10F) * 0.1F;
            this.LeftArm.zRot = 0.0F;
            this.LeftArm.yRot = -this.RightArm.yRot;
            this.LeftArm.xRot = this.RightArm.xRot;
        }
    }
""",
)

# =====================================================================================
# ModelRinnosuke — 霖之助
# =====================================================================================
M(
    name="ModelRinnosuke",
    entity="EntityRinnosuke",
    extends="EntityModel<EntityRinnosuke>",
    doc="""/**
 * 霖之助（{@code EntityRinnosuke}）のモデル。1.7.10 {@code ModelRinnosuke} の移植。
 * 1.7.10 は render() の中で setRotationAngles() の後に腕ポーズを上書きしていたため、
 * この setupAnim も同じ順序で適用する。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:rinnosuke}</p>
 */
""",
    setup="""    @Override
    public void setupAnim(EntityRinnosuke entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        this.bipedHead.yRot = netHeadYaw / (180F / (float)Math.PI);
        this.bipedHead.xRot = headPitch / (180F / (float)Math.PI);
        this.bipedBody.yRot = Mth.sin(Mth.sqrt(this.attackTime) * (float)Math.PI * 2.0F) * 0.2F;

        this.bipedRightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 0.7F * limbSwingAmount;
        this.bipedLeftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 0.7F * limbSwingAmount;
        this.bipedRightLeg.yRot = 0.0F;
        this.bipedLeftLeg.yRot = 0.0F;
        this.bipedRightLeg.zRot = 0.0F;
        this.bipedLeftLeg.zRot = 0.0F;

        this.bipedRightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedLeftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedRightArm.zRot = 10F / 180F * (float)Math.PI;
        this.bipedLeftArm.zRot = -this.bipedRightArm.zRot;

        if (this.riding)
        {
            this.bipedRightArm.xRot = -((float)Math.PI / 5F);
            this.bipedLeftArm.xRot = -((float)Math.PI / 5F);
            this.bipedRightLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.bipedLeftLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.bipedRightLeg.yRot = ((float)Math.PI / 14F);
            this.bipedLeftLeg.yRot = -((float)Math.PI / 14F);
            this.bipedRightLeg.zRot = ((float)Math.PI / 14F);
            this.bipedLeftLeg.zRot = -((float)Math.PI / 14F);
        }

        this.bipedRightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.bipedLeftArm.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.bipedRightArm.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.bipedLeftArm.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;

        // 腕の固定ポーズ（1.7.10 は render() 内で setRotationAngles の後に実行）
        this.bipedRightArm.xRot = -12F / 180F * (float)Math.PI;
        this.bipedRightArm.yRot = -10F / 180F * (float)Math.PI;
        this.bipedRightArm.zRot = 30F / 180F * (float)Math.PI;

        this.bipedLeftArm.xRot = this.bipedRightArm.xRot;
        this.bipedLeftArm.yRot = -this.bipedRightArm.yRot;
        this.bipedLeftArm.zRot = -this.bipedRightArm.zRot;
    }
""",
)

# =====================================================================================
# ModelTHFairy — 汎用妖精
# =====================================================================================
M(
    name="ModelTHFairy",
    entity="EntityTHFairy",
    extends="EntityModel<EntityTHFairy>",
    doc="""/**
 * 妖精（{@code EntityTHFairy}）のモデル。1.7.10 {@code ModelTHFairy} の移植。
 * {@code EntityFamiliar} / {@code EntityTHPhantom} / {@code EntitySunFlowerFairy}
 * もこのモデル層を使う（1.7.10 と同じ）。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:th_fairy}</p>
 */
""",
    setup="""    @Override
    public void setupAnim(EntityTHFairy entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        this.head.yRot = netHeadYaw / (180F / (float)Math.PI);
        this.head.xRot = headPitch / (180F / (float)Math.PI);
        this.body.yRot = Mth.sin(Mth.sqrt(this.attackTime) * (float)Math.PI * 2.0F) * 0.2F;
        this.skirt.xRot = 0F;

        if (this.riding)
        {
            this.rightArm.xRot = -((float)Math.PI / 5F);
            this.leftArm.xRot = -((float)Math.PI / 5F);
            this.rightLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.leftLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.rightLeg.yRot = ((float)Math.PI / 14F);
            this.leftLeg.yRot = -((float)Math.PI / 14F);
            this.rightLeg.zRot = ((float)Math.PI / 14F);
            this.leftLeg.zRot = -((float)Math.PI / 14F);

            this.skirt.xRot += -((float)Math.PI / 5F);
        }
        else if (entity.getFlyingHeight() == 0)
        {
            this.rightLeg.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount;
            this.leftLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount;
            this.rightLeg.zRot = 0F;
            this.leftLeg.zRot = 0F;

            if (limbSwing > 0F)
            {
                this.rightArm.xRot = Mth.cos(limbSwing + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
                this.leftArm.xRot = Mth.cos(limbSwing) * 2.0F * limbSwingAmount * 0.5F;
                this.rightArm.yRot = -10F / 180F * (float)Math.PI;
                this.rightArm.zRot = 20F / 180F * (float)Math.PI;
                this.leftArm.yRot = -this.rightArm.yRot;
                this.leftArm.zRot = -this.rightArm.zRot;
            }
            else
            {
                this.rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F;
                this.rightArm.yRot = 0.0F;
                this.rightArm.zRot = -0.6457718F;
                this.leftArm.xRot = this.rightArm.xRot;
                this.leftArm.yRot = 0.0F;
                this.leftArm.zRot = 0.6457718F;
            }
        }
        else
        {
            this.rightLeg.zRot = Math.abs(Mth.sin(ageInTicks / 10F) * 0.1F);
            this.leftLeg.zRot = -this.rightLeg.zRot;
            this.rightLeg.xRot = Math.abs(Mth.sin(ageInTicks / 10F) * 0.2F);
            this.leftLeg.xRot = this.rightLeg.zRot;

            this.rightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F;
            this.rightArm.yRot = 0.0F;
            this.rightArm.zRot = -0.6457718F;
            this.leftArm.xRot = this.rightArm.xRot;
            this.leftArm.yRot = 0.0F;
            this.leftArm.zRot = 0.6457718F;
        }

        if (entity.getHealth() > 0F)
        {
            this.body.xRot = 0F;
            if (entity.getFlyingHeight() > 0)
            {
                this.rightWing.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F;
                this.leftWing.yRot = -this.rightWing.yRot;
                this.rightWing2.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F;
                this.leftWing2.yRot = -this.rightWing2.yRot;
            }
            else
            {
                this.rightWing.yRot = Mth.cos(ageInTicks * 0.5F) * (float)Math.PI * 0.1F + (float)Math.PI * 0.15F;
                this.leftWing.yRot = -this.rightWing.yRot;
                this.rightWing2.yRot = Mth.cos(ageInTicks * 0.5F) * (float)Math.PI * 0.1F + (float)Math.PI * 0.15F;
                this.leftWing2.yRot = -this.rightWing2.yRot;
            }
        }
        else
        {
            // 1.7.10 は度の値をそのままラジアンとして代入している（-25F / 70F）。原文どおり保持。
            this.head.xRot = 70F;
            this.head.yRot = 0F;
            this.head.zRot = 0F;
            this.rightWing.yRot = -25F;
            this.leftWing.yRot = -this.rightWing.yRot;
            this.rightWing2.yRot = -25F;
            this.leftWing2.yRot = -this.rightWing2.yRot;
        }
    }
""",
)

# =====================================================================================
# ModelSunFlowerFairy — 向日葵妖精
# =====================================================================================
M(
    name="ModelSunFlowerFairy",
    entity="EntitySunFlowerFairy",
    extends="EntityModel<EntitySunFlowerFairy>",
    doc="""/**
 * 向日葵妖精（{@code EntitySunFlowerFairy}）のモデル。1.7.10
 * {@code ModelSunFlowerFairy} の移植（テクスチャ 128x64）。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:sun_flower_fairy}</p>
 */
""",
    setup="""    @Override
    public void setupAnim(EntitySunFlowerFairy entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        this.bipedHead.yRot = netHeadYaw / (180F / (float)Math.PI);
        this.bipedHead.xRot = headPitch / (180F / (float)Math.PI);
        this.bipedBody.yRot = Mth.sin(Mth.sqrt(this.attackTime) * (float)Math.PI * 2.0F) * 0.2F;
        this.skirtTop.xRot = 0F;

        if (this.riding)
        {
            this.bipedRightArm.xRot = -((float)Math.PI / 5F);
            this.bipedLeftArm.xRot = -((float)Math.PI / 5F);
            this.bipedRightLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.bipedLeftLeg.xRot = -((float)Math.PI * 2F / 5F);
            this.bipedRightLeg.yRot = ((float)Math.PI / 14F);
            this.bipedLeftLeg.yRot = -((float)Math.PI / 14F);
            this.bipedRightLeg.zRot = ((float)Math.PI / 14F);
            this.bipedLeftLeg.zRot = -((float)Math.PI / 14F);

            this.skirtTop.xRot += -((float)Math.PI / 5F);
        }
        else if (entity.getFlyingHeight() == 0)
        {
            this.bipedRightLeg.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount;
            this.bipedLeftLeg.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount;
            this.bipedRightLeg.zRot = 0F;
            this.bipedLeftLeg.zRot = 0F;

            if (limbSwing > 0F)
            {
                this.bipedRightArm.xRot = Mth.cos(limbSwing + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
                this.bipedLeftArm.xRot = Mth.cos(limbSwing) * 2.0F * limbSwingAmount * 0.5F;
                this.bipedRightArm.yRot = -10F / 180F * (float)Math.PI;
                this.bipedRightArm.zRot = 20F / 180F * (float)Math.PI;
                this.bipedLeftArm.yRot = -this.bipedRightArm.yRot;
                this.bipedLeftArm.zRot = -this.bipedRightArm.zRot;
            }
            else
            {
                this.bipedRightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F;
                this.bipedRightArm.yRot = 0.0F;
                this.bipedRightArm.zRot = -0.6457718F;
                this.bipedLeftArm.xRot = this.bipedRightArm.xRot;
                this.bipedLeftArm.yRot = 0.0F;
                this.bipedLeftArm.zRot = 0.6457718F;
            }
        }
        else
        {
            this.bipedRightLeg.zRot = Math.abs(Mth.sin(ageInTicks / 10F) * 0.1F);
            this.bipedLeftLeg.zRot = -this.bipedRightLeg.zRot;
            this.bipedRightLeg.xRot = Math.abs(Mth.sin(ageInTicks / 10F) * 0.2F);
            this.bipedLeftLeg.xRot = this.bipedRightLeg.zRot;

            this.bipedRightArm.xRot = -0.7F - Mth.sin(ageInTicks / 10F) * 0.1F;
            this.bipedRightArm.yRot = 0.0F;
            this.bipedRightArm.zRot = -0.6457718F;
            this.bipedLeftArm.xRot = this.bipedRightArm.xRot;
            this.bipedLeftArm.yRot = 0.0F;
            this.bipedLeftArm.zRot = 0.6457718F;
        }

        if (entity.getHealth() > 0F)
        {
            this.bipedBody.xRot = 0F;
            if (entity.getFlyingHeight() > 0)
            {
                this.rightWing.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F - (float)Math.PI;
                this.leftWing.yRot = -Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F;
            }
            else
            {
                this.rightWing.yRot = Mth.cos(ageInTicks * 0.5F) * (float)Math.PI * 0.1F - (float)Math.PI;
                this.leftWing.yRot = -Mth.cos(ageInTicks * 0.5F) * (float)Math.PI * 0.1F;
            }
        }
        else
        {
            // 1.7.10 は度の値をそのままラジアンとして代入している（70F / -25F）。原文どおり保持。
            this.bipedHead.xRot = 70F;
            this.bipedHead.yRot = 0F;
            this.bipedHead.zRot = 0F;
            this.rightWing.yRot = -25F;
            this.leftWing.yRot = -this.rightWing.yRot;
        }
    }
""",
)

# =====================================================================================
# ModelTHFairyCirno2 — チルノ第2形態の羽のみ
# =====================================================================================
M(
    name="ModelTHFairyCirno2",
    entity="EntityTHFairy",
    extends="EntityModel<EntityTHFairy>",
    doc="""/**
 * チルノ第2形態用の羽だけのモデル。1.7.10 {@code ModelTHFairyCirno2} の移植。
 * 1.7.10 ではどのレンダラからも参照されていない（死にコード）が、移植対象に
 * 含まれていたため同じ内容で残す。1.7.10 は render() の中で羽の位置と角度を
 * 毎フレーム設定していたので、その処理を setupAnim に移した。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:th_fairy_cirno2}（未使用）</p>
 */
""",
    setup="""    @Override
    public void setupAnim(EntityTHFairy entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        this.rightWing.setPos(0.0F, -3.0F, 2.0F);
        this.leftWing.setPos(0.0F, -3.0F, 2.0F);
        this.rightWing2.setPos(-1.0F, 0.0F, 2.0F);
        this.leftWing2.setPos(1.0F, 0.0F, 2.0F);
        this.rightWing3.setPos(-1.0F, -1.5F, 2.0F);
        this.leftWing3.setPos(1.0F, -1.5F, 2.0F);

        this.rightWing.yRot = Mth.cos(ageInTicks * 0.3F) * (float)Math.PI * 0.1F;
        this.leftWing.yRot = -this.rightWing.yRot;
        this.rightWing2.yRot = Mth.cos(ageInTicks * 0.3F) * (float)Math.PI * 0.1F;
        this.leftWing2.yRot = -this.rightWing2.yRot;
        this.rightWing3.yRot = Mth.cos(ageInTicks * 0.3F) * (float)Math.PI * 0.1F;
        this.leftWing3.yRot = -this.rightWing3.yRot;
    }
""",
)

# =====================================================================================
# ModelCursedDecoyDoll — 呪いのデコイ人形
# =====================================================================================
M(
    name="ModelCursedDecoyDoll",
    entity="EntityCursedDecoyDoll",
    extends="EntityModel<EntityCursedDecoyDoll>",
    extra_links=None,
    doc="""/**
 * 呪いのデコイ人形（{@code EntityCursedDecoyDoll}）のモデル。1.7.10
 * {@code ModelCursedDecoyDoll} の移植。1.7.10 の setRotationAngles は
 * super を呼ぶだけの空実装だったので、ここもアニメーションは無い。
 *
 * <p>1.7.10 は 12 個のパーツを addChild せず render() で個別に描いていた
 * （＝回転点がそのまま絶対位置）。したがって 1.21.1 でも全パーツを root 直下の
 * フラット構造で持つ。body の下に繋ぐと頭や腕の位置が二重に加算されてしまう。</p>
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:cursed_decoy_doll}</p>
 */
""",
    flat=True,
    setup="""    @Override
    public void setupAnim(EntityCursedDecoyDoll entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
""" + RESET + """
        // 1.7.10 の setRotationAngles は空実装
    }
""",
)


def build_source(cfg):
    info = G.parse(os.path.join(G.SRC, cfg["name"] + ".java"))
    for link in cfg.get("extra_links") or []:
        info["links"].append(link)

    declared = [p["name"] for p in info["parts"]]
    skip = cfg.get("skip_fields", [])
    fields = cfg.get("fields") or [n for n in declared if n not in skip]
    fields = [f for f in fields if f not in skip]

    nullable = cfg.get("nullable", False)
    flat = cfg.get("flat", False)
    getter = (lambda n: 'root.getChild("%s")' % n) if flat else \
             ((lambda n: 'part(root, "%s")' % n) if nullable else (lambda n: 'root.getChild("%s")' % n))

    out = ["package thkaguyamod.client.model.living;", ""]
    out.append(IMPORTS_COMMON)
    if "Mth." in cfg["setup"] or "Mth." in cfg.get("extra_methods", ""):
        out.append("import net.minecraft.util.Mth;")
        out.append("")
    out.append("import thkaguyamod.entity.living.%s;" % cfg["entity"])
    out.append("")
    out.append(cfg["doc"].strip())
    out.append("public class %s%s extends %s" % (cfg["name"], cfg.get("type_params", ""), cfg["extends"]))
    out.append("{")
    for f in fields:
        out.append("    public final ModelPart %s;" % f)
    if fields:
        out.append("")
    if cfg.get("extra_fields"):
        out.append(cfg["extra_fields"].strip("\n"))
        out.append("")
    out.append("    private final ModelPart root;")
    out.append("")
    out.append("    public %s(ModelPart root)" % cfg["name"])
    out.append("    {")
    if cfg.get("ctor_super"):
        out.append(cfg["ctor_super"].rstrip("\n"))
    out.append("        this.root = root;")
    if cfg.get("ctor_head"):
        out.append(cfg["ctor_head"].rstrip("\n"))
    for f in fields:
        out.append("        this.%s = %s;" % (f, getter(f)))
    if cfg.get("ctor_tail"):
        out.append(cfg["ctor_tail"].rstrip("\n"))
    out.append("    }")
    out.append("")
    if nullable:
        out.append("""    /** 1.7.10 は未生成パーツが null になり得たため、null 許容で取得する */
    protected static ModelPart part(ModelPart root, String name)
    {
        return root.hasChild(name) ? root.getChild(name) : null;
    }
""")
    if info["parts"]:
        out.append(G.emit(info))
        out.append("")
    else:
        out.append("""    /**
     * 基底は自身の箱を持たない。1.7.10 の {@code getTechneModelData()} が空だった
     * のと同じで、実際の形状はサブクラス（{@link ModelWriggle#createBodyLayer()}）が持つ。
     */
    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        return LayerDefinition.create(mesh, 64, 64);
    }
""")
    out.append("""    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
""")
    out.append(cfg["setup"].strip("\n"))
    out.append("")
    out.append(RAD.rstrip("\n"))
    out.append("}")
    return "\n".join(out) + "\n"


def main():
    os.makedirs(OUT, exist_ok=True)
    for cfg in MODELS:
        src = build_source(cfg)
        path = os.path.join(OUT, cfg["name"] + ".java")
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(src)
        print("%-26s %4d lines" % (cfg["name"] + ".java", src.count("\n")))


if __name__ == "__main__":
    main()
