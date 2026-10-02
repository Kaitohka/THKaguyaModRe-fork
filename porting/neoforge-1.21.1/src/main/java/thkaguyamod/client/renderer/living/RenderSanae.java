package thkaguyamod.client.renderer.living;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.model.living.ModelSanae;
import thkaguyamod.entity.living.EntitySanae;

/**
 * 早苗（{@link EntitySanae}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderSanae} の移植。
 *
 * <p>1.7.10 は {@link RenderTHBoss} を継承し、{@code doRender} の後に空の
 * {@code renderSanae} を呼ぶだけ（追加描画なし）だった。</p>
 *
 * <p>TODO K3-review: 1.7.10 の {@code ModelSanae} は {@code setRotationAngles} で
 * {@code super} を呼ばず（＝{@code ModelBiped} 既定の腕振りをしない）、
 * 早苗専用のポーズ計算だけを行う。この差はモデル側バッチの {@code setupAnim} が
 * 吸収しているはずだが、統合時に {@code ModelSanae} の実装と突き合わせること。</p>
 */
public class RenderSanae extends RenderTHBoss<EntitySanae, ModelSanae>
{
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "sanae"), "main");

	/** 1.7.10: textures/mob/SanaeTexture.png */
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/sanaetexture.png");

	public RenderSanae(EntityRendererProvider.Context context)
	{
		super(context, new ModelSanae(context.bakeLayer(LAYER_LOCATION)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntitySanae sanae)
	{
		return TEXTURE;
	}
}
