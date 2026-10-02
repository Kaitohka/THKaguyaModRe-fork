package thkaguyamod.danmaku;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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
