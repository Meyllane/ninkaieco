package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.enums.Cash;
import org.bukkit.inventory.ItemStack;

public class PlayerCash {

    private ItemStack itemStack;
    private Cash cash;

    public PlayerCash(ItemStack itemStack, Cash cash) {
        this.itemStack = itemStack;
        this.cash = cash;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Cash getCash() {
        return cash;
    }

    public int getValue() {
        return this.cash.value * this.itemStack.getAmount();
    }
}
