package com.xciel.steamturbine.content.boilerTurbine;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;

import java.util.function.Consumer;

public class ShaftCasingVisual extends KineticBlockEntityVisual<ShaftCasingBlockEntity> {

    private RotatingInstance shaft;

    public ShaftCasingVisual(VisualizationContext context, ShaftCasingBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        setupShaft();
    }

    private void setupShaft() {
        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING,
            Models.partial(AllPartialModels.SHAFT_HALF));

        shaft = instancer.createInstance();
        shaft.setup(blockEntity, Direction.Axis.Y, blockEntity.getSpeed())
            .setPosition(getVisualPosition())
            .setChanged();
    }

    @Override
    public void update(float partialTick) {
        float speed = blockEntity.getSpeed();
        shaft.setup(blockEntity, Direction.Axis.Y, speed).setChanged();
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