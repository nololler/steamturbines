package com.xciel.steamturbine.registrate;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.xciel.steamturbine.SteamTurbine;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import static com.xciel.steamturbine.SteamTurbine.MOD_ID;
import static com.xciel.steamturbine.SteamTurbine.REGISTRATE;

public class STItems {

    public static final ItemEntry<Item> TURBINE_BLADE = REGISTRATE.item("turbine_blade", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .register();

    public static final ItemEntry<Item> TURBINE_FAN = REGISTRATE.item("turbine_fan", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .register();

    public static final ItemEntry<Item> CORE_OF_THE_HEARTH = REGISTRATE.item("core_of_the_hearth", Item::new)
            .properties(p -> p.rarity(Rarity.RARE))
            .register();

    public static final ItemEntry<Item> PRESSURED_IRON_INGOT = REGISTRATE.item("pressured_iron_ingot", Item::new)
            .register();

    public static void register() {}
}