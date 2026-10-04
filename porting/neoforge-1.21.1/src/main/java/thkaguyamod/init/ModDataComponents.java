package thkaguyamod.init;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuParams;

/** DataComponent registry (replaces 1.7.10 ItemStack NBT). */
public final class ModDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, THKaguyaMod.MOD_ID);

    /** Danmaku parameters on shot/laser items (1.7.10: NBT Number/DanmakuForm/Speed/Gravity/Special/Color/Infinity). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DanmakuParams>> DANMAKU =
            DATA_COMPONENTS.register("danmaku", () -> DataComponentType.<DanmakuParams>builder()
                    .persistent(DanmakuParams.CODEC)
                    .networkSynchronized(DanmakuParams.STREAM_CODEC)
                    .build());

    /** Miracle charge on Sanae's spell cards (1.7.10: NBT int "Charge"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE =
            DATA_COMPONENTS.register("charge", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Gap Folding Umbrella dye colour (1.7.10: NBT byte "GapColor"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GAP_COLOR =
            DATA_COMPONENTS.register("gap_color", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Original item damage for mallet-restorable items (1.7.10: NBT "OriginalDamage"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ORIGINAL_DAMAGE =
            DATA_COMPONENTS.register("original_damage", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /**
     * Original item a mallet-transformed item reverts to when its durability runs out
     * (1.7.10: NBT int {@code "OriginalItemID"}, a numeric item id).
     *
     * <p>1.21.1 has no numeric item ids, so the component carries a registry {@link Holder} of the
     * {@link Item} instead of an int. 1.7.10 looked the item up with {@code Item.getItemById(id)};
     * the equivalent here is to read the holder and build a fresh {@code ItemStack} from it
     * (see {@link thkaguyamod.item.ItemCursedDecoyDoll#use}).
     *
     * <p>Written by {@link thkaguyamod.menu.MiracleMalletMenu} when a conversion produces an item
     * that must remember its source material. Currently no enabled conversion needs it (the
     * 1.7.10 conversion that produced {@code cursed_decoy_doll} used an external mod's item and
     * was dropped — see ledger P8), so in practice the value is set from code / GameTest; the
     * consumer is {@link thkaguyamod.item.ItemCursedDecoyDoll#use}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Holder<Item>>> ORIGINAL_ITEM =
            DATA_COMPONENTS.register("original_item", () -> DataComponentType.<Holder<Item>>builder()
                    .persistent(BuiltInRegistries.ITEM.holderByNameCodec())
                    .networkSynchronized(ByteBufCodecs.holderRegistry(Registries.ITEM))
                    .build());

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }

    private ModDataComponents() {}
}
