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
import top.mrxiaom.pluginbase.gui.IGui;
import top.mrxiaom.pluginbase.utils.*;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractGuiModule;
import top.mrxiaom.sweet.adaptiveshop.func.BuyShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.entry.BuyShop;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Group;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static top.mrxiaom.sweet.adaptiveshop.utils.Utils.resolveRefreshCount;
import static top.mrxiaom.sweet.adaptiveshop.utils.Utils.takeFirstRefreshCount;

@AutoRegister
public class GuiBuyShop extends AbstractGuiModule {
    public static final String REFRESH_ITEM = "SWEET_ADAPTIVE_SHOP_REFRESH";
    boolean closeAfterSubmit;
    public GuiBuyShop(SweetAdaptiveShop plugin) {
        super(plugin, new File(plugin.getDataFolder(), "gui/buy.yml")); // 界面配置文件
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        if (!file.exists()) { // 不存在时保存
            plugin.saveResource("gui/buy.yml", file);
        }
        super.reloadConfig(cfg);
    }

    @Override
    protected void reloadMenuConfig(YamlConfiguration config) {
        closeAfterSubmit = config.getBoolean("close-after-submit");
    }

    @Override
    protected String warningPrefix() {
        return "[gui/buy.yml]";
    }

    LoadedIcon buySlot, emptySlot, refreshIcon;
    String buyOne, buyStack, buyAll, refreshAvailable, refreshUnavailable;
    @Override
    protected void loadMainIcon(ConfigurationSection section, String id, LoadedIcon loadedIcon) {
        switch (id) {
            case "物":
                buySlot = loadedIcon;
                buyOne = section.getString(id, ".operations.one");
                buyStack = section.getString(id, ".operations.stack");
                buyAll = section.getString(id, ".operations.all");
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
                    if (emptySlot.material.equals(Material.AIR)) return new ItemStack(Material.AIR);
                    return emptySlot.generateIcon(player);
                }
                BuyShop shop = gui.items.get(i).getKey();
                Double dyn = plugin.getBuyShopDatabase().getDynamicValue(shop.id);
                double dynamic = dyn == null ? 0.0 : dyn;
                int count = shop.getCount(player);
                double price = shop.getPrice(dynamic);
                String priceString = String.format("%.2f", price);
                String dynamicDisplay = shop.getDisplayDynamic(dynamic);
                String dynamicPlaceholder = shop.getDynamicValuePlaceholder(dynamic);

                ItemStack item = shop.displayItem.clone();
                String displayName = buySlot.display.replace("%name%", shop.displayName);
                List<String> lore = new ArrayList<>();
                for (String s : buySlot.lore) {
                    if (s.equals("description")) {
                        lore.addAll(ItemStackUtil.getItemLore(shop.displayItem));
                        continue;
                    }
                    if (s.equals("operation")) {
                        if (count >= 1) {
                            lore.add(buyOne.replace("%price%", priceString));
                        }
                        int stackSize = item.getType().getMaxStackSize();
                        if (count >= stackSize) {
                            lore.add(buyStack.replace("%price%", String.format("%.2f", price * stackSize))
                                    .replace("%count%", String.valueOf(stackSize)));
                        }
                        if (count >= 1) {
                            lore.add(buyAll.replace("%price%", String.format("%.2f", price * count))
                                    .replace("%count%", String.valueOf(count)));
                        }
                        continue;
                    }
                    lore.add(s.replace("%price%", priceString)
                            .replace("%dynamic%", dynamicDisplay)
                            .replace("%dynamic_placeholder%", dynamicPlaceholder));
                }
                AdventureItemStack.setItemDisplayName(item, PAPI.setPlaceholders(player, displayName));
                AdventureItemStack.setItemLore(item, PAPI.setPlaceholders(player, lore));
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
                        lore.add(s);
                    }
                    return lore;
                });
            }
        }
        return null;
    }

    public static GuiBuyShop inst() {
        return instanceOf(GuiBuyShop.class);
    }

    public static Impl create(Player player, Group group) {
        GuiBuyShop self = inst();
        return self.new Impl(player, self.guiTitle, self.guiInventory, group);
    }

    public class Impl extends Gui {
        Group group;
        List<Pair<BuyShop, PlayerItem>> items;
        protected Impl(Player player, String title, char[] inventory, Group group) {
            super(player, title, inventory);
            this.group = group;
            this.items = BuyShopManager.inst().getPlayerItems(player, group.id);
        }

        @Override
        public void onClick(InventoryAction action, ClickType click, InventoryType.SlotType slotType,
                            int slot, ItemStack currentItem, ItemStack cursor,
                            InventoryView view, InventoryClickEvent event) {
            Character id = getClickedId(slot);
            if (id != null) {
                if (id.equals('刷')) {
                    if (!takeFirstRefreshCount(player, REFRESH_ITEM)) {
                        t(player, "&e你没有足够的刷新券!");
                        return;
                    }
                    // TODO: 刷新商品
                    return;
                }
                if (id.equals('物')) {
                    int i = getAppearTimes(id, slot) - 1;
                    if (i >= items.size()) return;
                    Pair<BuyShop, PlayerItem> pair = items.get(i);
                    BuyShop shop = pair.getKey();
                    int count = shop.getCount(player);
                    if (click.equals(ClickType.LEFT)) { // 提交1个
                        if (pair.getValue().isOutdate()) {
                            t(player, "&e这个商品已经过期了! 请重新打开菜单以刷新列表!");
                            return;
                        }
                        if (count < 1) {
                            t(player, "&e你没有足够的物品提交到商店!");
                            return;
                        }
                        shop.take(player, 1);
                        Double dyn = plugin.getBuyShopDatabase().getDynamicValue(shop.id);
                        double dynamic = dyn == null ? 0.0 : dyn;
                        double price = shop.getPrice(dynamic);
                        plugin.getEconomy().giveMoney(player, price);
                        AdventureUtil.sendMessage(player, "&a你提交了 &e1&a 个 &e" + shop.displayName + "&a，获得 &e" + String.format("%.2f", price) + "&a 金币!");
                        postSubmit(view);
                        return;
                    }
                    if (click.equals(ClickType.RIGHT)) { // 提交1组
                        if (pair.getValue().isOutdate()) {
                            t(player, "&e这个商品已经过期了! 请重新打开菜单以刷新列表!");
                            return;
                        }
                        int stackSize = shop.displayItem.getType().getMaxStackSize();
                        if (count < stackSize) {
                            t(player, "&e你没有足够的物品提交到商店!");
                            return;
                        }
                        shop.take(player, stackSize);
                        Double dyn = plugin.getBuyShopDatabase().getDynamicValue(shop.id);
                        double dynamic = dyn == null ? 0.0 : dyn;
                        double price = shop.getPrice(dynamic);
                        String money = String.format("%.2f", price * stackSize);
                        plugin.getEconomy().giveMoney(player, Double.parseDouble(money));
                        AdventureUtil.sendMessage(player, "&a你提交了 &e" + stackSize + "&a 个 &e" + shop.displayName + "&a，获得 &e" + money + "&a 金币!");
                        postSubmit(view);
                        return;
                    }
                    if (click.equals(ClickType.SHIFT_LEFT)) { // 提交全部
                        if (pair.getValue().isOutdate()) {
                            t(player, "&e这个商品已经过期了! 请重新打开菜单以刷新列表!");
                            return;
                        }
                        if (count < 1) {
                            t(player, "&e你没有足够的物品提交到商店!");
                            return;
                        }
                        shop.take(player, count);
                        Double dyn = plugin.getBuyShopDatabase().getDynamicValue(shop.id);
                        double dynamic = dyn == null ? 0.0 : dyn;
                        double price = shop.getPrice(dynamic);
                        String money = String.format("%.2f", price * count);
                        plugin.getEconomy().giveMoney(player, Double.parseDouble(money));
                        AdventureUtil.sendMessage(player, "&a你提交了 &e" + count + "&a 个 &e" + shop.displayName + "&a，获得 &e" + money + "&a 金币!");
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
            if (closeAfterSubmit) {
                player.closeInventory();
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, () -> updateInventory(view), 1L);
            }
        }
    }
}
