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

import thkaguyamod.entity.living.EntityTHFairy;

/**
 * チルノ第2形態用の羽だけのモデル。1.7.10 {@code ModelTHFairyCirno2} の移植。
 * 1.7.10 ではどのレンダラからも参照されていない（死にコード）が、移植対象に
 * 含まれていたため同じ内容で残す。1.7.10 は render() の中で羽の位置と角度を
 * 毎フレーム設定していたので、その処理を setupAnim に移した。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:th_fairy_cirno2}（未使用）</p>
 */
public class ModelTHFairyCirno2 extends EntityModel<EntityTHFairy>
{
    public final ModelPart rightWing;
    public final ModelPart leftWing;
    public final ModelPart rightWing2;
    public final ModelPart leftWing2;
    public final ModelPart rightWing3;
    public final ModelPart leftWing3;

    private final ModelPart root;

    public ModelTHFairyCirno2(ModelPart root)
    {
        this.root = root;
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

        PartDefinition rightWing = root.addOrReplaceChild("rightWing",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(-14.0F, 0.0F, 0.0F, 14, 3, 3),
                PartPose.offsetAndRotation(-1.0F, -2.0F, 2.0F, 0.0F, 0.4886922F, 0.3141593F));
        PartDefinition leftWing = root.addOrReplaceChild("leftWing",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(0.0F, 0.0F, 0.0F, 14, 3, 3),
                PartPose.offsetAndRotation(0.0F, -2.0F, 2.0F, 0.0F, -0.4886922F, -0.3141593F));
        PartDefinition rightWing2 = root.addOrReplaceChild("rightWing2",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(-14.0F, 0.0F, 0.0F, 14, 3, 3),
                PartPose.offsetAndRotation(-1.0F, 1.0F, 2.0F, 0.0F, 0.4886922F, -0.3141593F));
        PartDefinition leftWing2 = root.addOrReplaceChild("leftWing2",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(0.0F, 0.0F, 0.0F, 14, 3, 3),
                PartPose.offsetAndRotation(1.0F, 1.0F, 2.0F, 0.0F, -0.4886922F, 0.3141593F));
        PartDefinition rightWing3 = root.addOrReplaceChild("rightWing3",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(-14.0F, 0.0F, 0.0F, 14, 3, 3),
                PartPose.offsetAndRotation(-1.0F, 0.0F, 2.0F, 0.0F, 0.4886922F, 0.0F));
        PartDefinition leftWing3 = root.addOrReplaceChild("leftWing3",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(0.0F, 0.0F, 0.0F, 14, 3, 3),
                PartPose.offsetAndRotation(1.0F, 0.0F, 2.0F, 0.0F, -0.4886922F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(EntityTHFairy entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.root.getAllParts().forEach(ModelPart::resetPose);
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

    /** 度数をラジアンに変換（1.7.10 の rad() と同じ） */
    protected static float rad(float degree)
    {
        return degree / 180F * (float)Math.PI;
    }
}
