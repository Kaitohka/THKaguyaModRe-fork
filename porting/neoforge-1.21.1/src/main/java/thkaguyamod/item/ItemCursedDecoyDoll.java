package thkaguyamod.item;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import thkaguyamod.entity.item.EntityCursedDecoyDoll;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.init.ModEntities;

/**
 * 呪いのデコイ人形 — ported from 1.7.10 {@code thKaguyaMod.item.ItemCursedDecoyDoll}.
 *
 * <p>右クリックでデコイ人形（{@link EntityCursedDecoyDoll}）を出現させ、耐久を 1 消費する。
 * 周囲 40 ブロック以内に既にデコイ人形がいる場合は使用できない。
 * 耐久が尽きると、記録しておいた元アイテム（{@link ModDataComponents#ORIGINAL_ITEM}）と
 * 元 damage（{@link ModDataComponents#ORIGINAL_DAMAGE}）へ復元される。
 *
 * <p>1.7.10 は数値アイテム ID {@code OriginalItemID} を NBT に書き、
 * {@code Item.getItemById(originID)} で引き当てていた。1.21.1 に数値 ID は無いため、
 * 同じ役割をレジストリ {@code Holder<Item>} のコンポーネントが担う（台账 P11）。
 *
 * <p>1.7.10 は {@code onItemRightClick} の戻り値でスタックを差し替えていたが、
 * 1.21.1 では戻り値が使われないため {@code player.setItemInHand} で書き戻す
 * （{@link ItemGapFoldingUmbrella} と同じ扱い）。
 *
 * <p>TODO K3-review: 1.7.10 でこのアイテムを産出していた万宝槌の変換は外部 MOD の
 * 「アリスの人形」を素材にしており、その MOD に 1.21.1 版が無いため変換ごと削除された
 * （台账 P8）。したがって {@code ORIGINAL_ITEM} を書く経路は現在のゲーム内に存在せず、
 * このアイテムはクリエイティブ入手のみ。復元ロジックは「将来変換を戻したとき」に
 * そのまま使えるよう実装済み（GameTest でコンポーネントを直接載せて検証している）。
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

        // 1.7.10 はここで itemStack.setItemDamage(damage + 1) を直接書いていた。
        // 1.21.1 の hurtAndBreak はサーバー専用で、耐久を使い切るとスタックを shrink する。
        // しかも shrink で空になった ItemStack は getComponents() が DataComponentMap.EMPTY を
        // 返すため、復元先のコンポーネントは**壊す前に**読んでおく必要がある
        // （空スタックからは ORIGINAL_ITEM を読めない）。
        // 創造モードでは hurtAndBreak が何もしないので、下の isEmpty() は成立しない（原作と同じ）。
        Holder<Item> original = stack.get(ModDataComponents.ORIGINAL_ITEM.get());
        int originalDamage = stack.getOrDefault(ModDataComponents.ORIGINAL_DAMAGE.get(), 0);

        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

        // 1.7.10: if (itemStack.getItemDamage() >= getMaxDamage()) { ...復元... }
        // 1.21.1 は耐久を使い切った時点でスタックが空になるので、同じ条件は isEmpty() で表す。
        if (stack.isEmpty()) {
            // 1.7.10 は NBT "OriginalItemID"（数値アイテム ID）から元アイテムを引き当てていた
            // （Item.getItemById(originID)）。1.21.1 に数値 ID は無く、代わりに
            // ORIGINAL_ITEM（アイテムのレジストリ Holder）が同じ役割を担う。
            if (original != null && !original.is(Items.AIR.builtInRegistryHolder())) {
                ItemStack restored = new ItemStack(original);
                restored.setDamageValue(originalDamage);
                // 1.7.10 は onItemRightClick の戻り値でスタックを差し替えていた。
                // 1.21.1 は戻り値が使われないため明示的に書き戻す（ItemGapFoldingUmbrella と同じ）。
                player.setItemInHand(hand, restored);
                return InteractionResultHolder.success(restored);
            }

            // コンポーネントが無い場合は復元しない。原作で OriginalItemID が書かれていない場合と同義で、
            // 元アイテムを特定できないため 1.7.10 も空の ItemStack を返していた。
        }

        return InteractionResultHolder.success(stack);
    }

    /** アイテムを発光させる（1.7.10 の {@code hasEffect} 相当）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}