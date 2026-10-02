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

import thkaguyamod.client.model.ModelKinkakuzi;
import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.item.EntityKinkakuzi;

/**
 * 金閣寺の一枚天井（{@link EntityKinkakuzi}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.RenderKinkakuzi} の移植。
 *
 * <p>1.7.10 の {@code doRender(entity, x, y, z, yaw, pitch)} の第 6 引数 {@code pitch} は
 * 実体は {@code partialTicks}（RenderManager が partialTicks を渡していた）なので、
 * 1.21.1 では {@code partialTick} を使う。</p>
 *
 * <p>TODO K3-review: テクスチャ未コピー。1.7.10 {@code textures/Kinkakuzi.png} →
 * {@code textures/entity/shot/kinkakuzi.png} を想定。</p>
 */
public class RenderKinkakuzi extends EntityRenderer<EntityKinkakuzi>
{
	private static final ResourceLocation TEXTURE = DanmakuRenderTypes.texture("Kinkakuzi");

	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "kinkakuzi"), "main");

	private final ModelKinkakuzi model;

	public RenderKinkakuzi(EntityRendererProvider.Context context)
	{
		super(context);
		this.shadowRadius = 3.0F;
		this.model = new ModelKinkakuzi(context.bakeLayer(LAYER_LOCATION));
	}

	@Override
	public ResourceLocation getTextureLocation(EntityKinkakuzi entity)
	{
		return TEXTURE;
	}

	@Override
	public void render(EntityKinkakuzi entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		poseStack.pushPose();
		poseStack.scale(3.0F, 0.75F, 3.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(180F - entityYaw));

		float f = (float)entity.getTimeSinceHit() - partialTick;
		float f1 = entity.getDamageTaken() - partialTick;

		if (f1 < 0.0F)
		{
			f1 = 0.0F;
		}

		if (f > 0.0F)
		{
			poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(f) * f * f1 / 10.0F * (float)entity.getForwardDirection()));
		}

		VertexConsumer buffer = bufferSource.getBuffer(this.model.renderType(TEXTURE));
		this.model.renderToBuffer(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
	}
}
