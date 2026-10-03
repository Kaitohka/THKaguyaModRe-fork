package thkaguyamod.test;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.entity.effect.EntitySpellCardCircle;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModItems;
import thkaguyamod.spellcard.EntitySpellCard;

/**
 * 階段 5 品質基建（gametest-edge）：符卡宣言の<b>失敗分岐</b>の穴埋めテスト。
 *
 * <p>{@link SpellCardDeclarationTest} が「成功経路 / レベル不足 / 早苗符卡の CHARGE 欠落 /
 * Animal ターゲット」を押さえているのに対し、こちらは残っていた分岐だけを狙う
 * （重複テストを作らないのが目的なので、成功経路の再確認はしない）：
 * <ol>
 *   <li><b>視線内にターゲットが 1 体もいない</b> → 静默失敗（例外なし・符卡実体なし・
 *       {@code use()} は success のまま）。1.7.10 と同じ「何も起きない」が仕様；</li>
 *   <li><b>ターゲットとの間にブロックがある</b> → 同じく失敗（32 格の {@code ClipContext} が
 *       視線を遮る）。「見えている相手にだけ宣言できる」の実装確認；</li>
 *   <li><b>同一プレイヤーが 64 格以内に既に展開済み</b> → 2 回目の宣言は失敗する
 *       （{@code checkSpellCardDeclaration} の「this user already has a spell card active」分岐。
 *       既存テストで唯一の未到達分岐だった）；</li>
 *   <li><b>CHARGE の境界</b>：{@code charge=0} の明示設定は欠落と同様に失敗し、
 *       {@code charge=1}（最小の有効値）は宣言に進む（既存テストは「欠落」しか見ていない）。</li>
 * </ol>
 *
 * <p>失敗はすべて<b>静默</b>（チャット無し・例外無し）で、{@code use()} の戻り値は
 * 1.7.10 同様に success のままである点もここで固定する（「右键无反应」报告の切り分け基準）。
 *
 * <p>配置・向き・mock player の約定は {@link SpellCardDeclarationTest} と同じ
 * （施术者 (1.5,3,3.5) から +X 方向、yaw=-90 が +X、ターゲットは (5.5,3,3.5) の ArmorStand）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpellCardDeclarationEdgeTest {

    private static final String TEMPLATE = "empty";

    private static final Vec3 USER_POS = new Vec3(1.5D, 3.0D, 3.5D);
    private static final Vec3 TARGET_POS = new Vec3(5.5D, 3.0D, 3.5D);

    /** 1.21.1 の yaw 約定：-90° = 面朝 +X。 */
    private static final float FACING_POSITIVE_X = -90.0F;

    private SpellCardDeclarationEdgeTest() {}

    /**
     * 視線内にターゲットが無い（実体を 1 体も置かない）場合の静默失敗。
     */
    @GameTest(template = TEMPLATE)
    public static void noTargetInLineOfSightFailsSilently(GameTestHelper helper) {
        Player player = placeUser(helper, GameType.CREATIVE);
        // ターゲットは置かない。構造内の実体は mock player のみ（= 弾幕は空）

        ItemStack stack = new ItemStack(ModItems.SPELL_CARD_MUSOU_FUUIN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResultHolder<ItemStack> result = stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(result.getResult().consumesAction(),
                "失敗経路でも use() は success（= 消費扱い）を返すはず（1.7.10 と同じ）: " + result.getResult());
        helper.assertValueEqual(cardsNear(helper, player).size(), 0,
                "視線内にターゲットが無ければ符卡実体は出現しないはず（no valid target 分岐）");
        helper.assertValueEqual(circlesNear(helper, player).size(), 0, "符卡が出ないなら魔法陣も出ないはず");
        helper.succeed();
    }

    /**
     * ターゲットは居るが、間にブロックがある場合の失敗（視線遮蔽）。
     * <p>施术者と敵の間（x=3）に石を 2 段置き、32 格の {@code ClipContext} が遮ることを確認する。
     * ターゲットの実体（ArmorStand）は置いたままなので、失敗理由が
     * 「ターゲット不在」ではなく「遮蔽」であることがこのテストの主張。
     */
    @GameTest(template = TEMPLATE)
    public static void blockedLineOfSightFails(GameTestHelper helper) {
        Player player = placeUser(helper, GameType.CREATIVE);
        spawnTarget(helper);

        // 視線は eye 高さ（≒ y=4.62）を通る。y=4 のブロック正面で遮られる。
        helper.setBlock(3, 4, 3, Blocks.STONE);
        helper.setBlock(3, 3, 3, Blocks.STONE);

        ItemStack stack = new ItemStack(ModItems.SPELL_CARD_MUSOU_FUUIN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResultHolder<ItemStack> result = stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(result.getResult().consumesAction(), "遮蔽時も use() は success を返すはず（静默失敗）");
        helper.assertValueEqual(cardsNear(helper, player).size(), 0,
                "視線がブロックで遮られている間は宣言できないはず（1.7.10 の func_147447_a 相当）");
        helper.succeed();
    }

    /**
     * 同一プレイヤーが 64 格以内に既に符卡を展開している場合、2 回目は失敗する。
     * <p>創造モード（レベル消費なし）で 1 回目を成功させ、間を置かず 2 回目を撃つ。
     * 「同一声明者の重複展開禁止」分岐だけが理由になるよう、ターゲットは置いたままにする。
     */
    @GameTest(template = TEMPLATE)
    public static void secondDeclarationWhileActiveFails(GameTestHelper helper) {
        Player player = placeUser(helper, GameType.CREATIVE);
        spawnTarget(helper);

        ItemStack stack = new ItemStack(ModItems.SPELL_CARD_MUSOU_FUUIN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        List<EntitySpellCard> first = cardsNear(helper, player);
        helper.assertValueEqual(first.size(), 1, "1 回目の宣言は成功して符卡実体が 1 体出るはず（前提条件）");

        stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(cardsNear(helper, player).size(), 1,
                "64 格以内に自分の符卡がある間の 2 回目は失敗するはず（already active 分岐）");

        discardAll(helper, player);
        helper.succeed();
    }

    /**
     * CHARGE の境界：0 は（欠落と同様に）失敗、1 は成功して宣言に進む。
     * <p>早苗符卡（旧 meta 14）・非创造モード・経験レベル十分という条件で、
     * CHARGE の値だけを 0 / 1 に振る。1 のときは needLevel=1 を消費してレベルが 1 減る。
     */
    @GameTest(template = TEMPLATE)
    public static void chargeZeroFailsAndChargeOneSucceeds(GameTestHelper helper) {
        // charge = 0（明示設定。既存テストの「コンポーネント欠落」とは別経路）
        Player zeroPlayer = placeUser(helper, GameType.SURVIVAL);
        spawnTarget(helper);
        zeroPlayer.experienceLevel = 10;
        ItemStack zeroStack = new ItemStack(ModItems.SPELL_CARD_MIRACLE_FRUIT.get());
        zeroStack.set(ModDataComponents.CHARGE.get(), 0);
        zeroPlayer.setItemInHand(InteractionHand.MAIN_HAND, zeroStack);
        zeroStack.use(helper.getLevel(), zeroPlayer, InteractionHand.MAIN_HAND);

        helper.assertValueEqual(cardsNear(helper, zeroPlayer).size(), 0,
                "CHARGE を明示的に 0 にしても宣言できないはず（1.7.10 の Charge<=0 と等価）");
        helper.assertValueEqual(zeroPlayer.experienceLevel, 10, "CHARGE ガードで失敗したらレベルは消費されない");

        // charge = 1（最小の有効値）→ 宣言成功。needLevel=1 が経験レベルから引かれる
        Player onePlayer = placeUser(helper, GameType.SURVIVAL);
        onePlayer.experienceLevel = 10;
        ItemStack oneStack = new ItemStack(ModItems.SPELL_CARD_MIRACLE_FRUIT.get());
        oneStack.set(ModDataComponents.CHARGE.get(), 1);
        onePlayer.setItemInHand(InteractionHand.MAIN_HAND, oneStack);
        oneStack.use(helper.getLevel(), onePlayer, InteractionHand.MAIN_HAND);

        helper.assertValueEqual(cardsNear(helper, onePlayer).size(), 1,
                "CHARGE=1（最小の有効値）なら宣言に進み、符卡実体が 1 体出るはず");
        helper.assertValueEqual(onePlayer.experienceLevel, 9,
                "needLevel=1 の符卡は経験レベルを 1 消費するはず（bomb=0 → giveExperienceLevels(0-1)）");

        discardAll(helper, zeroPlayer);
        discardAll(helper, onePlayer);
        helper.succeed();
    }

    /* ------------------------------------------------------------------ */

    private static Player placeUser(GameTestHelper helper, GameType gameType) {
        Player player = helper.makeMockPlayer(gameType);
        // mock player は実体 tick リストに無いので、moveTo + setPos で包围盒と向きを明示的に刷る
        Vec3 pos = helper.absoluteVec(USER_POS);
        player.moveTo(pos, FACING_POSITIVE_X, 0.0F);
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    private static ArmorStand spawnTarget(GameTestHelper helper) {
        ArmorStand target = helper.spawn(EntityType.ARMOR_STAND, TARGET_POS);
        target.setNoGravity(true);
        return target;
    }

    private static List<EntitySpellCard> cardsNear(GameTestHelper helper, Player player) {
        return helper.getLevel().getEntitiesOfClass(EntitySpellCard.class, player.getBoundingBox().inflate(64.0D));
    }

    private static List<EntitySpellCardCircle> circlesNear(GameTestHelper helper, Player player) {
        return helper.getLevel().getEntitiesOfClass(EntitySpellCardCircle.class, player.getBoundingBox().inflate(64.0D));
    }

    private static void discardAll(GameTestHelper helper, Player player) {
        for (EntitySpellCard card : cardsNear(helper, player)) {
            card.discard();
        }
        for (EntitySpellCardCircle circle : circlesNear(helper, player)) {
            circle.discard();
        }
    }
}