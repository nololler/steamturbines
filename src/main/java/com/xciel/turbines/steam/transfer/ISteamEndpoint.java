package com.xciel.turbines.steam.transfer;

import net.minecraft.core.Direction;

public interface ISteamEndpoint {
    boolean canConnect(Direction direction);
}