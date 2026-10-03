package thkaguyamod.client.model.living;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

import net.minecraft.util.Mth;

import thkaguyamod.entity.living.EntityDanmakuMob;

/**
 * 東方キャラモデルの基底（1.7.10 {@code ModelTouhouDefault} の移植）。
 * 自身は箱を持たない抽象アンカーで、{@link ModelWriggle} がこの上に自分の
 * Techne データ（{@code getTechneModelData()} 相当）を組み立てる。
 * {@code T} は 1.7.10 の {@code EntityDanmakuMob} 引数に対応する。
 *
 * <p>モデルレイヤー名（呼び出し側）: 未登録。{@link ModelWriggle} が
 * {@code thkaguyamod:wriggle} を使う。</p>
 */
public class ModelTouhouDefault<T extends EntityDanmakuMob> extends EntityModel<T>
{
    public final ModelPart bipedHead;
    public final ModelPart bipedBody;
    public final ModelPart bipedRightArm;
    public final ModelPart bipedLeftArm;
    public final ModelPart bipedRightLeg;
    public final ModelPart bipedLeftLeg;
    public final ModelPart skirtTop;
    public final ModelPart skirtBottom;
    public final ModelPart longHair;
    public final ModelPart rightBraid;
    public final ModelPart leftBraid;

    /** float型のPI（1.7.10 と同じ値） */
    protected static final float fPI = 3.141593F;

    /** 肩幅（身体の中心から腕までの長さ） */
    protected float shoulderWidth = 3.0F;
    /** 身体の高さ（座高） */
    protected float bodyHeight = 0.0F;
    /** 足の長さ */
    protected float legHeight = 0.0F;

    private final ModelPart root;

    public ModelTouhouDefault(ModelPart root)
    {
        this.root = root;
        this.bipedBody = part(root, "bipedBody");
        // 1.7.10 は defaultAddChildSetting() で bipedBody 配下に組んでいたため、
        // ネストしたパーツは親チェーン経由で取得する（root 直接参照は不可）
        ModelPart body = this.bipedBody != null ? this.bipedBody : root;
        this.bipedHead = part(body, "bipedHead");
        this.bipedRightArm = part(body, "bipedRightArm");
        this.bipedLeftArm = part(body, "bipedLeftArm");
        this.bipedRightLeg = part(body, "bipedRightLeg");
        this.bipedLeftLeg = part(body, "bipedLeftLeg");
        this.skirtTop = part(body, "skirtTop");
        this.skirtBottom = part(this.skirtTop, "skirtBottom");
        ModelPart head = this.bipedHead != null ? this.bipedHead : body;
        this.longHair = part(head, "longHair");
        this.rightBraid = part(head, "rightBraid");
        this.leftBraid = part(head, "leftBraid");
    }

    /** 1.7.10 は未生成パーツが null になり得たため、null 許容で取得する */
    protected static ModelPart part(ModelPart parent, String name)
    {
        return parent != null && parent.hasChild(name) ? parent.getChild(name) : null;
    }

    /**
     * 基底は自身の箱を持たない。1.7.10 の {@code getTechneModelData()} が空だった
     * のと同じで、実際の形状はサブクラス（{@link ModelWriggle#createBodyLayer()}）が持つ。
     */
    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.root.getAllParts().forEach(ModelPart::resetPose);
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

    /** 度数をラジアンに変換（1.7.10 の rad() と同じ） */
    protected static float rad(float degree)
    {
        return degree / 180F * (float)Math.PI;
    }
}
