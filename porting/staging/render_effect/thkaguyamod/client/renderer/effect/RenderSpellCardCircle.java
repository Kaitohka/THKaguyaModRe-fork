package thkaguyamod.client.renderer.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.client.renderer.shot.RenderTHShot;
import thkaguyamod.entity.effect.EntitySpellCardCircle;

/**
 * スペルカード宣言時の魔法陣（{@link EntitySpellCardCircle}）のレンダラ。
 * 1.7.10 の {@code thKaguyaMod.client.render.effect.RenderSpellCardCircle} の忠実移植。
 *
 * <p>1.7.10 は Tessellator + GL11 を直接叩いていた。1.21.1 では
 * {@link RenderTHShot.Ctx}（PoseStack + MultiBufferSource + 弾幕用 RenderType）に置き換え、
 * 頂点の座標・UV・配色・回転角の数値は 1.7.10 から一字も変えていない。</p>
 *
 * <p>座標系: 1.7.10 の {@code GL11.glTranslatef(x, y, z)} に相当する移動は
 * EntityRenderDispatcher が既に poseStack に適用済みなので、ここでは行わない。</p>
 *
 * <p>ブレンド: 1.7.10 の {@code glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)} →
 * {@link DanmakuRenderTypes#MODE_ADD_ONE_MINUS_SRC_COLOR}。このレンダラは
 * {@code glDepthMask} を変更していないため深度書き込みは有効のまま
 * （{@code RenderTHShot.Ctx#buf(String)} を使用）。</p>
 *
 * <p>色: 1.7.10 は 0-255 の値を {@code setColorRGBA_F} に渡しており、固定機能パイプラインが
 * [0,1] にクランプしていた。1.21.1 の {@code setColor(float...)} は 255 倍して byte 化するため
 * 事前に /255F で正規化している（{@link RenderTHShot} と同じ書式）。虹色分岐の負値も同様に
 * {@link #quad} 内でクランプする。</p>
 *
 * <p>TODO K3-review: 虹色（{@code getCircleType() % 16 == 9}）分岐は 1.7.10 の
 * {@code renderManager.playerViewY / playerViewX} を直接使っていた。ここでは
 * {@code EntityRenderDispatcher.camera.getYRot()/getXRot()} で置換しているが、
 * 三人称裏視点（1.7.10 の {@code thirdPersonView == 2}）では Camera の yaw/pitch が
 * 1.7.10 の playerViewY/X と 180° ずれる。統合時に要確認。</p>
 */
public class RenderSpellCardCircle extends EntityRenderer<EntitySpellCardCircle>
{
	//使い魔の描画（1.7.10: new ResourceLocation("thkaguyamod", "textures/mob/Familiar.png")）
	private static final ResourceLocation circleTexture = ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/familiar.png");

	/** 1.7.10 の色パレット（0-255 の数値を /255F で正規化）。 */
	protected float colorR[] = { 255F/255F, 255F/255F, 255F/255F,   0F/255F,   0F/255F,   0F/255F, 255F/255F};
	protected float colorG[] = {   0F/255F, 165F/255F, 255F/255F, 255F/255F,   0F/255F, 255F/255F,   0F/255F};
	protected float colorB[] = {   0F/255F,   0F/255F,   0F/255F,   0F/255F, 255F/255F, 255F/255F, 255F/255F};

	/** コンストラクタ */
	public RenderSpellCardCircle(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntitySpellCardCircle entity)
	{
		return circleTexture;
	}

	@Override
	public void render(EntitySpellCardCircle entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		ResourceLocation texture = this.getTextureLocation(entity);
		poseStack.pushPose();
		RenderTHShot.Ctx ctx = new RenderTHShot.Ctx(poseStack, bufferSource, texture, this.entityRenderDispatcher);
		this.renderSpellCardCircle(ctx, entity, entityYaw, entityYaw);
		poseStack.popPose();
	}

	/**
	 * @param yaw,pitch 1.7.10 の doRender から渡されるが、このレンダラでは原実装でも未使用。
	 */
	public void renderSpellCardCircle(RenderTHShot.Ctx ctx, EntitySpellCardCircle circle, float yaw, float pitch)
	{
		//形状IDがマイナスなら描画しない（1.7.10 ではコメントアウトされているため移植しない）

		float size = circle.getCircleSize() * 6.0F;
		ctx.pose.scale(size, size, size);

		float umin = 0.0F;
		float umax = 1.0F;
		float vmin = 0.0F;
		float vmax = 1.0F;

		boolean rainbow = false;
		int color = circle.getCircleType();
		if(color >= 16)
		{
			float angle = Mth.sin(2F * (float)circle.getAnimationCount() / 180F * (float)Math.PI) * 40F;
			// 1.7.10: GL11.glRotatef(180F - renderManager.playerViewY + angle, 0.0F, 1.0F, 0.0F)
			ctx.pose.mulPose(Axis.YP.rotationDegrees(180F - ctx.entityRenderDispatcher.camera.getYRot() + angle));
			// 1.7.10: GL11.glRotatef(-renderManager.playerViewX - angle, 1.0F, 0.0F, 0.0F)
			ctx.pose.mulPose(Axis.XP.rotationDegrees(-ctx.entityRenderDispatcher.camera.getXRot() - angle));
			// 1.7.10: GL11.glRotatef(circle.getAnimationCount() * 5F, 0.0F, 0.0F, 1.0F)
			ctx.pose.mulPose(Axis.ZP.rotationDegrees(circle.getAnimationCount() * 5F));
			color %= 16;
		}
		else
		{
			ctx.pose.mulPose(Axis.XP.rotationDegrees(90F));
			ctx.pose.mulPose(Axis.ZP.rotationDegrees(circle.getAnimationCount() * 5F));
		}

		if(color == 9)
		{
			rainbow = true;
		}
		else
		{
			color %= 8;
		}

		float r;
		float g;
		float b;
		if(rainbow)
		{
			float rainbowTime = circle.tickCount / 3.0F;// 1.7.10 の ticksExisted
			r = Mth.sin(rainbowTime);
			g = Mth.cos(rainbowTime);
			b = -Mth.sin(rainbowTime) * 2F;
		}
		else
		{
			r = colorR[color];
			g = colorG[color];
			b = colorB[color];
		}

		// 1.7.10: glEnable(GL_BLEND) + glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)
		VertexConsumer vertex = ctx.buf(DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR);
		quad(vertex, ctx.pose.last(),
				-0.6F, -0.6F, 0.0F, umin, vmax,
				 0.6F, -0.6F, 0.0F, umax, vmax,
				 0.6F,  0.6F, 0.0F, umax, vmin,
				-0.6F,  0.6F, 0.0F, umin, vmin,
				r, g, b, 1.0F);
	}

	/** 4 頂点（QUADS）を 1 枚書き込む。UV は addVertexWithUV の (u,v) をそのまま渡す。 */
	private static void quad(VertexConsumer b, PoseStack.Pose pose,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3,
			float r, float g, float bl, float a)
	{
		// 1.7.10 の getBrightnessForRender() は常に 0xf000f0 を返す → LightTexture.FULL_BRIGHT。
		// POSITION_TEX_COLOR にライトマップ要素は無いため setLight は no-op だが、
		// 「常に最大輝度」という原仕様をここに明示しておく。
		// 色は固定機能の glColor4f と同じく [0,1] にクランプする（虹色分岐の負値対策）。
		b.addVertex(pose, x0, y0, z0).setColor(clamp01(r), clamp01(g), clamp01(bl), clamp01(a)).setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
		b.addVertex(pose, x1, y1, z1).setColor(clamp01(r), clamp01(g), clamp01(bl), clamp01(a)).setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
		b.addVertex(pose, x2, y2, z2).setColor(clamp01(r), clamp01(g), clamp01(bl), clamp01(a)).setUv(u2, v2).setLight(LightTexture.FULL_BRIGHT);
		b.addVertex(pose, x3, y3, z3).setColor(clamp01(r), clamp01(g), clamp01(bl), clamp01(a)).setUv(u3, v3).setLight(LightTexture.FULL_BRIGHT);
	}

	private static float clamp01(float value)
	{
		return Mth.clamp(value, 0.0F, 1.0F);
	}
}