package thkaguyamod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import thkaguyamod.config.ModConfig;
import thkaguyamod.entity.item.EntitySukima;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModEntities;
import thkaguyamod.init.ModItems;

/**
 * 隙間の折りたたみ傘 — ported from 1.7.10 {@code thKaguyaMod.item.ItemGapFoldingUmbrella}.
 *
 * <p>右クリックで見ている位置に色付きスキマを開き（色は {@code 32 + GapColor}）、耐久を 1 消費する。
 * スニーク右クリックで色（0..15）を切り替える。耐久が尽きると「隙間」({@link ModItems#GAP}) に戻る
 * （そのとき旧 damage を {@link ModDataComponents#ORIGINAL_DAMAGE} から復元し、
 * {@link ModDataComponents#GAP_COLOR} と ORIGINAL_DAMAGE を除去する）。
 *
 * <p>移植しないもの: {@code requiresMultipleRenderPasses}/{@code getIconFromDamage}/
 * {@code registerIcons}/{@code getIconFromDamageForRenderPass}/{@code getColorFromItemStack}
 * （2 層アイコン＋染料による色替え。1.21.1 ではモデル layer + {@code ItemColor} で表現する必要がある）。
 *
 * <p>TODO K3-review: 1.7.10 は {@code onItemRightClick} の戻り値として「別アイテムの ItemStack」を返し、
 * バニラ側がそれをインベントリへ書き戻すことで傘→隙間の置き換えが成立していた。
 * 1.21.1 の {@code use} には同等の書き戻し機構が無いため、本移植では
 * {@code player.setItemInHand(hand, newStack)} で明示的に差し替える。
 *
 * <p>TODO K3-review: 1.7.10 の {@code isBlockNormalCubeDefault(x, y, z, true)} は
 * {@code BlockState#isSolidRender} で近似している（{@link ItemSukima} と同じ）。
 */
public class ItemGapFoldingUmbrella extends Item {

    /** 色名（旧 getColorFromItemStack の添字。1.21.1 では未使用）。 */
    private final String colorName[] = {"black", "red", "green", "brown", "blue", "purple", "cyan", "lightGray",
            "gray", "pink", "lime", "yellow", "lightBlue", "magenta", "orange", "white"};

    public ItemGapFoldingUmbrella(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        double setX = player.getX() - Math.sin(player.getYRot() / 180F * (float) Math.PI) * Math.cos(player.getXRot() / 180F * (float) Math.PI) * 2.0D;
        double setY = player.getY() - Math.sin(player.getXRot() / 180F * (float) Math.PI) * 2.0D + player.getEyeHeight() / 2.0D;
        double setZ = player.getZ() + Math.cos(player.getYRot() / 180F * (float) Math.PI) * Math.cos(player.getXRot() / 180F * (float) Math.PI) * 2.0D;
        int color;

        if (player.isShiftKeyDown()) {
            color = stack.getOrDefault(ModDataComponents.GAP_COLOR.get(), 0);
            color++;
            if (color >= 16) {
                color = 0;
            }
            stack.set(ModDataComponents.GAP_COLOR.get(), color);
            return InteractionResultHolder.success(stack);
        }

        // 1.7.10 の isBlockNormalCubeDefault((int)setX, (int)setY, (int)setZ, true) を反転した条件
        BlockPos frontPos = BlockPos.containing(setX, setY, setZ);
        BlockState frontState = level.getBlockState(frontPos);
        if (!frontState.isSolidRender(level, frontPos)) {
            // 1.7.10 の "mob.endermen.portal"
            if (ModConfig.useDefaultGapSE) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                        0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));
            } else {
                // TODO K3-review: 原作は ModConfig.sukimaWarpSE（ユーザー指定音效名）を鳴らしていたが、
                //   1.21.1 の音效は登録制のため任意名を再生できない。既定音で代替する。
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            color = stack.getOrDefault(ModDataComponents.GAP_COLOR.get(), 0);
            if (color >= 16) {
                color = 0;
            }

            EntitySukima entitySukima = new EntitySukima(ModEntities.SUKIMA.get(), level, player, setX, setY, setZ, 32 + color);
            if (!level.isClientSide) {
                level.addFreshEntity(entitySukima);//スキマを生み出す
            }

            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

            if (stack.getDamageValue() >= stack.getMaxDamage()) {
                int originalDamage = stack.getOrDefault(ModDataComponents.ORIGINAL_DAMAGE.get(), 0);

                ItemStack gap = new ItemStack(ModItems.GAP.get());
                gap.setDamageValue(originalDamage);
                // 1.7.10 の nbt.removeTag("GapColor") / removeTag("OriginalDamage") に相当
                // （新規スタックに載せ替えるため、旧スタックのコンポーネントは引き継がれない）
                player.setItemInHand(hand, gap);
                return InteractionResultHolder.success(gap);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    /** アイテムを発光させる（1.7.10 の {@code hasEffect} 相当）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}