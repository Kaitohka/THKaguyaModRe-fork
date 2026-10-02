package thkaguyamod.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.DanmakuParams;
import thkaguyamod.init.ModBlocks;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModMenus;

/**
 * 弹幕合成台（弹版）— 1.7.10 {@code ContainerDanmakuCrafting} 的 1.21.1 移植。
 *
 * <p>功能：8 个功能背包 + 3×3 形状矩阵判定 + 数值累加 + 复制逻辑。
 * <ul>
 *   <li>{@code InventoryCrafting} → {@link SimpleContainer}（矩阵用 {@code SimpleContainer(9)}）。</li>
 *   <li>{@code InventoryCraftResult} → {@link net.minecraft.world.inventory.ResultContainer}。</li>
 *   <li>{@code onCraftMatrixChanged(IInventory)} → {@link #slotsChanged(Container)} / {@link #updateResult()}。</li>
 *   <li>全部 NBT 读写（Number/DanmakuForm/Speed/Gravity/Special/Color/Infinity）→
 *       {@link ModDataComponents#DANMAKU} 数据组件 + {@link DanmakuParams}。</li>
 *   <li>{@code onContainerClosed} → {@link #removed(Player)}；{@code transferStackInSlot} → {@link #quickMoveStack}；
 *       {@code slotClick} → {@link #clicked}（保留“点击后刷新结果”语义）。</li>
 *   <li>1.7.10 的 19 个 {@code shot_item} meta 变体 → 19 个独立物品，产出物即“素材槽物品本身”
 *       （矩阵判定出的 0-5 阵型写入 {@code DanmakuParams.form}）。</li>
 * </ul>
 *
 * <p>槽位索引表（与 1.7.10 完全一致）：
 * <pre>
 *   0  = 结果槽            (228, 35)
 *   1  = craftMaterial 素材 (8, 17)   type2
 *   2  = craftWay    弹数   (8, 35)   type0
 *   3  = craftSpeed  速度   (8, 53)   type0
 *   4  = craftCopy   复制   (192,17)  type1
 *   5  = craftColor  颜色   (69, 17)  type3
 *   6  = craftGravity 重力  (69, 35)  type0
 *   7  = craftBound  反弹   (69, 53)  type4
 *   8..16  = 3×3 形状矩阵   (134+col*18, 17+row*18)
 *   17..43 = 玩家主背包      (48+col*18, 84+row*18)
 *   44..52 = 玩家快捷栏      (48+col*18, 142)
 * </pre>
 */
public class DanmakuCraftingMenu extends AbstractContainerMenu {

    /** 阵型矩阵判定哨兵值：命中“全满(球状)/十字(环)”模式时，需再判 way>2。 */
    private static final int SHAPE_SPHERE = 4;
    private static final int SHAPE_RING = 5;
    /** 3×3 全空 → 阵型不变。 */
    private static final int SHAPE_UNCHANGED = -2;
    /** 其它占用模式 → 无效（不产出）。 */
    private static final int SHAPE_INVALID = -1;

    // 3×3 占用模式（index = row*3+col，与矩阵槽位 0..8 一致）
    private static final boolean[] PATTERN_POINT   = {false, false, false, false, true,  false, false, false, false}; // 仅中心 → 0
    private static final boolean[] PATTERN_FORWARD = {false, true,  false, false, true,  false, false, false, false}; // 中心+上 → 1
    private static final boolean[] PATTERN_SECTOR  = {true,  true,  true,  false, false, false, false, false, false}; // 上排3格 → 2
    private static final boolean[] PATTERN_AROUND  = {true,  true,  true,  true,  false, true,  true,  true,  true }; // 除中心全满 → 3
    private static final boolean[] PATTERN_SPHERE  = {true,  true,  true,  true,  true,  true,  true,  true,  true }; // 全满 → 4 (需 way>2)
    private static final boolean[] PATTERN_RING    = {false, true,  false, true,  false, true,  false, true,  false}; // 十字4格 → 5 (需 way>2)
    private static final boolean[] PATTERN_EMPTY   = {false, false, false, false, false, false, false, false, false}; // 全空 → 不变

    // ---- 背包（InventoryCrafting → SimpleContainer；结果用 ResultContainer）----
    public final Container craftMatrix   = new SimpleContainer(9);
    public final Container craftMaterial = new SimpleContainer(1);
    public final Container craftSpeed    = new SimpleContainer(1);
    public final Container craftWay      = new SimpleContainer(1);
    public final Container craftCopy     = new SimpleContainer(1);
    public final Container craftColor    = new SimpleContainer(1);
    public final Container craftGravity  = new SimpleContainer(1);
    public final Container craftBound    = new SimpleContainer(1);
    /**
     * 1.7.10 中 {@code craftSpecial} 从未上屏、也无槽位（构造函数里被注释掉），保留字段仅为结构对齐。
     * 因此它不可能有任何内容，{@link #removed(Player)} 也不需要像 craftBound 那样退还。
     */
    public final Container craftSpecial  = new SimpleContainer(1);
    public final Container craftResult   = new net.minecraft.world.inventory.ResultContainer();

    private final ContainerLevelAccess access;

    /** 最近一次合成结果（等价 1.7.10 的 {@code newShot} 字段）。 */
    private ItemStack resultItem = ItemStack.EMPTY;

    /** 网络打开用构造器（MenuType 工厂引用此签名）。 */
    public DanmakuCraftingMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    /** 便利构造器：仅带方块位置（1.7.10 的 (world, x, y, z) 简化对应）。 */
    public DanmakuCraftingMenu(int containerId, Inventory playerInventory, Level level, BlockPos pos) {
        this(containerId, playerInventory, ContainerLevelAccess.create(level, pos));
    }

    /** 带方块位置的完整便利构造器（服务端 MenuProvider 用）。 */
    public DanmakuCraftingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenus.DANMAKU_CRAFTING.get(), containerId);
        this.access = access;
        Player player = playerInventory.player;

        this.addSlot(new SlotDanmakuResult(player, this, this.craftResult, 0, 228, 35));
        this.addSlot(new SlotDanmakuCraftingA(this.craftMaterial, 0, 8, 17, 2));
        this.addSlot(new SlotDanmakuCraftingA(this.craftWay, 0, 8, 35, 0));
        this.addSlot(new SlotDanmakuCraftingA(this.craftSpeed, 0, 8, 53, 0));
        this.addSlot(new SlotDanmakuCraftingA(this.craftCopy, 0, 192, 17, 1));
        this.addSlot(new SlotDanmakuCraftingA(this.craftColor, 0, 69, 17, 3));
        this.addSlot(new SlotDanmakuCraftingA(this.craftGravity, 0, 69, 35, 0));
        this.addSlot(new SlotDanmakuCraftingA(this.craftBound, 0, 69, 53, 4));
        // 1.7.10 中被注释：new SlotDanmakuCraftingA(this.craftSpecial, 0, 69, 35, 0)

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new SlotDanmakuCraftingA(this.craftMatrix, col + row * 3, 134 + col * 18, 17 + row * 18, 0));
            }
        }

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 48 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 48 + col * 18, 142));
        }

        this.updateResult();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.DANMAKU_CRAFTING_TABLE.get());
    }

    /**
     * 1.7.10 {@code onCraftMatrixChanged(IInventory)} 的现代对应。
     * 注意：矩阵用 {@link SimpleContainer}（不触发回调），矩阵改动后由 {@link #clicked} 统一刷新结果。
     */
    @Override
    public void slotsChanged(Container container) {
        this.updateResult();
    }

    /** 重新计算合成结果并写入结果槽（忠实移植 1.7.10 的 {@code onCraftMatrixChanged} 主体）。 */
    public void updateResult() {
        ItemStack mat = this.craftMaterial.getItem(0);
        boolean hasParams = !mat.isEmpty() && mat.has(ModDataComponents.DANMAKU.get());

        // 初始状态：完全对照 1.7.10 的 way=1, form=0, addSpeed=0, color=8, gravity=0, special=0
        // （素材无组件时也走同一套默认值；color 默认 8=RANDOM 与原实现一致）
        int way = 1;
        int form = 0;
        int speed = 0;
        int gravity = 0;
        int special = 0;
        int color = 8;

        if (hasParams) {
            DanmakuParams base = mat.get(ModDataComponents.DANMAKU.get());
            way = base.number();       // Number
            form = base.form();        // DanmakuForm
            speed = base.speed();      // Speed
            gravity = base.gravity();  // Gravity
            special = base.special();  // Special
            color = base.color();      // Color
        }

        // 速度槽：按堆叠数累加，上限 64
        ItemStack speedStack = this.craftSpeed.getItem(0);
        if (!speedStack.isEmpty()) {
            speed += speedStack.getCount();
            if (speed > 64 || speed < 0) {
                speed = 64;
            }
        }

        // 弹数槽：按堆叠数累加，上限 ModConfig.shotMaxNumber
        ItemStack wayStack = this.craftWay.getItem(0);
        if (!wayStack.isEmpty()) {
            way += wayStack.getCount();
            if (way > ModConfig.shotMaxNumber || way < 0) {
                way = ModConfig.shotMaxNumber;
            }
        }

        // 重力槽：按堆叠数累加，上限 16
        ItemStack gravityStack = this.craftGravity.getItem(0);
        if (!gravityStack.isEmpty()) {
            gravity += gravityStack.getCount();
            if (gravity > 16 || gravity < 0) {
                gravity = 16;
            }
        }

        // 反弹槽：special 写成 DanmakuConstants.BOUND01 + n，上限 +3
        ItemStack boundStack = this.craftBound.getItem(0);
        if (!boundStack.isEmpty()) {
            int num = boundStack.getCount();
            if (special >= DanmakuConstants.BOUND01 && special <= DanmakuConstants.BOUND01 + 3) {
                special = special + num;
            } else {
                special = DanmakuConstants.BOUND01;
                num--;
                special += num;
            }
            if (special > DanmakuConstants.BOUND01 + 3) {
                special = DanmakuConstants.BOUND01 + 3;
            }
        }

        // 形状判定
        int shape = this.resolveShapeForm();
        if (shape == SHAPE_SPHERE) {
            if (way > 2) {
                form = SHAPE_SPHERE;
            }
        } else if (shape == SHAPE_RING) {
            if (way > 2) {
                form = SHAPE_RING;
            }
        } else if (shape == SHAPE_UNCHANGED) {
            // 变化なし
        } else if (shape == SHAPE_INVALID) {
            form = SHAPE_INVALID;
        } else {
            form = shape;
        }

        // 颜色槽：16 色染料 / 火药=随机(8) / 钻石=彩虹(9)
        ItemStack colorStack = this.craftColor.getItem(0);
        if (!colorStack.isEmpty()) {
            if (isDye(colorStack)) {
                color = ITEM_SHOT_COLOR_MAP[dyeIndex(colorStack) % 16];
            } else if (colorStack.is(Items.GUNPOWDER)) {
                color = DanmakuConstants.RANDOM;
            } else if (colorStack.is(Items.DIAMOND)) {
                color = DanmakuConstants.RAINBOW;
            }
        }

        // 产出判定：素材存在、球状(4)时 way<=32、阵型有效
        ItemStack produced = ItemStack.EMPTY;
        if (!mat.isEmpty() && !(form == 4 && way > 32) && form >= 0) {
            // 产出物 = 素材槽物品本身（1.21.1 中 19 弹形已拆分为独立物品，等价 1.7.10 的 setItemDamage(meta)）
            int copyNum = 1;
            if (!this.craftCopy.getItem(0).isEmpty()) {
                int oriNum = mat.getCount();
                copyNum = this.craftCopy.getItem(0).getCount() + 1;
                if (copyNum > oriNum) {
                    copyNum = oriNum;
                }
            }
            produced = new ItemStack(mat.getItem(), copyNum);
            // Infinity 不由合成台写入（1.7.10 亦未设置，仅在创造模式射击时置位）→ 固定 false
            produced.set(ModDataComponents.DANMAKU.get(),
                    new DanmakuParams(way, form, speed, gravity, special, color, false));
        }

        this.resultItem = produced;
        this.craftResult.setItem(0, produced.isEmpty() ? ItemStack.EMPTY : produced.copy());
    }

    /** 最近一次合成结果（1.7.10 {@code newShot} 字段的对应物）。 */
    public ItemStack getResultItem() {
        return this.resultItem;
    }

    /** 3×3 占用模式 → 阵型判定（仅作模式识别；球状/环还需 way>2，见 {@link #updateResult()}）。 */
    private int resolveShapeForm() {
        if (this.matches(PATTERN_POINT))   return 0;
        if (this.matches(PATTERN_FORWARD)) return 1;
        if (this.matches(PATTERN_SECTOR))  return 2;
        if (this.matches(PATTERN_AROUND))  return 3;
        if (this.matches(PATTERN_SPHERE))  return SHAPE_SPHERE;
        if (this.matches(PATTERN_RING))    return SHAPE_RING;
        if (this.matches(PATTERN_EMPTY))   return SHAPE_UNCHANGED;
        return SHAPE_INVALID;
    }

    private boolean matches(boolean[] pattern) {
        for (int i = 0; i < 9; i++) {
            if ((!this.craftMatrix.getItem(i).isEmpty()) != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从结果槽取出时的扣减逻辑（忠实移植 1.7.10 {@code SlotDanmakuResult.onPickupFromSlot}）。
     * 由 {@link SlotDanmakuResult#onTake} 调用。
     */
    void removeResult(Player player) {
        int copyNum = 1;
        int oriNum = 1;
        if (!this.craftCopy.getItem(0).isEmpty()) {
            copyNum += this.craftCopy.getItem(0).getCount();
        }
        if (!this.craftMaterial.getItem(0).isEmpty()) {
            oriNum = this.craftMaterial.getItem(0).getCount();
        }
        if (copyNum > oriNum) {
            copyNum = oriNum;
        }

        // 1) 阵型矩阵：每格消耗 1（容器物品返还，见 slotUseProcess 的说明）
        for (int i = 0; i < this.craftMatrix.getContainerSize(); ++i) {
            ItemStack st = this.craftMatrix.getItem(i);
            if (!st.isEmpty()) {
                this.craftMatrix.removeItem(i, 1);
            }
        }

        // 2) 弹数槽：上限 = shotMaxNumber - 素材当前编号
        {
            int usePoint = ModConfig.shotMaxNumber;
            ItemStack matStack = this.craftMaterial.getItem(0);
            boolean hasParams = !matStack.isEmpty() && matStack.has(ModDataComponents.DANMAKU.get());
            if (hasParams) {
                int baseNum = matStack.get(ModDataComponents.DANMAKU.get()).number();
                if (!this.craftWay.getItem(0).isEmpty()) {
                    if (this.craftWay.getItem(0).getCount() > usePoint - baseNum) {
                        usePoint = usePoint - baseNum;
                    }
                }
            } else {
                if (!this.craftWay.getItem(0).isEmpty()) {
                    int numNum = this.craftWay.getItem(0).getCount();
                    if (numNum > usePoint) {
                        usePoint--;
                    } else {
                        usePoint = numNum;
                    }
                }
            }
            slotUseProcess(player, this.craftWay, usePoint);
        }

        // 3) 速度槽：上限 = 64 - 素材当前速度
        {
            int usePoint = 64;
            ItemStack matStack = this.craftMaterial.getItem(0);
            boolean hasParams = !matStack.isEmpty() && matStack.has(ModDataComponents.DANMAKU.get());
            if (hasParams) {
                int baseSpeed = matStack.get(ModDataComponents.DANMAKU.get()).speed();
                if (!this.craftSpeed.getItem(0).isEmpty()) {
                    if (this.craftSpeed.getItem(0).getCount() > 64 - baseSpeed) {
                        usePoint = 64 - baseSpeed;
                    }
                }
            }
            slotUseProcess(player, this.craftSpeed, usePoint);
        }

        // 4) 重力槽：上限 = 16 - 素材当前重力
        {
            int usePoint = 16;
            ItemStack matStack = this.craftMaterial.getItem(0);
            boolean hasParams = !matStack.isEmpty() && matStack.has(ModDataComponents.DANMAKU.get());
            if (hasParams) {
                int baseGravity = matStack.get(ModDataComponents.DANMAKU.get()).gravity();
                if (!this.craftGravity.getItem(0).isEmpty()) {
                    if (this.craftGravity.getItem(0).getCount() > 16 - baseGravity) {
                        usePoint = 16 - baseGravity;
                    }
                }
            }
            slotUseProcess(player, this.craftGravity, usePoint);
        }

        // 5) 反弹槽：上限 = 4 - (素材 special - BOUND01 + 1)   [对应 craftSpecial]
        {
            int usePoint = 4;
            ItemStack matStack = this.craftMaterial.getItem(0);
            boolean hasParams = !matStack.isEmpty() && matStack.has(ModDataComponents.DANMAKU.get());
            if (hasParams) {
                int baseBound = matStack.get(ModDataComponents.DANMAKU.get()).special() - DanmakuConstants.BOUND01 + 1;
                if (!this.craftSpecial.getItem(0).isEmpty()) {
                    if (this.craftSpecial.getItem(0).getCount() > 4 - baseBound) {
                        usePoint = 4 - baseBound;
                    }
                }
            }
            slotUseProcess(player, this.craftSpecial, usePoint);
        }

        // 6) 素材 + 复制槽（对应 1.7.10 中 slotUseProcess(craftMaterial, copyNum) / (craftCopy, copyNum-1)）
        slotUseProcess(player, this.craftMaterial, copyNum);
        slotUseProcess(player, this.craftCopy, copyNum - 1);
    }

    /**
     * 取出成品后对单个槽位的消耗处理（1.7.10 {@code SlotDanmakuResult.slotUseProcess}）。
     * 容器物品返还：现代等价为 {@code Item#getCraftingRemainingItem(ItemStack)}；默认 None → 无操作。
     */
    private void slotUseProcess(Player player, Container container, int useNum) {
        if (useNum <= 0) {
            return;
        }
        ItemStack stack = container.getItem(0);
        if (stack.isEmpty()) {
            return;
        }
        container.removeItem(0, useNum);

        if (stack.hasCraftingRemainingItem()) {
            ItemStack remainder = stack.getCraftingRemainingItem();
            if (!remainder.isEmpty()) {
                if (!player.getInventory().add(remainder)) {
                    player.drop(remainder, false);
                }
            }
        }
    }

    // ---- 颜色映射（1.7.10 ItemTHShot.sellectbleColor：16 色染料 → 8 弹色）----
    // index = 原版染料 meta（0=ink_sac … 15=bone_meal）
    private static final int[] ITEM_SHOT_COLOR_MAP = {7, 0, 2, 0, 1, 4, 5, 7, 7, 4, 2, 3, 5, 4, 6, 7};

    /** 原版染料 meta 顺序（0..15）。 */
    private static final net.minecraft.world.item.Item[] DYE_BY_META = {
            Items.INK_SAC,          // 0
            Items.RED_DYE,          // 1
            Items.GREEN_DYE,        // 2
            Items.COCOA_BEANS,      // 3
            Items.LAPIS_LAZULI,     // 4
            Items.PURPLE_DYE,       // 5
            Items.CYAN_DYE,         // 6
            Items.LIGHT_GRAY_DYE,   // 7
            Items.GRAY_DYE,         // 8
            Items.PINK_DYE,         // 9
            Items.LIME_DYE,         // 10
            Items.YELLOW_DYE,       // 11
            Items.LIGHT_BLUE_DYE,   // 12
            Items.MAGENTA_DYE,      // 13
            Items.ORANGE_DYE,       // 14
            Items.BONE_MEAL         // 15
    };

    private static boolean isDye(ItemStack stack) {
        return dyeIndex(stack) >= 0;
    }

    /** 返回 1.7.10 染料 meta（0..15），非染料返回 -1。 */
    private static int dyeIndex(ItemStack stack) {
        for (int i = 0; i < DYE_BY_META.length; i++) {
            if (stack.is(DYE_BY_META[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 关闭界面：退还包括 3×3 矩阵在内的所有可交互槽位内容（1.7.10 {@code onContainerClosed}）。
     * craftSpecial 在原实现中被注释掉、且无槽位，故不退还（保持一致）。
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) {
            return;
        }
        this.clearContainer(player, this.craftMatrix);
        this.clearContainer(player, this.craftWay);
        this.clearContainer(player, this.craftSpeed);
        this.clearContainer(player, this.craftCopy);
        this.clearContainer(player, this.craftColor);
        this.clearContainer(player, this.craftGravity);
        this.clearContainer(player, this.craftBound);
        this.clearContainer(player, this.craftMaterial);
    }

    /**
     * 1.7.10 {@code transferStackInSlot} → {@code quickMoveStack}。
     * 槽段：0=结果；1..9=功能槽+矩阵；10..36=主背包；37..45=快捷栏。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index == 0) {
                if (!this.moveItemStackTo(slotStack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemstack);
            } else if (index >= 10 && index < 37) {
                if (!this.moveItemStackTo(slotStack, 37, 46, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 37 && index < 46) {
                if (!this.moveItemStackTo(slotStack, 10, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 10, 46, false)) {
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
     * 1.7.10 {@code slotClick}：先走原生点击，再刷新结果（“点击后刷新结果”语义）。
     * 现代 {@code clicked} 返回 void；刷新后的结果保存在 {@link #resultItem} 并写入结果槽。
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        super.clicked(slotId, button, clickType, player);
        this.updateResult();
    }
}
