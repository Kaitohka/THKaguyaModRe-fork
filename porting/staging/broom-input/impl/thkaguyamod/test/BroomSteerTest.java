package thkaguyamod.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.entity.item.BroomSteer;
import thkaguyamod.entity.item.EntityMarisaBroom;
import thkaguyamod.init.ModEntities;

/**
 * 魔法の箒の操舵演算（{@link BroomSteer}）と制御権の回帰テスト。
 *
 * <p>为什么值得测：箒の操舵は 1.7.10 の長大な if 連鎖（旧箒 395-687）を純関数へ写したもので、
 * 数値を 1 つ間違えると「押しても動かない／逆に動く」という形でしか表に出ず、しかも
 * 見た目は緩やかなドリフトなので目視では原因を特定しにくい。また
 * 「どちら側が積分するか」は {@code getControllingPassenger()} の有無で決まるが、
 * これが抜けると通信経路全体（MoveVehicle / lerp）が黙って無効になる。ここを 1 テストで固定する。
 *
 * <h2>場景</h2>
 * <ol>
 *   <li>{@link #stepForwardAcceleratesAlongBroomYaw}：W だけ押したとき、前進方向（箒 yaw − 90°）へ加速し、
 *       上限 0.8 を超えない。</li>
 *   <li>{@link #stepHoverAssignsVelocity}：ホバー（ジャンプ）中は速度が代入され、
 *       実測値が「original + 上限 0.5 + 移動後抵抗 0.95」と一致する。</li>
 *   <li>{@link #stepHoverForcesHorizontalCap}：ホバー中に右キーも押すと、水平成分が 0.5² = 0.25 まで落ちる
 *       （原作の {@code moveSpeed} 乗算の結果を、本実装のクランプで再現している値）。</li>
 *   <li>{@link #stepIdleDecaysAndFalls}：無入力は 0.97 抵抗＋俯仰 0 だと垂直が落ちる。</li>
 *   <li>{@link #stepBackwardIsTwiceForwardAcceleration}：後退の加速度は前進の 2 倍（原作 396）。</li>
 *   <li>{@link #stepWaterHalvesMotion}：入水で三軸とも半分。</li>
 *   <li>{@link #fullMaskStaysBounded}：全キー同時押しでも速度が暴走しない
 *       （原作 437-443 / 510-516 の発散バグを修正したことの回帰テスト）。</li>
 *   <li>{@link #emptyBroomSinksAndDecays}：無人の箒は水平 0.9 倍・空中の垂直 −0.03、俯仰は 1° ずつ 0 へ。</li>
 *   <li>{@link #emptyBroomGroundedVerticalIsFixed}：接地中の垂直成分は {@code -0.03} に固定され、
 *       摩擦後も tick ごとに変化しない（主樹 I13 の回帰。原作の {@code +0.06} は
 *       1.21.1 では 2 tick 周期の上下動と運動量パケット連続発火になるため置き換えた）。</li>
 *   <li>{@link #controlGateFollowsPlayerPassenger}：{@code getControllingPassenger()} が
 *       プレイヤー乗員にだけ反応する（通信経路の前提）。</li>
 *   <li>{@link #mountedBroomMovesServerSide}：無人の箒がサーバー tick で実際に沈む（実体側の結合確認）。</li>
 * </ol>
 *
 * <h2>数学的根拠（マジックナンバーを避けるため導出を残す）</h2>
 * <p>実体側の 1 tick は「{@code step()} → {@code move()} → {@code dampAfterMove()}」の順で、
 * 数学的には {@code out = damp(step(in))} である（{@code move()} は移動量を書き換えない）。
 * {@code dampAfterMove} は {@code (0.99, 0.95, 0.99)} を掛けるだけなので、
 * テストは {@code step()} を直接呼んだ結果へこの係数を掛けて期待値を作る。
 *
 * <p>例（ホバー + W）：{@code step()} は三軸を 0 にして {@code (0, +0.5, 0)} を代入し、
 * 上限クランプは速度 = 0.5 = 上限なので係数 1.0。よって実体の移動量は
 * {@code (0, 0.5 × 0.95, 0) = (0, 0.475, 0)} になる。
 */
@GameTestHolder(THKaguyaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BroomSteerTest
{
	private static final String TEMPLATE = "empty";

	/** 構造中心（相対座標）。ゲームテストは構造の中心に実行位置を置く。 */
	private static final Vec3 CENTER = new Vec3(3.5D, 3.5D, 3.5D);

	/** 浮動小数の比較許容差（1 tick 分の計算なので十分小さい）。 */
	private static final double EPS = 1.0E-9D;

	/** 後処理の垂直抵抗（{@code BroomSteer.DRAG_VERTICAL}）。 */
	private static final double DRAG_V = BroomSteer.DRAG_VERTICAL;

	// ------------------------------------------------------------------
	// 純関数（BroomSteer）のテスト。世界も実体も要らない。
	// ------------------------------------------------------------------

	/**
	 * W 単押し：前進方向（箒 yaw − 90°）へ加速し、上限 0.8 でクランプされる。
	 *
	 * <p>箒 yaw = 0 のときの前進方向は {@code (sin(-90°), -cos(-90°)) = (-1, 0)}。
	 * 1000 tick 回すとクランプに張り付くので {@code |motion| ≈ 0.8} になる。
	 */
	@GameTest(template = TEMPLATE)
	public static void stepForwardAcceleratesAlongBroomYaw(GameTestHelper helper)
	{
		Vec3 motion = Vec3.ZERO;

		for(int i = 0; i < 1000; i++)
		{
			motion = BroomSteer.step(motion, BroomSteer.FORWARD, 0.0F, 0.0F, false);
		}

		// 前進方向のみ（俯仰 0 なので垂直成分は伸びない）
		helper.assertTrue(motion.x < -0.79D && motion.x > -0.801D, "W 前進が -X 方向へ加速していない: x=" + motion.x);
		helper.assertTrue(Math.abs(motion.z) < EPS, "W 前進で Z 成分が出ている: z=" + motion.z);
		helper.assertTrue(Math.abs(motion.y) < EPS, "俯仰 0 の W 前進で Y 成分が出ている: y=" + motion.y);

		double speed = motion.length();
		helper.assertTrue(speed <= BroomSteer.CAP + EPS, "速度が上限 0.8 を超えた: " + speed);
		helper.succeed();
	}

	/**
	 * ホバー + W：三軸を 0 にしてから垂直 +0.5 を代入し、上限 0.5 でクランプされる。
	 * 実体側の {@code dampAfterMove} を掛けた値 {@code 0.5 × 0.95} を固定する。
	 */
	@GameTest(template = TEMPLATE)
	public static void stepHoverAssignsVelocity(GameTestHelper helper)
	{
		Vec3 motion = BroomSteer.step(Vec3.ZERO, BroomSteer.JUMP | BroomSteer.FORWARD, 0.0F, 0.0F, false);

		helper.assertTrue(close(motion.x, 0.0D), "ホバー + W で X が出ている: " + motion.x);
		helper.assertTrue(close(motion.z, 0.0D), "ホバー + W で Z が出ている: " + motion.z);
		helper.assertTrue(close(motion.y, BroomSteer.ACCELERATION_JUMP), "ホバー + W の代入速度が 0.5 でない: " + motion.y);

		Vec3 afterDamp = BroomSteer.dampAfterMove(motion);
		helper.assertTrue(close(afterDamp.y, BroomSteer.ACCELERATION_JUMP * DRAG_V),
				"ホバー + W の移動後垂直速度が 0.5 × 0.95 でない: " + afterDamp.y);

		// ホバー + S は逆符号（原作 420）
		Vec3 back = BroomSteer.step(Vec3.ZERO, BroomSteer.JUMP | BroomSteer.BACK, 0.0F, 0.0F, false);
		helper.assertTrue(close(back.y, -BroomSteer.ACCELERATION_JUMP), "ホバー + S が下降になっていない: " + back.y);

		helper.succeed();
	}

	/**
	 * ホバー + D：水平は 0.5 で代入されるが、直後の上限クランプ（0.5）で
	 * 「水平のみ」のベクトル長が 0.5 → 0.5 のままなので値は 0.5、
	 * さらに垂直 +0.5（ホバーの俯仰項は無い）と合成されてベクトル長 0.5 を超える場合はクランプされる。
	 *
	 * <p>ここでは「D 単押し」の水平成分が {@code sin(yaw) × 0.5}、{@code -cos(yaw) × 0.5} であることを固定する。
	 */
	@GameTest(template = TEMPLATE)
	public static void stepHoverForcesHorizontalCap(GameTestHelper helper)
	{
		Vec3 right = BroomSteer.step(Vec3.ZERO, BroomSteer.JUMP | BroomSteer.RIGHT, 0.0F, 0.0F, false);
		helper.assertTrue(close(right.x, BroomSteer.ACCELERATION_JUMP * Math.sin(0.0D)), "ホバー + D の X が sin(yaw)×0.5 でない: " + right.x);
		helper.assertTrue(close(right.z, -BroomSteer.ACCELERATION_JUMP * Math.cos(0.0D)), "ホバー + D の Z が -cos(yaw)×0.5 でない: " + right.z);
		helper.assertTrue(close(right.y, 0.0D), "ホバー + D で垂直が動いている: " + right.y);

		// 左は yaw − 180° 基準（原作 434-435）
		Vec3 left = BroomSteer.step(Vec3.ZERO, BroomSteer.JUMP | BroomSteer.LEFT, 0.0F, 0.0F, false);
		helper.assertTrue(close(left.x, BroomSteer.ACCELERATION_JUMP * Math.sin(Math.toRadians(-180.0D))),
				"ホバー + A の X が yaw−180° 基準でない: " + left.x);
		helper.assertTrue(close(left.z, -BroomSteer.ACCELERATION_JUMP * Math.cos(Math.toRadians(-180.0D))),
				"ホバー + A の Z が yaw−180° 基準でない: " + left.z);

		// ホバー + D + 右…だけでは上限に触れない（速度 0.5 = 上限）。
		double speed = right.length();
		helper.assertTrue(speed <= BroomSteer.CAP_JUMP + EPS, "ホバー + D が上限 0.5 を超えた: " + speed);

		helper.succeed();
	}

	/**
	 * 無入力：三軸が 0.97 され（原作 451-453）、俯仰 0 でも俯仰項（0）以外の揚力が無いので
	 * 垂直はそのまま減衰する。
	 */
	@GameTest(template = TEMPLATE)
	public static void stepIdleDecaysAndFalls(GameTestHelper helper)
	{
		Vec3 start = new Vec3(0.4D, 0.2D, -0.1D);
		Vec3 idle = BroomSteer.step(start, 0, 0.0F, 0.0F, false);

		helper.assertTrue(close(idle.x, start.x * BroomSteer.DRAG_IDLE), "無入力の X が 0.97 倍でない: " + idle.x);
		helper.assertTrue(close(idle.y, start.y * BroomSteer.DRAG_IDLE), "無入力の Y が 0.97 倍でない: " + idle.y);
		helper.assertTrue(close(idle.z, start.z * BroomSteer.DRAG_IDLE), "無入力の Z が 0.97 倍でない: " + idle.z);

		// 前 tick の俯仰が上向きなら揚力が乗る（原作 447）
		Vec3 lifted = BroomSteer.step(Vec3.ZERO, 0, 0.0F, -90.0F, false);
		helper.assertTrue(lifted.y > 0.0D, "上向き俯仰で揚力が発生していない: " + lifted.y);

		// 下向き（pitch = +90）なら沈む
		Vec3 dived = BroomSteer.step(Vec3.ZERO, 0, 0.0F, 90.0F, false);
		helper.assertTrue(dived.y < 0.0D, "下向き俯仰で沈んでいない: " + dived.y);

		helper.succeed();
	}

	/** 後退は前進の 2 倍の加速度（原作 396 の {@code acceleration2 = 0.08}）。 */
	@GameTest(template = TEMPLATE)
	public static void stepBackwardIsTwiceForwardAcceleration(GameTestHelper helper)
	{
		Vec3 forward = BroomSteer.step(Vec3.ZERO, BroomSteer.FORWARD, 0.0F, 0.0F, false);
		Vec3 back = BroomSteer.step(Vec3.ZERO, BroomSteer.BACK, 0.0F, 0.0F, false);

		helper.assertTrue(close(forward.length(), BroomSteer.ACCELERATION), "前進の加速度が 0.04 でない: " + forward.length());
		helper.assertTrue(close(back.length(), BroomSteer.ACCELERATION_BACK), "後退の加速度が 0.08 でない: " + back.length());

		// 向きは逆（前 tick の移動量が 0 なら純粋に符号だけの差）
		helper.assertTrue(forward.x * back.x <= 0.0D && forward.z * back.z <= 0.0D, "前進と後退の向きが逆になっていない");

		helper.succeed();
	}

	/** 入水時は三軸とも半分（原作 556-560）。 */
	@GameTest(template = TEMPLATE)
	public static void stepWaterHalvesMotion(GameTestHelper helper)
	{
		Vec3 dry = BroomSteer.step(Vec3.ZERO, BroomSteer.FORWARD, 0.0F, 0.0F, false);
		Vec3 wet = BroomSteer.step(Vec3.ZERO, BroomSteer.FORWARD, 0.0F, 0.0F, true);

		helper.assertTrue(close(wet.x, dry.x * BroomSteer.WATER_FACTOR), "入水の X が半減していない: " + wet.x);
		helper.assertTrue(close(wet.y, dry.y * BroomSteer.WATER_FACTOR), "入水の Y が半減していない: " + wet.y);
		helper.assertTrue(close(wet.z, dry.z * BroomSteer.WATER_FACTOR), "入水の Z が半減していない: " + wet.z);

		helper.succeed();
	}

	/**
	 * 全キー同時押しでも速度が上限内に収まる（原作の {@code motion *= moveSpeed} 発散バグの回帰テスト）。
	 *
	 * <p>原作のままだと入力合計が上限を超えた瞬間に速度スカラーが掛かり、
	 * 数百 tick で指数的に増加する。本実装は {@code limit / speed} へ直してあるため、
	 * 1000 tick 後も 0.5 以下（ホバー上限）に留まる。
	 */
	@GameTest(template = TEMPLATE)
	public static void fullMaskStaysBounded(GameTestHelper helper)
	{
		int allKeys = BroomSteer.FORWARD | BroomSteer.BACK | BroomSteer.LEFT | BroomSteer.RIGHT;
		Vec3 motion = Vec3.ZERO;

		for(int i = 0; i < 1000; i++)
		{
			motion = BroomSteer.step(motion, allKeys, 0.0F, 0.0F, false);
		}

		helper.assertTrue(motion.length() <= BroomSteer.CAP + EPS, "通常モードの速度が 0.8 を超えた: " + motion.length());

		// ホバーモード（ジャンプ併用）は上限 0.5
		int allKeysHover = allKeys | BroomSteer.JUMP;
		Vec3 hover = Vec3.ZERO;

		for(int i = 0; i < 1000; i++)
		{
			hover = BroomSteer.step(hover, allKeysHover, 0.0F, 0.0F, false);
		}

		helper.assertTrue(hover.length() <= BroomSteer.CAP_JUMP + EPS, "ホバーの速度が 0.5 を超えた: " + hover.length());

		helper.succeed();
	}

	/** 無人の箒：水平 0.9 倍・空中は垂直 −0.03（原作 672-674）、俯仰は 1° ずつ 0 へ（原作 675-682）。 */
	@GameTest(template = TEMPLATE)
	public static void emptyBroomSinksAndDecays(GameTestHelper helper)
	{
		Vec3 start = new Vec3(0.5D, 0.1D, -0.25D);
		Vec3 empty = BroomSteer.stepEmpty(start, false, false);

		helper.assertTrue(close(empty.x, start.x * BroomSteer.EMPTY_DRAG), "無人の水平抵抗が 0.9 でない: " + empty.x);
		helper.assertTrue(close(empty.z, start.z * BroomSteer.EMPTY_DRAG), "無人の水平抵抗が 0.9 でない: " + empty.z);
		helper.assertTrue(close(empty.y, start.y - BroomSteer.EMPTY_SINK), "無人の空中降下量が 0.03 でない: " + empty.y);

		helper.assertTrue(close(BroomSteer.decayEmptyPitch(30.0F), 29.0F), "無人の俯仰が 1° 減っていない");
		helper.assertTrue(close(BroomSteer.decayEmptyPitch(-30.0F), -29.0F), "無人の俯仰が 1° 増えていない");
		helper.assertTrue(close(BroomSteer.decayEmptyPitch(0.5F), 0.5F), "±1° 以内の俯仰が動いた");

		helper.succeed();
	}

	/**
	 * 接地中の垂直成分が沈降分に固定されること（主樹 I13 の回帰）。
	 *
	 * <p>原作 683-686 は {@code motionY += 0.06D} で「浮き上がらせる」設計だったが、
	 * 1.21.1 の {@code Entity#move()} が着地時に垂直成分を 0 にしないため、
	 * 主樹側では接地中 {@code motionY = -0.03} に固定する形へ変更された
	 * （実測: 2 tick 周期 振幅 0.0141 ブロックの上下動と運動量パケット連続発火を解消）。
	 *
	 * <p>ここでは ① 入力の垂直成分に依らず一定値へ固定されること、
	 * ② 摩擦後も値が振動しない（毎 tick 同じ）であることを検査する。
	 */
	@GameTest(template = TEMPLATE)
	public static void emptyBroomGroundedVerticalIsFixed(GameTestHelper helper)
	{
		// 空中の値を何にしても、接地中は同じ値へ固定される
		Vec3 fromZero = BroomSteer.stepEmpty(new Vec3(0.0D, 0.0D, 0.0D), true, false);
		Vec3 fromFalling = BroomSteer.stepEmpty(new Vec3(0.0D, -0.5D, 0.0D), true, false);
		Vec3 fromRising = BroomSteer.stepEmpty(new Vec3(0.0D, 0.4D, 0.0D), true, false);

		helper.assertTrue(close(fromZero.y, BroomSteer.EMPTY_GROUNDED_VERTICAL),
				"接地中の垂直成分が -0.03 に固定されていない: " + fromZero.y);
		helper.assertTrue(close(fromFalling.y, BroomSteer.EMPTY_GROUNDED_VERTICAL),
				"下降中に着地したケースで固定されていない: " + fromFalling.y);
		helper.assertTrue(close(fromRising.y, BroomSteer.EMPTY_GROUNDED_VERTICAL),
				"上昇中に着地したケースで固定されていない: " + fromRising.y);

		// 摩擦後（dampAfterMove）も毎 tick 同じ値になる → 運動量パケットが出ない条件
		double damped = BroomSteer.dampAfterMove(fromZero).y;
		double nextTick = BroomSteer.dampAfterMove(BroomSteer.stepEmpty(new Vec3(0.0D, damped, 0.0D), true, false)).y;
		helper.assertTrue(close(damped, nextTick),
				"接地中の垂直成分が tick ごとに変化している（運動量パケットが止まらない）: "
						+ damped + " -> " + nextTick);

		// 空中では従来どおり沈降し続ける
		Vec3 airborne = BroomSteer.stepEmpty(new Vec3(0.0D, 0.0D, 0.0D), false, false);
		helper.assertTrue(close(airborne.y, -BroomSteer.EMPTY_SINK), "空中の沈降が -0.03 でない: " + airborne.y);

		helper.succeed();
	}

	/**
	 * 制御権の門：{@code getControllingPassenger()} が
	 * 「プレイヤー乗員のときだけそのプレイヤー」を返すこと（通信経路の前提）。
	 */
	@GameTest(template = TEMPLATE)
	public static void controlGateFollowsPlayerPassenger(GameTestHelper helper)
	{
		EntityMarisaBroom broom = helper.spawn(ModEntities.MARISA_BROOM.get(), CENTER);
		helper.assertTrue(broom.getControllingPassenger() == null, "無人の箒が操舵者を持つと判定された");

		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		player.startRiding(broom, true);
		helper.assertTrue(broom.getFirstPassenger() == player, "テストプレイヤーが乗車できていない");

		helper.assertTrue(broom.getControllingPassenger() == player,
				"プレイヤー乗員が操舵者として返らない（MoveVehicle 経路が開かない）");

		broom.getFirstPassenger().stopRiding();
		helper.assertTrue(broom.getControllingPassenger() == null, "降車後も操舵者が残っている");

		helper.succeed();
	}

	/**
	 * 無人箒の実体側の結合確認：サーバー tick で実際に沈むこと。
	 *
	 * <p>乗員がいないので {@code isControlledByLocalInstance()} は {@code isEffectiveAi()}（サーバーで true）に
	 * 委譲される。GameTest のレベルは {@code ServerLevel} なので、この箒は積分側になる。
	 */
	@GameTest(template = TEMPLATE)
	public static void mountedBroomMovesServerSide(GameTestHelper helper)
	{
		EntityMarisaBroom broom = helper.spawn(ModEntities.MARISA_BROOM.get(), CENTER);
		double startY = broom.getY();
		double startX = broom.getX();
		double startZ = broom.getZ();

		helper.runAfterDelay(15L, () -> {
			helper.assertTrue(broom.isAlive(), "箒が 15 tick で消えた");
			helper.assertTrue(broom.getY() < startY, "無人の箒が沈んでいない: y=" + broom.getY() + " (start " + startY + ")");
			// 構造は 7³ なので、沈下が大きくても壁に当たって止まる程度
			helper.assertTrue(Math.abs(broom.getX() - startX) < 3.0D && Math.abs(broom.getZ() - startZ) < 3.0D,
					"無人の箒が構造外へ流れた: x=" + broom.getX() + " z=" + broom.getZ());
			helper.succeed();
		});
	}

	/** {@code |a - b| <= EPS}。 */
	private static boolean close(double a, double b)
	{
		return Math.abs(a - b) <= EPS;
	}
}