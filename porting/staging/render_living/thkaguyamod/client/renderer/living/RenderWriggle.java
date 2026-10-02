package thkaguyamod.client.renderer.living;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.model.living.ModelWriggle;
import thkaguyamod.entity.living.EntityWriggle;

/**
 * リグル（{@link EntityWriggle}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderWriggle} の移植。
 *
 * <p>1.7.10 は {@link RenderTHBoss} を継承し、{@code doRender} の後に空の
 * {@code render} を呼ぶだけ（追加描画なし）だった。</p>
 *
 * <p>1.7.10 のモデル生成は {@code new ModelWriggle(1.0F, 1.0F, 64, 64)}。
 * 1.21.1 の {@link ModelWriggle} は {@code ModelTouhouDefault} 由来の
 * 寸法パラメータを {@code createBodyLayer(...)} 側で固定し、コンストラクタは
 * {@code ModelPart} だけを受け取る。よってここでは層名 {@code wriggle} で bake した結果を渡す。</p>
 *
 * <p>TODO K3-review: 1.7.10 の引数 {@code (1.0F, 1.0F, 64, 64)} は
 * 「サイズ倍率 1.0 / Y オフセット 1.0 / テクスチャ 64x64」。モデル側バッチが
 * 同じ既定値の {@code createBodyLayer()} を用意していることを統合時に確認すること。</p>
 */
public class RenderWriggle extends RenderTHBoss<EntityWriggle, ModelWriggle>
{
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "wriggle"), "main");

	/** 1.7.10: textures/mob/WriggleTexture.png */
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/wriggletexture.png");

	public RenderWriggle(EntityRendererProvider.Context context)
	{
		super(context, new ModelWriggle(context.bakeLayer(LAYER_LOCATION)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityWriggle entity)
	{
		return TEXTURE;
	}
}
