package com.github.meyllane.ninkaiEco.enums;

public enum ErrorMessage {
    UNKNOWN_SELL_ORDER_ID("Cet ID de commande est inconnu."),
    CANT_SELL_TO_SELF("Vous ne pouvez pas vous vendre quelque chose à vous même."),
    NOT_RECIPIENT_OF_SELL_ORDER("Cette commande ne vous est pas destinée."),
    NOT_EMITTER_OF_SELL_ORDER("Vous n'êtes pas à l'origine de cette commande."),
    ALREADY_PROCESS_SELL_ORDER("Cette commande a déjà été traité."),
    NOT_ENOUGHT_MONEY_FOR_SELL_ORDER("Vous n'avez pas assez d'argent pour accepter cette vente."),
    UNKNOWN_ERROR("Une erreur inattendue est survenue. Contactez l'équipe technique.");

    public final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

}
