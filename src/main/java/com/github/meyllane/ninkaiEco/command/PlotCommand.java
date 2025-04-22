package com.github.meyllane.ninkaiEco.command;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import com.github.meyllane.ninkaiEco.dataclass.Plot;
import com.github.meyllane.ninkaiEco.enums.ErrorMessage;
import com.github.meyllane.ninkaiEco.enums.PlotStatus;
import com.github.meyllane.ninkaiEco.utils.PluginComponentProvider;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.github.meyllane.ninkaiEco.NinkaiEco.allPlots;

public class PlotCommand {
    private static final HashMap<String, ArgumentSuggestions<CommandSender>> suggestionMap = new HashMap<>();

    private static final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);
    private static final BukkitScheduler scheduler = plugin.getServer().getScheduler();
    private static final BukkitAudiences adventure = plugin.adventure();
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static void register() {
        suggestionMap.put("allPlotNames", ArgumentSuggestions.stringsAsync(info ->
            CompletableFuture.supplyAsync(() -> {
                return Objects.requireNonNull(allPlots).keySet().toArray(new String[0]);
            })
        ));

        suggestionMap.put("plotStatus", ArgumentSuggestions.strings(info ->
                Arrays.stream(PlotStatus.values()).map(plot -> "'" + plot.name + "'").toArray(String[]::new)
        ));

        suggestionMap.put("offlinePlayers", ArgumentSuggestions.strings(info ->
                Arrays.stream(plugin.getServer().getOfflinePlayers()).map(OfflinePlayer::getName).toArray(String[]::new)
        ));

        suggestionMap.put("plotOwners", ArgumentSuggestions.strings(info -> {
            String plotName = info.previousArgs().getByClass("plot", String.class);
            if (!allPlots.containsKey(plotName)) return new String[0];

            Plot plot = allPlots.get(plotName);
            return Arrays.stream(plugin.getServer().getOfflinePlayers())
                    .filter(offlinePlayer -> plot.getOwnersUUID().contains(offlinePlayer.getUniqueId().toString()))
                    .map(OfflinePlayer::getName)
                    .toArray(String[]::new);
        }));

        new CommandTree("plot")
                .thenNested(
                        new LiteralArgument("see"),
                        new StringArgument("plot")
                                .replaceSuggestions(suggestionMap.get("allPlotNames"))
                                .withPermission("ninkaieco.plot.see")
                                .executes(PlotCommand::seePlot)
                )
                .thenNested(
                        new LiteralArgument("checkplots"),
                        new StringArgument("target").setOptional(true).replaceSuggestions(suggestionMap.get("offlinePlayers"))
                                .withPermission("ninkaieco.plot.checkplots")
                                .executes(PlotCommand::seeOwnedPlots)
                )
                .thenNested(
                        new LiteralArgument("create"),
                        new StringArgument("plotName"),
                        new TextArgument("plotStatus").replaceSuggestions(suggestionMap.get("plotStatus")),
                        new IntegerArgument("plotPrice")
                                .withPermission("ninkaieco.plot.create")
                                .executes(PlotCommand::createPlot)
                )
                .thenNested(
                        new LiteralArgument("set"),
                        new StringArgument("plot").replaceSuggestions(suggestionMap.get("allPlotNames"))
                                .thenNested(
                                        new LiteralArgument("price"),
                                        new IntegerArgument("plotPrice")
                                                .withPermission("ninkaieco.plot.set.price")
                                                .executes(PlotCommand::setPrice)
                                )
                                .thenNested(
                                        new LiteralArgument("name"),
                                        new StringArgument("plotName")
                                                .withPermission("ninkaieco.plot.set.name")
                                                .executes(PlotCommand::setName)
                                )
                                .thenNested(
                                        new LiteralArgument("status"),
                                        new TextArgument("plotStatus").replaceSuggestions(suggestionMap.get("plotStatus"))
                                                .withPermission("ninkaieco.plot.set.status")
                                                .executes(PlotCommand::setStatus)
                                )
                                .thenNested(
                                        new LiteralArgument("owner")
                                                .thenNested(
                                                        new LiteralArgument("add"),
                                                        new StringArgument("target").replaceSuggestions(suggestionMap.get("offlinePlayers"))
                                                                .withPermission("ninkaieco.plot.set.owner.add")
                                                                .executes(PlotCommand::addOwner)
                                                )
                                                .thenNested(
                                                        new LiteralArgument("remove"),
                                                        new StringArgument("target").replaceSuggestions(suggestionMap.get("plotOwners"))
                                                                .withPermission("ninkaieco.plot.set.owner.remove")
                                                                .executes(PlotCommand::removeOwner)
                                                )
                                )
                )
                .register();
    }

    private static Plot getPlot(String plotName) throws WrapperCommandSyntaxException {
        if (!allPlots.containsKey(plotName)) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.NONE_EXISTING_PLOT.message)
            );
        }

        return allPlots.get(plotName);
    }

    private static void seePlot(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        Plot plot = getPlot(args.getByClass("plot", String.class));
        String message = String.format("""
                <color:#bfbfbf>Information du plot %s
                Nom: %s
                Statut: %s
                Prix: %,d ryos""", plot.getName(), plot.getFullName(), plot.getStatus().name, plot.getPrice());

        Component toSend = PluginComponentProvider.getPluginHeader()
                .append(mm.deserialize(message));

        if (!plot.getOwnersUUID().isEmpty()) {
            toSend = toSend.appendNewline();
            toSend = toSend.append(mm.deserialize("<color:#bfbfbf>Propriétaire(s): "));
            String[] owners = Arrays.stream(plugin.getServer().getOfflinePlayers())
                    .filter(off -> plot.getOwnersUUID().contains(off.getUniqueId().toString()))
                    .map(OfflinePlayer::getName).filter(Objects::nonNull).toArray(String[]::new);
            for (String name : owners) {
                toSend = toSend.appendNewline().append(mm.deserialize(
                        String.format("<color:#bfbfbf>- %s", name)
                ));
            }
        }

        adventure.player(player).sendMessage(toSend);
    }

    private static void createPlot(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        String plotName = args.getByClass("plotName", String.class);
        String plotStatusString = args.getByClass("plotStatus", String.class);
        int price = args.getByClassOrDefault("plotPrice", Integer.class, 0);

        //Check if the name is already take
        if (allPlots.containsKey(plotName)) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.ALREADY_EXISTING_PLOT.message)
            );
        }

        PlotStatus plotStatus = PlotStatus.getByName(plotStatusString);

        Plot plot = new Plot(plotName, plotStatus, price, player.getUniqueId().toString());

        scheduler.runTaskAsynchronously(plugin, plot::flush);

        allPlots.put(plotName, plot);

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader().append(
                        mm.deserialize("<color:#bfbfbf>Le plot a bien été créé !")
                )
        );
    }

    private static void setPrice(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        Plot plot = getPlot(args.getByClass("plot", String.class));
        int price = args.getByClassOrDefault("plotPrice", Integer.class, 0);

        plot.setPrice(price);
        plot.setUpdatedBy(player.getUniqueId().toString());

        scheduler.runTaskAsynchronously(plugin, plot::flush);

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader().append(
                        mm.deserialize("<color:#bfbfbf>Le prix du plot a bien été mis à jour !")
                )
        );
    }

    private static void setName(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        Plot plot = getPlot(args.getByClass("plot", String.class));
        String newName = args.getByClass("plotName", String.class);

        if (allPlots.containsKey(newName)) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.ALREADY_EXISTING_PLOT.message)
            );
        }

        plot.setName(newName);
        plot.setUpdatedBy(player.getUniqueId().toString());

        //update the hashmap
        allPlots.remove(args.getByClass("plot", String.class));
        allPlots.put(plot.getName(), plot);

        scheduler.runTaskAsynchronously(plugin, plot::flush);

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize("<color:#bfbfbf>Le nom du plot a bien été mise à jour !"))
        );
    }

    private static void setStatus(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        Plot plot = getPlot(args.getByClass("plot", String.class));
        String statusName = args.getByClassOrDefault("plotStatus", String.class, PlotStatus.FREE.name);

        PlotStatus plotStatus = PlotStatus.getByName(statusName);

        plot.setStatus(plotStatus);
        plot.setUpdatedBy(player.getUniqueId().toString());

        scheduler.runTaskAsynchronously(plugin, plot::flush);

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize("<color:#bfbfbf>Le statut du plot a bien été mise à jour !"))
        );
    }

    private static void addOwner(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        Plot plot = getPlot(args.getByClass("plot", String.class));
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args.getByClass("target", String.class));

        if (!plot.getStatus().canHavePlayerOwners) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.PLOT_CANT_HAVE_PLAYER_OWNER.message)
            );
        }

        if (plot.getOwnersUUID().contains(target.getUniqueId().toString())) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.PLAYER_ALREADY_OWNS_THIS_PLOT.message)
            );
        }

        plot.getOwnersUUID().add(target.getUniqueId().toString());

        scheduler.runTaskAsynchronously(plugin, plot::flush);

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize("<color:#bfbfbf>Lae joueur.euse a bien été.e ajouté au plot !"))
        );
    }

    private static void removeOwner(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        Plot plot = getPlot(args.getByClass("plot", String.class));
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args.getByClass("target", String.class));

        if (!plot.getOwnersUUID().contains(target.getUniqueId().toString())) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.PLAYER_NOT_PLOT_OWNER.message)
            );
        }

        plot.getOwnersUUID().remove(target.getUniqueId().toString());

        scheduler.runTaskAsynchronously(plugin, plot::flush);

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize("<color:#bfbfbf>Lae joueur.euse a bien été retiré.e du plot !"))
        );
    }

    private static void seeOwnedPlots(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        String targetName = args.getByClass("target", String.class);
        OfflinePlayer target;
        if (targetName == null) {
            target = player;
        } else {
           target = Arrays.stream(plugin.getServer().getOfflinePlayers())
                   .filter(off -> Objects.equals(off.getName(), targetName))
                   .findFirst()
                   .orElse(null);
        }

        if (target == null) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.NONE_EXISTING_OR_NEVER_SEEN_PLAYER.message)
            );
        }

        scheduler.runTaskAsynchronously(plugin, () -> {
            PlayerEco playerEco = PlayerEco.get(target.getUniqueId().toString());
            scheduler.runTask(plugin, () -> {
                Component message = mm.deserialize(String.format(
                        "<color:#bfbfbf>Propriétés de %s:", target.getName()
                ));

                for (Plot plot : playerEco.getPlotOwned()) {
                    message = message.appendNewline().append(
                            mm.deserialize(String.format(
                                    "<color:#bfbfbf>- %s (%s)", plot.getFullName(), plot.getStatus().name
                            ))
                    );
                }

                adventure.player(player).sendMessage(PluginComponentProvider.getPluginHeader().append(message));
            });
        });
    }
}
