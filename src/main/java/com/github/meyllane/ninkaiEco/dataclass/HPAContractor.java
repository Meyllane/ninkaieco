package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import me.Seisan.plugin.Main;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

public class HPAContractor {
    public static final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);

    private Integer id;
    private String playerUUID;
    private float salary_prop;
    private int HPA_id;
    private Date createdAt;
    private Date updatedAt;

    public HPAContractor(Integer id, String playerUUID, float salary_prop, int HPA_id, Date createdAt, Date updatedAt) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.salary_prop = salary_prop;
        this.HPA_id = HPA_id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public HPAContractor(String playerUUID, int HPA_id) {
        this.id = null;
        this.playerUUID = playerUUID;
        this.salary_prop = 0.5f;
        this.HPA_id = HPA_id;
        this.createdAt = new Date();
        this.updatedAt = new Date();
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
            pst.setString(2, this.playerUUID);
            pst.setInt(3, this.HPA_id);
            pst.setFloat(4, this.salary_prop);
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
                        res.getString("playerUUID"),
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
}
