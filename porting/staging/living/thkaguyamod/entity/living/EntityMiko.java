package thkaguyamod.entity.living;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModItems;
import thkaguyamod.spellcard.EntitySpellCard;

import static thkaguyamod.danmaku.DanmakuConstants.*;

/** 豊聡耳 神子 */
public class EntityMiko extends EntityDanmakuMob
{

	public EntityMiko(EntityType<? extends EntityMiko> type, Level level)
	{
		super(type, level);

		//MOBの当たり判定の大きさ（1.7.10 の setSize(1.0F, 1.8F) に相当。寸法は登録側で指定）

		this.xpReward = 250;//経験値の量

		this.setDanmakuMobName("Toyosatomimi no Miko");
		this.setSpecies(this.SPECIES_HUMAN_SAINT);

		this.setDanmakuPattern(NOT_ATTACK);
		this.setMaxHP(74.0F);
		this.setHealth(74.0F);
		this.setSpeed(0.4F);
		this.setAttackDistance(14.0D);
		this.setDetectionDistance(0.0D);
		this.setFlyingHeight(0);
		this.isFlyingMode = false;

		this.isSpellCardMode = false;
		this.attackInterval = 0;
	}

	/** 属性（1.7.10 の applyEntityAttributes に相当） */
	public static AttributeSupplier.Builder createAttributes()
	{
		return EntityDanmakuMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 2.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.25D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
				.add(Attributes.FOLLOW_RANGE, 30.0D);
	}

	/**
	 * 使用しているスペルカードNoを返す
	 * @return 使用しているスペルカードNo
	 */
	@Override
	public int getUsingSpellCardNo()
	{
		switch(this.getDanmakuPattern())
		{
			case SPELLCARD_ATTACK01:
				return EntitySpellCard.SC_REIMU_MusouFuuin;
			default:
				return -1;
		}
	}

	//死んでいるときに呼ばれる（1.7.10 の onDeathUpdate）
	@Override
	protected void tickDeath()
	{
		switch(getDanmakuPattern())
		{
			case NORMAL_ATTACK01:
				setFlyingHeight(2);
				moveDanmakuAttack(SPELLCARD_ATTACK01, 40, 60.0F, 160);
				break;
			case SPELLCARD_ATTACK01:
				moveDanmakuAttack(ATTACK_END, 90, 40.0F, 160);
				break;
			default:
				if(deathTime % 6 == 0)
				{
					THShotLib.explosionEffect(level(), getX(), getY(), getZ(), 1.0F + deathTime * 0.1F);
				}
				super.tickDeath();
				break;
		}
	}

	/** 常時呼ばれる */
	@Override
	public void tick()
	{
		super.tick();
	}

	/**
	 * 弾幕のパターンを記述
	 * @param level : EASY～LUNATICの難易度
	 */
	@Override
	public void danmakuPattern(int level)
	{
		switch(getDanmakuPattern())
		{
			case NORMAL_ATTACK01:
				//danmaku01(look, level);
				break;
			case SPELLCARD_ATTACK01:
				if(attackCounter == 1)
				{
					this.useSpellCard(EntitySpellCard.SC_REIMU_MusouFuuin);
				}
				else if(attackCounter >= 140)
				{
					attackCounter = 0;
				}
				break;
			default:
				break;
		}
	}

	//周りの妖精を呼び出すことができるか
	@Override
	protected boolean canFairyCall()
	{
		return false;
	}

	/**
	 * 無敵かどうかを返す（1.7.10 の isEntityInvulnerable）
	 */
	@Override
	public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource damageSource)
	{
		if(getDanmakuPattern() == NOT_ATTACK)
		{
			return true;
		}
		return super.isInvulnerableTo(damageSource);
	}

	//倒れたときに落とすアイテム
	@Override
	protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
			net.minecraft.world.damagesource.DamageSource damageSource, boolean hasBeenAttackedByPlayer)
	{
		super.dropCustomDeathLoot(level, damageSource, hasBeenAttackedByPlayer);

		if(this.isSpellCardAttack())
		{
			int j = 12;
			int k;
			ItemEntity item;
			Vec3 vec3;

			for (k = 0; k < j; ++k)
			{
				item = this.spawnAtLocation(new ItemStack(ModItems.POWER_UP_ITEM_SMALL.get()));
				if(item == null)
				{
					continue;
				}
				item.setYRot(k * 30F);
				item.setXRot(-60F);
				vec3 = THShotLib.getVecFromAngle(item.getYRot(), item.getXRot(), 0.5F);
				item.setPos(item.getX() + vec3.x * 2.0D, item.getY(), item.getZ() + vec3.z * 2.0D);

				item.setDeltaMovement(vec3);
			}

			for (k = 0; k < j; ++k)
			{
				item = this.spawnAtLocation(new ItemStack(ModItems.POINT_ITEM.get()));
				if(item == null)
				{
					continue;
				}
				item.setYRot(k * 30F + 15F);
				item.setXRot(-60F);
				vec3 = THShotLib.getVecFromAngle(item.getYRot(), item.getXRot(), 0.3F);
				item.setPos(item.getX() + vec3.x * 2.0D, item.getY(), item.getZ() + vec3.z * 2.0D);

				item.setDeltaMovement(vec3);
			}
		}
	}

	@Override
	public int getMaxSpawnClusterSize()
	{
		return 1;
	}

	//自然スポーンするときに呼ばれる。trueならスポーンする
	@Override
	public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnType)
	{
		return false;
	}

	/**
	 * 右クリックをされたときの処理
	 *
	 * <p>1.7.10 は {@code interact(EntityPlayer)} で弾幕難易度を EASY→NORMAL→HARD→LUNATIC→EASY と
	 * 循環させ、チャットに通知していた。1.21.1 では {@code mobInteract} が入口。
	 */
	@Override
	protected net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand)
	{
		if(tickCount % 2 == 0)
		{
			return net.minecraft.world.InteractionResult.PASS;
		}

		String str = "";

		if(ModConfig.danmakuLevel == EASY)
		{
			ModConfig.danmakuLevel = NORMAL;
			str = "NORMAL";
		}
		else if(ModConfig.danmakuLevel == NORMAL)
		{
			ModConfig.danmakuLevel = HARD;
			str = "HARD";
		}
		else if(ModConfig.danmakuLevel == HARD)
		{
			ModConfig.danmakuLevel = LUNATIC;
			str = "LUNATIC";
		}
		else
		{
			ModConfig.danmakuLevel = EASY;
			str = "EASY";
		}
		//TODO K3-review: 1.7.10 は ModConfig（旧 THKaguyaConfig）の静的フィールドを直接書き換えていた。
		//1.21.1 の ModConfig は ModConfigSpec 由来の値で、次回 sync() 時に上書きされる。
		//永続化するには ModConfigSpec 側の書き込み API が必要（未確定）。
		player.sendSystemMessage(Component.literal(str));
		return net.minecraft.world.InteractionResult.SUCCESS;
	}
}