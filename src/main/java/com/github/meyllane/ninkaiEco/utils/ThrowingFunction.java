package com.github.meyllane.ninkaiEco.utils;

import com.github.meyllane.ninkaiEco.dataclass.SellOrder;
import com.github.meyllane.ninkaiEco.error.NinkaiEcoError;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface ThrowingFunction<T, U extends Player> {
    void apply(T t, U u) throws NinkaiEcoError;
}
