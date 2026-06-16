package com.xciel.steamturbine.registrate;

import com.simibubi.create.foundation.data.SharedProperties;
import com.xciel.steamturbine.content.boiler.SteamBoilerBlock;
import com.xciel.steamturbine.content.compressor.SteamCompressorBlock;
import com.xciel.steamturbine.content.nd.NetworkDiagnoserBlock;
import com.xciel.steamturbine.content.pump.SteamPumpBlock;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock;
import com.xciel.steamturbine.content.shaft.TurbineShaftBlock;
import com.xciel.steamturbine.content.shaft.LavaDuctShaftBlock;
import com.xciel.steamturbine.content.dag.DirectionalAnalogGearshiftBlock;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlock;
import com.xciel.steamturbine.content.turbine.LavaDuctTurbineBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.tags.BlockTags;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import static com.xciel.steamturbine.SteamTurbine.REGISTRATE;

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

    public static void register() {}
}
