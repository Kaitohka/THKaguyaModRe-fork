package thkaguyamod.init;

import java.util.function.Supplier;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * Custom tool tiers, ported from the 1.7.10 {@code EnumHelper.addToolMaterial} calls in
 * {@code THKaguyaItems}. Parameters: (harvest level 0, maxUses, efficiency, damage, enchantability).
 */
public final class ModTiers {

    public static final Tier ICICLE = new SimpleTier(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 30, 0.0F, 2.0F, 0,
            () -> Ingredient.of(Items.ICE));

    public static final Tier LAEVATEINN = new SimpleTier(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 95, 9.0F, 3.0F, 0,
            () -> Ingredient.of(Items.MAGMA_CREAM));

    public static final Tier HAKUROUKEN = new SimpleTier(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 100, 0.0F, 0.0F, 0,
            () -> Ingredient.of(Items.IRON_INGOT));

    public static final Tier ROUKANKEN = new SimpleTier(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 251, 4.0F, 3.0F, 0,
            () -> Ingredient.of(Items.IRON_INGOT));

    public static final Tier HISOU = new SimpleTier(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 200, 4.0F, 0.0F, 0,
            () -> Ingredient.of(ModItems.HEAVENLY_PEACH.get()));

    /** Rod of Remorse (kaigo): negative damage bonus, it is not a weapon. */
    public static final Tier KAIGO = new SimpleTier(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 1, 0.0F, -4.0F, 0,
            Ingredient::of);

    /** Aliases so generated code can reference vanilla tiers through this class. */
    public static final Tier WOOD = Tiers.WOOD;
    public static final Tier IRON = Tiers.IRON;

    private ModTiers() {}
}
