package thkaguyamod.item;

import java.util.List;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import thkaguyamod.entity.item.EntitySakuyaWatch;
import thkaguyamod.init.ModEntities;

/**
 * 咲夜の懐中時計 — ported from 1.7.10 {@code thKaguyaMod.item.ItemSakuyaWatch}.
 *
 * <p>周囲 20 ブロック以内に既存の懐中時計が無いときだけ、時間停止／減速空間
 * （{@link EntitySakuyaWatch}）を出現させる。耐久値（0/1）で低速モードと停止モードを切り替える。
 * スニーク右クリック（2 tick おき）でモードをトグルする。
 *
 * <p>TODO K3-review: 1.7.10 は {@code Item#getDamage}/{@code setDamage} をモードフラグとして
 * 使っていた。1.21.1 では {@code ItemStack#setDamageValue} が {@code getMaxDamage()} でクランプ
 * するため、登録側に {@code durability(1)} 以上が無いと「1」を保存できない
 * （{@code ItemKaigo} の TODO と同じ問題）。登録側の properties で耐久 1 を確保するか、
 * 専用のデータコンポーネントに移すこと。
 *
 * <p>TODO K3-review: 原作の {@code onItemRightClick} は {@code int mode;} を宣言したまま
 * 一度も代入していなかった（死コード）。本移植では削除している。
 *
 * <p>TODO K3-review: 原作は {@code onEaten}（＝使用完了）を直接呼び出す形でモード判定しており、
 * {@code usedTime} は常に 24 固定だった（実プレイ時間を見ていない）。本移植も同じ定数 24 を
 * 使用して原作の挙動を維持している。
 */
public class ItemSakuyaWatch extends Item {

    public ItemSakuyaWatch(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // スニークではモード切り替えができる
        if (player.isShiftKeyDown() && player.tickCount % 2 == 0) {
            if (stack.getDamageValue() == 0) {
                stack.setDamageValue(1);
            } else {
                stack.setDamageValue(0);
            }
            return InteractionResultHolder.success(stack);
        }

        // クリエイティブの場合
        if (player.isCreative() && !level.isClientSide) {
            finishUsingItem(stack, level, player);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.success(stack);
    }

    /**
     * 使用を終えたときの処理（1.7.10 {@code onEaten}）。
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) {
            return stack;
        }

        int usedTime = 24;
        int mode = EntitySakuyaWatch.TIME_HALF_WITH_LIMIT;

        //空腹でないか、クリエイティブなら使用可能
        if (player.getFoodData().getFoodLevel() > 0 || player.isCreative()) {
            // クリエイティブの場合
            if (player.isCreative()) {
                mode = EntitySakuyaWatch.TIME_STOP;
                if (stack.getDamageValue() == 0)//スニークなら低速モード　　通常は停止モード
                {
                    mode = EntitySakuyaWatch.TIME_HALF;
                }
            }
            // クリエイティブではない場合で、1.2秒以上右クリックしている場合
            else if (usedTime >= getUseDuration(stack, player)) {
                mode = EntitySakuyaWatch.TIME_STOP_WITH_LIMIT;
                if (stack.getDamageValue() == 0)//スニークなら低速モード　　通常は停止モード
                {
                    mode = EntitySakuyaWatch.TIME_HALF_WITH_LIMIT;
                }
            }
            // 上記の場合以外無効
            else {
                if (player.isShiftKeyDown())//スニークなら低速モード　　通常は停止モード
                {
                    if (stack.getDamageValue() == 0) {
                        stack.setDamageValue(1);
                    } else {
                        stack.setDamageValue(0);
                    }
                }
                return stack;
            }

            //周囲のEntityを取得し、既に懐中時計があるなら失敗する
            List<EntitySakuyaWatch> list = level.getEntitiesOfClass(EntitySakuyaWatch.class,
                    player.getBoundingBox().inflate(20.0D, 20.0D, 20.0D));
            if (!list.isEmpty()) {
                return stack;
            }

            // 1.7.10 の "random.click"。1.21.1 では ui.button.click を近似として使用。
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS,
                    0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));

            EntitySakuyaWatch entityPrivateSquare = new EntitySakuyaWatch(ModEntities.SAKUYA_WATCH.get(), level, player, mode);
            if (!level.isClientSide) {
                level.addFreshEntity(entityPrivateSquare);//時間停止空間を生み出す
            }
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (stack.getDamageValue() == 0) {
            return 20;
        } else {
            return 48;
        }
    }

    /** 1.7.10 の {@code EnumAction.bow} に相当。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    /** damage 1（停止モード）のときだけ発光させる（1.7.10 の {@code hasEffect} 相当）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getDamageValue() != 0;
    }
}