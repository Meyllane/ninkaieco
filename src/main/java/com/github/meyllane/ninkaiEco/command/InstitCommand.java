package com.github.meyllane.ninkaiEco.command;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerInstitution;
import com.github.meyllane.ninkaiEco.enums.ErrorMessage;
import com.github.meyllane.ninkaiEco.enums.Institution;
import com.github.meyllane.ninkaiEco.enums.InstitutionDivision;
import com.github.meyllane.ninkaiEco.enums.InstitutionRank;
import com.github.meyllane.ninkaiEco.utils.PluginComponentProvider;
import dev.jorel.commandapi.CommandAPI;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class InstitCommand {
    private static final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);
    private static final BukkitAudiences adventure = plugin.adventure();
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final HashMap<String, Argument<?>> suggestions = new HashMap<>();
    private static final BukkitScheduler scheduler = plugin.getServer().getScheduler();

    /**
     * Registers the <code>CommandTree</code>
     */
    public static void register() {

        //Provides the correct Institution shortnames as suggestions
        suggestions.put("institShortName",
                new StringArgument("institShortName").replaceSuggestions(ArgumentSuggestions.strings(info ->
                    Arrays.stream(Institution.values()).map(instit -> instit.shortName).toArray(String[]::new))));

        //Provides the correct
        suggestions.put("institRankShortName",
                new StringArgument("institRankShortName").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    String institShortName = (String) info.previousArgs().get("institShortName");
                    if (institShortName.equals(Institution.NONE.shortName)) return new String[]{InstitutionRank.NONE.shortName};

                    return Arrays.stream(InstitutionRank.values())
                            .map(rank -> rank.shortName)
                            .filter(rank -> !rank.equals(InstitutionRank.NONE.shortName))
                            .toArray(String[]::new);
        })));

        suggestions.put("subDivisions",
                new StringArgument("subDivision").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    List<String> res = new ArrayList<>();
                    res.add("none");

                    OfflinePlayer target = plugin.getServer().getOfflinePlayerIfCached(info.previousArgs().getByClass("target", String.class));
                    if (target == null) return res.toArray(new String[0]);

                    PlayerEco targetEco = NinkaiEco.playerEcos.get(target.getUniqueId().toString());
                    List<String> divisions = targetEco.getPlayerInstitution()
                            .getInstitution()
                            .divisions.stream()
                            .map(div -> div.shortName)
                            .toList();

                    res.addAll(divisions);
                    return res.toArray(new String[0]);
                })));

        suggestions.put("offlinePlayers",
                new StringArgument("target").replaceSuggestions(ArgumentSuggestions.strings(
                    Arrays.stream(plugin.getServer().getOfflinePlayers())
                            .map(OfflinePlayer::getName)
                            .toArray(String[]::new)
                )));

        new CommandTree("instit")
                .withPermission("ninkaieco.instit.self.see")
                .executes(InstitCommand::seeInstit)
                .then(suggestions.get("offlinePlayers")
                        .then(new LiteralArgument("set")
                                .thenNested(
                                        new LiteralArgument("instit"),
                                        suggestions.get("institShortName"),
                                        suggestions.get("institRankShortName")
                                                .withPermission("ninkaieco.instit.set.instit")
                                                .executes(InstitCommand::setPlayerInstitution)
                                )
                                .thenNested(
                                        new LiteralArgument("division"),
                                        suggestions.get("subDivisions")
                                                .withPermission("ninkaieco.instit.set.division")
                                                .executes(InstitCommand::setPlayerDivision)
                                )
                        )
                        .withPermission("ninkaieco.instit.other.see")
                        .executes(InstitCommand::seeInstit)
                )
                .register();
    }

    private static void seeInstit(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;

        String targetName = args.getByClass("target", String.class);
        if (targetName == null) {
            targetName = player.getName();
        }
        OfflinePlayer target = plugin.getServer().getOfflinePlayerIfCached(targetName);

        Component message;
        if (player.getUniqueId().equals(target.getUniqueId())) {
            message = mm.deserialize("<color:#bfbfbf>Votre profil d'institution :<newline>");
        } else {
            Component targetComponent;
            if (target.isConnected()) {
                targetComponent = target.getPlayer().displayName();
            } else {
                targetComponent = Component.text(target.getName());
            }
            message = mm.deserialize("<color:#bfbfbf>Profil d'institution de ")
                    .append(targetComponent)
                    .append(mm.deserialize("<newline>"));
        }

        PlayerInstitution targetInstit = NinkaiEco.playerEcos.get(target.getUniqueId().toString()).getPlayerInstitution();

        Component infoPart = mm.deserialize(String.format("""
                <color:#bfbfbf>
                Institution: %s
                Division: %s
                Rang: %s""", targetInstit.getInstitution().name, targetInstit.getDivision().fullName, targetInstit.getRank().name));

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(message)
                        .append(infoPart)
        );
    }

    private static void setPlayerInstitution(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        OfflinePlayer target = plugin.getServer().getOfflinePlayerIfCached(args.getByClass("target", String.class));
        Institution instit = Institution.getByShortName(args.getByClass("institShortName", String.class));
        InstitutionRank rank = InstitutionRank.getByShortName(args.getByClass("institRankShortName", String.class));

        if (target == null) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.NONE_EXISTING_OR_NEVER_SEEN_PLAYER.message)
            );
        }

        PlayerEco targetEco = NinkaiEco.playerEcos.get(target.getUniqueId().toString());

        //If the institution is none, then the rank can't be anything else than none
        if (instit == Institution.NONE && rank != InstitutionRank.NONE) {
            rank = InstitutionRank.NONE;
        }

        //If the institution is not none, then the rank can't be none
        if (instit != Institution.NONE && rank == InstitutionRank.NONE) {
            rank = InstitutionRank.MEMBER;
        }

        //If change the insttit, remove the division
        if (instit != targetEco.getPlayerInstitution().getInstitution()) {
            targetEco.getPlayerInstitution().setDivision(InstitutionDivision.NONE);
        }

        boolean isConnected = target.isConnected();

        Component targetComponent;

        if (isConnected) {
            targetComponent = target.getPlayer().displayName();
        } else {
            targetComponent = Component.text(target.getName());
        }

        targetEco.getPlayerInstitution().setInstitution(instit);
        targetEco.getPlayerInstitution().setRank(rank);

        scheduler.runTaskAsynchronously(plugin, targetEco::flush);

        Component infoPart = mm.deserialize(String.format("""
                <color:#bfbfbf>
                Institution: %s
                Division: %s
                Rang: %s
                """, instit.name, targetEco.getPlayerInstitution().getDivision().fullName, rank.name));

        Component playerPart = mm.deserialize("<color:#bfbfbf>Vous avez mis à jour le profil d'institution de ")
                .append(targetComponent);

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(playerPart)
                        .append(infoPart)
        );

        if (isConnected) {
            Component targetPart = mm.deserialize("<color:#bfbfbf>Votre profil d'institution a été mis à jour :");
            adventure.player(target.getPlayer()).sendMessage(
                    PluginComponentProvider.getPluginHeader()
                            .append(targetPart)
                            .append(infoPart)
            );
            CommandAPI.updateRequirements(target.getPlayer());
        }
    }

    private static void setPlayerDivision(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        String divisionName = args.getByClass("subDivision", String.class);
        OfflinePlayer target = plugin.getServer().getOfflinePlayerIfCached(args.getByClass("target", String.class));
        if (target == null) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.NONE_EXISTING_OR_NEVER_SEEN_PLAYER.message)
            );
        }

        PlayerEco targetEco = NinkaiEco.playerEcos.get(target.getUniqueId().toString());
        InstitutionDivision division = InstitutionDivision.getByShortName(divisionName);

        if (!targetEco.getPlayerInstitution().getInstitution().divisions.contains(division) && division != InstitutionDivision.NONE) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.INCCORECT_DIVISION.message)
            );
        }

        targetEco.getPlayerInstitution().setDivision(division);

        scheduler.runTaskAsynchronously(plugin, targetEco::flush);

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize(String.format(
                                "<color:#bfbfbf>Le profil d'institution de %s a été mis à jour :",
                                target.getName()
                        )))
                        .append(getInfoComponent(targetEco.getPlayerInstitution()))
        );

        if (target.isConnected()) CommandAPI.updateRequirements(target.getPlayer());
    }

    private static Component getInfoComponent(PlayerInstitution playerInstitution) {
        return mm.deserialize(String.format("""
                <color:#bfbfbf>
                Institution: %s
                Division: %s
                Rang: %s
                """,
                playerInstitution.getInstitution().name,
                playerInstitution.getDivision().fullName,
                playerInstitution.getRank().name
        ));
    }
}
