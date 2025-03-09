package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.enums.Institution;
import com.github.meyllane.ninkaiEco.enums.InstitutionRank;
import me.Seisan.plugin.Main;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class PlayerInstitution {
    private final String playerUUID;
    private Institution institution;
    private InstitutionRank rank;
    private boolean dirty = false;

    public PlayerInstitution(String playerUUID, Institution institution, InstitutionRank rank) {
        this.playerUUID = playerUUID;
        this.institution = institution;
        this.rank = rank;
    }

    public Institution getInstitution() {
        return institution;
    }

    public InstitutionRank getRank() {
        return rank;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
        this.dirty = true;
    }

    public void setRank(InstitutionRank rank) {
        this.rank = rank;
        this.dirty = true;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void setDirty(boolean bool) {
        this.dirty = bool;
    }

    public void flush() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "UPDATE Institution SET institution = ?, rank = ? WHERE playerUUID = ?"
            );
            pst.setInt(1, this.institution.id);
            pst.setInt(2, this.rank.id);
            pst.setString(3, this.playerUUID);
            pst.execute();
        } catch (SQLException e) {
            NinkaiEco.getProvidingPlugin(NinkaiEco.class).getLogger().log(Level.SEVERE, e.getMessage());
        }
    }
}
