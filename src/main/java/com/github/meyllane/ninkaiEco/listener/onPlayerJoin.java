package com.github.meyllane.ninkaiEco.listener;

import com.github.meyllane.ninkaiEco.NinkaiEco;
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
import java.util.Date;
import java.util.logging.Level;

public class onPlayerJoin implements Listener {
    private final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);
    MiniMessage mm = MiniMessage.miniMessage();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) throws SQLException {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();

        PlayerEco playerEco = PlayerEco.get(playerUUID);

        NinkaiEco.playerEcoMap.put(playerUUID, playerEco);
        CommandAPI.updateRequirements(player);

        new PlayerEcoLoadedEvent(event.getPlayer(), playerEco).callEvent();
    }

    private RPRank getRPRank(String playerUUID) {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT rang FROM PlayerInfo WHERE uuid = ?"
            );
            pst.setString(1, playerUUID);
            ResultSet res = pst.executeQuery();
            if (!res.next()) return RPRank.STUDENT;

            RPRank rank = RPRank.getById(res.getInt("rang"));
            pst.close();
            return rank;
        } catch (SQLException e) {
            this.plugin.getLogger().log(
                    Level.SEVERE,
                    "Error when fetching the PlayerRank: " + e.getMessage()
            );
        }
        return RPRank.STUDENT;
    }
}
