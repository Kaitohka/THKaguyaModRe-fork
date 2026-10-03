package thkaguyamod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 万宝槌结果槽（1.7.10 {@code SlotMiracleMalletResult}）。
 *
 * <p>与 1.7.10 的对应关系：
 * <ul>
 *   <li>{@code isItemValid} → {@link #mayPlace}（恒 false，结果槽不可放入）。</li>
 *   <li>{@code onPickupFromSlot} → {@link #onTake}：先 {@code onCrafting}（现代为
 *       {@link #checkTakeAchievements}），再对素材槽做「消耗 1 + 容器物品返还」。</li>
 *   <li>{@code getSlotStackLimit} → {@link #getMaxStackSize}（64）。</li>
 *   <li>{@code decrStackSize} → {@link #remove}（累计 amountCrafted，1.7.10 仅累计未使用，
 *       这里保持一致语义并保留字段）。</li>
 * </ul>
 *
 * <p>消耗逻辑放在槽内（而非菜单）是为了贴近 1.7.10 的
 * {@code SlotMiracleMalletResult.slotUseProcess(IInventory, int)} 结构；
 * 容器物品返还改用现代 API：{@code ItemStack#hasCraftingRemainingItem()} /
 * {@code ItemStack#getCraftingRemainingItem()}（1.7.10 为
 * {@code Item#getContainerItem}/{@code doesContainerItemLeaveCraftingGrid}）。
 */
public class SlotMiracleMalletResult extends Slot {

    private final Container craftMaterial;
    private final Player player;

    private int amountCrafted;

    public SlotMiracleMalletResult(Player player, Container material, Container result, int index, int x, int y) {
        super(result, index, x, y);
        this.player = player;
        this.craftMaterial = material;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // 結果のスロットのためアイテムを入れられない
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
        this.checkTakeAchievements(stack);
        this.slotUseProcess(this.craftMaterial, 1);
        this.amountCrafted = 0;
    }

    /**
     * 取出成品后对素材槽的消耗处理（1.7.10 {@code SlotMiracleMalletResult.slotUseProcess}）。
     *
     * <p>容器物品返还：现代等价为 {@code ItemStack#getCraftingRemainingItem()}；
     * 耐久被耗尽（damage &gt; maxDamage）时触发
     * {@link net.neoforged.neoforge.event.EventHooks#onPlayerDestroyItem}（对应 1.7.10 的
     * {@code PlayerDestroyItemEvent}），并丢弃返还物。
     */
    private void slotUseProcess(Container container, int useNum) {
        ItemStack stack = container.getItem(0);
        if (stack.isEmpty()) {
            return;
        }

        container.removeItem(0, useNum);

        if (!stack.hasCraftingRemainingItem()) {
            return;
        }

        ItemStack remainder = stack.getCraftingRemainingItem();

        if (remainder.isDamageableItem() && remainder.getDamageValue() > remainder.getMaxDamage()) {
            net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(this.player, remainder, null);
            remainder = ItemStack.EMPTY;
        }

        if (!remainder.isEmpty() && !this.player.getInventory().add(remainder)) {
            if (container.getItem(0).isEmpty()) {
                container.setItem(0, remainder);
            } else {
                // 1.7.10: this.thePlayer.dropPlayerItem(itemstack2)
                this.player.drop(remainder, false);
            }
        }
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean isFake() {
        // 结果槽为「虚拟/只读」槽，渲染时不额外偏移（对齐 SlotDanmakuResult 的处理）
        return true;
    }
}
