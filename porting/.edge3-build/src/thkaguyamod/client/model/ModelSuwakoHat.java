package thkaguyamod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * 諏訪子の帽子のモデル。1.7.10 {@code thKaguyaMod.client.model.ModelSuwakoHat}（ModelBiped 継承）
 * の移植。1.7.10 では帽子の箱は {@code bipedHead} の子で、{@code render} は
 * {@code bipedHead.render(...)} のみを呼んでいた（素の頭ボックス＋帽子のみ描画）。
 *
 * <p>1.21.1 では {@link HumanoidModel} を継承し、素の頭ボックスは
 * {@code HumanoidModel.createMesh} が作る標準 "head"、帽子はその子パーツ "hat_base" 以下に置く。</p>
 *
 * <p>1.7.10 のコンストラクタ引数 {@code scale} は全ボックスの {@code CubeDeformation}
 * （旧 {@code addBox} の最後の float）で、唯一の呼び出し元 {@code ItemKerobou} が 0.4F を
 * 渡していた。{@link #createBodyLayer()} はその 0.4F を既定値として再現する。</p>
 *
 * <p>1.7.10 の引数 {@code yOffset} は 1.7.10 側でも未使用（受け取るだけでどこにも加算していない）
 * なので移植対象なし。1.21.1 でこのモデルを使う防具は
 * {@code IClientItemExtensions#getHumanoidArmorModel} 経由で本クラスを返すこと（アイテム側の
 * フックは本批次対象外）。</p>
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:suwako_hat}</p>
 */
public class ModelSuwakoHat extends HumanoidModel<LivingEntity>
{
	/** 帽子ベース */
	public final ModelPart hatBase;
	/** 目（右 / 左） */
	public final ModelPart eyeRight;
	public final ModelPart eyeLeft;
	/** つば */
	public final ModelPart brim;

	public ModelSuwakoHat(ModelPart root)
	{
		super(root);
		this.hatBase = this.head.getChild("hat_base");
		this.eyeRight = this.hatBase.getChild("eye_right");
		this.eyeLeft = this.hatBase.getChild("eye_left");
		this.brim = this.hatBase.getChild("brim");
	}

	/** 1.7.10 の唯一の呼び出し元（{@code ItemKerobou}）と同じ 0.4F の成長量で bake する。 */
	public static LayerDefinition createBodyLayer()
	{
		return createBodyLayer(new CubeDeformation(0.4F));
	}

	public static LayerDefinition createBodyLayer(CubeDeformation deformation)
	{
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition head = mesh.getRoot().getChild("head");

		PartDefinition hatBase = head.addOrReplaceChild("hat_base",
				CubeListBuilder.create().texOffs(0, 0).mirror()
						.addBox(-4.0F, -9.0F, -4.0F, 8.0F, 4.0F, 8.0F, deformation),
				PartPose.ZERO);
		hatBase.addOrReplaceChild("eye_right",
				CubeListBuilder.create().texOffs(0, 0).mirror()
						.addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, deformation),
				PartPose.offset(-4.0F, -9.0F, -4.0F));
		hatBase.addOrReplaceChild("eye_left",
				CubeListBuilder.create().texOffs(24, 0).mirror()
						.addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, deformation),
				PartPose.offset(4.0F, -9.0F, -4.0F));
		hatBase.addOrReplaceChild("brim",
				CubeListBuilder.create().texOffs(0, 16).mirror()
						.addBox(-6.0F, -1.0F, -6.0F, 12.0F, 1.0F, 12.0F, deformation),
				PartPose.offset(0.0F, -5.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		// 1.7.10 の setRotationAngles は ModelBiped の既定実装（＝super）そのまま
		super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
	{
		// 1.7.10 の render は bipedHead.render(size) のみ（頭ボックス＋帽子の子パーツ）
		this.head.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}