package thkaguyamod.item;

import java.util.List;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import thkaguyamod.entity.shot.EntityTHShot;

/**
 * 打ち出の小槌 — ported from 1.7.10 {@code thKaguyaMod.item.ItemUchide}.
 *
 * <p>空腹ゲージが 2 以上あるとき右クリックで使用でき、周囲 20 ブロック以内の
 * 「自分の弾幕」({@link EntityTHShot}) すべてのサイズを +0.5 する。使用すると耐久を 30 消費し、
 * インベントリにある限り毎 tick 耐久が 1 ずつ回復する（原作のまま）。
 *
 * <p>TODO K3-review: 原作には「万宝槌変換台」用の機能がコメントアウトで大量に残っており、
 * (1) スニーク時の GUI オープン、(2) 目の前 3 ブロックのドロップアイテムを
 * 「陰陽玉→血に餓えた陰陽玉」「呪いのデコイ人形」「隙間の折りたたみ傘」へ置換する処理、
 * (3) {@code getChestGenBase}（地牢战利品に万宝槌を注入）、の 3 つはいずれも移植していない。
 * (1)(2) は原作でもコメントアウト済み（死コード）、(3) は 1.21.1 の戦利品表 JSON 側で
 * 対応する必要がある（{@code porting/tools} の生成器 or 手書きデータパック）。
 *
 * <p>TODO K3-review: 1.7.10 の {@code ItemUchide} は {@code setItemDamage(damage + 30)} を
 * 直接行っていた。1.21.1 では登録側の耐久値（原作 100）に依存する。
 *
 * <p>TODO K3-review: 原作の {@code getFoodStats().getFoodLevel() > 0} は「空腹ゲージ 2 以上」と
 * コメントされているが実際の条件は「1 以上」。コメントと実装が食い違っているため、
 * 実装（1 以上）をそのまま移植している。
 */
public class ItemUchide extends Item {

    public ItemUchide(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getFoodData().getFoodLevel() > 0)//空腹ゲージ２以上で使用可能
        {
            //周囲のEntityを取得し、自分の弾幕すべてを大きくする
            List<EntityTHShot> list = level.getEntitiesOfClass(EntityTHShot.class,
                    player.getBoundingBox().inflate(20.0D, 20.0D, 20.0D));
            for (EntityTHShot shot : list) {
                if (shot.user == player) {
                    shot.setShotSize(shot.getShotSize() + 0.5F);
                }
            }

            // 1.7.10 の "random.bow"。1.21.1 では entity.arrow.shoot を近似として使用。
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                    0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));
            player.swing(InteractionHand.MAIN_HAND);//投げる動作をさせる
            stack.setDamageValue(stack.getDamageValue() + 30);
        }
        return InteractionResultHolder.success(stack);
    }

    /**
     * インベントリにある限り常時呼び出されるメソッド（1.7.10 {@code onUpdate}）。
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
    }
}