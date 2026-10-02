package thkaguyamod.client.renderer.living;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.model.living.ModelToziko;
import thkaguyamod.entity.living.EntityToziko;

/**
 * 屠自古（{@link EntityToziko}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderToziko} の移植。
 *
 * <p>1.7.10 は {@link RenderTHBoss} を継承し、{@code doRender} の後に空の
 * {@code renderToziko} を呼ぶだけ（追加描画なし）だった。</p>
 */
public class RenderToziko extends RenderTHBoss<EntityToziko, ModelToziko>
{
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "toziko"), "main");

	/** 1.7.10: textures/mob/TozikoTexture.png */
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/tozikotexture.png");

	public RenderToziko(EntityRendererProvider.Context context)
	{
		super(context, new ModelToziko(context.bakeLayer(LAYER_LOCATION)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityToziko toziko)
	{
		return TEXTURE;
	}
}
