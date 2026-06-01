package com.xciel.steamturbine.content.shaft;

import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlock;
import com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity;
import com.xciel.steamturbine.steam.transfer.ITurbineEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class TurbineNetworkWalker {
    private final Level level;
    private final Set<BlockPos> visited = new HashSet<>();
    private final List<TurbineInfo> foundTurbines = new ArrayList<>();

    public static class TurbineInfo {
        public final BlockPos pos;
        public final SteamTurbineBlockEntity turbine;
        public final int distance;
        public final List<BlockPos> path;

        public TurbineInfo(BlockPos pos, SteamTurbineBlockEntity turbine, int distance, List<BlockPos> path) {
            this.pos = pos;
            this.turbine = turbine;
            this.distance = distance;
            this.path = path;
        }
    }

    public TurbineNetworkWalker(Level level) {
        this.level = level;
    }

    public List<TurbineInfo> findTurbines(BlockPos start, Direction initialDirection) {
        visited.clear();
        foundTurbines.clear();

        BlockPos firstStep = start.relative(initialDirection);
        List<BlockPos> initialPath = new ArrayList<>();
        initialPath.add(start);
        initialPath.add(firstStep);

        walk(initialPath, firstStep, initialDirection.getOpposite());

        return foundTurbines;
    }

    private void walk(List<BlockPos> path, BlockPos current, Direction cameFrom) {
        if (!level.isLoaded(current) || visited.contains(current)) {
            return;
        }
        visited.add(current);

        BlockState state = level.getBlockState(current);
        Block block = state.getBlock();

        // If it's a turbine, record it and continue walking through
        if (block instanceof SteamTurbineBlock) {
            var be = level.getBlockEntity(current);
            if (be instanceof SteamTurbineBlockEntity turbine) {
                foundTurbines.add(new TurbineInfo(current, turbine, path.size() - 1, new ArrayList<>(path)));

                // Continue walking past the turbine in the same direction we came from
                BlockPos next = current.relative(cameFrom.getOpposite());
                List<BlockPos> newPath = new ArrayList<>(path);
                newPath.add(next);
                walk(newPath, next, cameFrom);
            }
            return;
        }

        // If it's a pipe, explore all connected directions
        if (block instanceof PressurizedPipeBlock) {
            // Check all 6 directions for valid connections
            for (Direction dir : Direction.values()) {
                if (dir == cameFrom) continue;

                BlockPos next = current.relative(dir);
                BlockState nextState = level.getBlockState(next);
                Block nextBlock = nextState.getBlock();

                // Skip if same pipe already visited
                if (nextBlock instanceof PressurizedPipeBlock) {
                    if (!visited.contains(next)) {
                        List<BlockPos> newPath = new ArrayList<>(path);
                        newPath.add(next);
                        walk(newPath, next, dir.getOpposite());
                    }
                    continue;
                }

                // If it's a turbine, check if it can connect from this direction
                if (nextBlock instanceof SteamTurbineBlock) {
                    var nextBE = level.getBlockEntity(next);
                    if (nextBE instanceof ITurbineEndpoint endpoint) {
                        // Check if turbine accepts connection from this direction
                        if (endpoint.canTurbineConnect(dir.getOpposite())) {
                            List<BlockPos> newPath = new ArrayList<>(path);
                            newPath.add(next);
                            walk(newPath, next, dir.getOpposite());
                        }
                    }
                }
            }
        }
    }

    public static TurbineInfo findLastTurbine(List<TurbineInfo> turbines) {
        if (turbines.isEmpty()) return null;
        return turbines.stream()
                .max(Comparator.comparingInt(t -> t.distance))
                .orElse(null);
    }

    public static float sumTurbineSpeed(List<TurbineInfo> turbines) {
        float sum = 0f;
        for (TurbineInfo info : turbines) {
            sum += Math.abs(info.turbine.getTurbineSpeed());
        }
        return sum;
    }

    public static float maxTurbineSpeed(List<TurbineInfo> turbines) {
        float max = 0f;
        for (TurbineInfo info : turbines) {
            max = Math.max(max, Math.abs(info.turbine.getTurbineSpeed()));
        }
        return max;
    }

    public static float sumExhaustThroughput(List<TurbineInfo> turbines) {
        float sum = 0f;
        for (TurbineInfo info : turbines) {
            sum += info.turbine.getExhaustThroughput();
        }
        return sum;
    }
}