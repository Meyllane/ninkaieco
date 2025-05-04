package com.github.meyllane.ninkaiEco.enums;

public enum ErrorMessage {
    UNKNOWN_SELL_ORDER_ID("Cet ID de commande est inconnu."),
    CANT_SELL_TO_SELF("Vous ne pouvez pas vous vendre quelque chose à vous même."),
    NOT_RECIPIENT_OF_SELL_ORDER("Cette commande ne vous est pas destinée."),
    NOT_EMITTER_OF_SELL_ORDER("Vous n'êtes pas à l'origine de cette commande."),
    ALREADY_PROCESS_SELL_ORDER("Cette commande a déjà été traité."),
    NOT_ENOUGHT_MONEY_FOR_SELL_ORDER("Vous n'avez pas assez d'argent pour accepter cette vente."),
    UNKNOWN_ERROR("Une erreur inattendue est survenue. Contactez l'équipe technique."),
    NONE_EXISTING_OR_NEVER_SEEN_PLAYER("Lae joueur.euse n'existe pas ou ne s'est jamais connecté.e sur le serveur."),
    NONE_EXISTING_PLOT("Le plot demandé n'existe pas."),
    ALREADY_EXISTING_PLOT("Un plot portant ce nom existe déjà."),
    PLOT_CANT_HAVE_PLAYER_OWNER("Ce plot ne peut pas être détenu par un.e joueur.euse"),
    PLAYER_ALREADY_OWNS_THIS_PLOT("Lae joueur.euse possède déjà ce plot."),
    PLAYER_NOT_PLOT_OWNER("Lae joueur.euse ne possède pas ce plot."),
    FIRST_REMOVE_CURRENT_OWNERS("Le plot a encore des propriétaires."),
    SAME_PLOT_STATUS("Le statut demandé est déjà celui du plot."),
    PLOT_DOESNT_HAVE_HPA("Ce plot n'a pas de contrat de location-vente en cours."),
    INCCORECT_DIVISION("La division demandée n'est pas une division de cette institution.");

    public final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

}
