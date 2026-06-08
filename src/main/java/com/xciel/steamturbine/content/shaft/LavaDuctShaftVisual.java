package com.xciel.steamturbine.content.shaft;

import java.util.function.Consumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class LavaDuctShaftVisual extends KineticBlockEntityVisual<LavaDuctShaftBlockEntity> {

    private RotatingInstance shaft1;
    private RotatingInstance shaft2;
    private Direction dir1;
    private Direction dir2;

    public LavaDuctShaftVisual(VisualizationContext context, LavaDuctShaftBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(LavaDuctShaftBlock.FACING);
        Direction.Axis rotationAxis = facing.getAxis();

        dir1 = facing.getAxis() == Direction.Axis.X ? Direction.EAST : Direction.NORTH;
        dir2 = dir1.getOpposite();

        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING,
            Models.partial(AllPartialModels.SHAFT_HALF));

        shaft1 = instancer.createInstance();
        shaft1.setup(blockEntity, rotationAxis, blockEntity.getSpeed())
            .setPosition(getVisualPosition())
            .rotateToFace(Direction.SOUTH, dir1)
            .setChanged();

        shaft2 = instancer.createInstance();
        shaft2.setup(blockEntity, rotationAxis, blockEntity.getSpeed())
            .setPosition(getVisualPosition())
            .rotateToFace(Direction.SOUTH, dir2)
            .setChanged();
    }

    @Override
    public void update(float partialTick) {
        float speed = blockEntity.getSpeed();
        Direction facing = blockState.getValue(LavaDuctShaftBlock.FACING);
        Direction.Axis rotationAxis = facing.getAxis();

        shaft1.setup(blockEntity, dir1.getAxis(), speed)
            .rotateToFace(Direction.SOUTH, dir1)
            .setChanged();

        shaft2.setup(blockEntity, dir2.getAxis(), speed)
            .rotateToFace(Direction.SOUTH, dir2)
            .setChanged();
    }

    @Override
    protected void _delete() {
        shaft1.delete();
        shaft2.delete();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(new FlatLit[]{shaft1, shaft2});
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft1);
        consumer.accept(shaft2);
    }
}