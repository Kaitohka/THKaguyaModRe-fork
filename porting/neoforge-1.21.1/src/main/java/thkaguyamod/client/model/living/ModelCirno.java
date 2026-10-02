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

import thkaguyamod.entity.living.EntityCirno;

/**
 * チルノ（{@code EntityCirno}）のモデル。1.7.10 {@code ModelCirno} の移植。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:cirno}</p>
 */
public class ModelCirno extends EntityModel<EntityCirno>
{
    public final ModelPart head;
    public final ModelPart longHair;
    public final ModelPart ribbonCenter;
    public final ModelPart ribbonLeft;
    public final ModelPart ribbonRight;
    public final ModelPart body;
    public final ModelPart skirt;
    public final ModelPart skirt2;
    public final ModelPart rightArm;
    public final ModelPart leftArm;
    public final ModelPart rightLeg;
    public final ModelPart leftLeg;
    public final ModelPart rightWing;
    public final ModelPart leftWing;
    public final ModelPart rightWing2;
    public final ModelPart leftWing2;
    public final ModelPart rightWing3;
    public final ModelPart leftWing3;

    private final ModelPart root;

    public ModelCirno(ModelPart root)
    {
        this.root = root;
        this.head = root.getChild("head");
        // 1.7.10 は head.addChild / skirt.addChild で親パーツ配下に付くため、親チェーン経由で取得する
        this.longHair = this.head.getChild("longHair");
        this.ribbonCenter = this.head.getChild("ribbonCenter");
        this.ribbonLeft = this.head.getChild("ribbonLeft");
        this.ribbonRight = this.head.getChild("ribbonRight");
        this.body = root.getChild("body");
        this.skirt = root.getChild("skirt");
        this.skirt2 = this.skirt.getChild("skirt2");
        this.rightArm = root.getChild("rightArm");
        this.leftArm = root.getChild("leftArm");
        this.rightLeg = root.getChild("rightLeg");
        this.leftLeg = root.getChild("leftLeg");
        this.rightWing = root.getChild("rightWing");
        this.leftWing = root.getChild("leftWing");
        this.rightWing2 = root.getChild("rightWing2");
        this.leftWing2 = root.getChild("leftWing2");
        this.rightWing3 = root.getChild("rightWing3");
        this.leftWing3 = root.getChild("leftWing3");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
                PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition longHair = head.addOrReplaceChild("longHair",
                    CubeListBuilder.create().texOffs(24, 0)
                            .addBox(-4.0F, 0.0F, -3.0F, 8, 5, 3),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition ribbonCenter = head.addOrReplaceChild("ribbonCenter",
                    CubeListBuilder.create().texOffs(0, 32)
                            .addBox(-1.0F, -1.0F, 0.0F, 2, 2, 1),
                    PartPose.offsetAndRotation(0.0F, -8.0F, 4.0F, 0.2443461F, 0.0F, 0.0F));
            PartDefinition ribbonLeft = head.addOrReplaceChild("ribbonLeft",
                    CubeListBuilder.create().texOffs(6, 32)
                            .addBox(0.0F, -2.0F, 0.0F, 5, 4, 1),
                    PartPose.offsetAndRotation(1.0F, -8.0F, 4.0F, 0.418879F, 0.0F, 0.0F));
            PartDefinition ribbonRight = head.addOrReplaceChild("ribbonRight",
                    CubeListBuilder.create().texOffs(6, 32)
                            .addBox(-5.0F, -2.0F, 0.0F, 5, 4, 1),
                    PartPose.offsetAndRotation(-1.0F, -8.0F, 4.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(32, 8)
                        .addBox(-3.0F, 0.0F, -2.0F, 6, 7, 4),
                PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition skirt = root.addOrReplaceChild("skirt",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-4.0F, 0.0F, -4.0F, 8, 5, 8),
                PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition skirt2 = skirt.addOrReplaceChild("skirt2",
                    CubeListBuilder.create().texOffs(24, 32)
                            .addBox(-5.0F, 0.0F, -5.0F, 10, 6, 10),
                    PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightArm = root.addOrReplaceChild("rightArm",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-1.0F, -1.0F, -1.0F, 2, 8, 2),
                PartPose.offsetAndRotation(-4.0F, 6.0F, 0.0F, -0.7679449F, 0.0F, -0.6457718F));
        PartDefinition leftArm = root.addOrReplaceChild("leftArm",
                CubeListBuilder.create().texOffs(56, 0)
                        .addBox(-1.0F, -1.0F, -1.0F, 2, 8, 2),
                PartPose.offsetAndRotation(4.0F, 6.0F, 0.0F, -0.7679449F, 0.0F, 0.6457718F));
        PartDefinition rightLeg = root.addOrReplaceChild("rightLeg",
                CubeListBuilder.create().texOffs(50, 16)
                        .addBox(-1.0F, 0.0F, -2.0F, 3, 12, 4),
                PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("leftLeg",
                CubeListBuilder.create().texOffs(50, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 3, 12, 4),
                PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightWing = root.addOrReplaceChild("rightWing",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(-14.0F, 0.0F, 0.0F, 14, 3, 3),
                PartPose.offsetAndRotation(-1.0F, 3.0F, 2.0F, 0.0F, 0.4886922F, 0.3141593F));
        PartDefinition leftWing = root.addOrReplaceChild("leftWing",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(0.0F, 0.0F, 0.0F, 14, 3, 3),
                PartPose.offsetAndRotation(0.0F, 3.0F, 2.0F, 0.0F, -0.4886922F, -0.3141593F));
        PartDefinition rightWing2 = root.addOrReplaceChild("rightWing2",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(-14.0F, 0.0F, 0.0F, 14, 3, 3),
                PartPose.offsetAndRotation(-1.0F, 6.0F, 2.0F, 0.0F, 0.4886922F, -0.3141593F));
        PartDefinition leftWing2 = root.addOrReplaceChild("leftWing2",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(0.0F, 0.0F, 0.0F, 14, 3, 3),
                PartPose.offsetAndRotation(1.0F, 6.0F, 2.0F, 0.0F, -0.4886922F, 0.3141593F));
        PartDefinition rightWing3 = root.addOrReplaceChild("rightWing3",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(-14.0F, 0.0F, 0.0F, 14, 3, 3),
                PartPose.offsetAndRotation(-1.0F, 5.0F, 2.0F, 0.0F, 0.4886922F, 0.0F));
        PartDefinition leftWing3 = root.addOrReplaceChild("leftWing3",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(0.0F, 0.0F, 0.0F, 14, 3, 3),
                PartPose.offsetAndRotation(1.0F, 5.0F, 2.0F, 0.0F, -0.4886922F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(EntityCirno entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.root.getAllParts().forEach(ModelPart::resetPose);
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

    /** 度数をラジアンに変換（1.7.10 の rad() と同じ） */
    protected static float rad(float degree)
    {
        return degree / 180F * (float)Math.PI;
    }
}
