package thkaguyamod.client.renderer;

import java.util.Random;

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
import thkaguyamod.entity.effect.EntityAjaRedStoneEffect;

/**
 * エイジャの赤石のエフェクト（{@link EntityAjaRedStoneEffect}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderAjaRedStoneEffect} の移植。
 *
 * <p>1.7.10 は {@code glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)} + {@code glDepthMask(false)}
 * で放射状の光条を描いていた。{@link DanmakuRenderTypes#MODE_ADD_ONE_MINUS_SRC_COLOR}
 * （深度書き込み無し）で再現する。</p>
 *
 * <p>1.7.10 の {@code glRotatef(-playerViewY)} + {@code glRotatef(playerViewX)} は
 * カメラ姿勢そのものなので {@code this.entityRenderDispatcher.cameraOrientation()} に置換。</p>
 *
 * <p>乱数（{@link Random}）は 1.7.10 と同じく描画のたびに生成し、同じ順序で消費する。</p>
 *
 * <p>TODO K3-review: テクスチャ未コピー。1.7.10 {@code textures/MiracleCircle.png} →
 * {@code textures/entity/shot/miraclecircle.png} を想定し、
 * {@link DanmakuRenderTypes#texture(String)} の規則で解決している。</p>
 */
public class RenderAjaRedStoneEffect extends EntityRenderer<EntityAjaRedStoneEffect>
{
	private static final ResourceLocation TEXTURE = DanmakuRenderTypes.texture("MiracleCircle");

	private final Random random = new Random();

	public RenderAjaRedStoneEffect(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityAjaRedStoneEffect entity)
	{
		return TEXTURE;
	}

	@Override
	public void render(EntityAjaRedStoneEffect ajaRedStoneEffect, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

		VertexConsumer buffer = bufferSource.getBuffer(
				DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR, false, true));

		int lightNum = ajaRedStoneEffect.getLightPower();
		lightNum = lightNum * lightNum;
		float widthX = 0.01F;
		float lengthY = (float)lightNum / 120.0F;
		float lengthY2 = lengthY;
		double zPos;

		float iniAngle = (float)random.nextInt(36000) / 100F;
		float angleSpan = 360F / (float)lightNum;

		float umin = 0.5F;
		float umax = 38F / 64F;
		float vmin = 0.0F;
		float vmax = 1.0F;

		poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(iniAngle / 180F * 3.141593F) / 3.141593F * 180F));
		for (int i = 0; i < lightNum; i++)
		{
			lengthY2 = lengthY * (0.2F + (float)random.nextInt(8000) / 8000.0F);
			zPos = (double)random.nextInt(10000) / 10000.0D;
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(angleSpan) / 3.141593F * 180F));

			float r = 1.0F * (float)random.nextInt(10000) / 10000.0F;
			quad(buffer, poseStack, r, 0.0F, 0.0F, 0.7F,
					-widthX, 0.0F,   0.0F,  umin, vmax,
					 widthX, 0.0F,   0.0F,  umax, vmax,
					 0.0F,   lengthY2, (float)zPos, umax, vmin,
					 0.0F,   lengthY2, (float)zPos, umin, vmin);
		}

		poseStack.popPose();
	}

	private static void quad(VertexConsumer buffer, PoseStack poseStack, float r, float g, float b, float a,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3)
	{
		int color = FastColor.ARGB32.color(
				Mth.clamp((int)(a * 255F), 0, 255),
				Mth.clamp((int)(r * 255F), 0, 255),
				Mth.clamp((int)(g * 255F), 0, 255),
				Mth.clamp((int)(b * 255F), 0, 255));
		buffer.addVertex(poseStack.last().pose(), x0, y0, z0).setColor(color).setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x1, y1, z1).setColor(color).setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x2, y2, z2).setColor(color).setUv(u2, v2).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x3, y3, z3).setColor(color).setUv(u3, v3).setLight(LightTexture.FULL_BRIGHT);
	}
}