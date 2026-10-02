package thkaguyamod.danmaku;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.THKaguyaMod;
import thkaguyamod.config.ModConfig;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModEntities;
import thkaguyamod.init.ModItems;
import thkaguyamod.init.ModSounds;
import thkaguyamod.item.ItemTHSpellCard;
import thkaguyamod.registry.SpellCardRegistry;
import thkaguyamod.spellcard.EntitySpellCard;
import thkaguyamod.spellcard.THSpellCard;

/**
 * 五つの難題MOD+用、汎用関数群
 *
 * <p>1.7.10 の {@code thKaguyaMod.THKaguyaLib} のうち、符卡システム基盤に関わる部分の移植。
 * リフレクション ({@code Class.newInstance()}) は {@link Supplier} ファクトリに置換済み。
 * 音声は 1.21.1 の SoundEvent 登録に、チャットは Component に置換した。
 */
public class THKaguyaLib
{

	/** 早苗のスペルカードの旧 meta 範囲（{@link ItemTHSpellCard} の SANAE_CARD_MIN/MAX と同値）。 */
	private static final int SANAE_CARD_MIN = 14;
	private static final int SANAE_CARD_MAX = 18;

	/**
	 *  持ち主の指定のポジションにぬるぬるついていく処理
	 * @param itemEntity    : 追尾するアイテムのEntity。通常はthis
	 * @param userEntity    : アイテムの持ち主。ついていく対象
	 * @param lengthToUser  : アイテムと持ち主の距離
	 * @param positionAngle : 持ち主から見てアイテムの来る角度。0Fで正面。
	 * @param yForrow       : 垂直方向の角度変化にも反応するか
	 * @param yOffset       : 垂直方向に反応しない場合の高さ
	 */
	public static void itemEffectFollowUser(Entity itemEntity, Entity userEntity, double lengthToUser, float positionAngle, boolean yForrow, double yOffset)
	{
		if(userEntity != null)
		{
			double posX, posY, posZ;

			if(yForrow)
			{
				posX = userEntity.getX() - Math.sin((userEntity.getYRot() + positionAngle) / 180F * 3.141593F) * Math.cos(userEntity.getXRot() / 180F * 3.141593F) * lengthToUser;
    			posZ = userEntity.getZ() + Math.cos((userEntity.getYRot() + positionAngle) / 180F * 3.141593F) * Math.cos(userEntity.getXRot() / 180F * 3.141593F) * lengthToUser;
				posY = userEntity.getY() - Math.sin(userEntity.getXRot() / 180F * 3.141593F) * lengthToUser + (double)userEntity.getEyeHeight() - 0.5D;//若干目の高さより下に設置
			}
			else
			{
				posX = userEntity.getX() - Math.sin((userEntity.getYRot() + positionAngle) / 180F * 3.141593F) * lengthToUser;
    			posZ = userEntity.getZ() + Math.cos((userEntity.getYRot() + positionAngle) / 180F * 3.141593F) * lengthToUser;
				posY = userEntity.getY() + yOffset;//若干目の高さより下に設置
			}
			//1.7.10 の motionX/Y/Z 直代入は 1.21.1 では setDeltaMovement に相当
    		itemEntity.setDeltaMovement(
    				userEntity.getDeltaMovement().x + (posX - itemEntity.getX()),
    				userEntity.getDeltaMovement().y + (posY - itemEntity.getY()),
    				userEntity.getDeltaMovement().z + (posZ - itemEntity.getZ()));
    		itemEntity.setPos(posX, posY, posZ);
		}
	}

	//↑と同じだが、垂直方向の角度の影響を受ける場合の簡易
	public static void itemEffectFollowUser(Entity itemEntity, Entity userEntity, double lengthToUser, float positionAngle)
	{
		itemEffectFollowUser(itemEntity, userEntity, lengthToUser, positionAngle, true, 0.0D);
	}

	/**
	 * Entityとして出現しているアイテムの効果が切れたときの処理
	 * @param itemEntity : 効果を終えるEntity。通常はthis
	 * @param userEntity : アイテムの持ち主。プレイヤー
	 * @param item       : アイテム化するアイテム
	 * @param damage     : アイテム化したときのダメージ値（1.21.1 ではアイテムが個別化されたため未使用）
	 * @return
	 */
	public static boolean itemEffectFinish(Entity itemEntity, LivingEntity userEntity, Item item, int damage)
	{
		ItemStack dropStack = new ItemStack(item, 1);

		//使用者がクリエイティブではなく、存在するなら
		if(userEntity != null )
		{
			if(userEntity instanceof Player userEntity_p)
			{
				if(userEntity_p.isCreative())//使用者がクリエイティブモードなら
				{
					if( !itemEntity.level().isClientSide )
					{
						itemEntity.discard();//そのまま消滅させる
					}
					return true;
				}
				//アイテムをインベントリに加える
				if(userEntity_p.getInventory().add(dropStack) == false)
				{
					//インベントリが一杯なら使用者の目の前でアイテム化
					//（クリエイティブでは常にインベントリには物が吸収される仕様があるらしく、クリエイティブでは落とさない（false判定がでない)）
					if(!itemEntity.level().isClientSide)
					{
						itemEntity.level().addFreshEntity(new ItemEntity(itemEntity.level(), itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), dropStack));
						itemEntity.discard();
					}
					return true;
				}
			}
			if(!userEntity.isAlive())//使用者が死んでいるなら
			{
				if(!itemEntity.level().isClientSide)
				{
					//アイテム化し、その場にドロップ
					itemEntity.level().addFreshEntity(new ItemEntity(itemEntity.level(), itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), dropStack));
				}
			}
		}
		else
		{
			//使用者がいない場合（再起動した際など）にはその場にアイテム化
			if(!itemEntity.level().isClientSide)
			{
				itemEntity.level().addFreshEntity(new ItemEntity(itemEntity.level(), itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), dropStack));
			}
    	}
		if(!itemEntity.level().isClientSide)
		{
			itemEntity.discard();
		}
		return true;
	}

	//↑のダメージ値のないアイテム版
	public static boolean itemEffectFinish(Entity itemEntity, LivingEntity userEntity, Item item)
	{
		return itemEffectFinish(itemEntity, userEntity, item, 0);
	}

	/**
	 * スペルカードを宣言する
	 * スペルカード宣言は相手を選択（遠くの相手でも可）することで可能
	 * スペルカードは一人あたり多重には宣言できない
	 *
	 * <p>早苗のスペルカード（旧 meta 14..18、{@link #SANAE_CARD_MIN}～{@link #SANAE_CARD_MAX}）は
	 * 「奇跡の力」（旧 NBT {@code Charge} / 1.21.1 {@link ModDataComponents#CHARGE}）が 0 以下の間は
	 * 宣言できない（非クリエイティブのみ。1.7.10 と同じ条件）。
	 * @param world            : 宣言するワールド
	 * @param itemStack        : スペルカード
	 * @param user 			: スペルカードの宣言者
	 * @param spellCardNumber  : スペルカードナンバー。使用するスペルカードを指定
	 * @param level            : スペルカードの難易度。1～4でEasy～Lunaticに相当。プレイヤー使用のスペルカードは2のNormal
	 * @param firstAttack      : スペルカード名とスペルカード宣言音を出すかどうか。trueなら出す
	 * @return スペルカード宣言に成功したらtrue
	 */
	public static boolean checkSpellCardDeclaration(Level world, ItemStack itemStack, LivingEntity user, int spellCardNumber, int level, boolean firstAttack)
	{

		int needLevel = 0;

		//宣言者がプレイヤーだった場合の処理
		if(user instanceof Player player)
		{
			//プレイヤーがクリエイティブでないなら（クリエイティブは処理不要）
			if(!player.isCreative())
			{
				//早苗のスペルカード（旧 meta 14..18）は「奇跡の力」が充填されている間だけ宣言できる。
				//1.7.10 の itemStack.getItem()==spell_card && damage in [14..18] 判定は、1.21.1 では
				//符卡が spell_card_* の個別アイテム化されたため spellCardNumber（= 旧 meta）の範囲判定に置換した。
				if(spellCardNumber >= SANAE_CARD_MIN && spellCardNumber <= SANAE_CARD_MAX)
				{
					//手に持っている符卡が宣言対象そのものか（別の早苗符卡で代用できないようにする）
					if(!(itemStack.getItem() instanceof ItemTHSpellCard card && card.getSpellCardID() == spellCardNumber))
					{
						logFail(user, spellCardNumber, "held item is not the declared spell card");
						return false;
					}
					//NBT "Charge" → データコンポーネント ModDataComponents.CHARGE（無い場合は 0 扱い）。
					//旧版は「NBT が無い」「Charge <= 0」の双方を宣言失敗にしていたので、getOrDefault(…, 0) <= 0 で同値。
					if(itemStack.getOrDefault(ModDataComponents.CHARGE.get(), 0) <= 0)
					{
						//1.7.10 には宣言失敗時のメッセージが無かったため、専用キーは存在しない。
						//TODO K3-review: lang キー "thKaguya.msg.sneScCharge"（「奇跡之力」不足の警告）が未登録。
						//キー追加までは旧版どおり無言で失敗させる（下の 1 行を有効化すれば表示に切り替わる）。
						//player.displayClientMessage(Component.translatable("thKaguya.msg.sneScCharge"), true);
						logFail(user, spellCardNumber, "no miracle charge on this Sanae card");
						return false;//宣言失敗
					}
				}

				Supplier<? extends THSpellCard> factory = SpellCardRegistry.getSpellCardFactory(spellCardNumber);
				if(factory != null)
				{
					THSpellCard useSpellCard = factory.get();
					if(useSpellCard != null)
					{
						needLevel = useSpellCard.getNeedLevel();
					}
				}

				int bombItemNum = 0;//スペルカードアイテムの総数をカウント

	    		//プレイヤーのインベントリを順番に見ていく
	    		for(int i = 0; i < player.getInventory().items.size(); i++)
	    		{
	    			ItemStack stack = player.getInventory().items.get(i);
	    			//i番目のアイテムがスペルカードアイテム（bomb_item）なら
	    			if(!stack.isEmpty() && stack.is(ModItems.BOMB_ITEM.get()))
	    			{
	    				//スペルカードアイテムをスタックしているなら、そのスタック分countを追加
	    				bombItemNum += stack.getCount();
	    			}
	    		}

	    		//プレイヤーのレベルとスペルカードアイテムの総数の合計が、必要レベルを下回るなら
				if((player.experienceLevel + bombItemNum) < needLevel)
				{
					logFail(user, spellCardNumber, "not enough level (xp=" + player.experienceLevel
							+ " + bomb_item=" + bombItemNum + " < needLevel=" + needLevel + ")");
					return false;//宣言失敗
				}
			}
		}

		// # 修复: 宣言失敗路径原本完全静默（1.7.10 也如此），导致「右键无反应」无法定位。
		// 下面的失败分支补 debug 级日志：只进日志、不进聊天栏，玩家可见行为与旧版一致。
		double angleX = -Math.sin(radTrans(user.getYRot())) * Math.cos(radTrans(user.getXRot()));
    	double angleY = -Math.sin(radTrans(user.getXRot()));
    	double angleZ =  Math.cos(radTrans(user.getYRot())) * Math.cos(radTrans(user.getXRot()));
    	//始点を登録
    	Vec3 vec3d = new Vec3(user.getX(), user.getY() + user.getEyeHeight(), user.getZ());
    	//終点を登録
    	Vec3 vec3d1 = new Vec3(user.getX() + angleX * 32.0D, user.getY() + angleY * 32.0D, user.getZ() + angleZ * 32.0D);
        //始点と終点からブロックとの衝突を取得（液体も含む）
    	//1.7.10 の world.func_147447_a(vec3d, vec3d1, false, true, true) に相当
    	BlockHitResult movingObjectPosition = world.clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, user));
        //ブロックと衝突していたなら
    	if (movingObjectPosition.getType() != HitResult.Type.MISS)
        {
        	//終点を衝突した点に変更
        	vec3d1 = movingObjectPosition.getLocation();
        }
    	Entity entity = null;

    	//このEntityから移動後までの線分に、指定分の範囲を追加した直方体と衝突するEntityのリストを取得
    	AABB searchBox = user.getBoundingBox().expandTowards(angleX * 32.0D, angleY * 32.0D, angleZ * 32.0D).inflate(1.0D, 1.0D, 1.0D);
        List<Entity> list = world.getEntities(user, searchBox);
        double nearDistance = 0.0D;
        for (int j = 0; j < list.size(); j++)
        {
        	// 衝突リストから、i番目のEntityを取得
            Entity entity1 = list.get(j);

            //TODO K3-review: 1.7.10 は EntityAnimal / EntityVillager を対象外にしていた。対応は Animal / AbstractVillager。
    		if(entity1 instanceof LivingEntity && entity1 instanceof Animal == false && entity1 instanceof AbstractVillager == false)
    		{
        		float expand = 0.3F;//当たり判定の加算値
            	AABB axisalignedbb = entity1.getBoundingBox().inflate(expand, expand, expand);
            	Optional<Vec3> movingObjectPosition1 = axisalignedbb.clip(vec3d, vec3d1);
        		//Entityとの衝突がないなら、このEntityはパスする
            	if (movingObjectPosition1.isPresent())
            	{
        			//始点からEntityに衝突した点までの距離を取得
            		double distanceToHitEntity = vec3d.distanceTo(movingObjectPosition1.get());
        			//今までで一番近いEntityなら、Entityと距離を記憶する
            		if (distanceToHitEntity < nearDistance || nearDistance == 0.0D)
            		{
                		entity = entity1;
                		nearDistance = distanceToHitEntity;
            		}
            	}
            }
        }

    	//同一プレイヤーが64格以内にすでにスペルカードを展開していないか
    	List<Entity> list2 = world.getEntities(user, user.getBoundingBox().inflate(64.0D, 64.0D, 64.0D));
    	for(int k = 0; k < list2.size(); k++)
    	{
    		Entity entity2 = list2.get(k);
    		if(entity2 instanceof EntitySpellCard entitySpellCard2)
    		{
    			if(entitySpellCard2.user == user)
    			{
    				entity = null;//スペルカード宣言を無効化
    				logFail(user, spellCardNumber, "this user already has a spell card active within 64 blocks");
    				return false;
    			}
    		}
    	}

    	//Entityに当たっていないなら宣言失敗
    	if (entity == null)
        {
        	// # 修复: 「右键无反应」最常见的原因就是这一条——32 格视线内没有合格目标
        	// （1.7.10 同样把 Animal / AbstractVillager 排除在外）。日志里直接给出候选数量，便于定位。
        	logFail(user, spellCardNumber, "no valid target in line of sight (32 blocks); candidates in search box="
        			+ list.size() + " blockHit=" + movingObjectPosition.getType());
        	return false;
        }

    	if (itemProssessionCheck(world, user, spellCardNumber))
        {
        	if(entity instanceof LivingEntity)
        	{
        		if(!world.isClientSide)
        		{
        			spellCardDeclaration(world, user, (LivingEntity)entity, spellCardNumber, needLevel, level, firstAttack);
        			return true;
        		}
        		else
        		{
        			return false;
        		}
        	}
        }
		return false;
	}

	/**
	 * 宣言に失敗した理由を debug ログに残す（1.7.10 は無言で失敗していたため、移植後の診断用に追加）。
	 * レベルは debug なので通常プレイのログを汚さず、プレイヤーには何も表示しない。
	 */
	private static void logFail(LivingEntity user, int spellCardNumber, String reason)
	{
		THKaguyaMod.LOGGER.debug("Spell card declaration rejected: card={} user={} reason={}",
				spellCardNumber, user.getScoreboardName(), reason);
	}

	/**
	 * スペルカードの宣言で宣言が可能だった場合の処理
	 * @param world
	 * @param user スペルカード使用者
	 * @param target スペルカードを宣言された相手
	 * @param spellCardNumber スペルカードNo
	 * @param needLevel 宣言に必要なレベル
	 * @param level スペルカードのレベル。1:Easy  2:Normal  3:Hard  4:Lunatic  プレイヤー宣言は基本2
	 * @param firstAttack trueならスペルカード名とスペルカード音が出る
	 */
	public static void spellCardDeclaration(Level world, LivingEntity user, LivingEntity target, int spellCardNumber, int needLevel, int level, boolean firstAttack)
	{
		//使用するスペルカード
		EntitySpellCard entitySpellCard = new EntitySpellCard(ModEntities.SPELL_CARD.get(), world, user, target, spellCardNumber, level);
		//スペルカード名
		//TODO K3-review: 1.7.10 は StatCollector.translateToLocal("item.thSpellCard." + num + ".name") だったが、
		//1.21.1 の符卡は spell_card_<name> の 35 個別アイテムでキー体系が異なる。表示キーの確定が必要。
		Component spellCardName = Component.translatable("item.thSpellCard." + spellCardNumber + ".name");

		if(!world.isClientSide)
		{
			world.addFreshEntity(entitySpellCard);//スペルカードを出現させる
		}

		//宣言者がプレイヤーの場合
	    if(user instanceof Player entityPlayer_user)
	    {
	       	int bomb = 0;

	       	if(!world.isClientSide)
	       	{
	       		//使用者がスペルカードを使ったというメッセージを出す
	       		entityPlayer_user.sendSystemMessage(spellCardName);
	       		//スペルカード宣言音を出す
	       		world.playSound(null, entityPlayer_user.blockPosition(), ModSounds.SPELLCARD.get(), SoundSource.PLAYERS, ModConfig.SpellCardVol, 1.0F);
	       	}

	       	//プレイヤーのインベントリを順番に見ていく
	       	//消費ボムカウントが必要レベルより低く、ボムアイテムを持っている間続ける
	       	while(bomb < needLevel && hasBombItem(entityPlayer_user))
	       	{
	       		//インベントリからボムアイテムを１つ消費する
	       		consumeOneBombItem(entityPlayer_user);
	       		bomb++;
	       	}

	       	//ボムアイテムで足りなかった分は、レベルを消費
	       	entityPlayer_user.giveExperienceLevels(bomb - needLevel);
	    }

	    //宣言対象がプレイヤーの場合
	    if(target instanceof Player entityPlayer_target)
	    {
	    	//継続スペルカードでなければ
	    	if(firstAttack)
	    	{
		       	if(!world.isClientSide)
		       	{
		       		//宣言対象にスペルカード宣言メッセージを出す
		       		entityPlayer_target.sendSystemMessage(spellCardName);
		       		//スペルカード宣言音を出す
		       		world.playSound(null, entityPlayer_target.blockPosition(), ModSounds.SPELLCARD.get(), SoundSource.PLAYERS, ModConfig.SpellCardVol, 1.0F);
		       	}
	    	}
	    }

		//継続的に使っているスペルカードでなければ、周囲の弾幕を回収する
       	if(firstAttack)
       	{
       		THShotLib.danmakuRemove(user, 40.0F, "Enemy", true);
       	}
	}

	/** インベントリにボムアイテム（bomb_item）を持っているか */
	private static boolean hasBombItem(Player player)
	{
		for(int i = 0; i < player.getInventory().items.size(); i++)
		{
			ItemStack stack = player.getInventory().items.get(i);
			if(!stack.isEmpty() && stack.is(ModItems.BOMB_ITEM.get()))
			{
				return true;
			}
		}
		return false;
	}

	/** インベントリからボムアイテムを１つ消費する（1.7.10 の consumeInventoryItem に相当） */
	private static void consumeOneBombItem(Player player)
	{
		for(int i = 0; i < player.getInventory().items.size(); i++)
		{
			ItemStack stack = player.getInventory().items.get(i);
			if(!stack.isEmpty() && stack.is(ModItems.BOMB_ITEM.get()))
			{
				player.getInventory().removeItem(i, 1);
				return;
			}
		}
	}

	private static boolean itemProssessionCheck(Level world, LivingEntity EntityLivingBase, int nomber)
	{
		return true;
	}

	/**
	 * プレイヤーのパワーを返す。最大400
	 * @param player
	 * @return
	 */
	public static float getPlayerPower(Player player)
	{
		float power = 0.0F;

		/* プレイヤーのインベントリを順番に見ていく */
		for(int i = 0; i < player.getInventory().items.size(); i++)
		{
			ItemStack stack = player.getInventory().items.get(i);
			/* i番目のアイテムがパワーアップアイテムの場合 */
			if(!stack.isEmpty() && stack.is(ModItems.POWER_UP_ITEM_SMALL.get()))
			{
				/* 小の場合パワー+1 */
				power += 1.0F * stack.getCount();
			}
			else if(!stack.isEmpty() && stack.is(ModItems.POWER_UP_ITEM_BIG.get()))
			{
				/* 大の場合パワー+10 */
				power += 10.0F * stack.getCount();
			}
		}

		if( power > 400.0F )
		{
			power = 400.0F;
		}

		return power;
	}


	/**
	 * 銀のナイフを出現させる
	 * @param user 銀のナイフの使用者
	 * @param pos 銀のナイフの発射される座標
	 * @param angle 銀のナイフの角度
	 * @param speed 銀のナイフの速度
	 * @param color 銀のナイフの色。0で青、1で赤、2で緑、3で白
	 * @param startCount
	 * @return 出現したEntitySilverKnifeを返す
	 */
	//TODO K3-review: 1.7.10 は EntitySilverKnife（thKaguyaMod.entity.item）を生成して addFreshEntity し返した。
	//EntitySilverKnife はまだ 1.21.1 に移植されていないため、本体は未実装のまま残す（戻り値も暫定で Entity）。
	//移植後: user.level() で EntitySilverKnife を生成 → setKnifeCount(startCount) → addFreshEntity → 返す。
	public static Entity createSilverKnife(LivingEntity user, Vec3 pos, Vec3 angle, double speed, int color, int startCount)
	{
		return null;
	}

	public static float radTrans(float deg)
	{
		return deg / 180F * (float)Math.PI;
	}

	public static float degTrans(float rad)
	{
		return rad / (float)Math.PI * 180F;
	}
}
