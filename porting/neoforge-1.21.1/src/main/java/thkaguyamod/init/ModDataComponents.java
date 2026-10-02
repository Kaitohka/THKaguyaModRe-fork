package thkaguyamod.init;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
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

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }

    private ModDataComponents() {}
}
