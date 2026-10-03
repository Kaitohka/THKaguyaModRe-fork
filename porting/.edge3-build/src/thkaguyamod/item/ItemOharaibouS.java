package thkaguyamod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.effect.EntityMiracleCircle;
import thkaguyamod.entity.shot.EntitySanaeWind;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModEntities;
import thkaguyamod.init.ModItems;

import static thkaguyamod.danmaku.DanmakuConstants.WIND01;
import static thkaguyamod.danmaku.THShotLib.gravity_Zero;
import static thkaguyamod.danmaku.THShotLib.rotate_Default;

/**
 * 早苗の持つお祓い棒 — ported from 1.7.10 {@code thKaguyaMod.item.ItemOharaibouS}.
 *
 * <p>信仰（プレイヤーの経験値レベル）に応じて使える技が変わる。右クリックで五芒星
 * （{@link EntityMiracleCircle}）を描き始め、離した時点で
 * <ul>
 *   <li>30 tick 未満なら通常攻撃「風起し」（{@link EntitySanaeWind}）</li>
 *   <li>30 tick 以上なら、インベントリの該当スペルカードに「奇跡の力」を付与（{@code Charge} コンポーネント）</li>
 * </ul>
 *
 * <p>1.7.10 は {@code spell_card} 1 アイテムの damage 値で 35 枚を表現していたため、
 * ここでは {@link ModItems#SPELL_CARD_MIRACLE_FRUIT} 等の個別アイテムと照合する。
 * damage 14=ミラクルフルーツ / 15=ファフロッキーズ / 16=妖力スポイラー /
 * 17=モーゼの奇跡 / 18=八坂の神風。
 *
 * <p>TODO K3-review: 1.7.10 は「奇跡の力」を NBT {@code Charge} に書き込んでいた。
 * 本移植は契約どおり {@link ModDataComponents#CHARGE} を使うが、符卡側
 * （{@code THKaguyaLib.checkSpellCardDeclaration}）の Charge ガードは未確定のため、
 * 書き込んだ値が実際に宣言判定へ効くかは符卡側の対応待ち。
 */
public class ItemOharaibouS extends Item {

    public ItemOharaibouS(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        player.startUsingItem(hand);//右クリックを押し始めた

        EntityMiracleCircle entityMiracleCircle = new EntityMiracleCircle(ModEntities.MIRACLE_CIRCLE.get(), level, player, 0);
        if (!level.isClientSide) {
            level.addFreshEntity(entityMiracleCircle);//１つ目の五芒星を出現させる
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * 右クリックを終了したときに呼び出されるメソッド（1.7.10 {@code onPlayerStoppedUsing}）。
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int usedTime) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        int time = getUseDuration(stack, player) - usedTime;//アイテムの右クリックしてた時間

        if (time >= 30)//30tick経過しているなら（五芒星を一つ以上描き終えているなら）
        {
            for (int i = 0; i < player.getInventory().items.size(); i++)//使用者のインベントリのアイテムを１つずつ見ていく
            {
                ItemStack inv = player.getInventory().items.get(i);
                if (inv.isEmpty()) {
                    continue;
                }

                //五芒星を５つ以上、使用者のレベルが４０以上、大奇跡「八坂の神風」なら
                if (time >= 150 && player.experienceLevel >= 40 && inv.is(ModItems.SPELL_CARD_YASAKA_NO_KAMIKAZE.get())) {
                    setMiraclePower(inv);//スペルカードを使用可能状態にする
                    stack.setDamageValue(Math.max(0, stack.getDamageValue() - 30));//お祓い棒の耐久を３０回復
                }
                //五芒星を４つ以上、使用者のレベルが３５以上、開海「モーゼの奇跡」なら
                else if (time >= 120 && player.experienceLevel >= 35 && inv.is(ModItems.SPELL_CARD_MOSES_NO_KISEKI.get())) {
                    setMiraclePower(inv);
                    stack.setDamageValue(Math.max(0, stack.getDamageValue() - 20));
                }
                //五芒星を３つ以上、使用者のレベルが３０以上、妖怪退治「妖力スポイラー」なら
                else if (time >= 90 && player.experienceLevel >= 30 && inv.is(ModItems.SPELL_CARD_YOURYOKU_SPOILER.get())) {
                    setMiraclePower(inv);
                    stack.setDamageValue(Math.max(0, stack.getDamageValue() - 15));
                }
                //五芒星を２つ以上、使用者のレベルが２５以上、奇跡「ファフロッキーズの奇跡」なら
                else if (time >= 60 && player.experienceLevel >= 25 && inv.is(ModItems.SPELL_CARD_FAFROTSKIES_NO_KISEKI.get())) {
                    setMiraclePower(inv);
                    stack.setDamageValue(Math.max(0, stack.getDamageValue() - 10));
                }
                //五芒星を１つ以上、使用者のレベルが２０以上、奇跡「ミラクルフルーツ」なら
                else if (player.experienceLevel >= 20 && inv.is(ModItems.SPELL_CARD_MIRACLE_FRUIT.get())) {
                    setMiraclePower(inv);
                    // 原作どおり -5（他分岐の -10/-15/-20/-30 とは非対称。原作のまま維持）
                    stack.setDamageValue(Math.max(0, stack.getDamageValue() - 5));
                }
            }
        }

        //五芒星を描き終えていないなら
        if (time < 30)//通常攻撃の風起し
        {
            Vec3 vec3 = THShotLib.getVecFromAngle(player.getYRot(), player.getXRot(), 1.0D);
            float damage = player.experienceLevel / 5F;
            int endurance = player.experienceLevel * 3;
            if (endurance > 120) {
                endurance = 120;
            } else if (endurance < 15) {
                endurance = 15;
            }
            if (damage == 0) {
                damage = 0.1F;
            }

            EntitySanaeWind entitySanaeWind = new EntitySanaeWind(ModEntities.SANAE_WIND.get(), level, player, player,
                    THShotLib.pos_Living(player), vec3, 0F,
                    rotate_Default(), 0F, 9999, 0.4D, 0.01D, 0.99D, gravity_Zero(), 0, 1.0F, damage, 0, endurance, WIND01);
            if (!level.isClientSide) {
                level.addFreshEntity(entitySanaeWind);
            }
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
    }

    /** スペルカードに奇跡の力を付与（1.7.10 の NBT {@code Charge} → データコンポーネント）。 */
    private void setMiraclePower(ItemStack stack) {
        stack.set(ModDataComponents.CHARGE.get(), 1000);
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
}
