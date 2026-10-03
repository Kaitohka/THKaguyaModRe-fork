package thkaguyamod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 激光版结果槽（1.7.10 {@code SlotLaserResult}）。
 *
 * <p>与 {@link SlotDanmakuResult} 同构，但激光版没有重力/反弹槽（其结果消耗逻辑见
 * {@link DanmakuLaserCraftingMenu#removeResult(Player)}）。1.7.10 的
 * {@code PlayerDestroyItemEvent} 在 1.21.1 无直接对应（{@code // TODO K3-review}）。
 */
public class SlotLaserResult extends Slot {

    private final DanmakuLaserCraftingMenu menu;
    private final Player player;

    private int amountCrafted;

    public SlotLaserResult(Player player, DanmakuLaserCraftingMenu menu, Container result, int index, int x, int y) {
        super(result, index, x, y);
        this.menu = menu;
        this.player = player;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.amountCrafted += Math.min(amount, this.getItem().getCount());
        }
        return super.remove(amount);
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        // 1.7.10: this.onCrafting(par2ItemStack)
        this.checkTakeAchievements(stack);
        this.menu.removeResult(this.player);
        this.amountCrafted = 0;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean isFake() {
        return true;
    }

    // TODO K3-review: 同 SlotDanmakuResult，PlayerDestroyItemEvent 的 1.21.1 触发时机待确认，暂略。
}
