package thkaguyamod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

import thkaguyamod.client.model.ModelYuukaParasol;
import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.item.EntityYuukaParasol;

/**
 * 幽香の日傘（{@link EntityYuukaParasol}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderYuukaParasol} の移植。
 *
 * <p>傘の骨組みは {@code Tessellator.instance} による 18 本 × 2 クワッド。1.7.10 は
 * ライティング有効・深度書き込みあり・両面（{@code glDisable(GL_CULL_FACE)}）だったので、
 * {@link DanmakuRenderTypes#MODE_OPAQUE}（NO_CULL を含む）で描く。
 * 1.7.10 の {@code setNormal(0, 1, 0)} 相当は 1.21.1 の
 * {@code POSITION_TEX_COLOR} 形式に法線要素が無く、この RenderType のシェーダも法線を
 * 使わないため落としている。</p>
 *
 * <p>モデル（持ち手）は 1.7.10 のバインド時と同じ {@code entityCutoutNoCull} で描く
 * （ライティング有効・両面）。</p>
 *
 * <p>TODO K3-review: テクスチャ未コピー。1.7.10 {@code textures/YuukaParasolTexture.png} →
 * {@code textures/entity/shot/yuukaparasoltexture.png} を想定し、{@link DanmakuRenderTypes#texture(String)}
 * の規則で解決している。</p>
 */
public class RenderYuukaParasol extends EntityRenderer<EntityYuukaParasol>
{
	private static final ResourceLocation TEXTURE = DanmakuRenderTypes.texture("YuukaParasolTexture");

	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "yuuka_parasol"), "main");

	private final ModelYuukaParasol model;

	public RenderYuukaParasol(EntityRendererProvider.Context context)
	{
		super(context);
		this.model = new ModelYuukaParasol(context.bakeLayer(LAYER_LOCATION));
	}

	@Override
	public ResourceLocation getTextureLocation(EntityYuukaParasol entity)
	{
		return TEXTURE;
	}

	@Override
	public void render(EntityYuukaParasol yuukaParasol, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();
		// 1.7.10: GL11.glTranslatef(x, y - 2.4F, z)（x,z は EntityRenderDispatcher が適用済み）
		poseStack.translate(0.0F, -2.4F, 0.0F);

		VertexConsumer buffer = bufferSource.getBuffer(DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_OPAQUE));

		double stickSideLength = 0.07D;
		float stickHeight = 2.7F;
		float stickUMin = 0.0F;
		float stickUMax = 5F / 64F;
		float stickVMin = 0.0F;
		float stickVMax = 1.0F;

		poseStack.mulPose(Axis.YP.rotationDegrees(180F - entityYaw));
		poseStack.mulPose(Axis.XP.rotationDegrees(yuukaParasol.getXRot()));

		float parasolUMin = 0.5F;
		float parasolUMax = 0.75F;
		float parasolVMin = 0.0F;
		float parasolVMax = 0.5F;
		float parasolLength = 2.2F;
		float parasolWidth = 0.5F;
		float angleSpan = 360F / 18F;
		poseStack.scale(0.7F, 0.7F, 0.7F);

		for (int i = 0; i < 18; i++)
		{
			quad(buffer, poseStack,
					 0.0F,           stickHeight, 0.0F,  parasolUMin, parasolVMax,
					 0.0F,           stickHeight, 0.0F,  parasolUMax, parasolVMax,
					 parasolWidth,   parasolLength, -1.0F, parasolUMax, parasolVMin,
					-parasolWidth,   parasolLength, -1.0F, parasolUMin, parasolVMin);

			quad(buffer, poseStack,
					-parasolWidth,      parasolLength,       -1.0F, parasolUMin, parasolVMax,
					 parasolWidth,      parasolLength,       -1.0F, parasolUMax, parasolVMax,
					 parasolWidth * 2F, parasolLength - 0.5F, -1.7F, parasolUMax, parasolVMin,
					-parasolWidth * 2F, parasolLength - 0.5F, -1.7F, parasolUMin, parasolVMin);

			poseStack.mulPose(Axis.YP.rotationDegrees(angleSpan));
		}

		poseStack.scale(0.3F, 0.4F, 0.3F);
		VertexConsumer modelBuffer = bufferSource.getBuffer(this.model.renderType(TEXTURE));
		this.model.renderToBuffer(poseStack, modelBuffer, packedLight, OverlayTexture.NO_OVERLAY);

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