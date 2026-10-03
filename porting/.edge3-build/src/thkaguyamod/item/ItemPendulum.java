package thkaguyamod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import thkaguyamod.entity.item.EntityPendulum;
import thkaguyamod.init.ModEntities;

/**
 * ナズーリンペンデュラム — ported from 1.7.10 {@code thKaguyaMod.item.ItemPendulum}.
 *
 * <p>使用時に「ホットバーの右隣のブロックアイテム」を探索対象として
 * {@link EntityPendulum} を出現させ、そのアイテム名をチャットに出す。
 * スニーク時は高感度モード（{@code mode = 1}）。
 *
 * <p>TODO K3-review: 1.7.10 は {@code StatCollector.translateToLocal(item.getUnlocalizedName() + ".name")}
 * でアイテム名を取り出していた。1.21.1 では {@link ItemStack#getHoverName()} が同じ役割を担う。
 * ただし原作は「ブロック ID から作られた ItemStack」の名前ではなくアイテム種別名を使っていたため、
 * エンチャント等の付加表示が付く場合は表示が僅かに変わる。
 *
 * <p>TODO K3-review: ホットバーの右端（{@code currentItem == 8}）では探索できない。原作どおり
 * 「右隣が存在しないなら失敗」という条件を維持している。
 */
public class ItemPendulum extends Item {

    public ItemPendulum(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        //右にアイテムがないなら失敗する
        if (player.getInventory().selected >= 8) {
            return InteractionResultHolder.pass(stack);
        }

        ItemStack searchItem = player.getInventory().getItem(player.getInventory().selected + 1);

        // 1.7.10 は null チェックだったが、1.21.1 は空スタック判定
        if (searchItem.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }
        //アイテムがブロックでないならサーチ不能
        if (!(searchItem.getItem() instanceof BlockItem)) {
            return InteractionResultHolder.pass(stack);
        }

        int mode = 0;
        if (player.isShiftKeyDown())//スニークなら高感度ナズーリンペンデュラムになる
        {
            mode = 1;
        }

        String itemName = searchItem.getHoverName().getString();

        EntityPendulum entityPendulum = new EntityPendulum(ModEntities.PENDULUM.get(), level, player, mode, itemName, searchItem);

        // 1.7.10 の "random.orb"。1.21.1 では entity.experience_orb.pickup を近似として使用。
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 0.9F);

        if (!level.isClientSide) {
            level.addFreshEntity(entityPendulum);//ペンデュラムを出現させる
            stack.shrink(1);//スタックから消滅させる
        }

        //使用時のメッセージを表示
        String str;
        if (mode == 0) {
            str = Component.translatable("thKaguya.msg.pendulum1").getString()
                    + itemName
                    + Component.translatable("thKaguya.msg.pendulum2").getString();
        } else {
            str = Component.translatable("thKaguya.msg.pendulum1").getString()
                    + itemName
                    + Component.translatable("thKaguya.msg.pendulum3").getString();
        }
        if (!level.isClientSide) {
            player.sendSystemMessage(Component.literal(str));
        }

        return InteractionResultHolder.success(stack);
    }
}