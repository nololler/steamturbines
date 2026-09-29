package com.xciel.turbines.content.hydro_turbine;

import com.simibubi.create.foundation.block.IBE;
import com.xciel.turbines.AllBlockEntityTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Upright is the water inlet; upside-down is the water exhaust. */
public class HydroTurbineIOBlock extends Block implements IBE<HydroTurbineIOBlockEntity> {

    public static final EnumProperty<Direction> FACING = EnumProperty.create(
        "facing", Direction.class, Direction.UP, Direction.DOWN);
    private static final double INVERTED_PLACEMENT_LOOK_THRESHOLD = 0.2;

    public HydroTurbineIOBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction facing;
        if (clickedFace.getAxis() == Direction.Axis.Y) {
            facing = clickedFace;
        } else if (context.getPlayer() != null
            && context.getPlayer().getLookAngle().y > INVERTED_PLACEMENT_LOOK_THRESHOLD) {
            // Allow an upward-angled side click to place the underside Exhaust form.
            facing = Direction.DOWN;
        } else {
            facing = Direction.UP;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state;
    }

    @Override
    public Class<HydroTurbineIOBlockEntity> getBlockEntityClass() {
        return HydroTurbineIOBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HydroTurbineIOBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.HYDRO_TURBINE_IO.get();
    }
}
