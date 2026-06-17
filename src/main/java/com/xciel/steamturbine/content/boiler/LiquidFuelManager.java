package com.xciel.steamturbine.content.boiler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class LiquidFuelManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Map<Fluid, LiquidFuelData> FUEL_MAP = new HashMap<>();

    public LiquidFuelManager() {
        super(GSON, "steamturbine/liquid_fuel");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        FUEL_MAP.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            JsonElement json = entry.getValue();
            if (!json.isJsonObject()) continue;

            JsonObject obj = json.getAsJsonObject();

            if (!obj.has("fluid") || !obj.has("burnTime")) continue;

            String fluidStr = obj.get("fluid").getAsString();
            int burnTime = obj.get("burnTime").getAsInt();
            boolean superheated = obj.has("superheated") && obj.get("superheated").getAsBoolean();
            float heatLevel = obj.has("heatLevel") ? obj.get("heatLevel").getAsFloat() : 0f;
            float consumptionMultiplier = obj.has("consumptionMultiplier") ? obj.get("consumptionMultiplier").getAsFloat() : 1.0f;

            ResourceLocation fluidId = ResourceLocation.tryParse(fluidStr);
            if (fluidId == null) continue;

            Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
            if (fluid == null || fluid == BuiltInRegistries.FLUID.get(BuiltInRegistries.FLUID.getDefaultKey())) continue;

            if (burnTime <= 0) continue;

            FUEL_MAP.put(fluid, new LiquidFuelData(burnTime, superheated, heatLevel, consumptionMultiplier));
        }
    }

    @Nullable
    public static LiquidFuelData getData(Fluid fluid) {
        return FUEL_MAP.get(fluid);
    }

    public static int getBurnTime(Fluid fluid) {
        LiquidFuelData data = FUEL_MAP.get(fluid);
        return data != null ? data.burnTime() : 0;
    }

    public static boolean isSuperheated(Fluid fluid) {
        LiquidFuelData data = FUEL_MAP.get(fluid);
        return data != null && data.superheated();
    }

    public static float getHeatLevel(Fluid fluid) {
        LiquidFuelData data = FUEL_MAP.get(fluid);
        return data != null && data.hasHeatOverride() ? data.heatLevel() : 0f;
    }

    public static float getConsumptionMultiplier(Fluid fluid) {
        LiquidFuelData data = FUEL_MAP.get(fluid);
        return data != null ? data.consumptionMultiplier() : 1.0f;
    }

    public static boolean isLiquidFuel(Fluid fluid) {
        return FUEL_MAP.containsKey(fluid);
    }

    public static int getFuelCount() {
        return FUEL_MAP.size();
    }
}
