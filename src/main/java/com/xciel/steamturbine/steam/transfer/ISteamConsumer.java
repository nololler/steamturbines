package com.xciel.steamturbine.steam.transfer;

import com.xciel.steamturbine.steam.SteamData;
import net.minecraft.core.Direction;

public interface ISteamConsumer {
    void receiveSteam(Direction direction, SteamData steam);

    boolean canReceive(Direction direction);

    float getMaxReceiveRate(Direction direction);
}