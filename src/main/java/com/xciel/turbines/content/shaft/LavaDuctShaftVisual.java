package com.xciel.turbines.content.shaft;

import java.util.function.Consumer;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import net.minecraft.world.level.block.state.BlockState;

public class LavaDuctShaftVisual extends KineticBlockEntityVisual<LavaDuctShaftBlockEntity> {

    public LavaDuctShaftVisual(VisualizationContext context, LavaDuctShaftBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
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