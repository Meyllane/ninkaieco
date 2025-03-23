package com.github.meyllane.ninkaiEco.command;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerInstitution;
import com.github.meyllane.ninkaiEco.enums.ErrorMessage;
import com.github.meyllane.ninkaiEco.enums.Institution;
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

import java.util.Arrays;
import java.util.HashMap;

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

        new CommandTree("instit")
                .withPermission("ninkaieco.instit.self.see")
                .executes(InstitCommand::seeInstit)
                .then(new OfflinePlayerArgument("target")
                        .then(new LiteralArgument("set")
                                .thenNested(
                                        suggestions.get("institShortName"),
                                        suggestions.get("institRankShortName")
                                                .withPermission("ninkaieco.instit.set")
                                                .executes(InstitCommand::setPlayerInstitution)
                                )
                        )
                        .withPermission("ninkaieco.instit.other.see")
                        .executes(InstitCommand::seeInstit)
                )
                .register();
    }

    private static void seeInstit(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;

        OfflinePlayer target = args.getByClassOrDefault("target", OfflinePlayer.class, player);

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
        boolean isConnected = target.isConnected();

        scheduler.runTaskAsynchronously(plugin, () -> {
            PlayerInstitution targetInstit;
            if (isConnected) {
                targetInstit = NinkaiEco.playerEcoMap.get(target.getUniqueId().toString()).getPlayerInstitution();
            } else {
                targetInstit = PlayerEco.get(target.getUniqueId().toString()).getPlayerInstitution();
            }

            scheduler.runTask(plugin, () -> {
                Component infoPart = mm.deserialize(String.format("""
                <color:#bfbfbf>
                Institution: %s
                Rang: %s""", targetInstit.getInstitution().name, targetInstit.getRank().name));

                adventure.player(player).sendMessage(
                        PluginComponentProvider.getPluginHeader()
                                .append(message)
                                .append(infoPart)
                );
            });
        });
    }

    private static void setPlayerInstitution(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        OfflinePlayer target = args.getByClass("target", OfflinePlayer.class);
        Institution instit = Institution.getByShortName(args.getByClass("institShortName", String.class));
        InstitutionRank rank = InstitutionRank.getByShortName(args.getByClass("institRankShortName", String.class));

        if (target == null) {
            throw CommandAPIBukkit.failWithAdventureComponent(
                    PluginComponentProvider.getErrorComponent(ErrorMessage.NONE_EXISTING_OR_NEVER_SEEN_PLAYER.message)
            );
        }

        //If the institution is none, then the rank can't be anything else than none
        if (instit == Institution.NONE && rank != InstitutionRank.NONE) {
            rank = InstitutionRank.NONE;
        }

        //If the institution is not none, then the rank can't be none
        if (instit != Institution.NONE && rank == InstitutionRank.NONE) {
            rank = InstitutionRank.MEMBER;
        }

        boolean isConnected = target.isConnected();

        InstitutionRank finalRank = rank;
        scheduler.runTaskAsynchronously(plugin, () -> {
            PlayerEco targetEco;
            Component targetComponent;
            if (isConnected) {
                targetEco = NinkaiEco.playerEcoMap.get(target.getUniqueId().toString());
                targetComponent = target.getPlayer().displayName();
            } else {
                targetEco = PlayerEco.get(target.getUniqueId().toString());
                targetComponent = Component.text(target.getName());
            }
            targetEco.getPlayerInstitution().setInstitution(instit);
            targetEco.getPlayerInstitution().setRank(finalRank);

            if (!isConnected) scheduler.runTaskAsynchronously(plugin, targetEco::flush);

            scheduler.runTask(plugin, () -> {
                Component infoPart = mm.deserialize(String.format("""
                <color:#bfbfbf>
                Institution: %s
                Rang: %s
                """, instit.name, finalRank.name));

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
            });
        });
    }
}
