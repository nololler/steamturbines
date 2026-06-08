package com.xciel.steamturbine.content.shaft;

import com.xciel.steamturbine.content.turbine.LavaDuctTurbineBlock;
import com.xciel.steamturbine.content.turbine.LavaDuctTurbineBlockEntity;
import com.xciel.steamturbine.steam.transfer.ILavaDuctTurbineEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class LavaDuctNetworkWalker {
    private final Level level;
    private final Set<BlockPos> visited = new HashSet<>();
    private final List<TurbineInfo> foundTurbines = new ArrayList<>();

    public static class TurbineInfo {
        public final BlockPos pos;
        public final LavaDuctTurbineBlockEntity turbine;
        public final int distance;

        public TurbineInfo(BlockPos pos, LavaDuctTurbineBlockEntity turbine, int distance) {
            this.pos = pos;
            this.turbine = turbine;
            this.distance = distance;
        }
    }

    private static final int MAX_DEPTH = 5;

    public LavaDuctNetworkWalker(Level level) {
        this.level = level;
    }

    public List<TurbineInfo> findTurbines(BlockPos start, Direction initialDirection) {
        visited.clear();
        foundTurbines.clear();

        BlockPos current = start.relative(initialDirection);
        walk(current, initialDirection.getOpposite(), 1);

        return foundTurbines;
    }

    private void walk(BlockPos current, Direction cameFrom, int depth) {
        if (depth > MAX_DEPTH || !level.isLoaded(current) || visited.contains(current)) {
            return;
        }
        visited.add(current);

        BlockState state = level.getBlockState(current);
        Block block = state.getBlock();

        if (block instanceof LavaDuctTurbineBlock) {
            var be = level.getBlockEntity(current);
            if (be instanceof LavaDuctTurbineBlockEntity turbine) {
                foundTurbines.add(new TurbineInfo(current, turbine, depth));

                Direction continueDir = Direction.DOWN;
                BlockPos next = current.relative(continueDir);
                if (!visited.contains(next)) {
                    walk(next, continueDir.getOpposite(), depth + 1);
                }
            }
            return;
        }

        if (block instanceof LavaDuctShaftBlock) {
            return;
        }
    }

    public static TurbineInfo findLastTurbine(List<TurbineInfo> turbines) {
        if (turbines.isEmpty()) return null;
        return turbines.stream()
                .max(Comparator.comparingInt(t -> t.distance))
                .orElse(null);
    }

    public static float sumTurbineSU(List<TurbineInfo> turbines) {
        float sum = 0f;
        for (TurbineInfo info : turbines) {
            if (info.turbine != null) {
                sum += info.turbine.getGeneratedSU();
            }
        }
        return sum;
    }
}