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

import thkaguyamod.entity.item.EntityMiniHakkero;

/**
 * ミニ八卦炉（{@code EntityMiniHakkero}）のモデル。1.7.10
 * {@code thKaguyaMod.client.model.ModelMiniHakkero} の移植。ボックス座標・サイズ・
 * テクスチャ UV は一対一（テクスチャ 64x32）。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:mini_hakkero}</p>
 */
public class ModelMiniHakkero extends EntityModel<EntityMiniHakkero>
{
	/** 前面板（miniHakkero[0]） */
	public final ModelPart front;
	/** 背面板（miniHakkero[1]） */
	public final ModelPart back;

	public ModelMiniHakkero(ModelPart root)
	{
		this.front = root.getChild("front");
		this.back = root.getChild("back");
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("front",
				CubeListBuilder.create().texOffs(0, 15)
						.addBox(-8.0F, -8.0F, 0.0F, 16.0F, 16.0F, 1.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("back",
				CubeListBuilder.create().texOffs(0, 15)
						.addBox(-8.0F, -8.0F, 5.0F, 16.0F, 16.0F, 1.0F),
				PartPose.ZERO);

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(EntityMiniHakkero entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		// 1.7.10 の setRotationAngles は空実装
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
	{
		this.front.render(poseStack, buffer, packedLight, packedOverlay, color);
		this.back.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}