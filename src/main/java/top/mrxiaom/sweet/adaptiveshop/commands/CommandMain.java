package top.mrxiaom.sweet.adaptiveshop.commands;
        
import com.google.common.collect.Lists;
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
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.Messages;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.func.BuyShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.TemplateManager;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Group;
import top.mrxiaom.sweet.adaptiveshop.func.entry.ItemTemplate;
import top.mrxiaom.sweet.adaptiveshop.gui.GuiBuyShop;
import top.mrxiaom.sweet.adaptiveshop.gui.GuiOrders;
import top.mrxiaom.sweet.adaptiveshop.utils.TimeUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

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
            Group buyGroup;
            if (type.equals("buy")) {
                String name = args.length >= 3 ? args[2] : "default";
                buyGroup = BuyShopManager.inst().getGroup(name);
                if (buyGroup == null) {
                    return t(sender, "分组 " + name + " 不存在");
                }
                playerCheckIndex = 3;
            } else {
                buyGroup = null;
                playerCheckIndex = 2;
            }
            if (args.length >= playerCheckIndex + 1) {
                if (sender.isOp()) {
                    player = Util.getOnlinePlayer(args[playerCheckIndex]).orElse(null);
                    if (player == null) {
                        return t(sender, "&e玩家 " + args[playerCheckIndex] + " 不在线");
                    }
                } else {
                    return t(sender, "&c你没有执行此操作的权限");
                }
            }
            if (player == null) {
                return t(sender, "只有玩家才能执行该命令");
            }
            switch (type.toLowerCase()) {
                case "buy":
                    if (buyGroup == null) return true;
                    if (!player.hasPermission("sweet.adaptive.shop.group." + buyGroup.id)) {
                        return t(player, "&c你没有执行此操作的权限");
                    }
                    GuiBuyShop.create(player, buyGroup).open();
                    return true;
                case "order":
                    if (!player.hasPermission("sweet.adaptive.shop.order")) {
                        return t(player, "&c你没有执行此操作的权限");
                    }
                    GuiOrders.create(player).open();
                    return true;
            }
            return t(sender, "&e找不到这个界面!");
        }
        if (args.length > 5 && "give".equalsIgnoreCase(args[0]) && sender.isOp()) {
            Player player = Util.getOnlinePlayer(args[1]).orElse(null);
            if (player == null) {
                return t(sender, "&e玩家 " + args[1] + " 不在线");
            }
            String templateId = args[2];
            TemplateManager manager = TemplateManager.inst();
            ItemTemplate template = manager.getTemplate(templateId);
            if (template == null) {
                return t(sender, "&e找不到名为 " + templateId + " 的物品模板");
            }
            int amount = Util.parseInt(args[3]).orElse(0);
            if (amount < 1) {
                return t(sender, "&e请输入正确的数量");
            }
            if (amount > template.material.getMaxStackSize()) {
                return t(sender, "&e你输入的数量太多了");
            }
            String nbtKey;
            if (args[4].equalsIgnoreCase("buy")) {
                nbtKey = GuiBuyShop.REFRESH_ITEM;
            } else if (args[4].equalsIgnoreCase("order")) {
                nbtKey = GuiOrders.REFRESH_ITEM;
            } else {
                return t(sender, "&e无效的物品类型 " + args[4]);
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
            ItemStackUtil.giveItemToPlayer(player, items);
            return t(sender, "&a成功给予 " + player.getName() + " " + amount + " 个 " + template.display);
        }
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0]) && sender.isOp()) {
            plugin.reloadConfig();
            return t(sender, "&a配置文件已重载");
        }
        if (args.length == 2 && "reload".equalsIgnoreCase(args[0]) && "database".equalsIgnoreCase(args[1]) && sender.isOp()) {
            plugin.options.database().reloadConfig();
            plugin.options.database().reconnect();
            return t(sender, "&a数据库已重新连接");
        }
        return (sender.isOp() ? Messages.help_op : Messages.help).tm(sender);
    }

    private static final List<String> emptyList = Lists.newArrayList();
    private static final List<String> listArg0 = Lists.newArrayList(
            "open");
    private static final List<String> listArgOpen = Lists.newArrayList(
            "buy", "order");
    private static final List<String> listArgGive = Lists.newArrayList(
            "buy", "order");
    private static final List<String> listOpArg0 = Lists.newArrayList(
            "open", "reload");
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
            if (args[0].equalsIgnoreCase("give") && sender.isOp()) {
                return null;
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("open") && listArgOpen.contains(args[1].toLowerCase()) && sender.isOp()) {
                return null;
            }
        }
        if (args.length == 5) {
            if (args[0].equalsIgnoreCase("give") && listArgGive.contains(args[2].toLowerCase()) && sender.isOp()) {
                return startsWith(TemplateManager.inst().itemTemplates(), args[4]);
            }
        }
        return emptyList;
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
