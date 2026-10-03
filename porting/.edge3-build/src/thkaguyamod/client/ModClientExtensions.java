package thkaguyamod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.client.model.ModelMarisaHat;
import thkaguyamod.client.model.ModelSuwakoHat;
import thkaguyamod.init.ModItems;

/**
 * 防具のカスタムモデル（魔理沙の帽子 / 諏訪子の帽子）を 1.21.1 の
 * {@link IClientItemExtensions} 経由で差し込むクラス。
 *
 * <p>1.7.10 は {@code ItemArmor#getArmorModel(EntityLivingBase, ItemStack, int)} を上書きして
 * {@code new ModelMarisaHat(0.4F, ...)} / {@code new ModelSuwakoHat(0.4F, ...)} を返していた
 * （{@code ItemMarisaHat}/{@code ItemKerobou}）。1.21.1 では同フックが
 * {@link IClientItemExtensions#getHumanoidArmorModel} に移っている。</p>
 *
 * <p>1.7.10 は毎回 {@code new} していたが、{@link net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer}
 * は描画のたびに {@code ClientHooks.copyModelProperties(original, replacement)} で姿勢・表示フラグを
 * コピーするため、モデルは 1 体を使い回せる（バニラの防具モデルも同じ扱い）。ここでは bake 結果を
 * レイヤーごとに 1 度だけ作ってキャッシュする。</p>
 *
 * <p>防具テクスチャは {@code ArmorMaterial.Layer} が
 * {@code textures/models/armor/<name>_layer_1.png} を解決する。1.7.10 の
 * {@code MarisaHatTexture.png} / {@code SuwakoHatTexture.png} は移植先で
 * {@code marisa_layer_1.png} / {@code suwako_layer_1.png} として配置済み（バイト一致）。
 * どちらも 64x32 で、モデルの UV（{@code ModelMarisaHat#createBodyLayer} /
 * {@code ModelSuwakoHat#createBodyLayer}）と一致する。</p>
 *
 * <p>レイヤー名は {@code thkaguyamod:marisa_hat} / {@code thkaguyamod:suwako_hat}（レイヤー {@code main}）。
 * レイヤー定義は {@code THKaguyaClient} の {@code RegisterLayerDefinitions} で
 * {@link ModelMarisaHat#createBodyLayer()} / {@link ModelSuwakoHat#createBodyLayer()} が登録済み。
 * 本クラス側の {@link ModelLayerLocation} は同値（equals/hashCode が
 * {@code (ResourceLocation, layer)} 比較）なので同一レイヤーを指す。</p>
 *
 * <p>接続（K3 が {@code THKaguyaClient} に追記する）: {@code RegisterClientExtensionsEvent} で
 * {@link #register(RegisterClientExtensionsEvent)} を呼ぶ。</p>
 */
public final class ModClientExtensions
{
	/** {@code THKaguyaClient.MARISA_HAT} と同値（{@code thkaguyamod:marisa_hat#main}）。 */
	public static final ModelLayerLocation MARISA_HAT =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, "marisa_hat"), "main");

	/** {@code THKaguyaClient.SUWAKO_HAT} と同値（{@code thkaguyamod:suwako_hat#main}）。 */
	public static final ModelLayerLocation SUWAKO_HAT =
			new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, "suwako_hat"), "main");

	private static final IClientItemExtensions MARISA_HAT_EXTENSIONS = new HatArmorExtensions(MARISA_HAT);
	private static final IClientItemExtensions SUWAKO_HAT_EXTENSIONS = new HatArmorExtensions(SUWAKO_HAT);

	private ModClientExtensions() {}

	/**
	 * 帽子 2 種のクライアント拡張を登録する。
	 * {@code RegisterClientExtensionsEvent}（mod イベントバス、クライアント専用）から呼ぶこと。
	 */
	public static void register(RegisterClientExtensionsEvent event)
	{
		event.registerItem(MARISA_HAT_EXTENSIONS, ModItems.MARISAS_HAT);
		event.registerItem(SUWAKO_HAT_EXTENSIONS, ModItems.SUWAKOS_HAT);
	}

	/**
	 * 頭スロットに着用されたときに bake 済みの帽子モデルを返す実装。
	 *
	 * <p>レイヤーは帽子ごとに固定なので、bake したモデルをそのままキャッシュする
	 * （{@code ItemStack} 依存の出し分けは無い）。</p>
	 */
	private static final class HatArmorExtensions implements IClientItemExtensions
	{
		private final ModelLayerLocation layer;

		private HumanoidModel<?> model;

		private HatArmorExtensions(ModelLayerLocation layer)
		{
			this.layer = layer;
		}

		@Override
		public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
				EquipmentSlot equipmentSlot, HumanoidModel<?> original)
		{
			if (equipmentSlot != EquipmentSlot.HEAD)
			{
				return original;
			}

			HumanoidModel<?> cached = this.model;
			if (cached == null)
			{
				ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(this.layer);
				// 1.7.10 の唯一の呼び出し元と同じ 0.4F の成長量は createBodyLayer() の既定値
				cached = this.layer == MARISA_HAT ? new ModelMarisaHat(root) : new ModelSuwakoHat(root);
				this.model = cached;
			}
			return cached;
		}
	}
}
