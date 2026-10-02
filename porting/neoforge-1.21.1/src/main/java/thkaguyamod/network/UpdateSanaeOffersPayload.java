package thkaguyamod.network;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.menu.MerchantSanaeMenu;

/**
 * Server→client payload carrying a Sanae merchant's current offers.
 *
 * <p>Needed because vanilla's ClientboundMerchantOffersPacket is hardcoded to
 * {@code MerchantMenu} only (see ClientPacketListener#handleMerchantOffers), so our
 * custom menu never receives it.
 */
public record UpdateSanaeOffersPayload(int containerId, MerchantOffers offers) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, "sanae_offers");
    public static final CustomPacketPayload.Type<UpdateSanaeOffersPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSanaeOffersPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UpdateSanaeOffersPayload::containerId,
            MerchantOffers.STREAM_CODEC, UpdateSanaeOffersPayload::offers,
            UpdateSanaeOffersPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateSanaeOffersPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof MerchantSanaeMenu menu && menu.containerId == payload.containerId()) {
                menu.setOffers(payload.offers());
            }
        });
    }
}
