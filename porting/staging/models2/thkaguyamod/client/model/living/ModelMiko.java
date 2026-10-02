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

import thkaguyamod.entity.living.EntityMiko;

/**
 * 神子（{@code EntityMiko}）のモデル。1.7.10 {@code ModelMiko} の移植。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:miko}</p>
 */
public class ModelMiko extends EntityModel<EntityMiko>
{
    public final ModelPart bipedBody;
    public final ModelPart bipedRightArm;
    public final ModelPart bipedLeftArm;
    public final ModelPart bipedRightLeg;
    public final ModelPart bipedLeftLeg;
    public final ModelPart skirtTop;
    public final ModelPart skirtBottom;
    public final ModelPart cape;
    public final ModelPart Manto1;
    public final ModelPart Manto2;
    public final ModelPart Manto3;
    public final ModelPart Manto4;
    public final ModelPart bipedHead;
    public final ModelPart longHair;
    public final ModelPart leftBraid;
    public final ModelPart rightBraid;
    public final ModelPart MimiateRight;
    public final ModelPart MimiateLeft;
    public final ModelPart HearLeft1;
    public final ModelPart HearLeft2;
    public final ModelPart HearRight1;
    public final ModelPart HearRight2;

    protected float legHeight = 12F;
    protected float bodyHeight = 9F;

    private final ModelPart root;

    public ModelMiko(ModelPart root)
    {
        this.root = root;
        this.bipedBody = root.getChild("bipedBody");
        this.bipedRightArm = root.getChild("bipedRightArm");
        this.bipedLeftArm = root.getChild("bipedLeftArm");
        this.bipedRightLeg = root.getChild("bipedRightLeg");
        this.bipedLeftLeg = root.getChild("bipedLeftLeg");
        this.skirtTop = root.getChild("skirtTop");
        this.skirtBottom = root.getChild("skirtBottom");
        this.cape = root.getChild("cape");
        this.Manto1 = root.getChild("Manto1");
        this.Manto2 = root.getChild("Manto2");
        this.Manto3 = root.getChild("Manto3");
        this.Manto4 = root.getChild("Manto4");
        this.bipedHead = root.getChild("bipedHead");
        this.longHair = root.getChild("longHair");
        this.leftBraid = root.getChild("leftBraid");
        this.rightBraid = root.getChild("rightBraid");
        this.MimiateRight = root.getChild("MimiateRight");
        this.MimiateLeft = root.getChild("MimiateLeft");
        this.HearLeft1 = root.getChild("HearLeft1");
        this.HearLeft2 = root.getChild("HearLeft2");
        this.HearRight1 = root.getChild("HearRight1");
        this.HearRight2 = root.getChild("HearRight2");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
                CubeListBuilder.create().texOffs(32, 8)
                        .addBox(-3.0F, 0.0F, -2.0F, 6, 9, 4),
                PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bipedRightArm = bipedBody.addOrReplaceChild("bipedRightArm",
                    CubeListBuilder.create().texOffs(56, 0)
                            .addBox(-0.8F, -1.0F, -1.0F, 2, 8, 2),
                    PartPose.offsetAndRotation(-4.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bipedLeftArm = bipedBody.addOrReplaceChild("bipedLeftArm",
                    CubeListBuilder.create().texOffs(56, 0)
                            .addBox(-1.2F, -1.0F, -1.0F, 2, 8, 2),
                    PartPose.offsetAndRotation(4.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bipedRightLeg = bipedBody.addOrReplaceChild("bipedRightLeg",
                    CubeListBuilder.create().texOffs(50, 18)
                            .addBox(-1.0F, 0.0F, -2.0F, 3, 12, 4),
                    PartPose.offsetAndRotation(-2.0F, 9.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bipedLeftLeg = bipedBody.addOrReplaceChild("bipedLeftLeg",
                    CubeListBuilder.create().texOffs(50, 18)
                            .addBox(-2.0F, 0.0F, -2.0F, 3, 12, 4),
                    PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition skirtTop = bipedBody.addOrReplaceChild("skirtTop",
                    CubeListBuilder.create().texOffs(0, 16)
                            .addBox(-4.0F, 0.0F, -6.0F, 8, 4, 8),
                    PartPose.offsetAndRotation(0.0F, 6.0F, 2.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition skirtBottom = skirtTop.addOrReplaceChild("skirtBottom",
                        CubeListBuilder.create().texOffs(16, 31)
                                .addBox(-5.0F, 0.0F, -10.0F, 10, 8, 10),
                        PartPose.offsetAndRotation(0.0F, 4.0F, 3.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition cape = bipedBody.addOrReplaceChild("cape",
                    CubeListBuilder.create().texOffs(30, 54)
                            .addBox(-5.0F, -4.0F, -2.5F, 10, 3, 7),
                    PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition Manto1 = bipedBody.addOrReplaceChild("Manto1",
                    CubeListBuilder.create().texOffs(0, 49)
                            .addBox(-5.5F, 0.0F, -1.0F, 11, 3, 1),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition Manto2 = Manto1.addOrReplaceChild("Manto2",
                        CubeListBuilder.create().texOffs(0, 53)
                                .addBox(-6.0F, 0.0F, -1.0F, 12, 4, 1),
                        PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                    PartDefinition Manto3 = Manto2.addOrReplaceChild("Manto3",
                            CubeListBuilder.create().texOffs(0, 58)
                                    .addBox(-6.5F, 0.0F, -1.0F, 13, 5, 1),
                            PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                        PartDefinition Manto4 = Manto3.addOrReplaceChild("Manto4",
                                CubeListBuilder.create().texOffs(0, 58)
                                        .addBox(-6.5F, 0.0F, -1.0F, 13, 5, 1),
                                PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bipedHead = bipedBody.addOrReplaceChild("bipedHead",
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
                    PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
                        CubeListBuilder.create().texOffs(0, 28)
                                .addBox(-4.0F, -2.0F, -3.0F, 8, 3, 3),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.2094395F, 0.0F, 0.0F));
                PartDefinition leftBraid = bipedHead.addOrReplaceChild("leftBraid",
                        CubeListBuilder.create().texOffs(0, 16)
                                .addBox(0.0F, -3.0F, -1.0F, 1, 4, 1),
                        PartPose.offsetAndRotation(3.0F, 0.0F, -3.0F, -0.2443461F, 0.0F, 0.0F));
                PartDefinition rightBraid = bipedHead.addOrReplaceChild("rightBraid",
                        CubeListBuilder.create().texOffs(0, 16)
                                .addBox(-1.0F, -3.0F, -1.0F, 1, 4, 1),
                        PartPose.offsetAndRotation(-3.0F, 0.0F, -3.0F, -0.2443461F, 0.0F, 0.0F));
                PartDefinition MimiateRight = bipedHead.addOrReplaceChild("MimiateRight",
                        CubeListBuilder.create().texOffs(0, 41)
                                .addBox(-2.0F, -5.0F, -1.0F, 4, 4, 1),
                        PartPose.offsetAndRotation(-4.0F, -1.0F, 0.0F, 0.0F, 1.570796F, 0.0F));
                PartDefinition MimiateLeft = bipedHead.addOrReplaceChild("MimiateLeft",
                        CubeListBuilder.create().texOffs(0, 41)
                                .addBox(-2.0F, -5.0F, -1.0F, 4, 4, 1),
                        PartPose.offsetAndRotation(4.0F, -1.0F, 0.0F, 0.0F, -1.570796F, 0.0F));
                PartDefinition HearLeft1 = bipedHead.addOrReplaceChild("HearLeft1",
                        CubeListBuilder.create().texOffs(24, 0)
                                .addBox(0.0F, -5.0F, -2.0F, 4, 2, 4),
                        PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.1745329F));
                    PartDefinition HearLeft2 = HearLeft1.addOrReplaceChild("HearLeft2",
                            CubeListBuilder.create().texOffs(38, 1)
                                    .addBox(0.0F, -1.0F, -1.5F, 3, 3, 3),
                            PartPose.offsetAndRotation(1.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.1745329F));
                PartDefinition HearRight1 = bipedHead.addOrReplaceChild("HearRight1",
                        CubeListBuilder.create().texOffs(24, 0)
                                .addBox(-4.0F, -5.0F, -2.0F, 4, 2, 4),
                        PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.0F, 0.0F, -0.1745329F));
                    PartDefinition HearRight2 = HearRight1.addOrReplaceChild("HearRight2",
                            CubeListBuilder.create().texOffs(38, 1)
                                    .addBox(-3.0F, -1.0F, -1.5F, 3, 3, 3),
                            PartPose.offsetAndRotation(-1.0F, -7.0F, 0.0F, 0.0F, 0.0F, -0.1745329F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(EntityMiko entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.root.getAllParts().forEach(ModelPart::resetPose);
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

    /** 度数をラジアンに変換（1.7.10 の rad() と同じ） */
    protected static float rad(float degree)
    {
        return degree / 180F * (float)Math.PI;
    }
}
