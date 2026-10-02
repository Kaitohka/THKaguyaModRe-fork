package thkaguyamod.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import thkaguyamod.menu.DanmakuLaserCraftingMenu;

/**
 * 激光合成台界面 — 1.7.10 {@code GuiDanmakuCraftingLaser} 的 1.21.1 移植。
 *
 * <p>文字坐标与 1.7.10 一致（不含重力/反弹标签）。
 */
public class DanmakuLaserCraftingScreen extends AbstractContainerScreen<DanmakuLaserCraftingMenu> {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/gui/laser_crafting_table.png");

    public DanmakuLaserCraftingScreen(DanmakuLaserCraftingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256; // 1.7.10: this.xSize = 256（imageHeight 用默认 166）
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BG_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable("laserCrafting.name"), 6, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.form"), 134, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.copy"), 192, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.material"), 26, 22, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.number"), 26, 39, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.speed"), 26, 56, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.color"), 87, 22, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 48, this.imageHeight - 96 + 2, 4210752, false);
    }
}
