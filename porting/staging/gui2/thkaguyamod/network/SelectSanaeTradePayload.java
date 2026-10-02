package thkaguyamod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.menu.MerchantSanaeMenu;

/**
 * 守矢神社出張早苗さん 商店的「选择交易」包。
 *
 * <p>1.7.10 对应 {@code C17PacketCustomPayload("MC|TrSel", ByteBuf)}：
 * {@code GuiMerchantSanae#actionPerformed} 里翻页按钮改完索引后，
 * 先 {@code ContainerMerchant.setCurrentRecipeIndex(index)} 本地生效，再把 {@code int} 索引发到服务端。
 * 1.21.1 里原版同名功能的包是 {@link net.minecraft.network.protocol.game.ServerboundSelectTradePacket}，
 * 但它被 {@code ServerGamePacketListenerImpl#handleSelectTrade} 硬编码为「只认 {@code MerchantMenu}」，
 * 自定义菜单收不到，因此这里自建等价包。
 *
 * <p>本包与 1.7.10 一样只携带一个 {@code int} 索引，不携带实体 ID：
 * 服务端按「当前打开的菜单」定位（{@link MerchantSanaeMenu}），与 1.7.10 的
 * {@code ((ContainerMerchant)this.inventorySlots).setCurrentRecipeIndex(...)} 语义一致。
 * 这也避免了原版 {@code ServerboundSelectTradePacket} 那种「无 menu 校验就丢包」的静默失败。
 *
 * <p>注册（由主控在 ModNetworking 统一做）：
 * <pre>
 * // TODO K3-review: 主控在 init/ModNetworking.java 中注册本包（SERVER 向，PLAY 阶段）：
 * //   public static void register(RegisterPayloadHandlersEvent event) {
 * //       PayloadRegistrar registrar = event.registrar("1").optional();
 * //       registrar.playToServer(SelectSanaeTradePayload.TYPE, SelectSanaeTradePayload.STREAM_CODEC,
 * //               SelectSanaeTradePayload::handle);
 * //   }
 * // 并发送端（MerchantSanaeScreen#selectTrade）已写成：
 * //   PacketDistributor.sendToServer(new SelectSanaeTradePayload(index));
 * </pre>
 *
 * @param index 选中的交易索引（0 起）
 */
public record SelectSanaeTradePayload(int index) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, "select_sanae_trade");

    public static final CustomPacketPayload.Type<SelectSanaeTradePayload> TYPE = new CustomPacketPayload.Type<>(ID);

    /** 载荷与 1.7.10 的 {@code bytebuf.writeInt(index)} 等价（VAR_INT 编码，语义同为 int）。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectSanaeTradePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SelectSanaeTradePayload::index,
                    SelectSanaeTradePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 服务端处理：等价 1.7.10 服务端的 {@code ContainerMerchant.setCurrentRecipeIndex(index)}。
     *
     * <p>写成静态方法以便主控在 registrar 里以方法引用接线（{@code SelectSanaeTradePayload::handle}）。
     * 注册时若未指定 {@code executesOn}，handler 在游戏主线程执行，可直接操作菜单。
     * 若将来改到网络线程执行，需要改成 {@code context.enqueueWork(...)}。
     */
    public static void handle(SelectSanaeTradePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            // 本包只注册为 playToServer；客户端收到即为接线错误
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu instanceof MerchantSanaeMenu sanaeMenu && menu.stillValid(player)) {
            sanaeMenu.setSelectionHint(payload.index());
        }
        // 1.7.10 没有 tryMoveItems 这一步（旧版 GUI 自己把素材塞进槽位），故不调用原版 MerchantMenu#tryMoveItems。
    }
}
