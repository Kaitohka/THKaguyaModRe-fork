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
import thkaguyamod.entity.effect.EntityCirnoIceBox;

/**
 * チルノの氷箱（{@link EntityCirnoIceBox}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderCirnoIceBox} の移植。
 *
 * <p>1.7.10 は {@code glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)}（加算発光）で
 * 6 面の直方体を描いていた。{@link DanmakuRenderTypes#MODE_ADD_ONE_MINUS_SRC_COLOR} で再現する。
 * 面の頂点順・UV は 1.7.10 のまま。</p>
 *
 * <p>1.7.10 のテクスチャは原版 {@code textures/blocks/ice.png}（1.7.10 のドメイン無し指定）。
 * 1.21.1 の原版では {@code minecraft:textures/block/ice.png} に改名されているため、
 * そのパスを使う。</p>
 */
public class RenderCirnoIceBox extends EntityRenderer<EntityCirnoIceBox>
{
	private static final ResourceLocation ICE_BOX_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/ice.png");

	public RenderCirnoIceBox(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityCirnoIceBox entity)
	{
		return ICE_BOX_TEXTURE;
	}

	@Override
	public void render(EntityCirnoIceBox iceBox, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();

		float width = iceBox.getIceBoxWidth() / 2.0F + 0.4F;
		float height = iceBox.getIceBoxHeight() + 0.8F;

		float px[] = { -width, width, width, -width, -width, width, width, -width};
		float py[] = { height, height, height, height, 0.0F, 0.0F, 0.0F, 0.0F};
		float pz[] = { width, width, -width, -width, -width, -width, width, width};

		int p[][] = {
				{ 0, 1, 2, 3},
				{ 3, 2, 5, 4},
				{ 2, 1, 6, 5},
				{ 1, 0, 7, 6},
				{ 0, 3, 4, 7},
				{ 4, 5, 6, 7}
		};

		VertexConsumer buffer = bufferSource.getBuffer(
				DanmakuRenderTypes.get(ICE_BOX_TEXTURE, DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR));

		for (int i = 0; i < 6; i++)
		{
			quad(buffer, poseStack,
					px[p[i][0]], py[p[i][0]], pz[p[i][0]], 0.0F, 1.0F,
					px[p[i][1]], py[p[i][1]], pz[p[i][1]], 1.0F, 1.0F,
					px[p[i][2]], py[p[i][2]], pz[p[i][2]], 1.0F, 0.0F,
					px[p[i][3]], py[p[i][3]], pz[p[i][3]], 0.0F, 0.0F);
		}

		poseStack.popPose();
	}

	private static void quad(VertexConsumer buffer, PoseStack poseStack,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3)
	{
		int white = FastColor.ARGB32.color(255, 255, 255, 255);
		buffer.addVertex(poseStack.last().pose(), x0, y0, z0).setColor(white).setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x1, y1, z1).setColor(white).setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x2, y2, z2).setColor(white).setUv(u2, v2).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x3, y3, z3).setColor(white).setUv(u3, v3).setLight(LightTexture.FULL_BRIGHT);
	}
}