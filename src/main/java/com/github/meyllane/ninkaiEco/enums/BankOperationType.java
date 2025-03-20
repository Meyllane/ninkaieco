package com.github.meyllane.ninkaiEco.enums;

/**
 * Enum representing different types of bank operations.
 */
public enum BankOperationType {
    /**
     * Represents a withdrawal operation where money is taken out of a player's account.
     */
    WITHDRAW(1, "withdraw"),

    /**
     * Represents a deposit operation where money is added to a player's account.
     */
    DEPOSIT(2, "deposit"),

    /**
     * Represents a payment operation for a sell order. Because the seller does not get the whole amount (only the margin),
     * this operation represents the money leaving the buyer's account.
     */
    SO_PAYMENT(3, "so_payment"),

    /**
     * Represents a received payment operation for a sell order. Because the seller does not get the whole amount (only the margin),
     * this operation represents the margin that the seller gets.
     */
    SO_RECEIVED(4, "so_received"),

    SALARY(5, "salary"),
    ADD(6, "add"),
    REMOVE(7, "remove"),
    SET(8, "set");

    /**
     * The unique identifier for the bank operation type.
     */
    public final int id;

    /**
     * The string representation of the bank operation type. This value will appear in the database in the "type" column of
     * BankOperation
     */
    public final String type;

    /**
     * Constructs a new BankOperationType with the specified id and type.
     *
     * @param id   the unique identifier for the bank operation type
     * @param type the string representation of the bank operation type. It's the value that will appear in the database.
     */
    BankOperationType(int id, String type) {
        this.id = id;
        this.type = type;
    }
}