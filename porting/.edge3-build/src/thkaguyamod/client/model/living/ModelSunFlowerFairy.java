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

import thkaguyamod.entity.living.EntitySunFlowerFairy;

/**
 * 向日葵妖精（{@code EntitySunFlowerFairy}）のモデル。1.7.10
 * {@code ModelSunFlowerFairy} の移植（テクスチャ 128x64）。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:sun_flower_fairy}</p>
 */
public class ModelSunFlowerFairy extends EntityModel<EntitySunFlowerFairy>
{
    public final ModelPart bipedBody;
    public final ModelPart bipedRightArm;
    public final ModelPart bipedLeftArm;
    public final ModelPart bipedRightLeg;
    public final ModelPart bipedLeftLeg;
    public final ModelPart skirtTop;
    public final ModelPart skirtBottom;
    public final ModelPart leftWing;
    public final ModelPart rightWing;
    public final ModelPart himawari1;
    public final ModelPart himawari2;
    public final ModelPart bipedHead;
    public final ModelPart longHair;
    public final ModelPart rightRibbon;
    public final ModelPart rightTail;
    public final ModelPart leftRibbon;
    public final ModelPart leftTail;

    private final ModelPart root;

    public ModelSunFlowerFairy(ModelPart root)
    {
        this.root = root;
        this.bipedBody = root.getChild("bipedBody");
        // 1.7.10 は defaultAddChildSetting() で bipedBody 配下に組んでいたため、親チェーン経由で取得する
        ModelPart body = this.bipedBody;
        ModelPart head = root.getChild("bipedHead");
        ModelPart lArm = root.getChild("bipedLeftArm");
        ModelPart sTop = body.getChild("skirtTop");
        this.bipedRightArm = root.getChild("bipedRightArm");
        this.bipedLeftArm = lArm;
        this.bipedRightLeg = root.getChild("bipedRightLeg");
        this.bipedLeftLeg = root.getChild("bipedLeftLeg");
        this.skirtTop = sTop;
        this.skirtBottom = sTop.getChild("skirtBottom");
        this.leftWing = body.getChild("leftWing");
        this.rightWing = body.getChild("rightWing");
        this.himawari1 = lArm.getChild("himawari1");
        this.himawari2 = lArm.getChild("himawari2");
        this.bipedHead = head;
        this.longHair = head.getChild("longHair");
        this.rightRibbon = head.getChild("rightRibbon");
        this.rightTail = head.getChild("rightTail");
        this.leftRibbon = head.getChild("leftRibbon");
        this.leftTail = head.getChild("leftTail");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
                CubeListBuilder.create().texOffs(32, 8)
                        .addBox(-3.0F, 0.0F, -2.0F, 6, 7, 4),
                PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedRightArm = root.addOrReplaceChild("bipedRightArm",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-1.0F, -1.0F, -1.0F, 2, 8, 2),
                PartPose.offsetAndRotation(-4.0F, 6.0F, 0.0F, -0.7679449F, 0.0F, -0.6457718F));
        PartDefinition bipedLeftArm = root.addOrReplaceChild("bipedLeftArm",
                CubeListBuilder.create().texOffs(56, 0)
                        .addBox(-1.0F, -1.0F, -1.0F, 2, 8, 2),
                PartPose.offsetAndRotation(4.0F, 6.0F, 0.0F, -0.7679449F, 0.0F, 0.6457718F));
        PartDefinition bipedRightLeg = root.addOrReplaceChild("bipedRightLeg",
                CubeListBuilder.create().texOffs(50, 16)
                        .addBox(-1.0F, 0.0F, -2.0F, 3, 11, 4),
                PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedLeftLeg = root.addOrReplaceChild("bipedLeftLeg",
                CubeListBuilder.create().texOffs(50, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 3, 11, 4),
                PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition skirtTop = bipedBody.addOrReplaceChild("skirtTop",
                    CubeListBuilder.create().texOffs(0, 16)
                            .addBox(-4.0F, 5.0F, -4.0F, 8, 4, 8),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition skirtBottom = skirtTop.addOrReplaceChild("skirtBottom",
                        CubeListBuilder.create().texOffs(32, 32)
                                .addBox(-5.0F, 0.0F, -5.0F, 10, 6, 10),
                        PartPose.offsetAndRotation(0.0F, 9.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition leftWing = bipedBody.addOrReplaceChild("leftWing",
                    CubeListBuilder.create().texOffs(64, 0)
                            .addBox(-1.0F, -16.0F, 0.0F, 20, 26, 1),
                    PartPose.offsetAndRotation(2.0F, 3.0F, 2.0F, 0.0F, -0.4886922F, 0.0167304F));
            PartDefinition rightWing = bipedBody.addOrReplaceChild("rightWing",
                    CubeListBuilder.create().texOffs(72, 32)
                            .addBox(-1.0F, -16.0F, 0.0F, 20, 26, 1),
                    PartPose.offsetAndRotation(-2.0F, 3.0F, 2.0F, 0.0F, -2.6529F, 0.0167304F));
            PartDefinition himawari1 = bipedLeftArm.addOrReplaceChild("himawari1",
                    CubeListBuilder.create().texOffs(0, 32)
                            .addBox(-7.0F, -20.0F, 0.0F, 12, 31, 1),
                    PartPose.offsetAndRotation(0.0F, 6.0F, -2.0F, 0.4089647F, -0.7435722F, 0.3666439F));
            PartDefinition himawari2 = bipedLeftArm.addOrReplaceChild("himawari2",
                    CubeListBuilder.create().texOffs(0, 32)
                            .addBox(-7.0F, -20.0F, 0.0F, 12, 31, 1),
                    PartPose.offsetAndRotation(-1.0F, 6.0F, -2.0F, 0.2230717F, -0.669215F, 0.2551081F));
        PartDefinition bipedHead = root.addOrReplaceChild("bipedHead",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
                PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
                    CubeListBuilder.create().texOffs(24, 0)
                            .addBox(-4.0F, 4.0F, -3.0F, 8, 5, 3),
                    PartPose.offsetAndRotation(0.0F, -5.0F, 4.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition rightRibbon = bipedHead.addOrReplaceChild("rightRibbon",
                    CubeListBuilder.create().texOffs(32, 19)
                            .addBox(-3.0F, -2.0F, -1.0F, 5, 4, 1),
                    PartPose.offsetAndRotation(-4.0F, -7.0F, 0.0F, -0.4712389F, 1.570796F, 0.0F));
            PartDefinition rightTail = bipedHead.addOrReplaceChild("rightTail",
                    CubeListBuilder.create().texOffs(32, 24)
                            .addBox(-2.0F, -1.0F, -2.0F, 3, 8, 2),
                    PartPose.offsetAndRotation(-5.0F, -7.0F, 0.0F, -0.2617994F, 1.570796F, 0.0F));
            PartDefinition leftRibbon = bipedHead.addOrReplaceChild("leftRibbon",
                    CubeListBuilder.create().texOffs(32, 19)
                            .addBox(-3.0F, -2.0F, 0.0F, 5, 4, 1),
                    PartPose.offsetAndRotation(4.0F, -7.0F, 0.0F, 0.4712389F, 1.570796F, 0.0F));
            PartDefinition leftTail = bipedHead.addOrReplaceChild("leftTail",
                    CubeListBuilder.create().texOffs(32, 24)
                            .addBox(-2.0F, -1.0F, 0.0F, 3, 8, 2),
                    PartPose.offsetAndRotation(5.0F, -7.0F, 0.0F, 0.2617994F, 1.570796F, 0.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(EntitySunFlowerFairy entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.root.getAllParts().forEach(ModelPart::resetPose);
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

    /** 度数をラジアンに変換（1.7.10 の rad() と同じ） */
    protected static float rad(float degree)
    {
        return degree / 180F * (float)Math.PI;
    }
}
