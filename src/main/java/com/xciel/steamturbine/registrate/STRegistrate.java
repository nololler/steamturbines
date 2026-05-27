package com.xciel.steamturbine.registrate;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.xciel.steamturbine.SteamTurbine;

public class STRegistrate {
    private static CreateRegistrate instance;

    public static CreateRegistrate getInstance() {
        if (instance == null) {
            instance = CreateRegistrate.create(SteamTurbine.MOD_ID);
        }
        return instance;
    }

    private STRegistrate() {}
}