package com.xciel.turbines.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xciel.turbines.Turbines;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public enum STGuiTextures implements ScreenElement {

    NETWORK_DIAGNOSER("network_diagnoser", 198, 134);

    public final ResourceLocation location;
    public final int width;
    public final int height;
    public final int startX;
    public final int startY;
    public final int texWidth;
    public final int texHeight;

    STGuiTextures(String location, int width, int height) {
        this(location, 0, 0, width, height, width, height);
    }

    STGuiTextures(String location, int startX, int startY, int width, int height, int texWidth, int texHeight) {
        this.location = ResourceLocation.fromNamespaceAndPath(Turbines.MOD_ID, "textures/gui/" + location + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    @OnlyIn(Dist.CLIENT)
    public void bind() {
        RenderSystem.setShaderTexture(0, location);
    }

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, 0, startX, startY, width, height, texWidth, texHeight);
    }

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y, Color c) {
        bind();
        UIRenderHelper.drawColoredTexture(graphics, c, x, y, startX, startY, width, height);
    }

}