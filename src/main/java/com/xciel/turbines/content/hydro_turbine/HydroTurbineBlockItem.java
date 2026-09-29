package com.xciel.turbines.content.hydro_turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

/** Places the controller one block above the bottom of the 3-block-tall machine. */
public class HydroTurbineBlockItem extends BlockItem {

    public HydroTurbineBlockItem(HydroTurbineBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        BlockPos controllerPos = context.getClickedPos().above();
        BlockPlaceContext adjustedContext = BlockPlaceContext.at(context, controllerPos, context.getClickedFace());
        return super.place(adjustedContext);
    }
}
