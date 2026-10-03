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

    // ---- D4 (悔悟の棒) ----

    /**
     * 悔悟の棒 (Rod of Remorse) の「文字が書かれている」状態。
     *
     * <p>1.7.10 は item damage (meta) 0 = 未使用 / 1 = 使用済み で表現していた
     * （{@code ItemKaigo.setMaxDamage(0)} + {@code setHasSubtypes(true)}）。
     * 1.21.1 の damage 値は「耐久値」の意味しか持たず、{@code ItemStack#setDamageValue}
     * は {@code MAX_DAMAGE} にクランプされる（登録が {@code durability(2)} のため 0/1 しか
     * 入らないが、耐久バーと {@code isDamaged()} の副作用を伴う）。ここでは 1.7.10 の
     * メタ値を意味そのままに独立コンポーネントへ移す。
     *
     * <p>不在 = false（＝未使用、効果あり）。{@code Codec.BOOL} は値 false を書かないため、
     * 未使用スタックはコンポーネントを持たずに済む。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ROD_OF_REMORSE_USED =
            DATA_COMPONENTS.register("rod_of_remorse_used", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }

    private ModDataComponents() {}
}
