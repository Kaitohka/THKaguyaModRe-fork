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

import thkaguyamod.client.model.ModelSilverKnife;
import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.item.EntitySilverKnife;

/**
 * 銀のナイフ（{@link EntitySilverKnife}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderSilverKnife} の移植。
 *
 * <p>1.7.10 の 4 色テクスチャは {@code textures/shot/SilverKnife_*.png} にあり、
 * 移植先では {@code textures/entity/shot/silverknife_*.png}（全小文字）に置かれている。
 * そのため {@link DanmakuRenderTypes#texture(String)}（同じ置換規則）で解決する。</p>
 *
 * <p>1.7.10 の {@code doRender(..., yaw, pitch)} の第 6 引数 {@code pitch} は
 * 実体は {@code partialTicks} だが、本レンダラでは未使用（1.7.10 も使っていない）。</p>
 */
public class RenderSilverKnife extends EntityRenderer<EntitySilverKnife>
{
	private static final ResourceLocation resourceLocation_Knife_Blue = DanmakuRenderTypes.texture("SilverKnife_Blue");
	private static final ResourceLocation resourceLocation_Knife_Red = DanmakuRenderTypes.texture("SilverKnife_Red");
	private static final ResourceLocation resourceLocation_Knife_Green = DanmakuRenderTypes.texture("SilverKnife_Green");
	private static final ResourceLocation resourceLocation_Knife_White = DanmakuRenderTypes.texture("SilverKnife_White");

	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "silver_knife"), "main");

	private final ModelSilverKnife model;

	public RenderSilverKnife(EntityRendererProvider.Context context)
	{
		super(context);
		this.shadowRadius = 0.3F;
		this.model = new ModelSilverKnife(context.bakeLayer(LAYER_LOCATION));
	}

	@Override
	public ResourceLocation getTextureLocation(EntitySilverKnife knife)
	{
		switch (knife.getKnifeColor())
		{
			case 0:
				return resourceLocation_Knife_Blue;
			case 1:
				return resourceLocation_Knife_Red;
			case 2:
				return resourceLocation_Knife_Green;
			default:
				return resourceLocation_Knife_White;
		}
	}

	@Override
	public void render(EntitySilverKnife silverKnife, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180F - entityYaw));
		poseStack.mulPose(Axis.XP.rotationDegrees(-silverKnife.getXRot()));
		poseStack.mulPose(Axis.ZP.rotationDegrees(silverKnife.getKnifeZAngle()));
		poseStack.scale(0.5F, 0.5F, 0.5F);

		ResourceLocation texture = this.getTextureLocation(silverKnife);
		VertexConsumer buffer = bufferSource.getBuffer(this.model.renderType(texture));
		this.model.renderToBuffer(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
	}
}