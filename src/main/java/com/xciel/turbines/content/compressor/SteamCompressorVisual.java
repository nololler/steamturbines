package com.xciel.turbines.content.compressor;

import java.util.function.Consumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.xciel.turbines.content.compressor.SteamCompressorBlock;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;

public class SteamCompressorVisual extends KineticBlockEntityVisual<SteamCompressorBlockEntity> {

    private RotatingInstance shaft;

    public SteamCompressorVisual(VisualizationContext context, SteamCompressorBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        var axis = ((SteamCompressorBlock) blockEntity.getBlockState().getBlock()).getRotationAxis(blockEntity.getBlockState());

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