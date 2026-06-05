package com.xciel.steamturbine.compat.rei.category;

import com.xciel.steamturbine.compat.rei.SteamTurbineREI;
import com.xciel.steamturbine.registrate.STItems;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MechanicalCraftingCategory implements DisplayCategory<MechanicalCraftingDisplay> {

    @Override
    public me.shedaniel.rei.api.common.category.CategoryIdentifier<MechanicalCraftingDisplay> getCategoryIdentifier() {
        return SteamTurbineREI.MECHANICAL_CRAFTING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("steamturbine.jei.category.mechanical_crafting");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(new ItemStack(STItems.TURBINE_BLADE.get()));
    }

    @Override
    public int getDisplayHeight() {
        return 130;
    }

    @Override
    public int getDisplayWidth(MechanicalCraftingDisplay display) {
        return 200;
    }

    @Override
    public List<Widget> setupDisplay(MechanicalCraftingDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        // Mechanical crafting recipes are always 5x5
        int gridWidth = 5;
        int gridHeight = 5;

// Grid position offset (in slots)
        int slotOffsetX = -2;
        int slotOffsetY = 0;

        // Output/Arrow position offset from recipe base center (in pixels)
        int outputOffsetX = 65;
        int outputOffsetY = 0;

        int startX = bounds.x + (bounds.width - gridWidth * 18) / 2 + slotOffsetX * 18;
        int startY = bounds.y + (bounds.height - gridHeight * 18) / 2 + slotOffsetY;

        // Input slots
        List<EntryIngredient> inputs = display.getInputEntries();
        int inputIndex = 0;

        Ingredient[] ingredients = display.getRecipe().getIngredients().toArray(new Ingredient[0]);

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int slotIndex = y * gridWidth + x;
                if (slotIndex < ingredients.length && !ingredients[slotIndex].isEmpty()) {
                    int slotX = startX + x * 18;
                    int slotY = startY + y * 18;
                    widgets.add(Widgets.createSlot(new Point(slotX, slotY))
                            .entries(inputs.get(inputIndex))
                            .disableHighlight());
                    inputIndex++;
                }
            }
        }

        // Output slot - centered vertically relative to the grid
        int gridCenterY = startY + (gridHeight * 18) / 2;
        int outputX = bounds.x + bounds.width / 2 + outputOffsetX - 9;
        int outputY = gridCenterY + outputOffsetY - 9;
        widgets.add(Widgets.createResultSlotBackground(new Point(outputX, outputY)));
        widgets.add(Widgets.createSlot(new Point(outputX, outputY))
                .entries(display.getOutputEntries().get(0))
                .disableBackground()
                .markOutput());

        // Arrow - positioned to the left of output
        widgets.add(Widgets.createArrow(new Point(outputX - 30, outputY)));

        return widgets;
    }
}
