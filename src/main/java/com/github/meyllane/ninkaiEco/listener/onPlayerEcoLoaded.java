package com.github.meyllane.ninkaiEco.listener;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.dataclass.BankOperation;
import com.github.meyllane.ninkaiEco.dataclass.Notification;
import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerSalary;
import com.github.meyllane.ninkaiEco.enums.BankOperationType;
import com.github.meyllane.ninkaiEco.enums.SalaryStatus;
import com.github.meyllane.ninkaiEco.events.PlayerEcoLoadedEvent;
import com.github.meyllane.ninkaiEco.utils.PluginComponentProvider;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;

public class onPlayerEcoLoaded implements Listener {
    private final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);
    private final BukkitAudiences adventure = plugin.adventure();
    private final MiniMessage mm = MiniMessage.miniMessage();

    @EventHandler()
    public void onEcoLoaded(PlayerEcoLoadedEvent event) {
        Player player = event.getPlayer();
        this.handlePlayerSalary(player, event.getPlayerEco());

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> this.handlePlayerNotifications(player));
    }

    public void handlePlayerSalary(Player player, PlayerEco playerEco) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, bukkitTask -> {
            ArrayList<PlayerSalary> list = PlayerSalary.getPendingPlayerSalaries(playerEco.getPlayerUUID());
            if (list.isEmpty()) return;
            list.forEach(playerSalary -> {
                playerSalary.setStatus(SalaryStatus.REDEEMED);
                playerSalary.flush();
                BankOperation bankOperation = new BankOperation(
                        playerSalary.getEmitterUUID(),
                        playerSalary.getReceiverUUID(),
                        playerSalary.getAmount(),
                        BankOperationType.SALARY
                );
                bankOperation.flush();
            });

            int total = list.stream()
                    .map(PlayerSalary::getAmount)
                    .reduce(0, Integer::sum);
            playerEco.addBankMoney(total);

            //Back in the main thread
            plugin.getServer().getScheduler().runTask(plugin, bukkitTask1 -> {
                adventure.player(player).sendMessage(
                        PluginComponentProvider.getPluginHeader()
                                .append(mm.deserialize(String.format(
                                        "<color:#bfbfbf>%,d salaire(s) trouvé(s) ! Un virement de %,d ryos a été fait vers votre compte.",
                                        list.size(),
                                        total
                                )))
                );
            });
        });
    }

    /**
     * Handles Notifications retrieval and processing. Has to be executed asynchronously.
     * @param player
     */
    public void handlePlayerNotifications(Player player) {
        ArrayList<Notification> notifs = Notification.getNotifications(player.getUniqueId().toString());
        notifs.forEach(notif -> {
            plugin.getServer().getScheduler().runTask(plugin, notif::send);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, notif::delete);
        });
    }
}
