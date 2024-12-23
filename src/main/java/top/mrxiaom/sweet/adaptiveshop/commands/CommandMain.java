package top.mrxiaom.sweet.adaptiveshop.commands;
        
import com.google.common.collect.Lists;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.func.BuyShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Group;
import top.mrxiaom.sweet.adaptiveshop.gui.GuiBuyShop;
import top.mrxiaom.sweet.adaptiveshop.gui.GuiOrders;

import java.util.*;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    public CommandMain(SweetAdaptiveShop plugin) {
        super(plugin);
        registerCommand("sweetadptiveshop", this);
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
                    GuiBuyShop.create(player, buyGroup).open();
                    return true;
                case "order":
                    GuiOrders.create(player).open();
                    return true;
            }
            return t(sender, "&e找不到这个界面!");
        }
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0]) && sender.isOp()) {
            plugin.reloadConfig();
            return t(sender, "&a配置文件已重载");
        }
        return true;
    }

    private static final List<String> emptyList = Lists.newArrayList();
    private static final List<String> listArg0 = Lists.newArrayList(
            "open");
    private static final List<String> listArgOpen = Lists.newArrayList(
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
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("open") && listArgOpen.contains(args[1].toLowerCase()) && sender.isOp()) {
                return null;
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
