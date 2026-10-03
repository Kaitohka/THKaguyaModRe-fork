package thkaguyamod.entity.item;

import net.minecraft.world.phys.Vec3;

/**
 * 魔法の箒の操舵演算（1.7.10 {@code EntityMarisaBroom#onUpdate} 360-687 の純関数化）。
 *
 * <p>このクラスは状態を持たず、{@code EntityMarisaBroom#tick()} からのみ呼ばれる。
 * 世界にもエンティティにも触れないため、GameTest から直接反復計算できる
 * （{@code BroomSteerTest} がその前提で書かれている）。
 *
 * <h2>座標系と角度の規約（原作との対応）</h2>
 * <p>原作は入力演算（395-479）で {@code this.rotationYaw} / {@code this.rotationPitch} を読み、
 * <b>その後</b>に騎手の角度から箒の角度を更新する（481-507）。したがって入力演算が使う角度は
 * 「前 tick の箒の角度」である。本クラスの引数 {@code broomYaw} / {@code broomPitch} は
 * その前 tick の値（= 呼び出し側の {@code this.getYRot()} / {@code this.getXRot()}）を受け取る。
 *
 * <p>原作の各軸の基準角（{@code R} = 前 tick の箒の yaw）:
 * <ul>
 *   <li>前進: {@code R - 90°}（457-461）</li>
 *   <li>後退: {@code R - 90°} の逆符号（462-466）</li>
 *   <li>右: {@code R}（429-430 / 468-472）</li>
 *   <li>左: {@code R - 180°}（434-435 / 473-477）</li>
 * </ul>
 * 箒の yaw は騎手 yaw − 90°（507）なので、前進は騎手の視線方向、右/左は騎手の右/左に一致する。
 *
 * <h2>原作からの意図的な差異（すべて TODO K3-review 付き）</h2>
 * <ol>
 *   <li><b>速度上限の実装（原作 437-443 / 510-516 のバグ修正）</b>:
 *       原作は上限判定の後 {@code motionX *= moveSpeed}（= 速度スカラーの乗算）と書いており、
 *       これは「正規化」でも「クランプ」でもない。速度が 1 を超えると正のフィードバックで発散する
 *       （単キーでは結果的に 0.2〜0.5 のソフト上限に見えるため、旧版では気付かれていない）。
 *       本実装は {@link #capFactor} で素直に {@code motion *= limit / speed} へ直す。</li>
 *   <li><b>俯仰の扱い</b>: 原作 489-504 の {@code pitch} クランプと {@code pitch *= 0.1F} は
 *       ローカル変数への代入で、{@code rotationPitch} には 486-487 で既に代入済みのため
 *       一切効いていない（死コード）。本実装は「{@code setXRot(騎手 xRot)} のみ」を再現する。</li>
 *   <li><b>入水時の AABB 走査（原作 283-296）</b>は結果 {@code d0/d4} が使われない死コードのため移植しない。</li>
 *   <li><b>速度係数 {@code speedMultiplier}（原作 483 の 0.6）</b>は書き込みのみで読み出しが無い死コード。
 *       {@code EntityMarisaBroom#getSpeedMultiplier()} の互換のためフィールドは残すが、ここでは扱わない。</li>
 * </ol>
 */
public final class BroomSteer
{
	/** 前進（原作 W キー / keyBindForward）。 */
	public static final int FORWARD = 1;
	/** 後退（原作 S キー）。 */
	public static final int BACK = 2;
	/** 左横移動（原作 A キー）。 */
	public static final int LEFT = 4;
	/** 右横移動（原作 D キー）。 */
	public static final int RIGHT = 8;
	/** ジャンプ＝ホバー操舵モード（原作 空格 / keyBindJump）。押下中は 5 キーが「速度代入」モードになる。 */
	public static final int JUMP = 16;

	/** 通常加速度（原作 395）。 */
	public static final double ACCELERATION = 0.04D;
	/** 後退加速度（原作 396。前進の 2 倍）。 */
	public static final double ACCELERATION_BACK = 0.08D;
	/** ホバーモードの加速度（原作 399。5 キーはこの値で「代入」される）。 */
	public static final double ACCELERATION_JUMP = 0.5D;
	/** 通常モードの速度上限（原作 511）。 */
	public static final double CAP = 0.8D;
	/** ホバーモードの速度上限（原作 438）。 */
	public static final double CAP_JUMP = 0.5D;
	/** 移動後の水平抵抗（原作 584）。 */
	public static final double DRAG_HORIZONTAL = 0.9900000095367432D;
	/** 移動後の垂直抵抗（原作 585）。 */
	public static final double DRAG_VERTICAL = 0.949999988079071D;
	/** 無入力時の追加抵抗（原作 451-453）。 */
	public static final double DRAG_IDLE = 0.97D;
	/** 入水時の減速（原作 556-560）。 */
	public static final double WATER_FACTOR = 0.5D;
	/** 無人の箒の水平抵抗（原作 672/674）。 */
	public static final double EMPTY_DRAG = 0.9D;
	/** 無人の箒の降下量（原作 673）。 */
	public static final double EMPTY_SINK = 0.03D;
	/** 無人の箒が接地しているときの浮き上がり（原作 685）。 */
	public static final double EMPTY_GROUND_LIFT = 0.06D;

	private BroomSteer()
	{
	}

	/**
	 * 操舵入力ビットを組み立てる（{@link #FORWARD} 等の OR）。
	 *
	 * <p>原作は各キーを独立した {@code if} で見ていたため、W+S や A+D の同時押しも
	 * 両方成立する（1.21.1 原版 {@code KeyboardInput#calculateImpulse} は同時押しで 0 になるので、
	 * 原版の入力値経由では失われる挙動。だからこそ独立ビットで持つ）。
	 */
	public static int maskOf(boolean forward, boolean back, boolean left, boolean right, boolean jump)
	{
		int mask = 0;
		if(forward) { mask |= FORWARD; }
		if(back) { mask |= BACK; }
		if(left) { mask |= LEFT; }
		if(right) { mask |= RIGHT; }
		if(jump) { mask |= JUMP; }
		return mask;
	}

	/** 移動系キー（W/A/S/D）が 1 つでも押されているか（原作 448-449）。 */
	public static boolean hasAnyMoveKey(int mask)
	{
		return (mask & (FORWARD | BACK | LEFT | RIGHT)) != 0;
	}

	/**
	 * 騎乗中の 1 tick 分の移動量を計算する（原作 395-560。{@code move()} と移動後抵抗は含まない）。
	 *
	 * @param motion     現在の移動量（原作 {@code motionX/Y/Z}）
	 * @param mask       {@link #maskOf} の操舵ビット
	 * @param broomYaw   前 tick の箒の yaw（呼び出し側の {@code getYRot()}）
	 * @param broomPitch 前 tick の箒の pitch（呼び出し側の {@code getXRot()}）
	 * @param inWater    入水中かどうか（原作 {@code this.inWater}）
	 * @return {@code move(MoverType.SELF, ...)} に渡す移動量
	 */
	public static Vec3 step(Vec3 motion, int mask, float broomYaw, float broomPitch, boolean inWater)
	{
		double mx = motion.x;
		double my = motion.y;
		double mz = motion.z;

		if((mask & JUMP) != 0)
		{
			// 原作 397-444: ホバーモード。まず三軸を 0 にしてから「代入」する。
			mx = 0.0D;
			my = 0.0D;
			mz = 0.0D;

			if((mask & FORWARD) != 0)
			{
				my = -Math.sin(Math.toRadians(-90.0D)) * ACCELERATION_JUMP;// 原作 412
			}

			if((mask & BACK) != 0)
			{
				my = -Math.sin(Math.toRadians(90.0D)) * ACCELERATION_JUMP;// 原作 420
			}

			if((mask & RIGHT) != 0)
			{
				mx = Math.sin(Math.toRadians(broomYaw)) * ACCELERATION_JUMP;// 原作 429
				mz = -Math.cos(Math.toRadians(broomYaw)) * ACCELERATION_JUMP;// 原作 430
			}

			if((mask & LEFT) != 0)
			{
				mx = Math.sin(Math.toRadians(broomYaw - 180.0D)) * ACCELERATION_JUMP;// 原作 434
				mz = -Math.cos(Math.toRadians(broomYaw - 180.0D)) * ACCELERATION_JUMP;// 原作 435
			}

			// 原作 437-443（発散バグの修正版。クラス javadoc §2.1 参照）
			double factor = capFactor(length(mx, my, mz), CAP_JUMP);
			mx *= factor;
			my *= factor;
			mz *= factor;
		}
		else
		{
			// 原作 447: 俯仰による揚力（毎 tick 無条件）
			my += -Math.sin(Math.toRadians(broomPitch)) * ACCELERATION;

			if(!hasAnyMoveKey(mask))
			{
				// 原作 448-454: 無入力なら三軸まとめて減衰
				mx *= DRAG_IDLE;
				my *= DRAG_IDLE;
				mz *= DRAG_IDLE;
			}
			else
			{
				if((mask & FORWARD) != 0)
				{
					mx += Math.sin(Math.toRadians(broomYaw - 90.0D)) * ACCELERATION;// 原作 459
					mz += -Math.cos(Math.toRadians(broomYaw - 90.0D)) * ACCELERATION;// 原作 460
				}

				if((mask & BACK) != 0)
				{
					// 原作 462-466: 前進と同じ軸の逆符号、加速度は ACCELERATION_BACK
					mx += -Math.sin(Math.toRadians(broomYaw - 90.0D)) * ACCELERATION_BACK;
					mz += Math.cos(Math.toRadians(broomYaw - 90.0D)) * ACCELERATION_BACK;
				}
			}

			if((mask & RIGHT) != 0)
			{
				mx += Math.sin(Math.toRadians(broomYaw)) * ACCELERATION;// 原作 470
				mz += -Math.cos(Math.toRadians(broomYaw)) * ACCELERATION;// 原作 471
			}

			if((mask & LEFT) != 0)
			{
				mx += Math.sin(Math.toRadians(broomYaw - 180.0D)) * ACCELERATION;// 原作 475
				mz += -Math.cos(Math.toRadians(broomYaw - 180.0D)) * ACCELERATION;// 原作 476
			}

			// 原作 510-516（発散バグの修正版）
			double factor = capFactor(length(mx, my, mz), CAP);
			mx *= factor;
			my *= factor;
			mz *= factor;
		}

		if(inWater)
		{
			// 原作 556-560
			mx *= WATER_FACTOR;
			my *= WATER_FACTOR;
			mz *= WATER_FACTOR;
		}

		return new Vec3(mx, my, mz);
	}

	/**
	 * 無人の箒の 1 tick 分の移動量（原作 665-687。{@code move()} と移動後抵抗は含まない）。
	 *
	 * <p>原作はこのブロックを {@code !worldObj.isRemote}（サーバー）でのみ実行していた。
	 * 1.21.1 では {@code isControlledByLocalInstance()} が同じ役割を果たす
	 * （乗員なしのときは {@code isEffectiveAi()} に委譲され、サーバーのみ true）。
	 *
	 * @param motion   現在の移動量
	 * @param onGround 接地しているか（原作 683）
	 * @param inWater  入水中かどうか
	 */
	public static Vec3 stepEmpty(Vec3 motion, boolean onGround, boolean inWater)
	{
		double mx = motion.x * EMPTY_DRAG;// 原作 672
		double my = motion.y - EMPTY_SINK;// 原作 673
		double mz = motion.z * EMPTY_DRAG;// 原作 674

		if(onGround)
		{
			my += EMPTY_GROUND_LIFT;// 原作 683-686
		}

		if(inWater)
		{
			mx *= WATER_FACTOR;
			my *= WATER_FACTOR;
			mz *= WATER_FACTOR;
		}

		return new Vec3(mx, my, mz);
	}

	/**
	 * {@code move()} の後の減衰（原作 584-587）。移動後の {@code deltaMovement} に適用する。
	 *
	 * <p>原作のこの 3 行はコメントアウトされた「衝突で破壊」ブロック（564-582）の
	 * {@code else} 節の外側にあり、常に実行される。
	 */
	public static Vec3 dampAfterMove(Vec3 motion)
	{
		return new Vec3(motion.x * DRAG_HORIZONTAL, motion.y * DRAG_VERTICAL, motion.z * DRAG_HORIZONTAL);
	}

	/**
	 * 無人の箒の俯仰を 1 度ずつ 0 に近づける（原作 675-682）。{@code stepEmpty} の直後に呼ぶ。
	 */
	public static float decayEmptyPitch(float xRot)
	{
		if(xRot > 1.0F)
		{
			return xRot - 1.0F;
		}

		if(xRot < -1.0F)
		{
			return xRot + 1.0F;
		}

		return xRot;
	}

	/**
	 * 速度上限を掛ける係数を返す。{@code speed <= limit} なら 1.0（無変更）。
	 *
	 * <p>TODO K3-review: これは原作のバグ修正箇所。原作 437-443 / 510-516 は
	 * {@code if (moveSpeed > limit) { motionX *= moveSpeed; ... }} であり、
	 * 「速度スカラーそのものを掛ける」ため速度 &gt; 1 で正のフィードバック発散になる
	 * （W+S+A+D 同時押しで 10 tick 強で 1 を超え、以後指数的に増加）。
	 * ここでは {@code limit / speed} を掛ける正しいクランプに置き換えている。
	 * 数値（0.5 / 0.8）は原作のまま維持している。
	 */
	private static double capFactor(double speed, double limit)
	{
		return speed > limit ? limit / speed : 1.0D;
	}

	private static double length(double x, double y, double z)
	{
		return Math.sqrt(x * x + y * y + z * z);
	}
}
