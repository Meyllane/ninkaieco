package com.github.meyllane.ninkaiEco.enums;

import net.kyori.adventure.text.Component;

import java.util.Objects;

public enum Cash {
    BRONZE_COIN(
            "ryo_3",
            1,
            "<color:#b57f4a>Pièce - <color:#b1955e>Ryos",
            "<color:#a8a8a8>Une pièce d'une valeur de <color:#b1955e>1</color:#b1955e> ryos."
    ),
    SILVER_COIN(
            "ryo_2",
            5,
            "<color:#b57f4a>Pièce - <color:#b1955e>Ryos",
            "<color:#a8a8a8>Une pièce d'une valeur de <color:#b1955e>5</color:#b1955e> ryos."
    ),
    GOLD_COIN(
            "ryo",
            10,
            "<color:#b57f4a>Pièce - <color:#b1955e>Ryos",
            "<color:#a8a8a8>Une pièce d'une valeur de <color:#b1955e>10</color:#b1955e> ryos."
    ),
    BILL(
            "ryo_billet",
            100,
            "<color:#b57f4a>Billet - <color:#b1955e>Ryos",
            "<color:#a8a8a8>Un billet d'une valeur de <color:#b1955e>100</color:#b1955e> ryos."
    ),
    BILL_STACK(
            "ryo_liasse",
            500,
            "<color:#b57f4a>Liasse de billets - <color:#b1955e>Ryos",
            "<color:#a8a8a8>Une liasse de billets d'une valeur de <color:#b1955e>500</color:#b1955e> ryos."
    );

    public final String textureName;
    public final int value;
    public final String itemName;
    public final String itemLore;

    Cash(String texture_name, int value, String itemName, String itemLore) {
        this.textureName = texture_name;
        this.value = value;
        this.itemName = itemName;
        this.itemLore = itemLore;
    }

    public static Cash getByTextureName(String texture_name) {
        for (Cash c : values()) {
            if (Objects.equals(c.textureName, texture_name)) return c;
        }
        return null;
    }

    public int getValue() {
        return this.value;
    }
}
