package top.mrxiaom.sweet.adaptiveshop.commands;

import com.google.common.collect.Lists;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.Messages;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.BuyShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.SellShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerOrder;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractPluginHolder;
import top.mrxiaom.sweet.adaptiveshop.func.config.*;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static top.mrxiaom.sweet.adaptiveshop.commands.CommandMain.startsWith;

/**
 * 管理员命令
 */
public class CommandOperator extends AbstractPluginHolder {
    public CommandOperator(SweetAdaptiveShop plugin) {
        super(plugin);
    }

    protected boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 5 && "give".equalsIgnoreCase(args[0])) {
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
            Messages.give__player.tm(player, amount, template.display);
            if (!last.isEmpty()) {
                Messages.give__full.tm(player);
                for (ItemStack item : last) {
                    player.getWorld().dropItem(player.getLocation(), item);
                }
            }
            return Messages.give__success.tm(sender, player.getName(), amount, template.display);
        }
        if (args.length >= 3 && "refresh".equalsIgnoreCase(args[0])) {
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
        if (args.length > 4 && "dynamic".equalsIgnoreCase(args[0])) {
            Player player;
            if (args.length > 5){
                player = Util.getOnlinePlayer(args[5]).orElse(null);
                if (player == null) {
                    return Messages.player__not_online.tm(sender);
                }
            } else {
                player = null;
            }
            if ("buy".equalsIgnoreCase(args[1])) {
                BuyShop item = BuyShopManager.inst().get(args[2]);
                if (item == null) {
                    return t(sender, "&e找不到收购商品&b " + args[2]);
                }
                String operation = args[3].toLowerCase();
                Double operateValue = Util.parseDouble(args[4]).orElse(null);
                if (operateValue == null) {
                    return Messages.int__invalid.tm(sender);
                }
                if (item.dynamicValuePerPlayer && player == null) {
                    return t(sender, "&e这个收购商品要求每个玩家的动态值独立，但你未输入玩家参数");
                }
                BuyShopDatabase db = plugin.getBuyShopDatabase();
                switch (operation) {
                    case "set": {
                        double newDynamic = Util.between(operateValue, 0, item.dynamicValueMaximum);
                        db.setDynamicValue(item, player, newDynamic);
                        if (player == null) {
                            return t(sender, "&a已设置收购商品&e " + item.id + " &a的动态值为&e " + newDynamic);
                        } else {
                            return t(sender, "&a已设置玩家&e " + player.getName() + " &a的收购商品&e " + item.id + " &a的动态值为&e " + newDynamic);
                        }
                    }
                    case "plus": {
                        Double dyn = db.getDynamicValue(item, player);
                        double dynamic = dyn == null ? 0 : dyn;
                        double newDynamic = Util.between(dynamic + operateValue, 0, item.dynamicValueMaximum);
                        db.setDynamicValue(item, player, newDynamic);
                        if (player == null) {
                            return t(sender, "&a已为收购商品&e " + item.id + " &a的动态值增加&e " + operateValue + "&a，增加后为&e " + newDynamic);
                        } else {
                            return t(sender, "&a已为玩家&e " + player.getName() + " &a的收购商品&e " + item.id + " &a的动态值增加&e &a" + operateValue + "，增加后为&e " + newDynamic);
                        }
                    }
                    case "minus": {
                        Double dyn = db.getDynamicValue(item, player);
                        double dynamic = dyn == null ? 0 : dyn;
                        double newDynamic = Util.between(dynamic - operateValue, 0, item.dynamicValueMaximum);
                        db.setDynamicValue(item, player, newDynamic);
                        if (player == null) {
                            return t(sender, "&a已为收购商品&e " + item.id + " &a的动态值减少&e " + operateValue + "&a，减少后为&e " + newDynamic);
                        } else {
                            return t(sender, "&a已为玩家&e " + player.getName() + " &a的收购商品&e " + item.id + " &a的动态值减少&e &a" + operateValue + "，减少后为&e " + newDynamic);
                        }
                    }
                    default:
                        return t(sender, "&e请输入正确的操作类型 &7(set, plus, minus)");
                }
            }
            if ("sell".equalsIgnoreCase(args[1])) {
                SellShop item = SellShopManager.inst().get(args[2]);
                if (item == null) {
                    return t(sender, "&e找不到出售商品&b " + args[2]);
                }
                String operation = args[3].toLowerCase();
                Double operateValue = Util.parseDouble(args[4]).orElse(null);
                if (operateValue == null) {
                    return Messages.int__invalid.tm(sender);
                }
                if (item.dynamicValuePerPlayer && player == null) {
                    return t(sender, "&e这个出售商品要求每个玩家的动态值独立，但你未输入玩家参数");
                }
                SellShopDatabase db = plugin.getSellShopDatabase();
                switch (operation) {
                    case "set": {
                        double newDynamic = Util.between(operateValue, 0, item.dynamicValueMaximum);
                        db.setDynamicValue(item, player, newDynamic);
                        if (player == null) {
                            return t(sender, "&a已设置出售商品&e " + item.id + " &a的动态值为&e " + newDynamic);
                        } else {
                            return t(sender, "&a已设置玩家&e " + player.getName() + " &a的出售商品&e " + item.id + " &a的动态值为&e " + newDynamic);
                        }
                    }
                    case "plus": {
                        Double dyn = db.getDynamicValue(item, player);
                        double dynamic = dyn == null ? 0 : dyn;
                        double newDynamic = Util.between(dynamic + operateValue, 0, item.dynamicValueMaximum);
                        db.setDynamicValue(item, player, newDynamic);
                        if (player == null) {
                            return t(sender, "&a已为出售商品&e " + item.id + " &a的动态值增加&e " + operateValue + "&a，增加后为&e " + newDynamic);
                        } else {
                            return t(sender, "&a已为玩家&e " + player.getName() + " &a的出售商品&e " + item.id + " &a的动态值增加&e &a" + operateValue + "，增加后为&e " + newDynamic);
                        }
                    }
                    case "minus": {
                        Double dyn = db.getDynamicValue(item, player);
                        double dynamic = dyn == null ? 0 : dyn;
                        double newDynamic = Util.between(dynamic - operateValue, 0, item.dynamicValueMaximum);
                        db.setDynamicValue(item, player, newDynamic);
                        if (player == null) {
                            return t(sender, "&a已为出售商品&e " + item.id + " &a的动态值减少&e " + operateValue + "&a，减少后为&e " + newDynamic);
                        } else {
                            return t(sender, "&a已为玩家&e " + player.getName() + " &a的出售商品&e " + item.id + " &a的动态值减少&e &a" + operateValue + "，减少后为&e " + newDynamic);
                        }
                    }
                    default:
                        return t(sender, "&e请输入正确的操作类型 &7(set, plus, minus)");
                }
            }
            return true;
        }
        if (args.length == 3 && "test".equalsIgnoreCase(args[0])) {
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
        return false;
    }

    protected final List<String> listArg0 = Lists.newArrayList(
            "give", "refresh", "dynamic", "test"
    );
    private static final List<String> listArgGive = Lists.newArrayList(
            "buy", "sell", "order");
    private static final List<String> listArgRefresh = Lists.newArrayList(
            "buy", "sell", "order");
    private static final List<String> listArg1Dynamic = Lists.newArrayList(
            "buy", "sell");
    private static final List<String> listArg3Dynamic = Lists.newArrayList(
            "set", "plus", "minus");
    private static final List<String> listArgTest = Lists.newArrayList(
            "buy", "sell", "order");
    @Nullable
    protected List<String> onTabComplete(CommandSender sender, String[] args, AtomicBoolean noChange) {
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("dynamic")) {
                return startsWith(listArg1Dynamic, args[1]);
            }
            if (args[0].equalsIgnoreCase("test")) {
                return startsWith(listArgTest, args[1]);
            }
            if (args[0].equalsIgnoreCase("give")) {
                return null;
            }
            if (args[0].equalsIgnoreCase("refresh")) {
                return null;
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("dynamic")) {
                if (args[1].equalsIgnoreCase("buy")) {
                    return startsWith(BuyShopManager.inst().keys(), args[2]);
                }
                if (args[1].equalsIgnoreCase("sell")) {
                    return startsWith(SellShopManager.inst().keys(), args[2]);
                }
            }
            if (args[0].equalsIgnoreCase("refresh")) {
                return startsWith(listArgRefresh, args[2]);
            }
        }
        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("dynamic")) {
                if (listArg1Dynamic.contains(args[1].toLowerCase())) {
                    return startsWith(listArg3Dynamic, args[3]);
                }
            }
            if (args[0].equalsIgnoreCase("refresh")) {
                if (args[2].equalsIgnoreCase("buy") || args[2].equalsIgnoreCase("sell")) {
                    return startsWith(GroupManager.inst().groups(sender), args[3]);
                }
            }
        }
        if (args.length == 5) {
            if (args[0].equalsIgnoreCase("give") && listArgGive.contains(args[2].toLowerCase())) {
                return startsWith(TemplateManager.inst().itemTemplates(), args[4]);
            }
        }
        if (args.length == 6) {
            if (args[0].equalsIgnoreCase("dynamic")
                    && listArg1Dynamic.contains(args[1].toLowerCase())
                    && listArg3Dynamic.contains(args[3].toLowerCase())
            ) {
                return null;
            }
        }
        noChange.set(true);
        return null;
    }
}
