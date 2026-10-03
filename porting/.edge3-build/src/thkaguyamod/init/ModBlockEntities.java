package thkaguyamod.init;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.blockentity.DivineSpiritBlockEntity;

/** BlockEntity registry. */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, THKaguyaMod.MOD_ID);

    public static final Supplier<BlockEntityType<DivineSpiritBlockEntity>> DIVINE_SPIRIT =
            BLOCK_ENTITY_TYPES.register("divine_spirit", () -> BlockEntityType.Builder
                    .of(DivineSpiritBlockEntity::new, ModBlocks.divineSpiritBlocks())
                    .build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    private ModBlockEntities() {}
}
