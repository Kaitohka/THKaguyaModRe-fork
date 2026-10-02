package thkaguyamod.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

/**
 * 御柱 (Onbashira) — ported from 1.7.10 {@code thKaguyaMod.item.ItemOnbashira}.
 *
 * <p>While carried, applies a 0.9× damping to horizontal motion (and to upward motion); holding
 * several stacks compounds the slowdown.
 *
 * <p>TODO K3-review: the 1.7.10 class also overrode {@code getDigSpeed} to always return 3.0F
 * (a deliberate nerf for a "heavy" weapon). 1.21.1 has no per-block dig-speed override on the
 * item; this needs a mining-speed attribute/modifier or an event hook — not implemented here.
 */
public class ItemOnbashira extends SwordItem {

    public ItemOnbashira(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // 移動速度が0.9倍になる。持てば持つほど移動速度が落ちる
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.9, 1.0, 0.9));
        if (entity.getDeltaMovement().y > 0.0) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.9, 1.0));
        }
        // TODO K3-review: 1.7.10 の setDeltaMovement は motion を直接書き換えていたため、
        //  クライアントへの同期のため ServerPlayer では connection 経由の補正が必要な場合がある。
    }
}
