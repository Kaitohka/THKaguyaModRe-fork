package thkaguyamod.test;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.config.ModConfig;
import thkaguyamod.entity.living.EntityDanmakuMob;
import thkaguyamod.init.ModEntities;
import thkaguyamod.registry.DanmakuPatternRegistry;

/**
 * 阶段 5 质量基建：妖精系（妖精 / ひまわり妖精 / 幽霊 / 使い魔）的 summon 冒烟测试。
 *
 * <p>覆盖的 P0 回归：1.7.10 在 {@code THKaguyaCore.preInit} 调用了
 * {@code CommonProxy.registerDanmakuPattern()} → {@code DanmakuPatternRegistry.setDanmakuPattern()}，
 * 移植版整条调用链丢失，导致 {@code DanmakuPatternRegistry.pattern} 恒为空表。
 * 而 {@link thkaguyamod.entity.living.EntityTHFairy#setPattern(int)} 直接取
 * {@code pattern.get(patternId)}，于是<b>构造函数</b>就抛
 * {@code IndexOutOfBoundsException} —— 妖精系实体既不能 {@code /summon} 也不能自然生成
 * （run-smoke 日志里 18 次 {@code Exception loading entity}）。
 *
 * <p>本测试走的是真实路径 {@link GameTestHelper#spawn(EntityType, int, int, int)} →
 * {@code EntityType.create(level)} → 实体构造函数 → {@code setPattern}，
 * 因此表为空时这里会直接抛异常（而不是靠断言才失败）。
 *
 * <h2>场景</h2>
 * <ol>
 *   <li>{@link #fairyTypesSpawnWithPattern}：四种妖精系实体逐个 spawn 不抛异常、
 *       生成后 {@code isAlive()}、{@code getDanmakuPattern()} 确为表里的某一项、
 *       {@code danmakuSpan} 已从表里读到（证明 {@code setPattern} 整条路径跑通）。</li>
 *   <li>{@link #solitaryFairyTypesSurviveTicks}：妖精／ひまわり妖精／幽霊 tick 20 次后仍存活
 *       （弹出即可不代表能活；「生成后 1 tick 就消失」也是一种失败模式）。日光会让幽霊自灭，
 *       所以先把时间调到夜里（13000 tick）。</li>
 *   <li>{@link #patternTableIsNotEmptyAtLevelZero}：danmakuLevel=0（配置下限）时样式表仍非空，
 *       妖精仍能生成（原作 default 兜底分支的回归锚点）。</li>
 * </ol>
 *
 * <p><b>为什么不把 使い魔 也塞进存活测试</b>：{@code EntityFamiliar.tick()} 里有一条
 * 「无骑乘者（{@code getShooter() == null}）就自行 {@code discard()}」的规则，
 * 1.7.10 原作 {@code EntityFamiliar.onUpdate} 同样如此（使い魔 必须骑在主人身上）。
 * 单独 spawn 的 使い魔 按设计就该消失，因此只断言它能生成、能拿到样式。
 *
 * <p>模板 {@code thkaguyamod:empty}（7×7×7 空结构）说明见 {@link RegistryCompletenessTest}。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DanmakuMobSpawnTest {

    private static final String TEMPLATE = "empty";

    /** 四种妖精系实体，全部经由 {@code EntityTHFairy} 构造函数读弹幕样式表。 */
    private static final List<EntityType<? extends EntityDanmakuMob>> FAIRY_TYPES = List.of(
            ModEntities.TH_FAIRY.get(),
            ModEntities.SUNFLOWER_FAIRY.get(),
            ModEntities.TH_PHANTOM.get(),
            ModEntities.FAMILIAR.get());

    /** 存活复检对象（不含 使い魔，理由见类注释）。 */
    private static final List<EntityType<? extends EntityDanmakuMob>> SOLITARY_FAIRY_TYPES = List.of(
            ModEntities.TH_FAIRY.get(),
            ModEntities.SUNFLOWER_FAIRY.get(),
            ModEntities.TH_PHANTOM.get());

    /** 存活复检的 tick 数（够跑完一轮攻击计数器/移动逻辑）。 */
    private static final int SURVIVAL_CHECK_TICKS = 20;

    private DanmakuMobSpawnTest() {}

    /**
     * danmakuLevel = 0（配置下限，范围 0~4）时样式表仍必须有内容。
     *
     * <p>原作 {@code DanmakuPatternRegistry.setDanmakuPattern()} 里 case 1~4 之后有
     * {@code default:} 兜底（注册 "000"/"001"/"002" 三条，span=999999、speed=0），
     * 因此 level=0 落到 default、表非空，{@code EntityTHFairy.setPattern} 不会越界。
     * 移植保留了这段 default，本测试把这个边界钉住：将来若有人把 default 删掉改成
     * 「level=0 就不注册」，妖精（level=0 仍会自然生成，原作 getCanSpawnHere 不看 danmakuLevel）
     * 会立刻 IndexOutOfBoundsException。
     *
     * <p>level=0 的「不出弹」由 {@code EntityDanmakuMob} 里的 danmakuLevel 判定负责
     * （tick 里清空 target、攻击分支要求 level != 0），与样式表无关。
     */
    @GameTest(template = TEMPLATE)
    public static void patternTableIsNotEmptyAtLevelZero(GameTestHelper helper) {
        int savedLevel = ModConfig.danmakuLevel;
        try {
            ModConfig.danmakuLevel = 0;
            DanmakuPatternRegistry.setDanmakuPattern();

            helper.assertTrue(DanmakuPatternRegistry.pattern.size() > 0,
                    "danmakuLevel=0 produced an empty pattern table — the switch has no default fallback");

            // 走真实构造路径：表为空这里就会 IndexOutOfBoundsException
            EntityDanmakuMob fairy = helper.spawn(ModEntities.TH_FAIRY.get(), 3, 2, 3);
            helper.assertTrue(fairy.isAlive(),
                    "th_fairy did not survive spawning at danmakuLevel=0");
        } finally {
            ModConfig.danmakuLevel = savedLevel;
            DanmakuPatternRegistry.setDanmakuPattern();
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fairyTypesSpawnWithPattern(GameTestHelper helper) {
        helper.assertTrue(DanmakuPatternRegistry.pattern.size() > 0,
                "DanmakuPatternRegistry.pattern is empty — setDanmakuPattern() was never called");

        for (int i = 0; i < FAIRY_TYPES.size(); i++) {
            EntityType<? extends EntityDanmakuMob> type = FAIRY_TYPES.get(i);
            // 错开位置避免互相推挤；y=2 即离地 2 格
            EntityDanmakuMob mob = helper.spawn(type, 1 + i * 2, 2, 3);

            helper.assertTrue(mob.isAlive(), type.toShortString() + " was not alive right after spawning");
            helper.assertTrue(
                    DanmakuPatternRegistry.danmaku.containsValue(mob.getDanmakuPattern()),
                    type.toShortString() + " carried danmaku pattern " + mob.getDanmakuPattern()
                            + ", which is not in DanmakuPatternRegistry.danmaku="
                            + DanmakuPatternRegistry.danmaku.values());
            helper.assertTrue(mob.danmakuSpan > 0,
                    type.toShortString() + " has danmakuSpan=" + mob.danmakuSpan
                            + " — setPattern() did not read the span map");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void solitaryFairyTypesSurviveTicks(GameTestHelper helper) {
        // 幽霊（EntityTHPhantom）は日光で自滅する（原作 EntityTHPhantom.onUpdate の
        // getBrightness > 0.5 判定）。空構造の真上は遮蔽物が無いので、昼のままだと
        // 必ず消える。夜へ落として「生存」という本題だけを見る。
        helper.setNight();

        List<EntityDanmakuMob> spawned = new ArrayList<>();
        for (int i = 0; i < SOLITARY_FAIRY_TYPES.size(); i++) {
            spawned.add(helper.spawn(SOLITARY_FAIRY_TYPES.get(i), 1 + i * 2, 2, 3));
        }

        helper.runAtTickTime(SURVIVAL_CHECK_TICKS, () -> {
            for (EntityDanmakuMob mob : spawned) {
                helper.assertTrue(mob.isAlive(),
                        mob.getType().toShortString() + " died within " + SURVIVAL_CHECK_TICKS
                                + " ticks (health=" + mob.getHealth() + ")");
            }
            helper.succeed();
        });
    }
}