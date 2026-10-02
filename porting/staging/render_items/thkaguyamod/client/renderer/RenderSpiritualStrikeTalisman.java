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
import thkaguyamod.config.ModConfig;
import thkaguyamod.entity.item.EntitySpiritualStrikeTalisman;

/**
 * 霊撃札（{@link EntitySpiritualStrikeTalisman}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderSpiritualStrikeTalisman} の移植。
 *
 * <p>1.7.10 は「札（両面・ブレンド無効・{@code glDisable(GL_CULL_FACE)}）」→
 * 「青い暗化（{@code glBlendFunc(GL_SRC_ALPHA, GL_ONE)}・{@code glDepthMask(false)}）」の
 * 2 パス構成だった。1.21.1 でもそのまま 2 パスで書き、
 * それぞれ {@link DanmakuRenderTypes#MODE_OPAQUE} / {@link DanmakuRenderTypes#MODE_ADD_SRC_ALPHA}
 * （深度書き込み無し）を使う。</p>
 *
 * <p>1.7.10 の {@code renderDark} は {@code alpha} 引数を無視して
 * 常に {@code setColorRGBA_F(0.2F, 0.3F, 1.0F, alpha)} を渡していた（呼び出し側は 0.4F）。</p>
 *
 * <p>TODO K3-review: テクスチャ未コピー。{@code textures/SpiritualStrikeTalismanTexture.png} →
 * {@code textures/entity/shot/spiritualstriketalismantexture.png}、
 * {@code textures/DarkTexture.png} → {@code textures/entity/shot/darktexture.png} を想定し、
 * {@link DanmakuRenderTypes#texture(String)} の規則で解決している。</p>
 */
public class RenderSpiritualStrikeTalisman extends EntityRenderer<EntitySpiritualStrikeTalisman>
{
	private static final ResourceLocation DARK_TEXTURE = DanmakuRenderTypes.texture("DarkTexture");
	private static final ResourceLocation TALISMAN_TEXTURE = DanmakuRenderTypes.texture("SpiritualStrikeTalismanTexture");

	public RenderSpiritualStrikeTalisman(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntitySpiritualStrikeTalisman entity)
	{
		return TALISMAN_TEXTURE;
	}

	@Override
	public void render(EntitySpiritualStrikeTalisman talisman, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		// --- 札本体 ---
		poseStack.pushPose();
		float rvl = 0F;
		float rul = 0.0F;
		float rvr = 0.5F;
		float rur = 1.00F;
		poseStack.mulPose(Axis.YP.rotationDegrees((float)talisman.tickCount * 21F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(30F));

		VertexConsumer talismanBuffer = bufferSource.getBuffer(DanmakuRenderTypes.get(TALISMAN_TEXTURE, DanmakuRenderTypes.MODE_OPAQUE));

		//裏面
		quad(talismanBuffer, poseStack, 1F, 1F, 1F, 1F,
				 0.2F,  0.4F, 0.0F, rvl, rul,
				-0.2F,  0.4F, 0.0F, rvr, rul,
				-0.2F, -0.4F, 0.0F, rvr, rur,
				 0.2F, -0.4F, 0.0F, rvl, rur);
		//表面
		quad(talismanBuffer, poseStack, 1F, 1F, 1F, 1F,
				-0.2F,  0.4F, 0.0F, rvl, rul,
				 0.2F,  0.4F, 0.0F, rvr, rul,
				 0.2F, -0.4F, 0.0F, rvr, rur,
				-0.2F, -0.4F, 0.0F, rvl, rur);

		poseStack.popPose();

		// --- 青い暗化（1.7.10 の renderDark） ---
		poseStack.pushPose();
		double size = talisman.tickCount * 3.0D;
		if (size > 240.0D)
		{
			size = 240.0D;
		}
		float alpha = 0.8F;
		if (talisman.tickCount > 5)
		{
			alpha -= (talisman.tickCount - 5) * 0.05F;
		}
		if (ModConfig.useTimeStopEffect)
		{
			VertexConsumer darkBuffer = bufferSource.getBuffer(
					DanmakuRenderTypes.get(DARK_TEXTURE, DanmakuRenderTypes.MODE_ADD_SRC_ALPHA, false, true));
			renderDark(darkBuffer, poseStack, size, (float)size, 0.0D, 0.4F, 0);
		}
		poseStack.popPose();
	}

	/** 暗闇の描画をする（1.7.10 の {@code renderDark}。色は青固定）。 */
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

				quad(buffer, poseStack, 0.2F, 0.3F, 1.0F, alpha,
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