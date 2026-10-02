package thkaguyamod.item;

import java.util.List;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * こいしの閉ざした第三の目 (Closed Third Eye) — ported from 1.7.10
 * {@code thKaguyaMod.item.ItemClosedThirdEye}.
 *
 * <p>Right-click grants 1 s of Invisibility and clears the user from the aggro target of every
 * mob within a 30-block cube (only when that mob cannot see the user or is &gt; 8 blocks away).
 */
public class ItemClosedThirdEye extends Item {

    public ItemClosedThirdEye(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0));//敵に気づかれなくなる
        }

        // プレイヤーを中心に30ブロックの立方体内の当たりを取得
        AABB area = player.getBoundingBox().inflate(30.0D, 30.0D, 30.0D);
        List<Mob> list = level.getEntitiesOfClass(Mob.class, area, e -> e != player);
        for (Mob living : list) {
            if (living.getTarget() == player) {
                // 間に壁があるか、ある程度の距離があれば有効
                if (!living.hasLineOfSight(player) || living.distanceTo(player) > 8.0D) {
                    living.setTarget(null);//使用者をターゲットから外す
                }
            }
        }
        return InteractionResultHolder.success(stack);
    }
}
