package thkaguyamod.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import thkaguyamod.config.ModConfig;
import thkaguyamod.danmaku.DanmakuParams;
import thkaguyamod.danmaku.LaserData;
import thkaguyamod.danmaku.THShotLib;
import thkaguyamod.init.ModDataComponents;

/**
 * 単発レーザーアイテム（1.7.10 {@code thKaguyaMod.item.ItemTHLaser} の 1.21.1 移植）。
 *
 * <p>長さタイプはコンストラクタ引数 {@code laserLengthType} で決まる（0=中/1=短/2=長、
 * 長さは 5/2/8 のみ異なる）。色はコンポーネントの {@code color} を優先し、無ければ
 * 快捷欄で選択中のスロットの右隣の染料から決める（1.7.10 の {@code Item.getItemById(351)} + meta）。
 *
 * <p>アイコン関連（{@code getIconFromDamage}/{@code registerIcons}/{@code getSubItems}）は
 * 1.21.1 のモデル／クリエイティブタブ側で処理されるため移植しない。
 */
public class ItemTHLaser extends Item {

    /** 登録した英語名（レーザー長さ種別の識別用）。 */
    public static final String laserNames[] = {
            "middle", "short", "long"
    };

    /** 旧アイコン名。1.21.1 ではモデル側で処理するため参照されない。 */
    public static final String laserIconName[] = {
            "MiddleLaser", "ShortLaser", "LongLaser"
    };

    /** 各弾の弾速。 */
    public static final float speed[] = {
            0.6F, 0.6F, 0.5F, 0.3F, 0.6F, 0.6F, 0.6F, 0.65F, 0.3F, 0.5F,
    };

    /** 各弾 ID の形状（1.7.10 では各形状の 0 番＝赤の ID を格納し、{@code /8} で形状番号に変換）。 */
    public static final int form[] = {
            THShotLib.SMALL[0]     , THShotLib.TINY[0]      , THShotLib.MEDIUM[0] , THShotLib.BIG[0],
            THShotLib.STAR[0]      , THShotLib.SMALLSTAR[0] , THShotLib.CIRCLE[0] , THShotLib.SCALE[0],
            THShotLib.BUTTERFLY[0] , THShotLib.LIGHT[0]
    };

    /** 陣型名（{@code thKaguya.danmakuForm.*} の添字）。 */
    public static final String danmakuForm[] = {
            "Point", "Random", "Sector", "Around", "Sphere", "未定義"
    };

    /** このアイテムの長さタイプ（0=中/1=短/2=長）。 */
    private final int laserLengthType;

    public ItemTHLaser(Item.Properties properties, int laserLengthType) {
        super(properties);
        this.laserLengthType = laserLengthType;
    }

    /** レーザーの長さタイプ（0=中/1=短/2=長）を返す。 */
    public int getLaserLengthType() {
        return this.laserLengthType;
    }

    /**
     * 右クリック時の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        int type = this.laserLengthType;
        int type2 = form[type] / 8;//IDの互換用

        DanmakuParams params = stack.getOrDefault(ModDataComponents.DANMAKU.get(), DanmakuParams.DEFAULT);

        // 発射色の優先順位（本移植の契約）：コンポーネントの color を優先し、
        // コンポーネントが無い場合のみ快捷欄の右隣の染料から決める。
        // TODO K3-review: 1.7.10 の ItemTHLaser.onItemRightClick はコンポーネント有無に関わらず
        //   「右隣の染料（あれば）or ランダム」で color を決めており、NBT の Color は setColor==9 のときだけ
        //   温存（それ以外は NBT Color で上書き）していた。本移植は契約に従い、コンポーネントが存在すれば
        //   その color を優先する。ただし合成台（DanmakuLaserCraftingMenu）は常に color を書き込むため、
        //   合成品では染料フォールバックに到達しない（旧挙動との差）。“中间色”をコンポーネント未設定で
        //   表現する設計なのか要確認。
        int color;
        if (stack.has(ModDataComponents.DANMAKU.get())) {
            color = params.color();
        } else {
            color = DanmakuParams.DEFAULT.color();
            // 1.7.10: 快捷欄の選択スロットが 0-7 のとき、右隣（currentItem+1）の染料で色を決める。
            // 1.21.1 では同スロットを Inventory.getItem(selected + 1) で参照する。
            Inventory inventory = player.getInventory();
            int selected = inventory.selected;
            if (selected < 8) {
                ItemStack colorItem = inventory.getItem(selected + 1);
                if (colorItem.getItem() instanceof DyeItem dyeItem) {
                    color = dyeColorToDanmakuColor(dyeItem.getDyeColor());
                }
            }
        }

        float length = 5.0F;
        if (type == 1) {
            length = 2.0F;
        } else if (type == 2) {
            length = 8.0F;
        }

        int way = params.number();
        // 1.7.10: shotSpeed = speed[type] * (1 + Speed * 0.03)
        double shotSpeed = speed[type] * (1.0D + (double)params.speed() * 0.03D);
        int danmakuForm = params.form();
        float damage = 5.0F;

        // config で設定した最高弾数より多いなら、最高弾数にする
        if (way > ModConfig.laserMaxNumber) {
            way = ModConfig.laserMaxNumber;
        }

        float wide;
        Vec3 look = THShotLib.angle(player.getYRot(), player.getXRot());

        LaserData laser = LaserData.laser(color, 0.1F, length, damage);

        switch (danmakuForm) {
            case 0:
                double shotSpeed2 = shotSpeed;
                for (int i = 1; i <= way; i++) {
                    THShotLib.createLaserA(player, THShotLib.pos_Living(player), look, shotSpeed2, laser);
                    shotSpeed2 = shotSpeed / (double)way * (double)i;
                }
                break;
            case 1:
                wide = 30F;
                if (player.isShiftKeyDown()) {
                    wide *= 0.5F;
                }
                THShotLib.createRandomRingLaser(player, player, THShotLib.pos_Living(player), look,
                        shotSpeed, shotSpeed, 0.0D, THShotLib.gravity_Zero(), laser, way, 0.2D, wide);
                break;
            case 2:
                wide = way * 5F;
                if (player.isShiftKeyDown()) {
                    wide = wide * 0.5F;
                }
                THShotLib.createWideLaserA(player, player, THShotLib.pos_Living(player), player.getLookAngle(),
                        shotSpeed, shotSpeed, 0.0D, THShotLib.gravity_Zero(), laser, way, wide, 0.1D);
                break;
            case 3:
                THShotLib.createCircleLaserA(player, player, THShotLib.pos_Living(player), player.getLookAngle(),
                        shotSpeed, shotSpeed, 0.0D, THShotLib.gravity_Zero(), laser, way, 0.1D);
                break;
            case 4:
                THShotLib.createSphereLaserA(player, player, THShotLib.pos_Living(player), look,
                        shotSpeed, shotSpeed, 0.0D, THShotLib.gravity_Zero(), laser, way, 0.2D, 0F);
                break;
            case 5:
                wide = 15F;
                if (player.isShiftKeyDown()) {
                    wide *= 0.5F;
                }
                THShotLib.createRingLaserA(player, player, THShotLib.pos_Living(player), look,
                        shotSpeed, shotSpeed, 0.0D, THShotLib.gravity_Zero(), laser, way, 0.1D, wide,
                        player.getRandom().nextFloat() * 360F);
                break;
            default:
                break;
        }

        if (!level.isClientSide) {
            stack.shrink(1);
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * 1.7.10 {@code ItemTHLaser} は「右隣の染料の meta（0=ink_sac … 15=bone_meal）」を
     * {@code sellectbleColor} で 8 色弹色に変換していた。1.21.1 では染料が {@link DyeItem} に
     * なっているため、{@link DyeColor} から同じ並びの meta を復元してから {@code sellectbleColor} を引く。
     */
    private static int dyeColorToDanmakuColor(DyeColor dyeColor) {
        int meta = switch (dyeColor) {
            case BLACK       -> 0;   // ink_sac
            case RED         -> 1;   // red_dye
            case GREEN       -> 2;   // green_dye
            case BROWN       -> 3;   // cocoa_beans
            case BLUE        -> 4;   // lapis_lazuli
            case PURPLE      -> 5;   // purple_dye
            case CYAN        -> 6;   // cyan_dye
            case LIGHT_GRAY  -> 7;   // light_gray_dye
            case GRAY        -> 8;   // gray_dye
            case PINK        -> 9;   // pink_dye
            case LIME        -> 10;  // lime_dye
            case YELLOW      -> 11;  // yellow_dye
            case LIGHT_BLUE  -> 12;  // light_blue_dye
            case MAGENTA     -> 13;  // magenta_dye
            case ORANGE      -> 14;  // orange_dye
            case WHITE       -> 15;  // bone_meal
        };
        return ItemTHShot.sellectbleColor[meta % 16];
    }

    /**
     * 1.7.10 {@code addInformation} → {@code appendHoverText}。
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        DanmakuParams params = stack.getOrDefault(ModDataComponents.DANMAKU.get(), DanmakuParams.DEFAULT);

        int type = this.laserLengthType;
        int shotNum = params.number();
        int formIndex = params.form();
        float shotSpeed = (float)speed[type] * (1.0F + (float)params.speed() * 0.03F);
        int color = params.color();

        if (formIndex >= danmakuForm.length) {
            formIndex = danmakuForm.length - 1;
        }

        tooltip.add(Component.translatable("danmakuCrafting.damage").append(" : 2.5"));
        tooltip.add(Component.translatable("danmakuCrafting.number").append(" : " + shotNum));
        tooltip.add(Component.translatable("danmakuCrafting.form")
                .append(" : ").append(Component.translatable("thKaguya.danmakuForm." + formIndex)));
        tooltip.add(Component.translatable("danmakuCrafting.speed").append(" : " + shotSpeed));
        tooltip.add(Component.translatable("danmakuCrafting.color")
                .append(" : ").append(Component.translatable("thKaguya.color." + color)));
    }
}
