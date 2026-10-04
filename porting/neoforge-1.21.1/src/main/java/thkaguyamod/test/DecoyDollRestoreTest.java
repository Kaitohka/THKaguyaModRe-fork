package thkaguyamod.test;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.entity.item.EntityCursedDecoyDoll;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModItems;
import thkaguyamod.menu.MiracleMalletMenu;

/**
 * 诅咒替身人偶「耐久耗尽还原原素材」的回归测试（对应已知问题 §5#8 / 台账 P11）。
 *
 * <h2>1.7.10 的语义</h2>
 * <p>原作靠 NBT 整数 {@code OriginalItemID}（数字物品 ID）+ {@code OriginalDamage} 记录素材，
 * 耐久耗尽时 {@code Item.getItemById(originID)} 取回原物品并套用旧 damage
 * （{@code ItemCursedDecoyDoll.onItemRightClick} 62-77）。写入者是万宝槌转换台
 * （{@code ContainerMiracleMallet} 118-124）。
 *
 * <h2>1.21.1 的等价物</h2>
 * <ul>
 *   <li>{@code OriginalItemID} → {@link ModDataComponents#ORIGINAL_ITEM}（レジストリ
 *       {@code Holder<Item>}）。数字 ID が無いため Holder で持つ；</li>
 *   <li>{@code OriginalDamage} → {@link ModDataComponents#ORIGINAL_DAMAGE}（既存、階段 4 で導入済み）；</li>
 *   <li>書き込み → {@link MiracleMalletMenu#updateResult()}（変換台の産出時に素材を記録）；</li>
 *   <li>読み出し → {@code ItemCursedDecoyDoll#use}（耐久を使い切ったら
 *       {@code player.setItemInHand} で差し替え）。</li>
 * </ul>
 *
 * <h2>注意：現在この経路はゲーム内から到達できない</h2>
 * <p>1.7.10 でデコイ人形を産出していた変換は外部 MOD「アリスの人形」を素材にしており、
 * その MOD に 1.21.1 版が無いため変換ごと削除された（台账 P8）。したがって
 * {@code ORIGINAL_ITEM} を載せたデコイ人形は正規プレイでは入手できない。
 * 本テストは「変換を将来復活させたときに壊れていないこと」を保証するために、
 * 変換台の書き込み経路と人形側の読み出し経路を**それぞれ直接叩いて**検証する。
 *
 * <p>テンプレートの約定は {@link RegistryCompletenessTest} を参照（{@code empty}）。
 *
 * <h2>偶発赤（フレーク）対策 — 本ファイルの 2 つの決定性ルール</h2>
 * <p>本ファイルの用例は当初、稀に「ORIGINAL_ITEM が空 / 復元されない」で赤になっていた。
 * 原因は 2 つあり、どちらもテスト側で除去した（製品コードは正しい）。
 * <ol>
 *   <li><b>テスト間干渉（支配的）</b>：{@code ItemCursedDecoyDoll#use} の冒頭には原作由来の
 *       「40 ブロック以内に既にデコイ人形がいるなら使用失敗」ガードがある。GameTest の
 *       {@code makeMockPlayer} は <b>BlockPos.ZERO</b> に居るため、このクラスの複数の用例が
 *       <b>同じ 40 ブロック圏内</b>で動く。GameTest の構造は 12 格間隔のグリッドに並ぶので、
 *       別構造で spawn されたデコイ人形（本クラスの他用例、あるいは将来の用例）が
 *       ガードに引っかかると {@code use()} は<b>耐久すら減らさずに早期 return</b> する。
 *       → 対策: 各用例の冒頭で {@link #clearDecoyDollsNearMockPlayers} を呼び、
 *       mock player の 40 格圏内のデコイ人形を除去する（実行環境の前提を明示的に整える）。</li>
 *   <li><b>読取タイミング（本ファイルでは既に対策済みだが明記）</b>：{@code hurtAndBreak} は
 *       耐久を使い切ると {@code shrink(1)} し、<b>空になった ItemStack は
 *       {@code getComponents()} が EMPTY を返す</b>ためコンポーネントが読めなくなる。
 *       そのため {@code ORIGINAL_ITEM}/{@code ORIGINAL_DAMAGE} は <b>use() を呼ぶ前に</b>
 *       スタックから読み、製品コードも同じ順序で読んでいる（{@code ItemCursedDecoyDoll:74-75}）。
 *       テストのアサーションも空スタックから読み直さない。</li>
 * </ol>
 * <p>さらに本ファイルは「複数の GameTest 構造にまたがる観測」を一切しない
 * （ワールド tick を跨ぐ観測窓を持たない）ので、{@code danmakuRemove} 等の
 * 近隣構造からの副作用の影響も受けない。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DecoyDollRestoreTest {

    private static final String TEMPLATE = "empty";

    private DecoyDollRestoreTest() {}

    /**
     * デコイ人形の「40 ブロック以内に既に居るなら使用不可」ガード（原作由来）から
     * テストを守るための前提整備。
     *
     * <p>GameTest の {@code makeMockPlayer} は BlockPos.ZERO に居るため、このクラスの
     * 用例はすべて同じ 40 格圏内で走る。GameTest の構造は 12 格間隔のグリッドなので、
     * 別構造で spawn されたデコイ人形もこの圏内に入り得る（→ {@code use()} が
     * 耐久を減らさずに早期 return し、復元アサーションが偶発赤になる）。
     * そこで実行前に圏内のデコイ人形を消す。
     *
     * <p>あわせて「消した後に本当に居ない」ことを確認する。ここが赤くなるなら
     * ガードの半径か mock player の位置が変わったということなので、原因を隠さず落とす。
     */
    private static void clearDecoyDollsNearMockPlayers(GameTestHelper helper, Player player) {
        AABB guardArea = player.getBoundingBox().inflate(40.0D, 40.0D, 40.0D);
        helper.getLevel().getEntitiesOfClass(EntityCursedDecoyDoll.class, guardArea)
                .forEach(Entity::discard);

        List<EntityCursedDecoyDoll> remaining =
                helper.getLevel().getEntitiesOfClass(EntityCursedDecoyDoll.class, guardArea);
        helper.assertTrue(remaining.isEmpty(),
                "使用ガード（40 格）の圏内にデコイ人形が残っている: " + remaining.size() + " 体");
    }

    /**
     * 変換台が産出時に素材を {@code ORIGINAL_ITEM} として記録すること。
     *
     * <p>現存する 2 条转换（陰陽玉 / 隙間）で検証する。1.7.10 の
     * {@code if (originID >= 0) nbt.setInteger("OriginalItemID", originID)} の一般化。
     */
    @GameTest(template = TEMPLATE)
    public static void malletRecordsOriginalItem(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        MiracleMalletMenu menu = new MiracleMalletMenu(0, player.getInventory());

        // 陰陽玉 → 血に餓えた陰陽玉
        ItemStack orb = new ItemStack(ModItems.YIN_YANG_ORB.get());
        menu.craftMaterial.setItem(0, orb);
        menu.slotsChanged(menu.craftMaterial);

        ItemStack bloodOrb = menu.getResultItem();
        helper.assertFalse(bloodOrb.isEmpty(), "変換台が陰陽玉から何も産出していない");
        helper.assertTrue(bloodOrb.is(ModItems.BLOODTHIRSTY_YIN_YANG_ORB.get()),
                "陰陽玉の変換結果が血に餓えた陰陽玉でない（変換表が変わった？）: " + bloodOrb);

        Holder<Item> recorded = bloodOrb.get(ModDataComponents.ORIGINAL_ITEM.get());
        helper.assertTrue(recorded != null, "ORIGINAL_ITEM が記録されていない（P11 の要）");
        helper.assertTrue(recorded.is(ModItems.YIN_YANG_ORB.get().builtInRegistryHolder()),
                "ORIGINAL_ITEM が素材（陰陽玉）を指していない: " + recorded);
        // 陰陽玉は耐久を持たない（durability 未設定）ので旧 damage は 0。
        // 「素材の damage がそのまま入る」ことは下の隙間（同じく非耐久）でも同じ 0 になるため、
        // ここでは「コンポーネントが必ず載る（欠落ではない）」ことを has() で固定する
        // （getOrDefault(…, 0) は欠落でも 0 を返すので、それだけでは書込みの有無を区別できない）。
        helper.assertTrue(bloodOrb.has(ModDataComponents.ORIGINAL_DAMAGE.get()),
                "ORIGINAL_DAMAGE が欠落している（getOrDefault の 0 と区別できない）");
        helper.assertValueEqual(bloodOrb.getOrDefault(ModDataComponents.ORIGINAL_DAMAGE.get(), 0),
                orb.getDamageValue(), "ORIGINAL_DAMAGE が素材の damage と一致しない");

        // 隙間 → 隙間の折りたたみ傘
        menu.craftMaterial.setItem(0, new ItemStack(ModItems.GAP.get()));
        menu.slotsChanged(menu.craftMaterial);

        ItemStack umbrella = menu.getResultItem();
        helper.assertFalse(umbrella.isEmpty(), "変換台が隙間から何も産出していない");
        helper.assertTrue(umbrella.is(ModItems.GAP_FOLDING_UMBRELLA.get()),
                "隙間の変換結果が隙間の折りたたみ傘でない: " + umbrella);
        Holder<Item> recorded2 = umbrella.get(ModDataComponents.ORIGINAL_ITEM.get());
        helper.assertTrue(recorded2 != null && recorded2.is(ModItems.GAP.get().builtInRegistryHolder()),
                "ORIGINAL_ITEM が隙間を指していない: " + recorded2);

        helper.succeed();
    }

    /**
     * 耐久を使い切ったデコイ人形が {@code ORIGINAL_ITEM} / {@code ORIGINAL_DAMAGE} の物品へ戻ること。
     *
     * <p>手順は原作と同じ「耐久 1 を消費する」経路（{@code use()}）を通す。
     * 1.21.1 の {@code hurtAndBreak} は使い切った時点でスタックを空にするので、
     * 空になったスタックからコンポーネントを読んで復元する（実装側の分岐と一致）。
     */
    @GameTest(template = TEMPLATE)
    public static void decoyDollRestoresOriginalOnBreak(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        clearDecoyDollsNearMockPlayers(helper, player);

        ItemStack doll = new ItemStack(ModItems.CURSED_DECOY_DOLL.get());
        // 素材 = 楼観剣（damage 5）とする。アイテムは何でもよいが、还原先が明確なものを使う。
        doll.set(ModDataComponents.ORIGINAL_ITEM.get(), ModItems.ROUKANKEN.get().builtInRegistryHolder());
        doll.set(ModDataComponents.ORIGINAL_DAMAGE.get(), 5);
        // 残り耐久 1 にして、次の使用で使い切る
        doll.setDamageValue(doll.getMaxDamage() - 1);

        // 読取タイミングを固定する: コンポーネントは use()（= hurtAndBreak → shrink）の**前に**
        // 読めること。空スタックは getComponents() が EMPTY を返すため、この前提が崩れると
        // 製品コードの復元もテストの期待値も同時に壊れる（回帰を早期に落とす）。
        Holder<Item> preItem = doll.get(ModDataComponents.ORIGINAL_ITEM.get());
        int preDamage = doll.getOrDefault(ModDataComponents.ORIGINAL_DAMAGE.get(), -1);
        helper.assertTrue(preItem != null && preItem.is(ModItems.ROUKANKEN.get().builtInRegistryHolder()),
                "use() 前に ORIGINAL_ITEM が読めない（前提条件が崩れている）: " + preItem);
        helper.assertValueEqual(preDamage, 5, "use() 前に ORIGINAL_DAMAGE が読めない");

        player.setItemInHand(InteractionHand.MAIN_HAND, doll);
        doll.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);

        ItemStack inHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertFalse(inHand.isEmpty(),
                "耐久を使い切ったデコイ人形が空になった（ORIGINAL_ITEM を読んで復元できていない）");
        helper.assertTrue(inHand.is(ModItems.ROUKANKEN.get()),
                "復元されたアイテムが素材（楼観剣）でない: " + inHand);
        helper.assertValueEqual(inHand.getDamageValue(), 5,
                "復元時に ORIGINAL_DAMAGE の旧 damage が反映されていない");

        helper.succeed();
    }

    /**
     * 負対照：{@code ORIGINAL_ITEM} が無いデコイ人形は復元しない（空になる）。
     *
     * <p>原作で {@code OriginalItemID} が書かれていない場合と同じ挙動。
     * このテストが赤になる＝「コンポーネント無しでも何かが復元される」＝余計な物品が出る回帰。
     */
    @GameTest(template = TEMPLATE)
    public static void decoyDollWithoutComponentDoesNotRestore(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        clearDecoyDollsNearMockPlayers(helper, player);

        ItemStack doll = new ItemStack(ModItems.CURSED_DECOY_DOLL.get());
        helper.assertFalse(doll.has(ModDataComponents.ORIGINAL_ITEM.get()),
                "新品のデコイ人形に ORIGINAL_ITEM が付いている（前提条件が崩れている）");
        doll.setDamageValue(doll.getMaxDamage() - 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, doll);
        doll.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);

        ItemStack inHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(inHand.isEmpty(),
                "ORIGINAL_ITEM が無いのに何かが復元された（余計な物品が出る回帰）: " + inHand);

        helper.succeed();
    }

    /**
     * 負対照（空 Holder 版）：{@code ORIGINAL_ITEM} に {@code minecraft:air} が入っていても復元しない。
     *
     * <p>1.7.10 の {@code originID == 0}（= air）で {@code getItemById} が空を返すケースの等価物。
     * 実装側の {@code original.is(AIR)} ガードが効いていることを固定する。
     */
    @GameTest(template = TEMPLATE)
    public static void decoyDollWithAirOriginalDoesNotRestore(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        clearDecoyDollsNearMockPlayers(helper, player);

        ItemStack doll = new ItemStack(ModItems.CURSED_DECOY_DOLL.get());
        doll.set(ModDataComponents.ORIGINAL_ITEM.get(), Items.AIR.builtInRegistryHolder());
        doll.setDamageValue(doll.getMaxDamage() - 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, doll);
        doll.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "ORIGINAL_ITEM=air で復元された（ガードが効いていない）");

        helper.succeed();
    }

    /**
     * コンポーネントの永続・ネットワーク両コーデックが {@code Holder<Item>} を往復できること。
     *
     * <p>ORIGINAL_ITEM は新規コンポーネントなので、保存・同期の両経路をここで固定する
     * （既存の Integer 系は {@link IntegerDataComponentBoundaryTest} が担当）。
     */
    @GameTest(template = TEMPLATE)
    public static void originalItemComponentRoundTrip(GameTestHelper helper) {
        var holder = ModDataComponents.ORIGINAL_ITEM;
        helper.assertTrue(holder.isBound(), "ModDataComponents.ORIGINAL_ITEM が bind されていない");
        var type = holder.get();

        ItemStack stack = new ItemStack(ModItems.CURSED_DECOY_DOLL.get());
        helper.assertFalse(stack.has(type), "新品スタックに ORIGINAL_ITEM が付いている");
        helper.assertTrue(stack.get(type) == null, "欠落時の get() は null のはず（getOrDefault を使う根拠）");

        Holder<Item> target = ModItems.ROUKANKEN.get().builtInRegistryHolder();
        stack.set(type, target);
        Holder<Item> read = stack.get(type);
        helper.assertTrue(read != null && read.is(target), "ItemStack get/set で Holder が保たれていない: " + read);

        // 永続コーデック（NBT = 実際の保存経路）
        var nbt = type.codecOrThrow().encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, target)
                .getOrThrow(msg -> new net.minecraft.gametest.framework.GameTestAssertException(
                        "ORIGINAL_ITEM の NBT encode に失敗: " + msg));
        Holder<Item> fromNbt = type.codecOrThrow().parse(net.minecraft.nbt.NbtOps.INSTANCE, nbt)
                .getOrThrow(msg -> new net.minecraft.gametest.framework.GameTestAssertException(
                        "ORIGINAL_ITEM の NBT decode に失敗: " + msg));
        helper.assertTrue(fromNbt.is(target), "永続コーデックの NBT 往復で別のアイテムになった: " + fromNbt);

        // ネットワークコーデック（RegistryFriendlyByteBuf）
        net.minecraft.core.RegistryAccess registries = helper.getLevel().registryAccess();
        net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), registries,
                net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
        try {
            type.streamCodec().encode(buf, target);
            Holder<Item> decoded = type.streamCodec().decode(buf);
            helper.assertTrue(decoded.is(target), "ネットワーク往復で別のアイテムになった: " + decoded);
            helper.assertValueEqual(buf.readableBytes(), 0, "ネットワーク往復で余剰バイトが残った");
        } finally {
            buf.release();
        }

        helper.succeed();
    }
}
