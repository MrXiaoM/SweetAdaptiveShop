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
import top.mrxiaom.pluginbase.temporary.TemporaryInteger;
import top.mrxiaom.pluginbase.utils.*;
import top.mrxiaom.sweet.adaptiveshop.Messages;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractGuiModule;
import top.mrxiaom.sweet.adaptiveshop.func.BuyShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.BuyShop;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Group;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    List<String> buyBypassLore;
    @Override
    protected void loadMainIcon(ConfigurationSection section, String id, LoadedIcon loadedIcon) {
        switch (id) {
            case "物":
                buySlot = loadedIcon;
                buyOne = section.getString(id + ".operations.one");
                buyStack = section.getString(id + ".operations.stack");
                buyAll = section.getString(id + ".operations.all");
                buyBypassLore = section.getStringList(id + ".lore-bypass");
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
                BuyShop shop = gui.items.get(i).getKey();
                boolean bypass = shop.hasBypass(player);
                double dynamic;
                if (bypass) {
                    dynamic = 0;
                } else {
                    Double dyn = plugin.getBuyShopDatabase().getDynamicValue(shop, player);
                    dynamic = dyn == null ? 0.0 : dyn;
                }
                boolean noCut = shop.dynamicValueMaximum == 0 || !shop.dynamicValueCutWhenMaximum;
                int count = shop.getCount(player);
                double price = bypass ? shop.priceBase : shop.getPrice(dynamic);
                TemporaryInteger buyCount = plugin.getBuyCountDatabase().getCount(player, shop);

                String priceString = String.format("%.2f", price).replace(".00", "");
                String dynamicDisplay = bypass ? "" : shop.getDisplayDynamic(dynamic);
                String dynamicPlaceholder = bypass ? "" : shop.getDynamicValuePlaceholder(dynamic);
                String limitation = shop.dynamicValueLimitationPlayer > 0
                        ? Messages.gui__limitation__format.str(
                                Pair.of("%current%", buyCount.getValue()),
                                Pair.of("%max%", shop.dynamicValueLimitationPlayer))
                        : Messages.gui__limitation__infinite.str();

                ItemStack item = shop.displayItem.clone();
                String displayName = buySlot.display.replace("%name%", shop.displayName);
                List<String> lore = new ArrayList<>();
                List<String> loreTemplate = bypass ? buyBypassLore : buySlot.lore;
                ListPair<String, Object> replacements = new ListPair<>();
                replacements.add("%price%", priceString);
                replacements.add("%dynamic%", dynamicDisplay);
                replacements.add("%dynamic_placeholder%", dynamicPlaceholder);
                replacements.add("%limitation%", limitation);
                for (String s : loreTemplate) {
                    if (s.equals("description")) {
                        lore.addAll(ItemStackUtil.getItemLore(shop.displayItem));
                        continue;
                    }
                    if (s.equals("footer")) {
                        lore.addAll(shop.footer);
                        continue;
                    }
                    if (s.equals("operation")) {
                        if (count >= 1) {
                            if (noCut || dynamic + shop.dynamicValueAdd <= shop.dynamicValueMaximum) {
                                lore.add(buyOne.replace("%price%", priceString));
                            }
                        }
                        int stackSize = item.getType().getMaxStackSize();
                        if (count >= stackSize) {
                            if (noCut || dynamic + shop.dynamicValueAdd * stackSize <= shop.dynamicValueMaximum) {
                                String priceStr = String.format("%.2f", price * stackSize).replace(".00", "");
                                lore.add(buyStack.replace("%price%", priceStr)
                                        .replace("%count%", String.valueOf(stackSize)));
                            }
                        }
                        if (count >= 1) {
                            if (noCut || dynamic + shop.dynamicValueAdd * count <= shop.dynamicValueMaximum) {
                                // 个人感觉按动态值上限算可以卖的总数量很麻烦，容易出BUG，就不写了
                                String priceStr = String.format("%.2f", price * count).replace(".00", "");
                                lore.add(buyAll.replace("%price%", priceStr)
                                        .replace("%count%", String.valueOf(count)));
                            }
                        }
                        continue;
                    }
                    lore.add(Pair.replace(s, replacements));
                }
                AdventureItemStack.setItemDisplayName(item, PAPI.setPlaceholders(player, displayName));
                AdventureItemStack.setItemLoreMiniMessage(item, PAPI.setPlaceholders(player, lore));
                if (!buySlot.nbtStrings.isEmpty() || !buySlot.nbtInts.isEmpty()) {
                    NBT.modify(item, nbt -> {
                        for (Map.Entry<String, String> entry : buySlot.nbtStrings.entrySet()) {
                            String value = PAPI.setPlaceholders(player, entry.getValue());
                            nbt.setString(entry.getKey(), value);
                        }
                        for (Map.Entry<String, String> entry : buySlot.nbtInts.entrySet()) {
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

    public static GuiBuyShop inst() {
        return instanceOf(GuiBuyShop.class);
    }

    public static Impl create(@NotNull Player player, @NotNull Group group) {
        GuiBuyShop self = inst();
        return self.new Impl(player, self.guiTitle, self.guiInventory, group);
    }

    public class Impl extends Gui {
        Group group;
        List<Pair<BuyShop, PlayerItem>> items;
        protected Impl(@NotNull Player player, String title, char[] inventory, @NotNull Group group) {
            super(player, PAPI.setPlaceholders(player, title.replace("%type%", group.display)), inventory);
            this.group = group;
            this.items = BuyShopManager.inst().getPlayerItems(player, group.id);
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
                        Messages.refresh__buy__not_enough.tm(player);
                        return;
                    }
                    group.refreshBuyShop(player);
                    Messages.refresh__buy__success.tm(player, group.display);
                    this.items = BuyShopManager.inst().getPlayerItems(player, group.id);
                    open();
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
                            Messages.gui__buy__outdate.tm(player);
                            return;
                        }
                        if (count < 1) {
                            Messages.gui__buy__not_enough.tm(player);
                            return;
                        }
                        if (shop.dynamicValueLimitationPlayer > 0) {
                            TemporaryInteger buyCount = plugin.getBuyCountDatabase().getCount(player, shop);
                            if (buyCount.getValue() + 1 > shop.dynamicValueLimitationPlayer) {
                                Messages.gui__limitation__reach_tips.tm(player);
                                return;
                            }
                        }
                        Double dyn = plugin.getBuyShopDatabase().getDynamicValue(shop, player);
                        double dynamic = dyn == null ? 0.0 : dyn;
                        if (shop.dynamicValueMaximum > 0 && shop.dynamicValueCutWhenMaximum) {
                            double add = shop.dynamicValueAdd;
                            if (dynamic + add > shop.dynamicValueMaximum) {
                                Messages.gui__buy__maximum.tm(player);
                                return;
                            }
                        }
                        shop.take(player, 1);
                        double price;
                        if (shop.hasBypass(player)) {
                            price = shop.priceBase;
                        } else {
                            price = shop.getPrice(dynamic);
                        }
                        plugin.getEconomy().giveMoney(player, price);
                        String money = String.format("%.2f", price).replace(".00", "");
                        Messages.gui__buy__success.tm(player, 1, shop.displayName, money);
                        postSubmit(view);
                        return;
                    }
                    if (click.equals(ClickType.RIGHT)) { // 提交1组
                        if (pair.getValue().isOutdate()) {
                            Messages.gui__buy__outdate.tm(player);
                            return;
                        }
                        int stackSize = shop.displayItem.getType().getMaxStackSize();
                        if (count < stackSize) {
                            Messages.gui__buy__not_enough.tm(player);
                            return;
                        }
                        Double dyn = plugin.getBuyShopDatabase().getDynamicValue(shop, player);
                        double dynamic = dyn == null ? 0.0 : dyn;
                        if (shop.dynamicValueMaximum > 0 && shop.dynamicValueCutWhenMaximum) {
                            double add = shop.dynamicValueAdd * stackSize;
                            if (dynamic + add > shop.dynamicValueMaximum) {
                                Messages.gui__buy__maximum.tm(player);
                                return;
                            }
                        }
                        shop.take(player, stackSize);
                        double price;
                        if (shop.hasBypass(player)) {
                            price = shop.priceBase;
                        } else {
                            price = shop.getPrice(dynamic);
                        }
                        String money = String.format("%.2f", price * stackSize).replace(".00", "");
                        plugin.getEconomy().giveMoney(player, Double.parseDouble(money));
                        Messages.gui__buy__success.tm(player, stackSize, shop.displayName, money);
                        postSubmit(view);
                        return;
                    }
                    if (click.equals(ClickType.SHIFT_LEFT)) { // 提交全部
                        if (pair.getValue().isOutdate()) {
                            Messages.gui__buy__outdate.tm(player);
                            return;
                        }
                        if (count < 1) {
                            Messages.gui__buy__not_enough.tm(player);
                            return;
                        }
                        Double dyn = plugin.getBuyShopDatabase().getDynamicValue(shop, player);
                        double dynamic = dyn == null ? 0.0 : dyn;
                        if (shop.dynamicValueMaximum > 0 && shop.dynamicValueCutWhenMaximum) {
                            double add = shop.dynamicValueAdd * count;
                            if (dynamic + add > shop.dynamicValueMaximum) {
                                Messages.gui__buy__maximum.tm(player);
                                return;
                            }
                        }
                        shop.take(player, count);
                        double price;
                        if (shop.hasBypass(player)) {
                            price = shop.priceBase;
                        } else {
                            price = shop.getPrice(dynamic);
                        }
                        String money = String.format("%.2f", price * count).replace(".00", "");
                        plugin.getEconomy().giveMoney(player, Double.parseDouble(money));
                        Messages.gui__buy__success.tm(player, count, shop.displayName, money);
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
