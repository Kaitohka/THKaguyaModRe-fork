package thkaguyamod.menu;

import javax.annotation.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import thkaguyamod.init.ModMenus;

/**
 * 守矢神社出張早苗さん 商店菜单 — 1.7.10 {@code ContainerMerchantSanae} 的 1.21.1 移植。
 *
 * <p>1.7.10 结构（仿 vanilla {@code ContainerMerchant}）：
 * {@code InventoryMerchant} 2 个输入槽 + 1 个结果槽（{@code SlotMerchantResult}），
 * 坐标 (36,53)/(62,53)/(120,53)；玩家背包 (8+col*18, 84+row*18)、快捷栏 y=142。
 * 1.21.1 vanilla 的对应物为 {@link MerchantContainer} + {@link MerchantResultSlot}，
 * 本菜单直接复用，仅把槽位坐标改回 1.7.10 的布局（vanilla {@code MerchantMenu} 是 136/162/220）。
 *
 * <p>与 1.7.10 的对应关系：
 * <ul>
 *   <li>{@code onCraftMatrixChanged} → {@link #slotsChanged(Container)}（调用
 *       {@link MerchantContainer#updateSellItem()}）。</li>
 *   <li>{@code setCurrentRecipeIndex(int)} → {@link #setSelectionHint(int)}（0 起索引，语义一致）。</li>
 *   <li>选中索引用自建包 {@code SelectSanaeTradePayload}（对应 1.7.10 的 {@code MC|TrSel}）。</li>
 *   <li>{@code canInteractWith} → {@link #stillValid}（1.7.10 恒 true，见方法注释）。</li>
 *   <li>{@code onContainerClosed} → {@link #removed(Player)}。</li>
 * </ul>
 *
 * <p>实体引用：1.7.10 的 {@code IMerchant theMerchant} 在此有两种形态 ——
 * 服务端用真实实体（早苗，假设实现 {@link Merchant}），
 * 客户端用 {@link ClientSideMerchant} 占位（1.21.1 的交易列表由
 * {@code ClientboundMerchantOffersPacket} 下发，客户端没有真实实体）。
 */
public class MerchantSanaeMenu extends AbstractContainerMenu {

    private static final int PAYMENT1_SLOT = 0;
    private static final int PAYMENT2_SLOT = 1;
    private static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;

    private final Merchant trader;
    private final MerchantContainer tradeContainer;

    /**
     * 服务端打开界面用（1.7.10 的「早苗のアイテム交換コンテナ」）。
     *
     * <p>{@code merchantEntityId} 是**实体网络 ID**（{@code Entity#getId()}），
     * 对应旧版 {@code player.displayGUIMerchant(this, ...)} 里「实体随调用一起传递」的角色：
     * 1.21.1 的 {@code MenuType} 工厂只能拿到 {@code (containerId, inventory, data)}，
     * 因此实体只能靠 ID 在服务端侧解析（见下方 TODO）。
     *
     * <p>TODO K3-review: 若主控希望客户端也拿到真实实体（例如显示早苗贴图/名称），
     *  需在打开菜单时额外下发实体 ID（MenuType 的 open data 或独立 payload），
     *  客户端再用 {@code level().getEntity(merchantEntityId)} 取回。当前实现中
     *  {@code merchantEntityId} 只用于服务端解析实体；客户端不使用（{@code 0} 即可）。
     *
     * @param containerId      容器 ID
     * @param playerInventory  玩家背包
     * @param merchantEntityId 服务端：早苗实体的网络 ID；实体缺失时退化为「空交易表」商店（与 1.7.10 传 null 等价）
     */
    public MerchantSanaeMenu(int containerId, Inventory playerInventory, int merchantEntityId) {
        super(ModMenus.MERCHANT_SANAE.get(), containerId);
        Player player = playerInventory.player;

        Merchant merchant = null;
        if (!player.level().isClientSide) {
            Entity entity = player.level().getEntity(merchantEntityId);
            if (entity instanceof Merchant tradingEntity) {
                merchant = tradingEntity;
                merchant.setTradingPlayer(player);
            }
        }
        if (merchant == null) {
            // 1.7.10 是 new InventoryMerchant(player, null)（交易表为空）；这里用 vanilla 的客户端占位实现
            // （net.minecraft.world.entity.npc.ClientSideMerchant），语义同为「没有真实交易者」。
            // TODO K3-review: 客户端构造器里的占位不可达（见下方 (int, Inventory) 构造器直接传 0），
            //  仅为防御「实体在打开瞬间被卸载」的服务端竞态；若主控认为多余可删除本分支改为抛异常。
            merchant = new ClientSideMerchant(player);
        }
        this.trader = merchant;
        this.tradeContainer = new MerchantContainer(merchant);

        this.addSlot(new Slot(this.tradeContainer, PAYMENT1_SLOT, 36, 53));
        this.addSlot(new Slot(this.tradeContainer, PAYMENT2_SLOT, 62, 53));
        this.addSlot(new MerchantResultSlot(player, merchant, this.tradeContainer, RESULT_SLOT, 120, 53));

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    /**
     * 客户端/无实体的简化构造器（与 {@code DanmakuCraftingMenu} 的「网络打开用构造器」约定一致）。
     *
     * <p>客户端拿不到真实实体 → 用 {@link ClientSideMerchant} 占位，交易列表由
     * {@code ClientboundMerchantOffersPacket} 经 {@link #setOffers(MerchantOffers)} 下发。
     * 因此 1.7.10 GUI 里那句
     * {@code ((ContainerMerchant)this.inventorySlots).setCurrentRecipeIndex(index)}
     * 在客户端侧不再有意义（客户端只把索引发给服务端），本菜单不提供客户端改写。
     */
    public MerchantSanaeMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, 0);
    }

    /**
     * 1.7.10 {@code ContainerMerchantSanae.canInteractWith} 恒 true
     * （旧代码把 {@code theMerchant.getCustomer() == player} 注释掉了）。
     *
     * <p>这里**保持恒 true**：若改判 {@code trader.getTradingPlayer() == player}，
     * 实体缺失时的降级分支（{@link ClientSideMerchant#getTradingPlayer()} 返回玩家自身除外）
     * 会让界面立刻被关闭，与旧版行为不一致。
     *
     * <p>TODO K3-review: 若主控要求更严格（防止他人共用界面），改为
     *  {@code this.trader.getTradingPlayer() == player}，并保证 setTradingPlayer 生命周期正确。
     */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * 1.7.10 {@code detectAndSendChanges()} 覆写（仅调用 super）在现代由
     * {@code AbstractContainerMenu#broadcastChanges()} 承担，无需覆写。
     */
    @Override
    public void slotsChanged(Container container) {
        this.tradeContainer.updateSellItem();
        super.slotsChanged(container);
    }

    /** 1.7.10 {@code setCurrentRecipeIndex(int)}：设置选中的交易索引（0 起）。 */
    public void setSelectionHint(int currentRecipeIndex) {
        this.tradeContainer.setSelectionHint(currentRecipeIndex);
    }

    /**
     * 服务端把交易列表下发给客户端（对应 vanilla 的 {@code ServerPlayer#sendMerchantOffers}，
     * 由主控在打开菜单处调用）。
     *
     * <p>注意：客户端调用会覆盖占位 merchant 的报价表（这正是期望行为）；
     * 服务端真实实体上不要调用，否则会覆盖实体自身维护的报价。
     */
    public void setOffers(MerchantOffers offers) {
        this.trader.overrideOffers(offers);
    }

    public MerchantOffers getOffers() {
        return this.trader.getOffers();
    }

    /** 1.7.10 {@code getMerchantInventory()}。 */
    public MerchantContainer getMerchantInventory() {
        return this.tradeContainer;
    }

    /** 当前生效的交易（缺货/提示渲染用）。 */
    @Nullable
    public MerchantOffer getActiveOffer() {
        return this.tradeContainer.getActiveOffer();
    }

    /**
     * 1.7.10 {@code updateProgressBar(int,int)} 为空实现；且早苗不是村民、无等级与进度条。
     * 固定 false，避免引入村民经验逻辑（屏幕端据此跳过进度条绘制）。
     */
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        // 对齐 vanilla MerchantMenu：整堆拾取不进入交易槽
        return false;
    }

    /**
     * 1.7.10 {@code transferStackInSlot} → {@code quickMoveStack}。
     * 槽段：0..2=交易槽；3..29=主背包；30..38=快捷栏（与旧版 3..39 / 30..39 一致）。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index == RESULT_SLOT) {
                if (!this.moveItemStackTo(slotStack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemstack);
            } else if (index != PAYMENT1_SLOT && index != PAYMENT2_SLOT) {
                if (index >= INV_SLOT_START && index < INV_SLOT_END) {
                    if (!this.moveItemStackTo(slotStack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= USE_ROW_SLOT_START && index < USE_ROW_SLOT_END
                        && !this.moveItemStackTo(slotStack, INV_SLOT_START, INV_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, INV_SLOT_START, USE_ROW_SLOT_END, false)) {
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

    /**
     * 1.7.10 {@code onContainerClosed} → {@code removed(Player)}：把两个输入槽的内容丢还给玩家。
     *
     * <p>行为差异说明（R4 要求注释差异）：1.7.10 的 {@code ContainerMerchantSanae} 无条件
     * {@code dropPlayerItemWithRandomChoice}（并且 super 被调了两次，属旧版笔误，此处不复刻）；
     * 1.21.1 vanilla 的 {@code MerchantMenu#removed} 在玩家存活时改为
     * {@code placeItemBackInInventory}。这里选择**忠实于本 MOD 旧实现**（无条件丢在地上），
     * 以免出现「关掉界面东西回到背包」这种旧版没有的行为。
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.trader.getTradingPlayer() == player) {
            this.trader.setTradingPlayer(null);
        }
        if (this.trader.isClientSide()) {
            return;
        }
        ItemStack payment1 = this.tradeContainer.removeItemNoUpdate(PAYMENT1_SLOT);
        if (!payment1.isEmpty()) {
            player.drop(payment1, false);
        }
        ItemStack payment2 = this.tradeContainer.removeItemNoUpdate(PAYMENT2_SLOT);
        if (!payment2.isEmpty()) {
            player.drop(payment2, false);
        }
    }
}