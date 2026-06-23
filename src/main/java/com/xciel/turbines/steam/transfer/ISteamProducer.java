package com.xciel.turbines.steam.transfer;

import com.xciel.turbines.steam.SteamData;
import net.minecraft.core.Direction;

public interface ISteamProducer {
    SteamData produceSteam(Direction from);

    float getMaxProduceRate(Direction from);

    boolean canProduce(Direction direction);
}