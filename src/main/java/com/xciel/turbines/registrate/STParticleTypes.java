package com.xciel.turbines.registrate;

import com.xciel.turbines.Turbines;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class STParticleTypes {

    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, Turbines.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLUE_HYDRO_POOF =
        PARTICLE_TYPES.register("blue_hydro_poof", () -> new SimpleParticleType(false));

    private STParticleTypes() {}

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
