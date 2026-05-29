package com.xciel.steamturbine.steam.transfer;

import net.minecraft.core.Direction;

public interface ITurbineEndpoint {
    boolean canTurbineConnect(Direction direction);
}