package thkaguyamod.test;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.entity.effect.EntitySpellCardCircle;
import thkaguyamod.init.ModItems;
import thkaguyamod.spellcard.EntitySpellCard;

/**
 * 符卡右クリック（宣言）链路的行为测试 —— 「右键无反应」类故障的定位工具。
 *
 * <p>这条链路跨 5 个环节，任何一环坏掉都表现为「右键什么都不发生」：
 * <ol>
 *   <li>{@code ItemTHSpellCard.use} 是否真的被调用到（返回 success / 播放挥臂）；</li>
 *   <li>{@code THKaguyaLib.checkSpellCardDeclaration} 的创造模式旁路与消耗校验（bomb_item + 经验等级 ≥ needLevel）；</li>
 *   <li>早苗符卡专属的「奇迹之力」（CHARGE 组件）守卫范围是否只覆盖旧 meta 14..18；</li>
 *   <li>32 格视线射线是否真的选中了一个合格目标（1.7.10 同样排除 {@code Animal} 与 {@code AbstractVillager}）；</li>
 *   <li>宣言成功路径是否真的 {@code addFreshEntity} 了 {@link EntitySpellCard} 与 {@link EntitySpellCardCircle}。</li>
 * </ol>
 *
 * <p>这里刻意用 {@code stack.use(...)} 走真实物品入口，而不是直接调库方法，这样注册表
 * （{@code spell_card_musou_fuuin} → spellCardId 0 → SpellCardRegistry）也一起被测到。
 * 目标实体用 {@link ArmorStand}：它是 {@code LivingEntity} 但不是 {@code Animal}/{@code AbstractVillager}，
 * 因此和 1.7.10 一样属于合格目标；而且不会自己走动，断言稳定。
 *
 * <p>模板与 mock player 的约定见 {@link RegistryCompletenessTest}。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpellCardDeclarationTest {

    private static final String TEMPLATE = "empty";

    /** 施术者站位（相对坐标，面朝 +X）。 */
    private static final Vec3 USER_POS = new Vec3(1.5D, 3.0D, 3.5D);
    /** 目标站位（相对坐标，在施术者 +X 方向 4 格处）。 */
    private static final Vec3 TARGET_POS = new Vec3(5.5D, 3.0D, 3.5D);

    /** 1.21.1 的 yaw 约定：-90° = 面朝 +X（对应 1.7.10 的 rotationYaw = -90）。 */
    private static final float FACING_POSITIVE_X = -90.0F;

    private SpellCardDeclarationTest() {}

    /**
     * 验收场景：创造模式手持梦想封印，对着一个合格生物右键 → 宣言成功（符卡实体 + 魔法阵出现）。
     */
    @GameTest(template = TEMPLATE)
    public static void creativePlayerDeclaresViaItemUse(GameTestHelper helper) {
        Player player = placeUser(helper, GameType.CREATIVE);
        spawnTarget(helper);

        ItemStack stack = new ItemStack(ModItems.SPELL_CARD_MUSOU_FUUIN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        InteractionResultHolder<ItemStack> result = stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult().consumesAction(),
                "use() は success（消費扱い）を返すはず。1.7.10 も常に itemStack を返していた: " + result.getResult());

        List<EntitySpellCard> cards = helper.getLevel().getEntitiesOfClass(
                EntitySpellCard.class, player.getBoundingBox().inflate(64.0D));
        helper.assertValueEqual(cards.size(), 1,
                "宣言成功なら EntitySpellCard が 1 体出現するはず（0 なら checkSpellCardDeclaration が早期 return している）");

        List<EntitySpellCardCircle> circles = helper.getLevel().getEntitiesOfClass(
                EntitySpellCardCircle.class, player.getBoundingBox().inflate(64.0D));
        helper.assertValueEqual(circles.size(), 1, "プレイヤーの宣言なら魔法陣（EntitySpellCardCircle）も 1 枚出現するはず");

        for (EntitySpellCard card : cards) {
            card.discard();
        }
        for (EntitySpellCardCircle circle : circles) {
            circle.discard();
        }
        helper.succeed();
    }

    /**
     * 1.7.10 の消耗校验：非创造模式且经验等级 + bomb_item 数 < needLevel（梦想封印为 5）→ 沉默失败。
     */
    @GameTest(template = TEMPLATE)
    public static void survivalWithoutLevelFails(GameTestHelper helper) {
        Player player = placeUser(helper, GameType.SURVIVAL);
        spawnTarget(helper);

        player.experienceLevel = 0;

        ItemStack stack = new ItemStack(ModItems.SPELL_CARD_MUSOU_FUUIN.get());
        StackUse use = declare(helper, player, stack);

        // 1.7.10 同样：use() 恒返回 itemStack（= 成功），失败是静默的
        helper.assertTrue(use.result().getResult().consumesAction(),
                "非创造模式的失败路径也应返回 success（1.7.10 行为）");
        helper.assertValueEqual(use.cards().size(), 0,
                "经验等级 0 且无 bomb_item 时梦想封印（needLevel=5）必须宣言失败");
        helper.succeed();
    }

    /**
     * 消耗校验的反面：等级足够时（needLevel=5）应通过。
     * 同时证明梦想封印（旧 meta 0）不受早苗符卡的 CHARGE 守卫影响。
     */
    @GameTest(template = TEMPLATE)
    public static void nonSanaeCardIgnoresChargeGuard(GameTestHelper helper) {
        Player player = placeUser(helper, GameType.SURVIVAL);
        spawnTarget(helper);

        player.experienceLevel = 10;

        ItemStack stack = new ItemStack(ModItems.SPELL_CARD_MUSOU_FUUIN.get());
        StackUse use = declare(helper, player, stack);

        helper.assertValueEqual(use.cards().size(), 1,
                "经验等级 10 ≥ needLevel 5 时应宣言成功；id 0 不应被 CHARGE（早苗 meta 14..18）守卫拦下");
        helper.assertValueEqual(player.experienceLevel, 5,
                "needLevel 5 应从经验等级中扣除（bomb=0 → giveExperienceLevels(0-5)）");

        use.discard();
        helper.succeed();
    }

    /**
     * 早苗符卡（旧 meta 14）的 CHARGE 守卫：同样的等级条件下，没有「奇迹之力」必须失败。
     */
    @GameTest(template = TEMPLATE)
    public static void sanaeCardWithoutChargeFails(GameTestHelper helper) {
        Player player = placeUser(helper, GameType.SURVIVAL);
        spawnTarget(helper);

        player.experienceLevel = 10;

        ItemStack stack = new ItemStack(ModItems.SPELL_CARD_MIRACLE_FRUIT.get());
        StackUse use = declare(helper, player, stack);

        helper.assertValueEqual(use.cards().size(), 0,
                "早苗符卡在 CHARGE 组件缺失（= 0）时必须宣言失败（1.7.10 的 NBT \"Charge\" 缺失同样失败）");
        helper.assertValueEqual(player.experienceLevel, 10, "守卫提前失败时不应扣除经验等级");
        helper.succeed();
    }

    /**
     * 1.7.10 的目标筛选是「LivingEntity 且不是 {@code EntityAnimal} 也不是 {@code EntityVillager}」，
     * 1.21.1 的等价类是 {@code Animal} / {@code AbstractVillager}。
     *
     * <p>这条测试把「猪不是合格目标」写死成回归断言：玩家对着猪右键时，两个版本都是<b>静默失败</b>
     * （没有任何反馈），这正是「右键无反应」报告里最容易踩到的一种情形。合格目标是
     * 僵尸 / 骷髅 / 末影人 / 本 mod 的弹幕生物 / 盔甲架这类非 Animal、非村民的生物。
     */
    @GameTest(template = TEMPLATE)
    public static void animalTargetIsRejectedLikeIn1710(GameTestHelper helper) {
        Player player = placeUser(helper, GameType.CREATIVE);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, TARGET_POS);
        pig.setNoGravity(true);

        ItemStack stack = new ItemStack(ModItems.SPELL_CARD_MUSOU_FUUIN.get());
        StackUse use = declare(helper, player, stack);

        helper.assertValueEqual(use.cards().size(), 0,
                "猪属于 Animal，1.7.10 与 1.21.1 都把它排除在符卡目标之外（对着猪右键必然静默失败）");
        helper.succeed();
    }

    /* ------------------------------------------------------------------ */

    private static Player placeUser(GameTestHelper helper, GameType gameType) {
        Player player = helper.makeMockPlayer(gameType);
        // # 诊断保留: mock player 不在实体 tick 列表里，构造后必须显式 moveTo 才会把包围盒
        // 刷到目标位置（不刷的话射线起点还停在 (0,0,0)，测试会误报「没有目标」）。
        Vec3 pos = helper.absoluteVec(USER_POS);
        player.moveTo(pos, FACING_POSITIVE_X, 0.0F);
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    private static ArmorStand spawnTarget(GameTestHelper helper) {
        // # 诊断保留: helper.spawn(type, Vec3) 内部已经调用 absoluteVec，这里必须传相对坐标。
        // （若再套一层 absoluteVec，坐标会被换算两次 → 目标跑到千万格外，射线必然打空。）
        ArmorStand target = helper.spawn(EntityType.ARMOR_STAND, TARGET_POS);
        target.setNoGravity(true);
        return target;
    }

    /** 手持符卡执行一次真实的物品右键入口，并返回现场快照。 */
    private static StackUse declare(GameTestHelper helper, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResultHolder<ItemStack> result = stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        List<EntitySpellCard> cards = helper.getLevel().getEntitiesOfClass(
                EntitySpellCard.class, player.getBoundingBox().inflate(64.0D));
        List<EntitySpellCardCircle> circles = helper.getLevel().getEntitiesOfClass(
                EntitySpellCardCircle.class, player.getBoundingBox().inflate(64.0D));
        return new StackUse(result, cards, circles);
    }

    /** 一次右键之后的现场快照（断言用，不可跨 tick 持有）。 */
    private record StackUse(InteractionResultHolder<ItemStack> result,
                            List<EntitySpellCard> cards,
                            List<EntitySpellCardCircle> circles) {

        void discard() {
            for (EntitySpellCard card : cards) {
                card.discard();
            }
            for (EntitySpellCardCircle circle : circles) {
                circle.discard();
            }
        }
    }
}