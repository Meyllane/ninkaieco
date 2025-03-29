package com.github.meyllane.ninkaiEco.listener;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.dataclass.Notification;
import com.github.meyllane.ninkaiEco.events.PlayerEcoLoadedEvent;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;

public class onPlayerEcoLoaded implements Listener {
    private final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);
    private final BukkitAudiences adventure = plugin.adventure();
    private final MiniMessage mm = MiniMessage.miniMessage();

    @EventHandler()
    public void onEcoLoaded(PlayerEcoLoadedEvent event) {
        Player player = event.getPlayer();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> this.handlePlayerNotifications(player));
    }

    /**
     * Handles Notifications retrieval and processing. Has to be executed asynchronously.
     * @param player
     */
    public void handlePlayerNotifications(Player player) {
        ArrayList<Notification> notifs = Notification.getNotifications(player.getUniqueId().toString());
        notifs.forEach(notif -> {
            plugin.getServer().getScheduler().runTask(plugin, notif::send);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, notif::delete);
        });
    }
}
