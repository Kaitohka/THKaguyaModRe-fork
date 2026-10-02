package thkaguyamod.entity.item;

import java.util.List;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THKaguyaLib;
import thkaguyamod.init.ModItems;
import thkaguyamod.spellcard.EntitySpellCard;

/**
 * 時間を停止させる空間（懐中時計）
 *
 * <p>1.7.10 {@code thKaguyaMod.entity.item.EntitySakuyaWatch} の移植。C級リスク項目だが、
 * 範囲内のEntityを1フレーム前の状態に巻き戻す（位置・回転・移動量・tickCount・fallDistance）+
 * プレイヤーの connection.teleport によるサーバー側位置の巻き戻し、という旧挙動をそのまま踏襲した。
 */
public class EntitySakuyaWatch extends Entity
{
	/** 使用者 */
	public LivingEntity userEntity;
	private int count;
	private int mode;
	private boolean isSpellCard;

	/** 時間停止 */
	public static final int TIME_STOP = 0;
	/** 特定のスペルカード内の時間停止 */
	public static final int TIME_STOP_IN_SPELLCARD = 1;
	/** 時間減速1/2 */
	public static final int TIME_HALF = 2;
	/** 時間制限付き時間停止 */
	public static final int TIME_STOP_WITH_LIMIT = 5;
	/** 時間制限付き時間減速1/2 */
	public static final int TIME_HALF_WITH_LIMIT = 6;

	public EntitySakuyaWatch(EntityType<? extends EntitySakuyaWatch> type, Level level)
	{
		super(type, level);
		noCulling = true;//中心が画面から外れても描画される（1.7.10 の ignoreFrustumCheck に相当）
//TODO K3-review: 1.7.10 の preventEntitySpawning / yOffset / shouldRenderInPass(-2) は
		//1.21.1 に対応するフィールドが無い（MISC カテゴリで自然スポーンせず、y オフセットと描画パスは
		//Renderer 側の責務になったため）。実害なしと判断して省略。
		mode = 2;
	}

	public EntitySakuyaWatch(EntityType<? extends EntitySakuyaWatch> type, Level level, LivingEntity living, int pmode)
	{
		this(type, level);

		userEntity = living;//使用者をuserEntityに保存
		THKaguyaLib.itemEffectFollowUser(this, userEntity, 1.2D, -30F);
		mode = pmode;

		isSpellCard = mode == TIME_STOP_IN_SPELLCARD;
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(1.0F, 1.0F) / yOffset = 0.0F に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(1.0F, 1.0F);
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {}

	/**
	 * 押すことができるか
	 */
	@Override
	public boolean isPushable()
	{
		return false;
	}

	/**
	 * 他のEntityと衝突するか 右クリックできるかもこれで判定
	 */
	@Override
	public boolean canBeCollidedWith()
	{
		return false;
	}

	/**
	 * 毎tick行う処理
	 */
	@Override
	public void tick()
	{
		super.tick();

		//使用者がいないなら
		if(!level().isClientSide && userEntity == null)
		{
			//アイテム化させる
			if(!isSpellCard)
			{
				THKaguyaLib.itemEffectFinish(this, userEntity, ModItems.SAKUYAS_POCKET_WATCH.get());
			}
			else
			{
				discard();
			}
			return;
		}

		//スペルカード内の時間停止で、60ticksを超えているなら
		if(mode == TIME_STOP_IN_SPELLCARD && tickCount > 60)
		{
			//アイテム化させる
			//THKaguyaLib.itemEffectFinish(this, userEntity, ModItems.SAKUYAS_POCKET_WATCH.get());
			if(!level().isClientSide)
			{
				discard();
			}
		}
		else if(mode == TIME_STOP_WITH_LIMIT && tickCount > 100)
		{
			//アイテム化させる
			THKaguyaLib.itemEffectFinish(this, userEntity, ModItems.SAKUYAS_POCKET_WATCH.get());
		}
		else if(mode == TIME_HALF_WITH_LIMIT && tickCount > 160)
		{
			//アイテム化させる
			THKaguyaLib.itemEffectFinish(this, userEntity, ModItems.SAKUYAS_POCKET_WATCH.get());
		}

		//使用者がいるなら
		if(!level().isClientSide && userEntity != null)
		{
			if(userEntity instanceof Player userEntity_p)
			{
				if(!userEntity_p.getAbilities().instabuild)//クリエイティブモードでないなら
				{
					if(mode == TIME_STOP)//時間停止モードなら
					{
						userEntity_p.causeFoodExhaustion(0.25F);//満腹度を大きく減らす
					}
					else if(mode == TIME_HALF)//時間減速モードなら
					{
						userEntity_p.causeFoodExhaustion(0.10F);//満腹度を少し減らす
					}
				}
			}
			if(tickCount > 10 && userEntity.isShiftKeyDown())//出現から時間が経っていて、使用者がスニークモードなら
			{
				if(!isSpellCard)
				{
					THKaguyaLib.itemEffectFinish(this, userEntity, ModItems.SAKUYAS_POCKET_WATCH.get());//終了処理に移る
				}
				else
				{
					discard();
				}
			}
			if(userEntity.hurtTime > 0)//使用者がダメージを受けたら終了
			{
				if(!isSpellCard)
				{
					THKaguyaLib.itemEffectFinish(this, userEntity, ModItems.SAKUYAS_POCKET_WATCH.get());//終了処理に移る
				}
				else
				{
					discard();
				}
			}
		}

		//懐中時計を使用者に追尾させる
		THKaguyaLib.itemEffectFollowUser(this, userEntity, 1.2D, -30F);

		if(mode == 3 && tickCount == 45)
		{
			mode = TIME_STOP;
		}
		if(mode == 4 && tickCount == 45)
		{
			mode = TIME_HALF;
		}

		int playerCount = 0;//停止空間内のプレイヤーの数を数える　誰もいなければ消滅させる

		//***********停止空間（時計じゃなくてもっと大きな見えない壁）とEntityの当たり判定を取る****************//
		boolean dubbleWatch = false;
		boolean pass = false;
		double effectiveBoundary = 40.0D;
		List<Entity> list = level().getEntities(this, getBoundingBox().inflate(effectiveBoundary, effectiveBoundary, effectiveBoundary));

		if(list != null && list.size() > 0)
		{
			for(int j1 = 0; j1 < list.size(); j1++)
			{
				Entity entity = list.get(j1);
				if(entity == null)
				{
					continue;
				}
				if(entity instanceof Player)
				{
					playerCount++;
				}

				if(!level().isClientSide && entity != userEntity && entity.getVehicle() != userEntity
						&& !(entity instanceof ItemFrame)
						&& !(entity instanceof Painting))
				{
					pass = false;
					//時計が同時に２つ出現しているときの処理。全ての時計を無効化しアイテムに戻す。
					if(entity instanceof EntitySakuyaWatch watch)
					{
						//アイテム化させる
						THKaguyaLib.itemEffectFinish(watch, watch.userEntity, ModItems.SAKUYAS_POCKET_WATCH.get());
						dubbleWatch = true;
					}
					if(entity instanceof EntitySpellCard spellCard)
					{
						if(spellCard.canMoveInTimeStop && spellCard.user.equals(this.userEntity))
						{
							pass = true;
							spellCard.specialProcessInTimeStop();
						}
					}
					if(!pass)
					{
						inPrivateSquare(entity);//時間操作空間の処理
					}
				}
			}
		}

		if(playerCount == 0)//範囲内にプレイヤーが一人もいなかったなら
		{
			if(!level().isClientSide)
			{
				THKaguyaLib.itemEffectFinish(this, userEntity, ModItems.SAKUYAS_POCKET_WATCH.get());//終了処理に移る
			}
		}

		if(dubbleWatch)
		{
			//アイテム化させる
			THKaguyaLib.itemEffectFinish(this, userEntity, ModItems.SAKUYAS_POCKET_WATCH.get());
		}

		//************************************************************************************//

		count++;

		if(mode > 2)
		{
			if(count % 20 == 19)
			{
				playTimeStopSound();
			}
		}
		else if(count % 20 == 19)
		{
			playTimeStopSound();
		}
	}

	private void playTimeStopSound()
	{
		//1.7.10 の "random.orb" / "random.click"
		level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.5F, 6.0F);
		level().playSound(null, blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 0.5F, 4.0F);
	}

	/**
	 * 保存するデータの書き込み
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		nbtTagCompound.putShort("count", (short)count);
		nbtTagCompound.putBoolean("isSpellCard", isSpellCard);
	}

	/**
	 * 保存したデータの読み込み
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		count = nbtTagCompound.getShort("count");
		isSpellCard = nbtTagCompound.getBoolean("isSpellCard");
	}

	//時間操作空間に入っていた時の処理
	protected void inPrivateSquare(Entity hitEntity)
	{
		//Entityが誕生してから２フレーム経過したもののみ判定
		if(hitEntity != null && hitEntity.tickCount >= 2)
		{
			//====時間停止空間処理====//

			//Entityを１フレーム前の状態に、できる限りもっていく処理
			//1.7.10 の setPosition(prevPosX, prevPosY, prevPosZ) に相当
			hitEntity.setPos(hitEntity.xo, hitEntity.yo, hitEntity.zo);

			hitEntity.setYRot(hitEntity.yRotO);
			hitEntity.setXRot(hitEntity.xRotO);
			//motionX = 0.0D; motionZ = 0.0D; 空中なら motionY = 0.0D（1.7.10 の分岐をそのまま踏襲）
			double motionY = hitEntity.getDeltaMovement().y;
			if(!hitEntity.onGround())
			{
				motionY = 0.0D;
			}
			hitEntity.setDeltaMovement(0.0D, motionY, 0.0D);

			hitEntity.setAirSupply(0);

			hitEntity.tickCount--;//誕生してからのカウントを増やさない

			hitEntity.fallDistance -= 0.076865F;//落下距離を変動しないように

			if(hitEntity instanceof LivingEntity living)//範囲内のEntityが生物なら
			{
				living.setYHeadRot(living.yHeadRotO);
				//TODO K3-review: 1.7.10 の EntityLivingBase.attackTime++ は int フィールドだった。
				//1.21.1 では振りアニメーション用の float attackAnim しか公開されておらず、
				//「攻撃モーションを巻き戻す」旧意図は再現できないため、attackAnim を1増やす近似に留めた。
				living.attackAnim += 1.0F;

				if(living instanceof Creeper entityCreeper)//EntityCreeperに属しているなら
				{
					entityCreeper.setSwellDir(-1);//爆発のカウントを戻す
					//TODO K3-review: 1.7.10 の timeSinceIgnited に相当するフィールドは 1.21.1 では
					//Creeper.SwellGoal 内に隠蔽されており外部から戻せない。
				}
				else if(living instanceof Ghast)
				{
					//TODO K3-review: 1.7.10 の EntityGhast.attackCounter は 1.21.1 では
					//Ghast.GhastShootFireballGoal.chargeTime に移動しており、外部から減算できない。
					//ガストの溜めカウントを戻す旧処理は未移植（過渡的に溜めが進む）。
				}

				if(living instanceof TamableAnimal)
				{
					//座っている動物は重力がなくなると立つみたい　わからない程度に重力をかける処理
					Vec3 motion = living.getDeltaMovement();
					living.setDeltaMovement(motion.x, motion.y - 0.000001D, motion.z);
				}

				//プレイヤーの動きを止める
				if(living instanceof ServerPlayer player)
				{
					player.connection.teleport(player.xo, player.yo, player.zo, player.getYRot(), player.getXRot(), Set.of());
				}
			}
		}
		//====時間減速空間の場合====//
		else if(mode == TIME_HALF || mode == TIME_HALF_WITH_LIMIT)//時間を1/2の進みにする
		{
			slowDownSpeed(hitEntity, 1, 2);
		}
		else if(mode == 3)
		{
			slowDownSpeed(hitEntity, 5 - (tickCount / 9), 5);
		}
		else if(mode == 4)
		{
			slowDownSpeed(hitEntity, 10 - (tickCount / 9), 10);
		}
	}

	/*時間を操る
	 * hitEntity : 操る対象
	 * timeRate  : 進む時間の倍率。減速しか扱えず、整数分の１の速度にしかできない。ex 1/2, 1/3, 1/4, 1/5...
	 */
	private void slowDownSpeed(Entity hitEntity, int timeRateNum, int timeRateDen)
	{
		float timeRate = (float)timeRateNum / (float)timeRateDen;

		if(timeRate < 1.0)
		{
			Vec3 motion = hitEntity.getDeltaMovement();
			hitEntity.setDeltaMovement(
					motion.x - (hitEntity.getX() - hitEntity.xo) * timeRate,
					motion.y - (hitEntity.getY() - hitEntity.yo) * timeRate,
					motion.z - (hitEntity.getZ() - hitEntity.zo) * timeRate);
			hitEntity.fallDistance -= 0.076865F * timeRate;//落下距離を半減（空中待機時間で変動してしまう）
			if(count % timeRateDen < timeRateNum)//偶数フレームのみ処理
			{
				hitEntity.tickCount--;//誕生してからのカウントを増やさない
				if(hitEntity instanceof LivingEntity living)
				{
					living.setYHeadRot(living.yHeadRotO);

					if(living instanceof Creeper creeper)
					{
						creeper.setSwellDir(-1);
					}
					else if(living instanceof Ghast)
					{
						//TODO K3-review: inPrivateSquare と同じ理由で未移植（外部から参照不可）。
					}
				}
			}
		}
		else
		{
			int timeRate2 = (int)timeRate;
			for(int i = 2; i <= timeRate2; i++)
			{
				hitEntity.tickCount++;
				hitEntity.tick();
			}
		}
	}
}