package thkaguyamod.client.renderer.living;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.model.living.ModelCursedDecoyDoll;
import thkaguyamod.entity.item.EntityCursedDecoyDoll;

/**
 * 呪いのデコイ人形（{@link EntityCursedDecoyDoll}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderCursedDecoyDoll} の移植。
 *
 * <p>1.7.10 は {@code RenderLiving} を継承し、{@code doRender} の後に空の {@code render} を
 * 呼ぶだけ（追加描画なし）だった。よって 1.21.1 では {@link LivingEntityRenderer} そのままで、
 * 貼り先だけ上書きする。</p>
 *
 * <p>1.7.10 の実体は {@code thKaguyaMod.entity.item.EntityCursedDecoyDoll}（{@code EntityDanmakuMob} 派生）
 * なので、1.21.1 でも {@code thkaguyamod.entity.item} 側の型を指す（アイテム側バッチの
 * {@code ItemCursedDecoyDoll} が同じ型を import している）。</p>
 */
public class RenderCursedDecoyDoll extends LivingEntityRenderer<EntityCursedDecoyDoll, ModelCursedDecoyDoll>
{
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "cursed_decoy_doll"), "main");

	/** 1.7.10: textures/mob/DecoyTexture.png */
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/decoytexture.png");

	public RenderCursedDecoyDoll(EntityRendererProvider.Context context)
	{
		super(context, new ModelCursedDecoyDoll(context.bakeLayer(LAYER_LOCATION)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityCursedDecoyDoll entity)
	{
		return TEXTURE;
	}
}
