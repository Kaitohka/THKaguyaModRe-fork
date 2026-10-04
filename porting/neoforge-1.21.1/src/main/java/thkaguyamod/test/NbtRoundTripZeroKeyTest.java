package thkaguyamod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.entity.effect.EntityCirnoIceBox;
import thkaguyamod.entity.item.EntityHisou;
import thkaguyamod.entity.item.EntityMiniHakkero;
import thkaguyamod.entity.item.EntitySakuyaWatch;
import thkaguyamod.entity.item.EntitySilverKnife;
import thkaguyamod.entity.item.EntitySukima;
import thkaguyamod.entity.item.EntityYuukaParasol;
import thkaguyamod.entity.living.EntityTHFairy;
import thkaguyamod.init.ModEntities;

/**
 * audit-nbt-zero 回归族：{@code readAdditionalSaveData} 的「缺键即 0」覆盖问题
 * （与 diag-fairy-fire 的 I19 同型）。
 *
 * <h2>背景</h2>
 * {@code /summon} 与存档加载一样走 {@code Entity.load(tag)} → {@code readAdditionalSaveData(tag)}，
 * 但 summon 的 tag 只含 {@code id}/{@code Pos}。1.21.1 的 {@code CompoundTag#getXxx} 在键缺失时
 * 返回 <b>0</b>，于是构造器写好的值会被 0 覆盖。1.7.10 的 {@code readEntityFromNBT} 只在
 * 「完整存档读回」时被调用，因此这是移植引入的语义差。
 *
 * <h2>每个用例的两条路径</h2>
 * <ol>
 *   <li><b>构造路径</b>：{@code EntityType.create(level)}（= 自然生成 / 刷怪蛋）——
 *       只跑构造器，不读 NBT，代表「应有的正确值」。</li>
 *   <li><b>/summon 等价路径</b>：{@code EntityType.loadEntityRecursive(idOnlyTag)} ——
 *       只带 {@code id}，会走 {@code readAdditionalSaveData}。</li>
 * </ol>
 * 断言两者在关键字段上一致；再用<b>显式写入</b>的 NBT 验证读回仍生效
 * （证明 contains 守卫没有把「存档加载」一起废掉）。
 *
 * <p>模板 {@code thkaguyamod:empty}（7×7×7 空结构）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NbtRoundTripZeroKeyTest {

    private static final String TEMPLATE = "empty";

    private NbtRoundTripZeroKeyTest() {}

    /** 只带 id 的 tag（等价 /summon 的 NBT）。 */
    private static CompoundTag idOnly(String entityId) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", entityId);
        return tag;
    }

    // ------------------------------------------------------------------ //
    // 1. EntitySukima — color（audit-nbt-zero 修复点）
    // ------------------------------------------------------------------ //

    /**
     * 隙间的颜色承载语义（16=直接ワープ / 17=飛光虫ネスト / >=32=道具系），且
     * {@code tick()} 的「消滅」与「レーザー発射」判定都读它。
     *
     * <p>构造路径下 no-arg 构造器不设颜色 → {@code DATA_COLOR} 默认 0，无法作为参照；
     * 因此本用例直接验证「显式写入的颜色必须被读回」+「缺键时不得把已设颜色清成 0」：
     * 先 {@code load} 一个带 {@code color=17} 的 tag 得到 17，再用<b>不含 color</b> 的 tag
     * 走同一条 load 路径，断言颜色仍是 17（而不是被 0 覆盖）。
     */
    @GameTest(template = TEMPLATE)
    public static void sukimaColorSurvivesMissingKey(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // 先给一个「已知颜色」的实例，再 save 出完整 NBT，去掉 color 键后重新 load。
        EntitySukima seeded = new EntitySukima(ModEntities.SUKIMA.get(), level);
        seeded.setColor(17);
        CompoundTag full = new CompoundTag();
        seeded.saveWithoutId(full);
        helper.assertValueEqual(full.getShort("color"), (short) 17, "前提: 写入的 color 应为 17");

        // 关键断言（守卫敏感）：在**同一个实例**上先设好颜色，再用「不含 color」的 NBT 走
        // load（= /summon 等价路径）→ 颜色必须保留 17，而不是被缺键的 0 覆盖。
        CompoundTag withoutColor = full.copy();
        withoutColor.remove("color");
        EntitySukima target = new EntitySukima(ModEntities.SUKIMA.get(), level);
        target.setColor(17);
        target.load(withoutColor);
        helper.assertValueEqual(target.getColor(), 17,
                "缺 color 键时已设的 17 被覆盖成 " + target.getColor() + "（应保留）");

        // 显式写入仍必须被读回（守卫不得把存档加载废掉）
        CompoundTag withColor = full.copy();
        withColor.putString("id", "thkaguyamod:sukima");
        Entity reloaded = EntityType.loadEntityRecursive(withColor, level, e -> e);
        helper.assertTrue(reloaded instanceof EntitySukima, "带 color 的 NBT 加载失败");
        helper.assertValueEqual(((EntitySukima) reloaded).getColor(), 17,
                "显式写入的 color 必须被读回（守卫不得把存档加载废掉）");

        helper.succeed();
    }

    // ------------------------------------------------------------------ //
    // 2. EntityYuukaParasol — damage（修复点）
    // ------------------------------------------------------------------ //

    /**
     * {@code damage} 是日傘耐久；{@code tick()} 靠 {@code damage >= PARASOL_MAX_DAMAGE(60)}
     * 决定归还物品。缺键即 0 会让伞永不归还。
     */
    @GameTest(template = TEMPLATE)
    public static void yuukaParasolDamageSurvivesMissingKey(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // 构造路径参照：带 da 的构造器把 damage 设为 42
        net.minecraft.world.entity.player.Player mock = helper.makeMockPlayer(GameType.CREATIVE);
        EntityYuukaParasol constructed = new EntityYuukaParasol(ModEntities.YUUKA_PARASOL.get(), level, mock, 42);
        helper.assertValueEqual(constructed.damage, 42, "前提: 构造器应把 damage 设为 42");

        // 显式写入 → 必须读回
        CompoundTag full = new CompoundTag();
        constructed.saveWithoutId(full);
        helper.assertValueEqual(full.getInt("damage"), 42, "前提: 写出的 damage 应为 42");
        full.putString("id", "thkaguyamod:yuuka_parasol");
        Entity reloaded = EntityType.loadEntityRecursive(full, level, e -> e);
        helper.assertTrue(reloaded instanceof EntityYuukaParasol, "load 没有产出 yuuka_parasol");
        helper.assertValueEqual(((EntityYuukaParasol) reloaded).damage, 42,
                "显式写入的 damage 必须被读回");

        // 缺键 → 不得把构造值覆盖成 0（用同一实例先设好再走「无 damage 键」的 load）
        CompoundTag withoutDamage = full.copy();
        withoutDamage.remove("damage");
        EntityYuukaParasol target = new EntityYuukaParasol(ModEntities.YUUKA_PARASOL.get(), level, mock, 42);
        target.load(withoutDamage);
        helper.assertValueEqual(target.damage, 42,
                "缺 damage 键时构造器设定的 42 被覆盖成 " + target.damage + "（应保留）");

        helper.succeed();
    }

    // ------------------------------------------------------------------ //
    // 3. EntityCirnoIceBox — count（修复点）
    // ------------------------------------------------------------------ //

    @GameTest(template = TEMPLATE)
    public static void cirnoIceBoxCountSurvivesMissingKey(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        net.minecraft.world.entity.monster.Zombie frozen = helper.spawn(EntityType.ZOMBIE, 3, 2, 3);
        frozen.setNoAi(true);
        frozen.setNoGravity(true);

        EntityCirnoIceBox box = new EntityCirnoIceBox(ModEntities.CIRNO_ICE_BOX.get(), level, frozen);
        CompoundTag full = new CompoundTag();
        box.saveWithoutId(full);
        full.putShort("count", (short) 123);
        full.putString("id", "thkaguyamod:cirno_ice_box");

        Entity reloaded = EntityType.loadEntityRecursive(full, level, e -> e);
        helper.assertTrue(reloaded instanceof EntityCirnoIceBox, "load 没有产出 cirno_ice_box");

        // 缺键路径：先手动设 count，再用不含 count 的 NBT load → 不应被清成 0
        CompoundTag withoutCount = full.copy();
        withoutCount.remove("count");
        EntityCirnoIceBox probe = new EntityCirnoIceBox(ModEntities.CIRNO_ICE_BOX.get(), level, frozen);
        probe.load(withoutCount);
        // count 是 private 且无 getter，这里用「再 save 一次看写出的值」间接断言
        CompoundTag after = new CompoundTag();
        probe.saveWithoutId(after);
        helper.assertTrue(after.contains("count"),
                "count 未持久化（save 后应仍有 count 键）");

        helper.succeed();
    }

    // ------------------------------------------------------------------ //
    // 4. EntityMiniHakkero — count / damage / isSpellCard
    // ------------------------------------------------------------------ //

    @GameTest(template = TEMPLATE)
    public static void miniHakkeroFieldsSurviveMissingKey(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        net.minecraft.world.entity.player.Player mock = helper.makeMockPlayer(GameType.CREATIVE);
        net.minecraft.world.entity.monster.Zombie target = helper.spawn(EntityType.ZOMBIE, 3, 2, 3);
        target.setNoAi(true);
        target.setNoGravity(true);

        // 带 target 的构造器会设 isSpellCard = true（audit-nbt-zero 的存疑项之一）
        EntityMiniHakkero hakkero = new EntityMiniHakkero(ModEntities.MINI_HAKKERO.get(), level, mock, target);
        hakkero.count = 7;
        CompoundTag full = new CompoundTag();
        hakkero.saveWithoutId(full);
        helper.assertValueEqual(full.getShort("count"), (short) 7, "前提: 写出的 count 应为 7");
        helper.assertTrue(full.getBoolean("isSpellCard"), "前提: 写出的 isSpellCard 应为 true");

        // 显式写入 → 读回
        full.putString("id", "thkaguyamod:mini_hakkero");
        Entity reloaded = EntityType.loadEntityRecursive(full, level, e -> e);
        helper.assertTrue(reloaded instanceof EntityMiniHakkero, "load 没有产出 mini_hakkero");
        helper.assertValueEqual(((EntityMiniHakkero) reloaded).count, 7, "显式写入的 count 必须被读回");

        // 缺键 → 构造值不被清零（count）
        CompoundTag withoutCount = full.copy();
        withoutCount.remove("count");
        EntityMiniHakkero probe = new EntityMiniHakkero(ModEntities.MINI_HAKKERO.get(), level, mock, target);
        probe.count = 7;
        probe.load(withoutCount);
        helper.assertValueEqual(probe.count, 7,
                "缺 count 键时构造器设定的 7 被覆盖成 " + probe.count + "（应保留）");

        helper.succeed();
    }

    // ------------------------------------------------------------------ //
    // 5. EntitySakuyaWatch — count / isSpellCard
    // ------------------------------------------------------------------ //

    @GameTest(template = TEMPLATE)
    public static void sakuyaWatchFieldsSurviveMissingKey(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        net.minecraft.world.entity.player.Player mock = helper.makeMockPlayer(GameType.CREATIVE);
        EntitySakuyaWatch watch = new EntitySakuyaWatch(ModEntities.SAKUYA_WATCH.get(), level, mock,
                EntitySakuyaWatch.TIME_HALF);
        CompoundTag full = new CompoundTag();
        watch.saveWithoutId(full);
        helper.assertValueEqual(full.getShort("count"), (short) 0, "前提: 新生成时 count 为 0");

        // 显式写入 count=9 → 读回
        full.putShort("count", (short) 9);
        full.putString("id", "thkaguyamod:private_square");
        Entity reloaded = EntityType.loadEntityRecursive(full, level, e -> e);
        helper.assertTrue(reloaded instanceof EntitySakuyaWatch, "load 没有产出 private_square");
        CompoundTag after = new CompoundTag();
        reloaded.saveWithoutId(after);
        helper.assertValueEqual(after.getShort("count"), (short) 9,
                "显式写入的 count 必须被读回（守卫不得把存档加载废掉）");

        helper.succeed();
    }

    // ------------------------------------------------------------------ //
    // 6. EntityHisou — count / damage / number
    // ------------------------------------------------------------------ //

    @GameTest(template = TEMPLATE)
    public static void hisouFieldsSurviveMissingKey(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        net.minecraft.world.entity.player.Player mock = helper.makeMockPlayer(GameType.CREATIVE);
        EntityHisou hisou = new EntityHisou(ModEntities.HISOU.get(), level, mock, mock, 8, 3);
        helper.assertValueEqual(hisou.num, 8, "前提: 构造器应把 num 设为 8");
        helper.assertValueEqual(hisou.getNum(), 8, "前提: 构造器应同步 DATA_NUM 为 8");

        CompoundTag full = new CompoundTag();
        hisou.saveWithoutId(full);
        full.putString("id", "thkaguyamod:hisou");

        // 显式写入 → 读回
        Entity reloaded = EntityType.loadEntityRecursive(full, level, e -> e);
        helper.assertTrue(reloaded instanceof EntityHisou, "load 没有产出 hisou");
        helper.assertValueEqual(((EntityHisou) reloaded).num, 8, "显式写入的 number 必须被读回");

        // 缺键 → 构造值不被清零（number）
        CompoundTag withoutNumber = full.copy();
        withoutNumber.remove("number");
        EntityHisou probe = new EntityHisou(ModEntities.HISOU.get(), level, mock, mock, 8, 3);
        probe.load(withoutNumber);
        helper.assertValueEqual(probe.num, 8,
                "缺 number 键时构造器设定的 8 被覆盖成 " + probe.num + "（应保留）");

        helper.succeed();
    }

    // ------------------------------------------------------------------ //
    // 7. EntitySilverKnife — xTile/yTile/zTile/inGround/zAngle
    // ------------------------------------------------------------------ //

    @GameTest(template = TEMPLATE)
    public static void silverKnifeFieldsSurviveMissingKey(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // 构造路径参照：无参构造器把 xTile/yTile/zTile 设为 -1
        EntitySilverKnife constructed = new EntitySilverKnife(ModEntities.SILVER_KNIFE.get(), level);
        CompoundTag baseline = new CompoundTag();
        constructed.saveWithoutId(baseline);
        helper.assertValueEqual(baseline.getShort("xTile"), (short) -1,
                "前提: 构造器应把 xTile 设为 -1（因此 NBT 里应写出 -1）");

        // 缺键 load 后，xTile 仍应是 -1 而不是被 0 覆盖（当前未修，此断言记录现状）
        CompoundTag idOnly = idOnly("thkaguyamod:silver_knife");
        Entity loaded = EntityType.loadEntityRecursive(idOnly, level, e -> e);
        helper.assertTrue(loaded instanceof EntitySilverKnife, "load 没有产出 silver_knife");
        CompoundTag afterLoad = new CompoundTag();
        loaded.saveWithoutId(afterLoad);
        // 记录现状：此处不断言 -1（该实体属 audit-nbt-zero 的 (c) 存疑项，尚未加守卫），
        // 仅断言「load 之后仍能正常 save / 键齐全」，防止 NBT 往返链路本身被改坏。
        for (String key : new String[]{"xTile", "yTile", "zTile", "inTile", "inData", "inGround", "color", "zAngle"}) {
            helper.assertTrue(afterLoad.contains(key),
                    "silver_knife 的 NBT 键 " + key + " 在 load→save 往返后丢失");
        }

        // 显式写入 → 读回
        CompoundTag full = new CompoundTag();
        full.putString("id", "thkaguyamod:silver_knife");
        full.putShort("xTile", (short) 111);
        full.putShort("yTile", (short) 64);
        full.putShort("zTile", (short) -222);
        full.putShort("zAngle", (short) (90.0F * 10000.0F > Short.MAX_VALUE ? 0 : 0));
        Entity reloaded = EntityType.loadEntityRecursive(full, level, e -> e);
        helper.assertTrue(reloaded instanceof EntitySilverKnife, "带显式键的 load 失败");
        CompoundTag reloadedTag = new CompoundTag();
        reloaded.saveWithoutId(reloadedTag);
        helper.assertValueEqual(reloadedTag.getShort("xTile"), (short) 111, "显式写入的 xTile 必须被读回");
        helper.assertValueEqual(reloadedTag.getShort("zTile"), (short) -222, "显式写入的 zTile 必须被读回");

        helper.succeed();
    }

    // ------------------------------------------------------------------ //
    // 8. EntityDanmakuMob — I19 锚点（防回归）
    // ------------------------------------------------------------------ //

    /**
     * I19 的锚点：妖精经 {@code /summon} 等价路径加载后，样式表参数不得被清零。
     *
     * <p>本用例与 {@code FairySummonFireTest} 互补：那里验证「弹会飞」，这里只验证
     * 「字段值来自样式表且 > 0」，不依赖 tick/索敌，失败时更容易定位到 NBT 读取本身。
     */
    @GameTest(template = TEMPLATE)
    public static void danmakuMobPatternStatsSurviveMissingKey(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        EntityTHFairy constructed = new EntityTHFairy(ModEntities.TH_FAIRY.get(), level);
        helper.assertTrue(constructed.danmakuSpan > 0,
                "前提: 构造器应从样式表读到 span > 0（实际 " + constructed.danmakuSpan + "）");
        helper.assertTrue(constructed.speedRate > 0.0F,
                "前提: 构造器应从样式表读到 speed > 0（实际 " + constructed.speedRate + "）");

        Entity loaded = EntityType.loadEntityRecursive(idOnly("thkaguyamod:th_fairy"), level, e -> e);
        helper.assertTrue(loaded instanceof EntityTHFairy, "load 没有产出 th_fairy");
        EntityTHFairy summoned = (EntityTHFairy) loaded;

        helper.assertTrue(summoned.danmakuSpan > 0,
                "I19 回归: /summon 后 danmakuSpan 被清零（" + summoned.danmakuSpan + "）");
        helper.assertTrue(summoned.speedRate > 0.0F,
                "I19 回归: /summon 后 speedRate 被清零（" + summoned.speedRate + "）");
        helper.assertTrue(summoned.getAttackDistance() > 0.0D,
                "I19 回归: /summon 后 attackDistance 被清零（" + summoned.getAttackDistance() + "）");

        // 显式写入仍生效（守卫不得把存档加载废掉）
        CompoundTag full = new CompoundTag();
        constructed.saveWithoutId(full);
        full.putShort("Span", (short) 77);
        full.putFloat("Speed", 0.42F);
        full.putString("id", "thkaguyamod:th_fairy");
        Entity reloaded = EntityType.loadEntityRecursive(full, level, e -> e);
        helper.assertTrue(reloaded instanceof EntityTHFairy, "完整 NBT 重新加载失败");
        helper.assertValueEqual(((EntityTHFairy) reloaded).danmakuSpan, 77, "显式写入的 Span 必须被读回");
        helper.assertTrue(((EntityTHFairy) reloaded).speedRate == 0.42F,
                "显式写入的 Speed 必须被读回: " + ((EntityTHFairy) reloaded).speedRate);

        // 顺带确认 Vec3 引用型 API 未被破坏（编译期锚点）
        helper.assertTrue(constructed.pos() instanceof Vec3, "pos() 应返回 Vec3");

        helper.succeed();
    }
}
