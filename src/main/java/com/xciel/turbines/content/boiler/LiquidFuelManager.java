package com.xciel.turbines.content.boiler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class LiquidFuelManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Map<Fluid, LiquidFuelData> FUEL_MAP = new HashMap<>();
    private static final Map<TagKey<Fluid>, LiquidFuelData> TAG_FUEL_MAP = new HashMap<>();

    public LiquidFuelManager() {
        super(GSON, "turbines/liquid_fuel");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        FUEL_MAP.clear();
        TAG_FUEL_MAP.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            JsonElement json = entry.getValue();
            if (!json.isJsonObject()) continue;

            JsonObject obj = json.getAsJsonObject();

            if ((!obj.has("fluid") && !obj.has("tag")) || !obj.has("burnTime")) continue;

            int burnTime = obj.get("burnTime").getAsInt();
            if (burnTime <= 0) continue;

            boolean superheated = obj.has("superheated") && obj.get("superheated").getAsBoolean();
            float heatLevel = obj.has("heatLevel") ? obj.get("heatLevel").getAsFloat() : 0f;
            float consumptionMultiplier = obj.has("consumptionMultiplier") ? obj.get("consumptionMultiplier").getAsFloat() : 1.0f;

            LiquidFuelData data = new LiquidFuelData(burnTime, superheated, heatLevel, consumptionMultiplier);

            if (obj.has("tag")) {
                String tagStr = obj.get("tag").getAsString();
                ResourceLocation tagId = ResourceLocation.tryParse(tagStr);
                if (tagId != null) {
                    TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
                    TAG_FUEL_MAP.put(tagKey, data);
                }
            }

            if (obj.has("fluid")) {
                String fluidStr = obj.get("fluid").getAsString();
                ResourceLocation fluidId = ResourceLocation.tryParse(fluidStr);
                if (fluidId != null) {
                    Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
                    if (fluid != null && fluid != BuiltInRegistries.FLUID.get(BuiltInRegistries.FLUID.getDefaultKey())) {
                        FUEL_MAP.put(fluid, data);
                    }
                }
            }
        }
    }

    @Nullable
    private static LiquidFuelData getTagData(Fluid fluid) {
        for (var entry : TAG_FUEL_MAP.entrySet()) {
            if (fluid.is(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Nullable
    public static LiquidFuelData getData(Fluid fluid) {
        LiquidFuelData data = FUEL_MAP.get(fluid);
        if (data != null) return data;
        return getTagData(fluid);
    }

    public static int getBurnTime(Fluid fluid) {
        LiquidFuelData data = getData(fluid);
        return data != null ? data.burnTime() : 0;
    }

    public static boolean isSuperheated(Fluid fluid) {
        LiquidFuelData data = getData(fluid);
        return data != null && data.superheated();
    }

    public static float getHeatLevel(Fluid fluid) {
        LiquidFuelData data = getData(fluid);
        return data != null && data.hasHeatOverride() ? data.heatLevel() : 0f;
    }

    public static float getConsumptionMultiplier(Fluid fluid) {
        LiquidFuelData data = getData(fluid);
        return data != null ? data.consumptionMultiplier() : 1.0f;
    }

    public static boolean isLiquidFuel(Fluid fluid) {
        if (FUEL_MAP.containsKey(fluid)) return true;
        return getTagData(fluid) != null;
    }

    public static int getFuelCount() {
        return FUEL_MAP.size();
    }
}
