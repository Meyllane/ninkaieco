package com.github.meyllane.ninkaiEco.command;

import com.github.meyllane.ninkaiEco.NinkaiEco;
import com.github.meyllane.ninkaiEco.dataclass.BankOperation;
import com.github.meyllane.ninkaiEco.dataclass.PlayerEco;
import com.github.meyllane.ninkaiEco.dataclass.SellOrder;
import com.github.meyllane.ninkaiEco.enums.BankOperationType;
import com.github.meyllane.ninkaiEco.enums.ErrorMessage;
import com.github.meyllane.ninkaiEco.enums.Institution;
import com.github.meyllane.ninkaiEco.enums.SellOrderStatus;
import com.github.meyllane.ninkaiEco.error.NinkaiEcoError;
import com.github.meyllane.ninkaiEco.utils.ErrorHandler;
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

import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class ArtisanCommand {
    private static final NinkaiEco plugin = NinkaiEco.getPlugin(NinkaiEco.class);
    private static final BukkitAudiences adventure = plugin.adventure();
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final BukkitScheduler scheduler = plugin.getServer().getScheduler();

    /**
     * Registers the /artisan command
     */
    public static void register() {
        new CommandTree("artisan")
                .then(
                        new LiteralArgument("sell")
                                .withPermission("ninkaieco.artisan.sell")
                                .withRequirement(ArtisanCommand::isMerchant)
                                .thenNested(
                                        new OfflinePlayerArgument("buyer"),
                                        new IntegerArgument("price"),
                                        new IntegerArgument("margin", 0, 20),
                                        new GreedyStringArgument("detail")
                                                .executes(ArtisanCommand::createSellOrder)
                                )
                )
                .thenNested(
                        new LiteralArgument("accept"),
                        new IntegerArgument("orderID")
                        .withPermission("ninkaieco.artisan.accept")
                                .executes(ArtisanCommand::acceptSellOrder)
                )
                .thenNested(
                        new LiteralArgument("reject"),
                        new IntegerArgument("orderID")
                                .withPermission("ninkaieco.artisan.reject")
                                .executes(ArtisanCommand::rejectSellOrder)
                )
                .then(
                        new LiteralArgument("cancel")
                                .withRequirement(ArtisanCommand::isMerchant)
                                .withPermission("ninkaieco.artisan.cancel")
                                .then(new IntegerArgument("orderID")
                                        .executes(ArtisanCommand::cancelSellOrder)
                                )
                )
                .register();
    }

    /** Checks if the player is a Merchant (in the Artisan Institution)
     *
     * @param sender A CommandSender. The command will only run if it's a subclass of <code>Player</code>
     * @return true if the player is a Merchant, otherwise false
     */
    public static boolean isMerchant(CommandSender sender) {
        if (!(sender instanceof Player player)) return false;

        PlayerEco playerEco = NinkaiEco.playerEcoMap.get(player.getUniqueId().toString());
        if (playerEco == null) return false;
        return playerEco.getPlayerInstitution().getInstitution() == Institution.ARTI;
    }

    /**
     * The function called by <code>/artisan sell buyer price margin detail</code>.
     *
     * @param sender A <code>CommandSender</code>. The command will only run if it's a subclass of <code>Player</code>
     * @param args <code>CommandArguments</code> that comes with the <code>.executes</code> of CommandAPI
     * @throws WrapperCommandSyntaxException CommandAPI will handle the propagation of the error.
     */
    public static void createSellOrder(CommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
        if (!(sender instanceof Player seller)) return;

        OfflinePlayer buyer = args.getByClass("buyer", OfflinePlayer.class);
        String detail = args.getByClass("detail", String.class);
        int price = args.getByClass("price", Integer.class);
        int margin = args.getByClass("margin", Integer.class);

        if (buyer == null) throw CommandAPIBukkit.failWithAdventureComponent(
                PluginComponentProvider.getErrorComponent(ErrorMessage.NONE_EXISTING_OR_NEVER_SEEN_PLAYER.message)
        );

        if (buyer == seller) {
            throw CommandAPIBukkit.failWithAdventureComponent(PluginComponentProvider.getErrorComponent(ErrorMessage.CANT_SELL_TO_SELF.message));
        }

        SellOrder sellOrder = new SellOrder(
                -1,
                seller.getUniqueId().toString(),
                buyer.getUniqueId().toString(),
                detail,
                price,
                margin,
                (int) (Math.ceil((double) margin/100*price)),
                SellOrderStatus.PENDING
        );

        boolean isConnected = buyer.isConnected();

        scheduler.runTaskAsynchronously(plugin, bukkitTask -> {
            sellOrder.flush();

            Component infoPart = sellOrder.getInfoComponent();
            Component accept = mm.deserialize(String.format(
                    "<color:#bfbfbf>[<color:#2bb427><click:run_command:/artisan accept %d>Accepter</click></color:#2bb427>]",
                    sellOrder.getID()
            ));
            Component reject = mm.deserialize(String.format(
                    "<color:#bfbfbf>[<color:#b42727><click:run_command:/artisan reject %d>Refuser</click></color:#b42727>]",
                    sellOrder.getID()
            ));
            Component optionPart = accept.append(Component.text(" ")).append(reject);

            scheduler.runTask(plugin, bukkitTask1 -> {
                Component targetComponent;
                if (isConnected) {
                    targetComponent = buyer.getPlayer().displayName();
                } else {
                    targetComponent = Component.text(buyer.getName());
                }
                adventure.player(seller).sendMessage(
                        PluginComponentProvider.getPluginHeader()
                                .append(mm.deserialize("<color:#bfbfbf>Vous avez proposé à "))
                                .append(targetComponent)
                                .append(mm.deserialize("<color:#bfbfbf> la vente suivante :"))
                                .append(infoPart)
                );
                if (isConnected) {
                    adventure.player(buyer.getPlayer()).sendMessage(
                            PluginComponentProvider.getPluginHeader()
                                    .append(seller.displayName())
                                    .append(mm.deserialize("<color:#bfbfbf> vous a proposé la vente suivante :"))
                                    .append(infoPart)
                                    .append(optionPart)
                    );
                }
            });
        });
    }

    /**
     * The function called by the command <code>/artisan accept orderID</code> or by clicking on the [Accepter] MiniMessage element
     * of a SellOrder proposal.
     * This function handles the first checks, and then calls <code>.handleAcceptSellOrder</code> to check the rest of
     * the conditions once the SellOrder is retrieved from the database
     *
     * @param sender A <code>CommandSender</code>. The command will only run if it's a subclass of <code>Player</code>
     * @param args <code>CommandArguments</code> that comes with the <code>.executes</code> of CommandAPI
     */
    public static void acceptSellOrder(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player buyer)) return;

        int id = args.getByClass("orderID", Integer.class);

        scheduler.runTaskAsynchronously(plugin, bukkitTask -> {
            //Run the database query in async
            try {
                SellOrder sellOrder = SellOrder.getSellOrder(id);
                //And then go back to sync for the processing
                scheduler.runTask(plugin, bukkitTask1 -> ErrorHandler.wrap(ArtisanCommand::handleAcceptSellOrder, sellOrder, buyer));
            } catch (SQLException e) {
                scheduler.runTask(plugin, bukkitTask1 -> {
                    adventure.player(buyer).sendMessage(
                            PluginComponentProvider.getErrorComponent(
                                    ErrorMessage.UNKNOWN_ERROR.message
                            )
                    );
                    e.printStackTrace();
                });
            }
        });

    }

    /**
     * Handles the verification of <code>acceptSellOrder</code> once the <code>SellOrder</code> is retrieved from the database.
     * This function will throw a bunch of <code>NinkaiEcoError</code> that are all handled by the <code>ErrorHandler.wrap()</code>
     * that surrounds the function.
     * @param sellOrder The <code>SellOrder</code> retrieved from the database.
     * @param buyer - the player that called the command (should be the buyer)
     */
    private static void handleAcceptSellOrder(SellOrder sellOrder, Player buyer) {
        if(sellOrder == null) throw new NinkaiEcoError(ErrorMessage.UNKNOWN_SELL_ORDER_ID.message);

        if (!buyer.getUniqueId().toString().equals(sellOrder.getBuyerUUID())) throw new NinkaiEcoError(ErrorMessage.NOT_RECIPIENT_OF_SELL_ORDER.message);

        if (sellOrder.getStatus() != SellOrderStatus.PENDING) throw new NinkaiEcoError(ErrorMessage.ALREADY_PROCESS_SELL_ORDER.message);

        PlayerEco buyerEco = NinkaiEco.playerEcoMap.get(buyer.getUniqueId().toString());

        if (buyerEco.getBankMoney() < sellOrder.getPrice()) throw new NinkaiEcoError(ErrorMessage.NOT_ENOUGHT_MONEY_FOR_SELL_ORDER.message);

        PlayerEco sellerEco = NinkaiEco.playerEcoMap.get(sellOrder.getSellerUUID());
        buyerEco.removeBankMoney(sellOrder.getPrice());
        sellerEco.addBankMoney(sellOrder.getMarginValue());
        scheduler.runTaskAsynchronously(plugin, bukkitTask -> {
            Date currDate = new Date();
            BankOperation soPayement = new BankOperation(
                    buyerEco.getPlayerUUID(),
                    sellerEco.getPlayerUUID(),
                    sellOrder.getPrice(),
                    BankOperationType.SO_PAYMENT,
                    currDate
            );
            soPayement.flush();

            BankOperation soReceived = new BankOperation(
                    buyerEco.getPlayerUUID(),
                    sellerEco.getPlayerUUID(),
                    sellOrder.getMarginValue(),
                    BankOperationType.SO_RECEIVED,
                    currDate
            );
            soReceived.flush();
        });

        adventure.player(buyer).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize(String.format(
                                "<color:#bfbfbf>Vous avez accepté la commande ! %,d ryos ont été prélevé de votre compte.",
                                sellOrder.getPrice()
                        )))
        );

        sellOrder.setStatus(SellOrderStatus.ACCEPTED);
        sellOrder.setUpdatedAt(new Date());
        scheduler.runTaskAsynchronously(plugin, bukkitTask -> sellOrder.flush());

        Player seller = plugin.getServer().getPlayer(UUID.fromString(sellOrder.getSellerUUID()));
        if (seller == null) return;
        adventure.player(seller).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(buyer.displayName())
                        .append(mm.deserialize("<color:#bfbfbf> a accepté la commande suivante :"))
                        .append(sellOrder.getInfoComponent())
                        .append(mm.deserialize(String.format(
                                "<color:#bfbfbf>%,d ryos ont été virés sur votre compte.",
                                sellOrder.getMarginValue()
                        )))
        );
    }

    /**
     * The function called by the command <code>/artisan reject orderID</code> or by clicking on the [Refuser] MiniMessage element
     * of a SellOrder proposal.
     * This function handles the first checks, and then calls <code>.handleRejectSellOrder</code> to check the rest of
     * the conditions once the <code>SellOrder</code> is retrieved from the database
     *
     * @param sender A <code>CommandSender</code>. The command will only run if it's a subclass of <code>Player</code>
     * @param args <code>CommandArguments</code> that comes with the <code>.executes</code> of CommandAPI
     */
    public static void rejectSellOrder(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player buyer)) return;

        int id = args.getByClass("orderID", Integer.class);

        scheduler.runTaskAsynchronously(plugin, bukkitTask -> {
            try {
                SellOrder sellOrder = SellOrder.getSellOrder(id);
                scheduler.runTask(plugin, bukkitTask1 -> ErrorHandler.wrap(ArtisanCommand::handleRejectSellOrder, sellOrder, buyer));
            } catch (SQLException e) {
                scheduler.runTask(plugin, bukkitTask1 -> {
                    adventure.player(buyer).sendMessage(
                            PluginComponentProvider.getErrorComponent(
                                    ErrorMessage.UNKNOWN_ERROR.message
                            )
                    );
                    e.printStackTrace();
                });
            }
        });
    }

    /**
     * Handles the verification of <code>rejectSellOrder</code> once the <code>SellOrder</code> is retrieved from the database.
     * This function will throw a bunch of <code>NinkaiEcoError</code> that are all handled by the <code>ErrorHandler.wrap()</code>
     * that surrounds the function.
     * @param sellOrder The <code>SellOrder</code> retrieved from the database.
     * @param buyer - the player that called the command (should be the buyer)
     */
    private static void handleRejectSellOrder(SellOrder sellOrder, Player buyer) {
        if (sellOrder == null) throw new NinkaiEcoError(ErrorMessage.UNKNOWN_SELL_ORDER_ID.message);

        if (!Objects.equals(sellOrder.getBuyerUUID(), buyer.getUniqueId().toString())) throw new NinkaiEcoError(ErrorMessage.NOT_RECIPIENT_OF_SELL_ORDER.message);

        if (sellOrder.getStatus() != SellOrderStatus.PENDING) throw new NinkaiEcoError(ErrorMessage.ALREADY_PROCESS_SELL_ORDER.message);

        sellOrder.setStatus(SellOrderStatus.REJECTED);
        sellOrder.setUpdatedAt(new Date());
        scheduler.runTaskAsynchronously(plugin, bukkitTask -> sellOrder.flush());

        adventure.player(buyer).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize("<color:#bfbfbf>Vous avez refusé la commande."))
        );

        Player seller = plugin.getServer().getPlayer(UUID.fromString(sellOrder.getSellerUUID()));
        if (seller == null) return;

        adventure.player(seller).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(buyer.displayName())
                        .append(mm.deserialize("<color:#bfbfbf> a refusé la commande suivante :"))
                        .append(sellOrder.getInfoComponent())
        );
    }

    /**
     * The function called by the command <code>/artisan cancel orderID</code>.
     * This function handles the first checks, and then calls <code>.handleCancelSellOrder</code> to check the rest of
     * the conditions once the <code>SellOrder</code> is retrieved from the database
     *
     * @param sender A <code>CommandSender</code>. The command will only run if it's a subclass of <code>Player</code>
     * @param args <code>CommandArguments</code> that comes with the <code>.executes</code> of CommandAPI
     */
    public static void cancelSellOrder(CommandSender sender, CommandArguments args) {
        if (!(sender instanceof Player seller)) return;

        int id = args.getByClass("orderID", Integer.class);

        scheduler.runTaskAsynchronously(plugin, bukkitTask -> {
            try {
                SellOrder sellOrder = SellOrder.getSellOrder(id);
                scheduler.runTask(plugin, bukkitTask1 -> ErrorHandler.wrap(ArtisanCommand::handleCancelSellOrder, sellOrder, seller));
            } catch (SQLException e) {
                scheduler.runTask(plugin, bukkitTask1 -> {
                    adventure.player(seller).sendMessage(
                            PluginComponentProvider.getErrorComponent(
                                    ErrorMessage.UNKNOWN_ERROR.message
                            )
                    );
                    e.printStackTrace();
                });
            }
        });
    }

    /**
     * Handles the verification of <code>cancelSellOrder</code> once the <code>SellOrder</code> is retrieved from the database.
     * This function will throw a bunch of <code>NinkaiEcoError</code> that are all handled by the <code>ErrorHandler.wrap()</code>
     * that surrounds the function.
     * @param sellOrder The <code>SellOrder</code> retrieved from the database.
     * @param seller - the player that called the command (should be the seller)
     */
    public static void handleCancelSellOrder(SellOrder sellOrder, Player seller) {
        if (sellOrder == null) throw new NinkaiEcoError(ErrorMessage.UNKNOWN_SELL_ORDER_ID.message);

        if (!sellOrder.getSellerUUID().equals(seller.getUniqueId().toString()))
            throw new NinkaiEcoError(ErrorMessage.NOT_EMITTER_OF_SELL_ORDER.message);

        if (sellOrder.getStatus() != SellOrderStatus.PENDING) throw new NinkaiEcoError(ErrorMessage.ALREADY_PROCESS_SELL_ORDER.message);

        sellOrder.setStatus(SellOrderStatus.CANCELED);
        sellOrder.setUpdatedAt(new Date());
        scheduler.runTaskAsynchronously(plugin, bukkitTask -> sellOrder.flush());

        adventure.player(seller).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(mm.deserialize("<color:#bfbfbf>Vous avez annulé la commande suivante :"))
                        .append(sellOrder.getInfoComponent())
        );

        Player buyer = plugin.getServer().getPlayer(UUID.fromString(sellOrder.getBuyerUUID()));
        if (buyer == null) return;

        adventure.player(buyer).sendMessage(
                PluginComponentProvider.getPluginHeader()
                        .append(seller.displayName())
                        .append(mm.deserialize("<color:#bfbfbf> a annulé la vente suivante :"))
                        .append(sellOrder.getInfoComponent())
        );
    }
}
