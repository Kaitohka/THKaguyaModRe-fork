package thkaguyamod.client;

import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.IEventBus;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModItems;

/**
 * 悔悟の棒 (Rod of Remorse) のアイテムモデル predicate 登録。
 *
 * <p>1.7.10 は {@code ItemKaigo#getIconFromDamage(int)} がメタ値 0/1 で
 * {@code kaigoStick_0}/{@code kaigoStick_1} の 2 枚を切り替えていた。
 * 1.21.1 ではモデル JSON の {@code overrides[].predicate} が参照する名前付き float を
 * クライアント側で供給する（{@link ItemProperties#register}）。
 *
 * <p>参照名は {@code thkaguyamod:remorse_used}（名前空間付き＝他 MOD と衝突しない）。
 * 値は 1.7.10 のメタ値と同じ 0.0 / 1.0。
 * モデル側 predicate は「値 &gt;= しきい値」判定なので 1 で十分。
 *
 * <p>接続（K3 が {@code THKaguyaClient} に追記する）:
 * {@code modEventBus.addListener(FMLClientSetupEvent.class, RodOfRemorseClientProperties::register);}
 * 既に {@code enqueueWork} を使う箇所があるなら {@code event.enqueueWork(...)} 経由でも等価。
 */
public final class RodOfRemorseClientProperties {

    /** モデル JSON の predicate キー（{@code thkaguyamod:remorse_used}）。 */
    public static final ResourceLocation REMORSE_USED =
            ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, "remorse_used");

    private RodOfRemorseClientProperties() {}

    /**
     * {@code FMLClientSetupEvent}（mod イベントバス、クライアント専用）から呼ぶこと。
     * アイテム登録が完了している必要があるため、mod 構築時ではなくこのイベントで登録する。
     */
    public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.ROD_OF_REMORSE.get(),
                REMORSE_USED,
                (ClampedItemPropertyFunction) (stack, level, entity, seed) -> remorseUsed(stack)));
    }

    /** 1.7.10 のメタ値相当: 1.0 = 使用済み（文字なし）, 0.0 = 未使用。 */
    private static float remorseUsed(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ROD_OF_REMORSE_USED.get(), false) ? 1.0F : 0.0F;
    }

    // TODO K3-review: ItemProperties.register / ItemPropertyFunction は 1.21.1 で @Deprecated
    //   （本バージョンに代替 API は無い。1.21.2+ では ItemModel/ItemTintSource 系へ移行が必要）。
    // TODO K3-review: ModItems.ROD_OF_REMORSE の durability(2) を外すかは K3 判断（PREP.md §6）。
    //   外す場合は本 predicate の動作は変わらない（耐久値はもう読んでいない）。
}