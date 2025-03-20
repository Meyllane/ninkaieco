package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.enums.SalaryStatus;
import me.Seisan.plugin.Main;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

public class PlayerSalary {
    private int id;
    private final String emitterUUID;
    private final String receiverUUID;
    private int amount;
    private SalaryStatus status;
    private final Date createdAt;
    private final Date updatedAt;

    public PlayerSalary(int id, String emitterUUID, String receiverUUID, int amount, SalaryStatus status, Date createdAt, Date updatedAt) {
        this.id = id;
        this.emitterUUID = emitterUUID;
        this.receiverUUID = receiverUUID;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public PlayerSalary(String emitterUUID, String receiverUUID, int amount, SalaryStatus status) {
        this.id = -1;
        this.emitterUUID = emitterUUID;
        this.receiverUUID = receiverUUID;
        this.amount = amount;
        this.status = status;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public int getAmount() {
        return amount;
    }

    public SalaryStatus getStatus() {
        return status;
    }

    public String getEmitterUUID() {
        return emitterUUID;
    }

    public String getReceiverUUID() {
        return receiverUUID;
    }

    public void setStatus(SalaryStatus status) {
        this.status = status;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void flush() {
        if (this.id < 0) {
            this.id = this.create();
        } else {
            this.update();
        }
    }

    protected int create() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    """
                            INSERT INTO PlayerSalary(emitterUUID, receiverUUID, amount, status, createdAt, updatedAt)
                            VALUES(?, ?, ?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            pst.setString(1, this.emitterUUID);
            pst.setString(2, this.receiverUUID);
            pst.setInt(3, this.amount);
            pst.setInt(4, this.status.id);
            pst.setTimestamp(5, new java.sql.Timestamp(this.createdAt.getTime()));
            pst.setTimestamp(6, new java.sql.Timestamp(this.updatedAt.getTime()));
            pst.execute();

            ResultSet res = pst.getGeneratedKeys();
            res.next();

            int id = res.getInt(1);
            pst.close();
            return id;
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error when creating PlayerSalary: " + e.getMessage()
            );
        }
        return -1;
    }

    protected void update() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "UPDATE PlayerSalary SET status = ?, updatedAt = ? WHERE ID = ?"
            );
            pst.setInt(1, this.status.id);
            pst.setTimestamp(2, new java.sql.Timestamp(new Date().getTime()));
            pst.setInt(3, this.id);
            pst.execute();
            pst.close();
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error when updating PlayerSalary: " + e.getMessage()
            );
        }
    }

    public static ArrayList<PlayerSalary> getPendingPlayerSalaries(String playerUUID) {
        ArrayList<PlayerSalary> list = new ArrayList<>();
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT * FROM PlayerSalary WHERE receiverUUID = ? AND status = ?"
            );
            pst.setString(1, playerUUID);
            pst.setInt(2, SalaryStatus.PENDING.id);
            ResultSet res = pst.executeQuery();
            while (res.next()) {
                list.add(new PlayerSalary(
                        res.getInt("ID"),
                        res.getString("emitterUUID"),
                        res.getString("receiverUUID"),
                        res.getInt("amount"),
                        SalaryStatus.getById(res.getInt("status")),
                        new Date(res.getTimestamp("createdAt").getTime()),
                        new Date(res.getTimestamp("updatedAt").getTime())
                ));
            }
            pst.close();
            return list;
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error when fetching PlayerSalaries: " + e.getMessage()
            );
        }
        return list;
    }
}
