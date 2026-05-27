package com.xciel.steamturbine.client.particle;

import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.steamturbine.steam.SteamConstants;
import com.xciel.steamturbine.steam.SteamType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SteamPipeParticleSpawner {
    private static final Map<BlockPos, Integer> lastSpawnTick = new ConcurrentHashMap<>();

    public static void spawnParticles(PressurizedPipeBlockEntity pipe, Level level, BlockPos pos) {
        if (level.isClientSide) {
            int currentTick = (int) (level.getGameTime() % Integer.MAX_VALUE);
            int lastTick = lastSpawnTick.getOrDefault(pos, 0);

            if (currentTick - lastTick < SteamConstants.MIN_TICKS_BETWEEN_PARTICLES) {
                return;
            }

            boolean anyHighPressure = false;
            for (Direction dir : Direction.values()) {
                if (pipe.getVisualPressure(dir) > 0.5f) {
                    anyHighPressure = true;
                    break;
                }
            }

            if (!anyHighPressure) {
                lastSpawnTick.remove(pos);
                return;
            }

            lastSpawnTick.put(pos, currentTick);
        }
    }
}