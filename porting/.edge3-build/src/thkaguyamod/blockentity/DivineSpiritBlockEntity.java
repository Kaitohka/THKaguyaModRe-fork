package thkaguyamod.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import thkaguyamod.init.ModBlockEntities;

/**
 * The 1.7.10 TileEntityDivineSpirit was an empty shell (its timer/self-destruct logic was
 * commented out). Kept minimal for now; the planned "self-destruct after 100 ticks" can be
 * reintroduced with a ticker if desired.
 */
public class DivineSpiritBlockEntity extends BlockEntity {

    public DivineSpiritBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIVINE_SPIRIT.get(), pos, state);
    }
}
