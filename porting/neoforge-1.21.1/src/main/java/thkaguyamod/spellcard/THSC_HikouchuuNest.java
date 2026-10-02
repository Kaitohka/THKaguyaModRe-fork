package thkaguyamod.spellcard;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.entity.item.EntitySukima;
import thkaguyamod.init.ModEntities;

/** 幻巣「飛光虫ネスト」 */
public class THSC_HikouchuuNest extends THSpellCard
{
	public THSC_HikouchuuNest()
	{
		this.setNeedLevel(4);
		this.setRemoveTime(50);
		this.setEndTime(160);
		this.setOriginalUserName(YUKARI);
	}

	@Override
	public void spellcard_main()
	{
		boolean flag = false;
		int count = 0;
		while(!flag && count < 4)
		{
			double length = rand.nextDouble() * 5.0D + 1.0D;
			float angleXZ = (card.getYRot() + 90F) / 180F * (float)Math.PI + rand.nextFloat() * (float)Math.PI;
			float angleY = rand.nextFloat() * (float)Math.PI - (float)Math.PI / 2.0F;
			double xVector = Math.sin(angleXZ) * Math.cos(angleY) * length;
			double yVector = Math.sin(angleY) * length;
			double zVector = Math.cos(angleXZ) * Math.cos(angleY) * length;
			double xPos, yPos, zPos;
			// 始点（現在地）
			Vec3 vec3d = new Vec3(user.getX(), user.getY(), user.getZ());
			// 終点（現在地に移動量を足した点）
			Vec3 vec3d1 = new Vec3(user.getX() + xVector, user.getY() + yVector, user.getZ() + zVector);
			// 始点と終点からブロックとの当たりを取得
			// 1.7.10: world.func_147447_a(vec3d, vec3d1, false, true, true) → Level.clip(ClipContext)
			BlockHitResult movingObjectPosition = clipBlocks(vec3d, vec3d1);
			// 何らかのブロックに当たっているなら
			if(movingObjectPosition != null && movingObjectPosition.getType() == HitResult.Type.BLOCK)
			{
				count++;
			}
			else
			{
				xPos = user.getX() + xVector;
				yPos = user.getY() + yVector;
				zPos = user.getZ() + zVector;
				EntitySukima entitySukima = new EntitySukima(ModEntities.SUKIMA.get(), level(), user, xPos, yPos, zPos, user.getYRot(), 17);
				if(!level().isClientSide)
				{
					level().addFreshEntity(entitySukima);
				}
				flag = true;
			}
		}
	}

	/** ブロックとの当たり判定（液体を含む、1.7.10 func_147447_a 相当） */
	private BlockHitResult clipBlocks(Vec3 startPos, Vec3 endPos)
	{
		BlockHitResult blockHit = level().clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, user));
		return blockHit != null && blockHit.getType() == HitResult.Type.BLOCK ? blockHit : null;
	}
}
