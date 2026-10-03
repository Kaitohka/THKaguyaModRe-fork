package thkaguyamod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;

/**
 * 弾幕の素（1.7.10 {@code thKaguyaMod.item.ItemShotMaterial} の 1.21.1 移植）。
 *
 * <p>右クリックで {@code FORM_TINY} の白弾を 1 発発射し、非クリエイティブなら 1 個消費する。
 * 「素材」アイテムは 1.21.1 では 19 種の弾／3 種のレーザーに分割されているが、
 * 本クラスは素材アイテム（{@code ModItems.SHOT_MATERIAL}）用。
 */
public class ItemShotMaterial extends Item {

    public ItemShotMaterial(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリック時の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 1.7.10: THShotLib.createShot(player, pos_Living(player), player.getLookVec(), 1.0D,
        //     ShotData.shot(THShotLib.FORM_TINY, THShotLib.WHITE));
        // createShot 内部で「サーバー側のみ addFreshEntity」のガードが掛かっている。
        THShotLib.createShot(player, THShotLib.pos_Living(player), player.getLookAngle(), 1.0D,
                ShotData.shot(THShotLib.FORM_TINY, THShotLib.WHITE));
        THShotLib.playShotSound(player);

        if (!level.isClientSide) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.success(stack);
    }
}
