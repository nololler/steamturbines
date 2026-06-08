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

    private RotatingInstance northShaft;
    private RotatingInstance southShaft;
    private Direction.Axis rotationAxis = Direction.Axis.Z;

    public LavaDuctShaftVisual(VisualizationContext context, LavaDuctShaftBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING,
            Models.partial(AllPartialModels.SHAFT_HALF));

        northShaft = instancer.createInstance();
        northShaft.setup(blockEntity, rotationAxis, blockEntity.getSpeed())
            .setPosition(getVisualPosition())
            .rotateToFace(Direction.SOUTH, Direction.NORTH)
            .setChanged();

        southShaft = instancer.createInstance();
        southShaft.setup(blockEntity, rotationAxis, blockEntity.getSpeed())
            .setPosition(getVisualPosition())
            .rotateToFace(Direction.NORTH, Direction.SOUTH)
            .setChanged();
    }

    @Override
    public void update(float partialTick) {
        float speed = blockEntity.getSpeed();

        northShaft.setup(blockEntity, rotationAxis, speed)
            .rotateToFace(Direction.SOUTH, Direction.NORTH)
            .setChanged();

        southShaft.setup(blockEntity, rotationAxis, speed)
            .rotateToFace(Direction.NORTH, Direction.SOUTH)
            .setChanged();
    }

    @Override
    protected void _delete() {
        northShaft.delete();
        southShaft.delete();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(new FlatLit[]{northShaft, southShaft});
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(northShaft);
        consumer.accept(southShaft);
    }
}