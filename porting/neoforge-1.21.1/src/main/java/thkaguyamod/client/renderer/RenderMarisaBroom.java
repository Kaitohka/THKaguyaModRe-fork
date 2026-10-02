package thkaguyamod.client.renderer;

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
import thkaguyamod.entity.item.EntityMarisaBroom;

/**
 * 魔理沙の箒（{@link EntityMarisaBroom}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderMarisaBroom} の移植。
 *
 * <p>1.7.10 は {@code Tessellator.instance} でロッド（柄）とブラシを手続き的に描いていた。
 * 1.21.1 では {@link DanmakuRenderTypes#MODE_OPAQUE}（ブレンド無効・LEQUAL 深度・
 * カリング無し）のバッファへ書き換えた。1.7.10 の {@code glDepthMask} 既定（true）と
 * {@code GL_LIGHTING} 有効は、1.21.1 では「深度書き込みあり + その他光源（{@code packedLight}）」に相当する。</p>
 *
 * <p>1.7.10 の {@code doRender(..., yaw, pitch)} の第 6 引数 {@code pitch} は
 * 実体は {@code partialTicks} なので 1.21.1 の {@code partialTick} を使う。</p>
 *
 * <p>TODO K3-review: テクスチャ未コピー。1.7.10 {@code textures/MagicBloom.png} →
 * {@code textures/entity/shot/magicbloom.png} を想定し、{@link DanmakuRenderTypes#texture(String)}
 * の規則（全小文字・{@code textures/entity/shot/}）で解決している。</p>
 */
public class RenderMarisaBroom extends EntityRenderer<EntityMarisaBroom>
{
	private static final ResourceLocation TEXTURE = DanmakuRenderTypes.texture("MagicBloom");

	public RenderMarisaBroom(EntityRendererProvider.Context context)
	{
		super(context);
		this.shadowRadius = 0.5F;
	}

	@Override
	public ResourceLocation getTextureLocation(EntityMarisaBroom entity)
	{
		return TEXTURE;
	}

	@Override
	public void render(EntityMarisaBroom entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		VertexConsumer buffer = bufferSource.getBuffer(DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_OPAQUE));

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

		float f2 = (float)entity.getTimeSinceHit() - partialTick;
		float f3 = entity.getDamageTaken() - partialTick;

		if (f3 < 0.0F)
		{
			f3 = 0.0F;
		}

		if (f2 > 0.0F)
		{
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(f2) * f2 * f3 / 10.0F * (float)entity.getForwardDirection()));
		}

		float size = 0.75F;
		poseStack.scale(size, size, size);
		poseStack.scale(1.0F / size, 1.0F / size, 1.0F / size);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
		poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));

		float rodWidth = 0.05F;
		float rodWidth2 = 0.3F;
		float rodWidth3 = 0.4F;
		float rodWidth4 = 0.15F;
		double rodLength = 2.8D;
		double rodLengthHalf = rodLength / 2.0D;

		for (int i = 0; i < 4; i++)
		{
			quad(buffer, poseStack,
					 rodWidth, -rodWidth,  (float)rodLengthHalf, 0F, 2F / 32F,
					 rodWidth, -rodWidth, (float)-rodLengthHalf, 1F, 2F / 32F,
					 rodWidth,  rodWidth, (float)-rodLengthHalf, 1F, 0F,
					 rodWidth,  rodWidth,  (float)rodLengthHalf, 0F, 0F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(90F));
		}

		quad(buffer, poseStack,
				 -rodWidth, -rodWidth,  (float)rodLengthHalf, 0F,        2F / 32F,
				  rodWidth, -rodWidth,  (float)rodLengthHalf, 2F / 64F,  2F / 32F,
				  rodWidth,  rodWidth,  (float)rodLengthHalf, 2F / 64F,  4F / 32F,
				 -rodWidth,  rodWidth,  (float)rodLengthHalf, 0F,        4F / 32F);

		quad(buffer, poseStack,
				 -rodWidth4, -rodWidth4,  (float)(-rodLengthHalf + 0.3D), 0.25F, 0.5F,
				  rodWidth4, -rodWidth4,  (float)(-rodLengthHalf + 0.3D), 0.50F, 0.5F,
				  rodWidth4,  rodWidth4,  (float)(-rodLengthHalf + 0.3D), 0.50F, 1.0F,
				 -rodWidth4,  rodWidth4,  (float)(-rodLengthHalf + 0.3D), 0.25F, 1.0F);

		quad(buffer, poseStack,
				 -rodWidth3, -rodWidth3,  (float)(-rodLengthHalf - 1.3D), 0.25F, 0.5F,
				 -rodWidth3,  rodWidth3,  (float)(-rodLengthHalf - 1.3D), 0.25F, 1.0F,
				  rodWidth3,  rodWidth3,  (float)(-rodLengthHalf - 1.3D), 0.50F, 1.0F,
				  rodWidth3, -rodWidth3,  (float)(-rodLengthHalf - 1.3D), 0.50F, 0.5F);

		for (int i = 0; i < 4; i++)
		{
			quad(buffer, poseStack,
					 rodWidth4,  -rodWidth4, (float)(-rodLengthHalf + 0.3D), 0.5F,  0.5F,
					 rodWidth,   -rodWidth,  (float)(-rodLengthHalf),        0.5F,  3F / 32F,
					 rodWidth,    rodWidth,  (float)(-rodLengthHalf),        1.0F,  3F / 32F,
					 rodWidth4,   rodWidth4, (float)(-rodLengthHalf + 0.3D), 1.0F,  0.5F);

			quad(buffer, poseStack,
					 rodWidth,   -rodWidth,  (float)(-rodLengthHalf),        0.5F, 0.5F,
					 rodWidth2,  -rodWidth2, (float)(-rodLengthHalf - 0.6D), 0.5F, 3F / 32F,
					 rodWidth2,   rodWidth2, (float)(-rodLengthHalf - 0.6D), 1.0F, 3F / 32F,
					 rodWidth,    rodWidth,  (float)(-rodLengthHalf),        1.0F, 0.5F);

			quad(buffer, poseStack,
					 rodWidth2,  -rodWidth2, (float)(-rodLengthHalf - 0.6D), 0.5F, 1.0F,
					 rodWidth3,  -rodWidth3, (float)(-rodLengthHalf - 1.3D), 0.5F, 16F / 32F,
					 rodWidth3,   rodWidth3, (float)(-rodLengthHalf - 1.3D), 1.0F, 16F / 32F,
					 rodWidth2,   rodWidth2, (float)(-rodLengthHalf - 0.6D), 1.0F, 1.0F);

			poseStack.mulPose(Axis.ZP.rotationDegrees(90F));
		}

		poseStack.popPose();
	}

	/**
	 * 1 枚のクワッドを書く。1.7.10 の {@code tessellator.setNormal(0, 1, 0)} と
	 * 既定の色（白・不透明）、{@code getBrightnessForRender() == 0xf000f0} を再現する。
	 */
	private static void quad(VertexConsumer buffer, PoseStack poseStack,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3)
	{
		buffer.addVertex(poseStack.last().pose(), x0, y0, z0).setColor(255, 255, 255, 255).setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x1, y1, z1).setColor(255, 255, 255, 255).setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x2, y2, z2).setColor(255, 255, 255, 255).setUv(u2, v2).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x3, y3, z3).setColor(255, 255, 255, 255).setUv(u3, v3).setLight(LightTexture.FULL_BRIGHT);
	}
}
