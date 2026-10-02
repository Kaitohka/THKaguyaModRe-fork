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

import thkaguyamod.entity.item.EntityMarisaBroom;

/**
 * 箒（{@code EntityMarisaBroom}）のモデル。1.7.10 {@code thKaguyaMod.client.model.ModelMarisaBroom}
 * の移植。1.7.10 のローカル定数 b0=24, b1=6, b2=20, b3=4 を展開した値を使い、
 * ボックス座標・サイズ・テクスチャ UV は一対一（テクスチャ 64x32）。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:marisa_broom}</p>
 */
public class ModelMarisaBroom extends EntityModel<EntityMarisaBroom>
{
	/** 柄（broom[0]） */
	public final ModelPart handle;
	/** 先端の房 4 本（broom[1..4]） */
	public final ModelPart bristleTop;
	public final ModelPart bristleBottom;
	public final ModelPart bristleLeft;
	public final ModelPart bristleRight;

	public ModelMarisaBroom(ModelPart root)
	{
		this.handle = root.getChild("handle");
		this.bristleTop = root.getChild("bristle_top");
		this.bristleBottom = root.getChild("bristle_bottom");
		this.bristleLeft = root.getChild("bristle_left");
		this.bristleRight = root.getChild("bristle_right");
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// broom[0]: addBox(-b0/2, -b2/2+2, -3, b0, b2-4, 4) / rotationPoint(0, b3, 0) / rotateAngleX = PI/2
		root.addOrReplaceChild("handle",
				CubeListBuilder.create().texOffs(0, 8)
						.addBox(-12.0F, -8.0F, -3.0F, 24.0F, 16.0F, 4.0F),
				PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, (float)Math.PI / 2F, 0.0F, 0.0F));

		// broom[1]: rotationPoint(-b0/2+1, b3, 0) / rotateAngleY = PI*3/2
		root.addOrReplaceChild("bristle_top",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F),
				PartPose.offsetAndRotation(-11.0F, 4.0F, 0.0F, 0.0F, (float)Math.PI * 3F / 2F, 0.0F));

		// broom[2]: rotationPoint(b0/2-1, b3, 0) / rotateAngleY = PI/2
		root.addOrReplaceChild("bristle_bottom",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F),
				PartPose.offsetAndRotation(11.0F, 4.0F, 0.0F, 0.0F, (float)Math.PI / 2F, 0.0F));

		// broom[3]: rotationPoint(0, b3, -b2/2+1) / rotateAngleY = PI
		root.addOrReplaceChild("bristle_left",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, 4.0F, -9.0F, 0.0F, (float)Math.PI, 0.0F));

		// broom[4]: rotationPoint(0, b3, b2/2-1) / 回転なし
		root.addOrReplaceChild("bristle_right",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F),
				PartPose.offset(0.0F, 4.0F, 9.0F));

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(EntityMarisaBroom entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		// 1.7.10 はアニメーションなし
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
	{
		this.handle.render(poseStack, buffer, packedLight, packedOverlay, color);
		this.bristleTop.render(poseStack, buffer, packedLight, packedOverlay, color);
		this.bristleBottom.render(poseStack, buffer, packedLight, packedOverlay, color);
		this.bristleLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
		this.bristleRight.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}