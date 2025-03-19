package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.enums.RPRankSalary;
import me.Seisan.plugin.Features.objectnum.RPRank;
import me.Seisan.plugin.Main;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class PlayerEco {
    private final String playerUUID;
    private int bankMoney;
    private Date lastPaid;
    private final RPRank rank;
    private final PlayerInstitution institution;
    private final RPRankSalary rpRankSalary;

    public PlayerEco(String playerUUID, int bankMoney, Date lastPaid, RPRank rank, PlayerInstitution institution, RPRankSalary rpRankSalary) {
        this.playerUUID = playerUUID;
        this.bankMoney = bankMoney;
        this.lastPaid = lastPaid;
        this.rank = rank;
        this.institution = institution;
        this.rpRankSalary = rpRankSalary;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public int getMonthlySalary() {
        return this.rpRankSalary.salary + this.institution.getRank().salary;
    }

    public long getTimeSinceLastPaid() {
        long diff = this.lastPaid.getTime() - (new java.util.Date()).getTime();
        return Math.abs(TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS));
    }

    public boolean canGetPaid() {
        return this.getTimeSinceLastPaid() >= 30;
    }

    public RPRank getRank() {
        return rank;
    }

    public Date getLastPaid() {
        return lastPaid;
    }

    public int getBankMoney() {
        return bankMoney;
    }

    public void setLastPaid(Date lastPaid) {
        this.lastPaid = lastPaid;
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
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "UPDATE Money SET bankMoney = ?, lastPaid = ? WHERE playerUUID = ?"
            );
            pst.setInt(1, this.bankMoney);
            pst.setTimestamp(2, new java.sql.Timestamp(this.lastPaid.getTime()));
            pst.setString(3, this.playerUUID);
            pst.execute();
            pst.close();
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(Level.SEVERE, e.getMessage());
        }
        this.institution.flush();
    }
}
