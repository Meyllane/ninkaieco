package com.github.meyllane.ninkaiEco.events;

import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerEcoLoadedEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final PlayerEco playerEco;
    private final Player player;

    public PlayerEcoLoadedEvent(Player player, PlayerEco playerEco) {
        this.playerEco = playerEco;
        this.player = player;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public PlayerEco getPlayerEco() {
        return playerEco;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
