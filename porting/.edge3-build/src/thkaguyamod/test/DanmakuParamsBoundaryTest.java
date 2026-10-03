package thkaguyamod.test;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.DanmakuParams;

/**
 * 階段 5 品質基建（gametest-edge）：{@link DanmakuParams} 編解碼の<b>境界値</b>往復テスト。
 *
 * <p>{@link DanmakuParamsCodecTest} が「3 経路（JSON / ByteBuf / DataComponent）が
 * 一致して往復すること」を見ているのに対し、こちらは<b>値の端</b>を突く：
 * <ul>
 *   <li>色の合法端 0（RED）と 7（WHITE）、および不正値 8/9/16/負値も往復できること；</li>
 *   <li>way 数 0（1 発も出ない設定）が往復で消えないこと；</li>
 *   <li>符号付きフィールドの負値（{@code Integer.MIN_VALUE} を含む）；</li>
 *   <li>VarInt の多バイト境界（127/128/16383/16384/2^31-1/-1）；</li>
 *   <li>JSON だけでなく <b>NBT</b>（{@link NbtOps}）経由でも同一に往復すること
 *       — アイテムの永続化は実際には NBT 経由なので、JSON だけの検証では穴が残る。</li>
 * </ul>
 *
 * <h2>「size 0 / 極大 float」について（重要な仕様確認の結果）</h2>
 * <p>{@code DanmakuParams} の record 成分は
 * {@code (int number, int form, int speed, int gravity, int special, int color, boolean infinity)}
 * であり、<b>size と float のフィールドは存在しない</b>（1.7.10 の NBT キーは
 * Number/DanmakuForm/Speed/Gravity/Special/Color/Infinity で、size は {@code ShotData} 側、
 * 弹サイズは {@code THShotLib.SIZE[form]} から形状経由で決まる）。したがって
 * 浮動小数点の丸めや {@code NaN} の往復はこのクラスの責務外であり、
 * 本テストでは「存在しないフィールドを要求したときコーデックが additional として黙って落とす」
 * こと（= 未知フィールドはエラーにならない）を代わりに固定する。
 *
 * <p>テンプレートの約定は {@link RegistryCompletenessTest} を参照（{@code empty}）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DanmakuParamsBoundaryTest {

    private static final String TEMPLATE = "empty";

    /** 色の合法端（0=RED / 7=WHITE）＋境界外（8=RANDOM / 9=RAINBOW / 16 / 負値）。 */
    private static final int[] COLORS = {0, 7, 8, 9, 16, -1, Integer.MIN_VALUE};

    /** 数値フィールドの境界（0 / ±1 / VarInt 境界 / int 端）。 */
    private static final int[] EDGE_INTS = {
            0, 1, -1, 127, 128, 16383, 16384, 2147483647, -2147483648};

    private DanmakuParamsBoundaryTest() {}

    /**
     * 色の端と境界外：JSON / NBT / ByteBuf の 3 経路で往復し、値が変わらないこと。
     */
    @GameTest(template = TEMPLATE)
    public static void colorBoundariesRoundTripOnAllCodecs(GameTestHelper helper) {
        for (int color : COLORS) {
            DanmakuParams params = new DanmakuParams(1, 0, 0, 0, 0, color, false);
            assertAllCodecsRoundTrip(helper, params, "color=" + color);
        }

        // 合法端は DanmakuConstants の定数と一致していること（数値の取り違え検出）
        assertAllCodecsRoundTrip(helper, new DanmakuParams(1, 0, 0, 0, 0, DanmakuConstants.RED, false), "RED=0");
        assertAllCodecsRoundTrip(helper, new DanmakuParams(1, 0, 0, 0, 0, DanmakuConstants.WHITE, false), "WHITE=7");
        helper.succeed();
    }

    /**
     * 各フィールドを境界値で 1 つずつ動かして 3 経路往復。
     * way 数 0（弾を出さない設定）が「未設定」に化けないことも含む。
     */
    @GameTest(template = TEMPLATE)
    public static void perFieldEdgeValuesRoundTrip(GameTestHelper helper) {
        for (int value : EDGE_INTS) {
            // number（way 数。0 は「1 発も出さない」有効値）
            assertAllCodecsRoundTrip(helper, new DanmakuParams(value, 0, 0, 0, 0, 0, false), "number=" + value);
            // form（弹形状 / レーザー発射陣形）
            assertAllCodecsRoundTrip(helper, new DanmakuParams(1, value, 0, 0, 0, 0, false), "form=" + value);
            // speed / gravity（ステータス加減算値）
            assertAllCodecsRoundTrip(helper, new DanmakuParams(1, 0, value, 0, 0, 0, false), "speed=" + value);
            assertAllCodecsRoundTrip(helper, new DanmakuParams(1, 0, 0, value, 0, 0, false), "gravity=" + value);
            // special（特殊弹 ID。285=GUNGNIR のような 3 桁）
            assertAllCodecsRoundTrip(helper, new DanmakuParams(1, 0, 0, 0, value, 0, false), "special=" + value);
        }
        helper.succeed();
    }

    /**
     * 全フィールド最悪値の同時往復（フィールド取り違えの検出）。
     * <p>各フィールドに<b>異なる</b>値を入れるので、CODEC でフィールド順を入れ替えると必ず落ちる。
     * infinity も同時に true にして boolean の位置ずれも見る。
     */
    @GameTest(template = TEMPLATE)
    public static void allFieldsDistinctExtremesRoundTrip(GameTestHelper helper) {
        DanmakuParams params = new DanmakuParams(
                0,                                  // number
                -2147483648,                        // form
                16384,                              // speed
                -16385,                             // gravity
                2147483647,                         // special
                7,                                  // color
                true);                              // infinity
        assertAllCodecsRoundTrip(helper, params, "全フィールド異値・極値");
        helper.succeed();
    }

    /**
     * infinity の真偽が他のフィールドに混ざらないこと（boolean は 1 bit なので
     * ByteBuf 経路では「読み忘れ」が最も起きやすい）。
     */
    @GameTest(template = TEMPLATE)
    public static void infinityFlagDoesNotLeakIntoOtherFields(GameTestHelper helper) {
        for (boolean infinity : new boolean[] {false, true}) {
            DanmakuParams params = new DanmakuParams(3, 9, 2, -1, 285, 5, infinity);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                DanmakuParams.STREAM_CODEC.encode(buf, params);
                int written = buf.readableBytes();
                helper.assertValueEqual(written,
                        varIntSize(params.number()) + varIntSize(params.form()) + varIntSize(params.speed())
                                + varIntSize(params.gravity()) + varIntSize(params.special())
                                + varIntSize(params.color()) + 1,
                        "STREAM_CODEC の書込バイト数（6 個の VarInt + 1 byte boolean）infinity=" + infinity);
                DanmakuParams decoded = DanmakuParams.STREAM_CODEC.decode(buf);
                helper.assertValueEqual(decoded, params, "infinity=" + infinity + " の StreamCodec 往復");
                helper.assertValueEqual(buf.readableBytes(), 0,
                        "STREAM_CODEC は書いた分だけ消費するはず（残留 = フィールド数のずれ）");
            } finally {
                buf.release();
            }
        }
        helper.succeed();
    }

    /**
     * 既定値（{@link DanmakuParams#DEFAULT}）が端の値に化けないこと。
     * <p>1.7.10 の合成台は「NBT が無い＝既定値」で動いていたため、既定値の往復は
     * 「タグ欠落時のフォールバック」と等価な意味を持つ。
     */
    @GameTest(template = TEMPLATE)
    public static void defaultParamsRoundTrip(GameTestHelper helper) {
        assertAllCodecsRoundTrip(helper, DanmakuParams.DEFAULT, "DEFAULT");
        helper.assertValueEqual(DanmakuParams.DEFAULT.number(), 1, "DEFAULT の way 数は 1");
        helper.assertValueEqual(DanmakuParams.DEFAULT.color(), DanmakuConstants.RED, "DEFAULT の色は RED(0)");
        helper.assertFalse(DanmakuParams.DEFAULT.infinity(), "DEFAULT は無限弹ではない");
        helper.succeed();
    }

    /**
     * 未知フィールド（size / 浮動小数）はエラーにならず無視されること。
     * <p>{@code DanmakuParams} に size/float 成分が無いことの仕様確認であり、
     * 将来フィールドを足すときに「古い JSON/NBT を読めなくなる」回帰を早期に検出する。
     */
    @GameTest(template = TEMPLATE)
    public static void unknownFieldsAreIgnored(GameTestHelper helper) {
        String json = "{\"number\":2,\"form\":0,\"speed\":3,\"gravity\":0,\"special\":0,\"color\":1,"
                + "\"infinity\":false,\"size\":0.0,\"damage\":1.5e38}";
        JsonElement element = com.google.gson.JsonParser.parseString(json);

        DanmakuParams decoded = DanmakuParams.CODEC.parse(JsonOps.INSTANCE, element)
                .getOrThrow(message -> new GameTestAssertException("未知フィールド付き JSON の decode に失敗: " + message));
        helper.assertValueEqual(decoded, new DanmakuParams(2, 0, 3, 0, 0, 1, false),
                "size/damage などの未知フィールドは無視され、既知フィールドだけが復元されるはず");
        helper.succeed();
    }

    /* ------------------------------------------------------------------ */

    /** JSON / NBT / ByteBuf の 3 経路すべてで往復一致を検査する。 */
    private static void assertAllCodecsRoundTrip(GameTestHelper helper, DanmakuParams params, String label) {
        // 1) JSON（DataComponentType#codec / アイテム JSON 経由）
        JsonElement json = DanmakuParams.CODEC.encodeStart(JsonOps.INSTANCE, params)
                .getOrThrow(message -> new GameTestAssertException(label + " の JSON encode に失敗: " + message));
        DanmakuParams fromJson = DanmakuParams.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(message -> new GameTestAssertException(label + " の JSON decode に失敗: " + message));
        helper.assertValueEqual(fromJson, params, label + " の JSON 往復 (json=" + json + ")");

        // 2) NBT（実際のアイテム永続化経路。JSON と書式が違うので別途検査する）
        Tag nbt = DanmakuParams.CODEC.encodeStart(NbtOps.INSTANCE, params)
                .getOrThrow(message -> new GameTestAssertException(label + " の NBT encode に失敗: " + message));
        DanmakuParams fromNbt = DanmakuParams.CODEC.parse(NbtOps.INSTANCE, nbt)
                .getOrThrow(message -> new GameTestAssertException(label + " の NBT decode に失敗: " + message));
        helper.assertValueEqual(fromNbt, params, label + " の NBT 往復 (nbt=" + nbt + ")");

        // 3) ByteBuf（ネットワーク同期経路）
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            DanmakuParams.STREAM_CODEC.encode(buf, params);
            DanmakuParams fromStream = DanmakuParams.STREAM_CODEC.decode(buf);
            helper.assertValueEqual(fromStream, params, label + " の StreamCodec 往復");
            helper.assertValueEqual(buf.readableBytes(), 0, label + " の StreamCodec 残留バイト");
        } finally {
            buf.release();
        }
    }

    /** VarInt のエンコード長（1/2/3/5 バイト）を期待値計算に使う。 */
    private static int varIntSize(int value) {
        if (value >= 0) {
            if (value < 1 << 7) {
                return 1;
            }
            if (value < 1 << 14) {
                return 2;
            }
            if (value < 1 << 21) {
                return 3;
            }
            if (value < 1 << 28) {
                return 4;
            }
            return 5;
        }
        return 5; // 負値は常に 5 バイト
    }
}