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

import thkaguyamod.entity.living.EntityToziko;

/**
 * 屠自古（{@code EntityToziko}）のモデル。1.7.10 {@code ModelToziko} の移植。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:toziko}</p>
 */
public class ModelToziko extends EntityModel<EntityToziko>
{
    public final ModelPart Body;
    public final ModelPart RightArm;
    public final ModelPart LeftArm;
    public final ModelPart Leg1;
    public final ModelPart Leg2;
    public final ModelPart Leg3;
    public final ModelPart SkirtTop;
    public final ModelPart SkirtBottom;
    public final ModelPart HeadBase;
    public final ModelPart BackHair;
    public final ModelPart LeftHair;
    public final ModelPart RightHair;
    public final ModelPart Cup;
    public final ModelPart CupLeft;
    public final ModelPart CupRight;
    public final ModelPart CupRibbon;

    private final ModelPart root;

    public ModelToziko(ModelPart root)
    {
        this.root = root;
        this.Body = root.getChild("Body");
        this.RightArm = root.getChild("RightArm");
        this.LeftArm = root.getChild("LeftArm");
        this.Leg1 = root.getChild("Leg1");
        this.Leg2 = root.getChild("Leg2");
        this.Leg3 = root.getChild("Leg3");
        this.SkirtTop = root.getChild("SkirtTop");
        this.SkirtBottom = root.getChild("SkirtBottom");
        this.HeadBase = root.getChild("HeadBase");
        this.BackHair = root.getChild("BackHair");
        this.LeftHair = root.getChild("LeftHair");
        this.RightHair = root.getChild("RightHair");
        this.Cup = root.getChild("Cup");
        this.CupLeft = root.getChild("CupLeft");
        this.CupRight = root.getChild("CupRight");
        this.CupRibbon = root.getChild("CupRibbon");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition Body = root.addOrReplaceChild("Body",
                CubeListBuilder.create().texOffs(32, 6)
                        .addBox(-3.0F, 0.0F, -2.0F, 6, 9, 4),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition RightArm = root.addOrReplaceChild("RightArm",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-2.0F, -1.0F, -1.0F, 2, 8, 2),
                PartPose.offsetAndRotation(-3.0F, 3.0F, 0.0F, -1.570796F, 0.0F, 0.0F));
        PartDefinition LeftArm = root.addOrReplaceChild("LeftArm",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(0.0F, -1.0F, -1.0F, 2, 8, 2),
                PartPose.offsetAndRotation(3.0F, 3.0F, 0.0F, -1.570796F, 0.0F, 0.0F));
        PartDefinition Leg1 = root.addOrReplaceChild("Leg1",
                CubeListBuilder.create().texOffs(32, 17)
                        .addBox(-4.0F, 0.0F, -4.0F, 8, 7, 8),
                PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition Leg2 = root.addOrReplaceChild("Leg2",
                CubeListBuilder.create().texOffs(32, 17)
                        .addBox(-3.0F, 0.0F, -3.0F, 6, 4, 6),
                PartPose.offsetAndRotation(0.0F, 17.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition Leg3 = root.addOrReplaceChild("Leg3",
                CubeListBuilder.create().texOffs(32, 17)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 4, 4),
                PartPose.offsetAndRotation(0.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition SkirtTop = root.addOrReplaceChild("SkirtTop",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-4.0F, 0.0F, -4.0F, 8, 4, 8),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition SkirtBottom = root.addOrReplaceChild("SkirtBottom",
                CubeListBuilder.create().texOffs(16, 32)
                        .addBox(-5.0F, 0.0F, -5.0F, 10, 7, 10),
                PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition HeadBase = root.addOrReplaceChild("HeadBase",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition BackHair = HeadBase.addOrReplaceChild("BackHair",
                    CubeListBuilder.create().texOffs(0, 48)
                            .addBox(-4.0F, -2.0F, -3.0F, 8, 2, 3),
                    PartPose.offsetAndRotation(0.0F, 2.0F, 4.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition LeftHair = HeadBase.addOrReplaceChild("LeftHair",
                    CubeListBuilder.create().texOffs(11, 56)
                            .addBox(-4.0F, -2.0F, 0.0F, 8, 5, 1),
                    PartPose.offsetAndRotation(5.0F, -3.0F, 0.0F, 0.0F, -1.570796F, 0.0F));
            PartDefinition RightHair = HeadBase.addOrReplaceChild("RightHair",
                    CubeListBuilder.create().texOffs(11, 56)
                            .addBox(-4.0F, -2.0F, 0.0F, 8, 5, 1),
                    PartPose.offsetAndRotation(-5.0F, -3.0F, 0.0F, 0.0F, 1.570796F, 0.0F));
            PartDefinition Cup = HeadBase.addOrReplaceChild("Cup",
                    CubeListBuilder.create().texOffs(0, 32)
                            .addBox(-2.0F, -8.0F, -2.0F, 4, 6, 4),
                    PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition CupLeft = Cup.addOrReplaceChild("CupLeft",
                        CubeListBuilder.create().texOffs(0, 42)
                                .addBox(0.0F, 4.0F, 0.0F, 4, 1, 1),
                        PartPose.offsetAndRotation(2.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition CupRight = Cup.addOrReplaceChild("CupRight",
                        CubeListBuilder.create().texOffs(0, 42)
                                .addBox(-4.0F, 4.0F, 0.0F, 4, 1, 1),
                        PartPose.offsetAndRotation(-2.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition CupRibbon = Cup.addOrReplaceChild("CupRibbon",
                        CubeListBuilder.create().texOffs(0, 44)
                                .addBox(-2.0F, 2.0F, 0.0F, 5, 4, 1),
                        PartPose.offsetAndRotation(2.0F, -7.0F, 1.0F, 0.0F, -0.4833219F, -0.5205006F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(EntityToziko entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.root.getAllParts().forEach(ModelPart::resetPose);
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

    /** 度数をラジアンに変換（1.7.10 の rad() と同じ） */
    protected static float rad(float degree)
    {
        return degree / 180F * (float)Math.PI;
    }
}
