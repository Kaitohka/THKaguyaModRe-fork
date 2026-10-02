package thkaguyamod.item;

import java.util.List;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import thkaguyamod.entity.item.EntitySakuyaStopWatch;
import thkaguyamod.entity.item.EntitySakuyaWatch;
import thkaguyamod.init.ModEntities;

/**
 * ストップウォッチ — ported from 1.7.10 {@code thKaguyaMod.item.ItemSakuyaStopWatch}.
 *
 * <p>周囲 20 ブロック以内に懐中時計またはストップウォッチが無いときだけ、
 * 時間停止空間（{@link EntitySakuyaStopWatch}）を出現させ、音を鳴らして
 * クリエイティブ以外は 1 個消費する。
 */
public class ItemSakuyaStopWatch extends Item {

    public ItemSakuyaStopWatch(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        //周囲に既存の懐中時計／ストップウォッチがあるなら使用は失敗する
        List<EntitySakuyaWatch> watches = level.getEntitiesOfClass(EntitySakuyaWatch.class,
                player.getBoundingBox().inflate(20.0D, 20.0D, 20.0D));
        if (!watches.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }
        List<EntitySakuyaStopWatch> stopWatches = level.getEntitiesOfClass(EntitySakuyaStopWatch.class,
                player.getBoundingBox().inflate(20.0D, 20.0D, 20.0D));
        if (!stopWatches.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        // 1.7.10 の "random.click"。1.21.1 では ui.button.click を近似として使用。
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS,
                0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

        EntitySakuyaStopWatch stopWatch = new EntitySakuyaStopWatch(ModEntities.SAKUYA_STOP_WATCH.get(), level, player);
        if (!level.isClientSide) {
            level.addFreshEntity(stopWatch);//時間停止空間を生み出す
        }

        // 使用者がクリエイティブではない場合
        if (!player.isCreative()) {
            stack.shrink(1);
        }

        return InteractionResultHolder.success(stack);
    }
}