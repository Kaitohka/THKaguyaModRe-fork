package thkaguyamod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import thkaguyamod.entity.item.EntityKinkakuzi;
import thkaguyamod.init.ModEntities;

/**
 * 金閣寺の一枚天井（巨大な板を投げつけるアイテム）
 * — ported from 1.7.10 {@code thKaguyaMod.item.ItemKinkakuzi}.
 *
 * <p>右クリックで使用者の上方向に一枚天井（{@link EntityKinkakuzi}）を投げつけて 1 個消費する。
 * スニーク時は威力が上がる（{@code pow} 0.7 → 1.0）。
 *
 * <p>TODO K3-review: 1.7.10 の {@code player.height} は 1.21.1 の {@code getBbHeight()} に相当。
 * 発射位置の Y は「使用者の足元 + 当たり判定高さ」で算出している。
 *
 * <p>TODO K3-review: 原作は {@code itemStack.stackSize--} をクライアントでも行っていた
 * （インベントリ同期前提）。本移植はサーバ側のみで消費する。
 */
public class ItemKinkakuzi extends Item {

    public ItemKinkakuzi(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        double pow = 0.7;
        if (player.isShiftKeyDown()) {
            pow = 1.0;
        }

        //使用者の上方向に発射
        EntityKinkakuzi entityKinkakuzi = new EntityKinkakuzi(ModEntities.KINKAKUZI.get(), level,
                player.getX(), player.getY() + player.getBbHeight(), player.getZ(), player, pow);

        // 1.7.10 の "random.drr"。1.21.1 に対応する音が無いため、block.wood.break を近似として使用。
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOOD_BREAK, SoundSource.PLAYERS,
                0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

        if (!level.isClientSide) {
            level.addFreshEntity(entityKinkakuzi);//金閣寺の一枚天井を出現させる
            stack.shrink(1);//スタックから消滅させる
        }

        return InteractionResultHolder.success(stack);
    }

    /** 1.7.10 の {@code EnumAction.bow} に相当。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return 72000;
    }

    /** アイテムを発光させる（1.7.10 の {@code hasEffect} 相当）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}