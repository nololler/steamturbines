package com.xciel.steamturbine.registrate;

import com.xciel.steamturbine.SteamTurbine;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class STSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, SteamTurbine.MOD_ID);

    public static final Supplier<SoundEvent> LAVA_DUCT_SHAFT = register("lava_duct_shaft");
    public static final Supplier<SoundEvent> STEAM_TURBINE = register("steam_turbine");
    public static final Supplier<SoundEvent> STEAM_THRUSTER = register("steam_thruster");

    private static Supplier<SoundEvent> register(String name) {
        ResourceLocation id = SteamTurbine.rl(name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
