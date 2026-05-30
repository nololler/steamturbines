package com.xciel.steamturbine.steam.transfer;

import com.xciel.steamturbine.steam.SteamData;
import net.minecraft.core.Direction;

public interface ISteamProducer {
    SteamData produceSteam(Direction from);

    float getMaxProduceRate(Direction from);

    boolean canProduce(Direction direction);
}