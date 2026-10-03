package thkaguyamod.client.renderer.living;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.model.living.ModelCirno;
import thkaguyamod.entity.living.EntityCirno;

/**
 * チルノ（{@link EntityCirno}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderCirno} の移植。
 *
 * <p>1.7.10 は {@link RenderTHBoss} を継承し、{@code doRender} の後に空の
 * {@code renderTHFairyCirno} を呼ぶだけ（追加描画なし）だった。1.21.1 でも
 * {@link RenderTHBoss} を継承して HP ゲージ／スペルカード名の描画をそのまま受け取る。</p>
 */
public class RenderCirno extends RenderTHBoss<EntityCirno, ModelCirno>
{
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "cirno"), "main");

	/** 1.7.10: textures/mob/CirnoTexture.png */
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/cirnotexture.png");

	public RenderCirno(EntityRendererProvider.Context context)
	{
		super(context, new ModelCirno(context.bakeLayer(LAYER_LOCATION)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityCirno cirno)
	{
		return TEXTURE;
	}
}
