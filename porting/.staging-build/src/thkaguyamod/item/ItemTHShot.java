package thkaguyamod.item;

import java.util.List;
import java.util.Random;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.DanmakuParams;
import thkaguyamod.danmaku.ShotData;
import thkaguyamod.danmaku.THKaguyaLib;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModDataComponents;

import static thkaguyamod.danmaku.DanmakuConstants.BOUND01;

/**
 * 弾アイテム（1.7.10 {@code thKaguyaMod.item.ItemTHShot} の 1.21.1 移植）。
 *
 * <p>1.7.10 からの主な差異：
 * <ul>
 *   <li>弾の形状はスタックの damage 値ではなく、この Item インスタンスが保持する {@code itemTypeIndex}
 *       （弾形番号 0-18、旧 meta 値）で決まる。1.21.1 では 19 種類の弾形が独立アイテムに分割されている
 *       （{@code ModItems.SMALL_SHOT} … {@code ModItems.ARROW_SHOT}）ため。</li>
 *   <li>弾幕パラメータは NBT ではなく {@link ModDataComponents#DANMAKU}（{@link DanmakuParams}）から読む。
 *       発射側は読み取り専用で、旧コードの「創造モードで Infinity を書き込む」処理は行わない
 *       （創造モードは {@code Player#getAbilities().instabuild} で判定する）。</li>
 *   <li>アイコン関連（{@code getIconFromDamage}/{@code requiresMultipleRenderPasses}/
 *       {@code getColorFromItemStack}/{@code registerIcons}/{@code getSubItems}）は移植しない
 *       （1.21.1 のモデル／クリエイティブタブ側で処理される）。</li>
 * </ul>
 */
public class ItemTHShot extends Item {

    /** 登録した英語名（弾形の識別用。アイコン名・アンロカライズ名には使われない）。 */
    public static final String shotNames[] = {
            "smallShot"    , "tinyShot"      , "mediumShot"   , "bigShot",
            "starShot"     , "smallStarShot" , "circleShot"   , "scaleShot",
            "butterflyShot", "lightShot"     , "knifeShot"    , "heartShot",
            "kunaiShot"    , "talismanShot"  , "bigLightShot" , "riceShot",
            "ovalShot"     , "crystalShot"   , "arrowShot"
    };

    /**
     * 旧アイコン名。1.7.10 では {@code getIconFromDamage} で使用されたが、1.21.1 では
     * モデルシステムが処理するため参照されない。データ保全のためだけに残している。
     */
    public static final String shotIconName[] = {
            "SmallShot", "TinyShot", "MediumShot", "BigShot",
            "StarShot", "SmallStarShot", "CircleShot", "ScaleShot",
            "ButterflyShot", "LightShot", "KnifeShot", "HeartShot",
            "KunaiShot", "TalismanShot", "BigLightShot", "RiceShot",
            "OvalShot", "CrystalShot", "ArrowShot"
    };

    /** 弾アイテム（0-18）と Entity 側の弾形状 ID の変換用。 */
    public static final int shotTypeTrans[] = {
             0,  1,  2, 30,
             9,  8,  4,  6,
             7,  5, 28, 12,
            13, 14, 15, 10,
            16, 11, 18
    };

    /** 各弾の弾速。 */
    public static final float speed[] = {
            0.50F, 0.50F, 0.40F, 0.25F,
            0.50F, 0.50F, 0.50F, 0.55F,
            0.30F, 0.50F, 0.65F, 0.40F,
            0.60F, 0.50F, 0.40F, 0.50F,
            0.40F, 0.45F, 0.50F
    };

    /** 各弾 ID の形状（Entity 側の弾形状 ID）。 */
    public static final int form[] = {
            THShotLib.FORM_SMALL     , THShotLib.FORM_TINY     , THShotLib.FORM_MEDIUM , THShotLib.FORM_BIG,
            THShotLib.FORM_STAR      , THShotLib.FORM_SMALLSTAR, THShotLib.FORM_CIRCLE , THShotLib.FORM_SCALE,
            THShotLib.FORM_BUTTERFLY , THShotLib.FORM_LIGHT    , THShotLib.FORM_KNIFE  , THShotLib.FORM_HEART,
            THShotLib.FORM_KUNAI     , THShotLib.FORM_TALISMAN , THShotLib.FORM_BIGLIGHT, THShotLib.FORM_RICE,
            THShotLib.FORM_OVAL      , THShotLib.FORM_CRYSTAL  , THShotLib.FORM_ARROW
    };

    public static final int SMALL       =  0;
    public static final int TINY        =  1;
    public static final int MEDIUM      =  2;
    public static final int BIG         =  3;
    public static final int STAR        =  4;
    public static final int SMALLSTAR   =  5;
    public static final int CIRCLE      =  6;
    public static final int SCALE       =  7;
    public static final int BUTTERFLY   =  8;
    public static final int LIGHT       =  9;
    public static final int KNIFE       = 10;
    public static final int HEART       = 11;
    public static final int KUNAI       = 12;
    public static final int TALISMAN    = 13;
    public static final int BIGLIGHT    = 14;
    public static final int RICE        = 15;
    public static final int OVAL        = 16;
    public static final int CRYSTAL     = 17;
    public static final int ARROW       = 18;

    /** 陣型名（{@code thKaguya.danmakuForm.*} の添字）。 */
    public static final String danmakuForm[] = {
            "Point", "Random", "Sector", "Around", "Sphere", "Ring", "未定義"
    };

    /** 16 色の染料を 8 色の弾色に対応させるための配列（合成台からも参照される）。 */
    public static final int sellectbleColor[] = {7, 0, 2, 0, 1, 4, 5, 7, 7, 4, 2, 3, 5, 4, 6, 7};

    // 1.7.10 の colorR/G/B（getColorFromItemStack 専用）は移植しない。
    // アイテムの色合いは 1.21.1 のモデル／tipped arrow 相当の機構に委ねる。

    /** このアイテムが表す弾形番号（旧 meta 値 0-18）。 */
    private final int itemTypeIndex;

    public ItemTHShot(Item.Properties properties, int itemTypeIndex) {
        super(properties);
        this.itemTypeIndex = itemTypeIndex;
    }

    /** 弾形番号（旧 meta 値 0-18）を返す。 */
    public int getItemTypeIndex() {
        return this.itemTypeIndex;
    }

    /**
     * 右クリック時の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 1.7.10: shootDanmaku(itemStack, player, player.isSneaking(),
        //     pos_Living(player), player.getLookVec(), SIZE[shotTypeTrans[damage]])
        shootDanmaku(stack, player, player.isShiftKeyDown(),
                THShotLib.pos_Living(player), player.getLookAngle(),
                THShotLib.SIZE[shotTypeTrans[this.itemTypeIndex]]);

        THShotLib.playShotSound(player);

        if (!level.isClientSide) {
            // 1.7.10 は「直前までに NBT Infinity が立っていたら消費しない」だった。
            // 創造モードは instabuild で、コンポーネント infinity はそのまま尊重する。
            DanmakuParams params = stack.getOrDefault(ModDataComponents.DANMAKU.get(), DanmakuParams.DEFAULT);
            if (!player.getAbilities().instabuild && !params.infinity()) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * 弾幕を発射する（1.7.10 {@code ItemTHShot.shootDanmaku} の忠実移植）。
     *
     * <p>弾形は {@code itemStack} の Item が {@link ItemTHShot} であることを前提に取得する。
     * 弾幕パラメータは {@link ModDataComponents#DANMAKU} から読む（書き込みはしない）。
     *
     * @param itemStack  発射に使う ItemStack（弾形とパラメータの供給元）
     * @param living     発射主
     * @param isSlowMode 潜行による散布幅の縮小フラグ
     * @param pos        発射地点
     * @param angle      発射方向
     * @param distance   発射地点からこの分だけ離して発射する
     */
    public static void shootDanmaku(ItemStack itemStack, LivingEntity living, boolean isSlowMode,
                                    Vec3 pos, Vec3 angle, double distance) {
        if (!(itemStack.getItem() instanceof ItemTHShot shotItem)) {
            return;
        }

        Random rand = new Random();
        int type = shotItem.itemTypeIndex;
        int type2 = form[type];//IDの互換用

        DanmakuParams params = itemStack.getOrDefault(ModDataComponents.DANMAKU.get(), DanmakuParams.DEFAULT);

        int color = params.color();
        int special = params.special();
        int way = params.number();
        int danmakuForm = params.form();
        // 1.7.10: shotSpeed = speed[type] * (1 + Speed * 0.03)
        double shotSpeed = speed[type] * (1.0D + (double)params.speed() * 0.03D);
        // 1.7.10: gravity = Gravity * -0.003
        double gravity = (double)params.gravity() * -0.003D;

        // config で設定した最高弾数より多いなら、最高弾数にする
        if (way > ModConfig.shotMaxNumber) {
            way = ModConfig.shotMaxNumber;
        }

        float wide;
        float damagePlus = 0.0F;
        if (living instanceof Player player) {
            damagePlus += THKaguyaLib.getPlayerPower(player) / 400.0F;
        }

        ShotData shot = ShotData.shot(type2, color, THShotLib.SIZE[type2], THShotLib.DAMAGE[type2] + damagePlus,
                0, 80, special);

        switch (danmakuForm) {
            case 0://一点
                double shotSpeed2 = shotSpeed;
                // 1.7.10 は可変 Vec3 を直接書き換えていた。1.21.1 の Vec3 は不変なので新規作成する。
                Vec3 shotPos = new Vec3(pos.x + angle.x * distance,
                                        pos.y + angle.y * distance,
                                        pos.z + angle.z * distance);
                for (int i = 1; i <= way; i++) {
                    THShotLib.createShot(living, living, shotPos, angle, 0F, shotSpeed2, shotSpeed2, 0.0D,
                            THShotLib.gravity(gravity), shot);
                    shotSpeed2 = shotSpeed / (double)way * (double)i;
                }
                break;
            case 1://前方ランダム
                wide = 120F;
                if (isSlowMode) {
                    wide = 60F;
                }
                THShotLib.createRandomRingShot(living, living, pos, angle, 0F, 9999, shotSpeed, shotSpeed, 0.0D,
                        THShotLib.gravity(0.0D, gravity, 0.0D), shot, way, distance, wide);
                break;
            case 2://前方n-way
                wide = way * 3F;
                if (isSlowMode) {
                    wide = wide * 0.5F;
                }
                THShotLib.createWideShot(living, living, pos, angle, shotSpeed, shotSpeed, 0.0D,
                        THShotLib.gravity(0.0D, gravity, 0.0D), shot, way, wide, distance);
                break;
            case 3://全方位弾
                THShotLib.createCircleShot(living, living, pos, angle, shotSpeed, shotSpeed, 0.0D,
                        THShotLib.gravity(0.0D, gravity, 0.0D), shot, way, distance, 0F);
                break;
            case 4://球状
                THShotLib.createSphereShot(living, living, pos, angle, 0F, THShotLib.rotate_Default(), 0F, 99999,
                        shotSpeed, shotSpeed, 0.0D, THShotLib.gravity_Zero(), shot, way, distance, 0F);
                break;
            case 5://リング
                wide = 15F;
                if (isSlowMode) {
                    wide *= 0.5F;
                }
                THShotLib.createRingShot(living, living, pos, angle, 0F, 9999, shotSpeed, shotSpeed, 0.0D,
                        THShotLib.gravity(0.0D, gravity, 0.0D), shot, way, distance, wide, rand.nextFloat() * 360F);
                break;
            default:
                break;
        }
    }

    /**
     * 1.7.10 {@code hasEffect(stack, pass)} → {@code isFoil(stack)}。
     * コンポーネント infinity が立っているときだけ発光させる。
     */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.DANMAKU.get(), DanmakuParams.DEFAULT).infinity();
    }

    /**
     * 1.7.10 {@code addInformation} → {@code appendHoverText}。
     *
     * <p>TODO K3-review: 旧コードは「基礎ダメージ + getPlayerPower(player)/400」を表示していたが、
     * 1.21.1 の {@link Item.TooltipContext} はプレイヤーを持たないため、ここでは基礎ダメージのみを
     * 表示する。プレイヤー由来の威力ボーナスを出すには拡張が必要（表示専用の差異）。
     * TODO K3-review: 旧コード末尾の {@code translateToLocal("Infinity")} は lang に "Infinity" キーが
     * 存在しないため、忠実な表示は "Infinity" 固定文字列になる。ここでは {@link Component#literal} を使う。
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        DanmakuParams params = stack.getOrDefault(ModDataComponents.DANMAKU.get(), DanmakuParams.DEFAULT);

        int type = this.itemTypeIndex;
        int shotNum = params.number();
        int formIndex = params.form();
        float shotSpeed = (float)speed[type] * (1.0F + (float)params.speed() * 0.03F);
        int color = params.color();
        double gravity = (double)params.gravity() * 0.003D;
        int bound = params.special() - BOUND01 + 1;
        boolean isInfinity = params.infinity();

        if (bound < 0) {
            bound = 0;
        }
        if (formIndex >= danmakuForm.length) {
            formIndex = danmakuForm.length - 1;
        }

        tooltip.add(Component.translatable("danmakuCrafting.damage")
                .append(" : " + ((float)THShotLib.DAMAGE[shotTypeTrans[type]] / 2F)));
        tooltip.add(Component.translatable("danmakuCrafting.number").append(" : " + shotNum));
        tooltip.add(Component.translatable("danmakuCrafting.form")
                .append(" : ").append(Component.translatable("thKaguya.danmakuForm." + formIndex)));
        tooltip.add(Component.translatable("danmakuCrafting.speed").append(" : " + shotSpeed));
        if (gravity != 0.0D) {
            tooltip.add(Component.translatable("danmakuCrafting.gravity").append(" : " + gravity));
        } else {
            tooltip.add(Component.translatable("danmakuCrafting.gravity").append(" : GFree"));
        }
        tooltip.add(Component.translatable("danmakuCrafting.color")
                .append(" : ").append(Component.translatable("thKaguya.color." + color)));
        if (bound >= 1 && bound <= 3) {
            tooltip.add(Component.translatable("danmakuCrafting.bound").append(" : " + bound));
        } else if (bound == 4) {
            tooltip.add(Component.translatable("danmakuCrafting.bound").append(" : Infinity"));
        } else {
            tooltip.add(Component.translatable("danmakuCrafting.bound").append(" : 0"));
        }
        if (isInfinity) {
            tooltip.add(Component.literal("Infinity"));
        }
    }
}
