package thkaguyamod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import thkaguyamod.client.model.ModelPrivateSquare;
import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.config.ModConfig;
import thkaguyamod.entity.item.EntitySakuyaStopWatch;

/**
 * ストップウォッチ（{@link EntitySakuyaStopWatch}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderSakuyaStopWatch} の移植。
 * 1.7.10 では {@code RenderPrivateSquare} とほぼ同一実装（暗化 + モデル）だった。
 *
 * <p>1.7.10 は {@code glBlendFunc(GL_ONE_MINUS_DST_COLOR, GL_ZERO)} + {@code glDepthMask(false)} で
 * 暗化クワッドを描いていた。1.21.1 に該当ブレンドが無いため、深度を書かない
 * {@link DanmakuRenderTypes#MODE_ADD_SRC_ALPHA} で近似する（{@code RenderPrivateSquare} と同じ判断）。</p>
 *
 * <p>TODO K3-review: モデルの貼り先テクスチャは 1.7.10 でも {@code SakuyaWatchTexture.png}
 * （ストップウォッチ用の {@code SakuyaStopWatchTexture.png} は {@code getEntityTexture} の
 * 戻り値としてだけ定義され、描画には未使用）。移植先では両方をコピーする想定で、
 * ここでは 1.7.10 の描画と同じ {@code SakuyaWatchTexture.png} を使う。</p>
 *
 * <p>TODO K3-review: テクスチャ未コピー。{@code textures/SakuyaWatchTexture.png} →
 * {@code textures/entity/shot/sakuyawatchtexture.png}、{@code textures/DarkTexture.png} →
 * {@code textures/entity/shot/darktexture.png} を想定し、{@link DanmakuRenderTypes#texture(String)}
 * の規則で解決している。</p>
 */
public class RenderSakuyaStopWatch extends EntityRenderer<EntitySakuyaStopWatch>
{
	private static final ResourceLocation TEXTURE = DanmakuRenderTypes.texture("SakuyaWatchTexture");
	private static final ResourceLocation DARK_TEXTURE = DanmakuRenderTypes.texture("DarkTexture");

	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "private_square"), "main");

	private final ModelPrivateSquare model;

	public RenderSakuyaStopWatch(EntityRendererProvider.Context context)
	{
		super(context);
		this.shadowRadius = 0.5F;
		this.model = new ModelPrivateSquare(context.bakeLayer(LAYER_LOCATION));
	}

	@Override
	public ResourceLocation getTextureLocation(EntitySakuyaStopWatch entity)
	{
		// 1.7.10 の RenderSakuyaStopWatch.getEntityTexture は SakuyaStopWatchTexture.png を返していた
		// （ただし描画は毎回 SakuyaWatchTexture.png を bind していた）
		return TEXTURE;
	}

	@Override
	public void render(EntitySakuyaStopWatch privateSquare, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();
		poseStack.scale(0.3F, 0.3F, 0.3F);

		double size = privateSquare.tickCount * 12;
		if (size > 240.0D)
		{
			size = 240.0D;
		}
		if (ModConfig.useTimeStopEffect)
		{
			VertexConsumer darkBuffer = bufferSource.getBuffer(
					DanmakuRenderTypes.get(DARK_TEXTURE, DanmakuRenderTypes.MODE_ADD_SRC_ALPHA, false, true));
			renderDark(darkBuffer, poseStack, size, (float)size, 0.0D, 1.0F, 0);
		}

		poseStack.mulPose(Axis.YP.rotationDegrees(180F - entityYaw + privateSquare.tickCount * 7F));
		VertexConsumer modelBuffer = bufferSource.getBuffer(this.model.renderType(TEXTURE));
		this.model.renderToBuffer(poseStack, modelBuffer, packedLight, OverlayTexture.NO_OVERLAY);

		poseStack.popPose();
	}

	/** 暗闇の描画をする（1.7.10 の {@code renderDark}。{@code RenderPrivateSquare} と同一）。 */
	protected void renderDark(VertexConsumer buffer, PoseStack poseStack, double length, float width, double zPos, float alpha, int time)
	{
		float maxWidth = (float)width / 2.0F;

		int zAngleDivNum = 18;
		double angleZ = 0F;
		double angleSpanZ = Math.PI * 2.0D / (double)zAngleDivNum;

		int zDivNum = 9;
		zPos = Math.sin(-Math.PI / 2.0D) * maxWidth;
		double zPosOld = zPos;

		float xPos = 0F;
		float yPos = 0F;
		float xPos2 = 0F;
		float yPos2 = 0F;
		float xPosOld = xPos;
		float yPosOld = yPos;
		float xPos2Old = xPos2;
		float yPos2Old = yPos2;
		float angle = -(float)Math.PI / 2.0F;
		float angleSpan = (float)Math.PI / (float)(zDivNum);
		angle += angleSpan;
		float widthOld = 0.0F;

		for (int j = 0; j < zDivNum; j++)
		{
			zPos = Math.sin(angle) * maxWidth;
			width = (float)Math.cos(angle) * maxWidth;

			xPos = width;
			yPos = 0F;
			angleZ = 0F;
			xPosOld = (float)Math.cos(angleZ) * width;
			yPosOld = (float)Math.sin(angleZ) * width;
			xPos2Old = (float)Math.cos(angleZ) * widthOld;
			yPos2Old = (float)Math.sin(angleZ) * widthOld;
			angleZ = angleSpanZ;

			for (int i = 1; i <= zAngleDivNum; i++)
			{
				xPos = (float)Math.cos(angleZ) * width;
				yPos = (float)Math.sin(angleZ) * width;
				xPos2 = (float)Math.cos(angleZ) * widthOld;
				yPos2 = (float)Math.sin(angleZ) * widthOld;

				// TODO K3-review: 1.7.10 の colorVar は計算されるだけで未使用（そのまま残す）
				double colorVar = 0.0D;
				if (time != 0)
				{
					colorVar = (time + j) / 10.0D;
				}

				quad(buffer, poseStack, alpha,
						xPos,     yPos,     (float)zPos,    1.0F, 0.0F,
						xPosOld,  yPosOld,  (float)zPos,    0.0F, 0.0F,
						xPos2Old, yPos2Old, (float)zPosOld, 0.0F, 1.0F,
						xPos2,    yPos2,    (float)zPosOld, 1.0F, 1.0F);

				xPosOld = xPos;
				yPosOld = yPos;
				xPos2Old = xPos2;
				yPos2Old = yPos2;
				angleZ += angleSpanZ;
			}

			zPosOld = zPos;
			angle += angleSpan;
			widthOld = width;
		}
	}

	private static void quad(VertexConsumer buffer, PoseStack poseStack, float alpha,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3)
	{
		int a = Mth.clamp((int)(alpha * 255F), 0, 255);
		buffer.addVertex(poseStack.last().pose(), x0, y0, z0).setColor(255, 255, 255, a).setUv(u0, v0);
		buffer.addVertex(poseStack.last().pose(), x1, y1, z1).setColor(255, 255, 255, a).setUv(u1, v1);
		buffer.addVertex(poseStack.last().pose(), x2, y2, z2).setColor(255, 255, 255, a).setUv(u2, v2);
		buffer.addVertex(poseStack.last().pose(), x3, y3, z3).setColor(255, 255, 255, a).setUv(u3, v3);
	}
}