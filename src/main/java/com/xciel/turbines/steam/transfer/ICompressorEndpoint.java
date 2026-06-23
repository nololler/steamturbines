package com.xciel.turbines.steam.transfer;

import net.minecraft.core.Direction;

public interface ICompressorEndpoint {
    boolean isCompressor();

    Direction getCompressorOutputDirection();
}