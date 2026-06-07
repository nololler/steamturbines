package com.xciel.steamturbine.content.boilerTurbine;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.ref.WeakReference;
import java.util.List;

public class ShaftCasingBlockEntity extends KineticBlockEntity {

    private WeakReference<BoilerTurbineBlockEntity> parent;

    public ShaftCasingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        findParent();
    }

    private void findParent() {
        BlockPos below = worldPosition.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.getBlock() instanceof BoilerTurbineBlock) {
            BoilerTurbineBlockEntity parentBE = (BoilerTurbineBlockEntity) level.getBlockEntity(below);
            if (parentBE != null) {
                parent = new WeakReference<>(parentBE);
            }
        }
    }

    public BoilerTurbineBlockEntity getParent() {
        BoilerTurbineBlockEntity be = parent != null ? parent.get() : null;
        if (be == null || be.isRemoved()) {
            findParent();
            be = parent != null ? parent.get() : null;
        }
        return be;
    }

    public float getSpeed() {
        BoilerTurbineBlockEntity parentBE = getParent();
        if (parentBE == null || !isValidShaft()) {
            return 0;
        }
        return parentBE.getShaftSpeed();
    }

    private boolean isValidShaft() {
        BlockState shaftState = level.getBlockState(worldPosition.relative(Direction.UP));
        return AllBlocks.SHAFT.has(shaftState) || AllBlocks.POWERED_SHAFT.has(shaftState);
    }

    public boolean isShaftPresent() {
        return isValidShaft();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return false;
    }

    public void onNeighborChanged() {
        setChanged();
        if (!level.isClientSide) {
            if (!isValidShaft()) {
                BoilerTurbineBlockEntity parentBE = getParent();
                if (parentBE != null) {
                    parentBE.onShaftRemovedFromCasing();
                }
            }
            sendData();
        }
    }
}