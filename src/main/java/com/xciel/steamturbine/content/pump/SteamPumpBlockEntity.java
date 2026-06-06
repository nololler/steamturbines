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
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
import com.xciel.steamturbine.steam.transfer.ISteamProducer;
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

public class SteamPumpBlockEntity extends KineticBlockEntity implements ISteamEndpoint, ISteamConsumer {
    private static final float MAX_TARGET_RATE = 75f;
    private static final int SCROLL_MIN = 0;
    private static final int SCROLL_MAX = 75;
    private static final float BASE_PULL_RATE = 20.0f;
    private static final float SPEED_REFERENCE_RPM = 256f;

    private final EnumMap<Direction, Boolean> connections = new EnumMap<>(Direction.class);
    private float targetPullRate = 10f;
    private float totalPulledSteam = 0f;
    private float lastPulledSteam = 0f;
    private Direction pullDirection;
    private Direction pushDirection;
    private SteamType storedSteamType = SteamType.REGULAR;

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
        Direction facing = getBlockState().getValue(SteamPumpBlock.FACING);

        boolean isVertical = facing == Direction.UP || facing == Direction.DOWN;
        ValueBoxTransform slot = isVertical ? new PumpScrollSlotVerticalPrimary() : new PumpScrollSlotHorizontal();

        ScrollValueBehaviour scrollBehaviour = new ScrollValueBehaviour(
            net.minecraft.network.chat.Component.literal("Pump Rate"),
            this,
            slot
        );
        scrollBehaviour.between(SCROLL_MIN, SCROLL_MAX);
        scrollBehaviour.withCallback(this::onScrollValueChanged);
        scrollBehaviour.withFormatter(v -> v + "%");
        scrollBehaviour.value = (int) targetPullRate;
        behaviours.add(scrollBehaviour);

        if (isVertical) {
            ScrollValueBehaviour scrollBehaviour2 = new ScrollValueBehaviour(
                net.minecraft.network.chat.Component.literal("Pump Rate 2"),
                this,
                new PumpScrollSlotVerticalSecondary()
            );
            scrollBehaviour2.between(SCROLL_MIN, SCROLL_MAX);
            scrollBehaviour2.withCallback(this::onScrollValueChanged);
            scrollBehaviour2.withFormatter(v -> v + "%");
            scrollBehaviour2.value = (int) targetPullRate;
            behaviours.add(scrollBehaviour2);
        }
    }

    // Horizontal facing (N/S/E/W) - button on UP/DOWN faces
    private static class PumpScrollSlotHorizontal extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 12.5);
        }

        @Override
        protected boolean isSideActive(net.minecraft.world.level.block.state.BlockState state, Direction direction) {
            Direction facing = state.getValue(SteamPumpBlock.FACING);

            if (facing == Direction.NORTH) {
                return direction == Direction.UP || direction == Direction.DOWN;
            }
            if (facing == Direction.SOUTH) {
                return direction == Direction.UP || direction == Direction.DOWN;
            }
            if (facing == Direction.EAST) {
                return direction == Direction.UP || direction == Direction.DOWN;
            }
            if (facing == Direction.WEST) {
                return direction == Direction.UP || direction == Direction.DOWN;
            }
            return false;
        }

        @Override
        public void rotate(net.minecraft.world.level.LevelAccessor level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, PoseStack ms) {
            Direction facing = state.getValue(SteamPumpBlock.FACING);

            if (facing == Direction.NORTH) {
                super.rotate(level, pos, state, ms);
                return;
            }
            if (facing == Direction.SOUTH) {
                TransformStack.of(ms).rotateYDegrees(180);
                super.rotate(level, pos, state, ms);
                return;
            }
            if (facing == Direction.EAST) {
                TransformStack.of(ms).rotateYDegrees(90);
                super.rotate(level, pos, state, ms);
                return;
            }
            if (facing == Direction.WEST) {
                TransformStack.of(ms).rotateYDegrees(-90);
                super.rotate(level, pos, state, ms);
                return;
            }
            super.rotate(level, pos, state, ms);
        }
    }

// Vertical facing (UP/DOWN) - primary button on N/S or E/W depending on rotation
    private static class PumpScrollSlotVerticalPrimary extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 12.5);
        }

        @Override
        protected boolean isSideActive(net.minecraft.world.level.block.state.BlockState state, Direction direction) {
            Direction facing = state.getValue(SteamPumpBlock.FACING);
            boolean alongFirst = state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE);

            if (facing == Direction.UP && !alongFirst) {
                return direction == Direction.NORTH || direction == Direction.SOUTH;
            }
            if (facing == Direction.UP && alongFirst) {
                return direction == Direction.EAST || direction == Direction.WEST;
            }
            if (facing == Direction.DOWN && !alongFirst) {
                return direction == Direction.NORTH || direction == Direction.SOUTH;
            }
            if (facing == Direction.DOWN && alongFirst) {
                return direction == Direction.EAST || direction == Direction.WEST;
            }
            return false;
        }

        @Override
        public void rotate(net.minecraft.world.level.LevelAccessor level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, PoseStack ms) {
            Direction side = getSide();
            if (side == Direction.UP || side == Direction.DOWN) {
                return;
            }
            super.rotate(level, pos, state, ms);
        }
    }

    // Vertical facing (UP/DOWN) - secondary button on opposite faces
    private static class PumpScrollSlotVerticalSecondary extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 12.5);
        }

        @Override
        protected boolean isSideActive(net.minecraft.world.level.block.state.BlockState state, Direction direction) {
            Direction facing = state.getValue(SteamPumpBlock.FACING);
            boolean alongFirst = state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE);

            // Opposite of Primary
            if (facing == Direction.UP && !alongFirst) {
                return direction == Direction.NORTH || direction == Direction.SOUTH;
            }
            if (facing == Direction.UP && alongFirst) {
                return direction == Direction.EAST || direction == Direction.WEST;
            }
            if (facing == Direction.DOWN && !alongFirst) {
                return direction == Direction.NORTH || direction == Direction.SOUTH;
            }
            if (facing == Direction.DOWN && alongFirst) {
                return direction == Direction.EAST || direction == Direction.WEST;
            }
            return false;
        }

        @Override
        public void rotate(net.minecraft.world.level.LevelAccessor level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, PoseStack ms) {
            Direction side = getSide();
            if (side == Direction.UP || side == Direction.DOWN) {
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

    private float getEffectivePullRate() {
        if (targetPullRate <= 0) return 0f;
        float speed = getSpeed();
        float absSpeed = Math.abs(speed);
        if (absSpeed < 1f) return 0f;
        float speedMultiplier = absSpeed / SPEED_REFERENCE_RPM;
        float effectiveRate = BASE_PULL_RATE * (targetPullRate / 100f) * speedMultiplier;
        return Math.max(0f, effectiveRate);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;
        if (level.isClientSide) return;
        updateConnectionStates();
        updatePullPushDirections();
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

        if (neighborBE instanceof PressurizedPipeBlockEntity) return true;

        if (neighborBE instanceof ISteamTransport transport) {
            return transport.canConnect(dir.getOpposite());
        }

        if (neighborBE instanceof ISteamProducer producer) {
            return producer.canProduce(dir.getOpposite());
        }

        if (neighborBE instanceof ISteamEndpoint endpoint) {
            return endpoint.canConnect(dir.getOpposite());
        }

        return false;
    }

    private void pullFromPipes() {
        float effectiveRate = getEffectivePullRate();
        if (effectiveRate <= 0) {
            lastPulledSteam = 0f;
            return;
        }

        float actualPull = 0f;
        float remainingToPull = effectiveRate;
        SteamType pulledType = SteamType.REGULAR;
        boolean typeInitialized = false;

        for (Direction dir : Direction.values()) {
            if (dir == pushDirection) continue;
            if (remainingToPull <= 0.01f) break;

            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            var neighborBE = level.getBlockEntity(neighborPos);

            if (neighborBE instanceof PressurizedPipeBlockEntity pipe) {
                SteamData pulled = pipe.pullSteamFromNetwork(dir.getOpposite(), remainingToPull);
                if (!pulled.isEmpty()) {
                    actualPull += pulled.getThroughput();
                    remainingToPull -= pulled.getThroughput();
                    if (!typeInitialized) {
                        pulledType = pulled.getSteamType();
                        typeInitialized = true;
                    }
                }
            } else if (neighborBE instanceof ISteamProducer producer) {
                if (producer.canProduce(dir.getOpposite())) {
                    SteamData pulled = producer.produceSteam(dir.getOpposite());
                    if (!pulled.isEmpty() && pulled.getThroughput() > 0) {
                        float amountToPull = Math.min(pulled.getThroughput(), remainingToPull);
                        actualPull += amountToPull;
                        remainingToPull -= amountToPull;
                        if (!typeInitialized) {
                            pulledType = pulled.getSteamType();
                            typeInitialized = true;
                        }
                    }
                }
            }
        }

        if (actualPull > 0) {
            lastPulledSteam = actualPull;
            storedSteamType = pulledType;
            totalPulledSteam += actualPull;
            setChanged();
            sendData();
        } else {
            lastPulledSteam = 0f;
        }
    }

    private void pushToOutput() {
        if (lastPulledSteam <= 0) return;

        BlockPos outputPos = worldPosition.relative(pushDirection);
        if (!level.isLoaded(outputPos)) return;

        var neighborBE = level.getBlockEntity(outputPos);
        SteamData toPush = SteamData.of(lastPulledSteam, storedSteamType, 1f, 1f, lastPulledSteam);

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

    public SteamType getStoredSteamType() {
        return storedSteamType;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        targetPullRate = tag.getFloat("TargetPullRate");
        totalPulledSteam = tag.getFloat("TotalPulledSteam");
        if (clientPacket) {
            lastPulledSteam = tag.getFloat("LastPulledSteam");
        }
        if (tag.contains("StoredSteamType")) {
            try {
                storedSteamType = SteamType.valueOf(tag.getString("StoredSteamType"));
            } catch (IllegalArgumentException ignored) {
                storedSteamType = SteamType.REGULAR;
            }
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
        tag.putString("StoredSteamType", storedSteamType.name());
        for (Direction dir : Direction.values()) {
            tag.putBoolean("conn_" + dir.getName(), connections.getOrDefault(dir, false));
        }
    }

    @Override
    public boolean canConnect(Direction direction) {
        return direction == pullDirection || direction == pushDirection;
    }

    @Override
    public void receiveSteam(Direction direction, SteamData steam) {
        if (steam == null || steam.isEmpty()) return;
        lastPulledSteam += steam.getThroughput();
        storedSteamType = steam.getSteamType();
        totalPulledSteam += steam.getThroughput();
        setChanged();
        sendData();
    }

    @Override
    public boolean canReceive(Direction direction) {
        return direction == pullDirection || direction == pushDirection;
    }

    @Override
    public float getMaxReceiveRate(Direction direction) {
        return 100f;
    }
}
