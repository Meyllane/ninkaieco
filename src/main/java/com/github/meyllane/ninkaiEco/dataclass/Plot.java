package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.enums.PlotStatus;
import me.Seisan.plugin.Features.data.NinjaArtsDB;
import me.Seisan.plugin.Main;
import org.yaml.snakeyaml.util.ArrayUtils;

import java.awt.image.AreaAveragingScaleFilter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;

public class Plot {
    private Integer id;
    private String name;
    private String fullName;
    private PlotStatus status;
    private Integer price;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private String updatedBy;
    private ArrayList<String> ownersUUID;
    private HPA hpa;

    private static NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);

    //New plot
    public Plot(String name, PlotStatus plotStatus,  Integer price, String creatorUUID) {
        this.id =  null;
        this.name = name;
        this.fullName = this.createFullName();
        this.createdBy = creatorUUID;
        this.updatedBy = creatorUUID;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.price = price;
        this.status = plotStatus;
        this.ownersUUID = new ArrayList<>();
        this.hpa = null;
    }

    //From database
    public Plot(Integer id, String name, PlotStatus status, Integer price, Date createdAt, Date updatedAt, String createdBy, String updatedBy) {
        this.id = id;
        this.name = name;
        this.fullName = this.createFullName();
        this.status = status;
        this.price = price;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public ArrayList<String> getOwnersUUID() {
        return ownersUUID;
    }

    public void setOwnersUUID(ArrayList<String> ownersUUID) {
        this.ownersUUID = ownersUUID;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public PlotStatus getStatus() {
        return status;
    }

    public Integer getPrice() {
        return price;
    }

    public HPA getHpa() {
        return hpa;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setName(String name) {
        this.name = name;
        this.fullName = this.createFullName();
    }

    public void setHpa(HPA hpa) {
        this.hpa = hpa;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public void setStatus(PlotStatus status) {
        this.status = status;
    }

    public void setUpdatedBy(String updaterUUID) {
        this.updatedBy = updatedBy;
    }

    public void flush() {
        this.flushPlotAttributs();
        if (this.hpa != null) this.hpa.flush();

        ArrayList<String> dbOwners = this.getCurrentPlotOwners();
        if (dbOwners == null) return;

        this.ownersUUID.forEach(uuid -> {
            if (!dbOwners.contains(uuid)) this.flushAddOwner(uuid);
        });

        dbOwners.forEach(uuid -> {
            if (!this.ownersUUID.contains(uuid)) this.flushRemoveOwner(uuid);
        });
    }

    private void flushPlotAttributs() {
        try {
            this.updatedAt = new Date();
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    """
                    INSERT INTO Plot(ID, name, status, price, createdAt, updatedAt, createdBy, updatedBy)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        name = VALUES(name),
                        status = VALUES(status),
                        price = VALUES(price),
                        createdAt = VALUES(createdAt),
                        updatedAt = VALUES(updatedAt),
                        createdBy = VALUES(createdBy),
                        updatedBy = VALUES(updatedBy)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            //For the basic INSERT part
            pst.setObject(1, this.id);
            pst.setString(2, this.name);
            pst.setInt(3, this.status.id);
            pst.setInt(4, this.price);
            pst.setTimestamp(5, new java.sql.Timestamp(this.createdAt.getTime()));
            pst.setTimestamp(6, new java.sql.Timestamp(this.updatedAt.getTime()));
            pst.setString(7, this.createdBy);
            pst.setString(8, this.updatedBy);
            pst.execute();
            ResultSet res = pst.getGeneratedKeys();
            if (res.next()) this.id = res.getInt(1);
            pst.close();
        } catch (SQLException e) {
            plugin.getServer().getLogger().log(
                    Level.SEVERE,
                    "Error while flushing Plot: " + e.getMessage()
            );
        }
    }

    //
    private ArrayList<String> getCurrentPlotOwners() {
        ArrayList<String> currentOwners = new ArrayList<>();
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT playerUUID FROM PlotOwner WHERE plotID=?"
            );
            pst.setInt(1, this.id);
            ResultSet res = pst.executeQuery();
            while (res.next()) {
                currentOwners.add(res.getString("playerUUID"));
            }
            pst.close();
            return currentOwners;
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while fetching currentPlotOwners: " + e.getMessage()
            );
        }
        return null;
    }

    private void flushAddOwner(String playerUUID) {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "INSERT INTO PlotOwner(playerUUID, plotID) VALUES (?, ?)"
            );
            pst.setString(1, playerUUID);
            pst.setInt(2, this.id);
            pst.execute();
            pst.close();
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while flush-adding a PlotOwner: " + e.getMessage()
            );
        }
    }

    private void flushRemoveOwner(String playerUUID) {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "DELETE FROM PlotOwner WHERE playerUUID=? AND plotID=?"
            );
            pst.setString(1, playerUUID);
            pst.setInt(2, this.id);
            pst.execute();
            pst.close();
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while flush-removing a PlotOwner: " + e.getMessage()
            );
        }
    }

    public static HashMap<String, Plot> getAllPlots() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT * FROM Plot"
            );
            ResultSet res = pst.executeQuery();
            HashMap<String, Plot> plots = new HashMap<>();
            while (res.next()) {
                Plot plot = new Plot(
                        res.getInt("ID"),
                        res.getString("name"),
                        PlotStatus.getByID(res.getInt("status")),
                        res.getInt("price"),
                        new Date(res.getTimestamp("createdAt").getTime()),
                        new Date(res.getTimestamp("updatedAt").getTime()),
                        res.getString("createdBy"),
                        res.getString("updatedBy")
                );
                plot.ownersUUID = fetchPlotOwners(plot.id);
                plot.hpa = HPA.getOnGoingHPA(plot);
                plots.put(res.getString("name"), plot);
            }
            pst.close();
            return plots;
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while fetching all Plots: " + e.getMessage()
            );
        }
        return null;
    }

    private static ArrayList<String> fetchPlotOwners(int id) {
        ArrayList<String> owners = new ArrayList<>();
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT playerUUID FROM PlotOwner WHERE plotID=?"
            );
            pst.setInt(1, id);
            ResultSet res = pst.executeQuery();
            while(res.next()) {
                owners.add(res.getString("playerUUID"));
            }
            pst.close();
            return owners;
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while fetching PlotOwners: " + e.getMessage()
            );
        }

        return null;
    }

    private String createFullName() {
        String[] res = this.name.split("_");
        StringBuilder fullName = new StringBuilder(res[0].toUpperCase());
        if (res.length == 1) return fullName.toString();

        for (int i = 1; i<=res.length-1; i++) {
            fullName.append(" - ").append(res[i]);
        }
        return fullName.toString();
    }
}
