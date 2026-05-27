package com.xciel.steamturbine.registrate;

import com.xciel.steamturbine.AllBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.xciel.steamturbine.SteamTurbine.MOD_ID;

public class STCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(MOD_ID, () ->
            CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> AllBlocks.STEAM_TURBINE.get().asItem().getDefaultInstance())
                    .title(Component.translatable("itemGroup." + MOD_ID + ".main"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(AllBlocks.STEAM_BOILER.get());
                        output.accept(AllBlocks.STEAM_COMPRESSOR.get());
                        output.accept(AllBlocks.PRESSURE_PIPE.get());
                        output.accept(AllBlocks.STEAM_TURBINE.get());
                        output.accept(AllBlocks.TURBINE_SHAFT.get());
                        output.accept(AllBlocks.PRESSURE_GAUGE.get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}