package thkaguyamod.entity.shot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModItems;

/** 陰陽玉 */
public class EntityOnmyoudama extends EntityTHShot
{
	private boolean shootingFlag;
	private boolean userHit;
	private int hitTimer;

	//ワールド読み込み時に呼び出されるコンストラクト
	public EntityOnmyoudama(EntityType<? extends EntityOnmyoudama> type, Level level)
	{
		super(type, level);
	}

	public EntityOnmyoudama(EntityType<? extends EntityOnmyoudama> type, Level level, LivingEntity user, Entity source,
		Vec3 pos, Vec3 angle, float slope, Vec3 rotate, float rotationSpeed, int rotationEnd,
		double firstSpeed, double limitSpeed, double acceleration,
		Vec3 gravity, int color, float size, float damage, int delay, int end, int special)
	{
		super(type, level, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration,
				gravity, color, size, damage, delay, end, special);
		shootingFlag = false;
		userHit = false;
		hitTimer = 0;
	}

	//ショットが存在する限り呼び出されるメソッド
	@Override
	public void tick()
	{
		if(shootingFlag)
		{
			shotAcceleration = 0.0D;
		}
		if(user != null)
		{
			if(user instanceof Player player)
			{
				if(player.isUsingItem() && !shootingFlag)
				{
					Vec3 vec3 = THShotLib.getVecFromAngle( -getYRot(), -getXRot(), 0.6D);

					setPos(user.getX() + vec3.x * 2, THShotLib.getPosYFromEye(user, -0.1D) + vec3.y, user.getZ() + vec3.z);
					setRot(-user.getYRot(), -user.getXRot());

					if(getShotSize() < 3.00F)
					{
						setShotSize(getShotSize() + 0.06F);
					}
					if(!player.getMainHandItem().isEmpty())
					{
						if(!player.getMainHandItem().is(ModItems.HAKUREI_SHRINE_MAIDEN_STICK.get()))
						{
							if(!level().isClientSide)
							{
								discard();
							}
							return;
						}
					}
					else
					{
						if(!level().isClientSide)
						{
							discard();
						}
						return;
					}
					return;
				}
				else if(shootingFlag == false)
				{
					if(!player.getMainHandItem().isEmpty())
					{
						if(!player.getMainHandItem().is(ModItems.HAKUREI_SHRINE_MAIDEN_STICK.get()))
						{
							if(!level().isClientSide)
							{
								discard();
							}
							return;
						}
					}
					else
					{
						if(!level().isClientSide)
						{
							discard();
						}
						return;
					}
					tickCount = 1;
					shotSpeed = 0.5D;
					setVector();
					shotAcceleration = 1.0D;
					shootingFlag = true;
				}
			}
		}
		else
		{
			if(!level().isClientSide)
			{
				discard();
				return;
			}
		}

		shotDamage = getShotSize() * 6.0F;

		super.tick();

		//始点（現在地）
		Vec3 supVec3d1 = new Vec3(getX(), getY(), getZ());
		//終点（現在地の真上に弾の大きさ分離した点）
		Vec3 supVec3d2 = new Vec3(getX(), getY() + getShotSize(), getZ());
		//始点と終点からブロックとの衝突を取得
		BlockHitResult blockHit = clipBlocks(supVec3d1, supVec3d2);

		if(blockHit != null)
		{
			double diveY = blockHit.getBlockPos().getY() - (getY() + getShotSize());
			setPos(getX(), blockHit.getBlockPos().getY() + getShotSize() + 0.01D + diveY, getZ());
			setRot(getYRot(), getXRot() * -1.0F);
			setVector();
			motionY *= 0.9D;
		}

		if(!userHit)
		{
			//始点を登録
			Vec3 vec3d = new Vec3(getX(), getY(), getZ());
			//終点を登録
			Vec3 vec3d2 = new Vec3(getX() + motionX, getY() + motionY, getZ() + motionZ);
			//始点と終点からブロックとの衝突を取得
			BlockHitResult movingHit = clipBlocks(supVec3d1, supVec3d2);
			if(!hitUserCheck(movingHit, vec3d, vec3d2))
			{
				hitTimer++;
				if(hitTimer >= 2)
				{
					userHit = true;
				}
			}
		}
	}

	@Override
	public boolean userHitCheck(Entity entity)
	{
		return userHit;
	}

	//使用者自身にも当たるかどうか
	@Override
	protected boolean isUserHit()
	{
		return true;
	}

	@Override
	public boolean entityHitSpecialProcess(Entity hitEntity)
	{
		setRot(getYRot() * -1.0F, getXRot() * -1.0F);
		setVector();
		motionX *= 0.8F;
		motionY *= 0.8F;
		motionZ *= 0.8F;

		return false;
	}

	//跳ね返せる弾かどうか
	@Override
	protected boolean isReturnableShot()
	{
		return true;
	}

	//当たり判定の追加
	@Override
	public boolean hitCheckEx(Entity entity)
	{
		return false;
	}

	//弾の強さ
	public int getShotStrength()
	{
		return 10;
	}
}
