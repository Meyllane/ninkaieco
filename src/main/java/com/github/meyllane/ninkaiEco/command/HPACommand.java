package com.github.meyllane.ninkaiEco.command;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.dataclass.HPAContractor;
import com.github.meyllane.ninkaiEco.dataclass.Plot;
import com.github.meyllane.ninkaiEco.enums.ErrorMessage;
import com.github.meyllane.ninkaiEco.utils.PluginComponentProvider;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.FloatArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashMap;

import static com.github.meyllane.ninkaiEco.command.PlotCommand.getPlot;

public class HPACommand {
    private static final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);
    private static final BukkitScheduler scheduler = plugin.getServer().getScheduler();
    private static final BukkitAudiences adventure = plugin.adventure();
    private static final MiniMessage mm = MiniMessage.miniMessage();

    private static final HashMap<String, ArgumentSuggestions<CommandSender>> suggestions = new HashMap<>();

    public static void register() {
        suggestions.put("ownedHPA", ArgumentSuggestions.strings(info -> {
            if (!(info.sender() instanceof Player player)) return new String[0];

            return NinkaiEco.allPlots.values().stream()
                    .filter(plot -> plot.getHpa() != null)
                    .filter(plot -> plot.getHpa().hasContractor(player.getUniqueId().toString()))
                    .map(Plot::getName)
                    .toArray(String[]::new);
        }));

        new CommandTree("hpa")
                .then(
                        new StringArgument("plot").replaceSuggestions(suggestions.get("ownedHPA"))
                                .thenNested(
                                        new LiteralArgument("set")
                                                .thenNested(
                                                        new LiteralArgument("hpa_contrib"),
                                                        new FloatArgument("perc", 0.01f, 1f)
                                                                .withPermission("ninkaieco.hpa.set.hpa_contrib")
                                                                .executes(HPACommand::setHPAContrib)
                                                )
                                )
                )
                .register();
    }

    private static void setHPAContrib(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        Plot plot = getPlot(args.getByClass("plot", String.class));
        float perc = args.getByClass("perc", Float.class);

        if (plot.getHpa() == null) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.PLOT_DOESNT_HAVE_HPA.message)
            );
        }

        if (!plot.getHpa().hasContractor(player.getUniqueId().toString())) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.PLAYER_NOT_PLOT_OWNER.message)
            );
        }

        HPAContractor hpaContractor = plot.getHpa().getContractors().stream()
                .filter(contractor -> contractor.getPlayerEco().getPlayerUUID().equals(player.getUniqueId().toString()))
                .findFirst()
                .orElseThrow();

        hpaContractor.setSalaryProp(perc);
        scheduler.runTaskAsynchronously(plugin, hpaContractor::flushUpsert);

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader().append(
                        mm.deserialize("<color:#bfbfbf>La part de votre salaire allant vers votre contrat de location vente a été changée!")
                )
        );
    }
}
