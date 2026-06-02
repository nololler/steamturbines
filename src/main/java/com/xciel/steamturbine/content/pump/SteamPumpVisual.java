package com.xciel.steamturbine.content.pump;

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
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class SteamPumpVisual extends KineticBlockEntityVisual<SteamPumpBlockEntity> {

    private final Map<Direction, RotatingInstance> shafts = new EnumMap<>(Direction.class);

    public SteamPumpVisual(VisualizationContext context, SteamPumpBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        BlockState state = blockEntity.getBlockState();
        Direction.Axis rotationAxis = ((IRotate) state.getBlock()).getRotationAxis(state);

        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING,
            Models.partial(AllPartialModels.SHAFT_HALF));

        for (Direction direction : Iterate.directionsInAxis(rotationAxis)) {
            RotatingInstance instance = instancer.createInstance();
            instance.setup(blockEntity, rotationAxis, blockEntity.getSpeed())
                .setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, direction)
                .setChanged();
            shafts.put(direction, instance);
        }
    }

    @Override
    public void update(float partialTick) {
        float speed = blockEntity.getSpeed();
        BlockState state = blockEntity.getBlockState();
        Direction.Axis rotationAxis = ((IRotate) state.getBlock()).getRotationAxis(state);
        for (RotatingInstance shaft : shafts.values()) {
            shaft.setup(blockEntity, rotationAxis, speed).setChanged();
        }
    }

    @Override
    protected void _delete() {
        shafts.values().forEach(RotatingInstance::delete);
    }

    @Override
    public void updateLight(float partialTick) {
        relight(shafts.values().toArray(FlatLit[]::new));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        shafts.values().forEach(consumer);
    }
}