package thkaguyamod.client.renderer.living;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

import thkaguyamod.client.model.living.ModelRinnosuke;
import thkaguyamod.entity.living.EntityRinnosuke;

/**
 * 森近霖之助（{@link EntityRinnosuke}）の描画。1.7.10
 * {@code thKaguyaMod.client.render.living.RenderRinnosuke} の移植。
 *
 * <p>1.7.10 は {@code RenderLiving} を継承し、{@code doRender} の後に空の
 * {@code renderRinnosuke} を呼ぶだけ（追加描画なし）だった。よって 1.21.1 では
 * {@link LivingEntityRenderer} そのままで、貼り先だけ上書きする。</p>
 *
 * <p>1.7.10 の {@code EntityRinnosuke} は {@code EntityVillager} を継承していたため、
 * 1.21.1 側でも村人系（{@code Villager}／{@code AbstractVillager} 等）の
 * {@code LivingEntity} 派生であることを前提にしている。</p>
 *
 * <p>TODO K3-review: モデルレイヤー名は設計メモの「rinnosuke」としたが、
 * モデル側バッチが別名（例 {@code touhou_default}）を採用した場合は
 * {@link #LAYER_LOCATION} を合わせて修正すること。</p>
 */
public class RenderRinnosuke extends LivingEntityRenderer<EntityRinnosuke, ModelRinnosuke>
{
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thkaguyamod", "rinnosuke"), "main");

	/** 1.7.10: textures/mob/RinnosukeTexture.png */
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/entity/living/rinnosuketexture.png");

	public RenderRinnosuke(EntityRendererProvider.Context context)
	{
		super(context, new ModelRinnosuke(context.bakeLayer(LAYER_LOCATION)), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityRinnosuke entity)
	{
		return TEXTURE;
	}
}
