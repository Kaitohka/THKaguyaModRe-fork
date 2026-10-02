package thkaguyamod.spellcard;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.world.phys.Vec3;

import thkaguyamod.danmaku.THShotLib;

import static thkaguyamod.danmaku.DanmakuConstants.FORM_STAR;

/** 星符「メテオニックシャワー」 */
public class THSC_MeteonicShower extends THSpellCard
{
	public THSC_MeteonicShower()
	{
		this.setNeedLevel(1);
		this.setRemoveTime(10);
		this.setEndTime(60);
		this.setOriginalUserName(MARISA);
	}

	@Override
	public void spellcard_main()
	{
		if(time > 10 && time < 40 && time % 2 == 0)
		{
			Vec3 angle = user.getLookAngle();
			if(target.isAlive())
			{
				angle = THShotLib.angle_ToPos(this.pos_User(), this.pos_Target());
			}
			ThreadLocalRandom rand = ThreadLocalRandom.current();
			int colors[] = {0, 2, 3, 4};
			int color = colors[rand.nextInt(4)];
			float size = 0.2F + rand.nextFloat() * 0.4F;
			int damage = (int)(size * 15F);

			THShotLib.createRandomRingShot(user, card, pos_User(angle, 0.7D), angle, 0.0F, 9999, 0.4D, 0.6D, 0.1D, gravity_Zero(), shot(FORM_STAR, color, size, damage, 3, 40), 1, 0.0D, 40F);
			THShotLib.playShotSound(user);
		}
	}
}
