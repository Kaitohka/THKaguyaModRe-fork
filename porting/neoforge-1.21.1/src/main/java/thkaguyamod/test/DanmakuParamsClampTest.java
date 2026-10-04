package thkaguyamod.test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.DanmakuParams;

/**
 * FIX 批（fix-p35-36）回归：{@link DanmakuParams} の値域夾緊。
 *
 * <h2>为什么需要它</h2>
 * <p>1.7.10 は各パラメータを NBT の short/byte に書いていたため、<b>型そのものが値域を切っていた</b>
 * （{@code Number}=short、{@code DanmakuForm}/{@code Speed}/{@code Gravity}/{@code Color}=byte）。
 * 移植では保存形式が int になり、この暗黙の切捨て保護が失われた。書き込み経路は
 * 合成台 2 種（{@code DanmakuCraftingMenu} / {@code DanmakuLaserCraftingMenu}）、
 * モブドロップ（{@code EntityDanmakuMob#dropShotItem}）、コーデック、ネットワークの 4 系統あり、
 * いずれかが負値・過大値を書くと downstream の配列添字
 * （{@code THShotLib.SIZE/DAMAGE[form % 32]}、{@code ShotData.shot} の {@code form % 32} /
 * {@code color % 16}、{@code rainbowPattern[color % 7]}、{@code dropShotItem} の負の剰余）
 * で例外や意図しない色になる。
 *
 * <p>本テストは「コンストラクタが唯一の関門」であることを固定する：
 * <ol>
 *   <li>各フィールドの負値・過大値が上界／下界へ夾緊される；</li>
 *   <li>夾緊は<b>冪等</b>（夾緊後の値をもう一度通しても変わらない）；</li>
 *   <li>夾緊後の値は JSON / NBT / ByteBuf の 3 経路で往復して一致する
 *       （＝保存・同期を経ても夾緊済みの値が保たれる）；</li>
 *   <li>合法域の代表値（{@code RED=0}、{@code WHITE=7}、{@code RANDOM=8}、{@code RAINBOW=9}、
 *       {@code FORM_KNIFE=28}、{@code GUNGNIR=285}、{@code SPECIAL_GAGOUJI01=4020}）は
 *       <b>潰されない</b>（夾緊しすぎの回帰検出）。</li>
 * </ol>
 *
 * <p>上界の根拠は {@link DanmakuParams#MAX_NUMBER} 等の定数 Javadoc を参照。
 * テンプレートの約定は {@link RegistryCompletenessTest} を参照（{@code empty}）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DanmakuParamsClampTest {

    private static final String TEMPLATE = "empty";

    private DanmakuParamsClampTest() {}

    /**
     * 各フィールドの負値／過大値が上界・下界へ夾緊されること。
     * <p>夾緊前の生値は {@code Integer.MIN_VALUE} / {@code Integer.MAX_VALUE} を含む。
     */
    @GameTest(template = TEMPLATE)
    public static void outOfRangeValuesAreClamped(GameTestHelper helper) {
        // 全部まとめて最悪値：すべて下界側へ落ちるはず
        DanmakuParams low = new DanmakuParams(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
                Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, false);
        helper.assertValueEqual(low.number(), 0, "number の下界夾緊");
        helper.assertValueEqual(low.form(), 0, "form の下界夾緊");
        helper.assertValueEqual(low.speed(), 0, "speed の下界夾緊");
        helper.assertValueEqual(low.gravity(), 0, "gravity の下界夾緊");
        helper.assertValueEqual(low.special(), 0, "special の下界夾緊");
        helper.assertValueEqual(low.color(), 0, "color の下界夾緊");

        // 全部まとめて最悪値：すべて上界側へ落ちるはず
        DanmakuParams high = new DanmakuParams(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        helper.assertValueEqual(high.number(), DanmakuParams.MAX_NUMBER, "number の上界夾緊");
        helper.assertValueEqual(high.form(), DanmakuParams.MAX_FORM, "form の上界夾緊");
        helper.assertValueEqual(high.speed(), DanmakuParams.MAX_SPEED, "speed の上界夾緊");
        helper.assertValueEqual(high.gravity(), DanmakuParams.MAX_GRAVITY, "gravity の上界夾緊");
        helper.assertValueEqual(high.special(), DanmakuParams.MAX_SPECIAL, "special の上界夾緊");
        helper.assertValueEqual(high.color(), DanmakuParams.MAX_COLOR, "color の上界夾緊");
        helper.assertTrue(high.infinity(), "infinity は夾緊の対象外（boolean）");

        // 個別の代表値（境界のすぐ外）
        helper.assertValueEqual(new DanmakuParams(-1, 0, 0, 0, 0, 0, false).number(), 0, "number=-1 → 0");
        helper.assertValueEqual(new DanmakuParams(0, -1, 0, 0, 0, 0, false).form(), 0, "form=-1 → 0（負の剰余で配列添字が壊れる経路）");
        helper.assertValueEqual(new DanmakuParams(0, 32, 0, 0, 0, 0, false).form(), DanmakuParams.MAX_FORM, "form=32 → 31");
        helper.assertValueEqual(new DanmakuParams(0, 0, 65, 0, 0, 0, false).speed(), DanmakuParams.MAX_SPEED, "speed=65 → 64");
        helper.assertValueEqual(new DanmakuParams(0, 0, 0, 17, 0, 0, false).gravity(), DanmakuParams.MAX_GRAVITY, "gravity=17 → 16");
        helper.assertValueEqual(new DanmakuParams(0, 0, 0, 0, 0, 16, false).color(), DanmakuParams.MAX_COLOR, "color=16 → 15");
        helper.succeed();
    }

    /**
     * 夾緊が冪等であること（= 夾緊済みの値を再度通しても変わらない）。
     * <p>コーデックは「夾緊済みの値を書く → 読むときに再度コンストラクタを通る」ため、
     * ここが崩れると往復一致が壊れる。
     */
    @GameTest(template = TEMPLATE)
    public static void clampingIsIdempotent(GameTestHelper helper) {
        DanmakuParams clamped = new DanmakuParams(Integer.MIN_VALUE, 999, -5, 999, 999999, 999, true);
        DanmakuParams again = new DanmakuParams(clamped.number(), clamped.form(), clamped.speed(),
                clamped.gravity(), clamped.special(), clamped.color(), clamped.infinity());
        helper.assertValueEqual(again, clamped, "夾緊の冪等性");
        helper.succeed();
    }

    /**
     * 夾緊後の値が JSON / NBT / ByteBuf の 3 経路で往復一致すること。
     * <p>「夾緊はコンストラクタにしか無い」ので、復号側でも同じ夾緊が効いていることを確かめる
     * （効いていなければ、過大値を書いた JSON を読んだときに元の過大値へ戻ってしまう）。
     */
    @GameTest(template = TEMPLATE)
    public static void clampedValuesSurviveAllCodecs(GameTestHelper helper) {
        DanmakuParams clamped = new DanmakuParams(Integer.MAX_VALUE, -1, 16384, -16385, 2147483647, 99, true);

        // JSON
        JsonElement json = DanmakuParams.CODEC.encodeStart(JsonOps.INSTANCE, clamped)
                .getOrThrow(m -> new GameTestAssertException("JSON encode 失敗: " + m));
        DanmakuParams fromJson = DanmakuParams.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(m -> new GameTestAssertException("JSON decode 失敗: " + m));
        helper.assertValueEqual(fromJson, clamped, "夾緊値の JSON 往復 (json=" + json + ")");

        // NBT（実際の永続化経路）
        Tag nbt = DanmakuParams.CODEC.encodeStart(NbtOps.INSTANCE, clamped)
                .getOrThrow(m -> new GameTestAssertException("NBT encode 失敗: " + m));
        DanmakuParams fromNbt = DanmakuParams.CODEC.parse(NbtOps.INSTANCE, nbt)
                .getOrThrow(m -> new GameTestAssertException("NBT decode 失敗: " + m));
        helper.assertValueEqual(fromNbt, clamped, "夾緊値の NBT 往復 (nbt=" + nbt + ")");

        // ByteBuf（ネットワーク同期経路）
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            DanmakuParams.STREAM_CODEC.encode(buf, clamped);
            DanmakuParams fromStream = DanmakuParams.STREAM_CODEC.decode(buf);
            helper.assertValueEqual(fromStream, clamped, "夾緊値の StreamCodec 往復");
            helper.assertValueEqual(buf.readableBytes(), 0, "STREAM_CODEC は書いた分だけ消費するはず");
        } finally {
            buf.release();
        }
        helper.succeed();
    }

    /**
     * 合法域の代表値が潰されないこと（夾緊しすぎの回帰検出）。
     *
     * <p>特に重要なのは色：{@code 8=RANDOM} / {@code 9=RAINBOW} は「発射時に解決される特別値」で、
     * 0-7 に潰すと合成台の「火薬=ランダム / ダイヤ=虹色」が黙って別の色になる。
     * また {@code FORM_KNIFE=28} は {@code SIZE}/{@code DAMAGE} の 32 要素表に実在し、
     * {@code SPECIAL_GAGOUJI01=4020} は実際にモブドロップで使われる最大の特殊弾 ID。
     */
    @GameTest(template = TEMPLATE)
    public static void legalValuesAreNotCollapsed(GameTestHelper helper) {
        DanmakuParams legal = new DanmakuParams(
                DanmakuParams.MAX_NUMBER,      // 512（ModConfig の上限）
                DanmakuConstants.FORM_KNIFE,   // 28
                DanmakuParams.MAX_SPEED,       // 64
                DanmakuParams.MAX_GRAVITY,     // 16
                4020,                          // SPECIAL_GAGOUJI01
                DanmakuConstants.RAINBOW,      // 9
                false);
        helper.assertValueEqual(legal.number(), DanmakuParams.MAX_NUMBER, "合法上界の number はそのまま");
        helper.assertValueEqual(legal.form(), 28, "FORM_KNIFE(28) は潰されない");
        helper.assertValueEqual(legal.speed(), 64, "合法上界の speed はそのまま");
        helper.assertValueEqual(legal.gravity(), 16, "合法上界の gravity はそのまま");
        helper.assertValueEqual(legal.special(), 4020, "SPECIAL_GAGOUJI01(4020) は潰されない");
        helper.assertValueEqual(legal.color(), 9, "RAINBOW(9) は 0-7 に潰されない");

        // 8=RANDOM と 9=RAINBOW は特別値としてそのまま残る
        helper.assertValueEqual(new DanmakuParams(1, 0, 0, 0, 0, DanmakuConstants.RANDOM, false).color(),
                DanmakuConstants.RANDOM, "RANDOM(8) はそのまま");
        helper.assertValueEqual(new DanmakuParams(1, 0, 0, 0, 0, DanmakuConstants.RAINBOW, false).color(),
                DanmakuConstants.RAINBOW, "RAINBOW(9) はそのまま");
        // 実色 0-7 もすべてそのまま
        for (int c = DanmakuConstants.RED; c <= DanmakuConstants.WHITE; c++) {
            helper.assertValueEqual(new DanmakuParams(1, 0, 0, 0, 0, c, false).color(), c,
                    "実色 " + c + " はそのまま");
        }

        // DEFAULT は夾緊の影響を受けない（既定値が変わると合成台の初期状態が変わる）
        helper.assertValueEqual(DanmakuParams.DEFAULT, new DanmakuParams(1, 0, 0, 0, 0, 0, false),
                "DEFAULT は夾緊後も同じ");
        helper.succeed();
    }

    /**
     * 夾緊後の値が downstream の表添字として安全であること（夾緊の目的そのもの）。
     * <p>{@code THShotLib.SIZE}/{@code DAMAGE} は 32 要素で、{@code ShotData.shot} は
     * {@code form % 32} / {@code color % 16} で引く。負値が通ると {@code -1 % 32 == -1} で
     * 添字が負になり得るため、夾緊後は必ず 0 以上になることを固定する。
     */
    @GameTest(template = TEMPLATE)
    public static void clampedFormAndColorAreSafeAsTableIndex(GameTestHelper helper) {
        int[] rawForms = {Integer.MIN_VALUE, -1, 0, 31, 32, Integer.MAX_VALUE};
        for (int raw : rawForms) {
            int form = new DanmakuParams(1, raw, 0, 0, 0, 0, false).form();
            helper.assertTrue(form >= 0 && form < 32,
                    "form=" + raw + " の夾緊結果 " + form + " が THShotLib.SIZE/DAMAGE(32 要素) の添字域外");
        }
        int[] rawColors = {Integer.MIN_VALUE, -1, 0, 15, 16, Integer.MAX_VALUE};
        for (int raw : rawColors) {
            int color = new DanmakuParams(1, 0, 0, 0, 0, raw, false).color();
            helper.assertTrue(color >= 0 && color < 16,
                    "color=" + raw + " の夾緊結果 " + color + " が THShotLib.COLOR_NAME(16 要素) の添字域外");
        }
        helper.succeed();
    }
}
