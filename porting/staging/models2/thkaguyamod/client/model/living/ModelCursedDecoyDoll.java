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

import thkaguyamod.entity.living.EntityCursedDecoyDoll;

/**
 * 呪いのデコイ人形（{@code EntityCursedDecoyDoll}）のモデル。1.7.10
 * {@code ModelCursedDecoyDoll} の移植。1.7.10 の setRotationAngles は
 * super を呼ぶだけの空実装だったので、ここもアニメーションは無い。
 *
 * <p>1.7.10 は 12 個のパーツを addChild せず render() で個別に描いていた
 * （＝回転点がそのまま絶対位置）。したがって 1.21.1 でも全パーツを root 直下の
 * フラット構造で持つ。body の下に繋ぐと頭や腕の位置が二重に加算されてしまう。</p>
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:cursed_decoy_doll}</p>
 */
public class ModelCursedDecoyDoll extends EntityModel<EntityCursedDecoyDoll>
{
    public final ModelPart bipedBody;
    public final ModelPart bipedRightArm;
    public final ModelPart bipedLeftArm;
    public final ModelPart bipedRightLeg;
    public final ModelPart bipedLeftLeg;
    public final ModelPart skirtTop;
    public final ModelPart bipedHead;
    public final ModelPart longHair;
    public final ModelPart leftBraid;
    public final ModelPart rightBraid;
    public final ModelPart leftRibbon;
    public final ModelPart rightRibbon;

    private final ModelPart root;

    public ModelCursedDecoyDoll(ModelPart root)
    {
        this.root = root;
        this.bipedBody = root.getChild("bipedBody");
        this.bipedRightArm = root.getChild("bipedRightArm");
        this.bipedLeftArm = root.getChild("bipedLeftArm");
        this.bipedRightLeg = root.getChild("bipedRightLeg");
        this.bipedLeftLeg = root.getChild("bipedLeftLeg");
        this.skirtTop = root.getChild("skirtTop");
        this.bipedHead = root.getChild("bipedHead");
        this.longHair = root.getChild("longHair");
        this.leftBraid = root.getChild("leftBraid");
        this.rightBraid = root.getChild("rightBraid");
        this.leftRibbon = root.getChild("leftRibbon");
        this.rightRibbon = root.getChild("rightRibbon");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
                CubeListBuilder.create().texOffs(32, 8)
                        .addBox(-3.0F, 0.0F, -2.0F, 6, 6, 4),
                PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedRightArm = root.addOrReplaceChild("bipedRightArm",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(0.0F, -1.0F, -1.0F, 4, 6, 2),
                PartPose.offsetAndRotation(-4.0F, 11.0F, 0.0F, 0.0F, 0.0F, 0.5235988F));
        PartDefinition bipedLeftArm = root.addOrReplaceChild("bipedLeftArm",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-4.0F, -1.0F, -1.0F, 4, 6, 2),
                PartPose.offsetAndRotation(4.0F, 11.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        PartDefinition bipedRightLeg = root.addOrReplaceChild("bipedRightLeg",
                CubeListBuilder.create().texOffs(36, 22)
                        .addBox(-1.0F, 0.0F, -2.0F, 3, 7, 4),
                PartPose.offsetAndRotation(-2.0F, 17.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedLeftLeg = root.addOrReplaceChild("bipedLeftLeg",
                CubeListBuilder.create().texOffs(36, 22)
                        .addBox(-2.0F, 0.0F, -2.0F, 3, 7, 4),
                PartPose.offsetAndRotation(2.0F, 17.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition skirtTop = root.addOrReplaceChild("skirtTop",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-4.0F, 0.0F, -5.0F, 8, 7, 6),
                PartPose.offsetAndRotation(0.0F, 16.0F, 2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bipedHead = root.addOrReplaceChild("bipedHead",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
                PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition longHair = root.addOrReplaceChild("longHair",
                CubeListBuilder.create().texOffs(0, 49)
                        .addBox(-5.0F, 0.0F, -5.0F, 10, 7, 8),
                PartPose.offsetAndRotation(0.0F, 4.0F, 2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftBraid = root.addOrReplaceChild("leftBraid",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(0.0F, 0.0F, -1.0F, 1, 6, 1),
                PartPose.offsetAndRotation(3.0F, 5.0F, -3.0F, -0.2443461F, 0.0F, 0.0F));
        PartDefinition rightBraid = root.addOrReplaceChild("rightBraid",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-1.0F, 0.0F, -1.0F, 1, 6, 1),
                PartPose.offsetAndRotation(-3.0F, 5.0F, -3.0F, -0.2443461F, 0.0F, 0.0F));
        PartDefinition leftRibbon = root.addOrReplaceChild("leftRibbon",
                CubeListBuilder.create().texOffs(46, 59)
                        .addBox(0.0F, -3.0F, 0.0F, 5, 4, 1),
                PartPose.offsetAndRotation(-4.0F, 4.0F, 0.0F, 0.4461433F, 0.0F, -0.7853982F));
        PartDefinition rightRibbon = root.addOrReplaceChild("rightRibbon",
                CubeListBuilder.create().texOffs(46, 59)
                        .addBox(0.0F, -3.0F, -1.0F, 5, 4, 1),
                PartPose.offsetAndRotation(-4.0F, 4.0F, 0.0F, -0.4461411F, 3.141593F, 0.3490659F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(EntityCursedDecoyDoll entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        // 1.7.10 の setRotationAngles は空実装
    }

    /** 度数をラジアンに変換（1.7.10 の rad() と同じ） */
    protected static float rad(float degree)
    {
        return degree / 180F * (float)Math.PI;
    }
}
