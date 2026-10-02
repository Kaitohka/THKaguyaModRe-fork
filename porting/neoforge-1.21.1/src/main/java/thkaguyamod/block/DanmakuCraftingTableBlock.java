package thkaguyamod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import thkaguyamod.menu.DanmakuCraftingMenu;
import thkaguyamod.menu.DanmakuLaserCraftingMenu;

/**
 * 弾幕作業台. 1.7.10 used one block with metadata (0 = shot, 1 = laser) and
 * {@code player.openGui} with numeric ids; the port splits the variants into two
 * blocks and opens the menu via {@link SimpleMenuProvider}.
 */
public class DanmakuCraftingTableBlock extends Block {

    private final boolean laser;

    public DanmakuCraftingTableBlock(Properties properties, boolean laser) {
        super(properties);
        this.laser = laser;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            Component title = Component.translatable(laser
                    ? "block.thkaguyamod.danmaku_laser_crafting_table"
                    : "block.thkaguyamod.danmaku_crafting_table");
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> laser
                            ? new DanmakuLaserCraftingMenu(id, inventory, level, pos)
                            : new DanmakuCraftingMenu(id, inventory, level, pos),
                    title));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
