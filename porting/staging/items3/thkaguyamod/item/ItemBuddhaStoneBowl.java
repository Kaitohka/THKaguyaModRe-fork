package thkaguyamod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 仏の御石の鉢 (Buddha's Stone Bowl) — ported from 1.7.10 {@code thKaguyaMod.item.ItemBuddhaStoneBowl}.
 *
 * <p>石の万能ツール（剣／ツルハシ／シャベル／斧）。1.7.10 は {@code ItemTool} を継承し、
 * {@code func_150897_b} と {@code getDigSpeed} を上書きしていた。1.21.1 の {@code ItemTool} は無いため、
 * 同じ効果を <b>{@code DataComponents.TOOL} コンポーネント経由</b>で与える
 * （{@code Tiers.STONE.createToolProperties(BlockTags.MINEABLE_WITH_PICKAXE)}）。
 * これにより {@code DiggerItem} を継承せず、コンストラクタは {@code (Item.Properties)} のままで、
 * 登録側（{@code ModItems} の {@code Item::new}）を変更する必要がない。
 *
 * <p>採取可否の対応関係（{@code func_150897_b} の再現）:
 * <ul>
 *   <li>石・アンビル・鉄ブロックなど {@code mineable/pickaxe} にあるブロック → 採取可（旧 Material.rock / iron / anvil）</li>
 *   <li>金・ダイヤ・エメラルド・レッドストーン・ラピス・黒曜石 → 採取不可
 *       （{@code incorrect_for_stone_tool} に含まれるため。Stone の採取レベル相当）</li>
 *   <li>ピッケーで採れないブロック（土・木など） → 採取不可（1.7.10 も false だった）</li>
 * </ul>
 *
 * <p>TODO K3-review: 登録側の {@code Item.Properties()} には
 * {@code .attributes(net.minecraft.world.item.DiggerItem.createAttributes(Tiers.STONE, 4.0F, -2.8F))}
 * を追加すると、旧版の攻撃力（ItemTool の 4.0F + ToolMaterial.STONE の 1.0F）を再現できる。
 * 攻撃速度に相当する概念が 1.7.10 に無いため -2.8F（バニラのツルハシ相当）を採用している。
 * 現状の登録（{@code Item.Properties()} のみ）では素手相当の攻撃力になる。
 */
public class ItemBuddhaStoneBowl extends Item {

    public ItemBuddhaStoneBowl(Item.Properties properties) {
        super(properties.component(DataComponents.TOOL,
                Tiers.STONE.createToolProperties(BlockTags.MINEABLE_WITH_PICKAXE)));
    }

    /** アイテムを発光させる（1.7.10 {@code hasEffect} は常に true）。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /**
     * ブロックの壊し易さ。1.7.10 は常に 4F を返していた（{@code ToolMaterial.STONE} の efficiency 4.0F）。
     * 1.21.1 のコンポーネントは {@code mineable/pickaxe} のブロックにだけ 4F を与えるので、
     * 旧版に合わせて常に 4F に上書きする。
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 4.0F;
    }

    /**
     * Entity に当たったときの処理（1.7.10 {@code hitEntity}）。
     * 旧版は true を返すだけで耐久を消費しなかったため、{@code DiggerItem.postHurtEnemy} 相当の
     * 耐久消費は行わない。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    /**
     * ブロックを破壊したときの処理（1.7.10 {@code onBlockDestroyed}）。
     * 旧版は true を返すだけで耐久を消費しなかった（1.21.1 の既定実装はここで耐久を減らすため上書きする）。
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        return true;
    }

    /** エンチャントできない。できるとかなりのチートアイテム化する（旧 {@code getItemEnchantability} = 0）。 */
    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    /** 金床でエンチャント本を使えないようにする（旧 Forge の {@code isBookEnchantable}）。 */
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }
}