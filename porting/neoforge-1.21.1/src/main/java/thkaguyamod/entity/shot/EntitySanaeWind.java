package thkaguyamod.entity.shot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 早苗の風起こし */
public class EntitySanaeWind extends EntityTHShot
{

	public int type;
	public int kamikaze;

	private double lastPlayerPosX;
	private double lastPlayerPosY;
	private double lastPlayerPosZ;

	/** ワールド読み込み時に呼び出されるコンストラクト */
	public EntitySanaeWind(EntityType<? extends EntitySanaeWind> type, Level level)
	{
		super(type, level);
	}

	public EntitySanaeWind(EntityType<? extends EntitySanaeWind> type, Level level, LivingEntity user, Entity source,
			Vec3 pos, Vec3 angle, float slope,
			Vec3 rotate, float rotationSpeed, int rotationEnd,
			double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
			int kazetype, float size, float damage, int delay, int end, int special)
	{
		super(type, level, user, source, pos, angle, slope,
				rotate, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration,
				gravity, kazetype, size, damage, delay, end, special);

		lastPlayerPosX = user.getX();
		lastPlayerPosY = user.getY();
		lastPlayerPosZ = user.getZ();
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		if(user != null)
		{
			double userMotionX = user.getX() - lastPlayerPosX;
			double userMotionY = user.getY() - lastPlayerPosY;
			double userMotionZ = user.getZ() - lastPlayerPosZ;

			if(getShotColor() == 1)
			{
				setRot(getYRot() + 2F, getXRot());
				shotSpeed = Math.sqrt(userMotionX * userMotionX + userMotionY * userMotionY + userMotionZ * userMotionZ);
				setVector();
			}
			else
			{
				double gravityX = userMotionX * 0.2D;
				double gravityY = userMotionY * 0.2D;
				double gravityZ = userMotionZ * 0.2D;
				if(gravityY < 0.0D)
				{
					gravityY = 0.0D;
				}
				if(user.isShiftKeyDown())
				{
					gravityY = -0.1D;
				}
				gravity = new Vec3(gravityX, gravityY, gravityZ);
			}
		}
		super.tick();

		if(user != null)
		{
			lastPlayerPosX = user.getX();
			lastPlayerPosY = user.getY();
			lastPlayerPosZ = user.getZ();
		}
	}

	public int getWindRenderPattern()
	{
		return tickCount % 4;
	}

	//diag-pose: 常に false。詳細は EntityTHShot#canBeCollidedWith を参照。
	//（早苗之风は使用者の目の高さに 1.0 格の box で生まれるため、true だと
	// プレイヤーが自動で Pose.SWIMMING（匍匐・眼高 0.4）へ落ちて「右键で趴下」する。
	// 1.7.10 に姿勢フィット機構は無く、原作では起きなかった）
	@Override
	public boolean canBeCollidedWith()
	{
		return false;
	}

	//照準・弾丸命中はこちらが担う（canBeHitByProjectile の既定実装が isPickable() を見る）
	@Override
	public boolean isPickable()
	{
		return true;
	}

	/**
	 * 準星（クロスヘア）の選択半径を返す。
	 *
	 * <p>1.7.10 の {@code getCollisionBorderSize() = 1.0F}（原作
	 * {@code thKaguyaMod.entity.shot.EntitySanaeWind:338-341}）に相当。旧版ではこの値が
	 * 「準星で狙えるか」と「弾丸の命中判定の膨らみ」の両方を兼ねていた。
	 * 1.21.1 では準星側が {@link net.minecraft.world.entity.Entity#getPickRadius()} に分離されたため、
	 * 風弾を狙って打ち返せるよう（原作の 1.0 格の加粗）ここで同じ値を返す。
	 *
	 * <p>P46（diag-pose の I8 系譜）: 移植当初この上書きが漏れており、既定値 0.0F のため
	 * 風弾だけ準星が痩せていた。{@code canBeHitByProjectile()} 側は {@code isPickable()} 経由で
	 * 生きているので、命中判定そのものには影響しない（準星の太さのみ）。
	 *
	 * <p>diag-pose との整合: {@link #canBeCollidedWith()} は false のままなので、
	 * この半径がプレイヤーの姿勢フィット（{@code getEntityCollisions}）に影響することはない。
	 */
	@Override
	public float getPickRadius()
	{
		return 1.0F;
	}
}
