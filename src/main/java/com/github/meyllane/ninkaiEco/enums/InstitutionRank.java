package com.github.meyllane.ninkaiEco.enums;

public enum InstitutionRank {

    NONE(1, "Aucun", 0, "none"),
    MEMBER(2, "Membre", 175, "mem"),
    MANAGER(3, "Responsable", 245, "resp");

    public final int id;
    public final String name;
    public final int salary;
    public final String shortName;

    InstitutionRank(int id, String name, int salary, String shortName) {
        this.id = id;
        this.name = name;
        this.salary = salary;
        this.shortName = shortName;
    }

    public static InstitutionRank getByID(int id) {
        for(InstitutionRank iRank : values()) {
            if (iRank.id == id) return iRank;
        }
        return NONE;
    }

    public static InstitutionRank getByShortName(String shortName) {
        for (InstitutionRank rank: values()) {
            if (rank.shortName.equals(shortName)) return rank;
        }
        return NONE;
    }
}
