package thkaguyamod.spellcard;

/**
 * 神槍「スピア・ザ・グングニル」
 *
 * <p>1.7.10 の空殻クラスを忠実移植（{@code THSC_Spear_the_Gungnir}）。
 * 弾幕ロジックは元から無く、{@code spellcard_main()} は基底の空実装のまま。
 * 専用のグングニル弾（LASER_GUNGNIR / GUNGNIR）は別途 ItemSpearTheGungnir 側で生成される。
 */
public class THSC_Spear_the_Gungnir extends THSpellCard
{
	public THSC_Spear_the_Gungnir()
	{
		this.setNeedLevel(3);
		this.setRemoveTime(10);
		this.setEndTime(50);
		this.setOriginalUserName(REMILIA);
	}
}
