package thkaguyamod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 死神の大鎌 (Scythe of Death) — ported from 1.7.10 {@code thKaguyaMod.item.ItemDeathScythe}.
 *
 * <p>Right-click traces up to 16 m along the look vector and, for the nearest living entity hit,
 * either attracts it (default) or repels it (while sneaking) using a 0.6 impulse along the look
 * vector. Each use costs one durability.
 */
public class ItemDeathScythe extends SwordItem {

    public ItemDeathScythe(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 有効距離
        double range = 16.0D;
        Vec3 look = player.getViewVector(1.0F);
        Vec3 start = new Vec3(player.getX(), player.getEyeY(), player.getZ());
        Vec3 end = start.add(look.scale(range));

        // 始点と終点からブロックとの衝突を取得
        BlockHitResult blockHit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();// 終点を衝突した点に変更
        }

        AABB search = player.getBoundingBox()
                .expandTowards(look.scale(range))
                .inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, end, search,
                e -> e != player && e instanceof LivingEntity);

        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target && !level.isClientSide) {
            double power = 0.6D;
            if (player.isShiftKeyDown()) {// スニークなら斥力
                target.push(look.x * power, look.y * power, look.z * power);
            } else {// スニークじゃないなら引力
                target.push(-look.x * power, -look.y * power, -look.z * power);
            }
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);// 距離を操った生物の数だけ耐久が減る
        }
        return InteractionResultHolder.success(stack);
    }

    /**
     * エンチャン適性値 7。
     * TODO K3-review: 本来は Tier 側の値で管理すべきだが、既存の登録(Tier=IRON, 適性14)を変えずに
     *  原作の 7 を再現するためここで上書きしている。Tier に専用定義を追加するならこの上書きは削除可。
     */
    @Override
    public int getEnchantmentValue() {
        return 7;
    }
}
