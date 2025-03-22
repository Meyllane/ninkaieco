package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.enums.RPRankSalary;
import me.Seisan.plugin.Features.objectnum.RPRank;
import me.Seisan.plugin.Main;
import org.checkerframework.checker.units.qual.N;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PlayerEco {
    private int id;
    private final String playerUUID;
    private int bankMoney;
    private final RPRank rank;
    private final PlayerInstitution institution;
    private final RPRankSalary rpRankSalary;

    public PlayerEco(int id, String playerUUID, int bankMoney, RPRank rank, PlayerInstitution institution, RPRankSalary rpRankSalary) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.bankMoney = bankMoney;
        this.rank = rank;
        this.institution = institution;
        this.rpRankSalary = rpRankSalary;
    }

    public PlayerEco(String playerUUID, RPRank rank) {
        this.id = -1;
        this.playerUUID = playerUUID;
        this.bankMoney = 0;
        this.rank = rank;
        this.institution = new PlayerInstitution(playerUUID);
        this.rpRankSalary = RPRankSalary.getByRPRank(this.rank);
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public int getMonthlySalary() {
        return this.rpRankSalary.salary + this.institution.getRank().salary;
    }

    public RPRank getRank() {
        return rank;
    }

    public int getBankMoney() {
        return bankMoney;
    }

    public void setBankMoney(int bankMoney) {
        this.bankMoney = bankMoney;
    }

    public void addBankMoney(int amount) {
        this.bankMoney += amount;
    }

    public void removeBankMoney(int amount) {
        this.bankMoney -= amount;
    }

    public PlayerInstitution getPlayerInstitution() {
        return institution;
    }

    public RPRankSalary getRpRankSalary() {
        return rpRankSalary;
    }

    public void flush() {
        if (this.id < 0) {
            this.id = this.create();
        } else {
            this.update();
        }
        this.institution.flush();
    }

    public static PlayerEco get(String playerUUID) {
        RPRank rank = PlayerEco.getRank(playerUUID);
        try {

            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    """
                            SELECT DISTINCT Money.*
                            FROM Money
                            WHERE Money.playerUUID = ?
                            """
            );
            pst.setString(1, playerUUID);
            ResultSet res = pst.executeQuery();

            if (!res.next()) return new PlayerEco(playerUUID, rank);

            PlayerEco pEco = new PlayerEco(
                    res.getInt("Money.ID"),
                    playerUUID,
                    res.getInt("Money.bankMoney"),
                    rank,
                    PlayerInstitution.get(playerUUID),
                    RPRankSalary.getByRPRank(rank)
            );
            pst.close();
            return pEco;
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error when fetching PlayerEco : " + e.getMessage()
            );
        }
        return new PlayerEco(playerUUID, rank);
    }

    public static RPRank getRank(String playerUUID) {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT rang FROM PlayerInfo WHERE uuid=?"
            );
            pst.setString(1, playerUUID);
            ResultSet res = pst.executeQuery();
            if (res.next()) {
                return RPRank.getById(res.getInt("rang"));
            }
            pst.close();
            return RPRank.STUDENT;
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error while fetching RPRank for PlayerEco: " + e.getMessage()
            );
        }
        return RPRank.STUDENT;
    }

    public static List<PlayerEco> getAll() {
        try {
            ArrayList<PlayerEco> list = new ArrayList<>();
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    """
                            SELECT DISTINCT Money.playerUUID
                            FROM Money
                            """
            );
            ResultSet res = pst.executeQuery();
            while (res.next()) {
                list.add(
                        PlayerEco.get(res.getString("Money.playerUUID"))
                );
            }
            pst.close();
            return list;
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error while fetching all the PlayerEco: " + e.getMessage()
            );
        }
        return null;
    }

    private int create() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "INSERT INTO Money(playerUUID, bankMoney) VALUES(?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            pst.setString(1, this.playerUUID);
            pst.setInt(2, this.bankMoney);
            pst.execute();

            ResultSet res = pst.getGeneratedKeys();
            if (!res.next()) return -1;

            int id = res.getInt(1);
            pst.close();
            return id;
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error when creating PlayerEco : " + e.getMessage()
            );
        }
        return -1;
    }

    private void update() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "UPDATE Money SET bankMoney = ? WHERE ID = ?"
            );
            pst.setInt(1, this.bankMoney);
            pst.setInt(2, this.id);
            pst.execute();
            pst.close();
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error when updating PlayerEco : " + e.getMessage()
            );
        }
    }
}
