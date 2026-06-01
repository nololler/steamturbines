package com.xciel.steamturbine.network;

import com.xciel.steamturbine.steam.SteamData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface NetworkRule {
    boolean blocksPath(BlockPos from, Direction fromDir, BlockPos to, Direction toDir, SteamData steam);
}