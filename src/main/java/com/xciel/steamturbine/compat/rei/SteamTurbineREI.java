package com.xciel.steamturbine.compat.rei;

import com.xciel.steamturbine.SteamTurbine;
import com.xciel.steamturbine.compat.rei.category.MechanicalCraftingCategory;
import com.xciel.steamturbine.compat.rei.category.MechanicalCraftingDisplay;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

@REIPluginClient
public class SteamTurbineREI implements REIClientPlugin {

    public static final CategoryIdentifier<MechanicalCraftingDisplay> MECHANICAL_CRAFTING =
            CategoryIdentifier.of(SteamTurbine.rl("mechanical_crafting"));

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new MechanicalCraftingCategory());
        registry.addWorkstations(MECHANICAL_CRAFTING, EntryStacks.of(AllBlocks.MECHANICAL_CRAFTER.get()));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return;

        List<RecipeHolder<CraftingRecipe>> recipes = minecraft.getConnection().getRecipeManager().getAllRecipesFor(AllRecipeTypes.MECHANICAL_CRAFTING.getType());

        for (RecipeHolder<CraftingRecipe> holder : recipes) {
            if (SteamTurbine.MOD_ID.equals(holder.id().getNamespace())) {
                registry.add(new MechanicalCraftingDisplay(holder));
            }
        }
    }
}