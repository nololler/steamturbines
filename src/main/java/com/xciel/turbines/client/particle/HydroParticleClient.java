package com.xciel.turbines.client.particle;

import com.xciel.turbines.Turbines;
import com.xciel.turbines.registrate.STParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = Turbines.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HydroParticleClient {

    private HydroParticleClient() {}

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(STParticleTypes.BLUE_HYDRO_POOF.get(), BlueHydroPoofParticle.Factory::new);
    }
}
