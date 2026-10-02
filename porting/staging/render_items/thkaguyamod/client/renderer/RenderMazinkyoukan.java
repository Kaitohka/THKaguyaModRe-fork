package thkaguyamod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.item.EntityMazinkyoukan;

/**
 * 魔人経巻（{@link EntityMazinkyoukan}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderMazinkyoukan} の移植。
 *
 * <p>取手 2 枚はブレンド無効・不透明、経巻の本文（{@code count} 枚）は
 * 1.7.10 の {@code glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)} を
 * {@link DanmakuRenderTypes#MODE_ADD_ONE_MINUS_SRC_COLOR} で再現する。
 * 1.7.10 の {@code glDisable(GL_CULL_FACE)}（両面）は両モードとも NO_CULL で再現済み。</p>
 *
 * <p>TODO K3-review: テクスチャ未コピー。1.7.10 {@code textures/Mazinkyoukan_Texture.png} →
 * {@code textures/entity/shot/mazinkyoukan_texture.png} を想定し、
 * {@link DanmakuRenderTypes#texture(String)} の規則（全小文字）で解決している。</p>
 */
public class RenderMazinkyoukan extends EntityRenderer<EntityMazinkyoukan>
{
	private static final ResourceLocation TEXTURE = DanmakuRenderTypes.texture("Mazinkyoukan_Texture");

	public RenderMazinkyoukan(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityMazinkyoukan entity)
	{
		return TEXTURE;
	}

	@Override
	public void render(EntityMazinkyoukan mazinkyoukan, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();

		int i;
		int count = mazinkyoukan.count;
		float umin;
		float umax;
		float vmin;
		float vmax;
		float angle = mazinkyoukan.getYRot() - 57F;
		float py = -0.3F;
		float px = -Mth.sin(angle / 180F * 3.141593F) * 1.6F;
		double pz = Mth.cos(angle / 180F * 3.141593F) * 1.6D;
		float nextpx = -Mth.sin((angle + 1F) / 180F * 3.141593F) * 1.6F;
		double nextpz = Mth.cos((angle + 1F) / 180F * 3.141593F) * 1.6D;
		float widthY = 0.3F;

		// 取手　左側
		float widthY2 = widthY + 0.1F;
		float px2min = -Mth.sin((angle - 2F) / 180F * 3.141593F) * 1.55F;
		float px2max = -Mth.sin((angle + 2F) / 180F * 3.141593F) * 1.65F;
		float pz2min = Mth.cos((angle - 2F) / 180F * 3.141593F) * 1.55F;
		float pz2max = Mth.cos((angle + 2F) / 180F * 3.141593F) * 1.65F;

		VertexConsumer opaqueBuffer = bufferSource.getBuffer(DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_OPAQUE));
		quad(opaqueBuffer, poseStack, 1F, 1F, 1F, 1F,
				px2min, py - widthY2, (float)pz2min, 0.875F, 1.0F,
				px2max, py - widthY2, (float)pz2max, 1.0F,   1.0F,
				px2max, py + widthY2, (float)pz2max, 1.0F,   0.5F,
				px2min, py + widthY2, (float)pz2min, 0.875F, 0.5F);

		float color;
		float colorRev = 3.141593F * 3F / 180F;
		float ticks = (float)mazinkyoukan.tickCount;

		// 文字の書いてある部分
		VertexConsumer textBuffer = bufferSource.getBuffer(
				DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR));
		for (i = 0; i < count; i++)
		{
			color = (angle + ticks) * colorRev;
			umin = (float)(i % 63) / 64F;
			umax = ((float)((i + 1) % 63)) / 64F;
			vmin = ((float)(i / 63) * 16F) / 32F;
			vmax = vmin + 0.5F;
			quad(textBuffer, poseStack, Mth.sin(color), Mth.cos(color), -Mth.sin(color), 0.5F,
					px,     py - widthY, (float)pz,     umin, vmax,
					nextpx, py - widthY, (float)nextpz, umax, vmax,
					nextpx, py + widthY, (float)nextpz, umax, vmin,
					px,     py + widthY, (float)pz,     umin, vmin);
			angle += 1.0F;
			px = nextpx;
			pz = nextpz;
			nextpx = -Mth.sin((angle + 1F) / 180F * 3.141593F) * 1.6F;
			nextpz = Mth.cos((angle + 1F) / 180F * 3.141593F) * 1.6D;
		}

		// 取手　右側
		px2min = -Mth.sin((angle - 2F) / 180F * 3.141593F) * 1.55F;
		px2max = -Mth.sin((angle + 2F) / 180F * 3.141593F) * 1.65F;
		pz2min = Mth.cos((angle - 2F) / 180F * 3.141593F) * 1.55F;
		pz2max = Mth.cos((angle + 2F) / 180F * 3.141593F) * 1.65F;
		quad(opaqueBuffer, poseStack, 1F, 1F, 1F, 1F,
				px2min, py - widthY2, (float)pz2min, 1.0F,   1.0F,
				px2max, py - widthY2, (float)pz2max, 0.875F, 1.0F,
				px2max, py + widthY2, (float)pz2max, 0.875F, 0.5F,
				px2min, py + widthY2, (float)pz2min, 1.0F,   0.5F);

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