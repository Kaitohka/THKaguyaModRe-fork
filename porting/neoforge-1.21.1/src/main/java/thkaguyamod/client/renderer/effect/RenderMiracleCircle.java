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
import thkaguyamod.entity.effect.EntityMiracleCircle;

/**
 * 早苗の詠唱エフェクト（{@link EntityMiracleCircle}）のレンダラ。
 * 1.7.10 の {@code thKaguyaMod.client.render.effect.RenderMiracleCircle} の忠実移植。
 *
 * <p>五芒星は純粋な線分ではなく「1 本の線を細い板ポリ（QUADS）で描く」実装であり、
 * テクスチャ {@code textures/MiracleCircle.png} の u=0.5-0.625 の縦帯を線の幅方向へ引き伸ばす。
 * 1.7.10 も Tessellator の QUADS を使っていたため、LINE_STRIP ではなく QUADS のまま移植する
 * （1 本ごとに {@code startDrawingQuads()/draw()} が分かれていたが、線ごとに頂点色が同じなので
 * 1 回のバッチでも見た目は同一）。</p>
 *
 * <p>ビルボード: 1.7.10 の {@code THKaguyaRenderLib.getGLRotatefByTherdPersonView(renderManager)} は
 * {@code poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation())} に置換した
 * （{@link RenderTHShot} と同じ手法。奥行き zbuf の並びも原実装のまま）。</p>
 *
 * <p>テクスチャ: 1.7.10 の {@code textures/MiracleCircle.png} は移植先の規約に合わせて
 * {@code textures/entity/shot/miraclecircle.png} を参照する（要アセット配置）。</p>
 *
 * <p>TODO K3-review: 1.7.10 は public フィールド {@code turnAngle} を直接読んでいた。
 * 移植先の EntityMiracleCircle が getter しか公開しない場合は
 * {@code miracleCircle.getTurnAngle()} に差し替えること（下の 1 行のみ変更）。</p>
 *
 * <p>TODO K3-review: 色番号 {@code getColor()} は 0-4 前提（配列は 5 要素）。1.7.10 も
 * 範囲外なら同じく例外になるため、防御的なクランプは入れず原実装どおりにしている。</p>
 */
public class RenderMiracleCircle extends EntityRenderer<EntityMiracleCircle>
{
	//早苗の詠唱エフェクトの星の描画（1.7.10: "textures/MiracleCircle.png"）
	private static final ResourceLocation miracleCircleTexture = DanmakuRenderTypes.texture("MiracleCircle");

	/** コンストラクタ */
	public RenderMiracleCircle(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityMiracleCircle entity)
	{
		return miracleCircleTexture;
	}

	@Override
	public void render(EntityMiracleCircle entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		ResourceLocation texture = this.getTextureLocation(entity);
		poseStack.pushPose();
		RenderTHShot.Ctx ctx = new RenderTHShot.Ctx(poseStack, bufferSource, texture, this.entityRenderDispatcher);
		this.doRenderMiracleCircle(ctx, entity, entityYaw, entityYaw);
		poseStack.popPose();
	}

	/**
	 * @param yaw,pitch 1.7.10 の doRender から渡されるが、このレンダラでは原実装でも未使用。
	 */
	public void doRenderMiracleCircle(RenderTHShot.Ctx ctx, EntityMiracleCircle miracleCircle, float yaw, float pitch)
	{
		//紫、黄、赤、青、緑
		float colorR[] = {0.7F, 0.7F, 0.9F, 0.3F, 0.2F};
		float colorG[] = {0.1F, 0.7F, 0.1F, 0.3F, 0.9F};
		float colorB[] = {0.9F, 0.1F, 0.2F, 0.9F, 0.2F};
		int color = miracleCircle.getColor();

		// 1.7.10: GL11.glScalef(1.0F, 1.0F, 1.0F)（恒等変換だが原実装どおり残す）
		ctx.pose.scale(1.0F, 1.0F, 1.0F);

		// プレイヤーに常に正面を向けるようにする
		ctx.pose.mulPose(ctx.entityRenderDispatcher.cameraOrientation());
		// 1.7.10: GL11.glRotatef(miracleCircle.turnAngle, 0.0F, 0.0F, 1.0F)
		// TODO K3-review: getter 公開時は miracleCircle.getTurnAngle() へ差し替え
		float turnAngle = miracleCircle.turnAngle;
		ctx.pose.mulPose(Axis.ZP.rotationDegrees(turnAngle));

		int i;
		int j;
		float umin = 0.5F;
		float umax = 0.625F;
		float vmin = 0.0F;
		float vmax = 1.0F;
		float angle = -108F;
		float length = 3.0F;
		float width = 0.10F;
		float py =  length / (2.0F * Mth.cos(0.3141593F) );
		float px = 0F;
		float nextpx = px + Mth.cos(angle / 180F * 3.141593F) * length;
		float nextpy = py + Mth.sin(angle / 180F * 3.141593F) * length;
		float widthX;
		float widthY;
		// 1.7.10 の nextY / starNum は未使用のため移植しない
		double zbuf;
		int lastLine = miracleCircle.getNumberOfDrewLine() - 1;
		j = color;

		// 1.7.10: glEnable(GL_BLEND) + glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)、深度書き込みは有効のまま
		VertexConsumer b = ctx.buf(DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR);

		//最後の１本を除く線を描く
		for(i = 0; i < lastLine; i++)
		{
			zbuf = 0.001D * (double)i;
			widthX = Mth.cos( (angle+90F) / 180F * 3.141593F) * width;
			widthY = Mth.sin( (angle+90F) / 180F * 3.141593F) * width;
			quad(b, ctx.pose.last(),
					px - widthX, py - widthY, (float)zbuf, umin, vmax,
					px + widthX, py + widthY, (float)zbuf, umax, vmax,
					nextpx + widthX, nextpy + widthY, (float)zbuf, umax, vmin,
					nextpx - widthX, nextpy - widthY, (float)zbuf, umin, vmin,
					colorR[j], colorG[j], colorB[j], 0.5F);
			angle += 144F;
			px = nextpx;
			py = nextpy;
			nextpx = px + Mth.cos(angle / 180F * 3.141593F) * length;
			nextpy = py + Mth.sin(angle / 180F * 3.141593F) * length;
		}
		//最後の1本
		float lastLineLength = length * (float)miracleCircle.getLastLineTime() / 10F;
		nextpx = px + Mth.cos(angle / 180F * 3.141593F) * lastLineLength;
		nextpy = py + Mth.sin(angle / 180F * 3.141593F) * lastLineLength;
		zbuf = 0.001D * (double)i;
		widthX = Mth.cos( (angle+90F) / 180F * 3.141593F) * width;
		widthY = Mth.sin( (angle+90F) / 180F * 3.141593F) * width;
		quad(b, ctx.pose.last(),
				px - widthX, py - widthY, (float)zbuf, umin, vmax,
				px + widthX, py + widthY, (float)zbuf, umax, vmax,
				nextpx + widthX, nextpy + widthY, (float)zbuf, umax, vmin,
				nextpx - widthX, nextpy - widthY, (float)zbuf, umin, vmin,
				colorR[j], colorG[j], colorB[j], 0.5F);
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
		b.addVertex(pose, x0, y0, z0).setColor(r, g, bl, a).setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
		b.addVertex(pose, x1, y1, z1).setColor(r, g, bl, a).setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
		b.addVertex(pose, x2, y2, z2).setColor(r, g, bl, a).setUv(u2, v2).setLight(LightTexture.FULL_BRIGHT);
		b.addVertex(pose, x3, y3, z3).setColor(r, g, bl, a).setUv(u3, v3).setLight(LightTexture.FULL_BRIGHT);
	}
}