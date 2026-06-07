package com.xciel.steamturbine.content.boilerTurbine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class BoilerTurbineBlockEntity extends GeneratingKineticBlockEntity implements ISteamEndpoint {

    private static final float BASE_SPEED = 32.0f;
    private static final float MAX_SPEED = 256.0f;

    public BoilerTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public float getGeneratedSpeed() {
        return MAX_SPEED;
    }

    @Override
    public float calculateAddedStressCapacity() {
        return 1024.0f;
    }

    @Override
    public boolean canConnect(Direction direction) {
        return direction == Direction.DOWN;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
    }
}