package com.xciel.steamturbine.content.dag;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

import net.createmod.catnip.lang.Lang;

public enum RedstoneLockMode implements INamedIconOptions {
    UNLOCKED(AllIcons.I_CONFIG_UNLOCKED),
    LOCKED(AllIcons.I_CONFIG_LOCKED);

    private final String translationKey;
    private final AllIcons icon;

    RedstoneLockMode(AllIcons icon) {
        this.icon = icon;
        this.translationKey = "steamturbine.dag.redstone_lock." + Lang.asId(name());
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }
}
