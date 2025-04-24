package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.enums.BankOperationType;
import com.github.meyllane.ninkaiEco.enums.HPAStatus;
import com.github.meyllane.ninkaiEco.enums.PlotStatus;
import me.Seisan.plugin.Main;
import org.checkerframework.checker.units.qual.A;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

public class HPA {
    private static final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);

    private Integer id;
    private String emitterUUID;
    private int totalPrice;
    private int remainingPrice;
    private Plot plot;
    private HPAStatus status;
    private Date createdAt;
    private Date updatedAt;
    private ArrayList<HPAContractor> contractors = new ArrayList<>();

    public HPA(Integer id, String emitterUUID, int totalPrice, int remainingPrice, Plot plot, HPAStatus status, Date createdAt, Date updatedAt) {
        this.id = id;
        this.emitterUUID = emitterUUID;
        this.totalPrice = totalPrice;
        this.remainingPrice = remainingPrice;
        this.plot = plot;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.contractors = HPAContractor.getContractors(this.id);
    }

    public HPA(int totalPrice, String emitterUUID, Plot plot) {
        this.id = null;
        this.totalPrice = totalPrice;
        this.remainingPrice = this.totalPrice;
        this.emitterUUID = emitterUUID;
        this.plot = plot;
        this.status = HPAStatus.ON_GOING;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public ArrayList<HPAContractor> getContractors() {
        return contractors;
    }

    public Integer getId() {
        return id;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public int getRemainingPrice() {
        return remainingPrice;
    }

    public Plot getPlot() {
        return plot;
    }

    public HPAStatus getStatus() {
        return status;
    }

    public void setStatus(HPAStatus status) {
        this.status = status;
    }

    public void addContractors(HPAContractor contractor) {
        this.contractors.add(contractor);
    }

    public void setContractors(ArrayList<HPAContractor> contractors) {
        this.contractors = contractors;
    }

    public void flush() {
        this.flushHPAAttributs();

        ArrayList<HPAContractor> oldContractors = HPAContractor.getContractors(this.id);
        //Adding new Contractors
        this.contractors.forEach(contractor -> {
            if (!oldContractors.contains(contractor)) contractor.flushUpsert();
        });

        oldContractors.forEach(contractor -> {
            if (!this.contractors.contains(contractor)) contractor.flushDelete();
        });
    }

    private void flushHPAAttributs() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    """
                            INSERT INTO HPA(ID, emitterUUID, totalPrice, remainingPrice, plotID, status, createdAt, updatedAt)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE
                                ID = VALUES(ID),
                                emitterUUID = VALUES(emitterUUID),
                                totalPrice = VALUES(totalPrice),
                                remainingPrice = VALUES(remainingPrice),
                                plotID = VALUES(plotID),
                                status = VALUES(status),
                                createdAt = VALUES(createdAt),
                                updatedAt = VALUES(updatedAt)
                            """, Statement.RETURN_GENERATED_KEYS
            );
            pst.setObject(1, this.id);
            pst.setString(2, this.emitterUUID);
            pst.setInt(3, this.totalPrice);
            pst.setInt(4, this.remainingPrice);
            pst.setInt(5, this.plot.getId());
            pst.setInt(6, this.status.id);
            pst.setTimestamp(7, new java.sql.Timestamp(this.createdAt.getTime()));
            pst.setTimestamp(8, new java.sql.Timestamp(this.updatedAt.getTime()));
            pst.execute();
            ResultSet res = pst.getGeneratedKeys();
            if (res.next()) this.id = res.getInt(1);
            pst.close();
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while flushing HPA: " + e.getMessage()
            );
        }
    }

    public static HPA getOnGoingHPA(Plot plot) {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT * FROM HPA WHERE plotID = ? AND status = ?"
            );
            pst.setInt(1, plot.getId());
            pst.setInt(2, HPAStatus.ON_GOING.id);
            ResultSet res = pst.executeQuery();
            if (res.next()) {
                HPA hpa = new HPA(
                        res.getInt("ID"),
                        res.getString("emitterUUID"),
                        res.getInt("totalPrice"),
                        res.getInt("remainingPrice"),
                        plot,
                        HPAStatus.getByID(res.getInt("status")),
                        new Date(res.getTimestamp("createdAt").getTime()),
                        new Date(res.getTimestamp("updatedAt").getTime())
                );
                pst.close();
                return hpa;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while fetching on going HPA: " + e.getMessage()
            );
        }
        return null;
    }

    public static ArrayList<HPA> getAllOnGoingHPA() {
        ArrayList<HPA> hpas = new ArrayList<>();
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT * FROM HPA WHERE status = ?"
            );
            pst.setInt(1, HPAStatus.ON_GOING.id);
            ResultSet res = pst.executeQuery();
            while (res.next()) {
                int id = res.getInt("ID");
                Plot plot = NinkaiEco.allPlots.values().stream()
                        .filter(p -> p.getHpa() != null && p.getHpa().id == id)
                        .findFirst()
                        .orElseThrow();
                HPA hpa = new HPA(
                        id,
                        res.getString("emitterUUID"),
                        res.getInt("totalPrice"),
                        res.getInt("remainingPrice"),
                        plot,
                        HPAStatus.getByID(res.getInt("status")),
                        new Date(res.getTimestamp("createdAt").getTime()),
                        new Date(res.getTimestamp("updatedAt").getTime())
                );
                hpas.add(hpa);
            }
            pst.close();
            return hpas;
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while fetching all on going HPA: " + e.getMessage()
            );
        }
        return hpas;
    }

    public int getTotalMonthlyPayment() {
        return this.contractors.stream()
                .map(HPAContractor::getMonthlyPayment)
                .reduce(0, Integer::sum);
    }

    public void handlePayment() {
        int totalPayment = this.getTotalMonthlyPayment();

        //Last cycle of payment
        if (this.remainingPrice < totalPayment) {
            for (HPAContractor contractor: this.contractors) {
                float prop = (float) contractor.getMonthlyPayment() / (float) totalPayment;
                double toPay = Math.ceil((double) this.remainingPrice * (double) prop);
                int payment = (int) toPay;

                contractor.getPlayerEco().removeBankMoney(payment);
                contractor.getPlayerEco().flush();

                //Add a BankOperation
                new BankOperation(
                        "Server",
                        contractor.getPlayerEco().getPlayerUUID(),
                        payment,
                        BankOperationType.HPA
                ).flush();

                //Add a notifcation
                new Notification(
                        payment,
                        contractor.getPlayerEco().getPlayerUUID(),
                        String.format(
                                "<color:#bfbfbf>%,d ryos ont été prélevés de votre salaire pour votre contrat de location-vente ! " +
                                        "Votre contrat est désormais entièrement payé !",
                                payment
                        )
                ).flush();

                //remove the contractor
                contractor.flushDelete();
            }
            this.remainingPrice = 0;
            this.status = HPAStatus.COMPLETED;
            this.contractors = new ArrayList<>();
            this.plot.setHpa(null);
            this.plot.setStatus(PlotStatus.OWNED);
            this.plot.flush();
            this.flush();
        } else { //Just another cycle
            for (HPAContractor contractor : this.contractors) {
                int payment = contractor.getMonthlyPayment();
                //Remove the money from the contractor
                contractor.getPlayerEco().removeBankMoney(payment);
                contractor.getPlayerEco().flush();

                //Add a BankOperation
                new BankOperation(
                        "Server",
                        contractor.getPlayerEco().getPlayerUUID(),
                        payment,
                        BankOperationType.HPA
                ).flush();

                //Add a notifcation
                new Notification(
                        payment,
                        contractor.getPlayerEco().getPlayerUUID(),
                        String.format(
                                "<color:#bfbfbf>%,d ryos ont été prélevés de votre salaire pour votre contrat de location-vente !",
                                payment
                        )
                ).flush();
                //Remove from the remaining price
                this.remainingPrice -= payment;
            }
        }
        this.flush();
    }
}
