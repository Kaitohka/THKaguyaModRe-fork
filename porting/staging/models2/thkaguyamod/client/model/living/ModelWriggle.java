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

import thkaguyamod.entity.living.EntityWriggle;

/**
 * リグル（{@code EntityWriggle}）のモデル。1.7.10 {@code ModelWriggle} の移植。
 * 基底（{@link ModelTouhouDefault}）の {@code getTechneModelData()} を差し替えて
 * 自前の Techne データを組み立てる構造で、そのため 1.7.10 と同じく
 * biped 系パーツもこのクラスの createBodyLayer で定義する。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:wriggle}</p>
 */
public class ModelWriggle extends ModelTouhouDefault<EntityWriggle>
{
    public final ModelPart rightTouch;
    public final ModelPart leftTouch;
    public final ModelPart Manto1;
    public final ModelPart Manto2;
    public final ModelPart Manto3;
    public final ModelPart rightZubon;
    public final ModelPart leftZubon;

    private final ModelPart root;

    public ModelWriggle(ModelPart root)
    {
        super(root);
        this.root = root;
        this.rightTouch = root.getChild("rightTouch");
        this.leftTouch = root.getChild("leftTouch");
        this.Manto1 = root.getChild("Manto1");
        this.Manto2 = root.getChild("Manto2");
        this.Manto3 = root.getChild("Manto3");
        this.rightZubon = root.getChild("rightZubon");
        this.leftZubon = root.getChild("leftZubon");
        this.setTouhouModelData();
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
                CubeListBuilder.create().texOffs(32, 8)
                        .addBox(-3.0F, 0.0F, -2.0F, 6, 7, 4),
                PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bipedRightArm = bipedBody.addOrReplaceChild("bipedRightArm",
                    CubeListBuilder.create().texOffs(56, 0)
                            .addBox(-2.0F, -1.0F, -1.0F, 2, 8, 2),
                    PartPose.offsetAndRotation(-2.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bipedLeftArm = bipedBody.addOrReplaceChild("bipedLeftArm",
                    CubeListBuilder.create().texOffs(56, 0)
                            .addBox(0.0F, -1.0F, -1.0F, 2, 8, 2),
                    PartPose.offsetAndRotation(2.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bipedRightLeg = bipedBody.addOrReplaceChild("bipedRightLeg",
                    CubeListBuilder.create().texOffs(32, 19)
                            .addBox(-1.0F, 0.0F, -2.0F, 3, 11, 4),
                    PartPose.offsetAndRotation(-2.0F, 13.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bipedLeftLeg = bipedBody.addOrReplaceChild("bipedLeftLeg",
                    CubeListBuilder.create().texOffs(32, 19)
                            .addBox(-2.0F, 0.0F, -2.0F, 3, 11, 4),
                    PartPose.offsetAndRotation(2.0F, 13.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bipedHead = bipedBody.addOrReplaceChild("bipedHead",
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
                    PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
                        CubeListBuilder.create().texOffs(24, 0)
                                .addBox(-4.0F, 0.0F, -3.0F, 8, 2, 3),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition leftBraid = bipedHead.addOrReplaceChild("leftBraid",
                        CubeListBuilder.create().texOffs(0, 16)
                                .addBox(0.0F, 0.0F, -1.0F, 1, 4, 1),
                        PartPose.offsetAndRotation(2.95F, -3.0F, -3.0F, -0.2443461F, 0.0F, 0.0F));
                PartDefinition rightBraid = bipedHead.addOrReplaceChild("rightBraid",
                        CubeListBuilder.create().texOffs(0, 16)
                                .addBox(-1.0F, 0.0F, -1.0F, 1, 4, 1),
                        PartPose.offsetAndRotation(-2.95F, -3.0F, -3.0F, -0.2443461F, 0.0F, 0.0F));
                PartDefinition rightTouch = bipedHead.addOrReplaceChild("rightTouch",
                        CubeListBuilder.create().texOffs(4, 16)
                                .addBox(0.0F, -6.0F, 0.0F, 1, 6, 1),
                        PartPose.offsetAndRotation(-3.0F, -8.0F, -4.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition leftTouch = bipedHead.addOrReplaceChild("leftTouch",
                        CubeListBuilder.create().texOffs(4, 16)
                                .addBox(-1.0F, -6.0F, 0.0F, 1, 6, 1),
                        PartPose.offsetAndRotation(3.0F, -8.0F, -4.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition Manto1 = bipedBody.addOrReplaceChild("Manto1",
                    CubeListBuilder.create().texOffs(38, 47)
                            .addBox(-6.0F, 0.0F, -1.0F, 12, 6, 1),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition Manto2 = Manto1.addOrReplaceChild("Manto2",
                        CubeListBuilder.create().texOffs(38, 54)
                                .addBox(-6.0F, 0.0F, -1.0F, 12, 4, 1),
                        PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                    PartDefinition Manto3 = Manto2.addOrReplaceChild("Manto3",
                            CubeListBuilder.create().texOffs(38, 59)
                                    .addBox(-6.0F, 0.0F, -1.0F, 12, 4, 1),
                            PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition rightZubon = bipedRightLeg.addOrReplaceChild("rightZubon",
                        CubeListBuilder.create().texOffs(0, 32)
                                .addBox(-2.0F, 0.0F, -2.5F, 4, 6, 5),
                        PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, 0.0F, 0.0F, 0.0F));
                PartDefinition leftZubon = bipedLeftLeg.addOrReplaceChild("leftZubon",
                        CubeListBuilder.create().texOffs(0, 32)
                                .addBox(-2.0F, 0.0F, -2.5F, 4, 6, 5),
                        PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, 0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
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

    /** 度数をラジアンに変換（1.7.10 の rad() と同じ） */
    protected static float rad(float degree)
    {
        return degree / 180F * (float)Math.PI;
    }
}
