package thkaguyamod.client;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModItems;

/**
 * 隙間の折りたたみ傘 (Gap Folding Umbrella) の 2 層アイコン＋染料色替え。
 *
 * <p>1.7.10 は {@code ItemGapFoldingUmbrella#requiresMultipleRenderPasses()} = true で
 * 2 パス描画し、{@code getIconFromDamageForRenderPass(damage, layer)} が
 * layer 0 = {@code GapFoldingUmbrella_base} / layer 1 = {@code GapFoldingUmbrella_ribbon} を返し、
 * {@code getColorFromItemStack(stack, pass)} が pass 0 に 0xFFFFFF、pass 1 に
 * パレット（{@code colorR/G/B[GapColor]}）を返していた。
 *
 * <p>1.21.1 では 2 層は既存の {@code models/item/gap_folding_umbrella.json}
 * （layer0 = base, layer1 = ribbon）がそのまま担い、{@code item/generated} の
 * 生成モデルが layer 添字をそのまま {@code tintindex} に写す
 * （{@code ItemModelGenerator#processFrames(int tintIndex, ...)}）ため、
 * 本クラスの {@link ItemColor} を tintIndex 0/1 で分岐させれば 1.7.10 と同義になる。
 *
 * <p>1.7.10 との意図的な差（{@code MIGRATION_RULES.md} §3「旧版 bug は直す」）:
 * <ul>
 *   <li>1.7.10 の {@code getColorFromItemStack} は描画中に NBT を書き換えていた
 *       （{@code GapColor} が 16 以上なら 0 に、NBT が無ければ作って 0 を書く）。
 *       1.21.1 の {@code ItemColor#getColor} はレンダスレッドから任意のスタックに対して
 *       呼ばれうるため、ここでは<b>読むだけ</b>にして 0..15 にクランプする
 *       （副作用なし・描画結果は同じ）。</li>
 *   <li>1.7.10 の戻り値は RGB のみ（pass 0 の {@code 0xFFFFFF} は ARGB では alpha=0）。
 *       1.21.1 の {@code ItemRenderer#renderQuadList} は戻り値の alpha を乗算に使うため、
 *       そのまま返すと完全透明になる。tint なしは {@code -1}（= 0xFFFFFFFF）を返す。</li>
 * </ul>
 *
 * <p>接続（K3 が {@code THKaguyaClient} に追記する）:
 * {@code modEventBus.addListener(RegisterColorHandlersEvent.Item.class, GapFoldingUmbrellaItemColor::register);}
 * なお {@link RegisterColorHandlersEvent.Item} は mod イベントバス（クライアント専用）で、
 * アイテム登録完了後に発火するため {@code FMLClientSetupEvent} より確実。
 */
public final class GapFoldingUmbrellaItemColor implements ItemColor {

    /**
     * 1.7.10 {@code colorR/G/B[16+1]} のパレット。
     * 添字 0..15 が {@code GapColor}（{@code colorName[]} の並び:
     * black, red, green, brown, blue, purple, cyan, lightGray, gray, pink, lime, yellow,
     * lightBlue, magenta, orange, white）。
     * 1.7.10 の 17 要素目は {@code colorR[color]} が color=16 のときだけ参照される保険で、
     * 呼び出し側の {@code if (color >= 16) color = 0;} により到達しない。ここでも再現しない。
     */
    private static final int[] COLOR_R = { 30, 179, 59, 81, 37, 123, 40, 128, 51, 216, 65, 222, 102, 211, 237, 240 };
    private static final int[] COLOR_G = { 27, 49, 81, 48, 49, 47, 118, 128, 51, 129, 203, 207, 133, 84, 136, 240 };
    private static final int[] COLOR_B = { 27, 44, 26, 26, 146, 190, 151, 128, 51, 152, 51, 42, 219, 205, 68, 240 };

    /** tintindex 0 = layer0 = base。1.7.10 の pass 0 は無着色。 */
    private static final int LAYER_BASE = 0;
    /** tintindex 1 = layer1 = ribbon。1.7.10 の pass 1 が染料色。 */
    private static final int LAYER_RIBBON = 1;

    public GapFoldingUmbrellaItemColor() {}

    /** {@code RegisterColorHandlersEvent.Item} から呼ぶこと。 */
    public static void register(RegisterColorHandlersEvent.Item event) {
        event.register(new GapFoldingUmbrellaItemColor(), ModItems.GAP_FOLDING_UMBRELLA.get());
    }

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex == LAYER_BASE) {
            return -1;
        }
        if (tintIndex != LAYER_RIBBON) {
            return -1;
        }

        int color = stack.getOrDefault(ModDataComponents.GAP_COLOR.get(), 0);
        if (color < 0 || color > 15) {
            // 1.7.10 はここで NBT を 0 に書き戻していた（描画中の副作用）。
            // 移植版は読むだけにして、値が壊れていても 0 として扱う。
            color = 0;
        }
        return (COLOR_R[color] << 16) | (COLOR_G[color] << 8) | COLOR_B[color];
    }

    // TODO K3-review: 1.7.10 は getColorFromItemStack を 2 パス分呼んでいたが、
    //   1.21.1 の ItemColor は tintindex ごとに 1 回呼ばれる。tintindex の割り当て
    //   （0=layer0=base, 1=layer1=ribbon）は ItemModelGenerator の実装依存なので、
    //   将来モデルを手書き elements に切り替える際は逐面 tintindex を明示すること（PREP.md B4）。
    // TODO K3-review: 1.7.10 は GapColor が 16 以上のとき「NBT を 0 に書き戻して」いた。
    //   移植版は無副作用（読むだけ）なので、壊れた値のスタックは毎回 0 として描画される。
    //   恒久修正したければ ItemGapFoldingUmbrella 側で inventoryTick などで正規化する。
}