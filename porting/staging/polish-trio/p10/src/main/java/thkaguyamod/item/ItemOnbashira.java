package thkaguyamod.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
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
 * <p>1.7.10 の {@code getDigSpeed} は恒 3.0F を返していた。1.21.1 にアイテム側の
 * 上書き点は無いため、{@link thkaguyamod.event.OnbashiraDigSpeedHandler}
 * （{@code PlayerEvent.BreakSpeed}）で置換する。詳細と代替案はそちらの javadoc を参照。
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