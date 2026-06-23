package com.xciel.turbines.steam.transfer;

import com.xciel.turbines.steam.SteamData;
import net.minecraft.core.Direction;

public interface ITurbineEndpoint {
    boolean canTurbineConnect(Direction direction);

    SteamData produceTurbineSteam(Direction from);
}