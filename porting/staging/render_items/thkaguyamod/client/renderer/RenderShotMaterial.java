package thkaguyamod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.entity.EntityShotMaterial;

/**
 * 弾の素（{@link EntityShotMaterial}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderShotMaterial} の移植。
 *
 * <p>1.7.10 は {@code glRotatef(-playerViewY)} + {@code glRotatef(playerViewX)} で
 * カメラへ正対させていたので、{@code this.entityRenderDispatcher.cameraOrientation()}
 * に置換。クワッドはライティング有効・深度書き込みありの
 * {@code entityCutoutNoCull}（1.7.10 の {@code bindEntityTexture} 既定パス）で描く。</p>
 *
 * <p>TODO K3-review: 1.7.10 の {@code getEntityTexture} は
 * {@code textures/items/material/ShotMaterial.png} を指していたが、そのファイルは
 * リポジトリに無い（実体は {@code material/ShotMaterial.png} ＝ stage3 の
 * {@code item/material/shot_material.png}）。本来の弾の素テクスチャに戻すこと。</p>
 */
public class RenderShotMaterial extends EntityRenderer<EntityShotMaterial>
{
	/** 1.7.10: textures/items/material/ShotMaterial.png（存在しないため stage3 の実体で代用） */
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/item/material/shot_material.png");

	public RenderShotMaterial(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityShotMaterial shotMaterial)
	{
		return TEXTURE;
	}

	@Override
	public void render(EntityShotMaterial shotMaterial, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

		float rvl = 0.0F;
		float rul = 0.0F;
		float rvr = 1.00F;
		float rur = 1.00F;

		VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		quad(buffer, poseStack, packedLight,
				-0.2F,  0.2F, 0.0F, rvl, rul,
				 0.2F,  0.2F, 0.0F, rvr, rul,
				 0.2F, -0.2F, 0.0F, rvr, rur,
				-0.2F, -0.2F, 0.0F, rvl, rur);

		poseStack.popPose();
	}

	private static void quad(VertexConsumer buffer, PoseStack poseStack, int packedLight,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3)
	{
		// 1.7.10 の setNormal(0, 1, 0) をそのまま使う
		buffer.addVertex(poseStack.last().pose(), x0, y0, z0).setColor(255, 255, 255, 255).setUv(u0, v0)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 1.0F, 0.0F);
		buffer.addVertex(poseStack.last().pose(), x1, y1, z1).setColor(255, 255, 255, 255).setUv(u1, v1)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 1.0F, 0.0F);
		buffer.addVertex(poseStack.last().pose(), x2, y2, z2).setColor(255, 255, 255, 255).setUv(u2, v2)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 1.0F, 0.0F);
		buffer.addVertex(poseStack.last().pose(), x3, y3, z3).setColor(255, 255, 255, 255).setUv(u3, v3)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 1.0F, 0.0F);
	}
}