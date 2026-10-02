package thkaguyamod.init;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import thkaguyamod.THKaguyaMod;

/**
 * Armor materials, ported from the 1.7.10 {@code EnumHelper.addArmorMaterial} calls.
 * 1.7.10 defense arrays are in boots/leggings/chestplate/helmet order.
 *
 * <p>Note: NeoForge's ArmorMaterial has no durability field; per-item durability lives
 * in the item properties (see ModItems). The 1.7.10 fire-rat robe was unbreakable
 * (maxDamage 0), which maps naturally to an item without durability.
 */
public final class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, THKaguyaMod.MOD_ID);

    /** Robe of the Fire Rat (chestplate). */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HINEZUMI_ROBE = ARMOR_MATERIALS.register(
            "hinezumi_robe", () -> new ArmorMaterial(
                    defense(1, 2, 2, 2), 0,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    Ingredient::of, List.of(layer("hinezumi")), 0.0F, 0.0F));

    /** Marisa's Hat (helmet). */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MARISA = ARMOR_MATERIALS.register(
            "marisa", () -> new ArmorMaterial(
                    defense(1, 1, 1, 1), 0,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    Ingredient::of, List.of(layer("marisa")), 0.0F, 0.0F));

    /** Kappa Cap (helmet). */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> KAPPA = ARMOR_MATERIALS.register(
            "kappa", () -> new ArmorMaterial(
                    defense(1, 2, 2, 2), 0,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    Ingredient::of, List.of(layer("hinezumi")), 0.0F, 0.0F));

    /** Suwako's Hat (helmet). */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SUWAKO = ARMOR_MATERIALS.register(
            "suwako", () -> new ArmorMaterial(
                    defense(1, 1, 1, 1), 0,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    Ingredient::of, List.of(layer("suwako")), 0.0F, 0.0F));

    private static EnumMap<ArmorItem.Type, Integer> defense(int boots, int leggings, int chestplate, int helmet) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.LEGGINGS, leggings);
        map.put(ArmorItem.Type.CHESTPLATE, chestplate);
        map.put(ArmorItem.Type.HELMET, helmet);
        return map;
    }

    private static ArmorMaterial.Layer layer(String name) {
        return new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, name));
    }

    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
    }

    private ModArmorMaterials() {}
}
