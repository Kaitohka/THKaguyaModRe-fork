package thkaguyamod.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 天界の桃 (Heavenly Peach) — ported from 1.7.10 {@code thKaguyaMod.item.ItemHeavenlyPeach}.
 *
 * <p>Eating grants Resistance (potion id 11 → {@link MobEffects#DAMAGE_RESISTANCE}) for 20 s at
 * amplifier 2 (== Resistance III).
 */
public class ItemHeavenlyPeach extends Item {

    public ItemHeavenlyPeach(Item.Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        // 1.7.10: onFoodEaten はプレイヤー限定だったため、プレイヤーのみ効果を付与する。
        if (!level.isClientSide && livingEntity instanceof Player player) {
            int effectTime = 20 * 20;// 効果時間（15秒なら 15 * 20 だが原作は 20 秒）
            int potionLevel = 2;// ポーション効果のレベル（0 がレベル1 なので 2 == レベル3）
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, effectTime, potionLevel));
        }
        return super.finishUsingItem(stack, level, livingEntity);
    }
}
