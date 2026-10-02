package thkaguyamod.entity.living;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/** 霖之助 */
public class EntityRinnosuke extends Villager
{

	public EntityRinnosuke(EntityType<? extends EntityRinnosuke> type, Level level)
	{
		super((EntityType<? extends Villager>)(EntityType<?>)type, level);
		//TODO K3-review: 1.7.10 は THKaguyaCore.villagerRinnosukeId (=932) を職業IDとして
		//setProfession していた。1.21.1 の職業は VillagerProfession レジストリオブジェクトで、
		//このMODは自前の職業をまだ登録していない。暫定として VillagerProfession.NONE を設定する。
		//専用職業（rinnosuke）を登録し、ここを差し替えること。
		this.setVillagerData(this.getVillagerData().setProfession(VillagerProfession.NONE));
	}

	/** 属性（1.7.10 の applyEntityAttributes に相当。Villager の既定値を継承） */
	public static AttributeSupplier.Builder createAttributes()
	{
		return Villager.createAttributes();
	}

	/**
	 * 1.7.10 の getProfession は常に villagerRinnosukeId を返していた。
	 * @return 職業（暫定で NONE）
	 */
	public VillagerProfession getRinnosukeProfession()
	{
		//TODO K3-review: 専用職業登録後に差し替える。
		return VillagerProfession.NONE;
	}

	//デスポーンするか（1.7.10 の canDespawn = true）
	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer)
	{
		return true;
	}

	//一つのチャンクに湧く最大数
	@Override
	public int getMaxSpawnClusterSize()
	{
		return 1;
	}

	/**
	 * 1.7.10 の createChild は null を返していた（繁殖しない）。
	 * 1.21.1 の対応は getBreedOffspring。
	 */
	@Override
	public Villager getBreedOffspring(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.AgeableMob partner)
	{
		return null;
	}

	/**
	 * 1.7.10 の isMating は false を返していた。
	 */
	@Override
	public boolean canBreed()
	{
		return false;
	}

	//自然スポーンするときに呼ばれる。trueならスポーンする
	@Override
	public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType)
	{
		//1.7.10 は super.getCanSpawnHere() を先に判定していた。1.21.1 の Villager 側チェックを尊重する。
		if(!super.checkSpawnRules(level, spawnType))
		{
			return false;
		}

		if(random.nextInt(100) < 98)
		{
			return false;
		}

		int yPosition = Mth.floor(this.getBoundingBox().minY);
		int xPosition = Mth.floor(this.getX());
		int zPosition = Mth.floor(this.getZ());
		var pointBlock = level.getBlockState(new BlockPos(xPosition, yPosition - 1, zPosition));
		if( 	level.getBlockState(new BlockPos(xPosition - 1, yPosition - 1, zPosition)).equals(pointBlock) &&
				level.getBlockState(new BlockPos(xPosition + 1, yPosition - 1, zPosition)).equals(pointBlock) &&
				level.getBlockState(new BlockPos(xPosition, yPosition - 1, zPosition - 1)).equals(pointBlock) &&
				level.getBlockState(new BlockPos(xPosition, yPosition - 1, zPosition + 1)).equals(pointBlock))
		{
			return false;
		}

		return true;
	}

	/**
	 * 村人データの型（暫定 PLAINS）。専用職業登録時に合わせて調整する。
	 */
	public VillagerData rinnosukeVillagerData()
	{
		return new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1);
	}
}