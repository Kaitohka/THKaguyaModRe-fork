package thkaguyamod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import thkaguyamod.entity.item.EntitySpiritualStrikeTalisman;
import thkaguyamod.init.ModEntities;

/**
 * 霊撃札 — ported from 1.7.10 {@code thKaguyaMod.item.ItemSpiritualStrikeTalisman}.
 *
 * <p>右クリックで霊撃札（{@link EntitySpiritualStrikeTalisman}）を出現させ、音を鳴らして
 * クリエイティブ以外は 1 枚消費する。
 */
public class ItemSpiritualStrikeTalisman extends Item {

    public ItemSpiritualStrikeTalisman(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        EntitySpiritualStrikeTalisman talisman = new EntitySpiritualStrikeTalisman(ModEntities.SPIRITUAL_STRIKE_TALISMAN.get(), level, player);

        if (!level.isClientSide) {
            level.addFreshEntity(talisman);
        }

        // 1.7.10 の "random.fizz"。1.21.1 では block.fire.extinguish を近似として使用。
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 2.0F, 12.0F);

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        return InteractionResultHolder.success(stack);
    }
}