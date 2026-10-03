package thkaguyamod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModItems;

import static thkaguyamod.danmaku.DanmakuConstants.BLUE;
import static thkaguyamod.danmaku.DanmakuConstants.DIFFUSION01;
import static thkaguyamod.danmaku.DanmakuConstants.FORM_AMULET;
import static thkaguyamod.danmaku.DanmakuConstants.HOMING01;
import static thkaguyamod.danmaku.DanmakuConstants.RED;
import static thkaguyamod.danmaku.THShotLib.gravity_Zero;
import static thkaguyamod.danmaku.THShotLib.pos_Living;

/**
 * 霊夢のホーミングアミュレット / 拡散アミュレット
 * — ported from 1.7.10 {@code thKaguyaMod.item.ItemHomingAmulet}.
 *
 * <p>1.7.10 は 1 アイテムの damage 値 (0=homing / 1=diffusion) で 2 種を表現していた。
 * 1.21.1 では {@link ModItems#HOMING_AMULET} / {@link ModItems#DIFFUSION_AMULET} の
 * 2 アイテムに分割されているため、本クラスはスタックから自分がどちらかを判定する。
 *
 * <p>移植しないもの（1.21.1 ではモデル／クリエイティブタブ側で処理）:
 * {@code getUnlocalizedName}, {@code getIconFromDamage}, {@code registerIcons}, {@code getSubItems}。
 */
public class ItemHomingAmulet extends Item {

    public static final String amuletTypeNames[] = {
            "homing", "diffusion"
    };

    public static final String amuletIconName[] = {
            "thkaguyamod:homingAmulet", "thkaguyamod:diffusionAmulet"
    };

    public ItemHomingAmulet(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリック時の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(ModItems.HOMING_AMULET.get())) {
            if (player.isShiftKeyDown()) {//低速モード
                ShotData shot = ShotData.shot(FORM_AMULET, RED, 1.0F, 8.0F, 0, 90, HOMING01);
                THShotLib.createWideShot(player, player, pos_Living(player), player.getLookAngle(),
                        0.7D, 0.7D, 0.7D, gravity_Zero(), shot, 2, 20F, 0.5D, 0F);
            } else {//高速モード
                ShotData shot = ShotData.shot(FORM_AMULET, RED, 0.4F, 5.0F, 0, 90, HOMING01);
                THShotLib.createWideShot(player, player, pos_Living(player), player.getLookAngle(),
                        0.7D, 0.7D, 0.7D, gravity_Zero(), shot, 5, 100F, 0.5D, 0F);
            }
        } else {
            ShotData shot = ShotData.shot(FORM_AMULET, BLUE, 1.0F, 8.0F, 0, 16, DIFFUSION01);
            THShotLib.createWideShot(player, player, pos_Living(player), player.getLookAngle(),
                    0.5D, 0.0D, -0.03D, gravity_Zero(), shot, 2, 60F, 0.5D, 0F);
        }

        // 1.7.10 の "random.bow"。1.21.1 では entity.arrow.shoot を近似として使用。
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

        if (!level.isClientSide) {
            stack.shrink(1);//一つ消費
        }

        return InteractionResultHolder.success(stack);
    }
}
