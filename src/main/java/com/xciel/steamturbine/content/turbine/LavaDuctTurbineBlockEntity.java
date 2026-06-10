package com.xciel.steamturbine.content.turbine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.xciel.steamturbine.steam.SteamConstants;
import com.xciel.steamturbine.steam.transfer.ILavaDuctTurbineEndpoint;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class LavaDuctTurbineBlockEntity extends SmartBlockEntity implements ILavaDuctTurbineEndpoint, IHaveGoggleInformation {

    private static final float SU_PER_LAVA_FACE = SteamConstants.LAVA_DUCT_SU_PER_FACE;

    private int lavaFaceCount;
    private float generatedSU;
    private int stageNumber;

    public LavaDuctTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(5);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        stageNumber = countTurbinesAbove();

        int prevCount = lavaFaceCount;

        int count = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;
            if (level.getBlockState(neighborPos).is(Blocks.LAVA))
                count++;
        }

        if (count != prevCount) {
            lavaFaceCount = count;
            generatedSU = lavaFaceCount * SU_PER_LAVA_FACE;
            setChanged();
            sendData();
        }
    }

    public void onNeighborChanged() {
        if (level == null || level.isClientSide) return;
        stageNumber = countTurbinesAbove();
        int count = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;
            if (level.getBlockState(neighborPos).is(Blocks.LAVA))
                count++;
        }
        lavaFaceCount = count;
        generatedSU = lavaFaceCount * SU_PER_LAVA_FACE;
        setChanged();
        sendData();
    }

    @Override
    public int getLavaFaceCount() {
        return lavaFaceCount;
    }

    @Override
    public float getGeneratedSU() {
        return generatedSU;
    }

    private int countTurbinesAbove() {
        int count = 0;
        BlockPos current = worldPosition.above();
        while (level != null && level.isLoaded(current)) {
            if (level.getBlockEntity(current) instanceof LavaDuctTurbineBlockEntity) {
                count++;
                current = current.above();
            } else {
                break;
            }
        }
        return count;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    Lava Duct Turbine").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("    Stage: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(stageNumber)).withStyle(ChatFormatting.WHITE)));
        return true;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        lavaFaceCount = tag.getInt("LavaFaceCount");
        generatedSU = tag.getFloat("GeneratedSU");
        stageNumber = tag.getInt("StageNumber");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("LavaFaceCount", lavaFaceCount);
        tag.putFloat("GeneratedSU", generatedSU);
        tag.putInt("StageNumber", stageNumber);
    }
}