package thkaguyamod.test;

import java.util.List;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.danmaku.DanmakuParams;
import thkaguyamod.init.ModDataComponents;

/**
 * 阶段 5 质量基建：弹幕参数数据组件（{@link DanmakuParams}）的编解码往返测试。
 *
 * <p>为什么值得测：{@code DanmakuParams} 同时被三种通道使用——物品 NBT（{@link DanmakuParams#CODEC}）、
 * 网络同步（{@link DanmakuParams#STREAM_CODEC}）、以及 {@link ModDataComponents#DANMAKU} 组件的
 * persistent/networkSynchronized 绑定。任何一处字段顺序写错（例如 CODEC 里 number/form 对调、
 * STREAM_CODEC 漏写一个字段）都不会编译报错，只会在存档/联机时静默错位。
 *
 * <p>覆盖：
 * <ol>
 *   <li>{@code CODEC} 经 {@link JsonOps} 序列化→反序列化后相等（含多组取值）；</li>
 *   <li>{@code STREAM_CODEC} 经 {@link FriendlyByteBuf} 写→读后相等，且字节流被恰好消费完；</li>
 *   <li>{@code ModDataComponents.DANMAKU} 已注册，且 {@link ItemStack} 上的 set/get 往返一致。</li>
 * </ol>
 *
 * <p>模板见 {@link RegistryCompletenessTest} 的说明（{@code minecraft:empty}）。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DanmakuParamsCodecTest {

    private static final String TEMPLATE = "empty";

    /** 覆盖：默认值、各字段非零、负值、大值（VarInt 多字节边界 127/128/16384）。 */
    private static final List<DanmakuParams> SAMPLES = List.of(
            DanmakuParams.DEFAULT,
            new DanmakuParams(1, 0, 0, 0, 0, 0, false),
            new DanmakuParams(5, 12, 3, -1, 40, 8, true),
            new DanmakuParams(127, 128, 16384, -2147483648, 2147483647, 9, false),
            new DanmakuParams(0, 31, -7, 2, 285, 15, true));

    private DanmakuParamsCodecTest() {}

    @GameTest(template = TEMPLATE)
    public static void jsonCodecRoundTrip(GameTestHelper helper) {
        for (DanmakuParams params : SAMPLES) {
            JsonElement json = DanmakuParams.CODEC.encodeStart(JsonOps.INSTANCE, params)
                    .getOrThrow(message -> new GameTestAssertException("encode failed for " + params + ": " + message));
            DanmakuParams decoded = DanmakuParams.CODEC.parse(JsonOps.INSTANCE, json)
                    .getOrThrow(message -> new GameTestAssertException("decode failed for " + json + ": " + message));
            helper.assertValueEqual(decoded, params, "DanmakuParams.CODEC round trip of " + json);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void streamCodecRoundTrip(GameTestHelper helper) {
        for (DanmakuParams params : SAMPLES) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                DanmakuParams.STREAM_CODEC.encode(buf, params);
                int written = buf.readableBytes();
                DanmakuParams decoded = DanmakuParams.STREAM_CODEC.decode(buf);
                helper.assertValueEqual(decoded, params, "DanmakuParams.STREAM_CODEC round trip");
                // 恰好消费完 = 编解码字段数一致（多写一个字段会留下残留字节）
                helper.assertValueEqual(buf.readableBytes(), 0,
                        "STREAM_CODEC must consume exactly the " + written + " bytes it wrote");
            } finally {
                buf.release();
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void componentIsRegisteredAndRoundTrips(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, "danmaku");
        helper.assertTrue(ModDataComponents.DANMAKU.isBound(),
                "ModDataComponents.DANMAKU was never bound (RegisterEvent did not run for data components)");

        DataComponentType<DanmakuParams> type = ModDataComponents.DANMAKU.get();
        DanmakuParams params = new DanmakuParams(3, 9, 2, 0, 11, 8, true);

        ItemStack stack = new ItemStack(Items.STICK);
        helper.assertFalse(stack.has(type), "fresh ItemStack should not carry the danmaku component");
        stack.set(type, params);
        helper.assertValueEqual(stack.get(type), params, "ItemStack component round trip");

        // 组件自身的持久化 codec（= DanmakuParams.CODEC）也必须能往返，否则存档读回会丢参数
        JsonElement json = type.codecOrThrow().encodeStart(JsonOps.INSTANCE, params)
                .getOrThrow(message -> new GameTestAssertException("component encode failed: " + message));
        DanmakuParams decoded = type.codecOrThrow().parse(JsonOps.INSTANCE, json)
                .getOrThrow(message -> new GameTestAssertException("component decode failed: " + message));
        helper.assertValueEqual(decoded, params, "DataComponentType persistent codec round trip of " + json);

        helper.succeed();
    }
}
