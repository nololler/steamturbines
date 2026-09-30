package com.xciel.turbines.content.large_turbine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.xciel.turbines.Turbines;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class LargeTurbineRenderer extends KineticBlockEntityRenderer<LargeTurbineBlockEntity> {

    private static final PartialModel ROTOR = PartialModel.of(Turbines.rl("block/turbine/large_turbine_rotor"));

    public LargeTurbineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(LargeTurbineBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        SuperByteBuffer rotor = CachedBuffers.partial(ROTOR, be.getBlockState());
        var axis = KineticBlockEntityRenderer.getRotationAxisOf(be);
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(be, be.getBlockPos(), axis);
        float speed = be.getSpeed() * be.getRotorSpeedMultiplier();
        float angle = ((time * speed * 3f / 10 + offset) % 360) / 180 * (float) Math.PI;
        KineticBlockEntityRenderer.kineticRotationTransform(rotor, be, axis, angle, light)
            .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    public static void register() {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            com.xciel.turbines.AllBlockEntityTypes.LARGE_TURBINE.get(),
            LargeTurbineRenderer::new);
    }
}
