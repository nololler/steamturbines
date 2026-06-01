package com.xciel.steamturbine.network;

import com.xciel.steamturbine.content.compressor.SteamCompressorBlockEntity;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlock;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity;
import com.xciel.steamturbine.steam.SteamData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class TurbineToCompressorRule implements NetworkRule {
    private final Level level;

    public TurbineToCompressorRule(Level level) { this.level = level; }

    @Override
    public boolean blocksPath(BlockPos from, Direction fromDir, BlockPos to, Direction toDir, SteamData steam) {
        if (!(level.getBlockEntity(to) instanceof SteamCompressorBlockEntity toComp)) return false;
        if (toComp.getSteamInputDirection() != toDir) return false;

        return hasTurbineSource(from, fromDir, 100);
    }

    private boolean hasTurbineSource(BlockPos pos, Direction dir, int maxDepth) {
        if (maxDepth <= 0 || !level.isLoaded(pos)) return false;
        var be = level.getBlockEntity(pos);

        if (be instanceof SteamTurbineBlockEntity turbine) {
            Direction facing = turbine.getBlockState().getValue(SteamTurbineBlock.FACING);
            return facing.getOpposite() == dir;
        }
        if (be instanceof PressurizedPipeBlockEntity) {
            return hasTurbineSource(pos.relative(dir), dir, maxDepth - 1);
        }
        return false;
    }
}