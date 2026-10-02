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
 * 激光合成台 — 1.7.10 {@code ContainerDanmakuCraftingLaser} 的 1.21.1 移植。
 *
 * <p>与弹版差异：无重力槽 / 反弹槽（原代码中已注释）；弹数上限为 {@code ModConfig.laserMaxNumber}；
 * 产出物为激光素材物品本身（3 种激光物品之一）。
 *
 * <p>槽位索引表：
 * <pre>
 *   0  = 结果槽            (228, 35)
 *   1  = craftMaterial 素材 (8, 17)   type5
 *   2  = craftWay    弹数   (8, 35)   type0
 *   3  = craftSpeed  速度   (8, 53)   type0
 *   4  = craftCopy   复制   (192,17)  type1
 *   5  = craftColor  颜色   (69, 17)  type3
 *   6..14 = 3×3 形状矩阵   (134+col*18, 17+row*18)
 *   15..41 = 玩家主背包     (48+col*18, 84+row*18)
 *   42..50 = 玩家快捷栏     (48+col*18, 142)
 * </pre>
 */
public class DanmakuLaserCraftingMenu extends AbstractContainerMenu {

    private static final int SHAPE_SPHERE = 4;
    private static final int SHAPE_RING = 5;
    private static final int SHAPE_UNCHANGED = -2;
    private static final int SHAPE_INVALID = -1;

    private static final boolean[] PATTERN_POINT   = {false, false, false, false, true,  false, false, false, false};
    private static final boolean[] PATTERN_FORWARD = {false, true,  false, false, true,  false, false, false, false};
    private static final boolean[] PATTERN_SECTOR  = {true,  true,  true,  false, false, false, false, false, false};
    private static final boolean[] PATTERN_AROUND  = {true,  true,  true,  true,  false, true,  true,  true,  true };
    private static final boolean[] PATTERN_SPHERE  = {true,  true,  true,  true,  true,  true,  true,  true,  true };
    private static final boolean[] PATTERN_RING    = {false, true,  false, true,  false, true,  false, true,  false};
    private static final boolean[] PATTERN_EMPTY   = {false, false, false, false, false, false, false, false, false};

    public final Container craftMatrix   = new SimpleContainer(9);
    public final Container craftMaterial = new SimpleContainer(1);
    public final Container craftSpeed    = new SimpleContainer(1);
    public final Container craftWay      = new SimpleContainer(1);
    public final Container craftCopy     = new SimpleContainer(1);
    public final Container craftColor    = new SimpleContainer(1);
    public final Container craftResult   = new net.minecraft.world.inventory.ResultContainer();

    private final ContainerLevelAccess access;

    private ItemStack resultItem = ItemStack.EMPTY;

    /** 网络打开用构造器。 */
    public DanmakuLaserCraftingMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    /** 便利构造器：仅带方块位置。 */
    public DanmakuLaserCraftingMenu(int containerId, Inventory playerInventory, Level level, BlockPos pos) {
        this(containerId, playerInventory, ContainerLevelAccess.create(level, pos));
    }

    /** 带方块位置的完整便利构造器。 */
    public DanmakuLaserCraftingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenus.DANMAKU_LASER_CRAFTING.get(), containerId);
        this.access = access;
        Player player = playerInventory.player;

        this.addSlot(new SlotLaserResult(player, this, this.craftResult, 0, 228, 35));
        this.addSlot(new SlotDanmakuCraftingA(this.craftMaterial, 0, 8, 17, 5));
        this.addSlot(new SlotDanmakuCraftingA(this.craftWay, 0, 8, 35, 0));
        this.addSlot(new SlotDanmakuCraftingA(this.craftSpeed, 0, 8, 53, 0));
        this.addSlot(new SlotDanmakuCraftingA(this.craftCopy, 0, 192, 17, 1));
        this.addSlot(new SlotDanmakuCraftingA(this.craftColor, 0, 69, 17, 3));

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
        return stillValid(this.access, player, ModBlocks.DANMAKU_LASER_CRAFTING_TABLE.get());
    }

    @Override
    public void slotsChanged(Container container) {
        this.updateResult();
    }

    /** 重新计算合成结果（对应 1.7.10 {@code onCraftMatrixChanged} 主体）。 */
    public void updateResult() {
        ItemStack mat = this.craftMaterial.getItem(0);
        boolean hasParams = !mat.isEmpty() && mat.has(ModDataComponents.DANMAKU.get());

        // 初始状态完全对照 1.7.10：way=1, form=0, addSpeed=0, color=8
        int way = 1;
        int form = 0;
        int speed = 0;
        int color = 8;

        if (hasParams) {
            DanmakuParams base = mat.get(ModDataComponents.DANMAKU.get());
            way = base.number();
            form = base.form();
            speed = base.speed();
            color = base.color();
        }

        ItemStack speedStack = this.craftSpeed.getItem(0);
        if (!speedStack.isEmpty()) {
            speed += speedStack.getCount();
            if (speed > 64 || speed < 0) {
                speed = 64;
            }
        }

        ItemStack wayStack = this.craftWay.getItem(0);
        if (!wayStack.isEmpty()) {
            way += wayStack.getCount();
            if (way > ModConfig.laserMaxNumber || way < 0) {
                way = ModConfig.laserMaxNumber;
            }
        }

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
            // 変化なし
        } else if (shape == SHAPE_INVALID) {
            form = SHAPE_INVALID;
        } else {
            form = shape;
        }

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

        ItemStack produced = ItemStack.EMPTY;
        if (!mat.isEmpty() && !(form == 4 && way > 32) && form >= 0) {
            // 产出物 = 素材槽物品本身（3 种激光物品之一）
            int copyNum = 1;
            if (!this.craftCopy.getItem(0).isEmpty()) {
                int oriNum = mat.getCount();
                copyNum = this.craftCopy.getItem(0).getCount() + 1;
                if (copyNum > oriNum) {
                    copyNum = oriNum;
                }
            }
            produced = new ItemStack(mat.getItem(), copyNum);
            // 激光版无 special/gravity 槽；Infinity 固定 false（1.7.10 Laser 未写 Infinity）
            produced.set(ModDataComponents.DANMAKU.get(),
                    new DanmakuParams(way, form, speed, 0, 0, color, false));
        }

        this.resultItem = produced;
        this.craftResult.setItem(0, produced.isEmpty() ? ItemStack.EMPTY : produced.copy());
    }

    /** 最近一次合成结果（1.7.10 {@code newShot} 字段的对应物）。 */
    public ItemStack getResultItem() {
        return this.resultItem;
    }

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

    /** 从结果槽取出时的扣减逻辑（对应 1.7.10 {@code SlotLaserResult.onPickupFromSlot}）。 */
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

        for (int i = 0; i < this.craftMatrix.getContainerSize(); ++i) {
            ItemStack st = this.craftMatrix.getItem(i);
            if (!st.isEmpty()) {
                this.craftMatrix.removeItem(i, 1);
            }
        }

        // 弹数槽：上限 = laserMaxNumber - 素材当前编号
        {
            int usePoint = ModConfig.laserMaxNumber;
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

        // 速度槽：上限 = 64 - 素材当前速度
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

        slotUseProcess(player, this.craftMaterial, copyNum);
        slotUseProcess(player, this.craftCopy, copyNum - 1);
    }

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

    private static final int[] ITEM_SHOT_COLOR_MAP = {7, 0, 2, 0, 1, 4, 5, 7, 7, 4, 2, 3, 5, 4, 6, 7};

    private static final net.minecraft.world.item.Item[] DYE_BY_META = {
            Items.INK_SAC, Items.RED_DYE, Items.GREEN_DYE, Items.COCOA_BEANS,
            Items.LAPIS_LAZULI, Items.PURPLE_DYE, Items.CYAN_DYE, Items.LIGHT_GRAY_DYE,
            Items.GRAY_DYE, Items.PINK_DYE, Items.LIME_DYE, Items.YELLOW_DYE,
            Items.LIGHT_BLUE_DYE, Items.MAGENTA_DYE, Items.ORANGE_DYE, Items.BONE_MEAL
    };

    private static boolean isDye(ItemStack stack) {
        return dyeIndex(stack) >= 0;
    }

    private static int dyeIndex(ItemStack stack) {
        for (int i = 0; i < DYE_BY_META.length; i++) {
            if (stack.is(DYE_BY_META[i])) {
                return i;
            }
        }
        return -1;
    }

    /** 关闭界面：退还包括 3×3 矩阵在内的所有可交互槽位内容。 */
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
        this.clearContainer(player, this.craftMaterial);
    }

    /**
     * 1.7.10 {@code transferStackInSlot} → {@code quickMoveStack}。
     *
     * <p>TODO K3-review: 原 1.7.10 激光版的槽段常量 (10/37/46) 疑似直接沿用弹版、未随激光布局
     * （玩家槽实际从 15 起）调整；此处按“忠实移植”照抄，行为与原版一致但可能与布局错位，待复核。
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

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        super.clicked(slotId, button, clickType, player);
        this.updateResult();
    }
}
