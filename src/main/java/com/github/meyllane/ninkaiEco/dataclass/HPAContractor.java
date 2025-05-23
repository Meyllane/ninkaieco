package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import me.Seisan.plugin.Main;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Level;

public class HPAContractor {
    public static final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);

    private Integer id;
    private PlayerEco playerEco;
    private float salaryProp;
    private int HPA_id;
    private Date createdAt;
    private Date updatedAt;

    public HPAContractor(Integer id, PlayerEco playerEco, float salaryProp, int HPA_id, Date createdAt, Date updatedAt) {
        this.id = id;
        this.playerEco = playerEco;
        this.salaryProp = salaryProp;
        this.HPA_id = HPA_id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public HPAContractor(PlayerEco playerEco, int HPA_id) {
        this.id = null;
        this.playerEco = playerEco;
        this.salaryProp = 0.5f;
        this.HPA_id = HPA_id;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public PlayerEco getPlayerEco() {
        return playerEco;
    }

    public float getSalaryProp() {
        return salaryProp;
    }

    public void setSalaryProp(float salaryProp) {
        this.salaryProp = salaryProp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HPAContractor other)) return false;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.id);
    }

    public void flushUpsert() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    """
                            INSERT INTO HPAContractor(ID, playerUUID, hpaID, salary_prop, createdAt, updatedAt)
                            VALUES (?, ?, ?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE
                                playerUUID = VALUES(playerUUID),
                                hpaID = VALUES(hpaID),
                                salary_prop = VALUES(salary_prop),
                                createdAt = VALUES(createdAt),
                                updatedAt = VALUES(updatedAt)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            pst.setObject(1, this.id);
            pst.setString(2, this.playerEco.getPlayerUUID());
            pst.setInt(3, this.HPA_id);
            pst.setFloat(4, this.salaryProp);
            pst.setTimestamp(5, new Timestamp(this.createdAt.getTime()));
            pst.setTimestamp(6, new Timestamp(this.updatedAt.getTime()));
            pst.execute();
            ResultSet res = pst.getGeneratedKeys();
            if (res.next()) this.id = res.getInt(1);
            pst.close();
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while flushing-upserting HPAContractor: " + e.getMessage()
            );
        }
    }

    public void flushDelete() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    """
                            DELETE FROM HPAContractor WHERE ID = ?
                            """
            );
            pst.setObject(1, this.id);
            pst.execute();
            pst.close();
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while flushing-deleting HPAContractor: " + e.getMessage()
            );
        }
    }

    public static ArrayList<HPAContractor> getContractors(int hpaId) {
        ArrayList<HPAContractor> contractors = new ArrayList<>();
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT * FROM HPAContractor WHERE hpaID = ?"
            );
            pst.setInt(1, hpaId);
            ResultSet res = pst.executeQuery();
            while (res.next()) {
                HPAContractor contractor = new HPAContractor(
                        res.getInt("ID"),
                        NinkaiEco.playerEcos.get(res.getString("playerUUID")),
                        res.getFloat("salary_prop"),
                        res.getInt("hpaID"),
                        new Date(res.getTimestamp("createdAt").getTime()),
                        new Date(res.getTimestamp("updatedAt").getTime())
                );
                contractors.add(contractor);
            }
            pst.close();
            return contractors;
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while fetching HPAContractors: " + e.getMessage()
            );
        }
        return contractors;
    }

    public int getMonthlyPayment() {
        float sum = (float) this.playerEco.getMonthlySalary() * this.salaryProp;
        return Math.round(sum);
    }
}
