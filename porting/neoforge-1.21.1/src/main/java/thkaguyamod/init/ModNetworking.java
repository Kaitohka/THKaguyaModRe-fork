package thkaguyamod.init;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.network.SelectSanaeTradePayload;
import thkaguyamod.network.UpdateSanaeOffersPayload;

/**
 * Custom network payloads (1.7.10 used a raw C17PacketCustomPayload on "MC|TrSel";
 * vanilla's own select-trade packet only targets MerchantMenu, and vanilla's
 * merchant-offers packet likewise, so both directions are custom here).
 */
public final class ModNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(THKaguyaMod.MOD_ID);
        registrar.playToServer(SelectSanaeTradePayload.TYPE, SelectSanaeTradePayload.STREAM_CODEC,
                SelectSanaeTradePayload::handle);
        registrar.playToClient(UpdateSanaeOffersPayload.TYPE, UpdateSanaeOffersPayload.STREAM_CODEC,
                UpdateSanaeOffersPayload::handle);
    }

    /** Push the merchant's current offers to the client that opened the menu. */
    public static void sendSanaeOffers(ServerPlayer player, int containerId, MerchantOffers offers) {
        PacketDistributor.sendToPlayer(player, new UpdateSanaeOffersPayload(containerId, offers));
    }

    private ModNetworking() {}
}
