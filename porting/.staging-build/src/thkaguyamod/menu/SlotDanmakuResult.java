package thkaguyamod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 弹版结果槽（1.7.10 {@code SlotDanmakuResult}）。
 *
 * <p>两端同步说明：结果槽内容由服务端 {@link DanmakuCraftingMenu#updateResult()} 填入，经
 * {@link net.minecraft.world.inventory.AbstractContainerMenu#broadcastChanges()} 下发。
 *
 * <p>取出时的消耗逻辑委托给菜单的 {@link DanmakuCraftingMenu#removeResult(Player)}；
 * 1.7.10 中的 {@code PlayerDestroyItemEvent}（矩阵/容器物品被破坏时触发）在 1.21.1 无直接对应
 * （见 {@code // TODO K3-review}）。
 */
public class SlotDanmakuResult extends Slot {

    private final DanmakuCraftingMenu menu;
    private final Player player;

    private int amountCrafted;

    public SlotDanmakuResult(Player player, DanmakuCraftingMenu menu, Container result, int index, int x, int y) {
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
        // 结果槽为“虚拟/只读”槽，渲染时不会额外偏移
        return true;
    }

    // TODO K3-review: 1.7.10 在矩阵/容器物品被“破坏”时 post PlayerDestroyItemEvent；
    //  1.21.1 对应 API 为 net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(...)，
    //  但触发时机（物品耐久耗尽/容器销毁）需结合容器物品语义确认，暂略。
}
