package com.xciel.turbines.registrate;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.xciel.turbines.Turbines;

public class STRegistrate {
    private static CreateRegistrate instance;

    public static CreateRegistrate getInstance() {
        if (instance == null) {
            instance = CreateRegistrate.create(Turbines.MOD_ID);
        }
        return instance;
    }

    private STRegistrate() {}
}