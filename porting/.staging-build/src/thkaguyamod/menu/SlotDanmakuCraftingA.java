package thkaguyamod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import thkaguyamod.init.ModItems;

/**
 * 合成台通用槽位（1.7.10 {@code SlotDanmakuCraftingA}）。按 {@code slotType} 过滤可放入物品。
 *
 * <p>type 语义（与 1.7.10 一致）：
 * <ul>
 *   <li>0 = 通用功能槽（弹数/速度/重力）；原返回 {@code power_item} meta0，现为 {@code POWER_UP_ITEM_SMALL}。</li>
 *   <li>1 = 复制槽 → {@code POINT_ITEM}</li>
 *   <li>2 = 素材槽 → 19 种弹物品之一</li>
 *   <li>3 = 颜色槽 → 16 色染料 / 火药 / 钻石</li>
 *   <li>4 = 反弹槽 → {@code POWER_UP_ITEM_BIG}（原 {@code power_item} meta1）</li>
 *   <li>5 = 激光素材槽 → 3 种激光物品之一</li>
 * </ul>
 */
public class SlotDanmakuCraftingA extends Slot {

    protected final int slotType;

    public SlotDanmakuCraftingA(Container container, int index, int x, int y, int type) {
        super(container, index, x, y);
        this.slotType = type;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        switch (this.slotType) {
            case 0:
                // 1.7.10: power_item && meta 0
                return itemStack.is(ModItems.POWER_UP_ITEM_SMALL.get());
            case 1:
                return itemStack.is(ModItems.POINT_ITEM.get());
            case 2:
                return isShotItem(itemStack);
            case 3:
                return isDye(itemStack) || itemStack.is(Items.GUNPOWDER) || itemStack.is(Items.DIAMOND);
            case 4:
                // 1.7.10: power_item && meta 1
                return itemStack.is(ModItems.POWER_UP_ITEM_BIG.get());
            case 5:
                return itemStack.is(ModItems.MIDDLE_LASER.get())
                        || itemStack.is(ModItems.SHORT_LASER.get())
                        || itemStack.is(ModItems.LONG_LASER.get());
            default:
                return false;
        }
    }

    /**
     * 1.7.10 中所有弹形共用 {@code shot_item}（meta 0-18）；1.21.1 拆为 19 个独立物品。
     * 这里枚举 19 个常量做成员判定。
     */
    public static boolean isShotItem(ItemStack stack) {
        return stack.is(ModItems.SMALL_SHOT.get())
                || stack.is(ModItems.TINY_SHOT.get())
                || stack.is(ModItems.MEDIUM_SHOT.get())
                || stack.is(ModItems.BIG_SHOT.get())
                || stack.is(ModItems.STAR_SHOT.get())
                || stack.is(ModItems.SMALL_STAR_SHOT.get())
                || stack.is(ModItems.CIRCLE_SHOT.get())
                || stack.is(ModItems.SCALE_SHOT.get())
                || stack.is(ModItems.BUTTERFLY_SHOT.get())
                || stack.is(ModItems.LIGHT_SHOT.get())
                || stack.is(ModItems.KNIFE_SHOT.get())
                || stack.is(ModItems.HEART_SHOT.get())
                || stack.is(ModItems.KUNAI_SHOT.get())
                || stack.is(ModItems.TALISMAN_SHOT.get())
                || stack.is(ModItems.BIG_LIGHT_SHOT.get())
                || stack.is(ModItems.RICE_SHOT.get())
                || stack.is(ModItems.OVAL_SHOT.get())
                || stack.is(ModItems.CRYSTAL_SHOT.get())
                || stack.is(ModItems.ARROW_SHOT.get());
    }

    /** 原版 16 色染料（1.7.10 {@code Items.dye}）。 */
    public static boolean isDye(ItemStack stack) {
        return stack.is(Items.INK_SAC)
                || stack.is(Items.RED_DYE)
                || stack.is(Items.GREEN_DYE)
                || stack.is(Items.COCOA_BEANS)
                || stack.is(Items.LAPIS_LAZULI)
                || stack.is(Items.PURPLE_DYE)
                || stack.is(Items.CYAN_DYE)
                || stack.is(Items.LIGHT_GRAY_DYE)
                || stack.is(Items.GRAY_DYE)
                || stack.is(Items.PINK_DYE)
                || stack.is(Items.LIME_DYE)
                || stack.is(Items.YELLOW_DYE)
                || stack.is(Items.LIGHT_BLUE_DYE)
                || stack.is(Items.MAGENTA_DYE)
                || stack.is(Items.ORANGE_DYE)
                || stack.is(Items.BONE_MEAL);
    }

    /** 1.7.10 {@code getSlotStackLimit()} → 64。 */
    @Override
    public int getMaxStackSize() {
        return 64;
    }
}
