package thkaguyamod.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

import thkaguyamod.entity.EntityDivineSpirit;
import thkaguyamod.init.ModEntities;

import net.minecraft.tags.EntityTypeTags;

/**
 * 神子の持つ宝剣 — ported from 1.7.10 {@code thKaguyaMod.item.ItemMikoSword}.
 *
 * <p>基本は鉄の剣と同じ。右クリックを押し続けると、離した時点で
 * 認識距離（最大 50 ブロック）内の生物を最大 10 体まで近い順に選び、
 * それぞれの頭上に小霊（{@link EntityDivineSpirit}）を出して種類に応じた色を付け、
 * スニーク時を除いて位置・高低・距離のチャット表示を行う。探知した数だけ耐久を消費する。
 *
 * <p>色分けの対応（1.7.10 → 1.21.1）:
 * <ul>
 *   <li>妖夢妖精（{@code EntityTHFairy}）→ 原作は 85%:1 / 10%:2 / 5%:4。
 *       <b>TODO K3-review</b>: {@code EntityTHFairy} はまだ 1.21.1 に移植されていないため
 *       この分岐は移植していない（妖精が移植されたら
 *       {@code nearEntity instanceof EntityTHFairy} の分岐を追加すること）。</li>
 *   <li>クリーパー → 2 / 村人 → 1 / 動物 → 3（手懐け済みなら 4）/ 水棲生物 → 5</li>
 *   <li>その他の生物 → アンデッド 7 / 虫 6 / それ以外 0</li>
 * </ul>
 *
 * <p>TODO K3-review: 1.7.10 の {@code EnumCreatureAttribute.UNDEAD/ARTHROPOD} は 1.21.1 に
 * 存在しないため {@code EntityTypeTags.UNDEAD}/{@code ARTHROPOD} で代替している。
 * 他 MOD が独自に属性だけを付けたエンティティは拾えない可能性がある。
 *
 * <p>TODO K3-review: 原作の角度計算には
 * {@code if(angle2 &lt; -180F) angle += 360F;} という（{@code angle2} ではなく {@code angle} を
 * 更新している）記述バグがある。本移植は原作の式をそのまま残している。
 *
 * <p>移植しないもの: {@code isFull3D}（1.21.1 はモデル側で処理）。
 */
public class ItemMikoSword extends SwordItem {

    public ItemMikoSword(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        player.startUsingItem(hand);//アイテムの使用継続時間を記憶させる

        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }

    /**
     * 右クリックを押し終わったときに呼び出されるメソッド（1.7.10 {@code onPlayerStoppedUsing}）。
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int usedTime) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        Entity[] nearEntitys;//11番目の空間はゴミ箱
        double[] nearDistance;
        nearEntitys = new Entity[11];
        nearDistance = new double[11];
        double recognizeDistance = (double) (72000 - usedTime);
        if (recognizeDistance > 50D) {
            recognizeDistance = 50D;
        }
        int i;
        for (i = 0; i < 11; i++) {
            nearEntitys[i] = null;
            nearDistance[i] = 999.9D;
        }

        // 1.7.10 の getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.expand(d, d, d))
        List<Entity> list = level.getEntities(player, player.getBoundingBox().inflate(recognizeDistance, recognizeDistance, recognizeDistance));
        for (Entity candidate : list) {
            double distance = player.distanceTo(candidate);
            if (candidate instanceof LivingEntity) {
                int n = 0;
                boolean flag = false;
                while (n < 10 && !flag) {
                    if (nearDistance[n] > distance) {
                        for (int k = 10; k > n; k--) {
                            nearEntitys[k] = nearEntitys[k - 1];
                            nearDistance[k] = nearDistance[k - 1];
                        }
                        nearEntitys[n] = candidate;
                        nearDistance[n] = distance;
                        flag = true;
                    }
                    n++;
                }
            }
        }

        String str = "";
        String strPos;
        String strUpDown;
        String strColor;
        String strNum;
        int num;
        double dx;
        double dz;
        float angle = (360F - player.getYRot()) % 360F;
        float angle2;
        if (angle < -180F) {
            angle += 360F;
        }
        if (angle > 180F) {
            angle -= 360F;
        }
        i = 0;
        while (i < 10 && nearEntitys[i] != null) {
            double eyePosX = nearEntitys[i].getX();
            double eyePosY = nearEntitys[i].getY() + nearEntitys[i].getEyeHeight();
            double eyePosZ = nearEntitys[i].getZ();

            int color = 0;
            // TODO K3-review: EntityTHFairy（妖精）は未移植のため、この分岐は移植していない。
            //   妖精が移植されたら 85%:1 / 10%:2 / 5%:4 のランダム色分岐を追加すること。
            if (nearEntitys[i] instanceof Creeper) {
                color = 2;
            } else if (nearEntitys[i] instanceof Villager) {
                color = 1;
            } else if (nearEntitys[i] instanceof Animal) {
                color = 3;
                if (nearEntitys[i] instanceof TamableAnimal tame && tame.isTame()) {
                    color = 4;
                }
            } else if (nearEntitys[i] instanceof WaterAnimal) {
                color = 5;
            } else if (nearEntitys[i] instanceof LivingEntity living) {
                if (living.getType().is(EntityTypeTags.UNDEAD)) {
                    color = 7;
                } else if (living.getType().is(EntityTypeTags.ARTHROPOD)) {
                    color = 6;
                } else {
                    color = 0;
                }
            }

            EntityDivineSpirit entityDivineSpirit = new EntityDivineSpirit(ModEntities.DIVINE_SPIRIT.get(), level,
                    eyePosX, eyePosY, eyePosZ, color);
            if (!level.isClientSide) {
                level.addFreshEntity(entityDivineSpirit);//小霊を出現させる
            }

            //スニークなら探知メッセージを表示しない
            if (!player.isShiftKeyDown()) {
                dx = nearEntitys[i].getX() - player.getX();//Entityと使用者の各要素の距離を保存
                dz = nearEntitys[i].getZ() - player.getZ();

                angle2 = ((float) Math.atan2(dx, dz) / 3.141593F * 180F) % 360F;//平面上の角度を求める
                // 原作のまま（angle2 ではなく angle を更新している。TODO K3-review 参照）
                if (angle2 < -180F) {
                    angle += 360F;
                }
                if (angle2 > 180F) {
                    angle -= 360F;
                }
                angle2 -= angle;

                if (angle2 < -135F || angle2 > 135F) {
                    strPos = Component.translatable("thKaguya.back").getString();
                } else if (angle2 < -45F) {
                    strPos = Component.translatable("thKaguya.right").getString();
                } else if (angle2 < 45F) {
                    strPos = Component.translatable("thKaguya.front").getString();
                } else {
                    strPos = Component.translatable("thKaguya.left").getString();
                }

                if (player.getY() > nearEntitys[i].getY() + 3.0D) {
                    strUpDown = Component.translatable("thKaguya.down").getString();
                } else if (player.getY() < nearEntitys[i].getY() - 3.0D) {
                    strUpDown = Component.translatable("thKaguya.up").getString();
                } else {
                    strUpDown = "";
                }

                if (nearEntitys[i] instanceof Monster) {
                    strColor = "\u00a74";
                } else if (nearEntitys[i] instanceof Animal) {
                    strColor = "\u00a7a";
                } else {
                    strColor = "\u00a73";
                }

                num = i + 1;
                strNum = num + ". ";
                if (num != 10) {
                    strNum = " " + strNum;
                }

                if (nearEntitys[i] instanceof Player nearEntityPlayer) {
                    str = strColor + strNum + nearEntityPlayer.getDisplayName().getString() + " " + strPos + " " + strUpDown + "  " + (int) nearDistance[i] + "m" + "  ";
                } else if (nearEntitys[i] instanceof LivingEntity nearEntityLiving) {
                    str = strColor + strNum + nearEntityLiving.getName().getString() + " " + strPos + " " + strUpDown + "  " + (int) nearDistance[i] + "m" + "  ";
                }

                if (!level.isClientSide) {
                    player.sendSystemMessage(Component.literal(str));
                }
            }
            i++;
        }

        //探知した数だけ耐久を減らす
        if (!player.isCreative()) {
            stack.setDamageValue(stack.getDamageValue() + i);
        }
    }

    /** エンチャント不可。 */
    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    /** 1.7.10 の {@code isBookEnchantable} 相当。 */
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }
}