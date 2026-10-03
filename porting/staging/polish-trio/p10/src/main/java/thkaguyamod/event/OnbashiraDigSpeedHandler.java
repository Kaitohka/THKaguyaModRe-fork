package thkaguyamod.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import thkaguyamod.init.ModItems;

/**
 * 御柱 (Onbashira) の採掘速度を 1.7.10 と同じ「恒 3.0」に固定する（P10）。
 *
 * <p>1.7.10 {@code ItemOnbashira#getDigSpeed(ItemStack, Block, int)} は
 * 常に {@code 3F} を返していた（{@code ToolMaterial.WOOD} の 2.0 を上書きする、
 * 意図的な nerf/挙動指定）。1.21.1 では鉱物ごとの採掘速度は
 * {@code DataComponents.TOOL}（{@code Tool.rules/defaultMiningSpeed}）が持ち、
 * アイテム側のメソッド上書き点は無い。一方 {@code Player#getDigSpeed(BlockState, BlockPos)} は
 * 最後に {@code EventHooks.getBreakSpeed(...)} を通すため、
 * {@link PlayerEvent.BreakSpeed} が唯一の「アイテム別・全ブロック一律」差し込み点になる。
 *
 * <p>実装方針: メインハンドまたはオフハンドに御柱を持つプレイヤーの破壊速度を常に 3.0 に置換する。
 * これは 1.7.10 の「{@code getDigSpeed} が 3F を返す」という字面の意味そのもの。
 *
 * <p>1.7.10 の細かい差（要 K3 判断・下の TODO 参照）:
 * 1.7.10 {@code EntityPlayer#getPlayerRelativeBlockHardness} は
 * 「適正ツールでないブロック」では {@code getDigSpeed} を使わず定数 1.0F を使う分岐だった
 * （{@code ItemSword#canHarvestBlock} はクモの巣のみ true）。つまり本来 3.0 が効いたのは
 * 実質クモの巣だけで、それ以外は 1.0 だった。1.21.1 は適正／不適正のどちらでも
 * 同じ速度値が式に入るため、ここで 3.0 を入れると 1.7.10 の「不適正ブロック＝1.0」より速くなる。
 * 字面どおりの移植を優先して 3.0 を採るが、体感差が出るなら下の代替案に切り替えること。
 *
 * <p>接続（K3 が {@code THKaguyaMod} のコンストラクタに追記する。既存の
 * {@code NeoForge.EVENT_BUS.register(new PlayerDeathEventHandler());} の隣）:
 * <pre>{@code
 * NeoForge.EVENT_BUS.register(new OnbashiraDigSpeedHandler());
 * }</pre>
 * イベントはクライアント（{@code LocalPlayer} の採掘予測）とサーバ（実破壊）の双方で発火するため、
 * 本クラスは {@code thkaguyamod.client} ではなく共通側（{@code thkaguyamod.event}）に置く
 * （{@code MIGRATION_RULES.md} R3 の client 分離ゲートに抵触しない）。
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

    // TODO K3-review: 代替案（1.7.10 の「不適正ブロック＝定数 1.0」まで再現する場合）:
    //   event.setNewSpeed(player.hasCorrectToolForDrops(event.getState(), player.level(),
    //       event.getPosition().orElse(player.blockPosition()))
    //       ? ONBASHIRA_DIG_SPEED : 1.0F);
    //   ただし御柱（SwordItem）が適正ツールになるブロックは #minecraft:sword_efficient のみなので、
    //   「クモの巣だけ 3.0、他は 1.0」という 1.7.10 実挙動になる。字面（恒 3.0）とどちらを採るかは K3 判断。
    //
    // TODO K3-review: 1.7.10 の getDigSpeed は「アイテムを使っている時」の値であり、
    //   プレイヤーのポーション効果（採掘速度上昇/低下）や BLOCK_BREAK_SPEED 属性は
    //   その後に掛かる。本ハンドラは EventHooks の最終段で置換するため、
    //   実効速度 = 3.0 × (採掘速度上昇等の係数) となり、1.7.10 と同じ順序になる。
}