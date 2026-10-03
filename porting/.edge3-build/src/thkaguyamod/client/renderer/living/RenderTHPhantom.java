package thkaguyamod.client.renderer.living;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.living.EntityTHPhantom;

/**
 * 幽霊（{@link EntityTHPhantom}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderTHPhantom} の移植。
 *
 * <p>1.7.10 は {@code super(null, 0.25F)} でモデルを持たない {@code RenderLiving} を継承し、
 * {@code doRender} では {@code super.doRender} を呼ばずに手続き的な半透明面片だけを描いていた。
 * よって 1.21.1 では {@link EntityRenderer} を直接継承する（モデル不要）。</p>
 *
 * <p>写像:</p>
 * <ul>
 *   <li>{@code glTranslatef(x, y, z)} → EntityRenderDispatcher が poseStack に適用済み</li>
 *   <li>{@code glRotatef(180 - playerViewY, Y)} + {@code glRotatef(-playerViewX, X)} →
 *       {@code poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation())}</li>
 *   <li>{@code glBlendFunc(GL_ONE, GL_ONE)} → {@link DanmakuRenderTypes#MODE_ADD_ONE}
 *       （1.21.1 の {@code ADDITIVE_TRANSPARENCY} と同一）</li>
 *   <li>{@code glDepthMask(false)} → {@code noDepthWrite = true}</li>
 * </ul>
 *
 * <p>1.7.10 は 3 つの残影を translate/scale の積み上げで描いていた（popMatrix しない）。
 * ここでは各残影の変換を poseStack の push/pop で分離し、同じ位置・同じ大きさになるように
 * 累積値をそのまま適用している（数値は 1.7.10 と同一）。</p>
 *
 * <p>TODO K3-review: 半透明残影の「重なり」は 1.7.10 の即時モード（1 枚ごとに draw して
 * 深度を書かない）と完全には一致しない。1.21.1 はバッチ式なので同じバッファに 4 頂点ずつ
 * 積まれ、深度書き込み無し・加法ブレンドの 1 パスとして描かれる。見た目の差が出る場合は
 * 残影ごとに RenderType を分ける（深度書き込みあり版を混ぜる）必要がある。</p>
 */
public class RenderTHPhantom extends EntityRenderer<EntityTHPhantom>
{
	/** 1.7.10: textures/shot/MusouFuuin.png（弾と同じ貼り先） */
	private static final ResourceLocation PHANTOM_TEXTURE = DanmakuRenderTypes.texture("MusouFuuin");

	/** 1.7.10 の色パレット（数値はそのまま）。 */
	protected float colorR[] = { 224F/255F,   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 255F/255F, 255F/255F};
	protected float colorG[] = {   0F/255F,   0F/255F, 224F/255F,  64F/255F,   0F/255F, 224F/255F, 128F/255F, 255F/255F};
	protected float colorB[] = {   0F/255F, 224F/255F,   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 255F/255F};

	public RenderTHPhantom(EntityRendererProvider.Context context)
	{
		super(context);
		this.shadowRadius = 0.25F;
	}

	@Override
	public ResourceLocation getTextureLocation(EntityTHPhantom thPhantom)
	{
		return PHANTOM_TEXTURE;
	}

	@Override
	public void render(EntityTHPhantom entity, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource bufferSource, int packedLight)
	{
		this.renderTHPhantom(entity, poseStack, bufferSource);
	}

	/** 1.7.10 {@code renderTHPhantom}。 */
	public void renderTHPhantom(EntityTHPhantom thPhantom, PoseStack poseStack, MultiBufferSource bufferSource)
	{
		int color = thPhantom.getForm();
		double time_r = (double)thPhantom.tickCount / 180.0D * Math.PI;

		VertexConsumer buffer = bufferSource.getBuffer(
				DanmakuRenderTypes.get(PHANTOM_TEXTURE, DanmakuRenderTypes.MODE_ADD_ONE, false, true));

		poseStack.pushPose();
		// 1.7.10: glRotatef(180 - playerViewY, Y) → glRotatef(-playerViewX, X)
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

		// 1 枚目
		float size = 1.0F;
		poseStack.scale(size, size, size);
		renderPhantomPart(buffer, poseStack.last(), color, time_r, thPhantom.hurtTime);

		// 2 枚目（1.7.10 は translate/scale が積み重なる）
		poseStack.translate((float)Math.sin(time_r * 5) * 0.2F,
				0.1F + (thPhantom.tickCount % 20) * 0.06F, 0F);
		size = 0.9F * ((20F - (float)(thPhantom.tickCount % 20)) / 20F);
		poseStack.scale(size, size, size);
		renderPhantomPart(buffer, poseStack.last(), color, time_r, thPhantom.hurtTime);

		// 3 枚目
		poseStack.translate((float)Math.cos(time_r * 5) * 0.2F,
				0.1F + (thPhantom.tickCount % 20) * 0.07F, 0F);
		size = 0.8F * ((20F - (float)(thPhantom.tickCount % 20)) / 20F);
		poseStack.scale(size, size, size);
		renderPhantomPart(buffer, poseStack.last(), color, time_r, thPhantom.hurtTime);

		poseStack.popPose();
	}

	/** 1.7.10 {@code renderPhantomPart}（4 枚の面片 + 被弾時の赤フラッシュ 3 枚）。 */
	private void renderPhantomPart(VertexConsumer buffer, PoseStack.Pose pose, int color, double time_r, int damage)
	{
		int pattern = 0;//time % 2;
		float umin = (float)((pattern % 32) * 32 + 0) / 64F;
		float umax = (float)((pattern % 32) * 32 + 32) / 64F;
		float vmin = 0.0F;
		float vmax = 1.0F;

		float alpha = (40F - (float)damage) / 40F;
		float r = colorR[color] * 0.3F;
		float g = colorG[color] * 0.3F;
		float b = colorB[color] * 0.3F;

		quad(buffer, pose,
				(float)(-0.7F + Math.sin(time_r * 3.0D) * 0.1F), (float)(-0.2F - Math.cos(time_r * 7.0D) * 0.1F), 0.0F, umin, vmax,
				(float)( 0.7F - Math.cos(time_r * 4.0D) * 0.1F), (float)(-0.2F - Math.sin(time_r * 5.0D) * 0.1F), 0.0F, umax, vmax,
				(float)( 0.7F + Math.sin(time_r * 5.0D) * 0.1F), (float)( 1.2F + Math.cos(time_r * 4.0D) * 0.1F), 0.0F, umax, vmin,
				(float)(-0.7F - Math.cos(time_r * 7.0D) * 0.1F), (float)( 1.2F + Math.sin(time_r * 3.0D) * 0.1F), 0.0F, umin, vmin,
				r, g, b, 0.3F * alpha);

		quad(buffer, pose,
				(float)(-0.6F - Math.cos(time_r * 7.0D) * 0.1F), (float)(-0.1F + Math.sin(time_r * 5.0D) * 0.1F), 0.001F, umin, vmax,
				(float)( 0.6F + Math.sin(time_r * 3.0D) * 0.1F), (float)(-0.1F + Math.cos(time_r * 4.0D) * 0.1F), 0.001F, umax, vmax,
				(float)( 0.6F - Math.cos(time_r * 4.0D) * 0.1F), (float)( 1.1F - Math.sin(time_r * 3.0D) * 0.1F), 0.001F, umax, vmin,
				(float)(-0.6F + Math.sin(time_r * 5.0D) * 0.1F), (float)( 1.1F - Math.cos(time_r * 7.0D) * 0.1F), 0.001F, umin, vmin,
				r, g, b, 0.7F * alpha);

		for(int i = 0; i < 3; i++)
		{
			float fr = 1.0F, fg = 1.0F, fb = 1.0F;
			if(damage > 0)
			{
				// 1.7.10: 被弾中は赤のみ（setColorRGBA_F(1,0,0,alpha)）
				fr = 1.0F;
				fg = 0.0F;
				fb = 0.0F;
			}
			quad(buffer, pose,
					-0.5F, 0.0F, 0.002F, umin, vmax,
					 0.5F, 0.0F, 0.002F, umax, vmax,
					 0.5F, 1.0F, 0.002F, umax, vmin,
					-0.5F, 1.0F, 0.002F, umin, vmin,
					fr, fg, fb, alpha);
		}
	}

	/** 4 頂点（QUADS）。{@code DanmakuRenderTypes} の POSITION_TEX_COLOR 用。 */
	private static void quad(VertexConsumer buffer, PoseStack.Pose pose,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3,
			float r, float g, float b, float a)
	{
		buffer.addVertex(pose, x0, y0, z0).setColor(r, g, b, a).setUv(u0, v0);
		buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1);
		buffer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setUv(u2, v2);
		buffer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a).setUv(u3, v3);
	}
}