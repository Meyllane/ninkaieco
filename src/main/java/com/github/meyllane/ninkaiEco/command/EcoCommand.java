package com.github.meyllane.ninkaiEco.command;

import com.github.meyllane.ninkaiEco.NinkaiEco;

import com.github.meyllane.ninkaiEco.dataclass.BankOperation;
import com.github.meyllane.ninkaiEco.dataclass.PlayerCash;
import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import com.github.meyllane.ninkaiEco.dataclass.PlayerInstitution;
import com.github.meyllane.ninkaiEco.enums.*;
import com.github.meyllane.ninkaiEco.utils.PluginComponentProvider;
import de.tr7zw.nbtapi.NBT;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import me.Seisan.plugin.Features.objectnum.RPRank;
import me.Seisan.plugin.Main;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;


public class EcoCommand {
    private static final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);
    private static final BukkitAudiences adventure = plugin.adventure();
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final BukkitScheduler scheduler = plugin.getServer().getScheduler();

    public static void register() {
        new CommandTree("eco")
                .then(new LiteralArgument("balance")
                        .then(new PlayerArgument("target")
                                .then(new LiteralArgument("add", "add")
                                        .withPermission("ninkaieco.balance.other.add")
                                        .then(new IntegerArgument("amount")
                                                .executes(EcoCommand::addBalance)
                                        )
                                )
                                .then(new LiteralArgument("remove", "remove")
                                        .withPermission("ninkaieco.balance.other.remove")
                                        .then(new IntegerArgument("amount")
                                                .executes(EcoCommand::removeBalance)
                                        )
                                )
                                .then(new LiteralArgument("set", "set")
                                        .withPermission("ninkaieco.balance.other.set")
                                        .then(new IntegerArgument("amount")
                                                .executes(EcoCommand::setBalance)
                                        )
                                )
                                .withPermission("ninkaieco.balance.other.see")
                                .executes(EcoCommand::seeBalance)
                        )
                        .then(new LiteralArgument("deposit")
                                .withPermission("ninkaieco.balance.self.deposit")
                                .executes(EcoCommand::deposit)
                        )
                        .then(new LiteralArgument("withdraw")
                                .withPermission("ninkaieco.balance.self.withdraw")
                                .then(new IntegerArgument("amount", 1).setOptional(true)
                                        .executes(EcoCommand::withdraw)
                                )
                        )
                        .withPermission("ninkaieco.balance.self.see")
                        .executes(EcoCommand::seeBalance)
                )

                .then(new LiteralArgument("salary")
                        .then(new LiteralArgument("see")
                                .withPermission("ninkaieco.salary.see")
                                .executes(EcoCommand::seeSalaryStart)
                        )
                        .then(new LiteralArgument("set")
                                .withPermission("ninkaieco.salary.set")
                                .then(new BooleanArgument("bool")
                                        .executes(EcoCommand::setSalaryStart)
                                )

                        )
                        .then(new LiteralArgument("force")
                                .then(new PlayerArgument("target")
                                        .withPermission("ninkaieco.salary.force")
                                        .executes(EcoCommand::forceSalary)
                                )
                                .then(new LiteralArgument("all")
                                        .withPermission("ninkaieco.salary.force.all")
                                        .executes(EcoCommand::forceSalaryAll)
                                )
                        )
                )
                .register();
    }

    private static void seeBalance(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;

        Player target = (Player) args.getOrDefault("target", player);

        int bankMoney = NinkaiEco.playerEcoMap.get(target.getUniqueId().toString()).getBankMoney();

        Component toSend;
        //Format the message based on if there is a target or not
        if (target == player) {
            String message = String.format("<color:#bfbfbf>Vous avez %,d ryos sur votre compte et %,d ryos en cash.",
                    bankMoney, EcoCommand.getCashValue(target));
            toSend = mm.deserialize(message);
        } else {
            Component targetName = target.displayName();
            String message = String.format(" <color:#bfbfbf>a %,d ryos sur son compte et %,d ryos en cash.",
                    bankMoney, EcoCommand.getCashValue(target));
            toSend = targetName.append(mm.deserialize(message));
        }

        //Send the message to the sender after adding the PluginHeader
        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(toSend)
        );
    }

    private static void addBalance(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;

        Player target = args.getByClass("target", Player.class);
        int amount = args.getByClassOrDefault("amount", Integer.class, 0);
        amount = Math.abs(amount); //Just in case the amount is negative

        NinkaiEco.playerEcoMap.get(target.getUniqueId().toString()).addBankMoney(amount);

        Component playerMessage = mm.deserialize(
                        String.format("<color:#bfbfbf>Vous avez ajouté %,d ryos sur le compte de ", amount)
                )
                .append(target.displayName())
                .append(Component.text("."));

        Component targetMessage = mm.deserialize(
                String.format("<color:#bfbfbf>%,d ryos ont été ajoutés à votre compte.", amount)
        );

        adventure.player(player).sendMessage(PluginComponentProvider.getPluginHeader().append(playerMessage));
        adventure.player(target).sendMessage(PluginComponentProvider.getPluginHeader().append(targetMessage));
    }

    private static void removeBalance(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;

        Player target = args.getByClass("target", Player.class);
        int amount = args.getByClassOrDefault("amount", Integer.class, 0);
        amount = Math.abs(amount); //Just in case the amount is negative

        NinkaiEco.playerEcoMap.get(target.getUniqueId().toString()).removeBankMoney(amount);

        Component playerMessage = mm.deserialize(
                        String.format("<color:#bfbfbf>Vous avez retiré %,d ryos sur le compte de ", amount)
                )
                .append(target.displayName())
                .append(Component.text("."));

        Component targetMessage = mm.deserialize(
                String.format("<color:#bfbfbf>%,d ryos ont été retirés à votre compte.", amount)
        );

        adventure.player(player).sendMessage(PluginComponentProvider.getPluginHeader().append(playerMessage));
        adventure.player(target).sendMessage(PluginComponentProvider.getPluginHeader().append(targetMessage));
    }

    private static void setBalance(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;

        Player target = args.getByClass("target", Player.class);
        int amount = args.getByClassOrDefault("amount", Integer.class, 0);

        NinkaiEco.playerEcoMap.get(target.getUniqueId().toString()).setBankMoney(amount);

        Component playerMessage = mm.deserialize("<color:#bfbfbf>Vous avez fixé le compte de ")
                .append(target.displayName())
                .append(Component.text(
                        String.format(" à %,d ryos.", amount)
                ));

        Component targetMessage = mm.deserialize(
                String.format("<color:#bfbfbf>Votre compte a été fixé à %,d ryos.", amount)
        );

        adventure.player(player).sendMessage(PluginComponentProvider.getPluginHeader().append(playerMessage));
        adventure.player(target).sendMessage(PluginComponentProvider.getPluginHeader().append(targetMessage));
    }

    private static void deposit(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;

        List<PlayerCash> playerCash = EcoCommand.getPlayerCash(player);

        int amount = EcoCommand.getCashValue(player);

        for (PlayerCash pc : playerCash) {
            player.getInventory().remove(pc.getItemStack());
        }

        NinkaiEco.playerEcoMap.get(player.getUniqueId().toString()).addBankMoney(amount);

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize(String.format("<color:#bfbfbf>Vous avez déposé %,d ryos sur votre compte." +
                                        " Sa valeur est désormais de %,d ryos.",
                                amount, NinkaiEco.playerEcoMap.get(player.getUniqueId().toString()).getBankMoney())))
        );

        scheduler.runTaskAsynchronously(plugin, bukkitTask -> {
            new BankOperation(
                    player.getUniqueId().toString(),
                    player.getUniqueId().toString(),
                    amount,
                    BankOperationType.DEPOSIT
            ).flush();
        });
    }

    private static void withdraw(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;

        int amount = (int) args.getOrDefault("amount", 0);

        if (amount > NinkaiEco.playerEcoMap.get(player.getUniqueId().toString()).getBankMoney()) {
            adventure.player(player).sendMessage(
                    PluginComponentProvider.getPluginHeader()
                            .append(mm.deserialize("<color:#bfbfbf>Vous ne pouvez pas retirer plus que ce que votre compte contient."))
            );
            return;
        }

        List<PlayerCash> playerCash = EcoCommand.subsetAmount(player, amount);
        List<ItemStack> itemsToGive = playerCash.stream().map(PlayerCash::getItemStack).toList();
        playerCash = EcoCommand.addPlayerCashNBT(playerCash);

        //Inventory managment
        ItemStack[] invContent = player.getInventory().getContents();
        HashMap<Integer, ItemStack> tryGive = player.getInventory().addItem(itemsToGive.toArray(new ItemStack[0]));
        if (!tryGive.isEmpty()) { //Failed to give all the items
            player.getInventory().setContents(invContent);
            adventure.player(player).sendMessage(
                    PluginComponentProvider.getPluginHeader()
                            .append(mm.deserialize("<color:#bfbfbf>Vous n'avez pas accès de place dans votre inventaire."))
            );
            return;
        }

        NinkaiEco.playerEcoMap.get(player.getUniqueId().toString()).removeBankMoney(amount);
        int playerMoney = NinkaiEco.playerEcoMap.get(player.getUniqueId().toString()).getBankMoney();

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize(String.format(
                                "<color:#bfbfbf>Vous avez retiré %,d ryos de votre compte." +
                                        " Sa valeur est désormais de %,d ryos.", amount, playerMoney
                        )))
        );

        scheduler.runTaskAsynchronously(plugin, bukkitTask -> {
            new BankOperation(
                    player.getUniqueId().toString(),
                    player.getUniqueId().toString(),
                    amount,
                    BankOperationType.WITHDRAW
            ).flush();
        });
    }

    private static void seeSalaryStart(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;

        boolean isActive = NinkaiEco.getPlugin(NinkaiEco.class).isSalaryStart();

        Component boolComponent;
        if (isActive) {
            boolComponent = mm.deserialize("<color:#40c242>activé</color:#40c242>.");
        } else {
            boolComponent = mm.deserialize("<color:#c24040>désactivé</color:#c24040>.");
        }

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize("<color:#bfbfbf>Le système automatique de virement des salaires est "))
                        .append(boolComponent)
        );
    }

    private static void setSalaryStart(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;

        boolean bool = args.getByClass("bool", Boolean.class);
        NinkaiEco.getPlugin(NinkaiEco.class).setSalaryStart(bool);

        Component boolComponent;
        if (bool) {
            boolComponent = mm.deserialize("<color:#40c242>activé</color:#40c242>.");
        } else {
            boolComponent = mm.deserialize("<color:#c24040>désactivé</color:#c24040>.");
        }

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize("<color:#bfbfbf>Le système automatique de virement des salaires est désormais "))
                        .append(boolComponent)
        );
    }

    private static void forceSalary(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;

        Player target = args.getByClass("target", Player.class);
        String targetUUID = target.getUniqueId().toString();
        PlayerEco targetEco = NinkaiEco.playerEcoMap.get(targetUUID);
        int amount = targetEco.getMonthlySalary();

        targetEco.addBankMoney(amount);

        NinkaiEco.playerEcoMap.remove(targetUUID);
        NinkaiEco.playerEcoMap.put(targetUUID, targetEco);

        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize("<color:#bfbfbf>Vous avez forcé l'arrivée du salaire de "))
                        .append(target.displayName())
                        .append(Component.text("."))
        );

        adventure.player(target).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize(String.format(
                                "<color:#bfbfbf>Votre salaire vous a été donné en avance ! %,d ryos ont été ajoutés à votre compte.",
                                amount
                        )))
        );
    }

    private static void forceSalaryAll(CommandSender sender, CommandArguments args) {
        NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, bukkitTask -> {
            //flush all PlayerEco in the HashMap
            NinkaiEco.playerEcoMap.forEach((key, playerEco) -> {
                playerEco.addBankMoney(playerEco.getMonthlySalary());
                playerEco.flush();
            });
            String[] toSkip = NinkaiEco.playerEcoMap.keySet().toArray(new String[0]);
            try {
                PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                        """
                                SELECT DISTINCT
                                    rang,
                                    uuid
                                FROM PlayerInfo
                                WHERE uuid NOT IN ?
                                """
                );
                pst.setArray(1, Main.dbManager.getConnection().createArrayOf("int", toSkip));
                ResultSet res = pst.executeQuery();
                while (res.next()) {
                    String playerUUID = res.getString("uuid");
                    RPRank rank = RPRank.getById(res.getInt("rang"));
                    PlayerEco playerEco = PlayerEco.get(playerUUID, rank);
                    playerEco.addBankMoney(playerEco.getMonthlySalary());
                    playerEco.flush();
                }
                plugin.getServer().getScheduler().runTask(plugin, bukkitTask1 -> {
                    if (!(sender instanceof Player player)) return;
                    adventure.player(player).sendMessage(
                            PluginComponentProvider.getPluginHeader()
                                    .append(mm.deserialize("<color:#bfbfbf>Vous avez forcé l'arrivée des salaires de tous les joueurs."))
                    );
                });
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, e.getMessage());
            }
        });
    }

    private static List<PlayerCash> getPlayerCash(Player player) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .filter(itemStack ->
                        itemStack.getType() == Material.GOLD_NUGGET &&
                                NBT.get(itemStack, nbt -> nbt.getString("ninkai") != null)
                )
                .map(itemStack -> new PlayerCash(
                        itemStack,
                        Cash.getByTextureName(NBT.get(itemStack, nbt -> (String) nbt.getString("ninkai")))))
                .filter(playerCash -> playerCash.getCash() != null)
                .toList();
    }

    private static int getCashValue(Player player) {
        return EcoCommand.getPlayerCash(player).stream()
                .map(PlayerCash::getValue)
                .reduce(0, Integer::sum);

    }

    private static List<PlayerCash> addPlayerCashNBT(List<PlayerCash> playerCashList) {
        return playerCashList.stream()
                .map(playerCash -> {
                    NBT.modify(playerCash.getItemStack(), nbt -> {
                        nbt.setString("ninkai", playerCash.getCash().textureName);
                        nbt.modifyMeta(((readableNBT, itemMeta) -> {
                            itemMeta.displayName(mm.deserialize(playerCash.getCash().itemName));
                            List<Component> lore = new ArrayList<>();
                            lore.add(mm.deserialize(playerCash.getCash().itemLore));
                            itemMeta.lore(lore);
                        }));
                    });
                    return playerCash;
                })
                .toList();

    }

    private static List<PlayerCash> subsetAmount(Player player, int amount) {
        List<Cash> reversedCash = Arrays.stream(Cash.values())
                .sorted(Comparator.comparingInt(Cash::getValue).reversed())
                .toList();

        List<PlayerCash> res = new ArrayList<>();

        for (Cash c : reversedCash) {
            if (amount/c.value < 1) continue;

            //Get the number of items of a given value that can be given
            int nbItems = (int) Math.floor((double) amount /c.value);

            if (nbItems > 64) {
                int stacks = (int) Math.floor((double) nbItems/64);
                for (int i=0;i <= stacks-1; i++) {
                    ItemStack itemStack = new ItemStack(Material.GOLD_NUGGET);
                    itemStack.setAmount(64);
                    res.add(new PlayerCash(itemStack, c));
                }
                if (nbItems % 64 > 0) {
                    ItemStack itemStack = new ItemStack(Material.GOLD_NUGGET);
                    itemStack.setAmount(nbItems % 64);
                    res.add(new PlayerCash(itemStack, c));
                }
            } else {
                ItemStack itemStack = new ItemStack(Material.GOLD_NUGGET);
                itemStack.setAmount(nbItems);
                res.add(new PlayerCash(itemStack, c));
            }

            amount -= nbItems * c.getValue();
            if (amount == 0) break;
        }

        return res;
    }
}
