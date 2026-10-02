package thkaguyamod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import thkaguyamod.init.ModItems;

/**
 * 舟幽霊の柄杓 (Ship Ghost's Dipper) — ported from 1.7.10 {@code thKaguyaMod.item.ItemHisyaku}.
 *
 * <p>1.7.10 had one item class with two metadata-less instances
 * ({@code ship_ghost_dipper_fill} = 設置モード, {@code ship_ghost_dipper_empty} = 除去モード);
 * 1.21.1 registers both as separate items ({@code SHIP_GHOSTS_DIPPER} /
 * {@code SHIP_GHOSTS_DIPPER_EMPTY}), and the mode is this constructor's {@code mode} argument.
 *
 * <ul>
 *   <li>設置モード (fill): places a water source at the targeted face; without a hit, at the block
 *       under the player's eyes (original formula {@code posY - yOffset + 0.5}). Costs 2 durability.
 *       In the Nether the water evaporates instead: fizz sound + 8 large smoke particles, no
 *       durability.</li>
 *   <li>除去モード (empty): removes the targeted water source at no durability cost (infinite).</li>
 * </ul>
 *
 * <p>Sneak + right-click switches between the two items, carrying the damage value over (the
 * original used {@code new ItemStack(THKaguyaItems.ship_ghost_dipper_*)} — same mechanism, the
 * returned stack replaces the held one).
 *
 * <p>TODO K3-review: registration in {@code ModItems} currently uses the plain {@code Item::new}
 * factory for both dippers. It has to become
 * {@code props -> new ItemHisyaku(props, ItemHisyaku.EMPTY)} /
 * {@code props -> new ItemHisyaku(props, ItemHisyaku.FILL)} for the behaviour to exist.
 *
 * <p>TODO K3-review: the original classified the target with
 * {@code Blocks.water.isBlockSolid(...)}; that method always answered {@code false} for water, so
 * the guard reduced to "not a solid block". Reproduced here as
 * {@code level.isEmptyBlock(pos) || !state.isSolid() || state.is(Blocks.WATER)}.
 *
 * <p>TODO K3-review: 1.7.10 の {@code player.yOffset} はプレイヤーでは 0 だったため、
 * 座標計算は {@code player.getEyeY()} で置き換えている（結果は同じ）。
 */
public class ItemHisyaku extends Item {

    /** 設置モード（旧 {@code ship_ghost_dipper_fill}、subID 0）。 */
    public static final int FILL = 0;
    /** 除去モード（旧 {@code ship_ghost_dipper_empty}、subID 1）。 */
    public static final int EMPTY = 1;

    private final int mode;

    /**
     * @param properties アイテムプロパティ
     * @param mode       {@link #FILL} または {@link #EMPTY}
     */
    public ItemHisyaku(Item.Properties properties, int mode) {
        super(properties);
        this.mode = mode;
    }

    /** 設置モードかどうか（{@link #EMPTY} なら除去モード）。 */
    public boolean isFillMode() {
        return this.mode != EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // モード切り替えでアイテムを切り替える処理
        if (player.isShiftKeyDown()) {
            ItemStack switchedStack = isFillMode()
                    ? new ItemStack(ModItems.SHIP_GHOSTS_DIPPER_EMPTY.get(), 1)
                    : new ItemStack(ModItems.SHIP_GHOSTS_DIPPER.get(), 1);
            // 耐久値を引き継ぐ（1.7.10 の new ItemStack(item, 1, itemStack.getItemDamage())）
            switchedStack.setDamageValue(stack.getDamageValue());
            return InteractionResultHolder.success(switchedStack);
        }

        // 除去モードは液体（水源）を対象に、設置モードは液体を無視してブロックを対象にする
        ClipContext.Fluid fluidMode = isFillMode() ? ClipContext.Fluid.NONE : ClipContext.Fluid.SOURCE_ONLY;
        BlockHitResult hit = getPlayerPOVHitResult(level, player, fluidMode);

        if (!isFillMode()) {
            // 水源を消す。耐久値は消費しない（無限に使える）
            if (hit.getType() != HitResult.Type.MISS) {
                BlockPos pos = hit.getBlockPos();
                if (level.getBlockState(pos).is(Blocks.WATER)
                        && level.getFluidState(pos).isSource()) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
            return InteractionResultHolder.success(stack);
        }

        // 設置モード：設置座標を決める
        int i;
        int j;
        int k;
        if (hit.getType() == HitResult.Type.MISS) {
            // 何もないところで使うと、目の高さのブロックが基準になる（旧 posY + eyeHeight - yOffset）
            // 1.21.1 に yOffset は無く、プレイヤーでは常に 0 だったため getEyeY() で足りる
            i = Mth.floor(player.getX() - 0.5D);
            j = Mth.floor(player.getEyeY() + 0.5D);
            k = Mth.floor(player.getZ() - 0.5D);
        } else {
            BlockPos hitPos = hit.getBlockPos();
            i = hitPos.getX();
            j = hitPos.getY();
            k = hitPos.getZ();

            // 1.7.10 の sideHit 0..5 は 1.21.1 の Direction.get3DDataValue() と同じ並び
            int[] offset = offsetForDirection(hit.getDirection().get3DDataValue());
            i += offset[0];
            j += offset[1];
            k += offset[2];
        }
        BlockPos targetPos = new BlockPos(i, j, k);
        BlockState state = level.getBlockState(targetPos);

        // 固体ブロックでなければ水を設置できる
        if (level.isEmptyBlock(targetPos) || !state.isSolid() || state.is(Blocks.WATER)) {
            if (level.dimension() == Level.NETHER) {
                // 地獄では水が蒸発する。1.7.10 の "random.fizz" は 1.21.1 では lava extinguish で近似
                level.playSound(null, i + 0.5D, j + 0.5D, k + 0.5D,
                        SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS,
                        0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);

                for (int l = 0; l < 8; l++) {
                    level.addParticle(ParticleTypes.LARGE_SMOKE,
                            i + level.random.nextDouble(),
                            j + level.random.nextDouble(),
                            k + level.random.nextDouble(),
                            0.0D, 0.0D, 0.0D);
                }
            } else {
                // 水源を設置する（1.7.10 の setBlock(x, y, z, Blocks.water) は meta 0 = 水源）
                level.setBlockAndUpdate(targetPos, Blocks.WATER.defaultBlockState());
                stack.hurtAndBreak(2, player, EquipmentSlot.MAINHAND);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    /** 1.7.10 の sideHit ごとの座標オフセット（{x, y, z}）。 */
    private static int[] offsetForDirection(int sideHit) {
        return switch (sideHit) {
            case 0 -> new int[] {0, -1, 0};// 下
            case 1 -> new int[] {0, 1, 0};// 上
            case 2 -> new int[] {0, 0, -1};// 北
            case 3 -> new int[] {0, 0, 1};// 南
            case 4 -> new int[] {-1, 0, 0};// 西
            case 5 -> new int[] {1, 0, 0};// 東
            default -> new int[] {0, 0, 0};
        };
    }
}