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

import thkaguyamod.entity.living.EntityRinnosuke;

/**
 * 霖之助（{@code EntityRinnosuke}）のモデル。1.7.10 {@code ModelRinnosuke} の移植。
 * 1.7.10 は render() の中で setRotationAngles() の後に腕ポーズを上書きしていたため、
 * この setupAnim も同じ順序で適用する。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:rinnosuke}</p>
 */
public class ModelRinnosuke extends EntityModel<EntityRinnosuke>
{
    public final ModelPart bipedBody;
    public final ModelPart bipedRightArm;
    public final ModelPart bipedLeftArm;
    public final ModelPart bipedRightLeg;
    public final ModelPart bipedLeftLeg;
    public final ModelPart box;
    public final ModelPart bottom;
    public final ModelPart bipedHead;
    public final ModelPart longHair;
    public final ModelPart Grass;

    private final ModelPart root;

    public ModelRinnosuke(ModelPart root)
    {
        this.root = root;
        this.bipedBody = root.getChild("bipedBody");
        this.bipedRightArm = root.getChild("bipedRightArm");
        this.bipedLeftArm = root.getChild("bipedLeftArm");
        this.bipedRightLeg = root.getChild("bipedRightLeg");
        this.bipedLeftLeg = root.getChild("bipedLeftLeg");
        // 1.7.10 は box/bottom を bipedBody の子、longHair/Grass を bipedHead の子にしていた
        this.box = this.bipedBody.getChild("box");
        this.bottom = this.bipedBody.getChild("bottom");
        this.bipedHead = root.getChild("bipedHead");
        this.longHair = this.bipedHead.getChild("longHair");
        this.Grass = this.bipedHead.getChild("Grass");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8, 10, 4),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedRightArm = root.addOrReplaceChild("bipedRightArm",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-2.0F, -1.0F, -1.5F, 3, 10, 3),
                PartPose.offsetAndRotation(-5.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedLeftArm = root.addOrReplaceChild("bipedLeftArm",
                CubeListBuilder.create().texOffs(12, 32)
                        .addBox(-1.0F, -1.0F, -1.5F, 3, 10, 3),
                PartPose.offsetAndRotation(5.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedRightLeg = root.addOrReplaceChild("bipedRightLeg",
                CubeListBuilder.create().texOffs(48, 16)
                        .addBox(-1.0F, 0.0F, -2.0F, 4, 14, 4),
                PartPose.offsetAndRotation(-3.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedLeftLeg = root.addOrReplaceChild("bipedLeftLeg",
                CubeListBuilder.create().texOffs(32, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 14, 4),
                PartPose.offsetAndRotation(2.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition box = bipedBody.addOrReplaceChild("box",
                    CubeListBuilder.create().texOffs(32, 10)
                            .addBox(-3.0F, -2.0F, 0.0F, 6, 4, 2),
                    PartPose.offsetAndRotation(0.0F, 8.0F, -4.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition bottom = bipedBody.addOrReplaceChild("bottom",
                    CubeListBuilder.create().texOffs(0, 46)
                            .addBox(-4.5F, 0.0F, -2.5F, 9, 13, 5),
                    PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedHead = root.addOrReplaceChild("bipedHead",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
                    CubeListBuilder.create().texOffs(24, 0)
                            .addBox(-4.0F, 0.0F, -3.0F, 8, 2, 3),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.0F, 0.0F, 0.0F));
            PartDefinition Grass = bipedHead.addOrReplaceChild("Grass",
                    CubeListBuilder.create().texOffs(48, 0)
                            .addBox(-4.0F, 0.0F, 0.0F, 8, 2, 1),
                    PartPose.offsetAndRotation(0.0F, -3.0F, -4.2F, 0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(EntityRinnosuke entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.root.getAllParts().forEach(ModelPart::resetPose);
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

    /** 度数をラジアンに変換（1.7.10 の rad() と同じ） */
    protected static float rad(float degree)
    {
        return degree / 180F * (float)Math.PI;
    }
}
