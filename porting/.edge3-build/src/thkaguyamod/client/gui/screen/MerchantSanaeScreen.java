package thkaguyamod.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.PacketDistributor;

import thkaguyamod.init.ModItems;
import thkaguyamod.menu.MerchantSanaeMenu;
import thkaguyamod.network.SelectSanaeTradePayload;

/**
 * 守矢神社出張早苗さん 商店界面 — 1.7.10 {@code GuiMerchantSanae} 的 1.21.1 移植。
 *
 * <p>布局完全沿用 1.7.10：{@code xSize = 176}（默认）、{@code ySize = 166}（默认），
 * 背景取 {@code minecraft:textures/gui/container/villager.png} 左上 176×166 区域
 * （旧版同款做法：只 blit 该贴图左上角，7 条交易列表行可见）。
 *
 * <p>对应关系：
 * <ul>
 *   <li>{@code GuiContainer} → {@link AbstractContainerScreen}；
 *       {@code drawGuiContainerForegroundLayer} → {@link #renderLabels}；
 *       {@code drawGuiContainerBackgroundLayer} → {@link #renderBg}；
 *       {@code updateScreen} → {@link #containerTick}；{@code drawScreen} 的附加绘制 → {@link #render} 覆写。</li>
 *   <li>{@code GuiButton} → {@link Button#builder}（见 {@link #ARROW_BUTTON_REMARK}）。</li>
 *   <li>{@code I18n.format} → {@link Component#translatable}。</li>
 *   <li>{@code C17PacketCustomPayload("MC|TrSel")} → {@link SelectSanaeTradePayload}
 *       （发送端 {@code PacketDistributor.sendToServer}）。</li>
 * </ul>
 *
 * <p>刻意保留的 1.7.10 行为（非 bug）：
 * <ul>
 *   <li>翻页按钮只改**客户端**索引并广播，服务端在收到包后设置选中提示；
 *       1.7.10 也是先 {@code setCurrentRecipeIndex} 本地生效再发包，故这里同样先调
 *       {@link MerchantSanaeMenu#setSelectionHint(int)} 再发包。</li>
 *   <li>交易行的三格物品在 1.7.10 中被**硬编码**（见 {@link #PLACEHOLDER_DISPLAY_REMARK}）。</li>
 * </ul>
 */
public class MerchantSanaeScreen extends AbstractContainerScreen<MerchantSanaeMenu> {

    /** 1.7.10 {@code field_147038_v}：原版交易界面贴图（新版本中该图 512×256）。 */
    private static final ResourceLocation VILLAGER_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/villager.png");

    /** 1.21.1 中缺货遮罩已改为 Sprite（1.7.10 为贴图区域 (212,0,28,21)）。 */
    private static final ResourceLocation OUT_OF_STOCK_SPRITE =
            ResourceLocation.withDefaultNamespace("container/villager/out_of_stock");

    private static final int TEXTURE_WIDTH = 512;
    private static final int TEXTURE_HEIGHT = 256;

    /** 交易预览三格坐标（1.7.10 硬编码：drawScreen 内的 (36,24)/(62,24)/(120,24)）。 */
    private static final int PREVIEW_X1 = 36;
    private static final int PREVIEW_X2 = 62;
    private static final int PREVIEW_X3 = 120;
    private static final int PREVIEW_Y = 24;

    private static final String ARROW_BUTTON_REMARK =
            "1.21.1 的 villager.png 已不再包含 1.7.10 那组 12×19 箭头按钮贴图"
            + "（旧版区域 u=176/188/200, v=0/19），故改用 Button.builder 默认按钮外观 + \"<\" / \">\" 文字标签";

    private static final String PLACEHOLDER_DISPLAY_REMARK =
            "1.7.10 的交易预览是硬编码的（第一格 aja_red_stone×1、第三格 illness_recovery_charm×5），"
            + "源码中 merchantrecipe.getItemToBuy()/getItemToSell() 被注释掉；这里保持原行为不做修正";

    /** 1.7.10 {@code field_147043_x}：下一笔交易。 */
    private Button nextButton;
    /** 1.7.10 {@code field_147042_y}：上一笔交易。 */
    private Button prevButton;

    /** 1.7.10 {@code field_147041_z}：客户端选中的交易索引（0 起）。 */
    private int selectionIndex;

    public MerchantSanaeScreen(MerchantSanaeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // 1.7.10: GuiContainer 默认 xSize=176 / ySize=166，未覆写 → 保持默认
    }

    @Override
    protected void init() {
        super.init();
        // 1.7.10 initGui: i=(width-xSize)/2, j=(height-ySize)/2（现代 leftPos/topPos 即二者）
        this.nextButton = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> this.changeSelection(1))
                .pos(this.leftPos + 120 + 27, this.topPos + 24 - 1)
                .size(12, 19)
                .build());
        this.prevButton = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> this.changeSelection(-1))
                .pos(this.leftPos + 36 - 19, this.topPos + 24 - 1)
                .size(12, 19)
                .build());
        // 1.7.10: 两个按钮初始均 disabled（由 updateScreen 按交易表长度开启）
        this.nextButton.active = false;
        this.prevButton.active = false;
        // TODO K3-review: 按钮贴图（见 ARROW_BUTTON_REMARK）。若主控要求还原箭头外观，
        //  需补一张 12×19×3 的箭头贴图资源（1.7.10 用的原版 villager.png 区域在 1.21.1 已不存在），
        //  改用 ImageButton + WidgetSprites。
    }

    /**
     * 1.7.10 {@code updateScreen()} 的现代对应：每 tick 按交易表长度刷新按钮可用状态。
     */
    @Override
    protected void containerTick() {
        super.containerTick();
        MerchantOffers offers = this.menu.getOffers();
        if (this.nextButton != null) {
            this.nextButton.active = this.selectionIndex < offers.size() - 1;
        }
        if (this.prevButton != null) {
            this.prevButton.active = this.selectionIndex > 0;
        }
    }

    /**
     * 1.7.10 {@code actionPerformed}：改索引 → 设置容器索引 → 发包。
     *
     * @param delta 1 = 下一笔（按钮 id 1），-1 = 上一笔（按钮 id 2）
     */
    private void changeSelection(int delta) {
        this.selectionIndex += delta;
        // 1.7.10: ((ContainerMerchant)this.inventorySlots).setCurrentRecipeIndex(field_147041_z)
        this.menu.setSelectionHint(this.selectionIndex);
        // 1.7.10: new C17PacketCustomPayload("MC|TrSel", bytebuf{writeInt(index)})
        PacketDistributor.sendToServer(new SelectSanaeTradePayload(this.selectionIndex));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 1.7.10: bindTexture(villager.png); drawTexturedModalRect(k, l, 0, 0, xSize, ySize)
        // 注意贴图是 512×256（非默认 256×256），必须显式给出贴图尺寸。
        guiGraphics.blit(VILLAGER_BACKGROUND, this.leftPos, this.topPos, 0, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        MerchantOffers offers = this.menu.getOffers();
        if (offers.isEmpty()) {
            return;
        }
        // 防御性越界检查（1.7.10 只判 isEmpty，交易表变化时会越界崩溃 —— 见 R6 稳定性）
        if (this.selectionIndex < 0 || this.selectionIndex >= offers.size()) {
            return;
        }
        MerchantOffer offer = offers.get(this.selectionIndex);
        if (offer.isOutOfStock()) {
            // 1.7.10: drawTexturedModalRect(guiLeft+83, guiTop+21|+51, 212, 0, 28, 21)
            guiGraphics.blitSprite(OUT_OF_STOCK_SPRITE, this.leftPos + 83, this.topPos + 21, 0, 28, 21);
            guiGraphics.blitSprite(OUT_OF_STOCK_SPRITE, this.leftPos + 83, this.topPos + 51, 0, 28, 21);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 1.7.10: 居中显示标题（field_147040_A = 传入的 "Sanae" / entity.Sanae.name）
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 6, 4210752, false);
        // 1.7.10: I18n.format("container.inventory") @ (8, ySize-96+2)
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    /**
     * 1.7.10 {@code drawScreen} 的附加绘制：交易预览三格 + 悬停提示。
     *
     * <p>这里在 {@code super.render(...)} 之后以小地图标方式绘制（1.21.1 中
     * {@code renderFakeItem} 自带 GUI 深度，等价于旧版的 {@code itemRender.zLevel = 100}）。
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        MerchantOffers offers = this.menu.getOffers();
        if (!offers.isEmpty() && this.selectionIndex >= 0 && this.selectionIndex < offers.size()) {
            MerchantOffer offer = offers.get(this.selectionIndex);

            // TODO K3-review: 见 PLACEHOLDER_DISPLAY_REMARK —— 硬编码展示物，忠实保留 1.7.10 行为。
            //  若要修正，应改为 offer.getCostA() / offer.getCostB() / offer.getResult()。
            ItemStack costA = new ItemStack(ModItems.RED_STONE_OF_AJA.get(), 1);      // 1.7.10: aja_red_stone ×1
            ItemStack costB = offer.getCostB();                                        // 1.7.10: merchantrecipe.getSecondItemToBuy()
            ItemStack result = new ItemStack(ModItems.ILLNESS_RECOVERY_CHARM.get(), 5); // 1.7.10: illness_recovery_charm ×5

            int x1 = this.leftPos + PREVIEW_X1;
            int x2 = this.leftPos + PREVIEW_X2;
            int x3 = this.leftPos + PREVIEW_X3;
            int y = this.topPos + PREVIEW_Y;

            guiGraphics.renderFakeItem(costA, x1, y);
            guiGraphics.renderItemDecorations(this.font, costA, x1, y);

            if (!costB.isEmpty()) {
                guiGraphics.renderFakeItem(costB, x2, y);
                guiGraphics.renderItemDecorations(this.font, costB, x2, y);
            }

            guiGraphics.renderFakeItem(result, x3, y);
            guiGraphics.renderItemDecorations(this.font, result, x3, y);

            // 1.7.10: func_146978_c(36,24,16,16,...) → renderToolTip
            if (this.isHovering(PREVIEW_X1, PREVIEW_Y, 16, 16, mouseX, mouseY)) {
                guiGraphics.renderTooltip(this.font, costA, mouseX, mouseY);
            } else if (!costB.isEmpty() && this.isHovering(PREVIEW_X2, PREVIEW_Y, 16, 16, mouseX, mouseY)) {
                guiGraphics.renderTooltip(this.font, costB, mouseX, mouseY);
            } else if (this.isHovering(PREVIEW_X3, PREVIEW_Y, 16, 16, mouseX, mouseY)) {
                guiGraphics.renderTooltip(this.font, result, mouseX, mouseY);
            }

            RenderSystem.enableDepthTest();
        }

        // 槽位悬停提示（父类实现；1.7.10 由 GuiContainer.drawScreen 处理）
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // TODO K3-review: 注册（主控在 THKaguyaClient 中接线）：
    //   MenuScreens.register(ModMenus.MERCHANT_SANAE.get(), MerchantSanaeScreen::new);
    // 打开界面时（EntitySanae#mobInteract 内）建议以
    //   player.openMenu(new SimpleMenuProvider((id, inv, p) ->
    //           new MerchantSanaeMenu(id, inv, this.getId()),
    //           Component.translatable("entity.thkaguyamod.sanae")))
    //   随后 serverPlayer.sendMerchantOffers(...)（或菜单内 setOffers + 包下发），
    //   旧版标题来源为 StatCollector.translateToLocal("entity.Sanae.name")，
    //   新注册名对应的键为 entity.thkaguyamod.sanae（待 living 批确认注册名后最终定键）。
}