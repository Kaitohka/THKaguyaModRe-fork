package thkaguyamod.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import thkaguyamod.THKaguyaMod;

/**
 * Sound events. The 1.7.10 release jar declared these four events in sounds.json but
 * never shipped the .ogg files; the port registers the events and maps them to vanilla
 * sounds as placeholders (see assets/thkaguyamod/sounds.json).
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, THKaguyaMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MASTER_SPARK = register("masterspark");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELLCARD = register("spellcard");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUKIMA = register("sukima");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOWN = register("down");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(THKaguyaMod.MOD_ID, name)));
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }

    private ModSounds() {}
}
