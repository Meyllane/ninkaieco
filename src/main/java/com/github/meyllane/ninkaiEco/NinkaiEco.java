package com.github.meyllane.ninkaiEco;

import com.github.meyllane.ninkaiEco.command.ArtisanCommand;
import com.github.meyllane.ninkaiEco.command.EcoCommand;
import com.github.meyllane.ninkaiEco.command.InstitCommand;
import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerSalary;
import com.github.meyllane.ninkaiEco.dataclass.SalaryTimer;
import com.github.meyllane.ninkaiEco.dataclass.SellOrder;
import com.github.meyllane.ninkaiEco.enums.SalaryStatus;
import com.github.meyllane.ninkaiEco.enums.SellOrderStatus;
import com.github.meyllane.ninkaiEco.listener.onPlayerEcoLoaded;
import com.github.meyllane.ninkaiEco.listener.onPlayerJoin;
import com.github.meyllane.ninkaiEco.listener.onPlayerQuit;
import me.Seisan.plugin.Main;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public final class NinkaiEco extends JavaPlugin implements Listener {
    public static HashMap<String, PlayerEco> playerEcoMap = new HashMap<>();
    private BukkitAudiences adventure;
    private boolean salaryStart;
    private FileConfiguration config;


    @Override
    public void onEnable() {
        this.adventure = BukkitAudiences.create(this);
        this.getServer().getPluginManager().registerEvents(new onPlayerJoin(), this);
        this.getServer().getPluginManager().registerEvents(new onPlayerQuit(), this);
        this.getServer().getPluginManager().registerEvents(new onPlayerEcoLoaded(), this);

        EcoCommand.register();
        InstitCommand.register();
        ArtisanCommand.register();

        //Save the config.yml file as in the resources if it does not exist already
        saveDefaultConfig();
        this.config = this.getConfig();

        this.salaryStart = this.config.getBoolean("salary-start");

        this.closeExpiredSellOrder();

        if (!SalaryTimer.isLastSalaryDateSet()) SalaryTimer.insertLastSalaryDate();

        if (this.salaryStart && SalaryTimer.isTimeForSalary()) this.handlePlayerSalaries();
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

        playerEcoList.forEach(playerEco -> this.getServer().getScheduler().runTaskAsynchronously(this, bukkitTask -> {
            if (playerEco.getMonthlySalary() == 0) return;
            PlayerSalary salary = new PlayerSalary(
                    "Server",
                    playerEco.getPlayerUUID(),
                    playerEco.getMonthlySalary(),
                    SalaryStatus.PENDING);
            salary.flush();
        }));

        SalaryTimer.setLastSalaryDate(new Date());
    }
}
