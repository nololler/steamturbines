package com.xciel.steamturbine;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.xciel.steamturbine.client.SteamTurbineClient;
import com.xciel.steamturbine.content.boiler.LiquidFuelManager;
import com.xciel.steamturbine.content.boiler.SteamBoilerArmInteractionPointType;
import com.xciel.steamturbine.content.shaft.LavaDuctShaftBlock;
import com.xciel.steamturbine.network.SteamTurbinePackets;
import com.xciel.steamturbine.registrate.*;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.item.KineticStats;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SteamTurbine.MOD_ID)
public class SteamTurbine {
    public static final String MOD_ID = "steamturbine";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
            .setTooltipModifierFactory(item ->
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
            );

    public SteamTurbine(IEventBus eventBus, ModContainer modContainer) {
        REGISTRATE.registerEventListeners(eventBus);

        eventBus.addListener(this::setup);
        eventBus.addListener(this::doClientStuff);
        eventBus.addListener(this::registerCapabilities);

        STSounds.register(eventBus);
        STBlocks.register();
        STBlockEntityTypes.register();
        STItems.register();
        STCreativeTabs.register(eventBus);

        Registry.register(
            CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE,
            rl("steam_boiler"),
            SteamBoilerArmInteractionPointType.getInstance()
        );

        NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, event ->
            event.addListener(new LiquidFuelManager())
        );

        LOGGER.info("Steam Turbine Initialized!");
    }

    private void setup(final FMLCommonSetupEvent event) {
        SteamTurbinePackets.register();
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        SteamTurbineClient.addClientListeners(event);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            AllBlockEntityTypes.STEAM_BOILER.get(),
            (be, context) -> be.getFluidHandler()
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            AllBlockEntityTypes.STEAM_BOILER.get(),
            (be, context) -> be.getItemHandler()
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            AllBlockEntityTypes.LAVA_DUCT_SHAFT.get(),
            (be, context) -> {
                if (context == null || LavaDuctShaftBlock.hasPipeTowards(be.getLevel(), be.getBlockPos(), be.getBlockState(), context))
                    return be.getFluidHandler();
                return null;
            }
        );
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}