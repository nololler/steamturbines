package com.xciel.steamturbine.content.turbine;

import com.xciel.steamturbine.data.PressureData;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SteamTurbineBlockEntity extends GeneratingKineticBlockEntity {
    private final PressureData pressureData;

    public SteamTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.pressureData = new PressureData();
    }

    public PressureData getPressureData() {
        return pressureData;
    }

    @Override
    public float getGeneratedSpeed() {
        return 0f;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        pressureData.loadFromNBT(tag);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        pressureData.saveToNBT(tag);
    }
}