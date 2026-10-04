package thkaguyamod.client.input;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import thkaguyamod.entity.item.BroomSteer;
import thkaguyamod.entity.item.EntityMarisaBroom;

/**
 * 魔法の箒の操舵キーをクライアント側で読み、騎乗中の箒へ書き込む。
 *
 * <p>1.7.10 原作は {@code EntityMarisaBroom#isPushKeyUp()} など
 * （旧箒 222-259）で {@code Minecraft.getMinecraft().gameSettings} を直接読んでいた。
 * 1.21.1 では同じことをしても公共コードから client-only クラスを参照できない
 * （{@code MIGRATION_RULES.md} R3）ため、クライアント専用パッケージから
 * 「押下ビット」だけを書き込む。
 *
 * <h2>なぜ {@code Options} を直接読み、{@code Input} を使わないか</h2>
 * <ul>
 *   <li>{@code LocalPlayer#input.jumping} は自動ジャンプ時に強制的に true にされる
 *       （{@code LocalPlayer#aiStep}、autoJumpTime &gt; 0 の分岐）ため、キー押下と一致しない。</li>
 *   <li>{@code KeyMapping#isDown()} はキー再割当・競合コンテキスト・
 *       画面を開いたときの {@code KeyMapping.releaseAll()} をすべて反映する。</li>
 *   <li>W+S / A+D の同時押しは原作では両方成立する（各キー独立の if）。原版の
 *       {@code KeyboardInput#calculateImpulse} は同時押しで 0 にしてしまうため、
 *       {@code xxa/zza} 経由ではこの挙動が失われる。だから独立 5 ビットで持つ。</li>
 * </ul>
 *
 * <h2>書き込み先とタイミング</h2>
 * <p>{@link EntityMarisaBroom#setSteerMask(int)} は「制御側のエンティティ」にだけ書く。
 * 送信（{@code ServerboundMoveVehiclePacket}）も制御側だけが行うため、
 * 非制御側のサーバー実体は入力を使わない（{@code isControlledByLocalInstance()} で分岐）。
 *
 * <p>{@link ClientTickEvent.Pre} は {@code Minecraft#tick()} の先頭（{@code level.tickEntities()} の前）
 * に発火する。実体の tick は「乗り物 → 乗員」（{@code ClientLevel#tickPassenger}）の順なので、
 * 箒の tick は必ず本メソッドの後に走る。
 *
 * <h2>登録（THKaguyaClient への 1 行）</h2>
 * <pre>
 * NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, BroomSteerClient::onClientTickPre);
 * </pre>
 */
public final class BroomSteerClient
{
	private BroomSteerClient()
	{
	}

	/**
	 * 毎クライアント tick、騎乗中の箒にキー押下ビットを書き込む。
	 *
	 * <p>乗っていないときは {@code 0} を書き込む（降車直後に前回のビットが残らないように）。
	 * 書き込み先が「今乗っている箒」なので、他の箒には触れない。
	 */
	public static void onClientTickPre(ClientTickEvent.Pre event)
	{
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;

		if(player == null || !(player.getVehicle() instanceof EntityMarisaBroom broom))
		{
			return;
		}

		Options options = minecraft.options;
		int mask = BroomSteer.maskOf(
				isDown(options.keyUp),
				isDown(options.keyDown),
				isDown(options.keyLeft),
				isDown(options.keyRight),
				isDown(options.keyJump));

		broom.setSteerMask(mask);
	}

	/**
	 * {@code KeyMapping} が null でも落ちないようにした押下判定。
	 *
	 * <p>{@code Options} の各キーは {@code final} フィールドで null にはならないが、
	 * 将来の差し替えに備えて防御的にしておく（コストは 1 回の null 比較）。
	 */
	private static boolean isDown(KeyMapping keyMapping)
	{
		return keyMapping != null && keyMapping.isDown();
	}
}
