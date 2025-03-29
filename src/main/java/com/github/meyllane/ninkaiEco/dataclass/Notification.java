package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.utils.PluginComponentProvider;
import me.Seisan.plugin.Main;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class Notification {
    private Integer id;
    private final Integer amount;
    private final String playerUUID;
    private final String message;
    private final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);
    private final BukkitAudiences adventure = plugin.adventure();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public Notification(Integer amount, String playerUUID, String message) {
        this.id = -1;
        this.amount = amount;
        this.playerUUID = playerUUID;
        this.message = message;
    }

    public Notification(Integer id, int amount, String playerUUID, String message) {
        this.id = id;
        this.amount = amount;
        this.playerUUID = playerUUID;
        this.message = message;
    }

    public void flush() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    """
                            INSERT INTO Notification(playerUUID, amount, message)
                            VALUES (?, ?, ?)
                            """
            );
            pst.setString(1, this.playerUUID);
            pst.setInt(2, this.amount);
            pst.setString(3, this.message);
            pst.execute();
            pst.close();
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error when flushing Notification :" + e.getMessage()
            );
        }
    }

    public void delete() {
        if (this.id == null) return;
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "DELETE FROM Notification WHERE ID=?"
            );
            pst.setInt(1, this.id);
            pst.execute();
            pst.close();
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while deleting Notification: " + e.getMessage()
            );
        }
    }

    public void send() {
        Player player = plugin.getServer().getPlayer(UUID.fromString(this.playerUUID));
        if (player == null) return;

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize(this.message))
        );
    }
}
