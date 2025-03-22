package com.github.meyllane.ninkaiEco.dataclass;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.enums.SellOrderStatus;
import me.Seisan.plugin.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SellOrder {
    private int ID;
    private final String sellerUUID;
    private final String buyerUUID;
    private final String detail;
    private final int price;
    private final int marginPerc;
    private final int marginValue;
    private SellOrderStatus status;
    private final Date createdAt;
    private Date updatedAt;

    private final MiniMessage mm = MiniMessage.miniMessage();

    public SellOrder(int ID, String sellerUUID, String buyerUUID, String detail, int price, int marginPerc, int marginValue, SellOrderStatus status) {
        this.ID = ID;
        this.sellerUUID = sellerUUID;
        this.buyerUUID = buyerUUID;
        this.detail = detail;
        this.price = price;
        this.marginPerc = marginPerc;
        this.marginValue = marginValue;
        this.status = status;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public SellOrder(int ID, String sellerUUID, String buyerUUID, String detail, int price, int marginPerc, int marginValue, SellOrderStatus status,
                     Date createdAt, Date updatedAt) {
        this.ID = ID;
        this.sellerUUID = sellerUUID;
        this.buyerUUID = buyerUUID;
        this.detail = detail;
        this.price = price;
        this.marginPerc = marginPerc;
        this.marginValue = marginValue;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getID() {
        return ID;
    }

    public String getSellerUUID() {
        return sellerUUID;
    }

    public String getBuyerUUID() {
        return buyerUUID;
    }


    public int getPrice() {
        return price;
    }

    public int getMarginValue() {
        return marginValue;
    }

    public SellOrderStatus getStatus() {
        return status;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setStatus(SellOrderStatus status) {
        this.status = status;
    }

    public void flush() {
        if (this.ID > -1) {
            this.update();
        } else {
            this.create();
        }
    }

    public boolean isExpired() {
        return this.getAge() > 7;
    }

    public long getAge() {
        long diff = this.createdAt.getTime() - (new java.util.Date()).getTime();
        return Math.abs(TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS));
    }

    private void create() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    """
                            INSERT INTO SellOrder(sellerUUID, buyerUUID, detail, price, marginPerc, marginValue, status, createdAt, updatedAt)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            pst.setString(1, this.sellerUUID);
            pst.setString(2, this.buyerUUID);
            pst.setString(3, this.detail);
            pst.setInt(4, this.price);
            pst.setInt(5, this.marginPerc);
            pst.setInt(6, this.marginValue);
            pst.setInt(7, this.status.id);
            pst.setTimestamp(8, new java.sql.Timestamp(this.createdAt.getTime()));
            pst.setTimestamp(9, new java.sql.Timestamp(this.updatedAt.getTime()));
            pst.execute();
            ResultSet res = pst.getGeneratedKeys();
            res.next();
            this.ID = res.getInt(1);
            pst.close();
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error while creating SellOrder: " + e.getMessage()
            );
        }
    }

    private void update() {
        try {
            this.updatedAt = new Date();
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "UPDATE SellOrder SET status = ?, updatedAt = ? WHERE ID = ?"
            );
            pst.setInt(1, this.status.id);
            pst.setTimestamp(2, new java.sql.Timestamp(this.updatedAt.getTime()));
            pst.setInt(3, this.ID);
            pst.execute();
            pst.close();
        } catch (SQLException e) {
            NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(
                    Level.SEVERE,
                    "Error while updating SellOrder: " + e.getMessage()
            );
        }
    }

    public static SellOrder getSellOrder(int id) throws SQLException {
        SellOrder sellOrder;
        PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                "SELECT * FROM SellOrder WHERE ID = ?"
        );
        pst.setInt(1, id);
        ResultSet res = pst.executeQuery();
        if (res.next()) {
            sellOrder = new SellOrder(
                    id,
                    res.getString("sellerUUID"),
                    res.getString("buyerUUID"),
                    res.getString("detail"),
                    res.getInt("price"),
                    res.getInt("marginPerc"),
                    res.getInt("marginValue"),
                    SellOrderStatus.getByID(res.getInt("status")),
                    new Date(res.getTimestamp("createdAt").getTime()),
                    new Date(res.getTimestamp("updatedAt").getTime())
            );
        } else {
            sellOrder = null;
        }
        pst.close();
        return sellOrder;
    }

    public Component getInfoComponent() {
        return mm.deserialize(String.format("""
                <color:#bfbfbf>
                • Identifiant: %d
                • Intitulé: %s

                • Prix: %,d
                • Marge artisan: %,d%%
                • Marge nette: %,d
                """, this.ID, this.detail, this.price, this.marginPerc, this.marginValue));
    }
}
