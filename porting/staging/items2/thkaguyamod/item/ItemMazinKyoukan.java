package thkaguyamod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import thkaguyamod.entity.item.EntityMazinkyoukan;
import thkaguyamod.init.ModEntities;

/**
 * 魔人経巻 — ported from 1.7.10 {@code thKaguyaMod.item.ItemMazinkyoukan}.
 *
 * <p>右クリックで使用者に魔人経巻（{@link EntityMazinkyoukan}）を出現させる。
 * 離したときの処理は原作どおり空。
 *
 * <p>TODO K3-review: 原作は {@code onPlayerStoppedUsing} と {@code onEaten} をどちらも空で
 * 上書きしていた。1.21.1 では {@code releaseUsing} が既定空実装のため上書きしていない
 * （{@code finishUsingItem} も同じく既定動作のまま）。挙動は等価。
 */
public class ItemMazinKyoukan extends Item {

    public ItemMazinKyoukan(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        player.startUsingItem(hand);//アイテムの使用継続時間を記憶させる

        EntityMazinkyoukan mazinkyoukan = new EntityMazinkyoukan(ModEntities.MAZINKYOUKAN.get(), level, player);
        if (!level.isClientSide) {
            level.addFreshEntity(mazinkyoukan);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /** 1.7.10 の {@code EnumAction.block} に相当。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    /** アイテムを発光させる（1.7.10 の {@code hasEffect} 相当）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}