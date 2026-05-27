package com.xciel.steamturbine.steam.transfer;

import com.xciel.steamturbine.steam.SteamData;
import net.minecraft.core.Direction;

public interface ISteamTransport {
    boolean canConnect(Direction direction);

    void pushSteam(Direction direction, SteamData steam);

    SteamData pullSteam(Direction direction, float amount);

    float getFlowRate(Direction direction);
}