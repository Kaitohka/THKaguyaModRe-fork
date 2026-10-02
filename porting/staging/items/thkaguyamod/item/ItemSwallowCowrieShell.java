package thkaguyamod.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 子安貝 (Swallow Cowrie Shell) — ported from 1.7.10 {@code thKaguyaMod.item.ItemSwallowCowrieShell}.
 *
 * <p>Eating consumes experience levels in exchange for hunger/saturation and a Regeneration
 * effect. Effects, food stats and the amount of levels consumed are preserved exactly
 * (regeneration amplifier 1 == Regeneration II; duration = consumedLevels * 3 seconds).
 */
public class ItemSwallowCowrieShell extends Item {

    public ItemSwallowCowrieShell(Item.Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        // 1.7.10 logic ran only for players; non-player living entities (e.g. mobs) fall back to vanilla.
        if (!(livingEntity instanceof Player player)) {
            return super.finishUsingItem(stack, level, livingEntity);
        }

        int performance = player.experienceLevel;
        // スニークでないなら空腹分のみ。スニークなら最大10レベル問答無用で消費する。
        if (!player.isShiftKeyDown()) {
            performance = (int) ((20 - player.getFoodData().getFoodLevel()) / 2.0 + 0.5);
            if (performance < 0) {
                performance = 0;
            } else if (performance > player.experienceLevel) {
                performance = player.experienceLevel;
            }
        }
        if (performance > 10) {
            performance = 10;// 消費は最大10レベル
        }

        player.giveExperienceLevels(-performance);// 経験値消費
        // 回復量 * 隠し満腹度(5.0F) * 2 の計算。子安貝は 20〜200（20の倍数）。
        player.getFoodData().eat(performance * 2, 5F);

        // TODO K3-review: 1.7.10 は addPotionEffect を world.isRemote でガードしていた（クライアントでも
        //  食べた側の見た目として効果が要るケースがあった）。ここではサーバ側のみで付与している。
        if (!level.isClientSide) {
            // 消費レベル×3秒間の強力なリジェネ。amplifier 1 == PotionEffect の「レベル2」。
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, performance * 3 * 20, 1));
        }
        return stack;
    }

    /** 常時発光させる（1.7.10 の hasEffect 相当）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
