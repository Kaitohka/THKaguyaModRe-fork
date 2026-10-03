package thkaguyamod.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import thkaguyamod.init.ModItems;

/**
 * 御柱 (Onbashira) の採掘速度 — <b>VARIANT B: 1.7.10 実機挙動（適正ツールで分岐）</b>（P10）。
 *
 * <p>1.7.10 の {@code ItemOnbashira#getDigSpeed} は字面こそ恒 {@code 3F} だが、
 * 実際にプレイヤーが感じていた速度はバニラの
 * {@code EntityPlayer#getPlayerRelativeBlockHardness} の分岐を通っていた。
 * {@code ItemSword#canHarvestBlock} が true を返すのは<b>クモの巣のみ</b>
 * （本機の {@code forge-1.12.2-14.23.5.2860} のマップ済みクラスで
 * {@code ItemSword.canHarvestBlock} を逆アセンブルし、{@code Blocks.WEB} との同一性比較のみを確認）、
 * それ以外のブロックでは除数が 100 になり、実効速度が 1.0 相当に落ちていた。
 *
 * <p>1.21.1 の対応物は {@code Player#hasCorrectToolForDrops(BlockState, Level, BlockPos)}
 * （内部で {@code EventHooks.doPlayerHarvestCheck} → {@link PlayerEvent.HarvestCheck}）。
 * これが true のときだけ 30 除算相当＝3.0F、それ以外は 1.0F を与えることで、
 * 1.7.10 の「クモの巣だけ 3.0、他は 1.0」を再現する。
 *
 * <p>クモの巣の扱いの差（要 K3 判断）:
 * 御柱の {@code Tool} は {@code SwordItem.createToolProperties()}（{@code minesAndDrops(COBWEB, 15.0F)} +
 * {@code overrideSpeed(#sword_efficient, 1.5F)}）なので、
 * <ul>
 *   <li>本変体を入れると クモの巣 = <b>3.0</b>（1.7.10 と一致）</li>
 *   <li>入れないと クモの巣 = 15.0（1.21.1 の剣の既定。1.7.10 の 3.0 より速い）</li>
 * </ul>
 * また {@code #sword_efficient}（草・葉・作物など）は本変体で <b>1.0</b> に落ちる
 * （1.21.1 の剣なら 1.5）。1.7.10 の実機ではこれらも 1.0 だったため、本変体のほうが実機準拠。
 *
 * <p>接続（K3 が {@code THKaguyaMod} のコンストラクタに追記）:
 * <pre>{@code
 * NeoForge.EVENT_BUS.register(new OnbashiraDigSpeedHandler());
 * }</pre>
 */
public final class OnbashiraDigSpeedHandler {

    /** 1.7.10 {@code ItemOnbashira#getDigSpeed} の戻り値。適正ツールのときだけ使う。 */
    private static final float ONBASHIRA_DIG_SPEED = 3.0F;

    /** 不適正ブロックの 1.7.10 実効速度（{@code Item#getDigSpeed} 既定 1.0F と同値）。 */
    private static final float ONBASHIRA_DIG_SPEED_WRONG_TOOL = 1.0F;

    public OnbashiraDigSpeedHandler() {}

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!holdsOnbashira(player)) {
            return;
        }

        // 位置が不明な文脈（pos == null）では攻撃者自身の位置で近似する
        // （BlockState に依存しないのでブロック位置の厳密さは不要）
        boolean correct = player.hasCorrectToolForDrops(
                event.getState(), player.level(), event.getPosition().orElse(player.blockPosition()));
        event.setNewSpeed(correct ? ONBASHIRA_DIG_SPEED : ONBASHIRA_DIG_SPEED_WRONG_TOOL);
    }

    private static boolean holdsOnbashira(Player player) {
        return isOnbashira(player.getMainHandItem()) || isOnbashira(player.getOffhandItem());
    }

    private static boolean isOnbashira(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.ONBASHIRA.get());
    }

    // TODO K3-review: hasCorrectToolForDrops は PlayerEvent.HarvestCheck を経由するため、
    //   他 MOD が HarvestCheck を書き換えていると本変体の速度も連動して変わる（意図的な連動）。
    // TODO K3-review: 1.7.10 の除数分岐は「かまどの採取可否」＝ canHarvestBlock であり、
    //   1.21.1 の hasCorrectToolForDrops は requiresCorrectToolForDrops なブロックに対してのみ
    //   TOOL の correct_for_drops を見る。御柱の Tool は COBWEB 以外で correctForDrops を持たないため、
    //   実際には「クモの巣だけ true」となり 1.7.10 と一致する（本機 sources で Tool.getMiningSpeed/
    //   isCorrectForDrops の実装を確認済み）。
    // TODO K3-review: 副手も認めている（1.7.10 は主手のみ）。厳密化するなら getOffhandItem() を削る。
}