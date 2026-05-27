package com.xciel.steamturbine.registrate;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

import static com.xciel.steamturbine.SteamTurbine.MOD_ID;
import static com.xciel.steamturbine.SteamTurbine.REGISTRATE;

public class STItems {

    public static final ItemEntry<Item> STEAM_BOILER = REGISTRATE.item("steam_boiler", Item::new)
            .register();

    public static final ItemEntry<Item> STEAM_COMPRESSOR = REGISTRATE.item("steam_compressor", Item::new)
            .register();

    public static final ItemEntry<Item> PRESSURE_PIPE = REGISTRATE.item("pressure_pipe", Item::new)
            .register();

    public static final ItemEntry<Item> STEAM_TURBINE = REGISTRATE.item("steam_turbine", Item::new)
            .register();

    public static final ItemEntry<Item> TURBINE_SHAFT = REGISTRATE.item("turbine_shaft", Item::new)
            .register();

    public static final ItemEntry<Item> PRESSURE_GAUGE = REGISTRATE.item("pressure_gauge", Item::new)
            .register();

    public static void register() {}
}