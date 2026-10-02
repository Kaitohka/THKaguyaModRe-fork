package thkaguyamod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import thkaguyamod.client.model.ModelMiniHakkero;
import thkaguyamod.client.model.ModelMiniHakkero2;
import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.item.EntityMiniHakkero;

/**
 * ミニ八卦炉（{@link EntityMiniHakkero}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderMiniHakkero} の移植。
 *
 * <p>1.7.10 の {@code doRender} は {@code renderMiniHakkero}（モデル + 手続き的なリング 19 枚）と
 * {@code renderMiniHakkero2}（2 層目のモデル）を続けて呼んでいた。この 2 段構成はそのまま残す。</p>
 *
 * <p>リングは {@code glDisable(GL_LIGHTING)} + {@code glBlendFunc(GL_SRC_ALPHA, GL_ONE)} で
 * 描かれていたため、{@link DanmakuRenderTypes#MODE_ADD_SRC_ALPHA} + {@link LightTexture#FULL_BRIGHT}
 * に置き換えた。</p>
 *
 * <p>1.7.10 の {@code GL11.glRotatef(angle, ax, ay, az)} は
 * {@code poseStack.mulPose(new Quaternionf().rotationAxis(...))} で再現している。</p>
 *
 * <p>TODO K3-review: テクスチャ未コピー。1.7.10 {@code textures/MiniHakkeroTexture.png} →
 * {@code textures/entity/shot/minihakkerotexture.png} を想定し、{@link DanmakuRenderTypes#texture(String)}
 * の規則で解決している。</p>
 */
public class RenderMiniHakkero extends EntityRenderer<EntityMiniHakkero>
{
	private static final ResourceLocation TEXTURE = DanmakuRenderTypes.texture("MiniHakkeroTexture");

	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "mini_hakkero"), "main");
	public static final ModelLayerLocation LAYER_LOCATION_2 =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "mini_hakkero2"), "main");

	private final ModelMiniHakkero modelMiniHakkero;
	private final ModelMiniHakkero2 modelMiniHakkero2;

	public RenderMiniHakkero(EntityRendererProvider.Context context)
	{
		super(context);
		this.shadowRadius = 0.5F;
		this.modelMiniHakkero = new ModelMiniHakkero(context.bakeLayer(LAYER_LOCATION));
		this.modelMiniHakkero2 = new ModelMiniHakkero2(context.bakeLayer(LAYER_LOCATION_2));
	}

	@Override
	public ResourceLocation getTextureLocation(EntityMiniHakkero entity)
	{
		return TEXTURE;
	}

	@Override
	public void render(EntityMiniHakkero entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		renderMiniHakkero(entity, entityYaw, poseStack, bufferSource, packedLight);
		renderMiniHakkero2(entity, entityYaw, poseStack, bufferSource, packedLight);
	}

	public void renderMiniHakkero(EntityMiniHakkero miniHakkero, float entityYaw, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();
		poseStack.mulPose(new Quaternionf().rotationAxis(miniHakkero.getXRot() * Mth.DEG_TO_RAD,
				-Mth.sin((entityYaw - 90F) / 180F * 3.141593F), 0.0F, Mth.cos((entityYaw - 90F) / 180F * 3.141593F)));
		poseStack.mulPose(new Quaternionf().rotationY((180F - entityYaw) * Mth.DEG_TO_RAD));
		poseStack.scale(0.5F, 0.5F, 0.5F);

		VertexConsumer modelBuffer = bufferSource.getBuffer(this.modelMiniHakkero.renderType(TEXTURE));
		this.modelMiniHakkero.renderToBuffer(poseStack, modelBuffer, packedLight, OverlayTexture.NO_OVERLAY);

		float constR[] = { 255F, 255F, 255F,   0F,   0F,  76F, 128F};
		float constG[] = {   0F, 165F, 255F, 128F,   0F,   0F,   0F};
		float constB[] = {   0F,   0F,   0F,   0F, 255F, 130F, 128F};
		float u_min  = 0F;
		float v_min  = 0F;
		float u_max  = 1F / 2F;
		float v_max  = 1F / 4F;

		float angle = miniHakkero.getCircleAngle();
		float pi18 = (float)Math.PI / 9F;
		int c = miniHakkero.tickCount;
		VertexConsumer ringBuffer = bufferSource.getBuffer(DanmakuRenderTypes.get(TEXTURE, DanmakuRenderTypes.MODE_ADD_SRC_ALPHA));

		poseStack.mulPose(new Quaternionf().rotationZ(Mth.sin(angle) / 3.141593F * 180F * Mth.DEG_TO_RAD));
		for (int i = 0; i < 19; i++)
		{
			//手前リング
			poseStack.mulPose(new Quaternionf().rotationZ(Mth.sin(pi18) / 3.141593F * 180F * Mth.DEG_TO_RAD));
			float cr = constR[c % 7] / 255F * 0.5F;
			float cg = constG[c % 7] / 255F * 0.5F;
			float cb = constB[c % 7] / 255F * 0.5F;
			quad(ringBuffer, poseStack,
					 0.77F, 4.5F, -6.0F, u_min, v_min,
					-0.77F, 4.5F, -6.0F, u_max, v_min,
					-0.77F, 4.5F, -4.0F, u_max, v_max,
					 0.77F, 4.5F, -4.0F, u_min, v_max,
					cr, cg, cb, 0.6F);

			quad(ringBuffer, poseStack,
					-0.77F, 4.5F, -6.0F, u_min, v_min,
					 0.77F, 4.5F, -6.0F, u_max, v_min,
					 0.77F, 4.5F, -4.0F, u_max, v_max,
					-0.77F, 4.5F, -4.0F, u_min, v_max,
					cr, cg, cb, 0.6F);

			//奥リング
			float br = constR[(c + 3) % 7] / 255F * 0.5F;
			float bg = constG[(c + 3) % 7] / 255F * 0.5F;
			float bb = constB[(c + 3) % 7] / 255F * 0.5F;
			quad(ringBuffer, poseStack,
					 1.11F, 6.5F, -12.0F, u_min, v_min,
					-1.11F, 6.5F, -12.0F, u_max, v_min,
					-1.11F, 6.5F, -10.0F, u_max, v_max,
					 1.11F, 6.5F, -10.0F, u_min, v_max,
					br, bg, bb, 0.6F);

			quad(ringBuffer, poseStack,
					-1.11F, 6.5F, -12.0F, u_min, v_min,
					 1.11F, 6.5F, -12.0F, u_max, v_min,
					 1.11F, 6.5F, -10.0F, u_max, v_max,
					-1.11F, 6.5F, -10.0F, u_min, v_max,
					br, bg, bb, 0.6F);

			angle += pi18;
		}

		poseStack.popPose();
	}

	public void renderMiniHakkero2(EntityMiniHakkero miniHakkero, float entityYaw, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();
		// TODO K3-review: 1.7.10 は軸の 3 成分に rotationYaw と yaw の両方を使っていた
		//   (rotationPitch の軸 = rotationYaw 由来、180F - yaw の回転 = 引数 yaw)。
		//   1.21.1 では entityYaw が両方の実体なので、そのまま entityYaw を使う。
		poseStack.mulPose(new Quaternionf().rotationAxis(miniHakkero.getXRot() * Mth.DEG_TO_RAD,
				-Mth.sin((entityYaw - 90F) / 180F * 3.141593F), 0.0F, Mth.cos((entityYaw - 90F) / 180F * 3.141593F)));
		poseStack.mulPose(new Quaternionf().rotationY((180F - entityYaw) * Mth.DEG_TO_RAD));

		//ずらした部分はここの倍率を変えて補う
		poseStack.scale(0.501F, 0.501F, 0.501F);

		VertexConsumer modelBuffer = bufferSource.getBuffer(this.modelMiniHakkero2.renderType(TEXTURE));
		this.modelMiniHakkero2.renderToBuffer(poseStack, modelBuffer, packedLight, OverlayTexture.NO_OVERLAY);

		poseStack.popPose();
	}

	private static void quad(VertexConsumer buffer, PoseStack poseStack,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3,
			float r, float g, float b, float a)
	{
		int color = FastColor.ARGB32.color((int)(a * 255F), (int)(r * 255F), (int)(g * 255F), (int)(b * 255F));
		buffer.addVertex(poseStack.last().pose(), x0, y0, z0).setColor(color).setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x1, y1, z1).setColor(color).setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x2, y2, z2).setColor(color).setUv(u2, v2).setLight(LightTexture.FULL_BRIGHT);
		buffer.addVertex(poseStack.last().pose(), x3, y3, z3).setColor(color).setUv(u3, v3).setLight(LightTexture.FULL_BRIGHT);
	}
}