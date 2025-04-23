package com.github.meyllane.ninkaiEco.enums;

public enum HPAStatus {
    ON_GOING(1, "En cours"),
    CANCELLED(2, "Annulé"),
    COMPLETED(3, "Complété");

    public final int id;
    public final String name;

    HPAStatus(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static HPAStatus getByID(int id) {
        for (HPAStatus status : values()) {
            if (status.id == id) return status;
        }
        return HPAStatus.ON_GOING;
    }
}
