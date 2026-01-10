package top.mrxiaom.sweet.adaptiveshop.commands;

import com.google.common.collect.Lists;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.Messages;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.BuyShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.SellShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerOrder;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.func.config.*;
import top.mrxiaom.sweet.adaptiveshop.func.config.customgui.CustomGui;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Group;
import top.mrxiaom.sweet.adaptiveshop.func.entry.ItemTemplate;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.BuyShop;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.SellShop;
import top.mrxiaom.sweet.adaptiveshop.gui.GuiBuyShop;
import top.mrxiaom.sweet.adaptiveshop.gui.GuiOrders;
import top.mrxiaom.sweet.adaptiveshop.gui.GuiSellShop;
import top.mrxiaom.sweet.adaptiveshop.utils.TimeUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    private final CommandOperator commandOperator;
    public CommandMain(SweetAdaptiveShop plugin) {
        super(plugin);
        this.commandOperator = new CommandOperator(plugin);
        registerCommand("SweetAdaptiveShop".toLowerCase(), this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && "open".equalsIgnoreCase(args[0])) {
            String type = "buy";
            if (args.length >= 2) {
                type = args[1];
            }
            Player player = sender instanceof Player ? (Player) sender : null;
            int playerCheckIndex;
            Group group;
            CustomGui model;
            if (type.equals("buy") || type.equals("sell")) {
                boolean buy = type.equals("buy");
                String name = args.length >= 3 ? args[2] : "default";
                group = GroupManager.inst().get(name);
                if (group == null || (buy && !group.enableBuy) || (!buy && !group.enableSell)) {
                    return Messages.group__not_found.tmf(sender, name);
                }
                model = null;
                playerCheckIndex = 3;
            } else if (type.equals("custom")) {
                group = null;
                String name = args.length >= 3 ? args[2] : "";
                if (name.isEmpty()) {
                    return Messages.custom_gui__not_input.tm(sender);
                }
                if (!sender.hasPermission("sweet.adaptive.shop.custom")) {
                    return Messages.custom_gui__not_found.tmf(sender, name);
                } else {
                    model = CustomGuiManager.inst().get(name);
                    if (model == null) {
                        return Messages.custom_gui__not_found.tmf(sender, name);
                    }
                }
                playerCheckIndex = 3;
            } else {
                group = null;
                model = null;
                playerCheckIndex = 2;
            }
            if (args.length >= playerCheckIndex + 1) {
                if (sender.isOp()) {
                    player = Util.getOnlinePlayer(args[playerCheckIndex]).orElse(null);
                    if (player == null) {
                        return Messages.player__not_online.tmf(sender, args[playerCheckIndex]);
                    }
                } else {
                    return Messages.player__no_permission.tm(sender);
                }
            }
            if (player == null) {
                return Messages.player__only.tm(sender);
            }
            switch (type.toLowerCase()) {
                case "buy":
                    if (group == null) return true;
                    if (!player.hasPermission("sweet.adaptive.shop.group." + group.id)) {
                        return Messages.player__no_permission.tm(player);
                    }
                    GuiBuyShop.create(player, group).open();
                    return true;
                case "sell":
                    if (group == null) return true;
                    if (!player.hasPermission("sweet.adaptive.shop.group." + group.id)) {
                        return Messages.player__no_permission.tm(player);
                    }
                    GuiSellShop.create(player, group).open();
                    return true;
                case "order":
                    if (!player.hasPermission("sweet.adaptive.shop.order")) {
                        return Messages.player__no_permission.tm(player);
                    }
                    GuiOrders.create(player).open();
                    return true;
                case "custom":
                    if (model == null) return true;
                    if (!player.hasPermission("sweet.adaptive.shop.custom") || !model.hasPermission(player)) {
                        return Messages.player__no_permission.tm(player);
                    }
                    CustomGuiManager.inst().create(player, model).open();
                    return true;
            }
            return Messages.gui__not_found.tm(sender);
        }
        if (sender.isOp() && commandOperator.onCommand(sender, args)) return true;
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0]) && sender.isOp()) {
            plugin.reloadConfig();
            return Messages.reload__config.tm(sender);
        }
        if (args.length == 2 && "reload".equalsIgnoreCase(args[0]) && "database".equalsIgnoreCase(args[1]) && sender.isOp()) {
            plugin.options.database().reloadConfig();
            plugin.options.database().reconnect();
            return Messages.reload__database.tm(sender);
        }
        return (sender.isOp() ? Messages.help_op : Messages.help).tm(sender);
    }

    private static final List<String> listArgOpen = Lists.newArrayList(
            "buy", "sell", "order", "custom");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (sender.isOp()) {
            AtomicBoolean noChange = new AtomicBoolean(false);
            List<String> list = commandOperator.onTabComplete(sender, args, noChange);
            if (!noChange.get()) {
                return list;
            }
        }
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("open");
            if (sender.isOp()) {
                list.addAll(commandOperator.listArg0);
                list.add("reload");
            }
            return startsWith(list, args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("open")) {
                return startsWith(listArgOpen, args[1]);
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("open") && listArgOpen.contains(args[1].toLowerCase())) {
                if (args[1].equalsIgnoreCase("buy") || args[1].equalsIgnoreCase("sell")) {
                    return startsWith(GroupManager.inst().groups(sender), args[2]);
                }
                if (args[1].equalsIgnoreCase("order") && sender.isOp()) {
                    return null;
                }
                if (args[1].equalsIgnoreCase("custom")) {
                    if (!sender.hasPermission("sweet.adaptive.shop.custom")) return Collections.emptyList();
                    return startsWith(CustomGuiManager.inst().keys(sender), args[2]);
                }
            }
        }
        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("open") && listArgOpen.contains(args[1].toLowerCase())) {
                if (!args[1].equalsIgnoreCase("order") && sender.isOp()) {
                    return null;
                }
            }
        }
        return Collections.emptyList();
    }

    protected static List<String> startsWith(Collection<String> list, String s) {
        return startsWith(null, list, s);
    }
    protected static List<String> startsWith(String[] addition, Collection<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        if (addition != null) stringList.addAll(0, Lists.newArrayList(addition));
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return stringList;
    }
}
