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

	//1.7.10 の canBeCollidedWith = true（常に当たり判定・照準対象）に相当
	@Override
	public boolean canBeCollidedWith()
	{
		return true;
	}

	@Override
	public boolean isPickable()
	{
		return true;
	}
}
