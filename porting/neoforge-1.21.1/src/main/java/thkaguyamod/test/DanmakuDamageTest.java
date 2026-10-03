package thkaguyamod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.item.EntityMarisaBroom;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;

/**
 * 实测回归测试：弹幕对生物造成伤害 / 扫帚可被玩家攻击破坏。
 *
 * <p>覆盖的回归：1.7.10 的 {@code canBeCollidedWith} 身兼「可碰撞・可被准星选取・可被弹丸命中」
 * 三职，1.21.1 拆为 {@code canBeCollidedWith}（推挤）/ {@code isPickable}（准星）/
 * {@code canBeHitByProjectile}（弹丸命中），且原版生物不再覆写 {@code canBeCollidedWith}
 * （恒 false）。移植只搬了第一职，导致：
 * <ul>
 *   <li>命中扫描过滤器 {@code canBeCollidedWith()} 把所有生物排除 → 弹幕/激光/符卡对怪零伤害；</li>
 *   <li>准星选取要求 {@code isPickable}（基类恒 false）→ 扫帚无法被攻击破坏、弹幕无法被打拳反弹。</li>
 * </ul>
 *
 * <h2>场景</h2>
 * <ol>
 *   <li>{@link #shotDamagesMob}：玩家弹直射 NoAi 僵尸 → 第 8 tick 弹必须已被打掉（未命中则会继续飞到
 *       屏障墙），第 40 tick 僵尸必须掉血或死亡；同时断言 {@code canBeHitByProjectile} 对生物为真（回归锚点）。</li>
 *   <li>{@link #broomBreakableByCreativePunch}：扫帚 {@code isPickable()} 为真，且创造模式
 *       玩家一击 {@code hurt} 后扫帚被移除（1.7.10 的クリエイティブ即壊し相当）。</li>
 * </ol>
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DanmakuDamageTest {

    private static final String TEMPLATE = "empty";

    /** 弹速（格/tick）。2.5 格距离约 5 tick 命中。 */
    private static final double SHOT_SPEED = 0.5D;

    /** 命中判定等待 tick 数（余量充足）。 */
    private static final int HIT_WAIT_TICKS = 40;

    /** 「弹已被僵尸打掉」的检查 tick（命中发生在第 5 tick 前后）。 */
    private static final int IMPACT_CHECK_TICKS = 8;

    private DanmakuDamageTest() {}

    @GameTest(template = TEMPLATE)
    public static void shotDamagesMob(GameTestHelper helper) {
        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 4, 2, 3);
        zombie.setNoAi(true);

        // 回归锚点：1.21.1 生物必须能被弹丸命中（旧过滤器 canBeCollidedWith 在此恒 false）
        helper.assertTrue(zombie.canBeHitByProjectile(),
                "zombie.canBeHitByProjectile() is false — hit-scan filters would skip all mobs");

        // 从僵尸正侧面 2.5 格、身体中线高度直射 +x
        Vec3 target = zombie.position().add(0.0D, zombie.getBbHeight() * 0.5D, 0.0D);
        Vec3 spawnPos = target.add(-2.5D, 0.0D, 0.0D);
        Vec3 direction = THShotLib.angle(1.0D, 0.0D, 0.0D);
        ShotData data = ShotData.shot(DanmakuConstants.FORM_SMALL, DanmakuConstants.RED, 0.3F, 2.0F, 0, 200);

        EntityTHShot shot = THShotLib.createShot(user, spawnPos, direction, SHOT_SPEED, data);
        helper.assertFalse(shot.isRemoved(), "shot was removed immediately after spawning");

        // 弹在约第 5 tick 命中僵尸；若命中扫描把僵尸过滤掉，弹会继续飞到 x=6 附近的屏障墙
        // （距发射点 5.5 格 ≈ 11 tick，endTime=200 远未到）。因此「第 8 tick 弹已消失」只能由
        // Entity 命中造成，这就是旧过滤器（canBeCollidedWith 对生物恒 false）必然失败的点。
        helper.runAtTickTime(IMPACT_CHECK_TICKS, () -> {
            helper.assertTrue(shot.isRemoved(),
                    "shot still in flight at tick " + IMPACT_CHECK_TICKS
                            + " — the hit scan never registered the zombie (shotPos=" + shot.position() + ")");
            helper.assertTrue(zombie.isRemoved() || zombie.getHealth() < zombie.getMaxHealth(),
                    "shot never damaged zombie (health=" + zombie.getHealth() + "/" + zombie.getMaxHealth() + ")");
        });

        helper.runAtTickTime(HIT_WAIT_TICKS, () -> {
            helper.assertTrue(zombie.isRemoved() || zombie.getHealth() < zombie.getMaxHealth(),
                    "zombie was never damaged (health=" + zombie.getHealth() + "/" + zombie.getMaxHealth() + ")");
            shot.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void broomBreakableByCreativePunch(GameTestHelper helper) {
        EntityMarisaBroom broom = helper.spawn(ModEntities.MARISA_BROOM.get(), 3, 2, 3);

        helper.assertTrue(broom.isPickable(),
                "broom.isPickable() is false — crosshair can never target it (cannot attack/remount)");

        Player user = helper.makeMockPlayer(GameType.CREATIVE);
        // GameTestHelper のモックプレイヤーは isCreative() を上書きするだけで Abilities.instabuild は
        // 既定 false のままなので、実プレイヤーと同じ創造状態に揃える（EntityMarisaBroom.hurt は
        // 1.7.10 の capabilities.isCreativeMode 相当として getAbilities().instabuild を見る）。
        user.getAbilities().instabuild = true;
        boolean accepted = broom.hurt(helper.getLevel().damageSources().playerAttack(user), 1.0F);
        helper.assertTrue(accepted, "broom.hurt() rejected creative player attack");
        helper.assertTrue(broom.isRemoved(), "creative punch should discard the broom");

        helper.succeed();
    }
}
