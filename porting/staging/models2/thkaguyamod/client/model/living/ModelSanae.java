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

import thkaguyamod.entity.living.EntitySanae;

/**
 * 早苗（{@code EntitySanae}）のモデル。1.7.10 {@code ModelSanae} の移植。
 * 1.7.10 は render() の中で setRotationAngles() の後にスペルカード宣言ポーズを
 * 上書きしていたため、この setupAnim も同じ順序で適用する。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:sanae}</p>
 */
public class ModelSanae extends EntityModel<EntitySanae>
{
    public final ModelPart bipedBody;
    public final ModelPart bipedRightArm;
    public final ModelPart rightHand;
    public final ModelPart bipedLeftArm;
    public final ModelPart leftHand;
    public final ModelPart bipedRightLeg;
    public final ModelPart bipedLeftLeg;
    public final ModelPart skirtTop;
    public final ModelPart skirtBottom;
    public final ModelPart bipedHead;
    public final ModelPart longHair;
    public final ModelPart leftBraid;
    public final ModelPart rightBraid;
    public final ModelPart leftBraid2;
    public final ModelPart kaeruBase;
    public final ModelPart kaeruEyeRight;
    public final ModelPart kaeruEyeLeft;

    private final ModelPart root;

    public ModelSanae(ModelPart root)
    {
        this.root = root;
        this.bipedBody = root.getChild("bipedBody");
        this.bipedRightArm = root.getChild("bipedRightArm");
        this.rightHand = root.getChild("rightHand");
        this.bipedLeftArm = root.getChild("bipedLeftArm");
        this.leftHand = root.getChild("leftHand");
        this.bipedRightLeg = root.getChild("bipedRightLeg");
        this.bipedLeftLeg = root.getChild("bipedLeftLeg");
        this.skirtTop = root.getChild("skirtTop");
        this.skirtBottom = root.getChild("skirtBottom");
        this.bipedHead = root.getChild("bipedHead");
        this.longHair = root.getChild("longHair");
        this.leftBraid = root.getChild("leftBraid");
        this.rightBraid = root.getChild("rightBraid");
        this.leftBraid2 = root.getChild("leftBraid2");
        this.kaeruBase = root.getChild("kaeruBase");
        this.kaeruEyeRight = root.getChild("kaeruEyeRight");
        this.kaeruEyeLeft = root.getChild("kaeruEyeLeft");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
                CubeListBuilder.create().texOffs(32, 8)
                        .addBox(-3.0F, 0.0F, -2.0F, 6, 9, 4),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedRightArm = root.addOrReplaceChild("bipedRightArm",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(0.0F, 0.0F, -1.0F, 4, 7, 2),
                PartPose.offsetAndRotation(-3.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition rightHand = bipedRightArm.addOrReplaceChild("rightHand",
                    CubeListBuilder.create().texOffs(52, 9)
                            .addBox(0.0F, 1.0F, -1.0F, 2, 1, 2),
                    PartPose.offsetAndRotation(-0.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedLeftArm = root.addOrReplaceChild("bipedLeftArm",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-4.0F, 0.0F, -1.0F, 4, 7, 2),
                PartPose.offsetAndRotation(3.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition leftHand = bipedLeftArm.addOrReplaceChild("leftHand",
                    CubeListBuilder.create().texOffs(52, 9)
                            .addBox(-2.0F, 1.0F, -1.0F, 2, 1, 2),
                    PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedRightLeg = root.addOrReplaceChild("bipedRightLeg",
                CubeListBuilder.create().texOffs(50, 18)
                        .addBox(-1.0F, 0.0F, -2.0F, 3, 13, 4),
                PartPose.offsetAndRotation(-2.0F, 11.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedLeftLeg = root.addOrReplaceChild("bipedLeftLeg",
                CubeListBuilder.create().texOffs(50, 18)
                        .addBox(-2.0F, 0.0F, -2.0F, 3, 13, 4),
                PartPose.offsetAndRotation(2.0F, 11.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition skirtTop = bipedBody.addOrReplaceChild("skirtTop",
                    CubeListBuilder.create().texOffs(0, 16)
                            .addBox(-4.0F, -2.0F, -4.0F, 8, 4, 8),
                    PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition skirtBottom = skirtTop.addOrReplaceChild("skirtBottom",
                        CubeListBuilder.create().texOffs(16, 32)
                                .addBox(-5.0F, -2.0F, -5.0F, 10, 8, 10),
                        PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedHead = root.addOrReplaceChild("bipedHead",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -6.0F, -4.0F, 8, 8, 8),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
                    CubeListBuilder.create().texOffs(0, 50)
                            .addBox(-4.0F, 1.0F, -3.0F, 8, 9, 3),
                    PartPose.offsetAndRotation(0.0F, -1.0F, 4.0F, 0.2094395F, 0.0F, 0.0F));
            PartDefinition leftBraid = bipedHead.addOrReplaceChild("leftBraid",
                    CubeListBuilder.create().texOffs(4, 32)
                            .addBox(0.0F, 1.0F, -1.0F, 1, 5, 1),
                    PartPose.offsetAndRotation(3.0F, -3.0F, -3.0F, -0.2443461F, 0.0F, 0.0F));
            PartDefinition rightBraid = bipedHead.addOrReplaceChild("rightBraid",
                    CubeListBuilder.create().texOffs(0, 32)
                            .addBox(-1.0F, 1.0F, -1.0F, 1, 5, 1),
                    PartPose.offsetAndRotation(-3.0F, -3.0F, -3.0F, -0.2443461F, 0.0F, 0.0F));
            PartDefinition leftBraid2 = bipedHead.addOrReplaceChild("leftBraid2",
                    CubeListBuilder.create().texOffs(8, 31)
                            .addBox(-1.0F, 2.0F, -1.0F, 2, 5, 2),
                    PartPose.offsetAndRotation(3.0F, 0.0F, -4.0F, -0.2443461F, 0.0F, 0.0F));
            PartDefinition kaeruBase = bipedHead.addOrReplaceChild("kaeruBase",
                    CubeListBuilder.create().texOffs(0, 38)
                            .addBox(-2.0F, 1.0F, -1.0F, 3, 2, 1),
                    PartPose.offsetAndRotation(3.0F, -7.0F, -4.0F, 0.0F, 0.0F, 0.3839724F));
            PartDefinition kaeruEyeRight = bipedHead.addOrReplaceChild("kaeruEyeRight",
                    CubeListBuilder.create().texOffs(8, 38)
                            .addBox(-1.0F, 2.0F, -1.0F, 1, 1, 1),
                    PartPose.offsetAndRotation(2.7F, -9.0F, -4.0F, 0.0F, 0.0F, 0.3839724F));
            PartDefinition kaeruEyeLeft = bipedHead.addOrReplaceChild("kaeruEyeLeft",
                    CubeListBuilder.create().texOffs(8, 38)
                            .addBox(0.0F, 2.0F, -1.0F, 1, 1, 1),
                    PartPose.offsetAndRotation(3.7F, -8.7F, -4.0F, 0.0F, 0.0F, 0.3839724F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(EntitySanae entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.root.getAllParts().forEach(ModelPart::resetPose);
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

    /** 度数をラジアンに変換（1.7.10 の rad() と同じ） */
    protected static float rad(float degree)
    {
        return degree / 180F * (float)Math.PI;
    }
}
