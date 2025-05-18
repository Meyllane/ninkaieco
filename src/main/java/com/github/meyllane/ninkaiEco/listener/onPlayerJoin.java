package com.github.meyllane.ninkaiEco.listener;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.dataclass.Notification;
import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerInstitution;
import com.github.meyllane.ninkaiEco.enums.Institution;
import com.github.meyllane.ninkaiEco.enums.InstitutionRank;
import com.github.meyllane.ninkaiEco.enums.RPRankSalary;
import com.github.meyllane.ninkaiEco.events.PlayerEcoLoadedEvent;
import com.github.meyllane.ninkaiEco.utils.PluginComponentProvider;
import dev.jorel.commandapi.CommandAPI;
import me.Seisan.plugin.Features.objectnum.RPRank;
import me.Seisan.plugin.Main;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

public class onPlayerJoin implements Listener {
    private final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);
    MiniMessage mm = MiniMessage.miniMessage();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) throws SQLException {
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
