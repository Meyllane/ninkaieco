package com.github.meyllane.ninkaiEco.events;

import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerEcoLoadedEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final PlayerEco playerEco;

    public PlayerEcoLoadedEvent(PlayerEco playerEco) {
        this.playerEco = playerEco;
    }
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public PlayerEco getPlayerEco() {
        return playerEco;
    }
}
