package thkaguyamod.client.renderer.living;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.model.living.ModelMiko;
import thkaguyamod.entity.living.EntityMiko;

/**
 * 神子（{@link EntityMiko}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderMiko} の移植。
 *
 * <p>1.7.10 は {@link RenderTHBoss} を継承し、{@code doRender} の後に空の
 * {@code render} を呼ぶだけ（追加描画なし）だった。</p>
 */
public class RenderMiko extends RenderTHBoss<EntityMiko, ModelMiko>
{
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "miko"), "main");

	/** 1.7.10: textures/mob/MikoTexture.png */
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/mikotexture.png");

	public RenderMiko(EntityRendererProvider.Context context)
	{
		super(context, new ModelMiko(context.bakeLayer(LAYER_LOCATION)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityMiko entity)
	{
		return TEXTURE;
	}
}
