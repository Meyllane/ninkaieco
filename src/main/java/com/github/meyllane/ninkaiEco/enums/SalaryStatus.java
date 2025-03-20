package com.github.meyllane.ninkaiEco.enums;

public enum SalaryStatus {
    ERROR(1),
    PENDING(2),
    REDEEMED(3);

    public final int id;

    SalaryStatus(int id) {
        this.id = id;
    }

    public static SalaryStatus getById(int id) {
        for (SalaryStatus status : values()) {
            if (status.id == id) return status;
        }
        return ERROR;
    }
}
