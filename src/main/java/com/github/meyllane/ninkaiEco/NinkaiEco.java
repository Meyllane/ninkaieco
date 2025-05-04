package com.github.meyllane.ninkaiEco;

import com.github.meyllane.ninkaiEco.command.*;
import com.github.meyllane.ninkaiEco.dataclass.*;
import com.github.meyllane.ninkaiEco.enums.SellOrderStatus;
import com.github.meyllane.ninkaiEco.listener.onPlayerEcoLoaded;
import com.github.meyllane.ninkaiEco.listener.onPlayerJoin;
import com.github.meyllane.ninkaiEco.listener.onPlayerQuit;
import me.Seisan.plugin.Main;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class NinkaiEco extends JavaPlugin implements Listener {
    public static HashMap<String, PlayerEco> playerEcoMap = new HashMap<>();
    private BukkitAudiences adventure;
    private boolean salaryStart;
    private FileConfiguration config;

    public static HashMap<String, Plot> allPlots = new HashMap<>();
    public static HashMap<String, PlayerEco> playerEcos = new HashMap<>();

    @Override
    public void onEnable() {
        this.adventure = BukkitAudiences.create(this);
        this.getServer().getPluginManager().registerEvents(new onPlayerJoin(), this);
        this.getServer().getPluginManager().registerEvents(new onPlayerQuit(), this);
        this.getServer().getPluginManager().registerEvents(new onPlayerEcoLoaded(), this);

        EcoCommand.register();
        InstitCommand.register();
        ArtisanCommand.register();
        PlotCommand.register();
        HPACommand.register();

        playerEcos = PlayerEco.getAllPlayerEco();
        allPlots = Plot.getAllPlots();

        //Save the config.yml file as in the resources if it does not exist already
        saveDefaultConfig();
        this.config = this.getConfig();

        this.salaryStart = this.config.getBoolean("salary-start");

        this.closeExpiredSellOrder();

        if (!SalaryTimer.isLastSalaryDateSet()) SalaryTimer.insertLastSalaryDate();

        if (this.salaryStart && SalaryTimer.isTimeForSalary()) {
            this.handlePlayerSalaries();
            ArrayList<HPA> hpas = HPA.getAllOnGoingHPA();
            hpas.forEach(HPA::handlePayment);
        }
    }

    @Override
    public void onDisable() {
        if(this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        this.saveConfig();
    }

    public @NonNull BukkitAudiences adventure() {
        return this.adventure;
    }

    public boolean isSalaryStart() {
        return salaryStart;
    }

    public void setSalaryStart(boolean salaryStart) {
        this.salaryStart = salaryStart;
        this.config.set("salary-start", salaryStart);
    }

    private void closeExpiredSellOrder() {
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT ID FROM SellOrder WHERE status = ?"
            );
            pst.setInt(1, SellOrderStatus.PENDING.id);
            ResultSet res = pst.executeQuery();
            while (res.next()) {
                SellOrder order = SellOrder.getSellOrder(res.getInt("ID"));
                if (order.isExpired()) {
                    order.setStatus(SellOrderStatus.CANCELED);
                    order.flush();
                }
            }
            pst.close();
        } catch (SQLException e) {
            this.getLogger().log(Level.SEVERE, e.getMessage());
        }
    }

    public void handlePlayerSalaries() {
        List<PlayerEco> playerEcoList = PlayerEco.getAll();
        if (playerEcoList == null) return;

        playerEcoList.forEach(playerEco -> {
            if (playerEco.getMonthlySalary() == 0) return;
            int salary = playerEco.getMonthlySalary();
            playerEco.addBankMoney(salary);
            playerEco.flush();
            String message = String.format("<color:#bfbfbf>Votre salaire est arrivé ! %,d ryos ont été versés sur votre compte.", salary);
            new Notification(salary, playerEco.getPlayerUUID(), message).flush();
        });

        SalaryTimer.setLastSalaryDate(new Date());
    }
}
