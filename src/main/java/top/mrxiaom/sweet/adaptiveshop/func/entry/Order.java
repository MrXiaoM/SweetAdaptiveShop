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

    Order(AbstractModule holder, File file, String id) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.id = id;
        this.icon = Utils.getItem(config.getString("icon", ""));
        this.name = config.getString("name", id);
        String limitString = config.getString("limit", "1");
        Integer limit;
        if (limitString.equalsIgnoreCase("unlimited")) {
            limit = null;
        } else {
            limit = Util.parseInt(limitString).orElse(null);
            if (limit == null) {
                throw new IllegalArgumentException("limit 的值无效");
            }
        }
        this.limit = limit;
        this.permission = config.getString("permission", "sweet.adaptive.shop.order." + id).replace("%id%", id);
        String display = config.getString("display");
        if (display == null) {
            throw new IllegalArgumentException("未输入物品显示名");
        }
        this.display = display;
        this.lore = config.getStringList("lore");
        if (lore.isEmpty()) {
            throw new IllegalArgumentException("未输入物品显示Lore");
        }
        this.opApply = config.getString("operations.apply", "");
        this.opCannot = config.getString("operations.cannot", "");
        this.opDone = config.getString("operations.done", "");
        List<String> needsRaw = config.getStringList("needs");
        this.needs = new ArrayList<>();
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
            this.needs.add(new Need(item, amount, affectDynamicValue));
        }
        this.needs.sort(Comparator.comparingInt(it -> it.item.getMatcherPriority())); // 确保 mythic 在前面
        this.rewards = loadActions(config, "rewards");
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
        try {
            return new Order(holder, file, id);
        } catch (Throwable t) {
            holder.warn("[order] 读取 " + id + " 错误，" + t.getMessage());
        }
        return null;
    }
}