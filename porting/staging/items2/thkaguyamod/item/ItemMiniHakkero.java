package thkaguyamod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import thkaguyamod.config.ModConfig;
import thkaguyamod.entity.item.EntityMiniHakkero;
import thkaguyamod.init.ModEntities;

/**
 * ミニ八卦炉 — ported from 1.7.10 {@code thKaguyaMod.item.ItemMiniHakkero}.
 *
 * <p>目の前の空間が空いていて、ファイアチャージを 32 個以上所持しているときだけ、
 * ミニ八卦炉（{@link EntityMiniHakkero}）を目の前に設置する。設置と同時にアイテムを 1 個消費し、
 * ファイアチャージを 32 個消費する。{@link ModConfig#UsingMasterSpark} が false なら何もしない。
 *
 * <p>TODO K3-review: 1.7.10 は「消費」をクライアントでも行っていた（インベントリが同期される前提）。
 * 本移植は 1.21.1 の作法に従い、ブロック判定・消費・設置のすべてをサーバ側でのみ実行する
 * （結果としてクライアントでは無反応に見えるが、次 tick の同期で整合する）。
 *
 * <p>旧耐久値は「チャージ具合」ではなく設置地点の damage 引き継ぎに使われていた
 * （{@code itemStack.getItemDamageForDisplay()} を {@code EntityMiniHakkero} に渡す）。
 * 登録側の properties で {@code stacksTo(1)} を維持すること。
 */
public class ItemMiniHakkero extends Item {

    public ItemMiniHakkero(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリック時の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        //マスタースパークの使用を許可しない設定なら何も起きない
        if (!ModConfig.UsingMasterSpark) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        //ミニ八卦炉の設置地点にブロックがないことを確認
        double setX = player.getX() - Math.sin(player.getYRot() / 180F * (float) Math.PI) * Math.cos(player.getXRot() / 180F * (float) Math.PI) * 1.3D;
        double setY = player.getY() - Math.sin(player.getXRot() / 180F * (float) Math.PI) * 1.3D + player.getEyeHeight();
        double setZ = player.getZ() + Math.cos(player.getYRot() / 180F * (float) Math.PI) * Math.cos(player.getXRot() / 180F * (float) Math.PI) * 1.3D;

        // 1.7.10 の isBlockNormalCubeDefault(x, y, z, true) に相当
        BlockPos setPos = BlockPos.containing(setX, setY, setZ);
        BlockState setState = level.getBlockState(setPos);
        if (setState.isSolidRender(level, setPos)) {
            return InteractionResultHolder.pass(stack);
        }

        //ファイアーチャージの所持数を数える
        int count = 0;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack inv = player.getInventory().items.get(i);
            if (!inv.isEmpty() && inv.is(Items.FIRE_CHARGE)) {
                count += inv.getCount();
            }
        }

        //ファイアーチャージを３２個以上持っていないなら使用できない
        if (count < 32) {
            return InteractionResultHolder.pass(stack);
        }

        //インベントリから３２個のファイアーチャージを減らす
        int remaining = 32;
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack inv = player.getInventory().items.get(i);
            if (!inv.isEmpty() && inv.is(Items.FIRE_CHARGE)) {
                int take = Math.min(remaining, inv.getCount());
                inv.shrink(take);
                remaining -= take;
            }
        }

        //ミニ八卦炉を目の前に出現させる
        EntityMiniHakkero miniHakkero = new EntityMiniHakkero(ModEntities.MINI_HAKKERO.get(), level, player, stack.getDamageValue());
        level.addFreshEntity(miniHakkero);//ミニ八卦炉を出す
        stack.shrink(1);//スタックから消滅させる

        return InteractionResultHolder.success(stack);
    }
}
