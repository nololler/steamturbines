package com.xciel.steamturbine.network;

import com.xciel.steamturbine.content.compressor.SteamCompressorBlockEntity;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.steamturbine.steam.SteamData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class CompressorLoopRule implements NetworkRule {
    private final Level level;

    public CompressorLoopRule(Level level) { this.level = level; }

    @Override
    public boolean blocksPath(BlockPos from, Direction fromDir, BlockPos to, Direction toDir, SteamData steam) {
        if (!(level.getBlockEntity(to) instanceof SteamCompressorBlockEntity toComp)) return false;
        if (toComp.getSteamInputDirection() != toDir) return false;

        // Check direct: from is a compressor output
        if (level.getBlockEntity(from) instanceof SteamCompressorBlockEntity fromComp)
            return fromComp.getCompressorOutputDirection() == fromDir;
        // Check through pipe: walk back one step
        if (level.getBlockEntity(from) instanceof PressurizedPipeBlockEntity) {
            if (level.getBlockEntity(from.relative(fromDir.getOpposite())) instanceof SteamCompressorBlockEntity src)
                return src.getCompressorOutputDirection() == fromDir.getOpposite();
        }
        return false;
    }
}