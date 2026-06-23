package com.xciel.turbines.steam.transfer;

import com.xciel.turbines.steam.SteamData;
import net.minecraft.core.Direction;

public interface ISteamSource {
    SteamData extractSteam(Direction direction, float requestedAmount);

    boolean canSource(Direction direction);

    float getAvailableSteam(Direction direction);
}