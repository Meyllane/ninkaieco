package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.enums.Institution;
import com.github.meyllane.ninkaiEco.enums.InstitutionDivision;
import com.github.meyllane.ninkaiEco.enums.InstitutionRank;
import me.Seisan.plugin.Features.data.NinjaArtsDB;
import me.Seisan.plugin.Main;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class PlayerInstitution {
    private int id;
    private final String playerUUID;
    private Institution institution;
    private InstitutionRank rank;
    private InstitutionDivision division;

    public PlayerInstitution(int id, String playerUUID, Institution institution, InstitutionRank rank, InstitutionDivision division) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.institution = institution;
        this.rank = rank;
        this.division = division;
    }

    public PlayerInstitution(String playerUUID) {
        this.id = -1;
        this.playerUUID = playerUUID;
        this.institution = Institution.NONE;
        this.rank = InstitutionRank.NONE;
        this.division = InstitutionDivision.NONE;
    }

    public Institution getInstitution() {
        return institution;
    }

    public InstitutionRank getRank() {
        return rank;
    }

    public InstitutionDivision getDivision() {
        return division;
    }

    public void setDivision(InstitutionDivision division) {
        this.division = division;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public void setRank(InstitutionRank rank) {
        this.rank = rank;
    }

    public void flush() {
        if (this.id < 0) {
            this.id = this.create();
        } else {
            this.update();
        }
    }

    private int create() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "INSERT INTO Institution(playerUUID, institution, rank, division) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            pst.setString(1, this.playerUUID);
            pst.setInt(2, this.institution.id);
            pst.setInt(3, this.rank.id);
            pst.setInt(4, this.division.id);
            pst.execute();

            ResultSet res = pst.getGeneratedKeys();
            if (!res.next()) return -1;

            int id = res.getInt(1);
            pst.close();
            return id;
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error when creating PlayerInstitution: " + e.getMessage()
            );
        }
        return -1;
    }

    private void update() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "UPDATE Institution SET institution=?, rank=?, division=? WHERE ID=?"
            );
            pst.setInt(1, this.institution.id);
            pst.setInt(2, this.rank.id);
            pst.setInt(3, this.division.id);
            pst.setInt(4, this.id);
            pst.execute();
            pst.close();
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error when updating PlayerInstitution: " + e.getMessage()
            );
        }
    }

    public static PlayerInstitution get(String playerUUID) {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT * FROM Institution WHERE playerUUID=?"
            );
            pst.setString(1, playerUUID);
            ResultSet res = pst.executeQuery();
            if (!res.next()) return new PlayerInstitution(playerUUID);

            PlayerInstitution pInstitution = new PlayerInstitution(
                    res.getInt("ID"),
                    playerUUID,
                    Institution.getByID(res.getInt("institution")),
                    InstitutionRank.getByID(res.getInt("rank")),
                    InstitutionDivision.getByID(res.getInt("division"))
            );

            pst.close();
            return pInstitution;
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error when fetching PlayerInstitution: " + e.getMessage()
            );
        }
        return new PlayerInstitution(playerUUID);
    }
}
