package thkaguyamod.item;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import thkaguyamod.danmaku.DanmakuConstants;
import thkaguyamod.danmaku.THKaguyaLib;
import thkaguyamod.init.ModDataComponents;
import thkaguyamod.registry.SpellCardRegistry;
import thkaguyamod.spellcard.THSpellCard;

/**
 * スペルカード (Spell Card) — ported from 1.7.10 {@code thKaguyaMod.item.ItemTHSpellCard}.
 *
 * <p>1.7.10 had one item whose metadata was the spell card id (0..34) and queried the declaration
 * cost through {@code Class.newInstance()} reflection. 1.21.1 registers one item per card
 * ({@code spell_card_<name>}), so the id is supplied by the constructor and the cost is read from
 * {@link SpellCardRegistry}'s {@link Supplier} factory.
 *
 * <p>Deliberately not ported (handled elsewhere):
 * <ul>
 *   <li>the two-layer icon ({@code requiresMultipleRenderPasses} /
 *       {@code getIconFromDamageForRenderPass} / {@code registerIcons}) — card face + level star are
 *       the two layers of the generated item model JSON;</li>
 *   <li>{@code getSubItems} and the metadata icon table — every card is its own registered item and
 *       {@code ModCreativeTabs} fills the spell card tab from {@code ModItems.SPELLCARD_TAB};</li>
 *   <li>{@code getUnlocalizedName(ItemStack)} — the per-item lang keys
 *       ({@code item.thkaguyamod.spell_card_<name>}) already carry the card name.</li>
 * </ul>
 */
public class ItemTHSpellCard extends Item {

    /** 早苗のスペルカードのID範囲（1.7.10 の meta 14..18）。 */
    private static final int SANAE_CARD_MIN = 14;
    private static final int SANAE_CARD_MAX = 18;

    /** このアイテムが表すスペルカードID（旧 meta 値）。 */
    private final int spellCardId;

    /**
     * @param properties  アイテムプロパティ（登録側で {@code stacksTo(1)} を設定する）
     * @param spellCardId スペルカードID（{@code ModSpellCards} の登録IDと同じ値）
     */
    public ItemTHSpellCard(Item.Properties properties, int spellCardId) {
        super(properties);
        this.spellCardId = spellCardId;
    }

    /** スペルカードID（旧 {@code getSpellCardID(ItemStack)} 相当）を返す。 */
    public int getSpellCardID() {
        return this.spellCardId;
    }

    /**
     * 宣言コストのレベル（1～5）を返す（旧 {@code getLevel(int spellCardID)}）。
     * ファクトリが無い／生成に失敗した場合は旧版と同じく 1 を返す。
     */
    public int getLevel() {
        Supplier<? extends THSpellCard> factory = SpellCardRegistry.getSpellCardFactory(this.spellCardId);
        if (factory != null) {
            THSpellCard spellCard = factory.get();
            if (spellCard != null) {
                return spellCard.getNeedLevel();
            }
        }
        return 1;
    }

    /**
     * 右クリックでスペルカードを宣言する（1.7.10 {@code onItemRightClick}）。
     *
     * <p>早苗のスペルカード (旧 meta 14..18) の「奇跡の力」（{@link ModDataComponents#CHARGE}）不足判定は
     * {@link THKaguyaLib#checkSpellCardDeclaration} 側に一本化した（1.7.10 と同じ位置）。
     * ここで重複判定は行わない。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 1.7.10: プレイヤーの宣言レベルは常に Normal(2)、firstAttack = true
        THKaguyaLib.checkSpellCardDeclaration(level, stack, player, this.spellCardId, DanmakuConstants.NORMAL, true);

        return InteractionResultHolder.success(stack);
    }

    /**
     * インベントリにある限り毎tick呼ばれる（1.7.10 {@code onUpdate}）。
     * NBT "Charge"（= {@link ModDataComponents#CHARGE}）を毎tick1ずつ減らし、1以下になったら
     * コンポーネントごと取り除く（旧 {@code removeTag("Charge")}）。
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        int miracleTime = stack.getOrDefault(ModDataComponents.CHARGE.get(), 0);
        if (miracleTime > 1) {
            stack.set(ModDataComponents.CHARGE.get(), miracleTime - 1);
        } else if (stack.has(ModDataComponents.CHARGE.get())) {
            stack.remove(ModDataComponents.CHARGE.get());
        }
    }

    /**
     * ツールチップ（1.7.10 {@code addInformation}）。
     * 早苗のスペルカードのみ、必要なレベルと残りチャージ（％）を表示する。
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("Lv." + getLevel() + " Spell Card"));

        int miracleTime = stack.getOrDefault(ModDataComponents.CHARGE.get(), 0);
        if (this.spellCardId >= SANAE_CARD_MIN && this.spellCardId <= SANAE_CARD_MAX) {
            int declarationLevel = 20 - ((14 - this.spellCardId) * 5);
            tooltip.add(Component.translatable("thKaguya.msg.sneSc1").append(String.valueOf(declarationLevel))
                    .append(Component.translatable("thKaguya.msg.sneSc2")).append(String.valueOf(this.spellCardId - 13))
                    .append(Component.translatable("thKaguya.msg.sneSc3")));
            tooltip.add(Component.translatable("thKaguya.msg.sneSc4").append((miracleTime / 10) + "%"));
        }
    }

    /**
     * エンチャントの輝き（1.7.10 {@code hasEffect}）。
     * 早苗のスペルカードがチャージされている間だけ光る。それ以外は光らない。
     */
    @Override
    public boolean isFoil(ItemStack stack) {
        if (this.spellCardId >= SANAE_CARD_MIN && this.spellCardId <= SANAE_CARD_MAX) {
            return stack.getOrDefault(ModDataComponents.CHARGE.get(), 0) > 0;
        }
        return false;
    }
}