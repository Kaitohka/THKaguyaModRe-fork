package thkaguyamod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModItems;
import thkaguyamod.init.ModMenus;

/**
 * 打ち出の小槌（万宝槌）菜单 — 1.7.10 {@code ContainerMiracleMallet} 的 1.21.1 移植。
 *
 * <p>与 1.7.10 的对应关系：
 * <ul>
 *   <li>{@code InventoryCrafting(1,1)} → {@link SimpleContainer}(1)（素材槽）；
 *       {@code InventoryCraftResult} → {@link ResultContainer}。</li>
 *   <li>{@code onCraftMatrixChanged(IInventory)} → {@link #slotsChanged(Container)} /
 *       {@link #updateResult()}。</li>
 *   <li>NBT {@code "OriginalDamage"} → {@link ModDataComponents#ORIGINAL_DAMAGE} 组件（Integer）。</li>
 *   <li>{@code onContainerClosed} → {@link #removed(Player)}；{@code transferStackInSlot} →
 *       {@link #quickMoveStack}；{@code slotClick} → {@link #clicked}（保留「点击后、玩家 ≥30 级才刷新配方」语义）。</li>
 * </ul>
 *
 * <p>槽位索引表（与 1.7.10 完全一致）：
 * <pre>
 *   0      = 结果槽          (228, 35)
 *   1      = craftMaterial   (8, 17)
 *   2..28  = 玩家主背包      (48+col*18, 84+row*18)
 *   29..37 = 玩家快捷栏      (48+col*18, 142)
 * </pre>
 */
public class MiracleMalletMenu extends AbstractContainerMenu {

    /** 1.7.10 {@code ContainerMiracleMallet.slotClick} 中的经验门槛：≥30 级才刷新配方。 */
    private static final int REFRESH_EXPERIENCE_LEVEL = 30;

    public final Container craftMaterial = new SimpleContainer(1);
    public final Container craftResult = new ResultContainer();

    /** 最近一次转换结果（等价 1.7.10 的 {@code resultItem} 字段）。 */
    private ItemStack resultItem = ItemStack.EMPTY;

    /** 网络打开用构造器（MenuType 工厂引用此签名）。 */
    public MiracleMalletMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.MIRACLE_MALLET.get(), containerId);
        Player player = playerInventory.player;

        this.addSlot(new SlotMiracleMalletResult(player, this.craftMaterial, this.craftResult, 0, 228, 35));
        this.addSlot(new Slot(this.craftMaterial, 0, 8, 17));

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 48 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 48 + col * 18, 142));
        }

        // 1.7.10: this.onCraftMatrixChanged(this.craftMatrix)（该行在旧代码中被注释掉，为对齐行为同样不预计算）
        // this.updateResult();
    }

    @Override
    public boolean stillValid(Player player) {
        // 1.7.10: couldInteractWith 恒 true
        return true;
    }

    /**
     * 1.7.10 {@code onCraftMatrixChanged(IInventory)} 的现代对应。
     * 素材槽是 {@link SimpleContainer}（不回调），实际刷新由 {@link #clicked} 驱动 —— 与旧版一致。
     */
    @Override
    public void slotsChanged(Container container) {
        this.updateResult();
    }

    /**
     * 重新计算转换结果（忠实移植 1.7.10 {@code onCraftMatrixChanged} 的硬编码转换表）。
     *
     * <p>旧版 4 条转换中的第 4 条（外部 MOD「アリスの人形」物品
     * {@code item.alicedoll.bare} → 呪いのデコイ人形）已删除：该 MOD 无 1.21.1 版本，
     * 按项目台账 P8 决定移除（详见方法内 {@code TODO K3-review} 注释）。
     */
    public void updateResult() {
        ItemStack originItem = this.craftMaterial.getItem(0);
        ItemStack produced = ItemStack.EMPTY;

        // 1.7.10: int originID = -1 —— 用于记录外部 MOD 物品的数字 ID（见下方注释）
        int originId = -1;
        boolean isCommonDamage = false; // 変化前、変化後でダメージ値を共有するアイテムかどうか

        if (!originItem.isEmpty()) {
            if (originItem.is(ModItems.YIN_YANG_ORB.get())) {
                // 血に餓えた陰陽玉に変化させる
                produced = new ItemStack(ModItems.BLOODTHIRSTY_YIN_YANG_ORB.get());
            } else if (originItem.is(ModItems.GAP.get())) {
                // 隙間の折りたたみ傘に変化させる
                produced = new ItemStack(ModItems.GAP_FOLDING_UMBRELLA.get());
            }
            // TODO K3-review: 1.7.10 第 3 条转换（外部 MOD 素材）
            //   else if (originItem.getUnlocalizedName().equals("item.alicedoll.bare"))
            //       产出 cursed_decoy_doll，并记录 originID = 该外部物品的数字 ID
            //   → 该外部 MOD 无 1.21.1 版本，整条删除（台账 P8）。cursed_decoy_doll 物品本身仍在游戏中，
            //     但已无「素材 → 呪いのデコイ人形」的转换入口。
            else if (originItem.is(ModItems.ROUKANKEN.get())) {
                // 楼観旋風刃に変化させる
                // TODO K3-review: 1.7.10 产出物为 THKaguyaItems.roukanSenpuuzin，
                //  但该物品在旧版 CommonProxy 中的注册被注释（从未注册、也无 lang 键），
                //  1.21.1 主树亦无 ROUKAN_SENPUUZIN；暂用同为刀剑的 ROUKANKEN 占位（台账 P8），
                //  待主控裁决「补注册 roukan_senpuuzin」还是「本条转换仅做耐久继承」。
                produced = new ItemStack(ModItems.ROUKANKEN.get());
                produced.setDamageValue(originItem.getDamageValue());
                isCommonDamage = true;
            }

            if (!produced.isEmpty() && !isCommonDamage) {
                // 1.7.10: nbt.setInteger("OriginalDamage", originItem.getItemDamage())
                // 耐久用尽的产物会据此还原成素材（ItemBloodthirstyOnmyoudama / ItemGapFoldingUmbrella /
                // ItemCursedDecoyDoll 的「破坏时还原」逻辑）。
                produced.set(ModDataComponents.ORIGINAL_DAMAGE.get(), originItem.getDamageValue());

                // TODO K3-review: 1.7.10 还写入 NBT "OriginalItemID"（外部 MOD 物品的数字 ID），
                //  ItemCursedDecoyDoll 用 getItemById(originID) 还原成原物品。
                //  1.21.1 无数字物品 ID，该字段「不可能等价还原」。此处按简报要求保留常量映射入口：
                //  当前无任何处于启用状态的转换需要它（唯一使用者是需要记录 originID 的外部 MOD 配方，
                //  该配方已随 MOD 缺失删除），故先不写入组件。若主控决定恢复「诅咒替身人偶 → 原物品」，
                //  需新增一个 DataComponent 记录 Item 引用（Holder<Item>）或直接改用
                //  ItemStack 的 crafting-remainder 语义，并在此处写入。
                if (originId >= 0) {
                    // 1.7.10: nbt.setInteger("OriginalItemID", originID)
                }
            }
        }

        // 制作結果のスロットに新しいアイテムを置く
        // FIX-3b: ResultContainer.setItem はスタック参照をそのまま保持するため、
        // 結果スロットと resultItem が同一インスタンスを共有してしまう。DanmakuCraftingMenu:269
        // と同じく copy() を渡して切り離す（取り出しで resultItem が空スタックを指す事故を防ぐ）。
        this.resultItem = produced;
        this.craftResult.setItem(0, produced.copy());
    }

    /** 最近一次转换结果（1.7.10 {@code resultItem} 字段的对应物）。 */
    public ItemStack getResultItem() {
        return this.resultItem;
    }

    /**
     * 1.7.10 {@code slotClick}：先走原生点击，再在玩家等级 ≥30 时刷新配方。
     * 现代 {@code clicked} 返回 void；刷新后的结果写入结果槽。
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        super.clicked(slotId, button, clickType, player);
        if (player.experienceLevel >= REFRESH_EXPERIENCE_LEVEL) {
            this.updateResult();
        }
    }

    /**
     * 1.7.10 {@code func_94530_a}：搭配「拾取全部（双击）」时不把结果槽计入。
     *
     * <p>TODO K3-review: 现代对应方法是 {@code canTakeItemForPickAll(ItemStack, Slot)}
     * （签名不同：参数为将被拾取的堆与目标槽）。此处按语义映射为「结果槽不参与整堆拾取」，
     * 若主控认为应完全对齐原版（vanilla 为恒 false），可改为直接 return false。
     */
    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.craftResult && super.canTakeItemForPickAll(stack, slot);
    }

    /**
     * 关闭界面：退还素材槽内容（1.7.10 {@code onContainerClosed}）。
     * 1.7.10 只退还素材槽，不退还结果槽（结果槽内容由 {@link #updateResult()} 重建）。
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) {
            return;
        }
        this.clearContainer(player, this.craftMaterial);
    }

    /**
     * 1.7.10 {@code transferStackInSlot} → {@code quickMoveStack}。
     * 槽段：0=结果；1=素材；2..28=主背包；29..37=快捷栏。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index == 0) {
                if (!this.moveItemStackTo(slotStack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemstack);
            } else if (index >= 2 && index < 29) {
                if (!this.moveItemStackTo(slotStack, 29, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 29 && index < 38) {
                if (!this.moveItemStackTo(slotStack, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 2, 38, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }
        return itemstack;
    }
}