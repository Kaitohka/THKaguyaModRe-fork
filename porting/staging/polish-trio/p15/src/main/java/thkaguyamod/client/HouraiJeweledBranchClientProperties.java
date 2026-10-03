package thkaguyamod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModItems;

/**
 * 蓬莱の玉の枝 (Jeweled Branch of Hourai) 6 段チャージアイコン。
 *
 * <p>1.7.10 は {@code ItemHouraiJeweledBranch#getIconFromDamage(int)} が
 * 「Charge（＝消費量。0 が満タン）」をしきい値で 6 分割し、
 * {@code houraiEda}（0）／{@code houraiEda_4}（1..50）／{@code _3}（51..100）／
 * {@code _2}（101..150）／{@code _1}（151..200）／{@code _0}（201..301）を返していた。
 * 1.21.1 では同じ 6 段階を、モデル JSON の overrides 用 predicate として供給する。
 *
 * <p>返す値は「段階番号」0..5（0 = 満タン, 5 = 空）。{@link ClampedItemPropertyFunction} は
 * 戻り値を 0..1 に clamp するので、モデル JSON 側のしきい値は
 * 0 / 0.2 / 0.4 / 0.6 / 0.8 / 1.0（＝段階の下限）を使い、上から順に並べる。
 * predicate は「値 &gt;= しきい値」判定（{@code ItemOverrides.BakedOverride#test}）。
 */
public final class HouraiJeweledBranchClientProperties {

    /** モデル JSON の predicate キー（{@code thkaguyamod:hourai_charge_stage}）。 */
    public static final ResourceLocation HOURAI_CHARGE_STAGE =
            ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, "hourai_charge_stage");

    /**
     * 1.7.10 {@code getIconFromDamage} のしきい値（アイコン添字 → 消費量の上限）。
     * 添字の意味は {@code ItemHouraiJeweledBranch#houraiJeweledBranchIconName}
     * ＝ { houraiEda, _4, _3, _2, _1, _0 }。
     */
    private static final int[] CHARGE_UPPER = { 0, 50, 100, 150, 200 };

    private HouraiJeweledBranchClientProperties() {}

    /**
     * {@code FMLClientSetupEvent}（mod イベントバス、クライアント専用）から呼ぶこと。
     * 接続は {@code THKaguyaClient} に
     * {@code modEventBus.addListener(FMLClientSetupEvent.class, HouraiJeweledBranchClientProperties::register);}
     * を 1 行追加するだけ（1.7.10 の {@code registerIcons} と同じ「セットアップ時に 1 度だけ」の位置づけ）。
     */
    public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.JEWELED_BRANCH_OF_HOURAI.get(),
                HOURAI_CHARGE_STAGE,
                (ClampedItemPropertyFunction) HouraiJeweledBranchClientProperties::stage));
    }

    /** 段階番号 0..5（0 = 満タン, 5 = 空）。Charge は「消費量」なので 0 が最良の状態。 */
    private static float stage(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
        int charge = stack.getOrDefault(ModDataComponents.CHARGE.get(), 0);
        for (int stage = 0; stage < CHARGE_UPPER.length; stage++) {
            if (charge <= CHARGE_UPPER[stage]) {
                return stage / 5.0F;
            }
        }
        return 1.0F;
    }

    // TODO K3-review: 1.7.10 は getIcon(stack, renderPass, player, usingItem, useRemaining) でも
    //   同じアイコンを返していた（＝使用中の pull 演出は無し）。1.21.1 では「使用中も同じモデル」を
    //   既定としたいが、BowItem の pull 挙動を外しているので追加 predicate は不要と判断した。
    //   使用中に別モデルを出したくなったら本体モデルに pull 用 override を足すこと。
    // TODO K3-review: ItemProperties.register / ItemPropertyFunction は 1.21.1 で @Deprecated
    //   （本バージョンに代替 API は無い。1.21.2+ では ItemModel/ItemTintSource 系へ移行が必要）。
    // TODO K3-review: 本体モデル parent を minecraft:item/bow にして 1.7.10 の弓型持握を復元するかは
    //   K3 判断（PREP.md A6 最終行）。本バッチは「アイコンのみ」の範囲に留める。
}