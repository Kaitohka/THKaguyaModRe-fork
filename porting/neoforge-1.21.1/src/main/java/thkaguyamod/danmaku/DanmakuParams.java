package thkaguyamod.danmaku;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * Danmaku parameters carried by shot/laser items.
 *
 * <p>Replaces the 1.7.10 NBT keys written by the danmaku crafting table:
 * {@code Number}(short), {@code DanmakuForm}(byte), {@code Speed}(byte), {@code Gravity}(byte),
 * {@code Special}(int), {@code Color}(byte), {@code Infinity}(boolean).
 *
 * @param number   way 数（弹幕数量）
 * @param form     弹幕阵型/形状（合成台 3×3 矩阵判定结果；对弹物品为 FORM_*，对激光物品为发射阵型）
 * @param speed    弹速加成
 * @param gravity  重力加成
 * @param special  特殊弹 ID（反弹等，见 {@link DanmakuConstants}）
 * @param color    颜色（见 {@link DanmakuConstants}，8=RANDOM / 9=RAINBOW）
 * @param infinity 无限弹（创造模式不消耗）
 */
public record DanmakuParams(int number, int form, int speed, int gravity, int special, int color, boolean infinity) {

    /*
     * P35（audit-common）: 1.7.10 は各値を NBT の short/byte に書いていたため、
     * 型そのものが値域を切っていた（Number=short, DanmakuForm/Speed/Gravity/Color=byte）。
     * 移植では保存形式が int になったことでその暗黙の切捨て保護が失われ、
     * 負値や過大値がそのまま downstream の配列添字（THShotLib.SIZE/DAMAGE[form%32]、
     * ShotData.shot の form%32/color%16、dropShotItem の負の剰余）へ流れ込む。
     * 書き込み経路が複数（合成台 2 種 / モブドロップ / コーデック / ネットワーク）あるため、
     * 個別に守るのではなく record の compact コンストラクタで一元的に夾緊する。
     * 上界はすべて「実在する合法値域」から決めた（下の定数を参照）。
     */

    /** way 数の上界。{@code ModConfig} の Shot Max Number の上限 512 に一致。 */
    public static final int MAX_NUMBER = 512;
    /** 弾形状の上界。{@code THShotLib.SIZE}/{@code DAMAGE} の要素数 32（{@code ShotData.shot} の {@code form % 32} と同じ）。 */
    public static final int MAX_FORM = 31;
    /** 弾速加算の上界。合成台のクランプ（{@code speed > 64 → 64}）と {@code dropShotItem} の {@code speed % 64} に一致。 */
    public static final int MAX_SPEED = 64;
    /** 重力加算の上界。合成台のクランプ（{@code gravity > 16 → 16}）と {@code dropShotItem} の {@code gravity % 16} に一致。 */
    public static final int MAX_GRAVITY = 16;
    /** 特殊弾 ID の上界。実使用の最大は {@code SPECIAL_GAGOUJI01 = 4020}（1.7.10 も int で保存していた）。 */
    public static final int MAX_SPECIAL = 65535;
    /**
     * 色の上界。{@code ShotData.shot} の {@code color % 16} と {@code THShotLib.COLOR_NAME} の要素数 16 に一致。
     * 0-7 が実色、8=RANDOM / 9=RAINBOW（発射時に解決）なので 0-7 に潰してはいけない。
     */
    public static final int MAX_COLOR = 15;

    /**
     * 全 int 成分を合法域へ夾緊する。
     *
     * <p>1.7.10 の NBT 型（short/byte）が担っていた値域保証の代替。夾緊後の値は
     * そのまま {@code number()}/{@code form()}/… として観測されるため、
     * コーデック往復（JSON/NBT/ByteBuf）は夾緊後の値どうしで一致する。
     */
    public DanmakuParams {
        number = Mth.clamp(number, 0, MAX_NUMBER);
        form = Mth.clamp(form, 0, MAX_FORM);
        speed = Mth.clamp(speed, 0, MAX_SPEED);
        gravity = Mth.clamp(gravity, 0, MAX_GRAVITY);
        special = Mth.clamp(special, 0, MAX_SPECIAL);
        color = Mth.clamp(color, 0, MAX_COLOR);
    }

    public static final DanmakuParams DEFAULT = new DanmakuParams(1, 0, 0, 0, 0, 0, false);

    public static final Codec<DanmakuParams> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("number").forGetter(DanmakuParams::number),
            Codec.INT.fieldOf("form").forGetter(DanmakuParams::form),
            Codec.INT.fieldOf("speed").forGetter(DanmakuParams::speed),
            Codec.INT.fieldOf("gravity").forGetter(DanmakuParams::gravity),
            Codec.INT.fieldOf("special").forGetter(DanmakuParams::special),
            Codec.INT.fieldOf("color").forGetter(DanmakuParams::color),
            Codec.BOOL.fieldOf("infinity").forGetter(DanmakuParams::infinity)
    ).apply(instance, DanmakuParams::new));

    public static final StreamCodec<FriendlyByteBuf, DanmakuParams> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DanmakuParams decode(FriendlyByteBuf buf) {
            return new DanmakuParams(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
        }

        @Override
        public void encode(FriendlyByteBuf buf, DanmakuParams value) {
            buf.writeVarInt(value.number());
            buf.writeVarInt(value.form());
            buf.writeVarInt(value.speed());
            buf.writeVarInt(value.gravity());
            buf.writeVarInt(value.special());
            buf.writeVarInt(value.color());
            buf.writeBoolean(value.infinity());
        }
    };
}
