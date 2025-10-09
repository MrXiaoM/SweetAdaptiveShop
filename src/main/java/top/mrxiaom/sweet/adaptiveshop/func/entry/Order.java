package top.mrxiaom.sweet.adaptiveshop.func.entry;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.func.config.BuyShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.BuyShop;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.io.File;
import java.util.*;

import static top.mrxiaom.pluginbase.actions.ActionProviders.loadActions;

public class Order {
    public static class Need {
        public final BuyShop item;
        public final int amount;
        public final boolean affectDynamicValue;

        public Need(BuyShop item, int amount, boolean affectDynamicValue) {
            this.item = item;
            this.amount = amount;
            this.affectDynamicValue = affectDynamicValue;
        }
    }

    public final String id, permission;
    public final ItemStack icon;
    public final String name;
    public final Integer limit;
    public final String display;
    public final List<String> lore;
    public final String opApply;
    public final String opCannot;
    public final String opDone;
    public final List<Need> needs;
    public final List<IAction> rewards;

    Order(String id, String permission, ItemStack icon, String name, Integer limit, String display, List<String> lore, String opApply, String opCannot, String opDone, List<Need> needs, List<IAction> rewards) {
        this.id = id;
        this.permission = permission;
        this.icon = icon;
        this.name = name;
        this.limit = limit;
        this.display = display;
        this.lore = lore;
        this.opApply = opApply;
        this.opCannot = opCannot;
        this.opDone = opDone;
        this.needs = needs;
        this.rewards = rewards;
    }

    public boolean hasPermission(Player player) {
        return player.hasPermission(permission);
    }

    public boolean match(Player player) {
        for (Order.Need need : needs) {
            int count = need.item.getCount(player);
            if (count < need.amount) return false;
        }
        return true;
    }

    public boolean isAllDone(int doneCount) {
        if (limit == null) return false;
        return doneCount >= limit;
    }

    public void takeAll(Player player) {
        PlayerInventory inv = player.getInventory();
        Map<Need, Integer> takeCount = new HashMap<>();
        for (int i = inv.getSize() - 1; i >= 0; i--) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().equals(Material.AIR) || item.getAmount() == 0) continue;
            for (Need need : needs) {
                if (need.item.match(item)) {
                    int needAmount = takeCount.getOrDefault(need, need.amount);
                    if (needAmount == 0) break;
                    int amount = item.getAmount();
                    if (needAmount >= amount) {
                        needAmount -= amount;
                        item.setType(Material.AIR);
                        item.setAmount(0);
                        item = null;
                    } else {
                        item.setAmount(amount - needAmount);
                        needAmount = 0;
                    }
                    takeCount.put(need, needAmount);
                    inv.setItem(i, item);
                    break;
                }
            }
        }
        SweetAdaptiveShop plugin = SweetAdaptiveShop.getInstance();
        for (Map.Entry<Need, Integer> entry : takeCount.entrySet()) {
            Need need = entry.getKey();
            Integer needToTake = entry.getValue();
            BuyShop item = need.item;
            double value = item.dynamicValueAdd * (need.amount - needToTake);
            if (need.affectDynamicValue) plugin.getScheduler().runTaskAsync(() -> item.addDynamicValue(plugin, player, value, need.amount));
            if (needToTake > 0) {
                plugin.warn("预料中的错误: 玩家 " + player + " 提交任务 " + id + " 的需求物品 " + item.id + " 时，有 " + needToTake + " 个物品未提交成功");
            }
        }
    }

    @Nullable
    public static Order load(AbstractModule holder, File file, String id) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ItemStack icon;
        try {
            icon = Utils.getItem(config.getString("icon", ""));
        } catch (IllegalStateException e) {
            holder.warn("[order] 读取 " + id + " 错误，" + e.getMessage());
            return null;
        }
        String name = config.getString("name", id);
        String limitString = config.getString("limit", "1");
        Integer limit;
        if (limitString.equalsIgnoreCase("unlimited")) {
            limit = null;
        } else {
            limit = Util.parseInt(limitString).orElse(null);
            if (limit == null) {
                holder.warn("[order] 读取 " + id + " 错误，limit 的值无效");
                return null;
            }
        }
        String permission = config.getString("permission", "sweet.adaptive.shop.order." + id).replace("%id%", id);
        String display = config.getString("display");
        if (display == null) {
            holder.warn("[order] 读取 " + id + " 错误，未输入物品显示名");
            return null;
        }
        List<String> lore = config.getStringList("lore");
        if (lore.isEmpty()) {
            holder.warn("[order] 读取 " + id + " 错误，未输入物品显示Lore");
            return null;
        }
        String opApply = config.getString("operations.apply", "");
        String opCannot = config.getString("operations.cannot", "");
        String opDone = config.getString("operations.done", "");
        List<String> needsRaw = config.getStringList("needs");
        List<Need> needs = new ArrayList<>();
        BuyShopManager manager = BuyShopManager.inst();
        for (String s : needsRaw) {
            String[] split = s.split(" ", 2);
            boolean affectDynamicValue = split.length == 2 && split[1].equals("true");
            split = split[0].split(":", 2);
            if (split.length != 2) {
                holder.warn("[order] 无法读取 " + id + " 中的需求商品 " + s);
                continue;
            }
            BuyShop item = manager.get(split[0]);
            if (item == null) {
                holder.warn("[order] 订单 " + id + " 中的需求商品 " + split[0] + " 不存在");
                continue;
            }
            Integer amount = Util.parseInt(split[1]).orElse(null);
            if (amount == null) {
                holder.warn("[order] 订单 " + id + " 中的需求物品数量 " + split[1] + " 不正确");
                continue;
            }
            needs.add(new Need(item, amount, affectDynamicValue));
        }
        needs.sort(Comparator.comparingInt(it -> it.item.getMatcherPriority())); // 确保 mythic 在前面
        List<IAction> rewards = loadActions(config, "rewards");
        return new Order(id, permission, icon, name, limit, display, lore, opApply, opCannot, opDone, needs, rewards);
    }
}