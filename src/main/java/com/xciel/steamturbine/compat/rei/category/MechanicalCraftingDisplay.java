package com.xciel.steamturbine.compat.rei.category;

import com.xciel.steamturbine.compat.rei.SteamTurbineREI;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public class MechanicalCraftingDisplay implements Display {

    private final RecipeHolder<CraftingRecipe> holder;
    private final List<EntryIngredient> inputs;
    private final List<EntryIngredient> outputs;

    public MechanicalCraftingDisplay(RecipeHolder<CraftingRecipe> holder) {
        this.holder = holder;
        CraftingRecipe recipe = holder.value();

        this.inputs = recipe.getIngredients().stream()
                .filter(ing -> !ing.isEmpty())
                .map(EntryIngredients::ofIngredient)
                .toList();

        ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        this.outputs = List.of(EntryIngredients.of(result));
    }

    public CraftingRecipe getRecipe() {
        return holder.value();
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return inputs;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return outputs;
    }

    @Override
    public me.shedaniel.rei.api.common.category.CategoryIdentifier<?> getCategoryIdentifier() {
        return SteamTurbineREI.MECHANICAL_CRAFTING;
    }
}