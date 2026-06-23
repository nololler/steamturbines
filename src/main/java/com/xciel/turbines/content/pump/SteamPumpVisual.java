package com.xciel.turbines.content.pump;

import java.util.function.Consumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;

public class SteamPumpVisual extends KineticBlockEntityVisual<SteamPumpBlockEntity> {

    private RotatingInstance shaft;

    public SteamPumpVisual(VisualizationContext context, SteamPumpBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        var axis = ((SteamPumpBlock) blockEntity.getBlockState().getBlock()).getRotationAxis(blockEntity.getBlockState());

        shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT))
            .createInstance();

        shaft.setup(blockEntity)
            .setPosition(getVisualPosition())
            .rotateToFace(axis)
            .setChanged();
    }

    @Override
    public void update(float partialTick) {
        shaft.setup(blockEntity).setChanged();
    }

    @Override
    protected void _delete() {
        shaft.delete();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(pos, shaft);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
    }
}