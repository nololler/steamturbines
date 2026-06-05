package com.xciel.steamturbine.compat.emi;

import com.xciel.steamturbine.SteamTurbine;
import com.xciel.steamturbine.compat.emi.category.MechanicalCraftingEmiRecipe;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

@EmiEntrypoint
public class SteamTurbineEMI implements EmiPlugin {

    public static EmiRecipeCategory MECHANICAL_CRAFTING;

    @Override
    public void register(EmiRegistry registry) {
        MECHANICAL_CRAFTING = new EmiRecipeCategory(
                SteamTurbine.rl("mechanical_crafting"),
                EmiStack.of(AllBlocks.MECHANICAL_CRAFTER)
        );

        registry.addCategory(MECHANICAL_CRAFTING);
        registry.addWorkstation(MECHANICAL_CRAFTING, EmiStack.of(AllBlocks.MECHANICAL_CRAFTER));

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return;

        List<RecipeHolder<CraftingRecipe>> recipes = minecraft.getConnection().getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.MECHANICAL_CRAFTING.getType());

        for (RecipeHolder<CraftingRecipe> holder : recipes) {
            if (SteamTurbine.MOD_ID.equals(holder.id().getNamespace())) {
                registry.addRecipe(new MechanicalCraftingEmiRecipe(holder));
            }
        }
    }
}