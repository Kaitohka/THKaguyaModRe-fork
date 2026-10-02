package thkaguyamod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 第三の眼 (Third Eye) — ported from 1.7.10 {@code thKaguyaMod.item.ItemThirdEye}.
 *
 * <p>Right-click traces up to 64 m along the look vector, latches onto the nearest living entity,
 * and then reports its name, HP and (for tamed animals) owner in chat on the next tick.
 * Durability regenerates 1 point per tick.
 *
 * <p>Note: like the original, players only get a name readout (HP is not shown for players).
 */
public class ItemThirdEye extends Item {

    private LivingEntity hitEntity = null;
    private boolean check = false;

    public ItemThirdEye(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        double range = 64.0D;
        Vec3 look = player.getViewVector(1.0F);
        Vec3 start = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        Vec3 end = start.add(look.x * range, look.y * range, look.z * range);

        // ブロックとの衝突を取得（1.7.10 の func_147447_a 相当）
        BlockHitResult blockHit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();// 終点を衝突した点に変更
        }

        // 線分に沿って最近の生物を取得
        AABB search = player.getBoundingBox()
                .expandTowards(look.x * range, look.y * range, look.z * range)
                .inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, end, search,
                e -> e != player && e instanceof LivingEntity);

        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living) {
            check = true;
            hitEntity = living;
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // 耐久が減っていたら徐々に回復（毎tick 1）
        if (stack.isDamaged()) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
        }
        if (check && hitEntity != null) {
            if (entity instanceof Player player && !level.isClientSide && hitEntity.isAlive()) {
                LivingEntity living = hitEntity;
                float hp = Math.round((living.getHealth() / 2F) * 100F) / 100F;
                String strHP = " HP ： " + hp + "/" + (living.getMaxHealth() / 2F);

                String strEntityName = living instanceof Player other
                        ? other.getDisplayName().getString()
                        : living.getName().getString();
                player.sendSystemMessage(Component.literal(strEntityName));//名前を表示
                player.sendSystemMessage(Component.literal(strHP));//体力を表示

                if (living instanceof TamableAnimal tame && tame.isTame() && tame.getOwner() != null) {
                    String strOW = "  Owner : " + tame.getOwner().getName().getString();
                    player.sendSystemMessage(Component.literal(strOW));//オーナー名を表示
                }
            }
            check = false;
            hitEntity = null;
        }
    }
}
