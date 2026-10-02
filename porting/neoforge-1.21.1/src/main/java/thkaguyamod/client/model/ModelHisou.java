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

import thkaguyamod.entity.item.EntityHisou;

/**
 * 緋想の剣（{@code EntityHisou}）のモデル。1.7.10 {@code thKaguyaMod.client.model.ModelHisou}
 * の移植。ボックス座標・サイズ・テクスチャ UV は 1.7.10 と一対一（テクスチャ 64x32）。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:hisou}</p>
 */
public class ModelHisou extends EntityModel<EntityHisou>
{
	/** 持つ場所 */
	public final ModelPart holder;
	/** 刃の部分 */
	public final ModelPart blade;
	/** 把手 */
	public final ModelPart handle;

	public ModelHisou(ModelPart root)
	{
		this.holder = root.getChild("holder");
		this.blade = root.getChild("blade");
		this.handle = root.getChild("handle");
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("holder",
				CubeListBuilder.create().texOffs(32, 0)
						.addBox(-2.0F, -12.0F, -2.0F, 4.0F, 8.0F, 4.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("blade",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-2.0F, -4.0F, -2.0F, 4.0F, 28.0F, 4.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("handle",
				CubeListBuilder.create().texOffs(32, 16)
						.addBox(-2.0F, -20.0F, 0.0F, 12.0F, 12.0F, 0.0F),
				PartPose.ZERO);

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(EntityHisou entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		// 1.7.10 はアニメーションなし（描画側で yaw / pitch / angle を poseStack に適用していた）
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
	{
		this.holder.render(poseStack, buffer, packedLight, packedOverlay, color);
		this.blade.render(poseStack, buffer, packedLight, packedOverlay, color);
		this.handle.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}
