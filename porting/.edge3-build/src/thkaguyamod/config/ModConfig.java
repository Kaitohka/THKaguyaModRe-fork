package thkaguyamod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 1.7.10 {@code THKaguyaConfig}（Forge Configuration, .cfg）→ NeoForge {@link ModConfigSpec}（TOML）。
 *
 * <p>Dropped on purpose: the EntityID category (modern entity ids are namespaced and
 * automatic) and the hardcoded sukima.ogg file probing (sounds are registered properly now).
 * Field names and derived-value semantics (fairySpawnRate = 100 - rate, volumes /100F,
 * danmakuAccuracy = 360 * (1 - hitRate/100)) are kept so downstream ports read naturally.
 */
public final class ModConfig {

    // ---- Limit ----
    public static boolean UsingMasterSpark = true;
    public static boolean MasterSparkDestroysBlocks = true;
    public static boolean canUseWallPassingChisel = true;
    public static int shotMaxNumber = 32;
    public static int laserMaxNumber = 8;

    // ---- Effect ----
    public static boolean useTimeStopEffect = true;
    public static boolean useDefaultGapSE = true;

    // ---- SE vol (stored 0..500, exposed as 0..5 float) ----
    public static float SpellCardVol = 0.5F;
    public static float MasterSparkVol = 0.5F;
    public static float HisoutenVol = 0.2F;
    public static float DownVol = 0.5F;

    // ---- Danmaku ----
    public static boolean danmakuOneKillMode = false;
    public static boolean spawnDanmakuMob = true;
    public static boolean spawnFairy = true;
    public static boolean spawnPhantom = true;
    public static boolean spawnBoss = true;
    public static int fairySpawnRate = 50; // 100 - configured rate
    public static int danmakuLevel = 2;
    public static float danmakuAccuracy = 360F * (1F - 0.8F);

    // ---- Other ----
    public static boolean spawnHanabeeper = false;
    public static boolean recipelessItemsRecipe = false;

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue CFG_USE_MASTER_SPARK;
    private static final ModConfigSpec.BooleanValue CFG_MASTER_SPARK_DESTROYS_BLOCKS;
    private static final ModConfigSpec.BooleanValue CFG_WALL_PASSING_CHISEL;
    private static final ModConfigSpec.IntValue CFG_SHOT_MAX_NUMBER;
    private static final ModConfigSpec.IntValue CFG_LASER_MAX_NUMBER;

    private static final ModConfigSpec.BooleanValue CFG_TIME_STOP_EFFECT;
    private static final ModConfigSpec.BooleanValue CFG_DEFAULT_GAP_SE;

    private static final ModConfigSpec.IntValue CFG_SPELL_CARD_VOL;
    private static final ModConfigSpec.IntValue CFG_MASTER_SPARK_VOL;
    private static final ModConfigSpec.IntValue CFG_HISOUTEN_VOL;
    private static final ModConfigSpec.IntValue CFG_DOWN_VOL;

    private static final ModConfigSpec.BooleanValue CFG_ONE_KILL;
    private static final ModConfigSpec.BooleanValue CFG_SPAWN_DANMAKU_MOB;
    private static final ModConfigSpec.BooleanValue CFG_SPAWN_FAIRY;
    private static final ModConfigSpec.BooleanValue CFG_SPAWN_PHANTOM;
    private static final ModConfigSpec.BooleanValue CFG_SPAWN_BOSS;
    private static final ModConfigSpec.IntValue CFG_FAIRY_SPAWN_RATE;
    private static final ModConfigSpec.IntValue CFG_DANMAKU_LEVEL;
    private static final ModConfigSpec.IntValue CFG_HIT_RATE;

    private static final ModConfigSpec.BooleanValue CFG_SPAWN_HANABEEPER;

    static {
        B.push("Limit");
        CFG_USE_MASTER_SPARK = B.define("Use Master Spark", true);
        CFG_MASTER_SPARK_DESTROYS_BLOCKS = B.define("Master Spark Destroys Blocks", true);
        CFG_WALL_PASSING_CHISEL = B.define("Can Use Wall-Passing Chisel", true);
        CFG_SHOT_MAX_NUMBER = B.defineInRange("Shot Max Number", 32, 1, 512);
        CFG_LASER_MAX_NUMBER = B.defineInRange("Laser Max Number", 8, 1, 128);
        B.pop();

        B.push("Effect");
        CFG_TIME_STOP_EFFECT = B.define("Appear Time stop effect", true);
        CFG_DEFAULT_GAP_SE = B.define("Use default Gap(Sukima)SE", true);
        B.pop();

        B.push("SE vol");
        CFG_SPELL_CARD_VOL = B.defineInRange("Spell Card Vol", 50, 0, 500);
        CFG_MASTER_SPARK_VOL = B.defineInRange("Master Spark Vol", 50, 0, 500);
        CFG_HISOUTEN_VOL = B.defineInRange("Hisouten Vol", 20, 0, 500);
        CFG_DOWN_VOL = B.defineInRange("Down Vol", 50, 0, 500);
        B.pop();

        B.push("Danmaku");
        CFG_ONE_KILL = B.define("Danmaku 1 Kill Mode", false);
        CFG_SPAWN_DANMAKU_MOB = B.define("Spawn Danmaku Mob", true);
        CFG_SPAWN_FAIRY = B.define("Spawn Fairy", true);
        CFG_SPAWN_PHANTOM = B.define("Spawn Phantom", true);
        CFG_SPAWN_BOSS = B.define("Spawn Danmaku Mob Boss", true);
        CFG_FAIRY_SPAWN_RATE = B.defineInRange("Fairy Spawn Rate(%)", 50, 0, 100);
        CFG_DANMAKU_LEVEL = B.defineInRange("Danmaku Level", 2, 0, 4);
        CFG_HIT_RATE = B.defineInRange("Hit Rate of Enemy Danmaku(row=0,high=100)", 80, 0, 100);
        B.pop();

        B.push("Other");
        CFG_SPAWN_HANABEEPER = B.define("Spawn Hanabeeper", false);
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    /** Copy the spec values into the plain static fields (called on config load/reload). */
    public static void sync() {
        UsingMasterSpark = CFG_USE_MASTER_SPARK.get();
        MasterSparkDestroysBlocks = CFG_MASTER_SPARK_DESTROYS_BLOCKS.get();
        canUseWallPassingChisel = CFG_WALL_PASSING_CHISEL.get();
        shotMaxNumber = CFG_SHOT_MAX_NUMBER.get();
        laserMaxNumber = CFG_LASER_MAX_NUMBER.get();

        useTimeStopEffect = CFG_TIME_STOP_EFFECT.get();
        useDefaultGapSE = CFG_DEFAULT_GAP_SE.get();

        SpellCardVol = CFG_SPELL_CARD_VOL.get() / 100F;
        MasterSparkVol = CFG_MASTER_SPARK_VOL.get() / 100F;
        HisoutenVol = CFG_HISOUTEN_VOL.get() / 100F;
        DownVol = CFG_DOWN_VOL.get() / 100F;

        danmakuOneKillMode = CFG_ONE_KILL.get();
        spawnDanmakuMob = CFG_SPAWN_DANMAKU_MOB.get();
        spawnFairy = CFG_SPAWN_FAIRY.get();
        spawnPhantom = CFG_SPAWN_PHANTOM.get();
        spawnBoss = CFG_SPAWN_BOSS.get();
        fairySpawnRate = 100 - CFG_FAIRY_SPAWN_RATE.get();
        danmakuLevel = CFG_DANMAKU_LEVEL.get();
        danmakuAccuracy = 360F * (1F - (CFG_HIT_RATE.get() / 100F));

        spawnHanabeeper = CFG_SPAWN_HANABEEPER.get();
    }

    private ModConfig() {}
}
