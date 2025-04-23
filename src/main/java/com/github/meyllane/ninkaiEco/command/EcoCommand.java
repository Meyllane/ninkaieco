package com.github.meyllane.ninkaiEco.command;

import com.github.meyllane.ninkaiEco.NinkaiEco;

import com.github.meyllane.ninkaiEco.dataclass.*;
import com.github.meyllane.ninkaiEco.enums.*;
import com.github.meyllane.ninkaiEco.utils.PluginComponentProvider;
import com.lishid.openinv.OpenInv;
import de.tr7zw.nbtapi.NBT;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import me.Seisan.plugin.Main;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
                        .then(new OfflinePlayerArgument("target")
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
                                .then(new OfflinePlayerArgument("target")
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

    private static void seeBalance(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        Player target;
        OfflinePlayer offTarget = args.getByClassOrDefault("target", OfflinePlayer.class, player);
        if (offTarget.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            target = player;
        } else {
            if (offTarget.isConnected()) {
                target = offTarget.getPlayer();
            } else {
                target = OpenInv.getPlugin(OpenInv.class).loadPlayer(offTarget);
            }

        }

        if (target == null) throw CommandAPIBukkit.failWithAdventureComponent(
                PluginComponentProvider.getErrorComponent(ErrorMessage.NONE_EXISTING_OR_NEVER_SEEN_PLAYER.message)
        );

        PlayerEco eco = NinkaiEco.playerEcoMap.get(target.getUniqueId().toString());

        Component toSend;
        //Format the message based on if there is a target or not
        if (target == player) {
            String message = String.format("<color:#bfbfbf>Vous avez %,d ryos sur votre compte et %,d ryos en cash.",
                    eco.getBankMoney(), EcoCommand.getCashValue(target));
            toSend = mm.deserialize(message);
        } else {
            Component targetName = target.displayName();
            String message = String.format(" <color:#bfbfbf>a %,d ryos sur son compte et %,d ryos en cash.",
                    eco.getBankMoney(), EcoCommand.getCashValue(target));
            toSend = targetName.append(mm.deserialize(message));
        }

        //Send the message to the sender after adding the PluginHeader
        adventure.player(player).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(toSend)
        );
    }

    private static void addBalance(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        OfflinePlayer target = args.getByClass("target", OfflinePlayer.class);

        if (target == null) throw CommandAPIBukkit.failWithAdventureComponent(
                PluginComponentProvider.getErrorComponent(ErrorMessage.NONE_EXISTING_OR_NEVER_SEEN_PLAYER.message)
        );

        boolean isConnected = target.isConnected();

        int amount = Math.abs(args.getByClassOrDefault("amount", Integer.class, 0));

        scheduler.runTaskAsynchronously(plugin, bukkitTask -> {
            Component targetComponent;
            if (isConnected) {
                NinkaiEco.playerEcoMap.get(target.getUniqueId().toString()).addBankMoney(amount);
                targetComponent = target.getPlayer().displayName();
            } else {
                PlayerEco targetEco = PlayerEco.get(target.getUniqueId().toString());
                targetEco.addBankMoney(amount);
                scheduler.runTaskAsynchronously(plugin, targetEco::flush);
                targetComponent = Component.text(target.getName());
            }

            new BankOperation(
                    player.getUniqueId().toString(),
                    target.getUniqueId().toString(),
                    amount,
                    BankOperationType.ADD
            ).flush();

            scheduler.runTask(plugin, bukkitTask1 -> {
                Component playerMessage = mm.deserialize(
                                String.format("<color:#bfbfbf>Vous avez ajouté %,d ryos sur le compte de ", amount)
                        )
                        .append(targetComponent)
                        .append(Component.text("."));

                adventure.player(player).sendMessage(PluginComponentProvider.getPluginHeader().append(playerMessage));

                if (isConnected) {
                    Component targetMessage = mm.deserialize(
                            String.format("<color:#bfbfbf>%,d ryos ont été ajoutés à votre compte.", amount)
                    );
                    adventure.player(target.getPlayer()).sendMessage(PluginComponentProvider.getPluginHeader().append(targetMessage));
                }
            });
        });
    }

    private static void removeBalance(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        OfflinePlayer target = args.getByClass("target", OfflinePlayer.class);
        int amount = Math.abs(args.getByClassOrDefault("amount", Integer.class, 0));

        if (target == null) throw CommandAPIBukkit.failWithAdventureComponent(
                PluginComponentProvider.getErrorComponent(ErrorMessage.NONE_EXISTING_OR_NEVER_SEEN_PLAYER.message)
        );

        boolean isConnected = target.isConnected();
        scheduler.runTaskAsynchronously(plugin, bukkitTask -> {
            Component targetComponent;
            if (isConnected) {
                NinkaiEco.playerEcoMap.get(target.getUniqueId().toString()).removeBankMoney(amount);
                targetComponent = target.getPlayer().displayName();
            } else {
                PlayerEco targetEco = PlayerEco.get(target.getUniqueId().toString());
                targetEco.removeBankMoney(amount);
                scheduler.runTaskAsynchronously(plugin, targetEco::flush);
                targetComponent = Component.text(target.getName());
            }
            new BankOperation(
                    player.getUniqueId().toString(),
                    target.getUniqueId().toString(),
                    amount,
                    BankOperationType.REMOVE
            ).flush();

            scheduler.runTask(plugin, bukkitTask1 -> {
                Component playerMessage = mm.deserialize(
                                String.format("<color:#bfbfbf>Vous avez retiré %,d ryos sur le compte de ", amount)
                        )
                        .append(targetComponent)
                        .append(Component.text("."));
                adventure.player(player).sendMessage(PluginComponentProvider.getPluginHeader().append(playerMessage));


                if (isConnected) {
                    Component targetMessage = mm.deserialize(
                            String.format("<color:#bfbfbf>%,d ryos ont été retirés à votre compte.", amount)
                    );
                    adventure.player(target.getPlayer()).sendMessage(PluginComponentProvider.getPluginHeader().append(targetMessage));
                }
            });
        });
    }

    private static void setBalance(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;

        OfflinePlayer target = args.getByClass("target", OfflinePlayer.class);
        int amount = args.getByClassOrDefault("amount", Integer.class, 0);

        if (target == null) throw CommandAPIBukkit.failWithAdventureComponent(
                PluginComponentProvider.getErrorComponent(ErrorMessage.NONE_EXISTING_OR_NEVER_SEEN_PLAYER.message)
        );

        boolean isConnected = target.isConnected();

        scheduler.runTaskAsynchronously(plugin, bukkitTask -> {
            Component targetComponent;
            if (isConnected) {
                NinkaiEco.playerEcoMap.get(target.getUniqueId().toString()).setBankMoney(amount);
                targetComponent = target.getPlayer().displayName();
            } else {
                PlayerEco targetEco = PlayerEco.get(target.getUniqueId().toString());
                targetEco.setBankMoney(amount);
                scheduler.runTaskAsynchronously(plugin, targetEco::flush);
                targetComponent = Component.text(target.getName());
            }
            new BankOperation(
                    player.getUniqueId().toString(),
                    target.getUniqueId().toString(),
                    amount,
                    BankOperationType.SET
            ).flush();

            scheduler.runTask(plugin, bukkitTask1 -> {
                Component playerMessage = mm.deserialize("<color:#bfbfbf>Vous avez fixé le compte de ")
                        .append(targetComponent)
                        .append(Component.text(
                                String.format(" à %,d ryos.", amount)
                        ));
                adventure.player(player).sendMessage(PluginComponentProvider.getPluginHeader().append(playerMessage));

                if (isConnected) {
                    Component targetMessage = mm.deserialize(
                            String.format("<color:#bfbfbf>Votre compte a été fixé à %,d ryos.", amount)
                    );
                    adventure.player(target.getPlayer()).sendMessage(PluginComponentProvider.getPluginHeader().append(targetMessage));
                }
            });
        });
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

    private static void handleForceSalary(Player player, OfflinePlayer target, boolean sendPlayerNotification) {
        boolean isConnected = target.isConnected();
        scheduler.runTaskAsynchronously(plugin, bukkitTask -> {
            PlayerEco targetEco;
            if (isConnected) {
                targetEco = NinkaiEco.playerEcoMap.get(target.getUniqueId().toString());
            } else {
                targetEco = PlayerEco.get(target.getUniqueId().toString());
            }
            scheduler.runTask(plugin, bukkitTask1 -> {
                if (sendPlayerNotification) {
                    Component playerMessage = PluginComponentProvider.getPluginHeader()
                            .append(mm.deserialize("<color:#bfbfbf>Vous avez donné à "))
                            .append(Component.text(target.getName()))
                            .append(mm.deserialize(" <color:#bfbfbf>son salaire en avance !"));

                    adventure.player(player).sendMessage(playerMessage);
                }

                int salary = targetEco.getMonthlySalary();
                targetEco.addBankMoney(salary);
                String message = String.format("<color:#bfbfbf>Votre salaire est arrivé en avance ! %,d ryos ont été versés sur votre compte.", salary);
                if (isConnected) {
                    Player t = plugin.getServer().getPlayer(target.getUniqueId());
                    Component targetMessage = PluginComponentProvider.getPluginHeader()
                            .append(mm.deserialize(message));
                    adventure.player(t).sendMessage(targetMessage);

                } else {
                    Notification notif = new Notification(targetEco.getMonthlySalary(), targetEco.getPlayerUUID(), message);
                    scheduler.runTaskAsynchronously(plugin, notif::flush);
                    scheduler.runTaskAsynchronously(plugin, targetEco::flush);
                }
            });
        });
    }

    //Salary commands are quite ugly, but they will be reworked eventually with the rework of the main plugin
    private static void forceSalary(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player player)) return;
        OfflinePlayer offTarget = args.getByClass("target", OfflinePlayer.class);

        if (offTarget == null) throw CommandAPIBukkit.failWithAdventureComponent(
                PluginComponentProvider.getErrorComponent(ErrorMessage.NONE_EXISTING_OR_NEVER_SEEN_PLAYER.message)
        );

        EcoCommand.handleForceSalary(player, offTarget, true);
    }

    private static void forceSalaryAll(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player player)) return;
        try {
            PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                    "SELECT playerUUID FROM Money"
            );
            ResultSet res = pst.executeQuery();
            while (res.next()) {
                OfflinePlayer target = plugin.getServer().getOfflinePlayer(UUID.fromString(res.getString("playerUUID")));
                EcoCommand.handleForceSalary(player, target, false);
            }
            pst.close();

            adventure.player(player).sendMessage(
                    PluginComponentProvider.getPluginHeader()
                            .append(mm.deserialize("<color:#bfbfbf>Vous avez forcé les salaires de toustes les joueur.euses !"))
            );
        } catch (SQLException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Error while forcing all salaries: " + e.getMessage()
            );
        }
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
