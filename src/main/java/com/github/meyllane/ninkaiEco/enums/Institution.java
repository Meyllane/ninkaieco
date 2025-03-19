package com.github.meyllane.ninkaiEco.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Institution {

    NONE(1, "Aucune", "none", new ArrayList<InstitutionDivision>()),
    DIPLOMATE(2, "Corps Diplomatique", "diplo", new ArrayList<InstitutionDivision>()),
    ARTI(3, "Guilde des Artisans et Commerçants", "arti", new ArrayList<InstitutionDivision>()),
    MED(4, "Corps Médical", "med", new ArrayList<InstitutionDivision>()),
    ACADEMIE(5, "Académie", "aca", new ArrayList<InstitutionDivision>()),
    ADMIN(6, "Administration", "admin", new ArrayList<InstitutionDivision>(List.of(InstitutionDivision.TOWN_PLANNING)));

    public final int id;
    public final String name;
    public final String shortName;
    public final List<InstitutionDivision> divisions;

    Institution(int id, String name, String shortName, ArrayList<InstitutionDivision> divisions) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.divisions = divisions;
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
