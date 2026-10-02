package thkaguyamod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import thkaguyamod.entity.item.EntityKinkakuzi;

/**
 * 金閣寺（{@code EntityKinkakuzi}）のモデル。1.7.10 {@code thKaguyaMod.client.model.ModelKinkakuzi}
 * の移植。ボックス座標・サイズ・テクスチャ UV は 1.7.10 と一対一（テクスチャ 64x32）。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:kinkakuzi}</p>
 */
public class ModelKinkakuzi extends EntityModel<EntityKinkakuzi>
{
	public final ModelPart kinkakuzi;

	public ModelKinkakuzi(ModelPart root)
	{
		this.kinkakuzi = root.getChild("kinkakuzi");
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("kinkakuzi",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-15.0F, -15.0F, -2.0F, 30.0F, 30.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float)Math.PI / 2F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(EntityKinkakuzi entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		// 1.7.10 の setRotationAngles は空実装
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
	{
		this.kinkakuzi.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}