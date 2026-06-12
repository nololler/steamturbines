package com.xciel.steamturbine.kinetics;

import net.minecraft.core.Direction;

/**
 * Interface for block entities that can override Create's kinetic propagation
 * to control their output speed/direction independently of input.
 *
 * When isControllingOutput() returns true, the framework mixin intercepts
 * RotationPropagator.getConveyedSpeed() and returns the controlled speed
 * directly for the output face, while blocking propagation on all other faces.
 *
 * The block entity must extend GeneratingKineticBlockEntity and override
 * getGeneratedSpeed() to return the controlled speed when isControllingOutput() is true.
 */
public interface IKineticController {

    boolean isControllingOutput();

    float getControlledSpeed();

    float getControlledDirectionSign();

    Direction getControlledOutputFace();
}