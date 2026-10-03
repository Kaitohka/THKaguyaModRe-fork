package thkaguyamod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import thkaguyamod.blockentity.DivineSpiritBlockEntity;

/**
 * 小神霊 block. In 1.7.10 this was a {@code BlockContainer} with eight metadata colours;
 * the colours are separate blocks now. The glowing billboard render (a 1.7.10 TESR) is
 * scheduled for the renderer phase.
 */
public class DivineSpiritBlock extends Block implements EntityBlock {

    public DivineSpiritBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DivineSpiritBlockEntity(pos, state);
    }

    /** 1.7.10: 自定义 renderId + ISBRH 空实现 ⇒ 本体不可见，仅 TESR 面片可见。 */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
