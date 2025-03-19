package com.github.meyllane.ninkaiEco.enums;

public enum InstitutionDivision {

    NONE(1, "Aucune", "none"),
    TOWN_PLANNING(2, "Urbanisme", "urba");

    public final int id;
    public final String fullName;
    public final String shortName;

    InstitutionDivision(int id, String fullName, String shortName) {
        this.id = id;
        this.fullName = fullName;
        this.shortName = shortName;
    }

    public static InstitutionDivision getByID(int id) {
        for (InstitutionDivision div : values()) {
            if (div.id == id) return div;
        }
        return NONE;
    }
}
