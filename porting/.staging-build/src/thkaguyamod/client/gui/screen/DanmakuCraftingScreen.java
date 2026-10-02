package thkaguyamod.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import thkaguyamod.menu.DanmakuCraftingMenu;

/**
 * 弹幕合成台（弹版）界面 — 1.7.10 {@code GuiDanmakuCrafting} 的 1.21.1 移植。
 *
 * <p>{@code GuiContainer} → {@link AbstractContainerScreen}；
 * {@code drawGuiContainerForegroundLayer} → {@link #renderLabels}；
 * {@code drawGuiContainerBackgroundLayer} → {@link #renderBg}；{@code I18n.format} → {@link Component#translatable}。
 * 文字坐标与 1.7.10 完全一致。
 */
public class DanmakuCraftingScreen extends AbstractContainerScreen<DanmakuCraftingMenu> {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/gui/danmaku_crafting_table.png");

    public DanmakuCraftingScreen(DanmakuCraftingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256; // 1.7.10: this.xSize = 256（imageHeight 用默认 166）
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BG_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 覆盖父类默认标题，完全按 1.7.10 的坐标绘制
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.name"), 6, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.form"), 134, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.copy"), 192, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.material"), 26, 22, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.number"), 26, 39, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.speed"), 26, 56, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.color"), 87, 22, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.gravity"), 87, 39, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.bound"), 87, 56, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 48, this.imageHeight - 96 + 2, 4210752, false);
    }
}
