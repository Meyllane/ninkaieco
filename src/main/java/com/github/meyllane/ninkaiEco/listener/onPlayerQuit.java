package com.github.meyllane.ninkaiEco.listener;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class onPlayerQuit implements Listener {
    private final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);

    @EventHandler()
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();

        //Retrieve the player
        PlayerEco playerEco = NinkaiEco.playerEcos.get(playerUUID);

        //Flush the player's data to the database
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, bukkitTask -> playerEco.flush());
    }
}
