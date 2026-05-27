package com.xciel.steamturbine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xciel.steamturbine.steam.SteamPressureTier;
import com.xciel.steamturbine.steam.SteamType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;

public class PipePressureRenderHelper {
    public static float[] getPressureColor(SteamPressureTier tier) {
        return switch (tier) {
            case NONE -> new float[]{0.5f, 0.5f, 0.5f};
            case REGULAR -> new float[]{0.7f, 0.7f, 0.8f};
            case PRESSURED_1 -> new float[]{0.6f, 0.8f, 1.0f};
            case PRESSURED_2 -> new float[]{0.4f, 0.9f, 0.9f};
            case PRESSURED_3 -> new float[]{0.8f, 0.6f, 1.0f};
        };
    }

    public static float getEmissiveStrength(SteamPressureTier tier) {
        return switch (tier) {
            case NONE -> 0.0f;
            case REGULAR -> 0.1f;
            case PRESSURED_1 -> 0.3f;
            case PRESSURED_2 -> 0.5f;
            case PRESSURED_3 -> 0.8f;
        };
    }

    public static float getSteamGlowIntensity(SteamType type) {
        return switch (type) {
            case REGULAR -> 0.0f;
            case PRESSURIZED -> 0.2f;
            case SUPERHEATED -> 0.5f;
            case EXHAUST -> 0.3f;
        };
    }
}