package top.mrxiaom.sweet.adaptiveshop.func.config.customgui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.PAPI;
import top.mrxiaom.sweet.adaptiveshop.Messages;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerOrder;
import top.mrxiaom.sweet.adaptiveshop.func.config.CustomGuiManager;
import top.mrxiaom.sweet.adaptiveshop.func.config.OrderManager;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Order;

import java.util.ArrayList;
import java.util.List;

public class ShopIconOrder extends ShopIcon {
    private final Order order;
    private final String orderLine;
    public ShopIconOrder(String itemId, Order order, ConfigurationSection config) {
        super("order", itemId);
        this.order = order;
        this.orderLine = config.getString("line");
    }

    private UnsupportedOperationException todo() {
        return new UnsupportedOperationException("由于数据表结构限制，暂时不支持使用自定义订单");
    }

    @Override
    public ItemStack generateIcon(CustomGuiManager.Impl gui, Player player) {
        throw todo();
        // return generateIcon(order, data, player, orderLine);
    }

    public static ItemStack generateIcon(Order order, PlayerOrder data, Player player, String orderLine) {
        int doneCount = data.getDoneCount();
        String doneCountStr = String.valueOf(doneCount);
        boolean hasDone = order.isAllDone(doneCount);
        ItemStack item = order.icon.clone();
        String display = order.display;
        List<String> lore = new ArrayList<>();
        for (String s : order.lore) {
            if (s.equals("needs")) {
                for (Order.Need need : order.needs) {
                    int count = hasDone ? need.amount : Math.min(need.amount, need.item.getCount(player));
                    lore.add(orderLine.replace("%name%", need.item.displayName)
                            .replace("%count%", String.valueOf(count))
                            .replace("%require%", String.valueOf(need.amount)));
                }
                continue;
            }
            if (s.equals("operation")) {
                if (hasDone) {
                    lore.add(order.opDone);
                } else {
                    lore.add(order.match(player) ? order.opApply : order.opCannot);
                }
                continue;
            }
            lore.add(s.replace("%done_count%", doneCountStr));
        }
        AdventureItemStack.setItemDisplayName(item, PAPI.setPlaceholders(player, display));
        AdventureItemStack.setItemLoreMiniMessage(item, PAPI.setPlaceholders(player, lore));
        return item;
    }

    @Override
    public void onClick(CustomGuiManager.Impl gui, Player player, ClickType click) {
        throw todo();
        // if (click(gui.manager().plugin, click, order, data, player)) {
        //     gui.postSubmit();
        // }
    }

    public static boolean click(SweetAdaptiveShop plugin, ClickType click, Order order, PlayerOrder data, Player player) {
        if (click.equals(ClickType.LEFT)) {
            if (data.isOutdate()) {
                Messages.gui__order__outdate.tm(player);
                return false;
            }
            if (order.isAllDone(data.getDoneCount())) {
                Messages.gui__order__has_done.tm(player);
                return false;
            }
            if (!order.match(player)) {
                Messages.gui__order__not_enough.tm(player);
                return false;
            }
            player.closeInventory();
            order.takeAll(player);
            plugin.getOrderDatabase().markOrderDone(player, order.id, data.getDoneCount() + 1);
            Messages.gui__order__success.tm(player, order.display);
            for (IAction reward : order.rewards) {
                reward.run(player);
            }
            return true;
        }
        return false;
    }

    @Nullable
    public static ShopIconOrder load(CustomGuiManager manager, String parentId, ConfigurationSection config, String itemId) {
        Order order = OrderManager.inst().get(itemId);
        if (order == null) {
            manager.warn("[shop/custom/" + parentId + "] 找不到订单 " + itemId);
            return null;
        }
        return new ShopIconOrder(itemId, order, config);
    }
}
