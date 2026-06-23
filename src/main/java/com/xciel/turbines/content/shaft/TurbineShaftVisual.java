package com.xciel.turbines.content.shaft;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class TurbineShaftVisual extends KineticBlockEntityVisual<TurbineShaftBlockEntity> {

    private RotatingInstance shaft;

    public TurbineShaftVisual(VisualizationContext context, TurbineShaftBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        BlockState state = blockEntity.getBlockState();
        Direction outputDir = ((TurbineShaftBlock) state.getBlock()).getShaftOutputDirection(state);

        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING,
            Models.partial(AllPartialModels.SHAFT_HALF));

        shaft = instancer.createInstance();
        shaft.setup(blockEntity, outputDir.getAxis(), blockEntity.getSpeed())
            .setPosition(getVisualPosition())
            .rotateToFace(Direction.SOUTH, outputDir)
            .setChanged();
    }

    @Override
    public void update(float partialTick) {
        float speed = blockEntity.getSpeed();
        BlockState state = blockEntity.getBlockState();
        Direction outputDir = ((TurbineShaftBlock) state.getBlock()).getShaftOutputDirection(state);
        shaft.setup(blockEntity, outputDir.getAxis(), speed).setChanged();
    }

    @Override
    protected void _delete() {
        shaft.delete();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(new FlatLit[]{shaft});
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
    }
}