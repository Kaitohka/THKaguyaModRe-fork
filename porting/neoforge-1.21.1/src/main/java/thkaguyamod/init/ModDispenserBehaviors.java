package thkaguyamod.init;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.DanmakuParams;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.item.EntitySilverKnife;
import thkaguyamod.item.ItemTHShot;

/**
 * 发射器行为（1.7.10 {@code CommonProxy.registerDispenser()} の移植）。
 *
 * <p>1.7.10 は {@code BlockDispenser.dispenseBehaviorRegistry.putObject(...)} に
 * {@code IBehaviorDispenseItem} のラムダを登録していた。1.21.1 では
 * {@link DispenserBlock#registerBehavior} に {@link net.minecraft.core.dispenser.DispenseItemBehavior} を登録する。
 *
 * <ul>
 *   <li>銀のナイフ（4 色）: {@link EntitySilverKnife} をディスペンサーの向きへ速度 0.58D で発射し、1 個消費する。</li>
 *   <li>弾物品（19 種）: {@link ItemTHShot#shootDanmaku} で弾幕を発射する。旧 NBT {@code "Infinity"} は
 *       {@link ModDataComponents#DANMAKU} の {@code infinity} に置き換わっており、
 *       無限フラグが立っていれば消費しない。</li>
 * </ul>
 *
 * <p>登録のタイミングは {@code FMLCommonSetupEvent}（登録側は {@code THKaguyaMod} のコンストラクタから
 * {@code modEventBus.addListener(FMLCommonSetupEvent.class, e -> e.enqueueWork(ModDispenserBehaviors::register));}）。
 *
 * <p>TODO K3-review: {@link EntitySilverKnife} は並行移植中のため、このファイルは
 * {@code EntitySilverKnife(EntityType, Level, LivingEntity, Vec3 pos, Vec3 angle, double speed, int color)}
 * という想定シグネチャで書いている。実際の移植版と食い違う場合はここを合わせること。
 * あわせて {@code ModEntities} に {@code SILVER_KNIFE} の登録が必要（現在は未登録）。
 */
public final class ModDispenserBehaviors {

    /** 銀のナイフの色（1.7.10 {@code EntitySilverKnife.KNIFE_COLOR_*}）。 */
    private static final int KNIFE_COLOR_BLUE = 0;
    private static final int KNIFE_COLOR_RED = 1;
    private static final int KNIFE_COLOR_GREEN = 2;
    private static final int KNIFE_COLOR_WHITE = 3;

    private ModDispenserBehaviors() {}

    public static void register() {
        DispenserBlock.registerBehavior(ModItems.SILVER_KNIFE_BLUE.get(), new KnifeDispenseBehavior(KNIFE_COLOR_BLUE));
        DispenserBlock.registerBehavior(ModItems.SILVER_KNIFE_RED.get(), new KnifeDispenseBehavior(KNIFE_COLOR_RED));
        DispenserBlock.registerBehavior(ModItems.SILVER_KNIFE_GREEN.get(), new KnifeDispenseBehavior(KNIFE_COLOR_GREEN));
        DispenserBlock.registerBehavior(ModItems.SILVER_KNIFE_WHITE.get(), new KnifeDispenseBehavior(KNIFE_COLOR_WHITE));

        for (var shotItem : new ItemTHShot[] {
                ModItems.SMALL_SHOT.get(), ModItems.TINY_SHOT.get(), ModItems.MEDIUM_SHOT.get(), ModItems.BIG_SHOT.get(),
                ModItems.STAR_SHOT.get(), ModItems.SMALL_STAR_SHOT.get(), ModItems.CIRCLE_SHOT.get(), ModItems.SCALE_SHOT.get(),
                ModItems.BUTTERFLY_SHOT.get(), ModItems.LIGHT_SHOT.get(), ModItems.KNIFE_SHOT.get(), ModItems.HEART_SHOT.get(),
                ModItems.KUNAI_SHOT.get(), ModItems.TALISMAN_SHOT.get(), ModItems.BIG_LIGHT_SHOT.get(), ModItems.RICE_SHOT.get(),
                ModItems.OVAL_SHOT.get(), ModItems.CRYSTAL_SHOT.get(), ModItems.ARROW_SHOT.get()}) {
            DispenserBlock.registerBehavior(shotItem, new ShotDispenseBehavior());
        }
    }

    /** 銀のナイフを発射する（旧 {@code silver_knife} のラムダ）。 */
    private static final class KnifeDispenseBehavior extends DefaultDispenseItemBehavior {

        private final int color;

        private KnifeDispenseBehavior(int color) {
            this.color = color;
        }

        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack) {
            ServerLevel level = source.level();
            Direction direction = source.state().getValue(DispenserBlock.FACING);

            // 旧: 射出座標はディスペンサー中心、向きは (射出口 - 中心) を正規化したもの
            Vec3 pos = Vec3.atCenterOf(source.pos());
            Vec3 angle = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());

            // TODO K3-review: EntitySilverKnife は並行移植中。想定シグネチャで生成している。
            EntitySilverKnife knife = new EntitySilverKnife(ModEntities.SILVER_KNIFE.get(), level, null, pos, angle, 0.58D, this.color);
            level.addFreshEntity(knife);// ナイフを出す

            stack.shrink(1);// アイテムをひとつ減らす
            return stack;
        }
    }

    /** 弾物品から弾幕を発射する（旧 {@code shot_item} のラムダ）。 */
    private static final class ShotDispenseBehavior extends DefaultDispenseItemBehavior {

        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack) {
            ServerLevel level = source.level();
            Direction direction = source.state().getValue(DispenserBlock.FACING);

            // 旧: 射出座標はディスペンサー中心（弾は shootDanmaku 側で distance 分だけ前に出る）
            Vec3 pos = Vec3.atCenterOf(source.pos());
            Vec3 angle = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());

            if (stack.getItem() instanceof ItemTHShot shotItem) {
                LivingEntity shooter = createShooter(level, source, direction);
                ItemTHShot.shootDanmaku(stack, shooter, false, pos, angle,
                        THShotLib.SIZE[ItemTHShot.shotTypeTrans[shotItem.getItemTypeIndex()]] + 1.0D);
            }

            // 旧 NBT "Infinity" → DataComponent DANMAKU.infinity()
            DanmakuParams params = stack.getOrDefault(ModDataComponents.DANMAKU.get(), DanmakuParams.DEFAULT);
            if (!params.infinity()) {
                stack.shrink(1);
            }
            return stack;
        }

        /**
         * 弾の発射主を作る。1.7.10 は {@code EntityFamiliar}（妖精）を新規生成していたが、
         * 1.21.1 にはまだ移植されていない。
         *
         * <p>弾は「発射主がいない／死んでいると消滅する」ため発射主は必須だが、
         * ワールドに追加すると発射主がそのまま残り続ける（アーマースタンドは自然消滅しない）。
         * そこで <b>ワールドに追加しない</b> {@link ArmorStand} を発射主として使う
         * （{@code isAlive()} は true のままなので弾は通常どおり寿命で消滅し、参照が切れれば GC される）。
         *
         * <p>TODO K3-review: {@code EntityFamiliar}/{@code EntityTHFairy} の移植後は、
         * ディスペンサー位置に妖精を出現させる元の挙動へ差し替えること。
         */
        private static LivingEntity createShooter(ServerLevel level, BlockSource source, Direction direction) {
            BlockPos frontPos = source.pos().relative(direction);
            ArmorStand shooter = new ArmorStand(level,
                    frontPos.getX() + 0.5D, frontPos.getY() + 0.5D, frontPos.getZ() + 0.5D);
            shooter.setYRot(direction.toYRot());
            shooter.setXRot(direction == Direction.UP ? -90.0F : direction == Direction.DOWN ? 90.0F : 0.0F);
            return shooter;
        }
    }
}