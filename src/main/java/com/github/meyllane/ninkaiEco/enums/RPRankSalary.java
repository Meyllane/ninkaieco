package com.github.meyllane.ninkaiEco.enums;

import me.Seisan.plugin.Features.objectnum.RPRank;

public enum RPRankSalary {

    NONE(1, RPRank.NULL, 0),
    STUDENT(2, RPRank.STUDENT, 0),
    GENIN(3, RPRank.GENIN, 150),
    CHUUNIN(4, RPRank.CHUUNIN, 350),
    JUUNIN(5, RPRank.JUUNIN, 525),
    SANNIN(5, RPRank.SANNIN, 525),
    CHIEF(5, RPRank.CHEF, 650);

    public final int id;
    public final RPRank rank;
    public final int salary;

    RPRankSalary(int id, RPRank rank, int salary) {
        this.id = id;
        this.rank = rank;
        this.salary = salary;
    }

    public static RPRankSalary getByID(int id) {
        for (RPRankSalary rank: values()) {
            if (rank.id == id) return rank;
        }
        return null;
    }

    public static RPRankSalary getByRPRank(RPRank rank) {
        for (RPRankSalary salary : values()) {
            if (salary.rank == rank) return salary;
        }
        return null;
    }
}
