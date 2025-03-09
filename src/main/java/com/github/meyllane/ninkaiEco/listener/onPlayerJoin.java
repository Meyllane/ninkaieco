package com.github.meyllane.ninkaiEco.listener;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerInstitution;
import com.github.meyllane.ninkaiEco.enums.Institution;
import com.github.meyllane.ninkaiEco.enums.InstitutionRank;
import com.github.meyllane.ninkaiEco.enums.RPRankSalary;
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
    NinkaiEco plugin;
    MiniMessage mm = MiniMessage.miniMessage();

    public onPlayerJoin(NinkaiEco plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) throws SQLException {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        ResultSet res = this.getPlayerEco(playerUUID);
        if (res == null) { //The player does not have a PlayerEco profil, so we will create one
            this.createPlayerEco(playerUUID);
            this.createPlayerInstitution(playerUUID);
            res = this.getPlayerEco(playerUUID);}

        //Get player information and put them in the HashMpa
        RPRank rank = this.getRank(playerUUID);

        PlayerEco playerEco = new PlayerEco(playerUUID, res.getInt("bankMoney"), res.getDate("lastPaid"), rank,
                this.getPlayerInstitution(playerUUID), this.getRPRankSalary(rank));

        //Check if the salary need to be given
        if (playerEco.canGetPaid() && this.plugin.isSalaryStart()) {
            playerEco.setBankMoney(playerEco.getBankMoney() + playerEco.getMonthlySalary());
            this.plugin.adventure().player(player).sendMessage(
                    PluginComponentProvider.getPluginHeader()
                            .append(mm.deserialize(
                                    String.format("<color:#bfbfbf>Votre salaire est arrivé ! %,d ryos ont été ajoutés à votre compte.",
                                            playerEco.getMonthlySalary())
                            ))
            );
            playerEco.setLastPaid(new Date()); //TODO: Voir pourquoi l'heure n'est pas prise en compte
        }

        NinkaiEco.playerEcoMap.put(playerUUID, playerEco);
        CommandAPI.updateRequirements(player);
    }

    public ResultSet getPlayerEco(String playerUUID) {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT * FROM Money WHERE playerUUID = ?"
            );
            pst.setString(1, playerUUID);
            ResultSet res = pst.executeQuery();
            if (res.next()) {
                return res;
            }
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, e.getMessage());
        }
        return null;
    }

    public void createPlayerEco(String playerUUID) {
        String sqlQuerry = "INSERT INTO Money(playerUUID, bankMoney) VALUES(?, ?)";
        try (PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(sqlQuerry)) {
            pst.setString(1, playerUUID);
            pst.setInt(2, 0);
            pst.execute();
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, e.getMessage());
        }
    }

    public void createPlayerInstitution(String playerUUID) {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "INSERT INTO Institution(playerUUID, institution, rank) VALUES (?, ?,?)"
            );
            pst.setString(1, playerUUID);
            pst.setInt(2, Institution.NONE.id);
            pst.setInt(3, InstitutionRank.NONE.id);
            pst.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e.getMessage());
        }
    }

    public RPRank getRank(String playerUUID) {
        try {
            String sqlQuerry = "SELECT rang FROM PlayerInfo WHERE uuid=?";
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(sqlQuerry);
            pst.setString(1, playerUUID);
            ResultSet res = pst.executeQuery();
            if (!res.next()) return null;
            return RPRank.getById(res.getInt("rang"));
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, e.getMessage());
        }
        return RPRank.NULL;
    }

    public PlayerInstitution getPlayerInstitution(String playerUUID) {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT * FROM Institution WHERE playerUUID = ?"
            );
            pst.setString(1, playerUUID);
            ResultSet res = pst.executeQuery();
            if (res.next()) {
                return new PlayerInstitution(
                        playerUUID,
                        Institution.getByID(res.getInt("institution")),
                        InstitutionRank.getByID(res.getInt("rank"))
                );
            }
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, e.getMessage());
        }
        return new PlayerInstitution(playerUUID, Institution.NONE, InstitutionRank.NONE);
    }

    public RPRankSalary getRPRankSalary(RPRank rank) {
        return RPRankSalary.getByRPRank(rank);
    }
}
