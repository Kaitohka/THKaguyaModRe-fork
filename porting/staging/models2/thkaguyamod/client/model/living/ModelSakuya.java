package thkaguyamod.client.model.living;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import net.minecraft.util.Mth;

import thkaguyamod.entity.living.EntitySakuya;

/**
 * 咲夜（{@code EntitySakuya}）のモデル。1.7.10 {@code ModelSakuya} の移植。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:sakuya}</p>
 */
public class ModelSakuya extends EntityModel<EntitySakuya>
{
    public final ModelPart bipedBody;
    public final ModelPart bipedRightArm;
    public final ModelPart bipedLeftArm;
    public final ModelPart bipedRightLeg;
    public final ModelPart bipedLeftLeg;
    public final ModelPart skirtTop;
    public final ModelPart skirtBottom;
    public final ModelPart leftRibbon;
    public final ModelPart rightRibbon;
    public final ModelPart bipedHead;
    public final ModelPart longHair;
    public final ModelPart leftBraid;
    public final ModelPart rightBraid;
    public final ModelPart leftKatyusha;
    public final ModelPart rightKatyusha;

    protected float legHeight = 14F;
    protected float bodyHeight = 9F;

    private final ModelPart root;

    public ModelSakuya(ModelPart root)
    {
        this.root = root;
        this.bipedBody = root.getChild("bipedBody");
        this.bipedRightArm = root.getChild("bipedRightArm");
        this.bipedLeftArm = root.getChild("bipedLeftArm");
        this.bipedRightLeg = root.getChild("bipedRightLeg");
        this.bipedLeftLeg = root.getChild("bipedLeftLeg");
        this.skirtTop = root.getChild("skirtTop");
        this.skirtBottom = root.getChild("skirtBottom");
        this.leftRibbon = root.getChild("leftRibbon");
        this.rightRibbon = root.getChild("rightRibbon");
        this.bipedHead = root.getChild("bipedHead");
        this.longHair = root.getChild("longHair");
        this.leftBraid = root.getChild("leftBraid");
        this.rightBraid = root.getChild("rightBraid");
        this.leftKatyusha = root.getChild("leftKatyusha");
        this.rightKatyusha = root.getChild("rightKatyusha");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
                CubeListBuilder.create().texOffs(32, 8)
                        .addBox(-3.0F, 0.0F, -2.0F, 6, 9, 4),
                PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bipedRightArm = bipedBody.addOrReplaceChild("bipedRightArm",
                    CubeListBuilder.create().texOffs(48, 0)
                            .addBox(-1.0F, -1.0F, -1.0F, 2, 8, 2),
                    PartPose.offsetAndRotation(-4.0F, 2.0F, 0.0F, -0.7679449F, 0.0F, -0.6457718F));
            PartDefinition bipedLeftArm = bipedBody.addOrReplaceChild("bipedLeftArm",
                    CubeListBuilder.create().texOffs(56, 0)
                            .addBox(-1.0F, -1.0F, -1.0F, 2, 8, 2),
                    PartPose.offsetAndRotation(4.0F, 2.0F, 0.0F, -0.7679449F, 0.0F, 0.6457718F));
            PartDefinition bipedRightLeg = bipedBody.addOrReplaceChild("bipedRightLeg",
                    CubeListBuilder.create().texOffs(50, 18)
                            .addBox(-1.0F, 0.0F, -2.0F, 3, 14, 4),
                    PartPose.offsetAndRotation(-2.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bipedLeftLeg = bipedBody.addOrReplaceChild("bipedLeftLeg",
                    CubeListBuilder.create().texOffs(50, 18)
                            .addBox(-2.0F, 0.0F, -2.0F, 3, 14, 4),
                    PartPose.offsetAndRotation(2.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition skirtTop = bipedBody.addOrReplaceChild("skirtTop",
                    CubeListBuilder.create().texOffs(0, 16)
                            .addBox(-4.0F, 0.0F, -4.0F, 8, 4, 8),
                    PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition skirtBottom = skirtTop.addOrReplaceChild("skirtBottom",
                        CubeListBuilder.create().texOffs(16, 32)
                                .addBox(-5.0F, 0.0F, -5.0F, 10, 5, 10),
                        PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition leftRibbon = bipedBody.addOrReplaceChild("leftRibbon",
                    CubeListBuilder.create().texOffs(0, 38)
                            .addBox(0.0F, 0.0F, 0.0F, 5, 4, 1),
                    PartPose.offsetAndRotation(0.0F, 5.0F, 3.0F, 0.4886922F, -0.3F, -0.296706F));
            PartDefinition rightRibbon = bipedBody.addOrReplaceChild("rightRibbon",
                    CubeListBuilder.create().texOffs(0, 38)
                            .addBox(0.0F, 0.0F, -1.0F, 5, 4, 1),
                    PartPose.offsetAndRotation(0.0F, 5.0F, 3.0F, -0.4886922F, -(float)Math.PI + 0.3F, 0.296706F));
            PartDefinition bipedHead = bipedBody.addOrReplaceChild("bipedHead",
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
                    PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
                        CubeListBuilder.create().texOffs(24, 0)
                                .addBox(-4.0F, -2.0F, -4.0F, 8, 5, 3),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.2094395F, 0.0F, 0.0F));
                PartDefinition leftBraid = bipedHead.addOrReplaceChild("leftBraid",
                        CubeListBuilder.create().texOffs(0, 32)
                                .addBox(0.0F, -1.0F, -1.0F, 1, 5, 1),
                        PartPose.offsetAndRotation(3.0F, -1.0F, -3.0F, -0.2443461F, 0.0F, 0.0F));
                PartDefinition rightBraid = bipedHead.addOrReplaceChild("rightBraid",
                        CubeListBuilder.create().texOffs(0, 32)
                                .addBox(-1.0F, -1.0F, -1.0F, 1, 5, 1),
                        PartPose.offsetAndRotation(-3.0F, -1.0F, -3.0F, -0.2443461F, 0.0F, 0.0F));
                PartDefinition leftKatyusha = bipedHead.addOrReplaceChild("leftKatyusha",
                        CubeListBuilder.create().texOffs(4, 32)
                                .addBox(0.0F, -1.0F, 0.0F, 4, 1, 1),
                        PartPose.offsetAndRotation(0.0F, -8.0F, -5.0F, 0.0F, -0.1745329F, 0.2268928F));
                PartDefinition rightKatyusha = bipedHead.addOrReplaceChild("rightKatyusha",
                        CubeListBuilder.create().texOffs(4, 32)
                                .addBox(-4.0F, -1.0F, 0.0F, 4, 1, 1),
                        PartPose.offsetAndRotation(0.0F, -8.0F, -5.0F, 0.0F, 0.1745329F, -0.2268928F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(EntitySakuya entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.root.getAllParts().forEach(ModelPart::resetPose);
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

    /** 度数をラジアンに変換（1.7.10 の rad() と同じ） */
    protected static float rad(float degree)
    {
        return degree / 180F * (float)Math.PI;
    }
}
