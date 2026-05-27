package com.xciel.steamturbine.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = com.xciel.steamturbine.SteamTurbine.MOD_ID, value = Dist.CLIENT)
public class SteamTurbineClient {

    public static void addClientListeners(IEventBus eventBus) {
    }
}