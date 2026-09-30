package com.xciel.turbines.content.hydro_turbine;

import com.xciel.turbines.content.large_turbine.LargeTurbineBlock;
import com.xciel.turbines.content.reinforced_glass.ReinforcedGlassBlock;
import com.xciel.turbines.content.shaft.HydroTurbineShaftBlock;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Shared structure and intake-water discovery for Hydro Turbine chambers. */
public final class HydroTurbineChamber {

    public static final int MIN_STAGES = 1;
    public static final int MAX_STAGES = 10;
    public static final int SOURCE_WATER_PER_STAGE = 9;
    public static final int TANK_WATER_PER_STAGE = 9_000;
    public static final int MAX_SOURCE_WATER = SOURCE_WATER_PER_STAGE * MAX_STAGES;
    public static final int MAX_TANK_NETWORK_WATER = TANK_WATER_PER_STAGE * MAX_STAGES;

    private static final int CHAMBER_RADIUS = 2;
    private static final int MAX_WATER_POSITIONS_VISITED = 512;
    private static final int MAX_FLUID_NETWORK_NODES = 4096;

    private HydroTurbineChamber() {}

    public record Result(boolean structureValid, String status, int stages, int sourceWaterBlocks,
                         BlockPos topShaft, BlockPos bottomShaft, BlockPos inlet, BlockPos exhaust,
                         List<BlockPos> turbinePositions) {
        private static Result invalid(String status) {
            return new Result(false, status, 0, 0, null, null, null, null, List.of());
        }

        public boolean hasEnoughWater() {
            return structureValid && sourceWaterBlocks > 0;
        }

        /** The upper shaft owns a two-ended chamber; otherwise the lower shaft owns it. */
        public boolean isOutputOwner(BlockPos shaftPos) {
            if (topShaft != null)
                return topShaft.equals(shaftPos);
            return bottomShaft != null && bottomShaft.equals(shaftPos);
        }
    }

    public record FluidNetworkInfo(int tankCapacity, int waterAmount, boolean sharedHydroUnit,
                                   boolean unresolvedHydroUnit, List<BlockPos> tankControllers,
                                   int requiredTankWater) {
        public FluidNetworkInfo(int tankCapacity, int waterAmount, boolean sharedHydroUnit,
                                boolean unresolvedHydroUnit) {
            this(tankCapacity, waterAmount, sharedHydroUnit, unresolvedHydroUnit, List.of(), MAX_TANK_NETWORK_WATER);
        }

        public FluidNetworkInfo(int tankCapacity, int waterAmount, boolean sharedHydroUnit,
                                boolean unresolvedHydroUnit, List<BlockPos> tankControllers) {
            this(tankCapacity, waterAmount, sharedHydroUnit, unresolvedHydroUnit, tankControllers,
                MAX_TANK_NETWORK_WATER);
        }

        public FluidNetworkInfo {
            tankControllers = List.copyOf(tankControllers);
        }

        public boolean satisfiesTankInput() {
            return tankCapacity > 0 && waterAmount > 0
                && !sharedHydroUnit && !unresolvedHydroUnit;
        }
    }

    public record StageVisualInfo(float rotorSpeedMultiplier, boolean transparentWalls) {}

    public static int minimumSourceWater(int stages) {
        return SOURCE_WATER_PER_STAGE * Math.max(MIN_STAGES, Math.min(MAX_STAGES, stages));
    }

    public static int minimumTankNetworkWater(int stages) {
        return TANK_WATER_PER_STAGE * Math.max(MIN_STAGES, Math.min(MAX_STAGES, stages));
    }

    /** Stage 1 is nearest the roof; each stage below it spins 5% slower. */
    public static StageVisualInfo stageVisualInfo(LevelReader level, BlockPos turbinePos) {
        for (int offset = -MAX_STAGES - 1; offset <= MAX_STAGES + 1; offset++) {
            if (offset == 0)
                continue;
            BlockPos shaftPos = turbinePos.offset(0, offset, 0);
            if (!level.hasChunkAt(shaftPos)
                || !(level.getBlockState(shaftPos).getBlock() instanceof HydroTurbineShaftBlock))
                continue;

            Result chamber = inspect(level, shaftPos);
            if (!chamber.structureValid() || !chamber.turbinePositions().contains(turbinePos))
                continue;
            int topStageY = chamber.turbinePositions().stream().mapToInt(BlockPos::getY).max().orElse(turbinePos.getY());
            int stageFromRoof = topStageY - turbinePos.getY();
            return new StageVisualInfo(Math.max(0.05f, 1f - stageFromRoof * 0.05f),
                hasTransparentWalls(level, chamber));
        }
        return new StageVisualInfo(1f, false);
    }

    public static float rotorSpeedMultiplier(LevelReader level, BlockPos turbinePos) {
        return stageVisualInfo(level, turbinePos).rotorSpeedMultiplier();
    }

    public static boolean hasTransparentWalls(LevelReader level, Result chamber) {
        if (chamber == null || !chamber.structureValid())
            return false;
        // Deliberately checks only the 5x5 side-wall perimeter; shafts, IO blocks, roof, and floor
        // must not make an otherwise opaque chamber render the water column.
        for (BlockPos stagePos : chamber.turbinePositions()) {
            for (int dx = -CHAMBER_RADIUS; dx <= CHAMBER_RADIUS; dx++) {
                for (int dz = -CHAMBER_RADIUS; dz <= CHAMBER_RADIUS; dz++) {
                    if (Math.abs(dx) != CHAMBER_RADIUS && Math.abs(dz) != CHAMBER_RADIUS)
                        continue;
                    if (Math.abs(dx) == CHAMBER_RADIUS && Math.abs(dz) == CHAMBER_RADIUS)
                        continue;
                    BlockPos wallPos = stagePos.offset(dx, 0, dz);
                    BlockState wall = level.getBlockState(wallPos);
                    if (wall.getBlock() instanceof ReinforcedGlassBlock || !wall.isSolidRender(level, wallPos))
                        return true;
                }
            }
        }
        return false;
    }

    public static List<String> playerChecklist() {
        return List.of(
            "block.turbines.hydro_shaft.checklist.stages",
            "block.turbines.hydro_shaft.checklist.clearance",
            "block.turbines.hydro_shaft.checklist.walls",
            "block.turbines.hydro_shaft.checklist.roof",
            "block.turbines.hydro_shaft.checklist.floor",
            "block.turbines.hydro_shaft.checklist.io",
            "block.turbines.hydro_shaft.checklist.shafts"
        );
    }

    public static Result inspect(LevelReader level, BlockPos shaftPos) {
        if (!level.hasChunkAt(shaftPos))
            return Result.invalid("block.turbines.hydro_shaft.status.not_loaded");

        BlockState shaftState = level.getBlockState(shaftPos);
        if (!(shaftState.getBlock() instanceof HydroTurbineShaftBlock))
            return Result.invalid("block.turbines.hydro_shaft.status.missing_shaft");

        Direction facing = shaftState.getValue(HydroTurbineShaftBlock.FACING);
        if (facing != Direction.UP && facing != Direction.DOWN)
            return Result.invalid("block.turbines.hydro_shaft.status.shaft_vertical");

        boolean startsAtTop = facing == Direction.UP;
        Direction towardStages = startsAtTop ? Direction.DOWN : Direction.UP;
        List<BlockPos> stages = new ArrayList<>();
        for (int index = 1; index <= MAX_STAGES + 1; index++) {
            BlockPos candidate = shaftPos.relative(towardStages, index);
            if (!level.hasChunkAt(candidate))
                return Result.invalid("block.turbines.hydro_shaft.status.not_loaded");

            if (!(level.getBlockState(candidate).getBlock() instanceof LargeTurbineBlock))
                break;
            if (index > MAX_STAGES)
                return Result.invalid("block.turbines.hydro_shaft.status.too_many_stages");
            stages.add(candidate);
        }

        if (stages.isEmpty())
            return Result.invalid("block.turbines.hydro_shaft.status.missing_stages");

        BlockPos oppositeCap = shaftPos.relative(towardStages, stages.size() + 1);
        if (!level.hasChunkAt(oppositeCap))
            return Result.invalid("block.turbines.hydro_shaft.status.not_loaded");

        BlockPos topShaft = startsAtTop ? shaftPos : null;
        BlockPos bottomShaft = startsAtTop ? null : shaftPos;
        BlockState oppositeState = level.getBlockState(oppositeCap);
        if (oppositeState.getBlock() instanceof HydroTurbineShaftBlock) {
            Direction expected = startsAtTop ? Direction.DOWN : Direction.UP;
            if (oppositeState.getValue(HydroTurbineShaftBlock.FACING) != expected)
                return Result.invalid("block.turbines.hydro_shaft.status.dual_shaft_facing");
            if (startsAtTop)
                bottomShaft = oppositeCap;
            else
                topShaft = oppositeCap;
        }

        BlockPos topCap = startsAtTop ? shaftPos : oppositeCap;
        BlockPos bottomCap = startsAtTop ? oppositeCap : shaftPos;
        CapIO topIO = findIO(level, topCap, Direction.UP);
        CapIO bottomIO = findIO(level, bottomCap, Direction.DOWN);
        if (!topIO.valid())
            return Result.invalid("block.turbines.hydro_shaft.status.missing_inlet");
        if (!bottomIO.valid())
            return Result.invalid("block.turbines.hydro_shaft.status.missing_exhaust");

        if (!validateCap(level, topCap, topShaft != null, topIO.pos(), true))
            return Result.invalid("block.turbines.hydro_shaft.status.roof_blocks");
        if (!validateCap(level, bottomCap, bottomShaft != null, bottomIO.pos(), false))
            return Result.invalid("block.turbines.hydro_shaft.status.floor_blocks");

        for (BlockPos turbinePos : stages)
            if (!validateStageLayer(level, turbinePos))
                return Result.invalid("block.turbines.hydro_shaft.status.stage_walls");

        int water = countSourceWaterAbove(level, topIO.pos());
        int sourceRequirement = minimumSourceWater(stages.size());
        int tankRequirement = minimumTankNetworkWater(stages.size());
        String status = water > 0 ? "block.turbines.hydro_shaft.status.water_available"
            : "block.turbines.hydro_shaft.status.water_needed";

        return new Result(true, status, stages.size(), water, topShaft, bottomShaft,
            topIO.pos(), bottomIO.pos(), List.copyOf(stages));
    }

    public static int countSourceWaterAbove(LevelReader level, BlockPos inletPos) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        BlockPos start = inletPos.above();
        if (!level.hasChunkAt(start))
            return 0;
        queue.add(start);
        visited.add(start);
        int sourceCount = 0;

        while (!queue.isEmpty() && visited.size() <= MAX_WATER_POSITIONS_VISITED
            && sourceCount < MAX_SOURCE_WATER) {
            BlockPos current = queue.removeFirst();
            if (!level.hasChunkAt(current))
                continue;
            FluidState fluid = level.getFluidState(current);
            if (!isWater(fluid))
                continue;
            if (fluid.isSource())
                sourceCount++;

            for (Direction direction : Direction.values()) {
                BlockPos adjacent = current.relative(direction);
                if (adjacent.getY() <= inletPos.getY() || visited.size() >= MAX_WATER_POSITIONS_VISITED)
                    continue;
                if (!visited.add(adjacent) || !level.hasChunkAt(adjacent))
                    continue;
                if (isWater(level.getFluidState(adjacent)))
                    queue.addLast(adjacent);
            }
        }
        return Math.min(sourceCount, MAX_SOURCE_WATER);
    }

    /**
     * Measures an attached Create fluid network by walking pipes, pumps, tank handlers, and
     * multiblock tanks. Water stored in the two Hydro IO buffers is included as circulating water.
     */
    public static FluidNetworkInfo connectedFluidNetwork(Level level, BlockPos startIOPos) {
        if (level == null || !level.hasChunkAt(startIOPos))
            return new FluidNetworkInfo(0, 0, false, true);

        Set<BlockPos> hydroUnits = new HashSet<>();
        BlockPos startUnit = chamberKeyForIO(level, startIOPos);
        boolean unresolvedHydroIO = startUnit == null;
        int requiredTankWater = MAX_TANK_NETWORK_WATER;
        if (startUnit != null)
            hydroUnits.add(startUnit);
        if (startUnit != null) {
            Result startChamber = inspect(level, startUnit);
            if (startChamber.structureValid())
                requiredTankWater = minimumTankNetworkWater(startChamber.stages());
            else
                unresolvedHydroIO = true;
        }

        long totalCapacity = 0;
        long totalWater = 0;
        boolean networkIncomplete = false;
        List<BlockPos> tankControllers = new ArrayList<>();
        BlockEntity startEntity = level.getBlockEntity(startIOPos);
        if (startEntity instanceof HydroTurbineIOBlockEntity io)
            totalWater += io.getBufferedWaterAmount();

        Deque<FluidNetworkNode> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<IFluidHandler> countedHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
        visited.add(startIOPos);
        for (Direction direction : Direction.values())
            enqueueFluidNeighbor(level, queue, visited, startIOPos.relative(direction), direction.getOpposite());

        int nodesVisited = 0;
        while (!queue.isEmpty() && nodesVisited++ < MAX_FLUID_NETWORK_NODES
            && totalCapacity < Integer.MAX_VALUE && totalWater < Integer.MAX_VALUE) {
            FluidNetworkNode node = queue.removeFirst();
            BlockPos pos = node.pos();
            if (!level.hasChunkAt(pos))
                continue;

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof HydroTurbineIOBlockEntity io) {
                BlockPos unit = chamberKeyForIO(level, pos);
                if (unit == null)
                    unresolvedHydroIO = true;
                else
                    hydroUnits.add(unit);
                totalWater += io.getBufferedWaterAmount();
                continue;
            }

            IFluidHandler handler = fluidHandlerAt(level, pos, node.entryFace());
            if (handler != null && countedHandlers.add(handler)) {
                for (int tank = 0; tank < handler.getTanks(); tank++) {
                    totalCapacity += Math.max(0, handler.getTankCapacity(tank));
                    FluidStack fluid = handler.getFluidInTank(tank);
                    if (!fluid.isEmpty() && fluid.getFluid() == Fluids.WATER)
                        totalWater += fluid.getAmount();
                }
                if (blockEntity instanceof FluidTankBlockEntity tankBlockEntity) {
                    BlockPos controller = tankBlockEntity.getController();
                    if (!tankControllers.contains(controller))
                        tankControllers.add(controller);
                }
            }

            FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, pos);
            if (pipe != null) {
                BlockState state = level.getBlockState(pos);
                for (Direction direction : FluidPropagator.getPipeConnections(state, pipe)) {
                    BlockPos next = pos.relative(direction);
                    if (!level.hasChunkAt(next)) {
                        networkIncomplete = true;
                        continue;
                    }
                    enqueueFluidNeighbor(level, queue, visited, next, direction.getOpposite());
                }
            }

            if (handler != null) {
                enqueueConnectedPipesAroundHandler(level, queue, visited, pos);
                if (blockEntity instanceof FluidTankBlockEntity tank) {
                    BlockPos controller = tank.getController();
                    for (Direction direction : Direction.values()) {
                        BlockPos adjacent = pos.relative(direction);
                        if (!level.hasChunkAt(adjacent))
                            continue;
                        BlockEntity adjacentEntity = level.getBlockEntity(adjacent);
                        if (adjacentEntity instanceof FluidTankBlockEntity adjacentTank
                            && controller.equals(adjacentTank.getController()))
                            enqueueFluidNeighbor(level, queue, visited, adjacent, direction.getOpposite());
                    }
                }
            }
        }

        networkIncomplete |= !queue.isEmpty();
        return new FluidNetworkInfo((int) Math.min(Integer.MAX_VALUE, totalCapacity),
            (int) Math.min(Integer.MAX_VALUE, totalWater), hydroUnits.size() > 1,
            unresolvedHydroIO || networkIncomplete, tankControllers, requiredTankWater);
    }

    private static void enqueueFluidNeighbor(Level level, Deque<FluidNetworkNode> queue, Set<BlockPos> visited,
                                             BlockPos pos, Direction entryFace) {
        if (visited.contains(pos) || !level.hasChunkAt(pos))
            return;
        FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, pos);
        boolean pipeConnected = pipe != null && pipe.canHaveFlowToward(level.getBlockState(pos), entryFace);
        if (pipeConnected
            || level.getBlockEntity(pos) instanceof HydroTurbineIOBlockEntity
            || fluidHandlerAt(level, pos, entryFace) != null) {
            visited.add(pos);
            queue.addLast(new FluidNetworkNode(pos, entryFace));
        }
    }

    private static void enqueueConnectedPipesAroundHandler(Level level, Deque<FluidNetworkNode> queue,
                                                            Set<BlockPos> visited, BlockPos handlerPos) {
        for (Direction direction : Direction.values()) {
            BlockPos pipePos = handlerPos.relative(direction);
            if (visited.contains(pipePos) || !level.hasChunkAt(pipePos))
                continue;
            FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, pipePos);
            if (pipe == null || !pipe.canHaveFlowToward(level.getBlockState(pipePos), direction.getOpposite()))
                continue;
            enqueueFluidNeighbor(level, queue, visited, pipePos, direction.getOpposite());
        }
    }

    private static IFluidHandler fluidHandlerAt(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    private static BlockPos chamberKeyForIO(LevelReader level, BlockPos ioPos) {
        for (int dy = -MAX_STAGES - 1; dy <= MAX_STAGES + 1; dy++) {
            for (Direction horizontal : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
                BlockPos candidate = ioPos.relative(horizontal).offset(0, dy, 0);
                if (!level.hasChunkAt(candidate)
                    || !(level.getBlockState(candidate).getBlock() instanceof HydroTurbineShaftBlock))
                    continue;
                Result result = inspect(level, candidate);
                if (!result.structureValid() || (!ioPos.equals(result.inlet()) && !ioPos.equals(result.exhaust())))
                    continue;
                return result.topShaft() != null ? result.topShaft() : result.bottomShaft();
            }
        }
        return null;
    }

    private static boolean validateStageLayer(LevelReader level, BlockPos turbinePos) {
        for (int dx = -CHAMBER_RADIUS; dx <= CHAMBER_RADIUS; dx++) {
            for (int dz = -CHAMBER_RADIUS; dz <= CHAMBER_RADIUS; dz++) {
                if (Math.abs(dx) == CHAMBER_RADIUS && Math.abs(dz) == CHAMBER_RADIUS)
                    continue;
                BlockPos pos = turbinePos.offset(dx, 0, dz);
                if (!level.hasChunkAt(pos))
                    return false;
                BlockState state = level.getBlockState(pos);
                boolean wall = Math.abs(dx) == CHAMBER_RADIUS || Math.abs(dz) == CHAMBER_RADIUS;
                if (wall) {
                    if (!isAcceptableChamberWall(level, pos, state))
                        return false;
                } else if (dx == 0 && dz == 0) {
                    if (!(state.getBlock() instanceof LargeTurbineBlock))
                        return false;
                } else if (!isAirOrWater(state)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean validateCap(LevelReader level, BlockPos capCenter, boolean hasShaft, BlockPos ioPos,
                                       boolean allowBottomSlabs) {
        for (int dx = -CHAMBER_RADIUS; dx <= CHAMBER_RADIUS; dx++) {
            for (int dz = -CHAMBER_RADIUS; dz <= CHAMBER_RADIUS; dz++) {
                if (Math.abs(dx) == CHAMBER_RADIUS && Math.abs(dz) == CHAMBER_RADIUS)
                    continue;
                BlockPos pos = capCenter.offset(dx, 0, dz);
                if (!level.hasChunkAt(pos))
                    return false;
                if (pos.equals(ioPos))
                    continue;
                if (dx == 0 && dz == 0 && hasShaft)
                    continue;
                if (!isStructuralCapBlock(level, pos, allowBottomSlabs))
                    return false;
            }
        }
        return true;
    }

    private static boolean isAcceptableChamberWall(LevelReader level, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty())
            return false;

        VoxelShape shape = state.getCollisionShape(level, pos);
        for (Direction direction : Direction.values()) {
            if (state.getBlock().isFlammable(state, level, pos, direction)
                || !Block.isFaceFull(shape, direction))
                return false;
        }

        if (state.getBlock() instanceof ReinforcedGlassBlock)
            return true;
        float stoneHardness = Blocks.STONE.defaultBlockState().getDestroySpeed(level, pos);
        float wallHardness = state.getDestroySpeed(level, pos);
        return wallHardness < 0 || wallHardness >= stoneHardness;
    }

    private static CapIO findIO(LevelReader level, BlockPos capCenter, Direction role) {
        BlockPos found = null;
        for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            BlockPos candidate = capCenter.relative(direction);
            if (!level.hasChunkAt(candidate))
                return new CapIO(false, null);
            BlockState state = level.getBlockState(candidate);
            if (!(state.getBlock() instanceof HydroTurbineIOBlock))
                continue;
            if (state.getValue(HydroTurbineIOBlock.FACING) != role || found != null)
                return new CapIO(false, null);
            found = candidate;
        }
        return new CapIO(found != null, found);
    }

    private static boolean isStructuralCapBlock(LevelReader level, BlockPos pos, boolean allowBottomSlabs) {
        BlockState state = level.getBlockState(pos);
        if (allowBottomSlabs && state.getBlock() instanceof SlabBlock
            && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM)
            return state.getFluidState().isEmpty()
                && isNonflammable(level, pos, state) && hasStoneHardness(level, pos, state);
        if (state.isAir() || !state.getFluidState().isEmpty() || !state.isSolidRender(level, pos))
            return false;

        VoxelShape shape = state.getCollisionShape(level, pos);
        for (Direction direction : Direction.values()) {
            if (state.getBlock().isFlammable(state, level, pos, direction))
                return false;
            if (!Block.isFaceFull(shape, direction))
                return false;
        }
        return true;
    }

    private static boolean isNonflammable(LevelReader level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values())
            if (state.getBlock().isFlammable(state, level, pos, direction))
                return false;
        return true;
    }

    private static boolean hasStoneHardness(LevelReader level, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(level, pos);
        return hardness < 0 || hardness >= Blocks.STONE.defaultBlockState().getDestroySpeed(level, pos);
    }

    private static boolean isAirOrWater(BlockState state) {
        return state.isAir() || isWater(state.getFluidState());
    }

    private static boolean isWater(FluidState state) {
        return state.getType() == Fluids.WATER || state.getType() == Fluids.FLOWING_WATER;
    }

    private record CapIO(boolean valid, BlockPos pos) {}
    private record FluidNetworkNode(BlockPos pos, Direction entryFace) {}
}
