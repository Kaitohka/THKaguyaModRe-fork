package thkaguyamod.item;

import java.util.List;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import thkaguyamod.entity.item.EntityCursedDecoyDoll;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModEntities;

/**
 * 呪いのデコイ人形 — ported from 1.7.10 {@code thKaguyaMod.item.ItemCursedDecoyDoll}.
 *
 * <p>右クリックでデコイ人形（{@link EntityCursedDecoyDoll}）を出現させ、耐久を 1 消費する。
 * 周囲 40 ブロック以内に既にデコイ人形がいる場合は使用できない。
 * 耐久が尽きると、記録しておいた元アイテム（{@code OriginalItemID}）と元 damage
 * （{@code OriginalDamage}）へ復元される。
 *
 * <p>TODO K3-review (blocking): 元アイテムの復元が未実装。1.7.10 は NBT 整数
 * {@code OriginalItemID}（数値アイテム ID）から {@code Item.getItemById(originID)} で
 * アイテムを引き当てていたが、1.21.1 は数値 ID が廃止されている。{@code OriginalItemID} に
 * 対応するデータコンポーネント（アイテムホルダ）が未確定のため、本移植では復元処理を
 * 行わず durability のみ消費する。復元先の決定後、
 * {@code ModDataComponents} に item 用コンポーネントを足して差し替えること。
 *
 * <p>TODO K3-review: 1.7.10 は ItemStack の戻り値経由でアイテムを差し替えていた。
 * 1.21.1 では {@code player.setItemInHand} での明示的な書き戻しが必要
 * （{@link ItemGapFoldingUmbrella} と同じ扱い）。復元処理を実装する際に併せて対応すること。
 */
public class ItemCursedDecoyDoll extends Item {

    public ItemCursedDecoyDoll(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右クリックを押した瞬間の処理（1.7.10 {@code onItemRightClick}）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        //周囲のEntityを取得
        List<EntityCursedDecoyDoll> list = level.getEntitiesOfClass(EntityCursedDecoyDoll.class,
                player.getBoundingBox().inflate(40.0D, 40.0D, 40.0D));
        for (EntityCursedDecoyDoll existing : list) {
            // すでに周囲に呪いのデコイ人形が出現している場合、使用は失敗する
            return InteractionResultHolder.pass(stack);
        }

        EntityCursedDecoyDoll decoy = new EntityCursedDecoyDoll(ModEntities.CURSED_DECOY_DOLL.get(), level, player);
        if (!level.isClientSide) {
            level.addFreshEntity(decoy);// 呪いのデコイ人形を出現させる
        }

        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            // TODO K3-review: 1.7.10 は NBT "OriginalItemID"（数値アイテム ID）から元アイテムへ
            //   復元していた。1.21.1 は数値 ID が無く、対応するコンポーネントも未定義のため未実装。
            //   復元先が決まったら:
            //     Item original = stack.get(ModDataComponents.ORIGINAL_ITEM.get());
            //     int originalDamage = stack.getOrDefault(ModDataComponents.ORIGINAL_DAMAGE.get(), 0);
            //     player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(original, 1, ...));
            stack.getOrDefault(ModDataComponents.ORIGINAL_DAMAGE.get(), 0);
        }

        return InteractionResultHolder.success(stack);
    }

    /** アイテムを発光させる（1.7.10 の {@code hasEffect} 相当）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}