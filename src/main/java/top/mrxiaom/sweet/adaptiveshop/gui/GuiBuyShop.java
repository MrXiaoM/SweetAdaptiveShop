package top.mrxiaom.sweet.adaptiveshop.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.gui.IGui;
import top.mrxiaom.pluginbase.utils.PAPI;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.Messages;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractGuiModule;
import top.mrxiaom.sweet.adaptiveshop.func.config.BuyShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.config.customgui.ShopIconBuy;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Group;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.BuyShop;

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
                return ShopIconBuy.generateIcon(plugin, shop, player, buySlot, buyBypassLore, buyOne, buyStack, buyAll);
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

    public class Impl extends Gui implements Refreshable, InventoryHolder {
        Group group;
        List<Pair<BuyShop, PlayerItem>> items;
        private Inventory inventory;
        protected Impl(@NotNull Player player, String title, char[] inventory, @NotNull Group group) {
            super(player, PAPI.setPlaceholders(player, title.replace("%type%", group.display)), inventory);
            this.group = group;
            this.items = BuyShopManager.inst().getPlayerItems(player, group.id);
        }

        @Override
        protected Inventory create(InventoryHolder holder, int size, String title) {
            return inventory = super.create(this, size, title);
        }

        @NotNull
        @Override
        public Inventory getInventory() {
            return inventory;
        }

        @Override
        public void refreshGui() {
            updateInventory(inventory);
            Util.submitInvUpdate(player);
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
                    if (ShopIconBuy.click(plugin, click, pair.key(), pair.value(), player)) {
                        postSubmit(view);
                    }
                    return;
                }
                LoadedIcon icon = otherIcons.get(id);
                if (icon != null) {
                    plugin.getScheduler().runTask(() -> icon.click(player, click));
                }
            }
        }

        private void postSubmit(InventoryView view) {
            plugin.getScheduler().runTaskLater(() -> {
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
