package top.mrxiaom.sweet.adaptiveshop.gui;

import de.tr7zw.changeme.nbtapi.NBT;
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
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.gui.IGui;
import top.mrxiaom.pluginbase.utils.*;
import top.mrxiaom.sweet.adaptiveshop.Messages;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractGuiModule;
import top.mrxiaom.sweet.adaptiveshop.func.config.SellShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Group;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.SellShop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static top.mrxiaom.sweet.adaptiveshop.utils.Utils.resolveRefreshCount;
import static top.mrxiaom.sweet.adaptiveshop.utils.Utils.takeFirstRefreshCount;

@AutoRegister
public class GuiSellShop extends AbstractGuiModule {
    public static final String REFRESH_ITEM = "SWEET_ADAPTIVE_SHOP_REFRESH_SELL";
    boolean closeAfterSubmit;
    public GuiSellShop(SweetAdaptiveShop plugin) {
        super(plugin, new File(plugin.getDataFolder(), "gui/sell.yml")); // 界面配置文件
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        if (!file.exists()) { // 不存在时保存
            plugin.saveResource("gui/sell.yml", file);
        }
        super.reloadConfig(cfg);
    }

    @Override
    protected void reloadMenuConfig(YamlConfiguration config) {
        closeAfterSubmit = config.getBoolean("close-after-submit");
    }

    @Override
    protected String warningPrefix() {
        return "[gui/sell.yml]";
    }

    LoadedIcon sellSlot, emptySlot, refreshIcon;
    String sellOne, sellStack, refreshAvailable, refreshUnavailable;
    List<String> sellBypassLore;
    @Override
    protected void loadMainIcon(ConfigurationSection section, String id, LoadedIcon loadedIcon) {
        switch (id) {
            case "物":
                sellSlot = loadedIcon;
                sellOne = section.getString(id + ".operations.one");
                sellStack = section.getString(id + ".operations.stack");
                sellBypassLore = section.getStringList(id + ".lore-bypass");
            case "物_空白":
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
            case '物': {
                int i = appearTimes - 1;
                if (i >= gui.items.size()) {
                    if (emptySlot.material.equals("AIR")) return new ItemStack(Material.AIR);
                    return emptySlot.generateIcon(player);
                }
                SellShop shop = gui.items.get(i).getKey();
                boolean bypass = shop.hasBypass(player);
                double dynamic;
                if (bypass) {
                    dynamic = 0;
                } else {
                    Double dyn = plugin.getSellShopDatabase().getDynamicValue(shop, player);
                    dynamic = dyn == null ? 0.0 : dyn;
                }
                boolean noCut = shop.dynamicValueMaximum == 0 || !shop.dynamicValueCutWhenMaximum;
                double price = bypass ? shop.priceBase : shop.getPrice(dynamic);
                int count = (int) Math.floor(plugin.getEconomy().get(player) / price);
                String priceString = String.format("%.2f", price).replace(".00", "");
                String dynamicDisplay = bypass ? "" : shop.getDisplayDynamic(dynamic);
                String dynamicPlaceholder = bypass ? "" : shop.getDynamicValuePlaceholder(dynamic);

                ItemStack item = shop.displayItem.clone();
                String displayName = sellSlot.display.replace("%name%", shop.displayName);
                List<String> lore = new ArrayList<>();
                List<String> loreTemplate = bypass ? sellBypassLore : sellSlot.lore;
                for (String s : loreTemplate) {
                    if (s.equals("description")) {
                        lore.addAll(ItemStackUtil.getItemLore(shop.displayItem));
                        continue;
                    }
                    if (s.equals("operation")) {
                        if (count >= 1) {
                            if (noCut || dynamic + shop.dynamicValueAdd <= shop.dynamicValueMaximum) {
                                lore.add(sellOne.replace("%price%", priceString));
                            }
                        }
                        int stackSize = item.getType().getMaxStackSize();
                        if (count >= stackSize) {
                            if (noCut || dynamic + shop.dynamicValueAdd * stackSize <= shop.dynamicValueMaximum) {
                                lore.add(sellStack.replace("%price%", String.format("%.2f", price * stackSize).replace(".00", ""))
                                        .replace("%count%", String.valueOf(stackSize)));
                            }
                        }
                        continue;
                    }
                    lore.add(s.replace("%price%", priceString)
                            .replace("%dynamic%", dynamicDisplay)
                            .replace("%dynamic_placeholder%", dynamicPlaceholder));
                }
                AdventureItemStack.setItemDisplayName(item, PAPI.setPlaceholders(player, displayName));
                AdventureItemStack.setItemLoreMiniMessage(item, PAPI.setPlaceholders(player, lore));
                if (!sellSlot.nbtStrings.isEmpty() || !sellSlot.nbtInts.isEmpty()) {
                    NBT.modify(item, nbt -> {
                        for (Map.Entry<String, String> entry : sellSlot.nbtStrings.entrySet()) {
                            String value = PAPI.setPlaceholders(player, entry.getValue());
                            nbt.setString(entry.getKey(), value);
                        }
                        for (Map.Entry<String, String> entry : sellSlot.nbtInts.entrySet()) {
                            String value = PAPI.setPlaceholders(player, entry.getValue());
                            Integer j = Util.parseInt(value).orElse(null);
                            if (j == null) continue;
                            nbt.setInteger(entry.getKey(), j);
                        }
                    });
                }
                return item;
            }
            case '刷': {
                int count = resolveRefreshCount(player, REFRESH_ITEM);
                return refreshIcon.generateIcon(player, name -> name.replace("%type%", gui.group.display), oldLore -> {
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

    public static GuiSellShop inst() {
        return instanceOf(GuiSellShop.class);
    }

    public static Impl create(@NotNull Player player, @NotNull Group group) {
        GuiSellShop self = inst();
        return self.new Impl(player, self.guiTitle, self.guiInventory, group);
    }

    public class Impl extends Gui {
        Group group;
        List<Pair<SellShop, PlayerItem>> items;
        protected Impl(@NotNull Player player, String title, char[] inventory, @NotNull Group group) {
            super(player, PAPI.setPlaceholders(player, title.replace("%type%", group.display)), inventory);
            this.group = group;
            this.items = SellShopManager.inst().getPlayerItems(player, group.id);
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
                        Messages.refresh__sell__not_enough.tm(player);
                        return;
                    }
                    group.refreshSellShop(player);
                    Messages.refresh__sell__success.tm(player, group.display);
                    this.items = SellShopManager.inst().getPlayerItems(player, group.id);
                    open();
                    return;
                }
                if (id.equals('物')) {
                    int i = getAppearTimes(id, slot) - 1;
                    if (i >= items.size()) return;
                    Pair<SellShop, PlayerItem> pair = items.get(i);
                    SellShop shop = pair.getKey();
                    if (click.equals(ClickType.LEFT)) { // 购买1个
                        if (pair.getValue().isOutdate()) {
                            Messages.gui__sell__outdate.tm(player);
                            return;
                        }
                        Double dyn = plugin.getSellShopDatabase().getDynamicValue(shop, player);
                        double dynamic = dyn == null ? 0.0 : dyn;
                        if (shop.dynamicValueMaximum > 0 && shop.dynamicValueCutWhenMaximum) {
                            double add = shop.dynamicValueAdd;
                            if (dynamic + add > shop.dynamicValueMaximum) {
                                Messages.gui__sell__maximum.tm(player);
                                return;
                            }
                        }
                        double price;
                        if (shop.hasBypass(player)) {
                            price = shop.priceBase;
                        } else {
                            price = shop.getPrice(dynamic);
                        }
                        String money = String.format("%.2f", price).replace(".00", "");
                        if (!plugin.getEconomy().has(player, price)) {
                            Messages.gui__sell__no_money.tm(player);
                            return;
                        }
                        plugin.getEconomy().takeMoney(player, price);
                        shop.give(player, 1);
                        Messages.gui__sell__success.tm(player, money, 1, shop.displayName);
                        postSubmit(view);
                        return;
                    }
                    if (click.equals(ClickType.RIGHT)) { // 提交1组
                        if (pair.getValue().isOutdate()) {
                            Messages.gui__sell__outdate.tm(player);
                            return;
                        }
                        int stackSize = shop.displayItem.getType().getMaxStackSize();
                        Double dyn = plugin.getSellShopDatabase().getDynamicValue(shop, player);
                        double dynamic = dyn == null ? 0.0 : dyn;
                        if (shop.dynamicValueMaximum > 0 && shop.dynamicValueCutWhenMaximum) {
                            double add = shop.dynamicValueAdd * stackSize;
                            if (dynamic + add > shop.dynamicValueMaximum) {
                                Messages.gui__sell__maximum.tm(player);
                                return;
                            }
                        }
                        double price;
                        if (shop.hasBypass(player)) {
                            price = shop.priceBase;
                        } else {
                            price = shop.getPrice(dynamic);
                        }
                        String money = String.format("%.2f", price * stackSize).replace(".00", "");
                        double total = Double.parseDouble(money);
                        if (!plugin.getEconomy().has(player, total)) {
                            Messages.gui__sell__no_money.tm(player);
                            return;
                        }
                        plugin.getEconomy().takeMoney(player, total);
                        shop.give(player, stackSize);
                        Messages.gui__sell__success.tm(player, money, stackSize, shop.displayName);
                        postSubmit(view);
                        return;
                    }
                    return;
                }
                LoadedIcon icon = otherIcons.get(id);
                if (icon != null) {
                    icon.click(player, click);
                }
            }
        }

        private void postSubmit(InventoryView view) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (closeAfterSubmit) {
                    player.closeInventory();
                    Util.submitInvUpdate(player);
                } else {
                    updateInventory(view);
                }
            }, 1L);
        }
    }
}
