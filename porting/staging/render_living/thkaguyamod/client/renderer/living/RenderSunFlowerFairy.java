package thkaguyamod.client.renderer.living;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.model.living.ModelSunFlowerFairy;
import thkaguyamod.entity.living.EntityTHFairy;

/**
 * ひまわり妖精の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderSunFlowerFairy} の移植。
 *
 * <p>1.7.10 の {@code doRender} は {@code super.doRender} の後に空の {@code renderTHFairy} を
 * 呼ぶだけ（追加描画なし）、貼り先も固定で {@code FairyTexture_10.png} だった。
 * よって 1.21.1 では {@link LivingEntityRenderer} そのままで、貼り先だけ上書きする。</p>
 */
public class RenderSunFlowerFairy extends LivingEntityRenderer<EntityTHFairy, ModelSunFlowerFairy>
{
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "sunflower_fairy"), "main");

	/** 1.7.10: textures/mob/FairyTexture_10.png */
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/fairytexture_10.png");

	public RenderSunFlowerFairy(EntityRendererProvider.Context context)
	{
		super(context, new ModelSunFlowerFairy(context.bakeLayer(LAYER_LOCATION)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityTHFairy thFairy)
	{
		return TEXTURE;
	}
}
