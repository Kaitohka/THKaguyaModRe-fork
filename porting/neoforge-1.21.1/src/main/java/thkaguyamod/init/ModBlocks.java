package thkaguyamod.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.block.DivineSpiritBlock;

/**
 * Block registry. The 1.7.10 metadata variants are split into separate blocks:
 * the danmaku crafting table (meta 0 = shot / 1 = laser) and the eight divine
 * spirit colours (meta 0-7).
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(THKaguyaMod.MOD_ID);

    public static final DeferredBlock<Block> DANMAKU_CRAFTING_TABLE = BLOCKS.registerBlock(
            "danmaku_crafting_table", Block::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5F, 1.0F)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> 15));

    public static final DeferredBlock<Block> DANMAKU_LASER_CRAFTING_TABLE = BLOCKS.registerBlock(
            "danmaku_laser_crafting_table", Block::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5F, 1.0F)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> 15));

    public static final DeferredBlock<DivineSpiritBlock> DIVINE_SPIRIT_RED = divineSpirit("red");
    public static final DeferredBlock<DivineSpiritBlock> DIVINE_SPIRIT_BLUE = divineSpirit("blue");
    public static final DeferredBlock<DivineSpiritBlock> DIVINE_SPIRIT_GREEN = divineSpirit("green");
    public static final DeferredBlock<DivineSpiritBlock> DIVINE_SPIRIT_YELLOW = divineSpirit("yellow");
    public static final DeferredBlock<DivineSpiritBlock> DIVINE_SPIRIT_PURPLE = divineSpirit("purple");
    public static final DeferredBlock<DivineSpiritBlock> DIVINE_SPIRIT_AQUA = divineSpirit("aqua");
    public static final DeferredBlock<DivineSpiritBlock> DIVINE_SPIRIT_ORANGE = divineSpirit("orange");
    public static final DeferredBlock<DivineSpiritBlock> DIVINE_SPIRIT_WHITE = divineSpirit("white");

    private static DeferredBlock<DivineSpiritBlock> divineSpirit(String color) {
        return BLOCKS.registerBlock("divine_spirit_" + color, DivineSpiritBlock::new,
                BlockBehaviour.Properties.of()
                        .strength(0.1F, 2000.0F)
                        .sound(SoundType.SNOW)
                        .lightLevel(state -> 8)
                        .noCollission()
                        .noOcclusion());
    }

    /** All divine spirit blocks in colour order (used for the BlockEntityType). */
    public static Block[] divineSpiritBlocks() {
        return new Block[]{
                DIVINE_SPIRIT_RED.get(), DIVINE_SPIRIT_BLUE.get(), DIVINE_SPIRIT_GREEN.get(),
                DIVINE_SPIRIT_YELLOW.get(), DIVINE_SPIRIT_PURPLE.get(), DIVINE_SPIRIT_AQUA.get(),
                DIVINE_SPIRIT_ORANGE.get(), DIVINE_SPIRIT_WHITE.get()};
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        registerBlockItem(DANMAKU_CRAFTING_TABLE);
        registerBlockItem(DANMAKU_LASER_CRAFTING_TABLE);
        registerBlockItem(DIVINE_SPIRIT_RED);
        registerBlockItem(DIVINE_SPIRIT_BLUE);
        registerBlockItem(DIVINE_SPIRIT_GREEN);
        registerBlockItem(DIVINE_SPIRIT_YELLOW);
        registerBlockItem(DIVINE_SPIRIT_PURPLE);
        registerBlockItem(DIVINE_SPIRIT_AQUA);
        registerBlockItem(DIVINE_SPIRIT_ORANGE);
        registerBlockItem(DIVINE_SPIRIT_WHITE);
    }

    private static void registerBlockItem(DeferredBlock<? extends Block> block) {
        ModItems.ITEMS.register(block.getId().getPath(),
                () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private ModBlocks() {}
}
