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

import thkaguyamod.entity.item.EntitySilverKnife;

/**
 * 銀のナイフのモデル。1.7.10 {@code thKaguyaMod.client.model.ModelSilverKnife} の移植。
 * ボックス座標・サイズ・テクスチャ UV は一対一（テクスチャ 64x32）。
 *
 * <p>1.7.10 {@code RenderTHShot} の FORM_KNIFE もこのモデルを使っていた（ナイフ弾は
 * {@link EntitySilverKnife} を使わないため、そちらからの利用は型引数の見直しか、
 * モデルを別途 bake して直接描画する必要がある。主控への引き継ぎ事項）。</p>
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:silver_knife}</p>
 */
public class ModelSilverKnife extends EntityModel<EntitySilverKnife>
{
	/** 鍔 */
	public final ModelPart guard;
	/** 柄 */
	public final ModelPart grip;
	/** 刃部 小 */
	public final ModelPart bladeTip;

	public ModelSilverKnife(ModelPart root)
	{
		this.guard = root.getChild("guard");
		this.grip = root.getChild("grip");
		this.bladeTip = root.getChild("blade_tip");
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("guard",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-4.0F, -5.0F, -1.0F, 7.0F, 1.0F, 3.0F),
				PartPose.rotation((float)Math.PI / 2F, 0.0F, 0.0F));
		root.addOrReplaceChild("grip",
				CubeListBuilder.create().texOffs(0, 16)
						.addBox(-2.0F, -9.0F, 0.0F, 3.0F, 14.0F, 1.0F),
				PartPose.rotation((float)Math.PI / 2F, 0.0F, 0.0F));
		root.addOrReplaceChild("blade_tip",
				CubeListBuilder.create().texOffs(32, 16)
						.addBox(-1.0F, 5.0F, 0.0F, 1.0F, 1.0F, 1.0F),
				PartPose.rotation((float)Math.PI / 2F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(EntitySilverKnife entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		// 1.7.10 はアニメーションなし
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
	{
		this.guard.render(poseStack, buffer, packedLight, packedOverlay, color);
		this.grip.render(poseStack, buffer, packedLight, packedOverlay, color);
		this.bladeTip.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}