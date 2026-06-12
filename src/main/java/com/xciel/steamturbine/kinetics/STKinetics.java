package com.xciel.steamturbine.kinetics;

/**
 * Entry point and utility registry for the SteamTurbine custom kinetic framework.
 *
 * This framework intercepts Create's RotationPropagator to allow block entities
 * to have independent control over their output rotation speed and direction,
 * bypassing the standard relay-only modifier system.
 *
 * The framework operates via mixins (see the mixin package) that hook into:
 * - RotationPropagator.getRotationSpeedModifier() — wraps the modifier call
 * - GeneratingKineticBlockEntity.applyNewSpeed() — prevents source overwrite
 */
public final class STKinetics {

    private STKinetics() {}

    public static final String MIXIN_REFMAP = "steamturbine.refmap.json";
}