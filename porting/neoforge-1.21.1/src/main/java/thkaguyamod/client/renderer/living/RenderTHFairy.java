package thkaguyamod.client.renderer.living;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.model.living.ModelTHFairy;
import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.living.EntityTHFairy;

/**
 * 妖精（{@link EntityTHFairy}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderTHFairy} の移植。
 *
 * <p>モデル描画は {@link LivingEntityRenderer} に任せ、その後に「ゾンビ妖精（体力 0 かつ
 * 弾幕パターンが負）の頭上に浮かぶ輪っか」を面片で足す。1.7.10 は Tessellator を
 * {@code glBlendFunc(GL_SRC_ALPHA, GL_ONE)}・カリング無効で使い、輪を y+2.5 の水平四角形として
 * 描いていた。ここでは同じブレンドの {@link DanmakuRenderTypes#MODE_ADD_SRC_ALPHA} を使う。</p>
 *
 * <p>1.7.10 の {@code renderDark}（時停エフェクト用）は呼び出し側がコメントアウトされていた
 * 死コードなので移植しない。</p>
 */
public class RenderTHFairy extends LivingEntityRenderer<EntityTHFairy, ModelTHFairy>
{
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "th_fairy"), "main");

	/** 1.7.10: textures/mob/FairyTexture_0..2.png / ZombieFairyTexture.png → 全小文字で entity/living 配下 */
	private static final String FAIRY_TEXTURE_PREFIX = "textures/entity/living/fairytexture_";
	private static final ResourceLocation ZOMBIE_FAIRY_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/zombiefairytexture.png");

	public RenderTHFairy(EntityRendererProvider.Context context)
	{
		super(context, new ModelTHFairy(context.bakeLayer(LAYER_LOCATION)), 0.25F);
	}

	@Override
	public void render(EntityTHFairy entity, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource bufferSource, int packedLight)
	{
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
		this.renderTHFairy(entity, poseStack, bufferSource);
	}

	/**
	 * 1.7.10 {@code renderTHFairy}: ゾンビ妖精かつ体力がないなら輪っかを描画する。
	 */
	public void renderTHFairy(EntityTHFairy thFairy, PoseStack poseStack, MultiBufferSource bufferSource)
	{
		if(thFairy.getHealth() > 0.0F)
		{
			return;
		}
		if(thFairy.getDanmakuPattern() >= 0)
		{
			return;
		}

		// 1.7.10: glBlendFunc(GL_SRC_ALPHA, GL_ONE) + カリング無効、テクスチャは自分の貼り先
		ResourceLocation texture = this.getTextureLocation(thFairy);
		VertexConsumer buffer = bufferSource.getBuffer(
				DanmakuRenderTypes.get(texture, DanmakuRenderTypes.MODE_ADD_SRC_ALPHA));

		// 1.7.10 のバインド済みテクスチャ UV（f3..f9）。数値はそのまま。
		float f3 = 0.75F;
		float f4 = 1.0F;
		float f5 = 0.5F;
		float f6 = 0.75F;

		PoseStack.Pose pose = poseStack.last();
		// 1.7.10 の頂点はワールド座標 (x±0.5, y+2.5, z±0.5)。poseStack は既にエンティティ位置へ移動済み。
		quad(buffer, pose,
				-0.5F, 2.5F,  0.5F, f3, f6,
				 0.5F, 2.5F,  0.5F, f4, f6,
				 0.5F, 2.5F, -0.5F, f4, f5,
				-0.5F, 2.5F, -0.5F, f3, f5);
	}

	/** 4 頂点（QUADS）。{@code DanmakuRenderTypes} の POSITION_TEX_COLOR 用。 */
	private static void quad(VertexConsumer buffer, PoseStack.Pose pose,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3)
	{
		buffer.addVertex(pose, x0, y0, z0).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u0, v0);
		buffer.addVertex(pose, x1, y1, z1).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u1, v1);
		buffer.addVertex(pose, x2, y2, z2).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u2, v2);
		buffer.addVertex(pose, x3, y3, z3).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u3, v3);
	}

	/**
	 * 1.7.10 {@code getEntityTexture}: {@code form >= 0} なら
	 * {@code "FairyTexture_" + danmakuPattern % 3}、そうでなければ {@code ZombieFairyTexture}。
	 */
	@Override
	public ResourceLocation getTextureLocation(EntityTHFairy thFairy)
	{
		if(thFairy.getForm() >= 0)
		{
			int pattern = thFairy.getDanmakuPattern() % 3;
			return ResourceLocation.fromNamespaceAndPath("thkaguyamod", FAIRY_TEXTURE_PREFIX + pattern + ".png");
		}
		return ZOMBIE_FAIRY_TEXTURE;
	}
}
