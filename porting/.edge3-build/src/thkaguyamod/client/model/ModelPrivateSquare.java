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

import net.minecraft.world.entity.Entity;

/**
 * 咲夜の懐中時計のモデル。1.7.10 {@code thKaguyaMod.client.model.ModelPrivateSquare} の移植。
 * 1.7.10 では時間停止空間 {@code EntitySakuyaWatch} と時間停止（短時間）の
 * {@code EntitySakuyaStopWatch} の双方がこのモデルを使っていた。1.21.1 では両者の共通基底
 * {@link EntitySakuyaWatch} を型引数に取り、どちらのレンダラからも使えるようにしている。
 * ボックス座標・サイズ・テクスチャ UV は一対一（テクスチャ 64x32）。
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:private_square}</p>
 */
public class ModelPrivateSquare extends EntityModel<Entity>
{
	/** 時計本体 */
	public final ModelPart watchBase;
	/** 時計中心 */
	public final ModelPart watchCenter;
	/** 時計取っ手 */
	public final ModelPart watchHandle;
	/** 時計カバー */
	public final ModelPart watchCover;

	public ModelPrivateSquare(ModelPart root)
	{
		this.watchBase = root.getChild("watch_base");
		this.watchCenter = root.getChild("watch_center");
		this.watchHandle = root.getChild("watch_handle");
		this.watchCover = root.getChild("watch_cover");
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("watch_base",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-6.0F, -6.0F, -2.0F, 12.0F, 12.0F, 4.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("watch_center",
				CubeListBuilder.create().texOffs(28, 14)
						.addBox(-8.0F, -8.0F, -1.0F, 16.0F, 16.0F, 2.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("watch_handle",
				CubeListBuilder.create().texOffs(32, 0)
						.addBox(-4.0F, 8.0F, 0.0F, 8.0F, 6.0F, 0.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("watch_cover",
				CubeListBuilder.create().texOffs(48, 16)
						.addBox(-8.0F, -8.0F, 0.0F, 16.0F, 16.0F, 0.0F),
				PartPose.offsetAndRotation(0.0F, -16.0F, -4.0F, (float)Math.PI / 6F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		// 1.7.10 はモデル側のアニメーションなし（回転はレンダラが poseStack に適用していた）
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
	{
		this.watchBase.render(poseStack, buffer, packedLight, packedOverlay, color);
		this.watchCenter.render(poseStack, buffer, packedLight, packedOverlay, color);
		this.watchHandle.render(poseStack, buffer, packedLight, packedOverlay, color);
		this.watchCover.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}