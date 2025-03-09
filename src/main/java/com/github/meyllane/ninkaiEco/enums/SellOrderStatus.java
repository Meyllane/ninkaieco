package com.github.meyllane.ninkaiEco.enums;

public enum SellOrderStatus {

    ERROR(1, "Error"),
    PENDING(2, "Pending"),
    ACCEPTED(3, "Accepted"),
    REJECTED(4, "Rejected"),
    CANCELED(5, "Canceled");

    public final int id;
    public final String status;

    SellOrderStatus(int id, String status) {
        this.id = id;
        this.status = status;
    }

    public static SellOrderStatus getByID(int id) {
        for (SellOrderStatus status : values()) {
            if (status.id == id) return status;
        }
        return ERROR;
    }
}
