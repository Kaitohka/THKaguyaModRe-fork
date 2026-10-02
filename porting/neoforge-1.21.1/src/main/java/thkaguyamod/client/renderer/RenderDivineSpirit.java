package thkaguyamod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.EntityDivineSpirit;

/**
 * 小神霊（{@link EntityDivineSpirit}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderDivineSpirit} の移植。
 *
 * <p>1.7.10 は {@code glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)} + {@code glDepthMask(false)}
 * で揺らぐ 3 枚の面片を描いていた。{@link DanmakuRenderTypes#MODE_ADD_ONE_MINUS_SRC_COLOR}
 * （深度書き込み無し）で再現する。テクスチャは {@code textures/shot/MusouFuuin.png} のまま。</p>
 *
 * <p>1.7.10 の {@code THKaguyaRenderLib.getGLRotatefByTherdPersonView(renderManager)} は
 * {@code this.entityRenderDispatcher.cameraOrientation()} に置換。</p>
 *
 * <p>1.7.10 には色パレットが {@code RenderTHShot} のものと別に定義されており、
 * インデックス 3 の緑成分だけが異なる（{@code 64/255}）。ここでも別配列として持つ。</p>
 */
public class RenderDivineSpirit extends EntityRenderer<EntityDivineSpirit>
{
	private static final ResourceLocation TEXTURE = DanmakuRenderTypes.texture("MusouFuuin");

	protected float colorR[] = { 224F/255F,   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 255F/255F, 255F/255F};
	protected float colorG[] = {   0F/255F,   0F/255F, 224F/255F,  64F/255F,   0F/255F, 224F/255F, 128F/255F, 255F/255F};
	protected float colorB[] = {   0F/255F, 224F/255F,   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 255F/255F};

	public RenderDivineSpirit(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityDivineSpirit entity)
	{
		return TEXTURE;
	}

	@Override
	public void render(EntityDivineSpirit divineSpirit, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();

		int color = divineSpirit.getDivineSpiritColor();
		double time_r = (double)divineSpirit.tickCount / 180.0D * Math.PI;

		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

		float size = (300F - (float)divineSpirit.getDivineSpiritCount()) / 300F;
		poseStack.scale(size, size, size);

		int pattern = 0;//time % 2;
		float umin = (float)((pattern % 32) * 32 + 0) / 64F;
		float umax = (float)((pattern % 32) * 32 + 32) / 64F;
		float vmin = 0.0F;
		float vmax = 1.0F;

		VertexConsumer buffer = bufferSource.getBuffer(
				DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR, false, true));

		float r = colorR[color];
		float g = colorG[color];
		float b = colorB[color];

		quad(buffer, poseStack, r, g, b, 0.3F,
				(float)(-0.7F + Math.sin(time_r * 3.0D) * 0.1F), (float)(-0.7F - Math.cos(time_r * 7.0D) * 0.1F), 0.0F, umin, vmax,
				(float)( 0.7F - Math.cos(time_r * 4.0D) * 0.1F), (float)(-0.7F - Math.sin(time_r * 5.0D) * 0.1F), 0.0F, umax, vmax,
				(float)( 0.7F + Math.sin(time_r * 5.0D) * 0.1F), (float)( 0.7F + Math.cos(time_r * 4.0D) * 0.1F), 0.0F, umax, vmin,
				(float)(-0.7F - Math.cos(time_r * 7.0D) * 0.1F), (float)( 0.7F + Math.sin(time_r * 3.0D) * 0.1F), 0.0F, umin, vmin);

		quad(buffer, poseStack, r, g, b, 0.7F,
				(float)(-0.6F - Math.cos(time_r * 7.0D) * 0.1F), (float)(-0.6F + Math.sin(time_r * 5.0D) * 0.1F), 0.001F, umin, vmax,
				(float)( 0.6F + Math.sin(time_r * 3.0D) * 0.1F), (float)(-0.6F + Math.cos(time_r * 4.0D) * 0.1F), 0.001F, umax, vmax,
				(float)( 0.6F - Math.cos(time_r * 4.0D) * 0.1F), (float)( 0.6F - Math.sin(time_r * 3.0D) * 0.1F), 0.001F, umax, vmin,
				(float)(-0.6F + Math.sin(time_r * 5.0D) * 0.1F), (float)( 0.6F - Math.cos(time_r * 7.0D) * 0.1F), 0.001F, umin, vmin);

		quad(buffer, poseStack, r, g, b, 1.0F,
				-0.4F, -0.4F, 0.002F, umin, vmax,
				 0.4F, -0.4F, 0.002F, umax, vmax,
				 0.4F,  0.4F, 0.002F, umax, vmin,
				-0.4F,  0.4F, 0.002F, umin, vmin);

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