package thkaguyamod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.config.ModConfig;

/**
 * 壁抜けの鑿 (Wall-Passing Chisel) — ported from 1.7.10 {@code thKaguyaMod.item.ItemKabenuke}.
 *
 * <p>Right-clicking a wall face probes up to 16 blocks along the face normal (i.e. into the wall)
 * looking for a 2-block-tall air pocket; on success the player is teleported there and one
 * durability is consumed. Bedrock always blocks the attempt. Disabled by
 * {@link ModConfig#canUseWallPassingChisel}.
 */
public class ItemKabenuke extends Item {

    public ItemKabenuke(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // コンフィグで使用制限をかけているなら使えない
        if (!ModConfig.canUseWallPassingChisel) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (!(player instanceof ServerPlayer playerMP) || level.isClientSide) {
            return InteractionResult.PASS;
        }

        // 面の法線の逆方向（壁の中へ）へ進む。1.7.10 の surface 0..5 スイッチ相当。
        Direction face = context.getClickedFace();
        int px = -face.getStepX();
        int py = -face.getStepY();
        int pz = -face.getStepZ();

        EquipmentSlot slot = LivingEntity.getSlotForHand(context.getHand());
        BlockPos pos = context.getClickedPos();
        for (int i = 0; i < 16; i++) {// 最大16ブロック抜けられる
            if (level.isEmptyBlock(pos)) {
                // 基準より上も空気ブロックかどうか？
                if (level.isEmptyBlock(pos.above())) {
                    teleport(level, playerMP, stack, slot, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                    return InteractionResult.PASS;
                }
                // 基準より下も空気ブロックかどうか？
                else if (level.isEmptyBlock(pos.below())) {
                    teleport(level, playerMP, stack, slot, pos.getX() + 0.5D, pos.getY() - 1.0D, pos.getZ() + 0.5D);
                    return InteractionResult.PASS;
                }
            }
            // 岩盤があったら失敗する 岩盤は壁抜け不可
            if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
                return InteractionResult.PASS;
            }
            pos = pos.offset(px, py, pz);
        }
        // TODO K3-review: 原作はテレポート後も含めて常に false(=PASS) を返していたため、その挙動を維持。
        //  ブロック側の use に流れるのを止めたい場合は SUCCESS を返すこと。
        return InteractionResult.PASS;
    }

    private void teleport(Level level, ServerPlayer player, ItemStack stack, EquipmentSlot slot,
                          double x, double y, double z) {
        // 1.7.10 の "random.fizz"。1.21.1 では lava extinguish を近似として使用（ピッチは原作 1.0F）。
        level.playSound(null, x, y, z, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
        stack.hurtAndBreak(1, player, slot);
    }
}
