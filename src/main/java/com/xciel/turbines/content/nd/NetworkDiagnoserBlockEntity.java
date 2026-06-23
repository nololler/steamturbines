package com.xciel.turbines.content.nd;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.gauge.GaugeBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class NetworkDiagnoserBlockEntity extends GaugeBlockEntity implements IHaveGoggleInformation {

    private static final float DEFAULT_MAX_SU = 64f;

    private float maxTestSU = DEFAULT_MAX_SU;
    private boolean stressTesting = false;

    public NetworkDiagnoserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {
        super.addBehaviours(list);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) return;

        if (networkDirty) {
            networkDirty = false;
            if (hasNetwork()) {
                getOrCreateNetwork().updateNetwork();
            }
        }
    }

    @Override
    public float calculateStressApplied() {
        if (stressTesting && hasNetwork()) {
            float speed = getTheoreticalSpeed();
            if (speed != 0) {
                return maxTestSU / Math.abs(speed);
            }
            return 0;
        }
        return super.calculateStressApplied();
    }

    @Override
    public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
        super.updateFromNetwork(maxStress, currentStress, networkSize);

        if (!com.simibubi.create.content.kinetics.base.IRotate.StressImpact.isEnabled()) {
            dialTarget = 0;
        } else if (maxStress == 0) {
            dialTarget = 0;
        } else {
            dialTarget = currentStress / maxStress;
        }

        color = getStressColor(dialTarget);
        sendData();
    }

    private int getStressColor(float ratio) {
        if (ratio <= 0) return 0x888888;
        if (ratio < 0.5f) {
            return mixColors(0x00FF00, 0xFFFF00, ratio * 2);
        } else if (ratio < 1f) {
            return mixColors(0xFFFF00, 0xFF8800, (ratio - 0.5f) * 2);
        } else {
            return 0xFF0000;
        }
    }

    private int mixColors(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        return (r << 16) | (g << 8) | b;
    }

    public void toggleStressTesting() {
        stressTesting = !stressTesting;
        if (hasNetwork()) {
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
            getOrCreateNetwork().updateNetwork();
        }
        setChanged();
        sendData();
    }

    public boolean isStressTesting() {
        return stressTesting;
    }

    public void setMaxTestSU(float su) {
        this.maxTestSU = su;
        if (stressTesting && hasNetwork()) {
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
        }
        setChanged();
        sendData();
    }

    public float getMaxTestSU() {
        return maxTestSU;
    }

    public float getNetworkStress() {
        return stress;
    }

    public float getNetworkCapacity() {
        return capacity;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    Network Diagnoser").withStyle(ChatFormatting.GOLD));

        double networkCapacity = this.capacity;
        double stressFraction = (networkCapacity > 0) ? stress / networkCapacity : 0;

        IRotate.SpeedLevel.getFormattedSpeedText(getSpeed(), false).forGoggles(tooltip);

        if (getTheoreticalSpeed() == 0) {
            CreateLang.text(TooltipHelper.makeProgressBar(3, 0))
                .translate("gui.stressometer.no_rotation")
                .style(ChatFormatting.DARK_GRAY)
                .forGoggles(tooltip);
        } else {
            IRotate.StressImpact.getFormattedStressText(stressFraction).forGoggles(tooltip);

            if (networkCapacity > 0) {
                CreateLang.translate("gui.stressometer.capacity")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);

                double remainingCapacity = networkCapacity - stress;
                IRotate.StressImpact stressLevel = IRotate.StressImpact.of(stressFraction);
                CreateLang.number((int) remainingCapacity)
                    .translate("generic.unit.stress")
                    .style(stressLevel.getRelativeColor())
                    .forGoggles(tooltip, 1);
            }
        }

        if (stressTesting) {
            tooltip.add(Component.literal("    Stress Test: ACTIVE").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("    Testing at: " + String.format("%.0f SU", maxTestSU)).withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("    Stress Test: OFF").withStyle(ChatFormatting.DARK_GRAY));
        }
        return true;
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        maxTestSU = tag.getFloat("MaxTestSU");
        stressTesting = tag.getBoolean("StressTesting");
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("MaxTestSU", maxTestSU);
        tag.putBoolean("StressTesting", stressTesting);
    }
}
