package thkaguyamod.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import thkaguyamod.init.ModItems;

/**
 * 御柱 (Onbashira) の採掘速度 — <b>VARIANT A: 字面どおり恒 3.0F</b>（P10）。
 *
 * <p>1.7.10 {@code ItemOnbashira#getDigSpeed(ItemStack, Block, int)} は常に {@code 3F} を返していた
 * （{@code ToolMaterial.WOOD} の 2.0 を置き換える意図的な指定）。本変体はこの<b>ソースの字面</b>を
 * そのまま移植し、御柱を持つプレイヤーの全ブロック破壊速度を 3.0 に置換する。
 *
 * <p>実装経路（1.21.1 にはアイテム側の上書き点が無いため）:
 * {@code Player#getDigSpeed} の最終段が {@code EventHooks.getBreakSpeed(...)} を通り、
 * そこで {@link PlayerEvent.BreakSpeed} が発火する。これが唯一の
 * 「アイテム別・全ブロック一律」差し込み点（{@code neoforge-21.1.252-sources.jar} の
 * {@code Player.java:775-805} / {@code EventHooks.java:213-216} で確認済み）。
 *
 * <p>副作用として、原版の空中ペナルティ（÷5）・水中ペナルティ・採掘速度低下も
 * この時点で上書きされる（1.7.10 も {@code getDigSpeed} の後に効いていた係数はポーションのみで、
 * 空中/水中の割り算は 1.7.10 の {@code getDigSpeed} 内で既に済んでいた → 効果の順序は同等）。
 *
 * <p>接続（K3 が {@code THKaguyaMod} のコンストラクタ、
 * 既存の {@code NeoForge.EVENT_BUS.register(new PlayerDeathEventHandler());} の隣に追記）:
 * <pre>{@code
 * NeoForge.EVENT_BUS.register(new OnbashiraDigSpeedHandler());
 * }</pre>
 * クライアント（{@code LocalPlayer} の破壊予測）とサーバ（実破壊）の双方で発火するため、
 * 本クラスは client パッケージに置かない（{@code MIGRATION_RULES.md} R3）。
 */
public final class OnbashiraDigSpeedHandler {

    /** 1.7.10 {@code ItemOnbashira#getDigSpeed} の戻り値そのもの。 */
    private static final float ONBASHIRA_DIG_SPEED = 3.0F;

    public OnbashiraDigSpeedHandler() {}

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (holdsOnbashira(player)) {
            event.setNewSpeed(ONBASHIRA_DIG_SPEED);
        }
    }

    private static boolean holdsOnbashira(Player player) {
        return isOnbashira(player.getMainHandItem()) || isOnbashira(player.getOffhandItem());
    }

    private static boolean isOnbashira(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.ONBASHIRA.get());
    }

    // TODO K3-review: 本変体は「不適正ブロックでも 3.0」になる。1.7.10 の実機挙動は
    //   適正ツール判定で 30/100 の除数が切り替わり、御柱の適正範囲が #minecraft:sword_efficient だけなので
    //   事実上「クモの巣 = 3.0 / それ以外 = 1.0」に近かった（詳細は VARIANTS.md §1、実測記録は §2）。
    //   実機準拠にしたい場合は VARIANT-B（OnbashiraDigSpeedHandler の B 版）を採用すること。
    // TODO K3-review: 副手も認めている。1.7.10 は主手のみ（getCurrentItem）。
    //   厳密に合わせるなら getOffhandItem() の判定を削る。
}
