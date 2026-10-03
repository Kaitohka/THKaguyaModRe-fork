package thkaguyamod.test;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.init.ModDataComponents;

/**
 * 階段 5 品質基建（gametest-edge）：整数データコンポーネント 3 種の境界テスト。
 *
 * <p>対象は 1.7.10 の NBT キーをそのまま置き換えた Integer コンポーネント
 * （いずれも {@code persistent(Codec.INT)} + {@code networkSynchronized(ByteBufCodecs.VAR_INT)}、
 * 定義は {@link ModDataComponents}）：
 * <ul>
 *   <li>{@link ModDataComponents#CHARGE} — 早苗符卡の「奇跡の力」（旧 NBT int {@code Charge}）。
 *       0 以下は「宣言不可」で、{@code ItemTHSpellCard#inventoryTick} が 1 以下で
 *       コンポーネントごと削除する（= <b>0 が「未設定」と同義になる境界</b>）；</li>
 *   <li>{@link ModDataComponents#GAP_COLOR} — 隙間折りたたみ傘の染色色（旧 NBT byte {@code GapColor}）。
 *       0 が「未染色」の既定値で、{@code getOrDefault(…, 0)} で読まれる；</li>
 *   <li>{@link ModDataComponents#ORIGINAL_DAMAGE} — 奇跡の槌で戻すための元ダメージ値
 *       （旧 NBT {@code OriginalDamage}）。0 は「新品」を意味する。</li>
 * </ul>
 *
 * <p>「境界」として固定するのは 4 点：
 * <ol>
 *   <li>0 / 1 / 上限値（{@code Integer.MAX_VALUE}）/ 負値が、コンポーネント永続コーデック（NBT 経由）
 *       とネットワークコーデック（VarInt）の両方で往復すること；</li>
 *   <li><b>欠落時の {@code getOrDefault} 意味論</b> — コンポーネント無しの {@link ItemStack} で
 *       {@code get()} は null（NPE 源）、{@code getOrDefault(type, 0)} は 0 を返す。
 *       mod 側が実際に使っているのは後者であることを、両方の実挙動として記録する；</li>
 *   <li>0 の明示設定と欠落が {@code has()} で区別できること（旧 NBT の「タグ有無」に相当）；</li>
 *   <li>{@code ItemStack} の NBT 保存 → 読み込み往復で値が保たれること（実アイテム経路）。</li>
 * </ol>
 *
 * <p>テンプレートの約定は {@link RegistryCompletenessTest} を参照（{@code empty}）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IntegerDataComponentBoundaryTest {

    private static final String TEMPLATE = "empty";

    /** 境界値：0（既定扱い） / 1（最小の「有効」） / 上限 / 負値 / int 端。 */
    private static final int[] EDGE_VALUES = {
            0, 1, Integer.MAX_VALUE, -1, Integer.MIN_VALUE, 1000, 65535};

    private IntegerDataComponentBoundaryTest() {}

    /**
     * CHARGE：0 と上限・負値の往復（永続 / ネットワーク両経路）。
     */
    @GameTest(template = TEMPLATE)
    public static void chargeComponentBoundariesRoundTrip(GameTestHelper helper) {
        DataComponentType<Integer> type = boundType(helper, ModDataComponents.CHARGE, "charge");
        for (int value : EDGE_VALUES) {
            assertStackComponentRoundTrip(helper, type, value, "charge=" + value);
        }
        // 宣言ガードは「getOrDefault(..., 0) <= 0」なので、0 は明示設定でも欠落でも同じ扱いになる
        ItemStack stack = new ItemStack(Items.STICK);
        helper.assertValueEqual(stack.getOrDefault(type, 0), 0,
                "CHARGE 欠落時の getOrDefault(…, 0) は 0（宣言ガードはこの 0 を『充填なし』として弾く）");
        stack.set(type, 0);
        helper.assertTrue(stack.getOrDefault(type, 0) <= 0,
                "CHARGE=0 を明示設定しても宣言ガードは同じく弾く（1.7.10 の Charge<=0 と等価）");
        helper.succeed();
    }

    /**
     * GAP_COLOR：0（未染色）と上限・負値の往復。
     */
    @GameTest(template = TEMPLATE)
    public static void gapColorComponentBoundariesRoundTrip(GameTestHelper helper) {
        DataComponentType<Integer> type = boundType(helper, ModDataComponents.GAP_COLOR, "gap_color");
        for (int value : EDGE_VALUES) {
            assertStackComponentRoundTrip(helper, type, value, "gap_color=" + value);
        }
        ItemStack stack = new ItemStack(Items.STICK);
        helper.assertValueEqual(stack.getOrDefault(type, 0), 0,
                "GAP_COLOR 欠落時の getOrDefault(…, 0) は 0（= 未染色。ItemGapFoldingUmbrella の読み方）");
        helper.succeed();
    }

    /**
     * ORIGINAL_DAMAGE：0（新品）と上限・負値の往復。
     */
    @GameTest(template = TEMPLATE)
    public static void originalDamageComponentBoundariesRoundTrip(GameTestHelper helper) {
        DataComponentType<Integer> type = boundType(helper, ModDataComponents.ORIGINAL_DAMAGE, "original_damage");
        for (int value : EDGE_VALUES) {
            assertStackComponentRoundTrip(helper, type, value, "original_damage=" + value);
        }
        ItemStack stack = new ItemStack(Items.STICK);
        helper.assertValueEqual(stack.getOrDefault(type, 0), 0,
                "ORIGINAL_DAMAGE 欠落時の getOrDefault(…, 0) は 0（= 元ダメージ 0 として復元される）");
        helper.succeed();
    }

    /**
     * 3 コンポーネント共通の境界意味論：
     * <ul>
     *   <li>欠落 → {@code has()==false}、{@code get()} は null、{@code getOrDefault(…, 0)} は 0；</li>
     *   <li>0 を明示設定 → {@code has()==true}、{@code get()==0}（欠落と区別できる）；</li>
     *   <li>{@code remove()} で欠落状態に戻る。</li>
     * </ul>
     * これは「旧 NBT のタグ有無」→「コンポーネント有無」の読み替えが正しいことの確認であり、
     * 0 を「未設定」と同一視して {@code has()} を判定に使うコードが無いことの防波堤でもある。
     */
    @GameTest(template = TEMPLATE)
    public static void absentAndExplicitZeroAreDistinguishable(GameTestHelper helper) {
        List<String> failures = new ArrayList<>();
        assertAbsentSemantics(helper, ModDataComponents.CHARGE, "charge", failures);
        assertAbsentSemantics(helper, ModDataComponents.GAP_COLOR, "gap_color", failures);
        assertAbsentSemantics(helper, ModDataComponents.ORIGINAL_DAMAGE, "original_damage", failures);
        helper.assertTrue(failures.isEmpty(), "欠落/0 の意味論に失敗: " + failures);
        helper.succeed();
    }

    /**
     * 3 コンポーネントを 1 つの {@link ItemStack} に同時に載せ、ItemStack の NBT 保存 →
     * 読み込み往復で全値が保たれること（互いの key が衝突していないことの確認でもある）。
     */
    @GameTest(template = TEMPLATE)
    public static void allThreeComponentsSurviveItemStackNbt(GameTestHelper helper) {
        DataComponentType<Integer> charge = boundType(helper, ModDataComponents.CHARGE, "charge");
        DataComponentType<Integer> gapColor = boundType(helper, ModDataComponents.GAP_COLOR, "gap_color");
        DataComponentType<Integer> originalDamage = boundType(helper, ModDataComponents.ORIGINAL_DAMAGE, "original_damage");

        ItemStack stack = new ItemStack(Items.STICK);
        stack.set(charge, 0);
        stack.set(gapColor, Integer.MAX_VALUE);
        stack.set(originalDamage, -1);

        RegistryAccess registries = helper.getLevel().registryAccess();
        Tag saved = stack.save(registries);
        ItemStack loaded = ItemStack.parse(registries, saved)
                .orElseThrow(() -> new GameTestAssertException("ItemStack.parse が空を返した（保存 NBT: " + saved + "）"));

        helper.assertValueEqual(loaded.getOrDefault(charge, -999), 0,
                "NBT 往復後の charge（0 が『未設定』に化けていないこと）");
        helper.assertValueEqual(loaded.getOrDefault(gapColor, -999), Integer.MAX_VALUE, "NBT 往復後の gap_color");
        helper.assertValueEqual(loaded.getOrDefault(originalDamage, -999), -1, "NBT 往復後の original_damage");
        helper.assertTrue(loaded.has(charge) && loaded.has(gapColor) && loaded.has(originalDamage),
                "NBT 往復後も 3 コンポーネントすべてが付いているはず");
        helper.succeed();
    }

    /**
     * ネットワーク同期コーデック（{@code networkSynchronized}）を実際に駆動して往復させる。
     * <p>{@code DataComponentType#streamCodec()} は {@code RegistryFriendlyByteBuf} を要求するので、
     * レベルから {@link RegistryAccess} を取って {@link RegistryFriendlyByteBuf} を組む。
     * ここで VarInt の読み書き（{@code ByteBufCodecs.VAR_INT}）が往復することを確認する。
     */
    @GameTest(template = TEMPLATE)
    public static void networkCodecRoundTripsEdgeValues(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        List<String> failures = new ArrayList<>();

        assertNetworkRoundTrip(helper, ModDataComponents.CHARGE, "charge", registries, failures);
        assertNetworkRoundTrip(helper, ModDataComponents.GAP_COLOR, "gap_color", registries, failures);
        assertNetworkRoundTrip(helper, ModDataComponents.ORIGINAL_DAMAGE, "original_damage", registries, failures);

        helper.assertTrue(failures.isEmpty(), "ネットワーク同期コーデックの境界往復に失敗: " + failures);
        helper.succeed();
    }

    /* ------------------------------------------------------------------ */

    /** 登録済みコンポーネントを取得する（未 bind ならその場で失敗させる）。 */
    private static DataComponentType<Integer> boundType(GameTestHelper helper,
            net.neoforged.neoforge.registries.DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> holder,
            String name) {
        helper.assertTrue(holder.isBound(), "ModDataComponents." + name + " が bind されていない（RegisterEvent 未実行）");
        return holder.get();
    }

    /** コンポーネント永続コーデック（JSON / NBT）＋ ItemStack set/get の往復。 */
    private static void assertStackComponentRoundTrip(GameTestHelper helper, DataComponentType<Integer> type,
            int value, String label) {
        // 永続コーデック（NBT = 実際の保存経路）
        Tag nbt = type.codecOrThrow().encodeStart(NbtOps.INSTANCE, value)
                .getOrThrow(message -> new GameTestAssertException(label + " の NBT encode に失敗: " + message));
        Integer fromNbt = type.codecOrThrow().parse(NbtOps.INSTANCE, nbt)
                .getOrThrow(message -> new GameTestAssertException(label + " の NBT decode に失敗: " + message));
        helper.assertValueEqual(fromNbt, value, label + " の永続コーデック NBT 往復 (nbt=" + nbt + ")");

        // 永続コーデック（JSON = データパック / デバッグ経路）
        JsonElement json = type.codecOrThrow().encodeStart(JsonOps.INSTANCE, value)
                .getOrThrow(message -> new GameTestAssertException(label + " の JSON encode に失敗: " + message));
        Integer fromJson = type.codecOrThrow().parse(JsonOps.INSTANCE, json)
                .getOrThrow(message -> new GameTestAssertException(label + " の JSON decode に失敗: " + message));
        helper.assertValueEqual(fromJson, value, label + " の永続コーデック JSON 往復 (json=" + json + ")");

        // ItemStack set/get（実行時に mod が使う経路）
        ItemStack stack = new ItemStack(Items.STICK);
        stack.set(type, value);
        helper.assertValueEqual(stack.get(type), value, label + " の ItemStack get/set 往復");
        helper.assertTrue(stack.has(type), label + " を set したら has() は true のはず");
    }

    /** 欠落 / 0 明示 / remove の意味論を 1 コンポーネントずつ検査する。 */
    private static void assertAbsentSemantics(GameTestHelper helper,
            net.neoforged.neoforge.registries.DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> holder,
            String name, List<String> failures) {
        if (!holder.isBound()) {
            failures.add(name + " が未 bind");
            return;
        }
        DataComponentType<Integer> type = holder.get();
        ItemStack stack = new ItemStack(Items.STICK);

        if (stack.has(type)) {
            failures.add(name + ": 新品 ItemStack にコンポーネントが付いていた");
        }
        if (stack.get(type) != null) {
            failures.add(name + ": 欠落時の get() は null のはず（getOrDefault を使う根拠）");
        }
        if (!Integer.valueOf(0).equals(stack.getOrDefault(type, 0))) {
            failures.add(name + ": 欠落時の getOrDefault(…, 0) は 0 のはず");
        }

        stack.set(type, 0);
        if (!stack.has(type)) {
            failures.add(name + ": 0 を明示 set したら has() は true のはず（欠落と区別できる）");
        }
        if (!Integer.valueOf(0).equals(stack.get(type))) {
            failures.add(name + ": 0 を明示 set したら get() は 0 のはず");
        }

        stack.remove(type);
        if (stack.has(type)) {
            failures.add(name + ": remove() 後は has() が false のはず");
        }
        if (!Integer.valueOf(0).equals(stack.getOrDefault(type, 0))) {
            failures.add(name + ": remove() 後の getOrDefault(…, 0) は 0 のはず");
        }
    }

    /** networkSynchronized コーデックを RegistryFriendlyByteBuf で往復させる。 */
    private static void assertNetworkRoundTrip(GameTestHelper helper,
            net.neoforged.neoforge.registries.DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> holder,
            String name, RegistryAccess registries, List<String> failures) {
        if (!holder.isBound()) {
            failures.add(name + " が未 bind");
            return;
        }
        DataComponentType<Integer> type = holder.get();
        for (int value : EDGE_VALUES) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries,
                    net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
            try {
                type.streamCodec().encode(buf, value);
                int written = buf.readableBytes();
                Integer decoded = type.streamCodec().decode(buf);
                if (!Integer.valueOf(value).equals(decoded)) {
                    failures.add(name + "=" + value + " のネットワーク往復が " + decoded + " になった");
                }
                if (buf.readableBytes() != 0) {
                    failures.add(name + "=" + value + " のネットワーク往復で " + buf.readableBytes()
                            + " バイト残留（書込 " + written + " バイト）");
                }
            } finally {
                buf.release();
            }
        }
    }
}