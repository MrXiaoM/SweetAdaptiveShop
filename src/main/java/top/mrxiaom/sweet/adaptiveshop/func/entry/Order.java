package top.mrxiaom.sweet.adaptiveshop.func.entry;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.func.BuyShopManager;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    public final String id;
    public final ItemStack icon;
    public final String name;
    public final String display;
    public final List<String> lore;
    public final String opApply;
    public final String opCannot;
    public final List<Need> needs;
    public final List<String> rewards;

    Order(String id, ItemStack icon, String name, String display, List<String> lore, String opApply, String opCannot, List<Need> needs, List<String> rewards) {
        this.id = id;
        this.icon = icon;
        this.name = name;
        this.display = display;
        this.lore = lore;
        this.opApply = opApply;
        this.opCannot = opCannot;
        this.needs = needs;
        this.rewards = rewards;
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
        List<String> rewards = config.getStringList("rewards");
        return new Order(id, icon, name, display, lore, opApply, opCannot, needs, rewards);
    }
}