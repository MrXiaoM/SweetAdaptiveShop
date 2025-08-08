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
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerOrder;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.func.config.CustomGuiManager;
import top.mrxiaom.sweet.adaptiveshop.func.config.GroupManager;
import top.mrxiaom.sweet.adaptiveshop.func.config.OrderManager;
import top.mrxiaom.sweet.adaptiveshop.func.config.TemplateManager;
import top.mrxiaom.sweet.adaptiveshop.func.config.customgui.CustomGui;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Group;
import top.mrxiaom.sweet.adaptiveshop.func.entry.ItemTemplate;
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

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    public CommandMain(SweetAdaptiveShop plugin) {
        super(plugin);
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
                    return Messages.group__not_found.tm(sender, name);
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
                    return Messages.custom_gui__not_found.tm(sender, name);
                } else {
                    model = CustomGuiManager.inst().get(name);
                    if (model == null) {
                        return Messages.custom_gui__not_found.tm(sender, name);
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
                        return Messages.player__not_online.tm(sender, args[playerCheckIndex]);
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
        if (args.length > 5 && "give".equalsIgnoreCase(args[0]) && sender.isOp()) {
            Player player = Util.getOnlinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.player__not_online.tm(sender);
            }
            String templateId = args[2];
            TemplateManager manager = TemplateManager.inst();
            ItemTemplate template = manager.getTemplate(templateId);
            if (template == null) {
                return Messages.template__not_found.tm(sender, templateId);
            }
            int amount = Util.parseInt(args[3]).orElse(0);
            if (amount < 1) {
                return Messages.int__invalid.tm(sender);
            }
            if (amount > template.material.getMaxStackSize()) {
                return Messages.int__much.tm(sender);
            }
            String nbtKey;
            if (args[4].equalsIgnoreCase("buy")) {
                nbtKey = GuiBuyShop.REFRESH_ITEM;
            } else if (args[4].equalsIgnoreCase("sell")) {
                nbtKey = GuiSellShop.REFRESH_ITEM;
            } else if (args[4].equalsIgnoreCase("order")) {
                nbtKey = GuiOrders.REFRESH_ITEM;
            } else {
                return Messages.give__type_not_found.tm(sender, args[4]);
            }
            String formatted;
            long outdate;
            if (args[5].equals("0") || args[5].equals("infinite")) {
                formatted = manager.format(null);
                outdate = 0L;
            } else {
                LocalDateTime time = TimeUtils.override(LocalDateTime.now(), args, 5);
                formatted = manager.format(time);
                outdate = time.toEpochSecond(ZoneOffset.UTC);
            }
            List<ItemStack> items = new ArrayList<>();
            int times = template.unique ? amount : 1;
            int count = template.unique ? 1 : amount;
            for (int i = 0; i < times; i++) {
                items.add(template.generateIcon(player, count, oldLore -> {
                    List<String> lore = new ArrayList<>();
                    for (String s : oldLore) {
                        lore.add(s.replace("%datetime%", formatted));
                    }
                    return lore;
                }, nbt -> nbt.setLong(nbtKey, outdate)));
            }
            Collection<ItemStack> last = player.getInventory().addItem(items.toArray(new ItemStack[0])).values();
            Messages.give__player.tm(player);
            if (!last.isEmpty()) {
                Messages.give__full.tm(player);
                for (ItemStack item : last) {
                    player.getWorld().dropItem(player.getLocation(), item);
                }
            }
            return Messages.give__success.tm(sender, player.getName(), amount, template.display);
        }
        if (args.length >= 3 && "refresh".equalsIgnoreCase(args[0]) && sender.isOp()) {
            Player player = Util.getOnlinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.player__not_online.tm(sender, args[1]);
            }
            switch (args[2]) {
                case "buy": {
                    if (args.length < 4) {
                        return Messages.group__not_input.tm(sender);
                    }
                    Group group = GroupManager.inst().get(args[3]);
                    if (group == null) {
                        return Messages.group__not_found.tm(sender, args[3]);
                    }
                    group.refreshBuyShop(player);
                    return Messages.refresh__buy__success_other.tm(sender, player.getName(), group.display);
                }
                case "sell": {
                    if (args.length < 4) {
                        return Messages.group__not_input.tm(sender);
                    }
                    Group group = GroupManager.inst().get(args[3]);
                    if (group == null) {
                        return Messages.group__not_found.tm(sender, args[3]);
                    }
                    group.refreshSellShop(player);
                    return Messages.refresh__sell__success_other.tm(sender, player.getName(), group.display);
                }
                case "order": {
                    OrderManager.inst().refresh(player);
                    return Messages.refresh__order__success_other.tm(sender, player.getName());
                }
            }
            return Messages.refresh__type_invalid.tm(sender, args[2]);
        }
        if (args.length == 3 && "test".equalsIgnoreCase(args[0]) && sender.isOp()) {
            if ("order".equalsIgnoreCase(args[1])) {
                OfflinePlayer p = Util.getOfflinePlayer(args[2]).orElse(null);
                if (p == null) {
                    return Messages.player__not_found.tm(sender, args[2]);
                }
                String key = plugin.getDBKey(p);
                List<PlayerOrder> orders = plugin.getOrderDatabase().getPlayerOrders(key);
                t(sender, "玩家 " + p.getName() + " 的订单列表: (" + orders.size() + ")");
                for (PlayerOrder order : orders) {
                    t(sender, "  - 订单 " + order.getOrder() + " 完成次数: " + order.getDoneCount() + " 到期时间: " + order.getOutdate());
                }
                return t(sender, "");
            }
            if ("buy".equalsIgnoreCase(args[1])) {
                OfflinePlayer p = Util.getOfflinePlayer(args[2]).orElse(null);
                if (p == null) {
                    return Messages.player__not_found.tm(sender, args[2]);
                }
                String key = plugin.getDBKey(p);
                List<PlayerItem> items = plugin.getBuyShopDatabase().getPlayerItems(key);
                t(sender, "玩家 " + p.getName() + " 的收购物品列表 &7(" + items.size() + ")");
                for (PlayerItem item : items) {
                    t(sender, "  - 商品 " + item.getItem() + " 到期时间: " + item.getOutdate());
                }
                return t(sender, "");
            }
            if ("sell".equalsIgnoreCase(args[1])) {
                OfflinePlayer p = Util.getOfflinePlayer(args[2]).orElse(null);
                if (p == null) {
                    return Messages.player__not_found.tm(sender, args[2]);
                }
                String key = plugin.getDBKey(p);
                List<PlayerItem> items = plugin.getSellShopDatabase().getPlayerItems(key);
                t(sender, "玩家 " + p.getName() + " 的出售物品列表 &7(" + items.size() + ")");
                for (PlayerItem item : items) {
                    t(sender, "  - 商品 " + item.getItem() + " 到期时间: " + item.getOutdate());
                }
                return t(sender, "");
            }
            return true;
        }
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

    private static final List<String> listArg0 = Lists.newArrayList(
            "open");
    private static final List<String> listArgOpen = Lists.newArrayList(
            "buy", "sell", "order", "custom");
    private static final List<String> listArgGive = Lists.newArrayList(
            "buy", "sell", "order");
    private static final List<String> listArgRefresh = Lists.newArrayList(
            "buy", "sell", "order");
    private static final List<String> listArgTest = Lists.newArrayList(
            "buy", "sell", "order");
    private static final List<String> listOpArg0 = Lists.newArrayList(
            "open", "give", "refresh", "test", "reload");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return startsWith(sender.isOp() ? listOpArg0 : listArg0, args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("open")) {
                return startsWith(listArgOpen, args[1]);
            }
            if (args[0].equalsIgnoreCase("test") && sender.isOp()) {
                return startsWith(listArgTest, args[1]);
            }
            if (args[0].equalsIgnoreCase("give") && sender.isOp()) {
                return null;
            }
            if (args[0].equalsIgnoreCase("refresh") && sender.isOp()) {
                return null;
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
            if (args[0].equalsIgnoreCase("refresh") && sender.isOp()) {
                return startsWith(listArgRefresh, args[2]);
            }
        }
        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("open") && listArgOpen.contains(args[1].toLowerCase())) {
                if (!args[1].equalsIgnoreCase("order") && sender.isOp()) {
                    return null;
                }
            }
            if (args[0].equalsIgnoreCase("refresh") && sender.isOp()) {
                if (args[2].equalsIgnoreCase("buy") || args[2].equalsIgnoreCase("sell")) {
                    return startsWith(GroupManager.inst().groups(sender), args[3]);
                }
            }
        }
        if (args.length == 5) {
            if (args[0].equalsIgnoreCase("give") && listArgGive.contains(args[2].toLowerCase()) && sender.isOp()) {
                return startsWith(TemplateManager.inst().itemTemplates(), args[4]);
            }
        }
        return Collections.emptyList();
    }

    public List<String> startsWith(Collection<String> list, String s) {
        return startsWith(null, list, s);
    }
    public List<String> startsWith(String[] addition, Collection<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        if (addition != null) stringList.addAll(0, Lists.newArrayList(addition));
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return stringList;
    }
}
