package com.github.meyllane.ninkaiEco.command;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerInstitution;
import com.github.meyllane.ninkaiEco.enums.Institution;
import com.github.meyllane.ninkaiEco.enums.InstitutionRank;
import com.github.meyllane.ninkaiEco.utils.PluginComponentProvider;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;

public class InstitCommand {
    private static final BukkitAudiences adventure = NinkaiEco.getPlugin(NinkaiEco.class).adventure();
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final HashMap<String, Argument<?>> suggestions = new HashMap<>();

    public static void register() {
        suggestions.put("institShortName",
                new StringArgument("institShortName").replaceSuggestions(ArgumentSuggestions.strings(info ->
                    Arrays.stream(Institution.values()).map(instit -> instit.shortName).toArray(String[]::new))));

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
                .then(new PlayerArgument("target")
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

        Player target = args.getByClassOrDefault("target", Player.class, player);
        PlayerInstitution playerInstitution = NinkaiEco.playerEcoMap.get(target.getUniqueId().toString()).getPlayerInstitution();

        Component message;
        if (player == target) {
            message = mm.deserialize("<color:#bfbfbf>Votre profil d'institution :<newline>");
        } else {
            message = mm.deserialize("<color:#bfbfbf>Profil d'institution de ")
                    .append(target.displayName())
                    .append(mm.deserialize("<newline>"));
        }

        Component infoPart = mm.deserialize(String.format("""
                <color:#bfbfbf>
                Institution: %s
                Rang: %s
                """, playerInstitution.getInstitution().name, playerInstitution.getRank().name));

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(message)
                        .append(infoPart)
        );
    }

    private static void setPlayerInstitution(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;

        Player target = args.getByClass("target", Player.class);
        Institution instit = Institution.getByShortName(args.getByClass("institShortName", String.class));
        InstitutionRank rank = InstitutionRank.getByShortName(args.getByClass("institRankShortName", String.class));

        //If the institution is none, then the rank can't be anything else than none
        if (instit == Institution.NONE && rank != InstitutionRank.NONE) {
            rank = InstitutionRank.NONE;
        }

        //If the institution is not none, then the rank can't be none
        if (instit != Institution.NONE && rank == InstitutionRank.NONE) {
            rank = InstitutionRank.MEMBER;
        }

        PlayerEco targetEco = NinkaiEco.playerEcoMap.get(target.getUniqueId().toString());
        targetEco.getPlayerInstitution().setInstitution(instit);
        targetEco.getPlayerInstitution().setRank(rank);

        Component infoPart = mm.deserialize(String.format("""
                <color:#bfbfbf>
                Institution: %s
                Rang: %s
                """, instit.name, rank.name));

        Component playerPart = mm.deserialize("<color:#bfbfbf>Vous avez mis à jour le profil d'institution de ")
                .append(target.displayName());

        Component targetPart = mm.deserialize("<color:#bfbfbf>Votre profil d'institution a été mis à jour :");

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(playerPart)
                        .append(infoPart)
        );

        adventure.player(target).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(targetPart)
                        .append(infoPart)
        );

        CommandAPI.updateRequirements(target);
    }
}
