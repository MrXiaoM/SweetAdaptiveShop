package top.mrxiaom.sweet.adaptiveshop.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.gui.IGui;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.PAPI;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.Messages;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerOrder;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractGuiModule;
import top.mrxiaom.sweet.adaptiveshop.func.config.OrderManager;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Order;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static top.mrxiaom.sweet.adaptiveshop.utils.Utils.resolveRefreshCount;
import static top.mrxiaom.sweet.adaptiveshop.utils.Utils.takeFirstRefreshCount;

@AutoRegister
public class GuiOrders extends AbstractGuiModule {
    public static final String REFRESH_ITEM = "SWEET_ADAPTIVE_SHOP_REFRESH_ORDER";
    boolean closeAfterSubmit;
    public GuiOrders(SweetAdaptiveShop plugin) {
        super(plugin, new File(plugin.getDataFolder(), "gui/order.yml"));
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        if (!file.exists()) { // 不存在时保存
            plugin.saveResource("gui/order.yml", file);
        }
        super.reloadConfig(cfg);
    }

    @Override
    protected void reloadMenuConfig(YamlConfiguration config) {
        closeAfterSubmit = config.getBoolean("close-after-submit");
    }

    @Override
    protected String warningPrefix() {
        return "[gui/order.yml]";
    }

    LoadedIcon emptySlot, refreshIcon;
    String orderLine, refreshAvailable, refreshUnavailable;
    @Override
    protected void loadMainIcon(ConfigurationSection section, String id, LoadedIcon loadedIcon) {
        switch (id) {
            case "订":
                orderLine = section.getString(id + ".line");
            case "订_空白":
                emptySlot = loadedIcon;
            case "刷":
                refreshIcon = loadedIcon;
                refreshAvailable = section.getString(id + ".operations.available");
                refreshUnavailable = section.getString(id + ".operations.unavailable");
                break;
        }
    }

    @Override
    protected ItemStack applyMainIcon(IGui instance, Player player, char id, int index, int appearTimes) {
        Impl gui = (Impl) instance;
        switch (id) {
            case '订': {
                int i = appearTimes - 1;
                if (i >= gui.orders.size()) {
                    if (emptySlot.material.equals("AIR")) return new ItemStack(Material.AIR);
                    return emptySlot.generateIcon(player);
                }
                Pair<Order, PlayerOrder> pair = gui.orders.get(i);
                Order order = pair.getKey();
                int doneCount = pair.getValue().getDoneCount();
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
            case '刷': {
                int count = resolveRefreshCount(player, REFRESH_ITEM);
                return refreshIcon.generateIcon(player, null, oldLore -> {
                    List<String> lore = new ArrayList<>();
                    for (String s : oldLore) {
                        if (s.equals("operation")) {
                            lore.add(count > 0 ? refreshAvailable : refreshUnavailable);
                            continue;
                        }
                        lore.add(s.replace("%count%", String.valueOf(count)));
                    }
                    return lore;
                });
            }
        }
        return null;
    }

    public static GuiOrders inst() {
        return instanceOf(GuiOrders.class);
    }

    public static Impl create(Player player) {
        GuiOrders self = inst();
        return self.new Impl(player, self.guiTitle, self.guiInventory);
    }

    public class Impl extends Gui {
        List<Pair<Order, PlayerOrder>> orders;
        protected Impl(Player player, String title, char[] inventory) {
            super(player, PAPI.setPlaceholders(player, title), inventory);
            this.orders = OrderManager.inst().getPlayerOrders(player);
        }

        @Override
        public void onClick(InventoryAction action, ClickType click, InventoryType.SlotType slotType,
                            int slot, ItemStack currentItem, ItemStack cursor,
                            InventoryView view, InventoryClickEvent event) {
            event.setCancelled(true);
            Character id = getClickedId(slot);
            if (id != null) {
                if (id.equals('刷')) {
                    if (!takeFirstRefreshCount(player, REFRESH_ITEM)) {
                        Messages.refresh__order__not_enough.tm(player);
                        return;
                    }
                    OrderManager.inst().refresh(player);
                    Messages.refresh__order__success.tm(player);
                    this.orders = OrderManager.inst().getPlayerOrders(player);
                    open();
                    return;
                }
                if (id.equals('订')) {
                    int i = getAppearTimes(id, slot) - 1;
                    if (i >= orders.size()) return;
                    Pair<Order, PlayerOrder> pair = orders.get(i);
                    Order order = pair.getKey();
                    if (click.equals(ClickType.LEFT)) {
                        PlayerOrder data = pair.getValue();
                        if (data.isOutdate()) {
                            Messages.gui__order__outdate.tm(player);
                            return;
                        }
                        if (order.isAllDone(data.getDoneCount())) {
                            Messages.gui__order__has_done.tm(player);
                            return;
                        }
                        if (!order.match(player)) {
                            Messages.gui__order__not_enough.tm(player);
                            return;
                        }
                        player.closeInventory();
                        order.takeAll(player);
                        plugin.getOrderDatabase().markOrderDone(player, order.id, data.getDoneCount() + 1);
                        Messages.gui__order__success.tm(player, order.display);
                        for (IAction reward : order.rewards) {
                            reward.run(player);
                        }
                        plugin.getScheduler().runTaskLater(() -> {
                            if (closeAfterSubmit) {
                                player.closeInventory();
                                Util.submitInvUpdate(player);
                            } else {
                                updateInventory(view);
                            }
                        }, 1L);
                    }
                    return;
                }
                LoadedIcon icon = otherIcons.get(id);
                if (icon != null) {
                    icon.click(player, click);
                }
            }
        }
    }
}
