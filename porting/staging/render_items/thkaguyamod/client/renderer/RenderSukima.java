package thkaguyamod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.item.EntitySukima;

/**
 * スキマ（{@link EntitySukima}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderSukima} の移植。
 *
 * <p>1.7.10 は本体 → 黒の重ね → リボン（表 2 枚 + 裏 2 枚）の順に
 * {@code Tessellator.instance} で描いていた。ここでも同じ順序で、
 * ブレンド有無ごとに {@link DanmakuRenderTypes} のモードを切り替えて書き込む。</p>
 *
 * <p>TODO K3-review: 1.7.10 の黒の重ねとリボンは
 * {@code glBlendFunc(GL_SRC_ALPHA, GL_SRC_ALPHA)} だった。{@link DanmakuRenderTypes} には
 * この組み合わせのモードが無いため {@link DanmakuRenderTypes#MODE_ADD_SRC_ALPHA}
 * （＝glBlendFunc(GL_SRC_ALPHA, GL_ONE)）で代用している。忠実に再現するには
 * DanmakuRenderTypes に (SRC_ALPHA, SRC_ALPHA) モードを追加すること。</p>
 *
 * <p>TODO K3-review: テクスチャ未コピー。1.7.10 {@code textures/SukimaTexture3.png} →
 * {@code textures/entity/shot/sukimatexture3.png} を想定し、{@link DanmakuRenderTypes#texture(String)}
 * の規則で解決している。</p>
 */
public class RenderSukima extends EntityRenderer<EntitySukima>
{
	private static final ResourceLocation TEXTURE = DanmakuRenderTypes.texture("SukimaTexture3");

	public RenderSukima(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntitySukima entity)
	{
		return TEXTURE;
	}

	@Override
	public void render(EntitySukima sukima, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		int colorR[] = { 30,179, 59, 81, 37,123, 40,128, 51,216, 65,222,102,211,237,240,179};
		int colorG[] = { 27, 49, 81, 48, 49, 47,118,128, 51,129,203,207,133, 84,136,240, 49};
		int colorB[] = { 27, 44, 26, 26,146,190,151,128, 51,152, 51, 42,219,205, 68,240, 44};

		poseStack.pushPose();
		// 1.7.10: GL11.glTranslatef(x, y + 1.0F + sin(ticksExisted / 20F) * 0.1F, z)
		poseStack.translate(0.0F, 1.0F + Mth.sin((float)sukima.tickCount / 20F) * 0.1F, 0.0F);

		int color = sukima.getColor();
		int color2 = 0;
		float f3 = (float)(color2 * 16) / 64F;// 1.7.10 でも未使用
		float f4 = (float)(color2 * 16 + 16) / 64F;// 同上
		float f5 = 0.0F;
		float f6 = 1.0F;
		float f7 = 1.0F;
		float f8 = 0.5F;
		float f9 = 0.25F;
		float xl =  1.0F;
		float yl = -0.75F;
		float xr = -1.0F;
		float yr =  0.75F;
		int time = sukima.tickCount;
		float openRate = Mth.sin((float)time * 15F / 360F * 3.141593F);

		if (color == 17)
		{
			openRate = Mth.sin((float)time * 40F / 360F * 3.141593F);
			color = 16;
		}
		if (color >= 16)
		{
			xl *= openRate;
			yl *= openRate;
			xr *= openRate;
			yr *= openRate;
		}

		color = color % 32;

		poseStack.mulPose(Axis.YP.rotationDegrees(180F - entityYaw));

		// スキマ本体（ブレンド無効）
		VertexConsumer bodyBuffer = bufferSource.getBuffer(DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_OPAQUE));
		quad(bodyBuffer, poseStack, 1F, 1F, 1F, 1F,
				-1.0F, -0.75F, 0.0F,  0.00F, 1.00F,
				 xl,      yl,   0.0F,  0.75F, 1.00F,
				 1.0F,   0.75F, 0.0F,  0.75F, 0.00F,
				 xr,      yr,   0.0F,  0.00F, 0.00F);

		// 黒の重ね（1.7.10: glBlendFunc(GL_SRC_ALPHA, GL_SRC_ALPHA)）
		VertexConsumer blackBuffer = bufferSource.getBuffer(DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_ADD_SRC_ALPHA));
		quad(blackBuffer, poseStack, 0F, 0F, 0F, 0.7F,
				 xl,     yl,   -0.0009F, 0.75F, 1.00F,
				-1.0F, -0.75F, -0.0009F, 0.00F, 1.00F,
				 xr,     yr,   -0.0009F, 0.00F, 0.00F,
				 1.0F,  0.75F, -0.0009F, 0.75F, 0.00F);

		// スキマのリボン　２つ
		poseStack.mulPose(Axis.ZP.rotationDegrees(30F));

		float r = (float)colorR[color] / 255F;
		float g = (float)colorG[color] / 255F;
		float b = (float)colorB[color] / 255F;
		VertexConsumer ribbonBuffer = bufferSource.getBuffer(DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_ADD_SRC_ALPHA));

		//右上　表
		quad(ribbonBuffer, poseStack, r, g, b, 0.7F,
				 1.00F, -0.2F, 0.001F, 0.75F, 1.00F,
				 1.25F, -0.2F, 0.001F, 1.00F, 1.00F,
				 1.25F,  0.3F, 0.001F, 1.00F, 0.00F,
				 1.00F,  0.3F, 0.001F, 0.75F, 0.00F);

		//左下　表
		quad(ribbonBuffer, poseStack, r, g, b, 0.7F,
				-1.30F, -0.3F, 0.001F, 0.75F, 1.00F,
				-1.05F, -0.3F, 0.001F, 1.00F, 1.00F,
				-1.05F,  0.2F, 0.001F, 1.00F, 0.00F,
				-1.30F,  0.2F, 0.001F, 0.75F, 0.00F);

		//右上　裏
		quad(ribbonBuffer, poseStack, 0F, 0F, 0F, 0.7F,
				 1.00F,  0.3F, -0.001F, 0.75F, 0.00F,
				 1.25F,  0.3F, -0.001F, 1.00F, 0.00F,
				 1.25F, -0.2F, -0.001F, 1.00F, 1.00F,
				 1.00F, -0.2F, -0.001F, 0.75F, 1.00F);

		//左下　裏
		quad(ribbonBuffer, poseStack, 0F, 0F, 0F, 0.7F,
				-1.30F,  0.2F, -0.001F, 0.75F, 0.00F,
				-1.05F,  0.2F, -0.001F, 1.00F, 0.00F,
				-1.05F, -0.3F, -0.001F, 1.00F, 1.00F,
				-1.30F, -0.3F, -0.001F, 0.75F, 1.00F);

		poseStack.popPose();
	}

	private static void quad(VertexConsumer buffer, PoseStack poseStack, float r, float g, float b, float a,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3)
	{
		int color = FastColor.ARGB32.color((int)(a * 255F), (int)(r * 255F), (int)(g * 255F), (int)(b * 255F));
		buffer.addVertex(poseStack.last().pose(), x0, y0, z0).setColor(color).setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x1, y1, z1).setColor(color).setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x2, y2, z2).setColor(color).setUv(u2, v2).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x3, y3, z3).setColor(color).setUv(u3, v3).setLight(LightTexture.FULL_BRIGHT);
	}
}