package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import me.Seisan.plugin.Main;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SalaryTimer {
    public static boolean isLastSalaryDateSet() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT COUNT(*) AS total FROM SalaryTimer WHERE ID = 1"
            );
            ResultSet res = pst.executeQuery();
            res.next();
            boolean bool = res.getInt("total") > 0;
            pst.close();
            return bool;
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error while checking if the lastSalaryDate was set :" + e.getMessage()
            );
        }
        return false;
    }

    public static boolean insertLastSalaryDate() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "INSERT INTO SalaryTimer(lastSalaryDate) VALUES (?)"
            );
            pst.setTimestamp(1, new Timestamp(new Date().getTime()));
            pst.execute();
            pst.close();
            return true;
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error while inserting lastSalaryDate :" + e.getMessage()
            );
        }
        return false;
    }

    public static Date getLastSalaryDate() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT lastSalaryDate FROM SalaryTimer WHERE ID = 1"
            );
            ResultSet res = pst.executeQuery();
            res.next();
            Date date = new Date(res.getTimestamp("lastSalaryDate").getTime());
            pst.close();
            return date;

        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error while getting lastSalaryDate :" + e.getMessage()
            );
        }
        return null;
    }

    public static boolean isTimeForSalary() {
        Date lastSalaryDate = SalaryTimer.getLastSalaryDate();
        if (lastSalaryDate == null) return false;

        long diff = new Date().getTime() - lastSalaryDate.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 30;
    }

    public static void setLastSalaryDate(Date date) {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "UPDATE SalaryTimer SET lastSalaryDate = ? WHERE ID = 1"
            );
            pst.setTimestamp(1, new java.sql.Timestamp(date.getTime()));
            pst.execute();
            pst.close();
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error while updating the lastSalaryDate: " + e.getMessage()
            );
        }
    }
}
