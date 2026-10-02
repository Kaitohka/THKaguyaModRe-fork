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
 * 魔理沙の帽子のモデル。1.7.10 {@code thKaguyaMod.client.model.ModelMarisaHat}（ModelBiped 継承）
 * の移植。帽子の箱は 1.7.10 では {@code bipedHead} の子で、{@code render} が
 * {@code bipedHead.render(...)} だけを呼んでいた（素の頭ボックス＋帽子のみ描画）。
 *
 * <p>1.21.1 では {@link HumanoidModel} を継承し、素の頭ボックスは
 * {@code HumanoidModel.createMesh} が作る標準 "head"、帽子はその子パーツ
 * "hat_base" 以下に置く。{@link #renderToBuffer} は 1.7.10 と同じく頭（＋帽子）だけを描画する。</p>
 *
 * <p>1.7.10 のコンストラクタ引数 {@code scale} は全ボックスの {@code CubeDeformation}
 * （旧 {@code addBox} の最後の float = 成長量）で、唯一の呼び出し元 {@code ItemMarisaHat} が
 * 0.4F を渡していた。{@link #createBodyLayer()} はその 0.4F を既定値として再現する。
 * 別値で bake したい場合は {@link #createBodyLayer(CubeDeformation)} を使う。</p>
 *
 * <p>頭の原点: 1.7.10 の {@code bipedHead.rotationPoint} は {@code (0, 0, 0)}（{@code ModelBiped}
 * 由来、体の原点＝足元基準）で、帽子の箱はその原点基準で足されていた。1.21.1 の
 * {@code HumanoidModel.createMesh} の "head" も {@code offset(0, 0, 0)} なので原点は一致する。</p>
 *
 * <p>1.7.10 の引数 {@code yOffset} は 1.7.10 側でも未使用（受け取るだけでどこにも加算していない）
 * なので移植対象なし。1.21.1 でこのモデルを使う防具は
 * {@code IClientItemExtensions#getHumanoidArmorModel} 経由で本クラスを返すこと（アイテム側の
 * フックは本批次対象外）。</p>
 *
 * <p>モデルレイヤー名（主控が登録）: {@code thkaguyamod:marisa_hat}</p>
 */
public class ModelMarisaHat extends HumanoidModel<LivingEntity>
{
	/** 帽子ベース */
	public final ModelPart hatBase;
	public final ModelPart hatBase2;
	public final ModelPart hatBase3;
	public final ModelPart hatBase4;
	/** つば */
	public final ModelPart brim;
	/** リボン（右 / 左） */
	public final ModelPart ribbonRight;
	public final ModelPart ribbonLeft;

	public ModelMarisaHat(ModelPart root)
	{
		super(root);
		this.hatBase = this.head.getChild("hat_base");
		this.hatBase2 = this.hatBase.getChild("hat_base2");
		this.hatBase3 = this.hatBase.getChild("hat_base3");
		this.hatBase4 = this.hatBase.getChild("hat_base4");
		this.brim = this.hatBase.getChild("brim");
		this.ribbonRight = this.hatBase.getChild("ribbon_right");
		this.ribbonLeft = this.hatBase.getChild("ribbon_left");
	}

	/** 1.7.10 の唯一の呼び出し元（{@code ItemMarisaHat}）と同じ 0.4F の成長量で bake する。 */
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
						.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 4.0F, 8.0F, deformation),
				PartPose.ZERO);
		hatBase.addOrReplaceChild("hat_base2",
				CubeListBuilder.create().texOffs(32, 2).mirror()
						.addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F, deformation),
				PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.2792527F, 0.0F, 0.0F));
		hatBase.addOrReplaceChild("hat_base3",
				CubeListBuilder.create().texOffs(48, 16).mirror()
						.addBox(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F, deformation),
				PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, -0.5410521F, 0.0F, 0.0F));
		hatBase.addOrReplaceChild("hat_base4",
				CubeListBuilder.create().texOffs(48, 24).mirror()
						.addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, deformation),
				PartPose.offsetAndRotation(0.0F, -11.0F, 1.0F, -0.8203047F, 0.0F, 0.0F));
		hatBase.addOrReplaceChild("brim",
				CubeListBuilder.create().texOffs(0, 16).mirror()
						.addBox(-6.0F, 0.0F, -6.0F, 12.0F, 1.0F, 12.0F, deformation),
				PartPose.offset(0.0F, -6.0F, 0.0F));
		hatBase.addOrReplaceChild("ribbon_right",
				CubeListBuilder.create().texOffs(44, 12).mirror()
						.addBox(0.0F, -2.0F, -1.0F, 4.0F, 3.0F, 1.0F, deformation),
				PartPose.offsetAndRotation(0.5F, -6.0F, -4.0F, 0.0F, 0.0F, -0.3490659F));
		hatBase.addOrReplaceChild("ribbon_left",
				CubeListBuilder.create().texOffs(32, 12).mirror()
						.addBox(-4.0F, -2.0F, -1.0F, 4.0F, 3.0F, 1.0F, deformation),
				PartPose.offsetAndRotation(-0.5F, -6.0F, -4.0F, 0.0F, 0.0F, 0.3490659F));

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		// 1.7.10 の setRotationAngles は super（ModelBiped）へ委譲するだけだった
		super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
	{
		// 1.7.10 の render は bipedHead.render(size) のみ（頭ボックス＋帽子の子パーツ）
		this.head.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}