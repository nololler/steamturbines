package com.xciel.turbines.registrate;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.xciel.turbines.Turbines.MOD_ID;

public class STCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(MOD_ID, () ->
            CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> STBlocks.STEAM_TURBINE.get().asItem().getDefaultInstance())
                    .title(Component.translatable("itemGroup." + MOD_ID + ".main"))
                    .displayItems((itemDisplayParameters, output) -> {
                        // Blocks
                        output.accept(STBlocks.STEAM_BOILER.get());
                        output.accept(STBlocks.PRESSURE_PIPE.get());
                        output.accept(STBlocks.STEAM_PUMP.get());
                        output.accept(STBlocks.STEAM_COMPRESSOR.get());
                        output.accept(STBlocks.STEAM_TURBINE.get());
                        output.accept(STBlocks.TURBINE_SHAFT.get());
                        output.accept(STBlocks.STEAM_JET_THRUSTER.get());
                        output.accept(STBlocks.LAVA_DUCT_TURBINE.get());
                        output.accept(STBlocks.LAVA_DUCT_SHAFT.get());
                        output.accept(STBlocks.DIRECTIONAL_ANALOG_GEARSHIFT.get());
                        output.accept(STBlocks.NETWORK_DIAGNOSER.get());
                        // Items
                        output.accept(STItems.TURBINE_BLADE.get());
                        output.accept(STItems.TURBINE_FAN.get());
                        output.accept(STItems.CORE_OF_THE_HEARTH.get());
                        output.accept(STItems.PRESSURED_IRON_INGOT.get());
                        output.accept(STFluids.PEARLESCENT_DEW.getBucket().get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
