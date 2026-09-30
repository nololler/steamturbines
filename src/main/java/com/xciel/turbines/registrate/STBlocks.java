package com.xciel.turbines.registrate;

import com.simibubi.create.foundation.data.SharedProperties;
import com.xciel.turbines.content.boiler.SteamBoilerBlock;
import com.xciel.turbines.content.compressor.SteamCompressorBlock;
import com.xciel.turbines.content.ejector.SteamEjectorBlock;
import com.xciel.turbines.content.nd.NetworkDiagnoserBlock;
import com.xciel.turbines.content.pump.SteamPumpBlock;
import com.xciel.turbines.content.transport.pipe.PressurizedPipeBlock;
import com.xciel.turbines.content.shaft.TurbineShaftBlock;
import com.xciel.turbines.content.shaft.LavaDuctShaftBlock;
import com.xciel.turbines.content.shaft.HydroTurbineShaftBlock;
import com.xciel.turbines.content.dag.DirectionalAnalogGearshiftBlock;
import com.xciel.turbines.content.green_nentia_block.GreenNentiaBlock;
import com.xciel.turbines.content.large_turbine.LargeTurbineBlock;
import com.xciel.turbines.content.open_air_turbine.OpenAirTurbineBlock;
import com.xciel.turbines.content.turbine.SteamTurbineBlock;
import com.xciel.turbines.content.turbine.LavaDuctTurbineBlock;
import com.xciel.turbines.content.sjth.SteamJetThrusterBlock;
import com.xciel.turbines.content.hydro_turbine.HydroTurbineBlock;
import com.xciel.turbines.content.hydro_turbine.HydroTurbineBlockItem;
import com.xciel.turbines.content.hydro_turbine.HydroTurbinePartBlock;
import com.xciel.turbines.content.hydro_turbine.HydroTurbineIOBlock;
import com.xciel.turbines.content.reinforced_glass.ReinforcedGlassBlock;
import com.xciel.turbines.content.pressured_sand.PressuredSandBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.tags.BlockTags;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import static com.xciel.turbines.Turbines.REGISTRATE;

public class STBlocks {

    public static final BlockEntry<SteamBoilerBlock> STEAM_BOILER = REGISTRATE.block("steam_boiler", SteamBoilerBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK).strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<SteamCompressorBlock> STEAM_COMPRESSOR = REGISTRATE.block("steam_compressor", SteamCompressorBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK).strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .onRegister(b -> com.simibubi.create.api.stress.BlockStressValues.IMPACTS.register(b, () -> 32.0))
            .item()
            .build()
            .register();

    public static final BlockEntry<PressurizedPipeBlock> PRESSURE_PIPE = REGISTRATE.block("pressure_pipe", PressurizedPipeBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK).strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<SteamTurbineBlock> STEAM_TURBINE = REGISTRATE.block("steam_turbine", SteamTurbineBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<TurbineShaftBlock> TURBINE_SHAFT = REGISTRATE.block("turbine_shaft", TurbineShaftBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .onRegister(b -> com.simibubi.create.api.stress.BlockStressValues.CAPACITIES.register(b, () -> 256.0))
            .item()
            .build()
            .register();

    public static final BlockEntry<SteamPumpBlock> STEAM_PUMP = REGISTRATE.block("steam_pump", SteamPumpBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .onRegister(b -> com.simibubi.create.api.stress.BlockStressValues.IMPACTS.register(b, () -> 32.0))
            .item()
            .build()
            .register();

    public static final BlockEntry<LavaDuctTurbineBlock> LAVA_DUCT_TURBINE = REGISTRATE.block("lava_duct_turbine", LavaDuctTurbineBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<LavaDuctShaftBlock> LAVA_DUCT_SHAFT = REGISTRATE.block("lava_duct_shaft", LavaDuctShaftBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .onRegister(b -> com.simibubi.create.api.stress.BlockStressValues.CAPACITIES.register(b, () -> 256.0))
            .item()
            .build()
            .register();

    public static final BlockEntry<DirectionalAnalogGearshiftBlock> DIRECTIONAL_ANALOG_GEARSHIFT = REGISTRATE.block("directional_analog_gearshift", DirectionalAnalogGearshiftBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .properties(p -> p.noOcclusion())
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<NetworkDiagnoserBlock> NETWORK_DIAGNOSER = REGISTRATE.block("network_diagnoser", NetworkDiagnoserBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .properties(p -> p.noOcclusion())
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .onRegister(b -> com.simibubi.create.api.stress.BlockStressValues.IMPACTS.register(b, () -> 0.0))
            .onRegister(b -> com.simibubi.create.api.stress.BlockStressValues.CAPACITIES.register(b, () -> 0.0))
            .item()
            .build()
            .register();

    public static final BlockEntry<SteamJetThrusterBlock> STEAM_JET_THRUSTER = REGISTRATE.block("steam_jet_thruster", SteamJetThrusterBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK).strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .properties(p -> p.noOcclusion())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<SteamEjectorBlock> STEAM_EJECTOR = REGISTRATE.block("steam_ejector", SteamEjectorBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK).strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .properties(p -> p.noOcclusion())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<GreenNentiaBlock> GREEN_NENTIA_BLOCK = REGISTRATE.block("green_nentia_block", GreenNentiaBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<OpenAirTurbineBlock> OPEN_AIR_TURBINE = REGISTRATE.block("open_air_turbine", OpenAirTurbineBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK).strength(3.0f, 6.0f).requiresCorrectToolForDrops())
            .properties(p -> p.noOcclusion())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .onRegister(b -> com.simibubi.create.api.stress.BlockStressValues.CAPACITIES.register(b, () -> 256.0))
            .item()
            .build()
            .register();

    public static final BlockEntry<LargeTurbineBlock> LARGE_TURBINE = REGISTRATE.block("large_turbine", LargeTurbineBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK).strength(5.0f, 6.0f).requiresCorrectToolForDrops())
            .properties(p -> p.noOcclusion())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<ReinforcedGlassBlock> REINFORCED_GLASS = REGISTRATE.block("reinforced_glass", ReinforcedGlassBlock::new)
            .initialProperties(() -> Blocks.GLASS)
            .properties(p -> p.sound(SoundType.GLASS).strength(1.5f, 6.0f).requiresCorrectToolForDrops().noOcclusion())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<PressuredSandBlock> PRESSURED_SAND = REGISTRATE.block("pressured_sand", PressuredSandBlock::new)
            .initialProperties(() -> Blocks.SAND)
            .properties(p -> p.sound(SoundType.SAND).strength(0.5f))
            .tag(BlockTags.SAND, BlockTags.MINEABLE_WITH_SHOVEL)
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<HydroTurbineShaftBlock> HYDRO_TURBINE_SHAFT = REGISTRATE.block("hydro_shaft", HydroTurbineShaftBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK).strength(3.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<HydroTurbineIOBlock> HYDRO_TURBINE_IO = REGISTRATE.block("hydro_turbine_io", HydroTurbineIOBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK).strength(3.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item()
            .build()
            .register();

    public static final BlockEntry<HydroTurbinePartBlock> HYDRO_TURBINE_PART = REGISTRATE.block("hydro_turbine_part", HydroTurbinePartBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.noOcclusion().strength(0.5f, 0.5f))
            .register();

    public static final BlockEntry<HydroTurbineBlock> HYDRO_TURBINE = REGISTRATE.block("hydro_turbine", HydroTurbineBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK).strength(3.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .transform(pickaxeOnly())
            .loot((lt, b) -> lt.add(b, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(b))
                            .when(ExplosionCondition.survivesExplosion()))))
            .item(HydroTurbineBlockItem::new)
            .build()
            .register();

    public static void register() {}
}
