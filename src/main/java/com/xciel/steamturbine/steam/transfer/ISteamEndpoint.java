package com.xciel.steamturbine.steam.transfer;

import net.minecraft.core.Direction;

public interface ISteamEndpoint {
    boolean canConnect(Direction direction);
}