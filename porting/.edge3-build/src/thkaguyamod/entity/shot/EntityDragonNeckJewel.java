package thkaguyamod.entity.shot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THShotLib;

import static thkaguyamod.danmaku.DanmakuConstants.FORM_LIGHT;

/** 龍の頸の玉 着弾地点に雷を落とす */
public class EntityDragonNeckJewel extends EntityTHShot
{
	public float color_r;
	public float color_g;
	public float color_b;

	public EntityDragonNeckJewel(EntityType<? extends EntityDragonNeckJewel> type, Level level)
	{
		super(type, level);
	}

	public EntityDragonNeckJewel(EntityType<? extends EntityDragonNeckJewel> type, Level level, LivingEntity user, Vec3 angle, float cr, float cg, float cb)
	{
		super(type, level, user, user, THShotLib.pos_Living(user), angle, 0F, THShotLib.rotate_Default(), 0F, 9999, 0.9D, 0.9D, 0.0D, THShotLib.gravity_Default(), ShotData.shot(0, 0, 1.0F, 7.0F));
		color_r = cr;
		color_g = cg;
		color_b = cb;
	}

	public EntityDragonNeckJewel(EntityType<? extends EntityDragonNeckJewel> type, Level level, LivingEntity user, Entity source, Vec3 pos, Vec3 angle, float cr, float cg, float cb)
	{
		super(type, level, user, source, pos, angle, 0F, THShotLib.rotate_Default(), 0F, 9999, 0.9D, 0.9D, 0.0D, THShotLib.gravity_Default(), ShotData.shot(0, 0, 1.0F, 7.0F));
		color_r = cr;
		color_g = cg;
		color_b = cb;
	}

	//ブロックやEntityに当たったときの処理
	@Override
	protected boolean onImpact(HitResult hitResult)
	{
		super.onImpact(hitResult);

		if( !createLightningBoltDanmaku(hitResult) )
		{
			delete();
			return true;
		}

		return false;
	}

	protected boolean createLightningBoltDanmaku(HitResult hitResult)
	{
		if( hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof EntityTHShot )
		{
			return true;
		}

		int colors[] = {0, 2, 4, 5, 6};
		float angle = random.nextFloat() * 360F;
		for(int i = 0; i < 5; i++)
		{
			THShotLib.createLaserA(user, this, THShotLib.pos(getX(), getY() + 0.4D, getZ()), THShotLib.angle(angle, -70F), 0.1D, 0.3D, 0.1D, THShotLib.gravity_Zero(), LaserData.laser(colors[i], 0.15F, 3.0F, 3.0F, 0, 40, 0));
			angle += 72F;
		}
		for(int i = 0; i < 10; i++)
		{
			double xVector = random.nextGaussian() * 0.2D;
			double yVector = 0.8D + random.nextDouble() * 0.2D;
			double zVector = random.nextGaussian() * 0.2D;

			ShotData shot = ShotData.shot(FORM_LIGHT, colors[random.nextInt(5)], 0.3F, 3.0F, 1, 120);
			THShotLib.createShot(user, this, THShotLib.pos(getX(), getY() + 0.4D, getZ()), THShotLib.angle(xVector, yVector, zVector), 0F, 0.4D, 0.4D, 0.00D, THShotLib.gravity(0.0D, -0.01D, 0.0D), shot);
		}

		return false;
	}

	@Override
	public boolean blockHitSpecialProcess(BlockHitResult hitResult)
	{
		return createLightningBoltDanmaku(hitResult);
	}
}
