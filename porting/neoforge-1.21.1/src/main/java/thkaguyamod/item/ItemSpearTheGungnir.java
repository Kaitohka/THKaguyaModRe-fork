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

import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.THKaguyaLib;
import thkaguyamod.danmaku.THShotLib;

import static thkaguyamod.danmaku.DanmakuConstants.GUNGNIR;
import static thkaguyamod.danmaku.DanmakuConstants.LASER_GUNGNIR;
import static thkaguyamod.danmaku.DanmakuConstants.RED;

/**
 * スピア・ザ・グングニル — ported from 1.7.10 {@code thKaguyaMod.item.ItemSpearTheGungnir}.
 *
 * <p>右クリックで符卡 No.27「神槍「スピア・ザ・グングニル」」を宣言し、成功すると
 * 視線の逆方向へグングニルレーザーを放つ（クリエイティブ以外は耐久 1 消費）。
 *
 * <p>TODO K3-review: 1.7.10 は {@code THKaguyaLib.checkSpellCardDeclaration(world, itemStack, player, 27, 2, true)}
 * を呼んでいた。1.21.1 版の同メソッド（{@code Level}, {@code ItemStack}, {@code LivingEntity}, int, int, boolean）を
 * そのまま使用している。第 2 引数の {@code itemStack} は現状未使用（符卡所持チェックは
 * インベントリ内の {@code spell_card} を直接走査する実装になっている）。
 *
 * <p>TODO K3-review: 原作は耐久を {@code setItemDamage(damage + 1)} で直接増やしていた。
 * 1.21.1 では {@code ItemStack#setDamageValue} が {@code getMaxDamage()} でクランプするため、
 * 登録側の耐久値（原作 95）を超える書き込みはできない。本移植は
 * {@code setDamageValue(getDamageValue() + 1)} としている（登録値が原作どおりなら等価）。
 */
public class ItemSpearTheGungnir extends Item {

    public ItemSpearTheGungnir(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        //神槍「スピア・ザ・グングニル」を使用する
        boolean spellCard = THKaguyaLib.checkSpellCardDeclaration(level, stack, player, 27, 2, true);

        if (spellCard) {
            Vec3 look = player.getLookAngle();
            THShotLib.createLaserA(player, player,
                    THShotLib.pos(player.getX(), THShotLib.getPosYFromEye(player, +0.6D), player.getZ()),
                    THShotLib.angle(-look.x, look.y, -look.z), 0.0D, 2.0D, 0.0D, THShotLib.gravity_Zero(),
                    LaserData.laser(LASER_GUNGNIR, RED, 1.0F, 10.0F, 14F, 0, 120, GUNGNIR));

            if (!level.isClientSide) {
                // 1.7.10 の "mob.zombie.remedy"（1.21.1 では entity.zombie_villager.cure を近似として使用）
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.5F, 2.7F);
                // 1.7.10 の "mob.wither.spawn"
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.2F, 10.0F);
            }

            if (!player.isCreative()) {
                stack.setDamageValue(stack.getDamageValue() + 1);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    /** エンチャント不可。 */
    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    /** 1.7.10 の {@code isBookEnchantable} 相当。 */
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }
}