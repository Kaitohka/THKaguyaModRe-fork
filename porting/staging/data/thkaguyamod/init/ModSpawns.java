package thkaguyamod.init;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import thkaguyamod.config.ModConfig;

/**
 * 自然生成「放置规则」注册 —— 1.7.10 {@code EntityRegistry.addSpawn} 的 1.21.1 后半段。
 *
 * <p>分工：<b>权重 / 数量 / 群系</b> 由 {@code data/thkaguyamod/biome_modifier/*.json}
 * （{@code neoforge:add_spawns}）表达；<b>落点与生成条件</b>（旧 {@code getCanSpawnHere}）
 * 由本类的 {@link net.minecraft.world.entity.SpawnPlacements.SpawnPredicate} 表达。
 *
 * <p>本类同时承担 <b>config 开关</b>：{@code spawnDanmakuMob / spawnFairy / spawnPhantom /
 * spawnBoss / spawnHanabeeper} 是运行期配置，JSON 层无法表达，因此放进谓词里读
 * {@link ModConfig}（与 1.7.10 在 {@code registerEntitySpawn()} 里用 if 包住 addSpawn 等价）。
 * 谓词只在自然生成路径被调用（{@code NaturalSpawner} → {@code SpawnPlacements.checkSpawnRules}），
 * 刷怪笼 / 刷怪蛋不受影响，与原版行为一致。
 *
 * <h2>接线（主控在步骤 2 生物批次合并后执行）</h2>
 * <ol>
 *   <li>本文件落到 {@code thkaguyamod/init/ModSpawns.java}。</li>
 *   <li>{@code THKaguyaMod} 构造器里加 {@code ModSpawns.register(modEventBus);}。</li>
 *   <li>确认 {@link ModEntities} 里存在下列 {@code DeferredHolder} 字段（本类依赖的注册名契约）：
 *     <pre>
 * TH_FAIRY          -> thkaguyamod:th_fairy
 * SUNFLOWER_FAIRY   -> thkaguyamod:sunflower_fairy
 * TH_PHANTOM        -> thkaguyamod:th_phantom
 * DANMAKU_CREEPER   -> thkaguyamod:danmaku_creeper
 * CIRNO             -> thkaguyamod:cirno
 * RUMIA             -> thkaguyamod:rumia
 * TOZIKO            -> thkaguyamod:toziko
 * WRIGGLE           -> thkaguyamod:wriggle
 * SANAE             -> thkaguyamod:sanae
 * SAKUYA            -> thkaguyamod:sakuya
 * RINNOSUKE         -> thkaguyamod:rinnosuke
 *     </pre>
 *     字段名不符时改本文件的引用即可（JSON 只依赖注册名，不依赖字段名）。</li>
 *   <li>MobCategory 必须与 1.7.10 的 {@code EnumCreatureType} 对齐，否则生成时段/上限不同：
 *     妖精·幽灵·花火·Boss 系 = {@code MONSTER}；早苗 / 咲夜 / 霖之助 = {@code AMBIENT}
 *     （旧 {@code EnumCreatureType.ambient}）。</li>
 * </ol>
 *
 * <p><b>与生物批的分工</b>：旧 {@code getCanSpawnHere} 的等价实现写在下面的谓词里。
 * 若生物批已在实体 {@code checkSpawnRules} 里实现了同一套判断，两处会 AND 叠加——
 * 结果仍正确（更严格），但请只保留一处；建议保留本类（自然生成路径必经此处，
 * 且 config 门控集中在本类）。
 *
 * <p>已知偏差：旧 {@code super.getCanSpawnHere()} 含「无实体碰撞 + 不在液体中」两项，
 * 前者需要实体实例，{@code SpawnPredicate} 拿不到，用 ON_GROUND 放置类型 + 液体检查近似。
 */
public final class ModSpawns {

    private ModSpawns() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModSpawns::onRegisterSpawnPlacements);
    }

    private static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // 全部为新增条目，因此必须给出非空 placementType + heightmap（NeoForge 要求）。
        // 1.7.10 全部是地表生成（EntityCreature / EntityVillager 系），落点用 ON_GROUND。
        event.register(ModEntities.TH_FAIRY.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::fairySpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SUNFLOWER_FAIRY.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::fairySpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.TH_PHANTOM.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::phantomSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.DANMAKU_CREEPER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::creeperSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.CIRNO.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::cirnoSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.RUMIA.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::darkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.TOZIKO.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::tozikoSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.WRIGGLE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::wriggleSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SANAE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::rareVisitorSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SAKUYA.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::rareVisitorSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.RINNOSUKE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::rareVisitorSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /* ------------------------------------------------------------------ *
     * 谓词：1.7.10 getCanSpawnHere 的等价实现
     * ------------------------------------------------------------------ */

    /**
     * 妖精 / 花之妖精（EntityTHFairy 及子类 EntitySunFlowerFairy）。
     *
     * <p>1.7.10 {@code EntityTHFairy#getCanSpawnHere}：
     * <ol>
     *   <li>{@code rand.nextInt(100) < fairySpawnRate} → 拒绝（fairySpawnRate = 100 − 配置百分比）；</li>
     *   <li>{@code y < 64} 时再以 {@code y < rand.nextInt(64)} 概率拒绝（越低越难生成）；</li>
     *   <li>天空光 − 减光量 ≤ 7 → 拒绝（即要求地表白天）；</li>
     *   <li>脚下必须是草 / 土 / 沙。</li>
     * </ol>
     * EntitySunFlowerFairy 未覆写该方法，条件完全相同（仅权重/数量不同）。
     */
    private static <T extends Mob> boolean fairySpawnRules(EntityType<T> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!ModConfig.spawnDanmakuMob || !ModConfig.spawnFairy) {
            return false;
        }
        // 1.7.10: rand.nextInt(100) < fairySpawnRate → false（fairySpawnRate 越大越难生成）
        if (random.nextInt(100) < ModConfig.fairySpawnRate) {
            return false;
        }
        // 1.7.10: y < 64 → 以 y < rand.nextInt(64) 再筛一次
        if (pos.getY() < 64 && pos.getY() < random.nextInt(64)) {
            return false;
        }
        // 1.7.10: getSavedLightValue(Sky) - skylightSubtracted <= 7 → false
        if (level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken() <= 7) {
            return false;
        }
        // 1.7.10: 脚下 == grass || dirt || sand
        BlockState below = level.getBlockState(pos.below());
        return below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.SAND);
    }

    /**
     * 幽灵（EntityTHPhantom）：1.7.10 直接复用 {@code EntityMob} 的
     * {@code difficulty != PEACEFUL && isValidLightLevel()}。
     * 1.21.1 的对应物是 {@link Monster#checkMonsterSpawnRules}（亮度判断已现代化为
     * {@code isDarkEnoughToSpawn}）。
     */
    private static <T extends Mob> boolean phantomSpawnRules(EntityType<T> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!ModConfig.spawnDanmakuMob || !ModConfig.spawnPhantom) {
            return false;
        }
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    /** 花火（EntityDanmakuCreeper）继承 vanilla EntityCreeper 的生成条件，无自定义覆写。 */
    private static <T extends Mob> boolean creeperSpawnRules(EntityType<T> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!ModConfig.spawnDanmakuMob || !ModConfig.spawnHanabeeper) {
            return false;
        }
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    /**
     * 琪露诺（EntityCirno）：fairySpawnRate 门控 + 生成位置必须是雪层 +
     * 周围 ±2 格、y−1 处存在冰。
     */
    private static <T extends Mob> boolean cirnoSpawnRules(EntityType<T> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!ModConfig.spawnDanmakuMob || !ModConfig.spawnBoss) {
            return false;
        }
        if (random.nextInt(100) < ModConfig.fairySpawnRate) {
            return false;
        }
        if (!level.getBlockState(pos).is(Blocks.SNOW)) {
            return false;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (level.getBlockState(pos.offset(dx, -1, dz)).is(Blocks.ICE)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 露米娅（EntityRumia）：{@code difficulty != PEACEFUL && isValidLightLevel()}，
     * 与幽灵同源；受 spawnBoss 门控。
     */
    private static <T extends Mob> boolean darkMonsterSpawnRules(EntityType<T> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!ModConfig.spawnDanmakuMob || !ModConfig.spawnBoss) {
            return false;
        }
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    /** 屠自古（EntityToziko）：暗处生成 + 必须正在雷雨（{@code worldObj.isThundering()}）。 */
    private static <T extends Mob> boolean tozikoSpawnRules(EntityType<T> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!ModConfig.spawnDanmakuMob || !ModConfig.spawnBoss) {
            return false;
        }
        return level.getLevel().isThundering()
                && Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    /** 莉格露（EntityWriggle）：暗处生成 + 周围 ±2 格、y−1 处存在水。 */
    private static <T extends Mob> boolean wriggleSpawnRules(EntityType<T> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!ModConfig.spawnDanmakuMob || !ModConfig.spawnBoss) {
            return false;
        }
        if (!Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (level.getBlockState(pos.offset(dx, -1, dz)).getFluidState().is(FluidTags.WATER)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 早苗 / 咲夜 / 霖之助（EntitySanae / EntitySakuya / EntityRinnosuke）：
     * 1.7.10 是 {@code super.getCanSpawnHere()}（无实体碰撞、不在液体中）之后
     * {@code rand.nextInt(100) < 98 → false}，即约 2% 通过率；
     * 再要求脚下四邻方块不与正下方同质（避免生成在整片同种地面的正中）。
     *
     * <p>这三位在 1.7.10 用 {@code EnumCreatureType.ambient} 注册，MobCategory 必须是
     * AMBIENT 才能落在同一生成时段/上限里。1.7.10 的 ambient 生成本身带「仅夜晚」限制，
     * 1.21.1 由原版 {@code NaturalSpawner} 自行处理，故此处不再加亮度判断。
     */
    private static <T extends Mob> boolean rareVisitorSpawnRules(EntityType<T> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!ModConfig.spawnDanmakuMob || !ModConfig.spawnBoss) {
            return false;
        }
        // 1.7.10: rand.nextInt(100) < 98 → false
        if (random.nextInt(100) < 98) {
            return false;
        }
        // 1.7.10: super.getCanSpawnHere() 的「不在液体中」项（碰撞项无法在谓词里表达）
        if (!level.getBlockState(pos).getFluidState().isEmpty()
                || !level.getBlockState(pos.above()).getFluidState().isEmpty()) {
            return false;
        }
        // 1.7.10: 四邻脚下方块必须与正下方不同
        BlockState below = level.getBlockState(pos.below());
        if (level.getBlockState(pos.offset(-1, -1, 0)).equals(below)
                && level.getBlockState(pos.offset(1, -1, 0)).equals(below)
                && level.getBlockState(pos.offset(0, -1, -1)).equals(below)
                && level.getBlockState(pos.offset(0, -1, 1)).equals(below)) {
            return false;
        }
        return true;
    }
}
