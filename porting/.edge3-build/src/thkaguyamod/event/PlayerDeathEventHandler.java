package thkaguyamod.event;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import thkaguyamod.config.ModConfig;
import thkaguyamod.init.ModItems;
import thkaguyamod.init.ModSounds;

/**
 * 残機（エクステンドアイテム）を持っているプレイヤーが倒れたとき、その場で復活させる。
 * 1.7.10 {@code THKaguyaPlayerDeathEventHandler} の移植。
 */
public final class PlayerDeathEventHandler {

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.EXTEND_ITEM.get())) {
                stack.shrink(1);
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth());
                player.invulnerableTime = 100;
                player.deathTime = 0;
                player.level().playSound(null, player.blockPosition(),
                        ModSounds.DOWN.get(), SoundSource.PLAYERS, ModConfig.DownVol, 1.0F);
                // TODO(phase4/spellcard): spawn the purple EntitySpellCardCircle revive effect
                break;
            }
        }
    }

    public PlayerDeathEventHandler() {}
}
