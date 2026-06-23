package com.xciel.turbines.content.ejector;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;
import com.xciel.turbines.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.turbines.steam.SteamData;
import com.xciel.turbines.steam.transfer.ISteamTransport;
import com.xciel.turbines.steam.transfer.IPressurizedConsumer;
import com.xciel.turbines.steam.transfer.ISteamEndpoint;
import com.xciel.turbines.steam.transfer.ISteamProducer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;
import java.util.Map.Entry;

public class SteamEjectorBlockEntity extends SmartBlockEntity implements
    IPressurizedConsumer, ISteamEndpoint, ISteamTransport, IHaveGoggleInformation {

    private static final float MAX_STEAM_BUFFER = 100f;
    private static final float STEAM_TO_FLUID_MULTIPLIER = 1.0f;

    private SteamData inputSteam = SteamData.empty();

    public SteamEjectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new EjectorFluidTransferBehaviour(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;
        if (level.isClientSide) return;

        inputSteam = SteamData.empty();
        pullSteamFromPipe();
    }

    private void pullSteamFromPipe() {
        for (Direction face : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(face);
            if (!level.isLoaded(neighborPos)) continue;

            var neighbor = level.getBlockEntity(neighborPos);
            if (neighbor instanceof PressurizedPipeBlockEntity pipe) {
                SteamData pulled = pipe.pullSteamFromNetwork(face.getOpposite(), MAX_STEAM_BUFFER);
                if (!pulled.isEmpty()) {
                    inputSteam = pulled;
                    setChanged();
                    return;
                }
            } else if (neighbor instanceof ISteamProducer producer) {
                if (producer.canProduce(face.getOpposite())) {
                    SteamData pulled = producer.produceSteam(face.getOpposite());
                    if (!pulled.isEmpty() && pulled.getThroughput() > 0) {
                        inputSteam = pulled.withThroughput(Math.min(pulled.getThroughput(), MAX_STEAM_BUFFER));
                        setChanged();
                        return;
                    }
                }
            } else if (neighbor instanceof ISteamTransport transport) {
                if (transport.canConnect(face.getOpposite())) {
                    SteamData pulled = transport.pullSteam(face.getOpposite(), MAX_STEAM_BUFFER);
                    if (!pulled.isEmpty()) {
                        inputSteam = pulled;
                        setChanged();
                        return;
                    }
                }
            }
        }
    }

    public Direction getFacing() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    public Direction getFluidInputDirection() {
        return getFacing().getOpposite();
    }

    public Direction getFluidOutputDirection() {
        return getFacing();
    }

    public boolean isFront(Direction side) {
        return getFacing() == side;
    }

    public Direction getSteamInputDirection() {
        Direction facing = getFacing();
        boolean alongFirst = getBlockState().getValue(SteamEjectorBlock.AXIS_ALONG_FIRST_COORDINATE);

        if (facing.getAxis().isVertical()) {
            return alongFirst ? Direction.NORTH : Direction.EAST;
        }

        return Direction.fromAxisAndDirection(facing.getClockWise().getAxis(), Direction.AxisDirection.NEGATIVE);
    }

    @Override
    public boolean canReceive(Direction direction) {
        return true;
    }

    @Override
    public void receiveSteam(Direction direction, SteamData steam) {
        if (steam == null || steam.isEmpty()) return;

        if (inputSteam.isEmpty()) {
            inputSteam = steam;
        } else {
            inputSteam = inputSteam.withPressureAndThroughputAdded(steam.getPressure(), steam.getThroughput());
        }
        inputSteam = inputSteam.withThroughput(Math.min(inputSteam.getThroughput(), MAX_STEAM_BUFFER));
        setChanged();
    }

    @Override
    public float getMaxReceiveRate(Direction direction) {
        return MAX_STEAM_BUFFER;
    }

    @Override
    public boolean canConnect(Direction direction) {
        Direction steamInput = getSteamInputDirection();
        return direction == steamInput || direction == steamInput.getOpposite();
    }

    @Override
    public void pushSteam(Direction direction, SteamData steam) {
        receiveSteam(direction, steam);
    }

    @Override
    public SteamData pullSteam(Direction direction, float amount) {
        return SteamData.empty();
    }

    @Override
    public float getFlowRate(Direction direction) {
        return inputSteam.getThroughput();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    Steam Ejector").withStyle(ChatFormatting.GOLD));
        if (!inputSteam.isEmpty()) {
            tooltip.add(Component.literal("    Steam: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%.1f @ %.1f/t", inputSteam.getPressure(), inputSteam.getThroughput())).withStyle(ChatFormatting.DARK_GRAY)));
        } else {
            tooltip.add(Component.literal("    Idle").withStyle(ChatFormatting.DARK_GRAY));
        }
        return true;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("InputSteam")) {
            inputSteam = SteamData.loadFromNBT(tag.getCompound("InputSteam"), registries);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        CompoundTag inputTag = new CompoundTag();
        inputSteam.saveToNBT(inputTag, registries);
        tag.put("InputSteam", inputTag);
    }

    private class EjectorFluidTransferBehaviour extends FluidTransportBehaviour {
        public EjectorFluidTransferBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            Direction facing = getFacing();
            return direction == facing || direction == facing.getOpposite();
        }

        @Override
        public void tick() {
            super.tick();
            if (interfaces == null) return;
            for (Entry<Direction, PipeConnection> entry : interfaces.entrySet()) {
                boolean pull = !isFront(entry.getKey());
                Couple<Float> pressure = entry.getValue().getPressure();
                float pressureValue = inputSteam.isEmpty() ? 0f : inputSteam.getThroughput() * STEAM_TO_FLUID_MULTIPLIER;
                pressure.set(pull, pressureValue);
                pressure.set(!pull, 0f);
            }
            setChanged();
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state,
                                                         Direction direction) {
            AttachmentTypes attachment = super.getRenderedRimAttachment(world, pos, state, direction);
            if (attachment == AttachmentTypes.RIM)
                return AttachmentTypes.NONE;
            return attachment;
        }
    }
}
