package com.github.meyllane.ninkaiEco.enums;

import com.github.meyllane.ninkaiEco.dataclass.Plot;

public enum PlotStatus {
    FREE(1, "Libre", false),
    RENT(2, "En location-vente", true),
    OWNED(3, "Acheté", true),
    NPC_OWNED(4, "Acheté (PNJ)", false);

    public final int id;
    public final String name;
    public final boolean canHavePlayerOwners;
    PlotStatus(int id, String name, boolean canHavePlayerOwners) {
        this.id = id;
        this.name = name;
        this.canHavePlayerOwners = canHavePlayerOwners;
    }

    public static PlotStatus getByID(int id) {
        for (PlotStatus p : values()) {
            if (p.id == id) return p;
        }
        return PlotStatus.FREE;
    }

    public static PlotStatus getByName(String name) {
        for (PlotStatus p : values()) {
            if (p.name.equals(name)) return p;
        }
        return PlotStatus.FREE;
    }
}
