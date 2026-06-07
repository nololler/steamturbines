package com.xciel.steamturbine.content.boilerTurbine;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;

import java.util.function.Consumer;

public class ShaftCasingVisual extends KineticBlockEntityVisual<ShaftCasingBlockEntity> {

    public ShaftCasingVisual(VisualizationContext context, ShaftCasingBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
    }

    @Override
    public void update(float partialTick) {
    }

    @Override
    protected void _delete() {
    }

    @Override
    public void updateLight(float partialTick) {
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
    }
}