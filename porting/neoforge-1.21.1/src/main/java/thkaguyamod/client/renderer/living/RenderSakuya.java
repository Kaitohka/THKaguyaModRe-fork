package thkaguyamod.client.renderer.living;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.model.living.ModelSakuya;
import thkaguyamod.entity.living.EntitySakuya;

/**
 * 咲夜（{@link EntitySakuya}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderSakuya} の移植。
 *
 * <p>1.7.10 は {@link RenderTHBoss} を継承し、{@code doRender} の後に空の
 * {@code render} を呼ぶだけ（追加描画なし）だった。</p>
 *
 * <p>TODO K3-review: 1.7.10 の {@code EntitySakuya} は時停（{@code SakuyaStopWatch} 系）で
 * {@code EntitySpellCardCircle} を参照するが、この <i>レンダラ</i> 側には魔法陣・時停の
 * 追加描画は無かった（該当描画は {@code RenderPrivateSquare} / {@code RenderSakuyaStopWatch} /
 * {@code RenderSpellCardCircle} 側）。よってここでは何も足していない。</p>
 */
public class RenderSakuya extends RenderTHBoss<EntitySakuya, ModelSakuya>
{
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "sakuya"), "main");

	/** 1.7.10: textures/mob/SakuyaTexture.png */
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/sakuyatexture.png");

	public RenderSakuya(EntityRendererProvider.Context context)
	{
		super(context, new ModelSakuya(context.bakeLayer(LAYER_LOCATION)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntitySakuya entity)
	{
		return TEXTURE;
	}
}
