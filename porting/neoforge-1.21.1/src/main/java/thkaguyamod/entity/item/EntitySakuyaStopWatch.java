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
import thkaguyamod.spellcard.EntitySpellCard;

/**
 * 僅かな間、時間を停止させる空間（ストップウォッチ）
 *
 * <p>1.7.10 {@code thKaguyaMod.entity.item.EntitySakuyaStopWatch} の移植。
 * 40tickで消滅する短時間の時間停止。範囲内Entityの巻き戻し処理は {@link EntitySakuyaWatch} と同じ。
 */
public class EntitySakuyaStopWatch extends Entity
{
	/** 使用者 */
	public LivingEntity user;
	private int count;

	public EntitySakuyaStopWatch(EntityType<? extends EntitySakuyaStopWatch> type, Level level)
	{
		super(type, level);
		noCulling = true;//中心が画面から外れても描画される（1.7.10 の ignoreFrustumCheck に相当）
		//TODO K3-review: preventEntitySpawning / yOffset / shouldRenderInPass(-2) は 1.21.1 に対応が無い。
	}

	public EntitySakuyaStopWatch(EntityType<? extends EntitySakuyaStopWatch> type, Level level, LivingEntity userLiving)
	{
		this(type, level);

		user = userLiving;
		THKaguyaLib.itemEffectFollowUser(this, user, 1.2D, -30F);
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(0.2F, 0.2F) / yOffset = 0.0F に相当） */
	@Override
	public EntityDimensions getDimensions(Pose pose)
	{
		return EntityDimensions.fixed(0.2F, 0.2F);
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

		//40ticksを超えている場合
		if(tickCount > 40)
		{
			//消滅させる
			if(!level().isClientSide)
			{
				discard();
			}
			return;
		}

		//使用者がいない場合
		if(user == null)
		{
			//消滅させる
			if(!level().isClientSide)
			{
				discard();
			}
			return;
		}
		//使用者がいる場合
		else
		{
			//使用者がダメージを受けた場合
			if(user.hurtTime > 0)
			{
				//消滅させる
				if(!level().isClientSide)
				{
					discard();
				}
				return;
			}
		}

		//ストップウォッチを使用者に追尾させる
		THKaguyaLib.itemEffectFollowUser(this, user, 1.2D, -30F);

		//***********停止空間（時計じゃなくてもっと大きな見えない壁）とEntityの当たり判定を取る****************//
		int playerCount = 0;//停止空間内のプレイヤーの数を数える　誰もいなければ消滅させる
		boolean dubbleWatch = false;
		boolean pass = false;
		double effectiveBoundary = 40.0D;//停止空間の有効範囲
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

				//Entityがプレイヤーの場合
				if(entity instanceof Player)
				{
					playerCount++;
				}

				if(entity != user && entity.getVehicle() != user
						&& !(entity instanceof ItemFrame)
						&& !(entity instanceof Painting))
				{
					pass = false;
					//時計が同時に２つ出現しているときの処理。全ての時計を無効化しアイテムに戻す。
					if(entity instanceof EntitySakuyaWatch watch)
					{
						//アイテム化させる
						THKaguyaLib.itemEffectFinish(watch, watch.userEntity, thkaguyamod.init.ModItems.SAKUYAS_POCKET_WATCH.get());
						dubbleWatch = true;
					}
					if(entity instanceof EntitySakuyaStopWatch watch)
					{
						//アイテム化させる
						//THKaguyaLib.itemEffectFinish(watch, watch.user, ModItems.SAKUYAS_POCKET_WATCH.get());
						if(!watch.level().isClientSide)
						{
							watch.discard();
						}
						dubbleWatch = true;
					}
					if(entity instanceof EntitySpellCard spellCard)
					{
						// P36（audit-common）: spellCard.user は同期されない public フィールドで、
						// クライアント側では恒に null。旧コードはそれを receiver にしていたため、
						// 将来 canMoveInTimeStop を同期した瞬間にクライアントで NPE になる。
						// 等価比較なので receiver を this.user 側へ寄せて null 安全にする。
						if(spellCard.canMoveInTimeStop && this.user != null
								&& this.user.equals(spellCard.user))
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

		//範囲内にプレイヤーが一人もいない場合
		if(playerCount == 0)
		{
			//消滅させる
			if(!level().isClientSide)
			{
				discard();
			}
			return;
		}

		//時計が２つ以上出現している場合
		if(dubbleWatch)
		{
			//消滅させる
			if(!level().isClientSide)
			{
				discard();
			}
			return;
		}

		//************************************************************************************//

		count++;

		if(count % 20 == 19)//20フレーム周期で音を出す
		{
			//1.7.10 の "random.orb" / "random.click"
			level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.5F, 6.0F);
			level().playSound(null, blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 0.5F, 4.0F);
		}
	}

	/**
	 * 保存するデータの書き込み
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		nbtTagCompound.putShort("count", (short)count);
	}

	/**
	 * 保存したデータの読み込み
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		count = nbtTagCompound.getShort("count");
	}

	//時間操作空間に入っていた時の処理
	protected void inPrivateSquare(Entity hitEntity)
	{
		//Entityが誕生してから２フレーム経過したもののみ判定
		if(hitEntity.tickCount >= 2)
		{
			//====時間停止空間処理====//

			//Entityを１フレーム前の状態に、できる限りもっていく処理
			hitEntity.setPos(hitEntity.xo, hitEntity.yo, hitEntity.zo);

			hitEntity.setYRot(hitEntity.yRotO);
			hitEntity.setXRot(hitEntity.xRotO);
			double motionY = hitEntity.getDeltaMovement().y;
			if(!hitEntity.onGround())
			{
				motionY = 0.0D;
			}
			hitEntity.setDeltaMovement(0.0D, motionY, 0.0D);

			hitEntity.setAirSupply(0);

			hitEntity.tickCount--;//誕生してからのカウントを増やさない

			hitEntity.fallDistance -= 0.076865F;//落下距離を変動しないように

			//範囲内のEntityが生物の場合
			if(hitEntity instanceof LivingEntity living)
			{
				living.setYHeadRot(living.yHeadRotO);
				//TODO K3-review: 1.7.10 の EntityLivingBase.attackTime++（int）に相当するフィールドは
				//1.21.1 に無い（attackAnim は float の描画用）。attackAnim を1増やす近似に留めた。
				living.attackAnim += 1.0F;

				//クリーパーに属している場合
				if(living instanceof Creeper entityCreeper)
				{
					entityCreeper.setSwellDir(-1);//爆発のカウントを戻す
					//TODO K3-review: timeSinceIgnited は 1.21.1 では SwellGoal 内に隠蔽され外部から戻せない。
				}
				//ガストに属している場合
				else if(living instanceof Ghast)
				{
					//TODO K3-review: 1.7.10 の EntityGhast.attackCounter は 1.21.1 では
					//GhastShootFireballGoal.chargeTime に移動しており外部から減算できない。未移植。
				}

				//テイム可能な場合
				if(living instanceof TamableAnimal)
				{
					//座っている動物は重力がなくなると立つみたい　わからない程度に重力をかける処理
					Vec3 motion = living.getDeltaMovement();
					living.setDeltaMovement(motion.x, motion.y - 0.000001D, motion.z);
				}

				//プレイヤーの場合
				if(living instanceof ServerPlayer player)
				{
					player.connection.teleport(player.xo, player.yo, player.zo, player.getYRot(), player.getXRot(), Set.of());
				}
			}
		}
	}
}