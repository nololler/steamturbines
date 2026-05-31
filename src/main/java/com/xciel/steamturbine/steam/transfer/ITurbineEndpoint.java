package com.xciel.steamturbine.steam.transfer;

import com.xciel.steamturbine.steam.SteamData;
import net.minecraft.core.Direction;

public interface ITurbineEndpoint {
    boolean canTurbineConnect(Direction direction);

    SteamData produceTurbineSteam(Direction from);
}