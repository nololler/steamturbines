package com.xciel.steamturbine.content.shaft;

import com.xciel.steamturbine.steam.SteamData;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineShaftBlockEntity extends GeneratingKineticBlockEntity {
    private final SteamData steamData;

    public TurbineShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.steamData = new SteamData();
    }

    public SteamData getSteamData() {
        return steamData;
    }

    @Override
    public float getGeneratedSpeed() {
        return 0f;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        steamData.loadFromNBT(tag);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        steamData.saveToNBT(tag);
    }
}