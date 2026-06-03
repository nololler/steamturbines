package com.xciel.steamturbine.content.boiler;

import com.simibubi.create.AllTags;
import com.simibubi.create.api.data.datamaps.BlazeBurnerFuel;
import com.simibubi.create.api.registry.CreateDataMaps;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.xciel.steamturbine.content.transport.pipe.PressurizedPipeBlock;
import com.xciel.steamturbine.steam.SteamConstants;
import com.xciel.steamturbine.steam.SteamData;
import com.xciel.steamturbine.steam.SteamType;
import com.xciel.steamturbine.steam.transfer.ISteamConsumer;
import com.xciel.steamturbine.steam.transfer.ISteamEndpoint;
import com.xciel.steamturbine.steam.transfer.ISteamProducer;
import com.xciel.steamturbine.steam.transfer.ISteamTransport;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SteamBoilerBlockEntity extends SmartBlockEntity implements ISteamEndpoint, ISteamConsumer, ISteamTransport, ISteamProducer, IHaveGoggleInformation {
    private static final int WATER_TANK_CAPACITY = 2000;
    private static final int WATER_PER_STEAM = 5;
    private static final int INFINITE_THRESHOLD = 20 * 3600 * 24 * 30;

    private final EnumMap<Direction, Boolean> connections = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, SteamData> receivedSteam = new EnumMap<>(Direction.class);
    private SteamData outputSteam = SteamData.empty();

    private final ItemStackHandler fuelInventory;
    private final FluidTank waterTank;
    private final IFluidHandler waterHandler;
    private int remainingBurnTime;
    private int clientRemainingBurnTime;
    private int clientTotalBurnTime;
    private FuelType activeFuel;
    private float heatLevel;
    private boolean boilerActive;

    private enum FuelType {
        NONE,
        NORMAL,
        SPECIAL
    }

    public SteamBoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
        for (Direction dir : Direction.values()) {
            connections.put(dir, false);
            receivedSteam.put(dir, SteamData.empty());
        }
        fuelInventory = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return getFuelBurnTime(stack) > 0;
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        waterTank = new FluidTank(WATER_TANK_CAPACITY, fluidStack -> fluidStack.getFluid() == Fluids.WATER) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        waterHandler = waterTank;
        remainingBurnTime = 0;
        clientRemainingBurnTime = 0;
        clientTotalBurnTime = 0;
        activeFuel = FuelType.NONE;
        heatLevel = 0f;
        boilerActive = false;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            spawnOutputParticles();
            clientVisualUpdate();
        } else {
            updateFuel();
            updateHeat();
            generateSteam();
            pushSteamOutput();
            updateConnectionStates();
            for (Direction dir : Direction.values()) {
                receivedSteam.put(dir, SteamData.empty());
            }
        }
    }

    private void spawnOutputParticles() {
        if (level == null || !outputSteam.shouldPropagate()) return;
        Direction outputDir = getOutputDirection();
        BlockPos outputPos = worldPosition.relative(outputDir);
        if (!level.isLoaded(outputPos)) return;
        var block = level.getBlockState(outputPos).getBlock();
        if (block instanceof PressurizedPipeBlock || block instanceof SteamBoilerBlock) return;
        float distance = Math.min(outputSteam.getPressure() * outputSteam.getThroughput() * 0.05f, 1.5f);
        if (distance < 0.05f) return;
        if (level.random.nextInt(3) != 0) return;
        Vec3 normal = Vec3.atLowerCornerOf(outputDir.getNormal());
        Vec3 offset = normal.scale(0.5).add(0.5, 0.5, 0.5);
        double speed = distance * 0.05;
        level.addParticle(ParticleTypes.LARGE_SMOKE,
            worldPosition.getX() + offset.x + (level.random.nextDouble() - 0.5) * 0.1,
            worldPosition.getY() + offset.y + (level.random.nextDouble() - 0.5) * 0.1,
            worldPosition.getZ() + offset.z + (level.random.nextDouble() - 0.5) * 0.1,
            normal.x * speed, normal.y * speed * 0.3, normal.z * speed);
    }

    private void updateFuel() {
        ItemStack fuelStack = fuelInventory.getStackInSlot(0);

        if (remainingBurnTime > 0) {
            remainingBurnTime--;
            if (remainingBurnTime == 0) {
                activeFuel = FuelType.NONE;
                if (!fuelStack.isEmpty()) {
                    tryConsumeFuel();
                }
            }
        } else {
            if (!fuelStack.isEmpty()) {
                tryConsumeFuel();
            } else {
                activeFuel = FuelType.NONE;
            }
        }

        if (remainingBurnTime <= 0) {
            activeFuel = FuelType.NONE;
        }

        if (fuelStack.isEmpty() && remainingBurnTime <= 0) {
            activeFuel = FuelType.NONE;
        }
    }

    @SuppressWarnings("removal")
    private void tryConsumeFuel() {
        if (remainingBurnTime > 0) return;

        ItemStack fuelStack = fuelInventory.getStackInSlot(0);
        if (fuelStack.isEmpty()) return;

        int burnTime = getFuelBurnTime(fuelStack);
        if (burnTime <= 0) return;

        fuelStack.shrink(1);
        if (fuelStack.isEmpty()) {
            fuelInventory.setStackInSlot(0, ItemStack.EMPTY);
        } else {
            fuelInventory.setStackInSlot(0, fuelStack);
        }
        remainingBurnTime = burnTime;

        var holder = fuelStack.getItem().builtInRegistryHolder();
        BlazeBurnerFuel superheated = holder.getData(CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS);
        BlazeBurnerFuel regular = holder.getData(CreateDataMaps.REGULAR_BLAZE_BURNER_FUELS);
        if (superheated != null || regular != null) {
            activeFuel = FuelType.SPECIAL;
        } else if (AllTags.AllItemTags.BLAZE_BURNER_FUEL_SPECIAL.matches(fuelStack)) {
            activeFuel = FuelType.SPECIAL;
        } else {
            activeFuel = FuelType.NORMAL;
        }
        setChanged();
        sendData();
    }

    private int getBurnTime(ItemStack stack) {
        return stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
    }

    public boolean isCurrentFuelInfinite() {
        return this.remainingBurnTime >= INFINITE_THRESHOLD;
    }

    public boolean isTotalFuelInfinite() {
        return this.isCurrentFuelInfinite() || getItemBurnTime(this.fuelInventory.getStackInSlot(0)) >= INFINITE_THRESHOLD;
    }

    public int getCurrentBurnTime() {
        return this.remainingBurnTime;
    }

    public int getItemBurnTime(ItemStack stack) {
        return getBurnTime(stack);
    }

    public int getTotalBurnTime() {
        return this.remainingBurnTime + (this.fuelInventory.getStackInSlot(0).getCount() * getItemBurnTime(this.fuelInventory.getStackInSlot(0)));
    }

    @SuppressWarnings("removal")
    private int getFuelBurnTime(ItemStack stack) {
        var holder = stack.getItem().builtInRegistryHolder();

        BlazeBurnerFuel superheatedFuel = holder.getData(CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS);
        if (superheatedFuel != null) return superheatedFuel.burnTime();

        BlazeBurnerFuel normalFuel = holder.getData(CreateDataMaps.REGULAR_BLAZE_BURNER_FUELS);
        if (normalFuel != null) return normalFuel.burnTime();

        if (AllTags.AllItemTags.BLAZE_BURNER_FUEL_SPECIAL.matches(stack)) return 3200;

        int vanillaBurn = getBurnTime(stack);
        if (vanillaBurn > 0) return vanillaBurn;

        if (AllTags.AllItemTags.BLAZE_BURNER_FUEL_REGULAR.matches(stack)) return 1600;

        return 0;
    }

    private void updateHeat() {
        float targetHeat = 0f;

        if (activeFuel == FuelType.SPECIAL) {
            targetHeat = SteamConstants.HEAT_LEVEL_MAX;
        } else if (activeFuel == FuelType.NORMAL) {
            targetHeat = SteamConstants.HEAT_LEVEL_NORMAL;
        }

        BlockState below = level.getBlockState(worldPosition.below());
        float blockHeat = getBlockHeat(below);
        targetHeat = Math.max(targetHeat, blockHeat);

        float lerpSpeed = targetHeat > heatLevel ? 0.15f : 0.08f;
        heatLevel += (targetHeat - heatLevel) * lerpSpeed;

        if (heatLevel < 0.05f) heatLevel = 0f;
        if (heatLevel > SteamConstants.HEAT_LEVEL_MAX) heatLevel = SteamConstants.HEAT_LEVEL_MAX;
    }

    private float getBlockHeat(BlockState state) {
        if (state.is(Blocks.LAVA)) return 1.5f;
        if (state.is(Blocks.MAGMA_BLOCK)) return 1.0f;
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) return 0.5f;
        if (state.is(BlockTags.CAMPFIRES) && state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT))
            return 0.5f;
        if (state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)) {
            return switch (state.getValue(BlazeBurnerBlock.HEAT_LEVEL)) {
                case SEETHING -> SteamConstants.HEAT_LEVEL_MAX;
                case KINDLED -> SteamConstants.HEAT_LEVEL_NORMAL;
                case FADING -> 1.5f;
                case SMOULDERING -> 0.5f;
                default -> 0f;
            };
        }
        return 0f;
    }

    private void generateSteam() {
        boolean hasWater = waterTank.getFluidAmount() >= WATER_PER_STEAM;
        boolean hasHeat = heatLevel >= SteamConstants.MIN_HEAT_FOR_STEAM;
        boilerActive = hasHeat && hasWater;

        if (boilerActive) {
            waterTank.drain(WATER_PER_STEAM, IFluidHandler.FluidAction.EXECUTE);
            float throughput = heatLevel * SteamConstants.BASE_THROUGHPUT_PER_TICK;
            outputSteam = SteamData.of(heatLevel, SteamType.REGULAR, 1f, 1f, throughput);
        } else {
            outputSteam = SteamData.empty();
        }
        setChanged();
        sendData();
    }

    private void pushSteamOutput() {
        Direction outputDir = getOutputDirection();
        if (outputDir == null || !outputSteam.shouldPropagate()) return;

        BlockPos neighborPos = worldPosition.relative(outputDir);
        if (!level.isLoaded(neighborPos)) return;

        var neighbor = level.getBlockEntity(neighborPos);
        SteamData toSend = outputSteam.withPropagationLoss();
        if (!toSend.shouldPropagate()) return;

        if (neighbor instanceof ISteamTransport transport) {
            transport.pushSteam(outputDir.getOpposite(), toSend);
        } else if (neighbor instanceof ISteamConsumer consumer) {
            if (consumer.canReceive(outputDir.getOpposite())) {
                consumer.receiveSteam(outputDir.getOpposite(), toSend);
            }
        }
    }

    private void clientVisualUpdate() {
    }

    public void updateConnectionStates() {
        if (level == null) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);
            Block neighborBlock = neighborState.getBlock();

            boolean connected = isValidConnection(neighborBlock, dir);
            connections.put(dir, connected);
        }
        setChanged();
    }

    private boolean isValidConnection(Block block, Direction dir) {
        if (block instanceof PressurizedPipeBlock) return true;
        if (block instanceof SteamBoilerBlock) return true;
        return false;
    }

    public IItemHandler getItemHandler() {
        return fuelInventory;
    }

    public IFluidHandler getFluidHandler() {
        return waterHandler;
    }

    public Direction getOutputDirection() {
        return getBlockState().getValue(SteamBoilerBlock.FACING);
    }

    public boolean isOutputSide(Direction dir) {
        return dir == getOutputDirection();
    }

    public boolean hasConnection(Direction dir) {
        return connections.getOrDefault(dir, false);
    }

    public float getHeatLevel() {
        return heatLevel;
    }

    public boolean isActive() {
        return boilerActive;
    }

    public int getRemainingBurnTime() {
        return remainingBurnTime;
    }

    public FuelType getActiveFuel() {
        return activeFuel;
    }

    public int getWaterAmount() {
        return waterTank.getFluidAmount();
    }

    public SteamData getOutputSteam() {
        return outputSteam;
    }

    public int getNextFuelBurnTime() {
        ItemStack stack = fuelInventory.getStackInSlot(0);
        if (stack.isEmpty()) return 0;
        return getFuelBurnTime(stack);
    }

    // ISteamEndpoint
    @Override
    public boolean canConnect(Direction direction) {
        return isOutputSide(direction);
    }

    // ISteamConsumer
    @Override
    public void receiveSteam(Direction direction, SteamData steam) {
        if (steam.isEmpty()) return;
        receivedSteam.put(direction, steam);
        setChanged();
    }

    @Override
    public boolean canReceive(Direction direction) {
        return false;
    }

    @Override
    public float getMaxReceiveRate(Direction direction) {
        return 0f;
    }

    // ISteamTransport
    @Override
    public void pushSteam(Direction direction, SteamData steam) {
        receiveSteam(direction, steam);
    }

    @Override
    public SteamData pullSteam(Direction direction, float amount) {
        return SteamData.empty();
    }

    @Override
    public float getFlowRate(Direction direction) {
        return outputSteam.getThroughput();
    }

    // ISteamProducer
    @Override
    public SteamData produceSteam(Direction from) {
        if (outputSteam.isEmpty()) return SteamData.empty();
        return outputSteam;
    }

    @Override
    public float getMaxProduceRate(Direction from) {
        return isOutputSide(from) ? 100f : 0f;
    }

    @Override
    public boolean canProduce(Direction direction) {
        return isOutputSide(direction);
    }

    // IHaveGoggleInformation
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("   ").append(Component.translatable("steamturbine.goggles.boiler.header")
                .withStyle(ChatFormatting.GOLD)));

        if (boilerActive) {
            tooltip.add(Component.literal("  ").append(Component.translatable("steamturbine.goggles.boiler.status.active")
                    .withStyle(ChatFormatting.GREEN)));
        } else if (clientRemainingBurnTime > 0) {
            tooltip.add(Component.literal("   ").append(Component.translatable("steamturbine.goggles.boiler.status.heating")
                    .withStyle(ChatFormatting.YELLOW)));
        } else {
            tooltip.add(Component.literal("  ").append(Component.translatable("steamturbine.goggles.boiler.status.inactive")
                    .withStyle(ChatFormatting.GRAY)));
        }

        ItemStack fuelStack = fuelInventory.getStackInSlot(0);
        if (!fuelStack.isEmpty()) {
            tooltip.add(Component.literal("   ").append(Component.translatable("steamturbine.goggles.boiler.fuel",
                            fuelStack.getHoverName(),
                            fuelStack.getCount())
                    .withStyle(ChatFormatting.GRAY)));
            if (clientRemainingBurnTime > 0) {
                if (isTotalFuelInfinite()) {
                    tooltip.add(Component.literal("    ").append(Component.translatable("steamturbine.goggles.boiler.burn_time_infinite")
                            .withStyle(ChatFormatting.DARK_GRAY)));
                } else {
                    tooltip.add(Component.literal("  ").append(Component.translatable("steamturbine.goggles.boiler.burn_time",
                                    formatTime(clientRemainingBurnTime),
                                    formatTime(clientTotalBurnTime))
                            .withStyle(ChatFormatting.DARK_GRAY)));
                }
            }
        } else if (clientRemainingBurnTime > 0) {
            tooltip.add(Component.literal("   ").append(Component.translatable("steamturbine.goggles.boiler.fuel_empty")
                    .withStyle(ChatFormatting.DARK_GRAY)));
            if (isCurrentFuelInfinite()) {
                tooltip.add(Component.literal("    ").append(Component.translatable("steamturbine.goggles.boiler.burn_time_infinite")
                        .withStyle(ChatFormatting.DARK_GRAY)));
            } else {
                tooltip.add(Component.literal("  ").append(Component.translatable("steamturbine.goggles.boiler.burn_time",
                                formatTime(clientRemainingBurnTime),
                                formatTime(clientRemainingBurnTime))
                        .withStyle(ChatFormatting.DARK_GRAY)));
            }
        } else {
            tooltip.add(Component.literal("   ").append(Component.translatable("steamturbine.goggles.boiler.fuel_empty")
                    .withStyle(ChatFormatting.DARK_GRAY)));
        }

        tooltip.add(Component.literal("  ").append(Component.translatable("steamturbine.goggles.boiler.water",
                        waterTank.getFluidAmount(),
                        WATER_TANK_CAPACITY)
                .withStyle(ChatFormatting.GRAY)));

        if (outputSteam.shouldPropagate()) {
            tooltip.add(Component.literal("  ").append(Component.translatable("steamturbine.goggles.boiler.steam_output",
                            String.format("%.1f", outputSteam.getPressure()),
                            String.format("%.2f", outputSteam.getThroughput()))
                    .withStyle(ChatFormatting.AQUA)));
        }

        tooltip.add(Component.literal("   ").append(Component.translatable("steamturbine.goggles.boiler.heat",
                        String.format("%.1f", heatLevel))
                .withStyle(ChatFormatting.RED)));

        return true;
    }

    private String formatTime(int ticks) {
        int seconds = ticks / 20;
        String s = "";
        int min = seconds / 60;
        final int hour = min / 60;
        seconds = Math.floorMod(seconds, 60);
        min = Math.floorMod(min, 60);
        if (hour > 0) {
            s += hour + "h ";
        }
        if (min < 10 && hour > 0) {
            s += 0;
        }
        if (min > 0 || hour > 0) {
            s += min + "m ";
        }
        if (seconds < 10 && min > 0) {
            s += 0;
        }
        s += seconds + "s";
        return s;
    }

    // NBT
    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        outputSteam = SteamData.loadFromNBT(tag, registries);
        remainingBurnTime = tag.getInt("RemainingBurnTime");
        if (clientPacket) {
            clientRemainingBurnTime = tag.getInt("ClientRemainingBurnTime");
            clientTotalBurnTime = tag.getInt("ClientTotalBurnTime");
        }
        activeFuel = FuelType.valueOf(tag.contains("ActiveFuel") ? tag.getString("ActiveFuel") : "NONE");
        heatLevel = tag.getFloat("HeatLevel");
        boilerActive = tag.getBoolean("BoilerActive");
        fuelInventory.deserializeNBT(registries, tag.getCompound("FuelInventory"));
        waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        for (Direction dir : Direction.values()) {
            connections.put(dir, tag.getBoolean("conn_" + dir.getName()));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        outputSteam.saveToNBT(tag, registries);
        tag.putInt("RemainingBurnTime", remainingBurnTime);
        tag.putInt("ClientRemainingBurnTime", remainingBurnTime);
        tag.putInt("ClientTotalBurnTime", getTotalBurnTime());
        tag.putString("ActiveFuel", activeFuel.name());
        tag.putFloat("HeatLevel", heatLevel);
        tag.putBoolean("BoilerActive", boilerActive);
        tag.put("FuelInventory", fuelInventory.serializeNBT(registries));
        CompoundTag waterTag = new CompoundTag();
        waterTank.writeToNBT(registries, waterTag);
        tag.put("WaterTank", waterTag);
        for (Direction dir : Direction.values()) {
            tag.putBoolean("conn_" + dir.getName(), connections.getOrDefault(dir, false));
        }
    }
}
