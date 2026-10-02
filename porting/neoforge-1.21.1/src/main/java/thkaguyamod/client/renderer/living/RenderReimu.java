package thkaguyamod.client.renderer.living;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.model.living.ModelReimu;
import thkaguyamod.entity.living.EntityReimu;

/**
 * 霊夢（{@link EntityReimu}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderReimu} の移植。
 *
 * <p>1.7.10 は {@link RenderTHBoss} を継承し、{@code doRender} の後に空の
 * {@code render} を呼ぶだけ（追加描画なし）だった。</p>
 */
public class RenderReimu extends RenderTHBoss<EntityReimu, ModelReimu>
{
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "reimu"), "main");

	/** 1.7.10: textures/mob/ReimuTexture.png */
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/reimutexture.png");

	public RenderReimu(EntityRendererProvider.Context context)
	{
		super(context, new ModelReimu(context.bakeLayer(LAYER_LOCATION)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityReimu entity)
	{
		return TEXTURE;
	}
}
