package thkaguyamod.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import thkaguyamod.menu.MiracleMalletMenu;

/**
 * 打ち出の小槌（万宝槌）界面 — 1.7.10 {@code GuiMiracleMallet} 的 1.21.1 移植。
 *
 * <p>与 1.7.10 的对应关系：
 * <ul>
 *   <li>{@code GuiContainer} → {@link AbstractContainerScreen}；{@code xSize = 256}（1.7.10 只在构造器里设了宽度，
 *       高度用默认 166）→ {@code imageWidth = 256} / {@code imageHeight = 166}（默认）。</li>
 *   <li>背景 {@code thkaguyamod:textures/gui/danmaku_crafting_table.png}（与弹幕合成台共用素材）。
 *       该贴图是 256×256，1.7.10 只画左上 256×166，这里用同样尺寸的 blit 保持像素一致。</li>
 *   <li>{@code drawGuiContainerForegroundLayer} → {@link #renderLabels}；
 *       {@code I18n.format} → {@link Component#translatable}。</li>
 *   <li>3 个 {@code GuiButton}（"1 SWING"/"2 SWING"/"3 SWING"）→ {@link Button#builder}
 *       （1.21.1 中 {@code enabled} 字段更名为 {@code active}）。</li>
 * </ul>
 *
 * <p>刻意保留的 1.7.10 行为（非 bug）：
 * <ul>
 *   <li>三个 SWING 按钮**没有任何点击处理**：旧版 {@code GuiMiracleMallet} 未覆写
 *       {@code actionPerformed}，且每个前景帧都把第一个按钮设为 disabled
 *       （{@code drawGuiContainerForegroundLayer} 里那句 {@code doneBtn1.enabled = false}）。
 *       这里同样不给按钮挂动作，并在 {@link #renderLabels} 中把第一个按钮置灰。</li>
 * </ul>
 */
public class MiracleMalletScreen extends AbstractContainerScreen<MiracleMalletMenu> {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("thkaguyamod", "textures/gui/danmaku_crafting_table.png");

    /** 1.7.10 的 3 个 SWING 按钮（无动作，仅装饰）。 */
    private Button swingButton1;
    private Button swingButton2;
    private Button swingButton3;

    public MiracleMalletScreen(MiracleMalletMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256; // 1.7.10: this.xSize = 256（imageHeight 用默认 166）
    }

    @Override
    protected void init() {
        super.init();
        // 1.7.10 initGui: (width - xSize)/2 + 100, (height - ySize)/2 + 3 / +26 / +49，尺寸 64×20
        int x = this.leftPos + 100;
        this.swingButton1 = this.addRenderableWidget(Button.builder(Component.literal("1 SWING"), b -> {
        }).pos(x, this.topPos + 3).size(64, 20).build());
        this.swingButton2 = this.addRenderableWidget(Button.builder(Component.literal("2 SWING"), b -> {
        }).pos(x, this.topPos + 26).size(64, 20).build());
        this.swingButton3 = this.addRenderableWidget(Button.builder(Component.literal("3 SWING"), b -> {
        }).pos(x, this.topPos + 49).size(64, 20).build());
        // TODO K3-review: 三个按钮与旧版一样「空动作」（旧版没有 actionPerformed 覆写）。
        //  若主控判定这是未完成的功能（“1/2/3 SWING”字样暗示挥槌次数），需另立批次设计交互，
        //  本批不做功能新增。
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 贴图 256×256，1.7.10 只画左上 (0,0)-(256,166)
        guiGraphics.blit(BG_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 1.7.10 坐标原样保留
        // TODO K3-review: 旧 lang 文件里只有 item.miracleMallet.name（物品名）。
        //  1.21.1 的合并 lang 中该键已重命名为 item.thkaguyamod.miracle_mallet，故标题用新键（同译名）。
        guiGraphics.drawString(this.font, Component.translatable("item.thkaguyamod.miracle_mallet"),
                6, 6, 4210752, false);
        // TODO K3-review: 1.7.10 引用的 miracleMallet.material / miracleMallet.result 两个键
        //  在旧 lang 的 5 个语言文件里都不存在（1.7.10 会直接显示键名）。这里保留同样的键，
        //  行为等价；若要求不再显示裸键，请在 lang JSON 补 miracleMallet.material / .result 条目。
        guiGraphics.drawString(this.font, Component.translatable("miracleMallet.material"), 134, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("miracleMallet.result"), 192, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.material"), 26, 22, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"),
                48, this.imageHeight - 96 + 2, 4210752, false);

        // 1.7.10: doneBtn1.enabled = false（每帧禁用第一个按钮）
        if (this.swingButton1 != null) {
            this.swingButton1.active = false;
        }
    }

    // TODO K3-review: 注册（主控在 THKaguyaClient 中接线）：
    //   MenuScreens.register(ModMenus.MIRACLE_MALLET.get(), MiracleMalletScreen::new);
    // 菜单构造器已按约定提供 (int containerId, Inventory playerInventory)，
    // 由 ItemUchide 的右键打开（1.7.10 guiMiracleMalletID = 3）——物品行为在 agent-18 批，
    // 接线时请确认用法：player.openMenu(new SimpleMenuProvider(
    //     (id, inv, p) -> new MiracleMalletMenu(id, inv),
    //     Component.translatable("item.thkaguyamod.miracle_mallet")));
}