package thkaguyamod.entity.item;

import java.util.List;
import java.util.Set;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModItems;
import thkaguyamod.init.ModSounds;

import static thkaguyamod.danmaku.DanmakuConstants.PURPLE;

/** スキマ */
public class EntitySukima extends Entity
{
	//同期データ (1.7.10 の dataWatcher 16 = スキマの色 に相当)
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(EntitySukima.class, EntityDataSerializers.INT);

	private int count;

	private double warpPosX;
	private double warpPosY;
	private double warpPosZ;
	/** 使用者 */
	public LivingEntity userEntity;
	private float warpAngle;
	private boolean warp;
	private int pattern;//スキマのアニメーション用　未使用

	public EntitySukima(EntityType<? extends EntitySukima> type, Level level)
	{
		super(type, level);
		//TODO K3-review: 1.7.10 は preventEntitySpawning / setSize(1.5F, 1.0F) / yOffset = 0.5F を
		//すべてコメントアウトしており、Entity 既定サイズのままだった。1.21.1 でも
		//getDimensions() で 1.7.10 の Entity 既定値（0.6F, 1.8F）を返す。
		pattern = 0;
	}

	/** 当たり判定の大きさ（1.7.10 の Entity 既定値。setSize は呼ばれていない） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(0.6F, 1.8F);
	}

	public EntitySukima(EntityType<? extends EntitySukima> type, Level level, LivingEntity entityLivingBase, double x, double y, double z, float angle)
	{
		super(type, level);

		setPos(x, y, z);
		setRot(angle, 0.0F);
		count = 0;
		pattern = 0;
		setColor(1);
		setNewWarpPoint();
		userEntity = entityLivingBase;
	}

	public EntitySukima(EntityType<? extends EntitySukima> type, Level level, LivingEntity entityLivingBase, double x, double y, double z, int sukimaColor)
	{
		this(type, level);

		setPos(x, y, z);
		float rotationYaw = (float)Math.atan2(x - entityLivingBase.getX(), z - entityLivingBase.getZ()) / 3.141593F * 180F;
		setRot(360F - rotationYaw, 0.0F);
		count = 0;
		pattern = 0;
		setColor(sukimaColor);
		setNewWarpPoint();
		userEntity = entityLivingBase;
	}

	public EntitySukima(EntityType<? extends EntitySukima> type, Level level, LivingEntity entityLivingBase, double x, double y, double z, float angle, int sukimaColor)
	{
		this(type, level);

		setPos(x, y, z);
		setRot(angle + 180F, 0.0F);
		count = 0;
		pattern = 0;
		setColor(sukimaColor);
		setNewWarpPoint();
		userEntity = entityLivingBase;
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_COLOR, 0);//スキマの色
	}

	//押すことができるか
	@Override
	public boolean isPushable()
	{
		return false;
	}

	//当たり判定の有無　falseだと右クリックの選択ですらできない
	@Override
	public boolean canBeCollidedWith()
	{
		return true;
	}

	//照準で狙えるかどうか（1.7.10 の canBeCollidedWith が兼ねていた照準フィルタ相当）
	@Override
	public boolean isPickable()
	{
		return true;
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		super.tick();

		//直接ワープスキマは時間消滅する
		if(getColor() == 16 || getColor() >= 32)
		{
			if(tickCount > 24)
			{
				discard();
			}
		}

		// 飛光虫ネストのスキマ
		if(getColor() == 17)
		{
			if(tickCount == 5)
			{
				THShotLib.createLaserA(userEntity, this, THShotLib.pos_Entity(this), THShotLib.angle(getYRot() - 180F, getXRot()), 0.1D, 1.4D, 0.1D, THShotLib.gravity_Zero(),
						LaserData.laser(PURPLE, 0.1F, 4.0F, 8.0F, 0, 40, 0));
			}
			if(tickCount > 9)
			{
				discard();
			}
			return;
		}

		//***********スキマとEntityの当たり判定を取る****************//
		List<Entity> list = level().getEntities(this, getBoundingBox().inflate(10.0D, 10.0D, 10.0D));//判定を行う箱の大きさ

		double minX, minY, minZ, maxX, maxY, maxZ;
		minX = getX() - 1.0D;
		minY = getY() - 1.5D;
		minZ = getZ() - 1.0D;
		maxX = getX() + 1.0D;
		maxY = getY() + 1.0D;
		maxZ = getZ() + 1.0D;

		if(list != null && list.size() > 0)
		{
			for(int j1 = 0; j1 < list.size(); j1++)
			{
				Entity entity = list.get(j1);
				if(!(entity instanceof EntitySukima) &&
						entity.getX() > minX && entity.getX() < maxX &&
						entity.getY() > minY && entity.getY() < maxY &&
						entity.getZ() > minZ && entity.getZ() < maxZ)
				{
					//通常のスキマなら新しいワープポイントを探す
					//if(getColor() != 16)
					//{
						setNewWarpPoint();
					//}
					//TODO K3-review: 1.7.10 の Entity.timeUntilPortal（ポータル再入防止カウンタ）は
					//1.21.1 に同名フィールドが無い。同義の portalCooldown（1.21.1 では
					//getPortalCooldown()/setPortalCooldown()、毎tick自動デクリメント）に置き換えた。
					if(warp && entity.getPortalCooldown() == 0 && !level().isClientSide)//ワープが有効だった場合の入ったEntityの処理
					{
						double entityY = entity.getY();
						entity.xo += warpPosX - entity.getX();
						entity.yo += warpPosY - entity.getY();
						entity.zo += warpPosZ - entity.getZ();
						//1.7.10 は posY だけ直接代入していた（当たり判定の再計算のため setPos で代替）
						entity.setPos(entity.getX(), warpPosY, entity.getZ());
						if(entity instanceof ServerPlayer player)
						{
							//1.7.10 の playerNetServerHandler.setPlayerLocation に相当
							player.connection.teleport(warpPosX, warpPosY, warpPosZ, player.getYRot(), player.getXRot(), Set.of());
						}
						else if(!level().isClientSide)
						{
							entity.setPos(warpPosX, warpPosY, warpPosZ);
						}
						//entity.yo = warpPosY;
						if(entity instanceof EntityTHShot shot)
						{
							//1.7.10 は shot.angle を直接書き換えていたが、1.21.1 の Vec3 は不変なので再代入する
							Vec3 move = THShotLib.getVecFromAngle(warpAngle - THShotLib.getAngleAndAngleSpan(getYRot(), shot.getYRot()), shot.getXRot());
							shot.angle = new Vec3(-move.x, -move.y, move.z);
							double speed = shot.getSpeed();
							shot.motionX = shot.lastShotMotionX = shot.angle.x * speed;
							shot.motionY = shot.lastShotMotionY = shot.angle.y * speed;
							shot.motionZ = shot.lastShotMotionZ = shot.angle.z * speed;
							//shot.updateAngle();
						}
						else
						{
							entity.setYRot(entity.getYRot() + warpAngle);
							Vec3 motion = entity.getDeltaMovement();
							entity.setDeltaMovement(
									motion.x * -Math.sin(entity.getYRot()),
									motion.y,
									motion.z * Math.cos(entity.getYRot()));
						}
						/*if(warpPosY < entityY)
						{
							entity.fallDistance = (float)(warpPosY - entityY);
						}
						else
						{
							entity.fallDistance = 0.0F;
						}*/
						//entity.onGround = true;
						entity.setPortalCooldown(40);//２秒間スキマの影響を受けない
						playWarpSound(entity);
					}
				}
			}
		}
	}

	/** ワープ音を鳴らす（1.7.10 の THKaguyaConfig.sukimaWarpSE に相当） */
	private void playWarpSound(Entity entity)
	{
		if(ModConfig.useDefaultGapSE)
		{
			//1.7.10 の既定値 "mob.endermen.portal"
			level().playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
		else
		{
			level().playSound(null, entity.blockPosition(), ModSounds.SUKIMA.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
	}

	//プレイヤーが、Entityに対して右クリックしたときに呼び出される（1.7.10 の interactFirst に相当）
	@Override
	public InteractionResult interact(Player player, InteractionHand hand)
	{
		if(getColor() >= 16)
		{
			return InteractionResult.PASS;
		}
		ItemStack itemstack = player.getMainHandItem();//プレイヤーの選択しているアイテムを取得
		if(!level().isClientSide)
		{
			//TODO K3-review: 1.7.10 は「染料なら染料のダメージ値をそのままスキマの色にする」
			//（染料 16 色のメタデータとスキマの色番号 0-15 が一致していた）。
			//1.21.1 では DyeItem + DyeColor なので DyeColor.getId() で番号を取得する（対応は同一）。
			if(!itemstack.isEmpty() && itemstack.getItem() instanceof DyeItem dye)
			{
				if(ModConfig.useDefaultGapSE)
				{
					//1.7.10 の "mob.endermen.portal"
					level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.5F, 0.4F);
				}
				else
				{
					level().playSound(null, player.blockPosition(), ModSounds.SUKIMA.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
				}
				setColor(dye.getDyeColor().getId());
			}
			//悔悟の棒を持った状態
			else if(!itemstack.isEmpty() && itemstack.is(ModItems.ROD_OF_REMORSE.get()))
			{
				//アイテム：スキマをドロップさせる
				spawnAtLocation(new ItemStack(ModItems.GAP.get()));
				discard();//消滅させる
			}
			else if(player.isShiftKeyDown())//染料も持ってなく、スニークなら
			{
				//アイテム：スキマをドロップさせる
				spawnAtLocation(new ItemStack(ModItems.GAP.get()));
				discard();//消滅させる
			}
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * 保存するデータの書き込み
	 */
	@Override
	protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag nbtTagCompound)
	{
		nbtTagCompound.putShort("color", (short)getColor());
	}

	/**
	 * 保存したデータの読み込み
	 */
	@Override
	protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag nbtTagCompound)
	{
		setColor(nbtTagCompound.getShort("color"));
	}

	//スキマの色を設定する
	public void setColor(int color)
	{
		this.entityData.set(DATA_COLOR, color);
	}

	//スキマの色を取得する
	public int getColor()
	{
		return this.entityData.get(DATA_COLOR);
	}

	//新しいワープポイントを取得する
	public void setNewWarpPoint()
	{
		double nearDistance = 1000.0D;//一番スキマの距離として、ありえないデタラメな値を保存
		double distance;
		EntitySukima setSukima;
		EntitySukima nearSukima = null;
		List<Entity> list = level().getEntities(this, getBoundingBox().inflate(128.0D, 128.0D, 128.0D));//周囲256x256x256の立方体内が有効範囲

		if(list != null && list.size() > 0)//範囲内のEntityをチェック
		{
			for(int j1 = 0; j1 < list.size(); j1++)
			{
				Entity entity = list.get(j1);
				if(entity instanceof EntitySukima)//スキマのみチェック
				{
					setSukima = (EntitySukima)entity;
					if((getColor() % 32) == (setSukima.getColor() % 32))//スキマ同士の色が同じ場合のみ判定
					{
						distance = distanceTo(entity);
						if(distance < nearDistance && distance > 5.0D)//距離が５m以上で、距離が近いなら一番近いスキマとして保存
						{
							nearDistance = distance;
							nearSukima = setSukima;
						}
					}
				}
			}
			if(nearSukima != null)//範囲内に有効なスキマがあればワープさせる
			{
				float angleNS = nearSukima.getYRot() / 180F * 3.141593F;
				warpPosX = nearSukima.getX() + (double)Math.sin(angleNS) * 1.8D;
				warpPosY = nearSukima.getY();
				warpPosZ = nearSukima.getZ() - (double)Math.cos(angleNS) * 1.8D;
				warpAngle = 180F - ((getYRot() % 360) - (nearSukima.getYRot() % 360));
				warp = true;
			}
			else//範囲内に有効なスキマがなければ何も起きない
			{
				warp = false;
			}
		}
	}
}