package thkaguyamod.entity.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.entity.shot.EntityTHShot;
import thkaguyamod.init.ModEntities;
import thkaguyamod.init.ModItems;

/** 銀のナイフ */
public class EntitySilverKnife extends Entity
{
	public static final byte KNIFE_COLOR_BLUE	= 0x00;
	public static final byte KNIFE_COLOR_RED	= 0x01;
	public static final byte KNIFE_COLOR_GREEN	= 0x02;
	public static final byte KNIFE_COLOR_WHITE	= 0x03;

	//同期データ (1.7.10 の dataWatcher 18-20 に相当)
	private static final EntityDataAccessor<Integer> DATA_KNIFE_COUNT = SynchedEntityData.defineId(EntitySilverKnife.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_KNIFE_COLOR = SynchedEntityData.defineId(EntitySilverKnife.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> DATA_KNIFE_Z_ANGLE = SynchedEntityData.defineId(EntitySilverKnife.class, EntityDataSerializers.INT);

	private byte color;//ナイフの色
	private int xTile;//Xの整数座標
	private int yTile;//Yの整数座標
	private int zTile;//Zの整数座標
	private int inTile;//今いる場所のブロックのBlockState ID（1.7.10 のブロックIDに相当）
	//TODO K3-review: 1.7.10 の inData（ブロックメタデータ）は 1.21.1 に存在しない。
	//NBT 互換のためフィールドと入出力は残すが常に 0 になる。
	private int inData;
	private boolean inGround;//地中にいるか
	private int ticksInAir;

	/** 使用者 */
	public LivingEntity shootingEntity;
	public LivingEntity playerEntity;

	private int ticksInGround;
	public double accelerationX;
	public double accelerationY;
	public double accelerationZ;

	private boolean hit;//衝突済み判定
	private boolean rockhit;//刺さらないブロックの上に乗っているかの判定

	public float angleZ;

	private int lastTime;
	private double lastShotMotionX;
	private double lastShotMotionY;
	private double lastShotMotionZ;
	private int delay;

	/*
	 * 弾の移動量。EntityTHShot / EntityShotMaterial と同じく 1.7.10 の motionX/Y/Z 相当の
	 * 独自フィールドで管理する（このEntityは move() を使わず tick() で明示的に位置へ加算する）。
	 */
	public double motionX;
	public double motionY;
	public double motionZ;

	/** ワールド読み込み時に呼び出されるコンストラクタ */
	public EntitySilverKnife(EntityType<? extends EntitySilverKnife> type, Level level)
	{
		super(type, level);
		xTile = -1;
		yTile = -1;
		zTile = -1;
		inTile = 0;
		inData = 0;
		inGround = false;

		hit = false;
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(0.1F, 0.1F) に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(0.1F, 0.1F);
	}

	public EntitySilverKnife(EntityType<? extends EntitySilverKnife> type, Level level, Vec3 pos, Vec3 angle, double speed, int t)
	{
		this(type, level, null, pos, angle, speed, t);
	}

	public EntitySilverKnife(EntityType<? extends EntitySilverKnife> type, Level level, LivingEntity entityLivingBase, Vec3 pos, Vec3 angle, double speed, int t)
	{
		super(type, level);
		color = (byte)t;//ナイフの色を保存
		setKnifeColor(color);
		shootingEntity = entityLivingBase;
		playerEntity = entityLivingBase;

		motionX = angle.x * speed;
		motionY = angle.y * speed;
		motionZ = angle.z * speed;
		lastShotMotionX = motionX;
		lastShotMotionY = motionY;
		lastShotMotionZ = motionZ;
		accelerationX = (motionX / 2.0);
		accelerationY = (motionY / 2.0);
		accelerationZ = (motionZ / 2.0);
		setKnifeZAngle(90.0F);
		float rotationYaw = (float)Math.atan2(motionX, -motionZ) / 3.141593F * 180F;
		float rotationPitch = (float)Math.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)) / 3.141593F * 180F;
		//1.7.10 の setLocationAndAngles に相当
		moveTo(pos.x + angle.x * speed, pos.y + angle.y * speed, pos.z + angle.z * speed, rotationYaw, rotationPitch);
		//1.7.10 は prevPos を発射地点に上書きしていた
		this.xo = pos.x;
		this.yo = pos.y;
		this.zo = pos.z;

		lastTime = 0;

		delay = 0;
		if(t == 3)
		{
			delay = -30;
			//setKnifeCount(-30);
		}
	}

	//TODO K3-review: 1.7.10 の SILVER_KNIFE（銀のナイフ）エンティティ登録が未実施のため、
	//このクラスは thkaguyamod.init.ModEntities に `SILVER_KNIFE` (silver_knife) を追加するまで
	//コンパイルできない（内部の生成箇所 2 か所で ModEntities.SILVER_KNIFE を参照）。
	//ModEntities 追記後にこのコメントを削除すること。

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_KNIFE_COUNT, 0);
		builder.define(DATA_KNIFE_COLOR, (byte)0);
		builder.define(DATA_KNIFE_Z_ANGLE, 0);
	}

	/**
	 * 移動量から速度を取得する
	 * @return 速度を返す
	 */
	public double getEntitySpeed()
	{
		return Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
	}

	/** 消滅させる */
	public void delete()
	{
		if(!level().isClientSide)
		{
			discard();
		}
	}

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		super.tick();

		if(tickCount > 1)
		{
			//時間が停止しているなら
			if(tickCount <= lastTime)
			{
				setKnifeCount(getKnifeCount());
				return;
			}
			//時間が通常通り流れているなら
			else
			{
				motionX = lastShotMotionX;
				motionY = lastShotMotionY;
				motionZ = lastShotMotionZ;
				if(!level().isClientSide)
				{
					delay++;
					if(delay >= 0)
					{
						setKnifeCount(getKnifeCount() + 1);
					}
				}
			}
		}

		if(getKnifeCount() < 0)
		{
			setKnifeCount(getKnifeCount() + 1);
			return;
		}

		//どこともぶつからずに長時間経過した場合、消滅させる
		if(!hit && tickCount >= 300)
		{
			delete();
		}
		//ぶつかっている場合は、かなり長い間停滞する
		else if(hit && tickCount >= 6000)
		{
			delete();
		}

		//白色のナイフで、壁に衝突していたら消滅させる
		if(getKnifeColor() == 3 && hit && tickCount >= 1)
		{
			delete();
		}

		rockhit = false;//硬いブロックと衝突しているかの判定変数

		//地面の中なら
		if(inGround)
		{
			//衝突しているブロックのBlockState IDを取得（1.7.10 の Block.getIdFromBlock に相当）
			BlockPos hitPos = new BlockPos(xTile, yTile, zTile);
			BlockState hitState = level().getBlockState(hitPos);
			int hitBlockId = Block.getId(hitState);
			//TODO K3-review: 1.7.10 はブロックメタデータも比較していたが 1.21.1 には無い。
			//BlockState ID の比較に置き換えた（メタデータ相当は BlockState に含まれるため実質同等）。
			int hitBlockMetadata = 0;

			if(hitBlockId != inTile || hitBlockMetadata != inData)
			{
				inGround = false;
				motionX *= random.nextFloat() * 0.2F;
				motionY *= random.nextFloat() * 0.2F;
				motionZ *= random.nextFloat() * 0.2F;
				ticksInGround = 0;
				setKnifeCount(3);
			}
			else
			{
				ticksInGround++;//地中カウンタを増やす

				if(ticksInGround == 3000)//地中カウンタが3000なら消滅
				{
					delete();
				}
			}

			return;//これ以上の処理は行わない
		}
		else
		{
			ticksInAir = 5;
		}

		//始点を登録
		Vec3 posStart = THShotLib.pos_Entity(this);
		//終点を登録
		Vec3 posEnd = THShotLib.pos(getX() + motionX, getY() + motionY, getZ() + motionZ);
		//始点と終点からブロックとの衝突を取得（液体も含まない、1.7.10 の func_147447_a(..., false, true, false)）
		BlockHitResult blockHit = clipBlocks(posStart, posEnd, ClipContext.Fluid.NONE);

		posStart = THShotLib.pos_Entity(this);
		posEnd = THShotLib.pos(getX() + motionX, getY() + motionY, getZ() + motionZ);
		//ブロックと衝突していたなら
		if(blockHit != null)
		{
			//終点を衝突した点に変更
			posEnd = blockHit.getLocation();
		}

		Entity hitEntity = null;
		//このEntityから移動後までの線分に、指定分の範囲を追加した直方体と衝突するEntityのリストを取得
		List<Entity> list = level().getEntities(this, getBoundingBox().expandTowards(motionX, motionY, motionZ).inflate(0.01D, 0.01D, 0.01D));
		//現在最も近い距離を保存
		double nearDistance = 999.0D;

		for(int j = 0; j < list.size(); j++)
		{
			// 衝突リストから、j番目のEntityを取得
			Entity entityCheck = list.get(j);

			if(entityCheck.canBeCollidedWith() && // 当たり判定を持ったEntityであるか？
					entityCheck != shootingEntity && // 使用者ではないか？
					ticksInAir >= 5 &&
					!(entityCheck instanceof EntityTHShot))//弾幕には当たらない
			{
				if(entityCheck instanceof EntitySilverKnife entitySilverKnife)
				{
					if(this.getKnifeColor() != 3 && entitySilverKnife.getKnifeColor() != 3)
					{
						float boxExtend = 0.01F;
						AABB axisalignedbb = entityCheck.getBoundingBox().inflate(boxExtend, boxExtend, boxExtend);
						Optional<Vec3> intercept = axisalignedbb.clip(posStart, posEnd);

						//Entityとの衝突がないなら、このEntityはパスする
						if(intercept.isPresent())
						{
							//始点からEntityに衝突した点までの距離を取得
							double distance = posStart.distanceTo(intercept.get());
							//今までで一番近いEntityなら、Entityと距離を記憶する
							if(distance < nearDistance || nearDistance == 999.0D)
							{
								hitEntity = entityCheck;
								nearDistance = distance;
							}
						}
					}
				}
				else
				{
					float boxExtend = 0.1F;
					AABB axisalignedbb = entityCheck.getBoundingBox().inflate(boxExtend, boxExtend, boxExtend);
					Optional<Vec3> intercept = axisalignedbb.clip(posStart, posEnd);
					//Entityとの衝突がないなら、このEntityはパスする
					if(intercept.isPresent())
					{
						//始点からEntityに衝突した点までの距離を取得
						double d1 = posStart.distanceTo(intercept.get());
						//今までで一番近いEntityなら、Entityと距離を記憶する
						if(d1 < nearDistance || nearDistance == 999.0D)
						{
							hitEntity = entityCheck;
							nearDistance = d1;
						}
					}
				}
			}
		}

		//Entityに当たっていたなら
		Entity hitPlayer = hitEntity;
		if(hitPlayer != null && hitPlayer != shootingEntity)
		{
			if(hitPlayer instanceof Player entityPlayer)
			{
				//1.7.10 の capabilities.disableDamage に相当。旧版は
				//「無敵プレイヤー or 撃ったのがプレイヤー本人」なら当たりを無効化していた。
				if(entityPlayer.getAbilities().invulnerable || this.shootingEntity instanceof Player)
				{
					hitPlayer = null;
				}
			}
		}

		if(hitPlayer != null)
		{
			//Entityに当たっていたなら
			if(!(hitPlayer instanceof EntitySilverKnife))
			{
				//衝突処理
				onImpact(hitPlayer);

				//ナイフの動きを鈍らせ、逆方向に働かせる
				motionX *= -0.210000000149011612D;
				motionY *= -0.210000000149011612D;
				motionZ *= -0.210000000149011612D;
				accelerationX *= -0.01;
				accelerationY *= -0.01;
				accelerationZ *= -0.01;
				setYRot(getYRot() + 180F);
				this.yRotO += 180F;
				//生存カウンタを3まで戻す
				setKnifeCount(3);
				//衝突チェックをtrueにする
				hit = true;
				setKnifeZAngle(0.0F);
			}
			//1.7.10 は Entity ヒットのときも xTile/yTile/zTile に衝突座標を入れていた。
			//1.21.1 では Entity ヒット時にブロック座標が無いため記録しない（上空で刺さった判定にならない）。
		}

		if(blockHit != null)
		{
			xTile = blockHit.getBlockPos().getX();
			yTile = blockHit.getBlockPos().getY();
			zTile = blockHit.getBlockPos().getZ();
			BlockState hitState = level().getBlockState(blockHit.getBlockPos());
			inTile = Block.getId(hitState);
			inData = 0;//1.7.10 の getBlockMetadata 相当は 1.21.1 に存在しない
			//当たったブロックの材質を取得（1.7.10 の Material は 1.21.1 に無いため SoundType で近似）
			SoundType material = hitState.getSoundType();

			//当たったブロックの材質が、rock,iron,glass,iceなら
			//TODO K3-review: 1.7.10 の Material.rock/iron/glass/ice に直接対応する API が無い。
			//金属音を出す硬質ブロックの判定を SoundType.STONE / METAL / GLASS で近似した
			//（氷は 1.21.1 では GLASS 音なので含まれる。MOD 独自 Material は判定から漏れる可能性あり）。
			if(material == SoundType.STONE || material == SoundType.METAL || material == SoundType.GLASS || material == SoundType.DEEPSLATE
					|| material == SoundType.NETHERITE_BLOCK || material == SoundType.COPPER)
			{
				//速度が遅すぎでなければ
				if(Math.abs(getEntitySpeed()) > 0.08F)
				{
					//銀のナイフに衝突しているなら
					if(hitPlayer instanceof EntitySilverKnife entitySilverKnife)
					{
						//どちらかが白のナイフでなければ
						if(entitySilverKnife.getKnifeColor() != 3 && this.getKnifeColor() != 3)
						{
							//衝突時の金属音のような音を出す
							level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.5F, 5.0F);

							//衝突処理をし、少しランダムに向きを変える
							float randangle = (float)random.nextInt(120);
							setYRot(getYRot() + 165.0F - 15.0F + randangle);
							motionX += Math.sin(randangle);
							motionZ += Math.cos(randangle);
							motionX *= -0.150000000149011612D * Math.sin(randangle);//移動方向を逆にする
							motionY *= -0.150000000149011612D;
							motionZ *= -0.150000000149011612D * Math.cos(randangle);
							accelerationX *= -0.01;
							accelerationZ *= -0.01;
							//このナイフと当たったナイフの上下関係で速度に変化が出る
							if(getY() > hitPlayer.getY())
							{
								accelerationY *= 0.01;
							}
							else
							{
								accelerationY *= -1.2;
							}
						}
					}
					//硬いブロックに当たっているなら
					if(hitPlayer == null)
					{
						//衝突時の金属音のような音を出す
						level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.5F, 5.0F);

						//跳ね返り係数
						double returnRate;
						if(getKnifeColor() == 0)
						{
							returnRate = 0.01D;
						}

						double supPosX1 = getX();
						double supPosX2 = getX() + motionX;
						double supPosY1 = getY();
						double supPosY2 = getY() + motionY;
						double supPosZ1 = getZ();
						double supPosZ2 = getZ() + motionZ;
						//始点を登録
						Vec3 supVec3d1 = new Vec3(supPosX1, supPosY1, supPosZ1);
						//終点を登録
						Vec3 supVec3d2 = new Vec3(supPosX2, supPosY2, supPosZ2);
						//始点と終点からブロックとの衝突を取得
						BlockHitResult supMovingObjectPosition = clipBlocks(supVec3d1, supVec3d2, ClipContext.Fluid.NONE);

						//何かにあたっている間
						while(supMovingObjectPosition != null)
						{
							returnRate = 0.25D * getEntitySpeed();
							if(getKnifeColor() != 1)
							{
								returnRate = 0.00001D;
							}
							//衝突したブロックの面ごとに判定
							switch(supMovingObjectPosition.getDirection().get3DDataValue())
							{
								case 0://下面
									motionY *= -returnRate;
									accelerationY *= -returnRate;
									break;
								case 1://上面
									motionY *= -returnRate;
									accelerationY *= -returnRate;
									setKnifeCount(0);
									if(motionY < 0.2D)
									{
										motionX *= 0.3D;
										motionZ *= 0.3D;
									}
									if(getKnifeZAngle() > 0.0F)
									{
										setKnifeZAngle(getKnifeZAngle() - 21.0F);
									}
									else
									{
										setKnifeZAngle(0.0F);
									}
									break;
								case 2:
									motionZ *= -returnRate;
									accelerationZ *= -returnRate;
									break;
								case 3:
									motionZ *= -returnRate;
									accelerationZ *= -returnRate;
									break;
								case 4:
									motionX *= -returnRate;
									accelerationX *= -returnRate;
									break;
								default:
									motionX *= -returnRate;
									accelerationX *= -returnRate;
									break;
							}
							if(getKnifeColor() != 1)
							{
								motionX *= 0.3D;
								motionY *= 0.3D;
								motionZ *= 0.3D;
								accelerationX *= 0.3D;
								accelerationY *= 0.3D;
								accelerationZ *= 0.3D;
							}
							//rotationYaw += 180F;//向きを180度変える
							supPosX1 = supPosX1 + (supMovingObjectPosition.getLocation().x - supPosX1) * 0.999D;
							supPosX2 = supPosX1 + motionX;
							supPosY1 = supPosY1 + (supMovingObjectPosition.getLocation().y - supPosY1) * 0.999D;
							supPosY2 = supPosY1 + motionY;
							supPosZ1 = supPosZ1 + (supMovingObjectPosition.getLocation().z - supPosZ1) * 0.999D;
							supPosZ2 = supPosZ1 + motionZ;
							//始点を登録
							supVec3d1 = new Vec3(supPosX1, supPosY1, supPosZ1);
							//終点を登録
							supVec3d2 = new Vec3(supPosX2, supPosY2, supPosZ2);
							//始点と終点からブロックとの衝突を取得
							supMovingObjectPosition = clipBlocks(supVec3d1, supVec3d2, ClipContext.Fluid.NONE);
						}
					}
					if(Math.abs(motionX) < 0.1D)
					{
						motionX = 0.0D;
					}
					if(Math.abs(motionZ) < 0.1D)
					{
						motionZ = 0.0D;
					}
					setKnifeCount(3);
					hit = false;
				}
				//速度が遅すぎて、かつ、衝突先のブロックが空気でなければ
				else if(!hitState.isAir())
				{
					//遅すぎる場合はほぼ地面についていると考える
					//角度を水平にする
					if(blockHit.getDirection().get3DDataValue() == 1)
					{
						setXRot(0F);
						motionX = blockHit.getLocation().x - getX();
						motionY = blockHit.getLocation().y - getY();
						motionZ = blockHit.getLocation().z - getZ();
						accelerationX = 0.0D;
						accelerationY = 0.0D;
						accelerationZ = 0.0D;
						//当たったブロックとナイフの距離
						double f2 = Mth.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
						//めり込んだ分だけ引き戻し
						setPos(getX() - (motionX / f2) * 0.05000000074505806D,
								getY() - (motionY / f2) * 0.05000000074505806D,
								getZ() - (motionZ / f2) * 0.05000000074505806D);
						//地中チェックをtrue
						inGround = true;
						//衝突チェックをtrue
						hit = true;
						//石衝突チェックをtrue
						rockhit = true;
						setKnifeCount(0);
						setKnifeZAngle(0.0F);
					}
					else
					{
						motionX = blockHit.getLocation().x - getX();
						motionY = blockHit.getLocation().y - getY();
						motionZ = blockHit.getLocation().z - getZ();
						accelerationX *= -0.01;
						accelerationY *= -0.01;
						accelerationZ *= -0.01;
						//当たったブロックとナイフの距離
						double f2 = Mth.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
						//めり込んだ分だけ引き戻し
						setPos(getX() - (motionX / f2) * 0.05000000074505806D,
								getY() - (motionY / f2) * 0.05000000074505806D,
								getZ() - (motionZ / f2) * 0.05000000074505806D);
						//地中チェックをtrue
						inGround = true;
						//衝突チェックをtrue
						hit = true;
						//石衝突チェックをtrue
						rockhit = true;
					}
				}
				else
				{
					hit = false;
				}
			}
			//衝突先のブロックの材質が、硬いものでなく、空気でもなければ
			else if(!hitState.isAir())
			{
				//その場で刺さって停止
				motionX = (float)(blockHit.getLocation().x - getX());
				motionY = (float)(blockHit.getLocation().y - getY());
				motionZ = (float)(blockHit.getLocation().z - getZ());
				accelerationX *= -0.01;
				accelerationY *= -0.01;
				accelerationZ *= -0.01;
				float f2 = Mth.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));//当たったブロックとナイフの距離
				//めり込んだ分だけ引き戻し
				setPos(getX() - (motionX / (double)f2) * 0.05000000074505806D,
						getY() - (motionY / (double)f2) * 0.05000000074505806D,
						getZ() - (motionZ / (double)f2) * 0.05000000074505806D);
				inGround = true;
				setKnifeCount(3);
				hit = true;
			}
		}

		if(delay < 0)
		{
			return;
		}

		if(!level().isClientSide && getKnifeCount() > 2 && delay >= 0)
		{
			setPos(getX() + motionX, getY() + motionY, getZ() + motionZ);
		}

		//緑のナイフの白ナイフ出現処理
		if(getKnifeColor() == 2 && Math.abs(getEntitySpeed()) > 0.08F && tickCount < 60 && tickCount > 2 && tickCount % 2 == 1)
		{
			Vec3 angle = THShotLib.angle(motionX, motionY, motionZ);
			angle = THShotLib.getVectorNomalize(angle);

			if(!level().isClientSide)
			{
				EntitySilverKnife knife;
				//TODO K3-review: 暫定登録は thkaguyamod.init.ModEntities.SILVER_KNIFE に置き換える。
				if(this.shootingEntity == null || this.playerEntity == null)
				{
					knife = new EntitySilverKnife(ModEntities.SILVER_KNIFE.get(), level(), THShotLib.pos_Entity(this), angle, getEntitySpeed(), 3);
				}
				else
				{
					knife = new EntitySilverKnife(ModEntities.SILVER_KNIFE.get(), level(), this.shootingEntity, THShotLib.pos_Entity(this), angle, getEntitySpeed(), 3);
					knife.setKnifeCount(0);
				}
				level().addFreshEntity(knife);

				knife.accelerationX = this.accelerationX;
				knife.accelerationY = this.accelerationY;
				knife.accelerationZ = this.accelerationZ;
			}
		}

		float f = Mth.sqrt((float)(motionX * motionX + motionZ * motionZ));
		float rotationYaw = (float)((Math.atan2(motionX, -motionZ) * 180D) / 3.1415927410125732D);
		float rotationPitch = getXRot();
		if(!level().isClientSide)
		{
			for(rotationPitch = (float)((Math.atan2(motionY, f) * 180D) / 3.1415927410125732D); rotationPitch - this.xRotO < -180F; this.xRotO -= 360F) {}
		}
		for(; rotationPitch - this.xRotO >= 180F; this.xRotO += 360F) {}
		for(; rotationYaw - this.yRotO < -180F; this.yRotO -= 360F) {}
		for(; rotationYaw - this.yRotO >= 180F; this.yRotO += 360F) {}
		rotationPitch = this.xRotO + (rotationPitch - this.xRotO) * 1.0F;

		if(rockhit)
		{
			rotationPitch = 0F;
		}

		rotationYaw = this.yRotO + (rotationYaw - this.yRotO) * 1.0F;
		float slowDownRate = 0.95F;//減速率
		if(isInWater())//水に入っている場合
		{
			slowDownRate = 0.8F;//さらに減速率を上げる
		}

		if(delay >= 0/*getKnifeCount() >= 0*/)
		{
			motionX += accelerationX;
			motionY += accelerationY;
			motionZ += accelerationZ;

			//重力処理
			motionY -= 0.008 * getKnifeCount();

			motionX *= slowDownRate;
			motionY *= slowDownRate;
			motionZ *= slowDownRate;
		}

		setPos(getX(), getY(), getZ());
		setRot(rotationYaw, rotationPitch);

		if(tickCount > lastTime)
		{
			lastTime = tickCount;
			lastShotMotionX = motionX;
			lastShotMotionY = motionY;
			lastShotMotionZ = motionZ;
		}
	}

	/**
	 * 始点と終点からブロックとの当たりを取得する
	 * 1.7.10 の worldObj.func_147447_a(start, end, false, true, false) に相当。
	 * @return 当たっていなければ null
	 */
	protected BlockHitResult clipBlocks(Vec3 startPos, Vec3 endPos, ClipContext.Fluid fluidMode)
	{
		BlockHitResult result = level().clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, fluidMode, this));
		return result.getType() == HitResult.Type.MISS ? null : result;
	}

	//衝突したときの処理
	protected void onImpact(Entity hitEntity)
	{
		//当たったEntityがnullでなければ
		if(!level().isClientSide && hitEntity != null)
		{
			//当たったEntityがナイフを投げたEntityでなければ
			if(hitEntity != shootingEntity)
			{
				float damage = 4.0F;
				//当たったEntityがLivingEntityに属しているなら
				if(hitEntity instanceof LivingEntity entityLivingBase)
				{
					//当たったEntityLivingBaseが無敵時間なら
					if(entityLivingBase.hurtTime > 0)
					{
						boolean isLive = entityLivingBase.getHealth() > 0.0F;
						//TODO K3-review: 1.7.10 は heal(-damage) で無敵時間を無視したダメージを与え、
						//0以下なら onDeath() を直接呼んでいた（ダメージイベント/防具を完全に無視する乱暴な処理）。
						//1.21.1 では heal(負値) は無効化されているため setHealth() で直接減算し、
						//死亡は die() を直接呼ぶ。意図的な「無敵貫通」挙動は保持しているが、
						//NeoForge の LivingDamageEvent 等は通らない。
						entityLivingBase.setHealth(entityLivingBase.getHealth() - damage);
						if(entityLivingBase.getHealth() <= 0.0F && isLive)
						{
							entityLivingBase.die(level().damageSources().mobAttack(shootingEntity));
						}
						//衝突チェックをtrueにする
						hit = true;
					}
					//当たったEntityLivingBaseが無敵時間でないなら
					else
					{
						//ハート2.0の通常ダメージ
						hitEntity.hurt(level().damageSources().mobAttack(shootingEntity), damage);
					}
				}
			}
		}
	}

	/**
	 * 保存するデータの書き込み
	 */
	@Override
	public void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		nbtTagCompound.putShort("xTile", (short)xTile);
		nbtTagCompound.putShort("yTile", (short)yTile);
		nbtTagCompound.putShort("zTile", (short)zTile);
		nbtTagCompound.putByte("inTile", (byte)inTile);
		nbtTagCompound.putByte("inData", (byte)inData);

		nbtTagCompound.putByte("inGround", (byte)(inGround ? 1 : 0));

		nbtTagCompound.putShort("color", getKnifeColor());
		nbtTagCompound.putShort("zAngle", (short)(getKnifeZAngle() * 10000.0F));
	}

	/**
	 * 保存したデータの読み込み
	 */
	@Override
	public void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		xTile = nbtTagCompound.getShort("xTile");
		yTile = nbtTagCompound.getShort("yTile");
		zTile = nbtTagCompound.getShort("zTile");
		inTile = nbtTagCompound.getByte("inTile") & 0xff;
		inData = nbtTagCompound.getByte("inData") & 0xff;
		inGround = nbtTagCompound.getByte("inGround") == 1;

		setKnifeColor(nbtTagCompound.getByte("color"));
		setKnifeZAngle((float)nbtTagCompound.getShort("zAngle") / 10000.0F);
	}

	/**
	 * 刺さっているものなら回収可能
	 * @param player : 落ちているナイフに触れたプレイヤー
	 */
	@Override
	public void playerTouch(Player player)
	{
		if(!level().isClientSide && inGround && player.getInventory().add(new ItemStack(knifeItemFor(getKnifeColor()))))
		{
			//1.7.10 の "random.pop"
			level().playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 4.0F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
			player.take(this, 1);
			discard();
		}
	}

	/**
	 * ナイフの色に対応するアイテムを返す
	 * 1.7.10 は silver_knife のメタデータ 0-3 で色を持っていたが、
	 * 1.21.1 では SILVER_KNIFE_BLUE/RED/GREEN/WHITE の 4 アイテムに分割されている。
	 */
	private static net.minecraft.world.item.Item knifeItemFor(byte color)
	{
		switch(color)
		{
			case KNIFE_COLOR_BLUE:
				return ModItems.SILVER_KNIFE_BLUE.get();
			case KNIFE_COLOR_RED:
				return ModItems.SILVER_KNIFE_RED.get();
			case KNIFE_COLOR_GREEN:
				return ModItems.SILVER_KNIFE_GREEN.get();
			case KNIFE_COLOR_WHITE:
			default:
				return ModItems.SILVER_KNIFE_WHITE.get();
		}
	}

	@Override
	public boolean canBeCollidedWith()
	{
		return true;
	}

	//物体の当たり判定　大きいと透明な判定が邪魔だよ
	@Override
	public float getPickRadius()
	{
		return 0.01F;
	}

	/**
	 * 攻撃されたときの処理
	 * TODO K3-review: 1.7.10 のシグネチャは attackEntityFrom(DamageSource, int) だった。
	 * 1.21.1 の実メソッドは hurt(DamageSource, float) なので int 版は互換のためだけに残す
	 * （フックされるのは hurt のみ）。
	 */
	@Deprecated
	public boolean attackEntityFrom(net.minecraft.world.damagesource.DamageSource damagesource, int i)
	{
		return this.hurt(damagesource, i);
	}

	@Override
	public boolean hurt(net.minecraft.world.damagesource.DamageSource damagesource, float i)
	{
		markHurt();
		if(damagesource.getEntity() != null)
		{
			Vec3 vec3d = damagesource.getEntity().getLookAngle();
			if(vec3d != null)
			{
				motionX = vec3d.x;
				motionY = vec3d.y;
				motionZ = vec3d.z;
				accelerationX = motionX * 0.10000000000000001D;
				accelerationY = motionY * 0.10000000000000001D;
				accelerationZ = motionZ * 0.10000000000000001D;
			}
			if(damagesource.getEntity() instanceof LivingEntity living)
			{
				shootingEntity = living;
			}
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * ナイフのカウントを設定
	 * @param count ナイフのカウント
	 */
	public void setKnifeCount(int count)
	{
		this.entityData.set(DATA_KNIFE_COUNT, count);
	}

	/**
	 * ナイフのカウントを取得
	 * @return ナイフのカウントを返す
	 */
	public int getKnifeCount()
	{
		return this.entityData.get(DATA_KNIFE_COUNT);
	}

	/**
	 * ナイフの色を設定
	 * @param color ナイフの色
	 */
	public void setKnifeColor(int color)
	{
		this.entityData.set(DATA_KNIFE_COLOR, (byte)color);
	}

	/**
	 * ナイフの色を取得
	 * @return ナイフの色
	 */
	public byte getKnifeColor()
	{
		return this.entityData.get(DATA_KNIFE_COLOR);
	}

	//ナイフの奥行き方向への角度設定する
	public void setKnifeZAngle(float angle)
	{
		this.entityData.set(DATA_KNIFE_Z_ANGLE, (int)(angle * 10000.0F));
	}

	//ナイフの奥行き方向への角度を返す
	public float getKnifeZAngle()
	{
		return (float)this.entityData.get(DATA_KNIFE_Z_ANGLE) / 10000.0F;
	}
}