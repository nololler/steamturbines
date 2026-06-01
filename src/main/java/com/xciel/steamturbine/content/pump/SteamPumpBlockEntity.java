package com.xciel.steamturbine.content.pump;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.SteamType;
import com.xciel.steamturbine.steam.transfer.ISteamConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamTransport;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SteamPumpBlockEntity extends KineticBlockEntity {
    private static final float MAX_TARGET_RATE = 100f;
    private static final int SCROLL_MIN = 0;
    private static final int SCROLL_MAX = 100;

    private final EnumMap<Direction, Boolean> connections = new EnumMap<>(Direction.class);
    private float targetPullRate = 10f;
    private float totalPulledSteam = 0f;
    private float lastPulledSteam = 0f;
    private Direction pullDirection;
    private Direction pushDirection;

    public SteamPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
        for (Direction dir : Direction.values()) {
            connections.put(dir, false);
        }
        pullDirection = Direction.DOWN;
        pushDirection = Direction.UP;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        ScrollValueBehaviour scrollBehaviour = new ScrollValueBehaviour(
            net.minecraft.network.chat.Component.literal("Pump Rate"),
            this,
            new PumpScrollSlot()
        );
        scrollBehaviour.between(SCROLL_MIN, SCROLL_MAX);
        scrollBehaviour.withCallback(this::onScrollValueChanged);
        scrollBehaviour.withFormatter(v -> v + "%");
        scrollBehaviour.value = (int) targetPullRate;
        behaviours.add(scrollBehaviour);
    }

    private static class PumpScrollSlot extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.5);
        }

        @Override
        protected boolean isSideActive(net.minecraft.world.level.block.state.BlockState state, Direction direction) {
            Direction facing = state.getValue(SteamPumpBlock.FACING);
            boolean alongFirst = state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE);

            if (facing == Direction.UP || facing == Direction.DOWN) {
                return alongFirst
                    ? direction == Direction.NORTH || direction == Direction.SOUTH
                    : direction == Direction.EAST || direction == Direction.WEST;
            }

            return direction == Direction.UP || direction == Direction.DOWN;
        }

        @Override
        public void rotate(net.minecraft.world.level.LevelAccessor level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, PoseStack ms) {
            Direction facing = state.getValue(SteamPumpBlock.FACING);
            boolean alongFirst = state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE);

            if (facing.getAxis().isVertical()) {
                float yRot = AngleHelper.horizontalAngle(facing) + 180;
                float xRot = facing == Direction.UP ? 90 : 270;

                if (alongFirst) {
                    yRot += 90;
                }

                TransformStack.of(ms)
                    .rotateYDegrees(yRot)
                    .rotateXDegrees(xRot);
                return;
            }

            super.rotate(level, pos, state, ms);
        }
    }

    private void onScrollValueChanged(int value) {
        targetPullRate = value;
        setChanged();
        sendData();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        updateConnectionStates();
        updatePullPushDirections();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) return;
        pullFromPipes();
        pushToOutput();
    }

    private void updatePullPushDirections() {
        Direction facing = getBlockState().getValue(SteamPumpBlock.FACING);
        pushDirection = facing;
        pullDirection = facing.getOpposite();
    }

    private void updateConnectionStates() {
        if (level == null) return;

        boolean changed = false;
        for (Direction dir : Direction.values()) {
            boolean wasConnected = connections.getOrDefault(dir, false);
            boolean isConnected = hasNeighborAcceptingSteam(dir);

            if (isConnected != wasConnected) {
                connections.put(dir, isConnected);
                changed = true;
            }
        }
        if (changed) setChanged();
    }

    private boolean hasNeighborAcceptingSteam(Direction dir) {
        BlockPos neighborPos = worldPosition.relative(dir);
        if (!level.isLoaded(neighborPos)) return false;

        BlockState neighborState = level.getBlockState(neighborPos);
        var neighborBE = level.getBlockEntity(neighborPos);

        if (neighborState.getBlock() instanceof PressurizedPipeBlock) return true;

        if (neighborBE instanceof ISteamTransport transport) {
            return transport.canConnect(dir.getOpposite());
        }

        return false;
    }

    private void pullFromPipes() {
        if (targetPullRate <= 0) {
            lastPulledSteam = 0f;
            totalPulledSteam = 0f;
            return;
        }

        float actualPull = 0f;
        float remainingToPull = targetPullRate;

        for (Direction dir : Direction.values()) {
            if (dir == pushDirection) continue;

            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            var neighborBE = level.getBlockEntity(neighborPos);
            if (!(neighborBE instanceof ISteamTransport transport)) continue;
            if (!transport.canConnect(dir.getOpposite())) continue;

            float available = transport.getFlowRate(dir.getOpposite());
            if (available <= 0) continue;

            float toPull = Math.min(remainingToPull, available);
            if (toPull > 0.01f) {
                transport.pullSteam(dir.getOpposite(), toPull);
                actualPull += toPull;
                remainingToPull -= toPull;
            }

            if (remainingToPull <= 0.01f) break;
        }

        totalPulledSteam += actualPull;
        lastPulledSteam = actualPull;
        setChanged();
        sendData();
    }

    private void pushToOutput() {
        if (lastPulledSteam <= 0) return;

        BlockPos outputPos = worldPosition.relative(pushDirection);
        if (!level.isLoaded(outputPos)) return;

        var neighborBE = level.getBlockEntity(outputPos);
        SteamData toPush = SteamData.of(lastPulledSteam, SteamType.REGULAR, 1f, 1f, lastPulledSteam);

        if (neighborBE instanceof PressurizedPipeBlockEntity pipe) {
            pipe.receiveSteam(pushDirection.getOpposite(), toPush);
        } else if (neighborBE instanceof ISteamTransport transport) {
            if (transport.canConnect(pushDirection.getOpposite())) {
                transport.pushSteam(pushDirection.getOpposite(), toPush);
            }
        } else if (neighborBE instanceof ISteamConsumer consumer) {
            if (consumer.canReceive(pushDirection.getOpposite())) {
                consumer.receiveSteam(pushDirection.getOpposite(), toPush);
            }
        }
    }

    public float getTargetPullRate() {
        return targetPullRate;
    }

    public void setTargetPullRate(float rate) {
        targetPullRate = Math.max(0, Math.min(rate, MAX_TARGET_RATE));
    }

    public float getLastPulledSteam() {
        return lastPulledSteam;
    }

    public Direction getPullDirection() {
        return pullDirection;
    }

    public Direction getPushDirection() {
        return pushDirection;
    }

    public Map<Direction, Boolean> getConnections() {
        return connections;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        targetPullRate = tag.getFloat("TargetPullRate");
        totalPulledSteam = tag.getFloat("TotalPulledSteam");
        if (clientPacket) {
            lastPulledSteam = tag.getFloat("LastPulledSteam");
        }
        for (Direction dir : Direction.values()) {
            connections.put(dir, tag.getBoolean("conn_" + dir.getName()));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("TargetPullRate", targetPullRate);
        tag.putFloat("TotalPulledSteam", totalPulledSteam);
        tag.putFloat("LastPulledSteam", lastPulledSteam);
        for (Direction dir : Direction.values()) {
            tag.putBoolean("conn_" + dir.getName(), connections.getOrDefault(dir, false));
        }
    }
}