package thkaguyamod.entity.item;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import thkaguyamod.danmaku.THKaguyaLib;
import thkaguyamod.entity.living.EntityDanmakuMob;
import thkaguyamod.init.ModItems;

/**
 * 呪いのデコイ人形のEntity
 *
 * <p>1.7.10 {@code thKaguyaMod.entity.item.EntityCursedDecoyDoll} の移植。
 * 継承元 {@link EntityDanmakuMob} は並行移植中のため、公開 API は旧版と同名と仮定している
 * （setDanmakuMobName / setSpecies / setDanmakuPattern / setMaxHP / setSpeed /
 * setAttackDistance / setDetectionDistance / setFlyingHeight / isFlyingMode /
 * isSpellCardMode / attackInterval / canFairyCall）。
 */
public class EntityCursedDecoyDoll extends EntityDanmakuMob
{
	/** 使用者 */
	public LivingEntity userEntity;

	public EntityCursedDecoyDoll(net.minecraft.world.entity.EntityType<? extends EntityCursedDecoyDoll> type, Level level)
	{
		super(type, level);
		//サイズは getDimensions() で指定する（1.7.10 の setSize(1.0F, 1.8F) に相当）
		this.xpReward = 1;//経験値の量

		this.setDanmakuMobName("");
		this.setSpecies(SPECIES_SHIKIGAMI_DOLL);

		this.setDanmakuPattern(NOT_ATTACK);
		this.setMaxHP(40.0F);
		this.setHealth(40.0F);
		this.setSpeed(0.0F);
		this.setAttackDistance(0.0D);
		this.setDetectionDistance(0.0D);
		this.setFlyingHeight(0);
		this.isFlyingMode = false;

		this.isSpellCardMode = false;
		this.attackInterval = 0;
	}

	/** 当たり判定の大きさ（1.7.10 の setSize(1.0F, 1.8F) に相当）
	 *  TODO K3-review: LivingEntity.getDimensions(Pose) は 1.21.1 で final のため、
	 *  1.7.10 の setSize 相当は getDefaultDimensions をオーバーライドして指定する。 */
	@Override
	protected net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose)
	{
		return net.minecraft.world.entity.EntityDimensions.fixed(1.0F, 1.8F);
	}

	public EntityCursedDecoyDoll(net.minecraft.world.entity.EntityType<? extends EntityCursedDecoyDoll> type, Level level, LivingEntity living)
	{
		this(type, level);

		userEntity = living;//使用者をuserEntityに保存
		this.setPos(living.getX(), living.getY(), living.getZ());
	}

	//周りの妖精を呼び出すことができるか
	@Override
	protected boolean canFairyCall()
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
		/*if(!level().isClientSide && userEntity == null)
		{
			//アイテム化させる
			if(!level().isClientSide)
			{
				discard();
			}
			return;
		}*/

		//180ticksを超えているなら
		if(tickCount > 180)
		{
			//アイテム化させる
			//THKaguyaLib.itemEffectFinish(this, userEntity, ModItems.CURSED_DECOY_DOLL.get());
			if(!level().isClientSide)
			{
				discard();
			}
		}

		int playerCount = 0;//停止空間内のプレイヤーの数を数える　誰もいなければ消滅させる

		//***********停止空間（時計じゃなくてもっと大きな見えない壁）とEntityの当たり判定を取る****************//
		boolean dubbleWatch = false;
		double effectiveBoundary = 40.0D;//停止空間の有効範囲
		List<Entity> list = level().getEntities(this, getBoundingBox().inflate(effectiveBoundary, effectiveBoundary, effectiveBoundary));

		if(list != null && list.size() > 0)//取得したリストにEntityがいるなら
		{
			for(int j1 = 0; j1 < list.size(); j1++)//取得したEntityリストの最初から最後まで見る
			{
				Entity entity = list.get(j1);
				if(entity == null)//Entityがいるなら
				{
					continue;
				}
				if(entity instanceof Player)//プレイヤーなら
				{
					playerCount++;//範囲内のプレイヤー数カウントを増加
				}
				//movingObjectPositionがあり、それが使用者でないなら
				if(entity != userEntity && entity.getVehicle() != userEntity
						&& !(entity instanceof ItemFrame)
						&& !(entity instanceof Painting))
				{
					//時計が同時に２つ出現しているときの処理。全ての時計を無効化しアイテムに戻す。
					if(entity instanceof EntityCursedDecoyDoll decoy)
					{
						//アイテム化させる
						THKaguyaLib.itemEffectFinish(decoy, decoy.userEntity, ModItems.CURSED_DECOY_DOLL.get());
						dubbleWatch = true;
					}
					inDecoyEffectedArea(entity);//呪いのデコイ人形の有効範囲に入っていた場合の処理
				}
			}
		}

		if(playerCount == 0)//範囲内にプレイヤーが一人もいなかったなら
		{
			if(!level().isClientSide)
			{
				THKaguyaLib.itemEffectFinish(this, userEntity, ModItems.CURSED_DECOY_DOLL.get());//終了処理に移る
			}
		}

		if(dubbleWatch)
		{
			//アイテム化させる
			THKaguyaLib.itemEffectFinish(this, userEntity, ModItems.CURSED_DECOY_DOLL.get());
		}

		//************************************************************************************//

		this.setYRot(this.yRotO);
		this.setXRot(this.xRotO);
	}

	/**
	 * 呪いのデコイ人形の有効範囲に入っていたときの処理
	 * @param entity 範囲内のEntity
	 */
	public void inDecoyEffectedArea(Entity entity)
	{
		//1.7.10 の EntityCreature は 1.21.1 の Mob に相当
		if(entity instanceof Mob effectedCreature)
		{
			if(effectedCreature.getTarget() != null)
			{
				effectedCreature.setTarget(this);
			}
		}
		if(entity instanceof LivingEntity effectedLiving)
		{
			//1.7.10 の setRevengeTarget は 1.21.1 の setLastHurtByMob に相当
			effectedLiving.setLastHurtByMob(this);
		}
	}

	//生きてるときに出す音
	@Override
	protected net.minecraft.sounds.SoundEvent getAmbientSound()
	{
		return null;
	}

	//攻撃を受けたときの音
	@Override
	protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source)
	{
		return null;
	}

	//倒れたときの音
	@Override
	protected net.minecraft.sounds.SoundEvent getDeathSound()
	{
		return null;
	}

	/**
	 * 保存するデータの書き込み
	 */
	@Override
	public void addAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		super.addAdditionalSaveData(nbtTagCompound);
	}

	/**
	 * 保存したデータの読み込み
	 */
	@Override
	public void readAdditionalSaveData(CompoundTag nbtTagCompound)
	{
		super.readAdditionalSaveData(nbtTagCompound);
	}
}