package thkaguyamod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.item.EntitySilverKnife;
import thkaguyamod.init.ModEntities;

/**
 * 咲夜の銀のナイフ — ported from 1.7.10 {@code thKaguyaMod.item.ItemSilverKnife}.
 *
 * <p>投げてよし、切ってよし。右クリックで 1 本投げ（白ナイフ以外は消費）、
 * 近接攻撃で 3.0 ダメージ、白ナイフ（damage 3）は攻撃で 1 本消滅する。
 *
 * <p>1.7.10 は 1 アイテムの damage 値 (0=青 / 1=赤 / 2=緑 / 3=白) で 4 種を表現していたが、
 * 1.21.1 では {@link thkaguyamod.init.ModItems#SILVER_KNIFE_BLUE} 以下の 4 アイテムに
 * 分割されている。本クラスはスタックのアイテムから色番号を導出する。
 *
 * <p>移植しないもの: {@code getUnlocalizedName}/{@code getIconFromDamage}/{@code registerIcons}/
 * {@code getSubItems}（アイコンと表示名は 1.21.1 のモデル／個別アイテム側で処理）。
 *
 * <p>TODO K3-review: 原作の {@code getSubItems} は damage 0..2 の 3 種しか登録しておらず
 * 白ナイフが創造タブに出ていなかった（原作のバグ）。1.21.1 では 4 種すべてが個別アイテムとして
 * 登録されるためこの不整合は解消される。
 */
public class ItemSilverKnife extends Item {

    private static final String knifeColorNames[] = {
            "blue", "red", "green", "white"
    };

    private static final String knifeIconName[] = {
            "SilverKnife_Blue", "SilverKnife_Red", "SilverKnife_Green", "SilverKnife_White"
    };

    public ItemSilverKnife(Item.Properties properties) {
        super(properties);
    }

    /**
     * スタックに対応する色番号（旧 damage 値 0..3）を返す。
     * 4 種のうちどれでもない場合は 0（青）として扱う。
     */
    public static int getKnifeColor(ItemStack stack) {
        if (stack.is(thkaguyamod.init.ModItems.SILVER_KNIFE_RED.get())) {
            return 1;
        }
        if (stack.is(thkaguyamod.init.ModItems.SILVER_KNIFE_GREEN.get())) {
            return 2;
        }
        if (stack.is(thkaguyamod.init.ModItems.SILVER_KNIFE_WHITE.get())) {
            return 3;
        }
        return 0;
    }

    /**
     * 色名（旧 {@code getUnlocalizedName} の接尾辞。1.21.1 では未使用）。
     */
    public static String getKnifeColorName(ItemStack stack) {
        return knifeColorNames[getKnifeColor(stack)];
    }

    /**
     * Entityに当たったときの処理（1.7.10 {@code hitEntity}）。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.hurt(target.damageSources().mobAttack(attacker), 3.0F);
        //白ナイフなら消える
        if (getKnifeColor(stack) == 3) {
            stack.shrink(1);
        }
        return true;
    }

    /** 原作の {@code hitEntity} は耐久を消費しなかったため、既定の消費は行わない。 */
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        double speed = 0.28D;

        Vec3 angle = THShotLib.angle(player.getYRot(), player.getXRot());
        Vec3 pos = THShotLib.pos_Distance(THShotLib.pos_Living(player), angle, speed);

        EntitySilverKnife silverKnife = new EntitySilverKnife(ModEntities.SILVER_KNIFE.get(), level, player, pos, angle, speed, getKnifeColor(stack));

        // 1.7.10 の "random.bow"。1.21.1 では entity.arrow.shoot を近似として使用。
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

        if (!level.isClientSide) {
            level.addFreshEntity(silverKnife);//銀のナイフを出現させる
            stack.shrink(1);//一つ消費
        }

        player.swing(InteractionHand.MAIN_HAND);//投げる動作をさせる

        return InteractionResultHolder.success(stack);
    }
}