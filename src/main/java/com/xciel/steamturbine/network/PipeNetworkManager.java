package com.xciel.steamturbine.network;

import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.steamturbine.steam.SteamData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public class PipeNetworkManager {
    private static final Map<Level, Map<Long, PipeNetwork>> networks = new WeakHashMap<>();
    private static final Map<Level, Map<BlockPos, Long>> blockMap = new WeakHashMap<>();
    private static long nextId = 1;

    public static PipeNetwork getOrCreate(Level level, BlockPos start) {
        var netMap = networks.computeIfAbsent(level, k -> new HashMap<>());
        var posMap = blockMap.computeIfAbsent(level, k -> new HashMap<>());

        Long existing = posMap.get(start);
        if (existing != null) {
            PipeNetwork n = netMap.get(existing);
            if (n != null) return n;
        }

        Set<BlockPos> foundPipes = new HashSet<>();
        Map<BlockPos, BlockEntity> foundEndpoints = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        BlockEntity startBE = level.getBlockEntity(start);
        if (startBE instanceof PressurizedPipeBlockEntity) {
            foundPipes.add(start);
            queue.add(start);
        }

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = cur.relative(d);
                if (foundPipes.contains(n) || foundEndpoints.containsKey(n) || !level.isLoaded(n)) continue;
                BlockEntity nbe = level.getBlockEntity(n);
                if (nbe instanceof PressurizedPipeBlockEntity) {
                    foundPipes.add(n);
                    queue.add(n);
                } else if (isEndpoint(nbe) && canConnect(nbe, d.getOpposite())) {
                    foundEndpoints.put(n, nbe);
                }
            }
        }

        long id = nextId++;
        PipeNetwork net = new PipeNetwork(id, level);
        for (BlockPos p : foundPipes) { net.addPipe(p); posMap.put(p, id); }
        for (var e : foundEndpoints.entrySet()) { net.addEndpoint(e.getKey(), e.getValue()); posMap.put(e.getKey(), id); }
        netMap.put(id, net);
        return net;
    }

    public static PipeNetwork get(Level level, BlockPos pos) {
        var pm = blockMap.get(level);
        if (pm == null) return null;
        Long id = pm.get(pos);
        if (id == null) return null;
        var nm = networks.get(level);
        return nm != null ? nm.get(id) : null;
    }

    public static void pushSteam(Level level, BlockPos source, Direction dir, SteamData steam) {
        if (steam == null || steam.isEmpty()) return;
        PipeNetwork net = getOrCreate(level, source.relative(dir));
        if (net != null) net.enqueue(source.relative(dir), dir.getOpposite(), steam);
    }

    public static void tickAll(Level level) {
        var nm = networks.get(level);
        if (nm == null) return;
        for (PipeNetwork n : new ArrayList<>(nm.values())) n.tick();
    }

    public static void invalidate(Level level, BlockPos pos) {
        var pm = blockMap.get(level);
        if (pm == null) return;
        Long id = pm.get(pos);
        if (id == null) return;
        var nm = networks.get(level);
        if (nm == null) return;
        PipeNetwork net = nm.get(id);
        if (net == null) return;
        for (BlockPos p : net.getPipes()) pm.remove(p);
        for (BlockPos ep : new ArrayList<>(net.getEndpoints().keySet())) pm.remove(ep);
        nm.remove(id);
    }

    private static boolean isEndpoint(BlockEntity be) {
        return be instanceof com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity
            || be instanceof com.xciel.steamturbine.content.compressor.SteamCompressorBlockEntity
            || be instanceof com.xciel.steamturbine.content.boiler.SteamBoilerBlockEntity
            || be instanceof com.xciel.steamturbine.content.shaft.TurbineShaftBlockEntity;
    }

    private static boolean canConnect(BlockEntity be, Direction from) {
        if (be instanceof com.xciel.steamturbine.steam.transfer.ISteamEndpoint e) return e.canConnect(from);
        if (be instanceof com.xciel.steamturbine.steam.transfer.ITurbineEndpoint e) return e.canTurbineConnect(from);
        return false;
    }
}