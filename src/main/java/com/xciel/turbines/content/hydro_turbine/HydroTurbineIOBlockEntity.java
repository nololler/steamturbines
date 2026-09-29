package com.xciel.turbines.content.hydro_turbine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class HydroTurbineIOBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public HydroTurbineIOBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean inlet = getBlockState().getValue(HydroTurbineIOBlock.FACING) == net.minecraft.core.Direction.UP;
        String key = inlet ? "block.turbines.hydro_turbine_io.goggles.inlet"
            : "block.turbines.hydro_turbine_io.goggles.exhaust";
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GOLD));
        return true;
    }
}
