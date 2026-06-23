package com.xciel.turbines.compat.emi.category;

import com.xciel.turbines.compat.emi.SteamTurbineEMI;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.List;

public class MechanicalCraftingEmiRecipe extends BasicEmiRecipe {

    private final RecipeHolder<CraftingRecipe> holder;

    public MechanicalCraftingEmiRecipe(RecipeHolder<CraftingRecipe> holder) {
        super(SteamTurbineEMI.MECHANICAL_CRAFTING, holder.id(), 200, 130);
        this.holder = holder;
        CraftingRecipe recipe = holder.value();

        this.inputs = recipe.getIngredients().stream()
                .filter(ing -> !ing.isEmpty())
                .map(EmiIngredient::of)
                .toList();

        ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        this.outputs = List.of(EmiStack.of(result));
    }

    public RecipeHolder<CraftingRecipe> getBackingRecipe() {
        return holder;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int gridWidth = 5;
        int gridHeight = 5;

        int slotOffsetX = -2;
        int slotOffsetY = 0;

        int outputOffsetX = 65;
        int outputOffsetY = 0;

        int startX = (widgets.getWidth() - gridWidth * 18) / 2 + slotOffsetX * 18;
        int startY = (widgets.getHeight() - gridHeight * 18) / 2 + slotOffsetY;

        var ingredients = holder.value().getIngredients();
        int inputIndex = 0;

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int slotIndex = y * gridWidth + x;
                if (slotIndex < ingredients.size() && !ingredients.get(slotIndex).isEmpty()) {
                    int slotX = startX + x * 18;
                    int slotY = startY + y * 18;
                    widgets.addSlot(inputs.get(inputIndex), slotX, slotY)
                            .drawBack(false);
                    inputIndex++;
                }
            }
        }

        int gridCenterY = startY + (gridHeight * 18) / 2;
        int outputX = widgets.getWidth() / 2 + outputOffsetX - 9;
        int outputY = gridCenterY + outputOffsetY - 9;

        widgets.addSlot(outputs.get(0), outputX, outputY)
                .drawBack(false)
                .recipeContext(this);
    }
}