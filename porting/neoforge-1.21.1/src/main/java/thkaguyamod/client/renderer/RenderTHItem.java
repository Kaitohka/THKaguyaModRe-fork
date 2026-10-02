package thkaguyamod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.entity.EntityTHItem;

/**
 * 各種東方アイテム（{@link EntityTHItem}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderTHItem} の移植。
 *
 * <p>1.7.10 は {@code THKaguyaRenderLib.getGLRotatefByTherdPersonView(renderManager)} で
 * 常にプレイヤーへ正対させていたので、{@code this.entityRenderDispatcher.cameraOrientation()}
 * に置換。1.7.10 の {@code glScalef(2.0F, ...)} と頂点順・UV はそのまま。</p>
 *
 * <p>1.7.10 の {@code textures/items/material/&lt;X&gt;.png} は stage3 で
 * {@code textures/item/material/&lt;snake_case&gt;.png} に改名済み。</p>
 */
public class RenderTHItem extends EntityRenderer<EntityTHItem>
{
	private static final ResourceLocation smallPower =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/item/material/power_up_item_small.png");
	private static final ResourceLocation bigPower =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/item/material/power_up_item_big.png");
	private static final ResourceLocation point =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/item/material/point_item.png");
	private static final ResourceLocation bomb =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/item/material/bomb_item.png");
	private static final ResourceLocation extend =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/item/material/extend_item.png");

	public RenderTHItem(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityTHItem item)
	{
		switch (item.getItemType())
		{
			case 1:
				return smallPower;
			case 2:
				return bigPower;
			case 4:
				return point;
			case 10:
				return bomb;
			case 11:
				return extend;
			default:
				return smallPower;
		}
	}

	@Override
	public void render(EntityTHItem item, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float sizeRate = 2.0F;
		poseStack.scale(sizeRate, sizeRate, sizeRate);

		float rvl = 0.0F;
		float rul = 0.0F;
		float rvr = 1.00F;
		float rur = 1.00F;

		VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(this.getTextureLocation(item)));
		quad(buffer, poseStack, packedLight,
				 0.2F,  0.2F, 0.0F, rvr, rul,
				-0.2F,  0.2F, 0.0F, rvl, rul,
				-0.2F, -0.2F, 0.0F, rvl, rur,
				 0.2F, -0.2F, 0.0F, rvr, rur);

		poseStack.popPose();
	}

	private static void quad(VertexConsumer buffer, PoseStack poseStack, int packedLight,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3)
	{
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