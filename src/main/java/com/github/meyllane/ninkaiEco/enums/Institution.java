package com.github.meyllane.ninkaiEco.enums;

public enum Institution {

    NONE(1, "Aucune", "none"),
    DIPLOMATE(2, "Corps Diplomatique", "diplo"),
    ARTI(3, "Guilde des Artisans et Commerçants", "arti"),
    MED(4, "Corps Médical", "med"),
    ACADEMIE(5, "Académie", "aca"),
    ADMIN(6, "Administration", "admin");

    public final int id;
    public final String name;
    public final String shortName;

    Institution(int id, String name, String shortName) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
    }

    public static Institution getByID(int id) {
        for (Institution instit: values()) {
            if (instit.id == id) return instit;
        }
        return NONE;
    }

    public static Institution getByShortName(String shortName) {
        for (Institution instit: values()) {
            if (instit.shortName.equals(shortName)) return instit;
        }
        return NONE;
    }
}
