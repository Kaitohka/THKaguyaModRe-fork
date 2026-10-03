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
import thkaguyamod.entity.item.EntityPendulum;

/**
 * ナズーリンペンデュラム（{@link EntityPendulum}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderPendulum} の移植。
 *
 * <p>1.7.10 は {@code glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)} で描いていたので
 * {@link DanmakuRenderTypes#MODE_ADD_ONE_MINUS_SRC_COLOR} を使う。
 * 途中の {@code glDisable(GL_CULL_FACE)}（針の部分）も両モードとも NO_CULL で再現している。</p>
 *
 * <p>TODO K3-review: テクスチャ未コピー。1.7.10 {@code textures/PendulumTexture.png} →
 * {@code textures/entity/shot/pendulumtexture.png} を想定し、
 * {@link DanmakuRenderTypes#texture(String)} の規則で解決している。</p>
 */
public class RenderPendulum extends EntityRenderer<EntityPendulum>
{
	private static final ResourceLocation TEXTURE = DanmakuRenderTypes.texture("PendulumTexture");

	public RenderPendulum(EntityRendererProvider.Context context)
	{
		super(context);
		this.shadowRadius = 0.5F;
	}

	@Override
	public ResourceLocation getTextureLocation(EntityPendulum entity)
	{
		return TEXTURE;
	}

	@Override
	public void render(EntityPendulum pendulum, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();

		float rotationLength = (float)pendulum.getSearchCount() / 48.0F;
		float xDrawPos = -Mth.sin(pendulum.getSearchAngle() / 180F * 3.141593F) * rotationLength;
		float zDrawPos = Mth.cos(pendulum.getSearchAngle() / 180F * 3.141593F) * rotationLength;
		// 1.7.10: GL11.glTranslatef(x + xDrawPos, y, z + zDrawPos)（x,z は Dispatcher が適用済み）
		poseStack.translate(xDrawPos, 0.0F, zDrawPos);

		poseStack.mulPose(Axis.YP.rotationDegrees(180F - entityYaw));

		int i, j;

		float u_min = 16F / 64F;
		float v_min = 20F / 32F;
		float u_max = 28F / 64F;
		float v_max = 32F / 32F;

		float top_p1_u = 12F / 64F;
		float top_p1_v = 4F / 32F;
		float top_p2_u = 1F / 64F;
		float top_p2_v = 19F / 32F;
		float top_p3_u = 22F / 64F;
		float top_p3_v = 19F / 32F;

		float down_p1_u = 33F / 64F;
		float down_p1_v = 0F / 32F;
		float down_p2_u = 55F / 64F;
		float down_p2_v = 0F / 32F;
		float down_p3_u = 44F / 64F;
		float down_p3_v = 31F / 32F;

		VertexConsumer buffer = bufferSource.getBuffer(
				DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR));

		float rate = 0.3F;
		poseStack.scale(rate, rate, rate);

		for (i = 0; i < 4; i++)
		{
			poseStack.mulPose(Axis.YP.rotationDegrees(90F));
			quad(buffer, poseStack,
					0.0F, 1.75F, 0.0F, top_p1_u, top_p1_v,
					0.0F, 1.75F, 0.0F, top_p1_u, top_p1_v,
					0.3F, 1.25F, -0.3F, top_p3_u, top_p3_v,
					-0.3F, 1.25F, -0.3F, top_p2_u, top_p2_v);
			quad(buffer, poseStack,
					-0.3F, 1.25F, -0.3F, down_p1_u, down_p1_v,
					0.3F, 1.25F, -0.3F, down_p2_u, down_p2_v,
					0.0F, 0.25F, 0.0F, down_p3_u, down_p3_v,
					0.0F, 0.25F, 0.0F, down_p3_u, down_p3_v);
		}

		// 1.7.10: glDisable(GL_CULL_FACE)（針の部分）
		for (i = 0; i < 4; i++)
		{
			poseStack.mulPose(Axis.YP.rotationDegrees(45F));
			quad(buffer, poseStack,
					-0.2F, 1.90F, 0.0F, u_min, v_max,
					0.2F, 1.90F, 0.0F, u_max, v_max,
					0.2F, 1.60F, 0.0F, u_max, v_min,
					-0.2F, 1.60F, 0.0F, u_min, v_min);
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