package com.xciel.steamturbine.network;

import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlockEntity;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.transfer.ISteamConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
import com.xciel.steamturbine.steam.transfer.ISteamTransport;
import com.xciel.steamturbine.steam.transfer.ITurbineEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class PipeNetwork {
    private final long id;
    private final Level level;
    private final Set<BlockPos> pipes = new HashSet<>();
    private final Map<BlockPos, BlockEntity> endpoints = new HashMap<>();
    private final Map<BlockPos, Set<Direction>> connections = new HashMap<>();
    private final List<QueueEntry> queue = new ArrayList<>();
    private final List<NetworkRule> rules;
    private boolean dirty = true;

    private record QueueEntry(BlockPos entryPoint, Direction fromDir, SteamData steam) {}

    public PipeNetwork(long id, Level level) {
        this.id = id;
        this.level = level;
        this.rules = new ArrayList<>();
        this.rules.add(new CompressorLoopRule(level));
        this.rules.add(new TurbineToCompressorRule(level));
    }

    public void addPipe(BlockPos pos) { pipes.add(pos); dirty = true; }
    public void removePipe(BlockPos pos) { pipes.remove(pos); dirty = true; }
    public void addEndpoint(BlockPos pos, BlockEntity be) { endpoints.put(pos, be); dirty = true; }
    public void removeEndpoint(BlockPos pos) { endpoints.remove(pos); dirty = true; }

    public boolean contains(BlockPos pos) { return pipes.contains(pos) || endpoints.containsKey(pos); }
    public Set<BlockPos> getPipes() { return Collections.unmodifiableSet(pipes); }
    public Map<BlockPos, BlockEntity> getEndpoints() { return Collections.unmodifiableMap(endpoints); }
    public List<BlockPos> getTurbines() {
        List<BlockPos> t = new ArrayList<>();
        for (var e : endpoints.entrySet())
            if (e.getValue() instanceof com.xciel.steamturbine.content.turbine.SteamTurbineBlockEntity) t.add(e.getKey());
        return t;
    }

    public void enqueue(BlockPos entry, Direction from, SteamData steam) {
        if (steam != null && !steam.isEmpty()) queue.add(new QueueEntry(entry, from, steam));
    }

    public void tick() {
        if (dirty) rebuild();
        if (queue.isEmpty()) return;
        List<QueueEntry> batch = new ArrayList<>(queue);
        queue.clear();
        for (QueueEntry e : batch) propagate(e.entryPoint, e.fromDir, e.steam);
    }

    private void propagate(BlockPos src, Direction cameFrom, SteamData steam) {
        if (!steam.shouldPropagate()) return;
        Deque<BlockPos> bfs = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        bfs.add(src); visited.add(src);

        while (!bfs.isEmpty()) {
            BlockPos cur = bfs.poll();
            for (Direction dir : directionsFrom(cur)) {
                if (cur.equals(src) && dir == cameFrom) continue;
                BlockPos next = cur.relative(dir);
                if (visited.contains(next) || !contains(next)) continue;
                visited.add(next);

                boolean blocked = false;
                for (NetworkRule r : rules)
                    if (r.blocksPath(cur, dir, next, dir.getOpposite(), steam)) { blocked = true; break; }
                if (blocked) continue;

                if (endpoints.containsKey(next)) deliver(next, dir.getOpposite(), steam);
                if (pipes.contains(next)) bfs.add(next);
            }
        }
    }

    private void deliver(BlockPos to, Direction from, SteamData steam) {
        BlockEntity be = endpoints.get(to);
        if (be == null) return;
        if (be instanceof ISteamConsumer c && c.canReceive(from)) c.receiveSteam(from, steam);
        else if (be instanceof ISteamTransport t && t.canConnect(from)) t.pushSteam(from, steam);
    }

    private Set<Direction> directionsFrom(BlockPos pos) {
        Set<Direction> built = connections.get(pos);
        if (built != null) return built;

        // Lazy compute
        Set<Direction> dirs = new HashSet<>();
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (pipes.contains(n)) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof PressurizedPipeBlock && PressurizedPipeBlock.getConnection(state, d))
                    dirs.add(d);
            } else if (endpoints.containsKey(n) && canConnectEndpoint(n, d.getOpposite()))
                dirs.add(d);
        }
        // Also add directions for endpoints (e.g. turbines)
        if (endpoints.containsKey(pos)) {
            for (Direction d : Direction.values()) {
                BlockPos n = pos.relative(d);
                if (pipes.contains(n) || (endpoints.containsKey(n) && canConnectEndpoint(n, d.getOpposite())))
                    dirs.add(d);
            }
        }
        connections.put(pos, dirs);
        return dirs;
    }

    private boolean canConnectEndpoint(BlockPos ep, Direction from) {
        BlockEntity be = endpoints.get(ep);
        if (be instanceof ISteamEndpoint e) return e.canConnect(from);
        if (be instanceof ITurbineEndpoint e) return e.canTurbineConnect(from);
        return false;
    }

    private void rebuild() {
        connections.clear();
        dirty = false;
    }
}