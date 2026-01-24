package top.mrxiaom.sweet.adaptiveshop.func.config.customgui;

import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.Messages;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.func.config.CustomGuiManager;
import top.mrxiaom.sweet.adaptiveshop.func.config.SellShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.SellShop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopIconSell extends ShopIcon {
    private final SellShop shop;
    private final LoadedIcon sellSlot;
    private final String sellOne, sellStack;
    private final List<String> sellBypassLore;
    public ShopIconSell(String itemId, SellShop shop, ConfigurationSection config) {
        super("sell", itemId);
        this.shop = shop;
        this.sellSlot = LoadedIcon.load(config);
        this.sellOne = config.getString("operations.one");
        this.sellStack = config.getString("operations.stack");
        this.sellBypassLore = config.getStringList("lore-bypass");
    }

    @Override
    public ItemStack generateIcon(CustomGuiManager.Impl gui, Player player) {
        return generateIcon(gui.manager().plugin, shop, player, sellSlot, sellBypassLore, sellOne, sellStack);
    }

    public static ItemStack generateIcon(SweetAdaptiveShop plugin, SellShop shop, Player player, LoadedIcon sellSlot, List<String> sellBypassLore, String sellOne, String sellStack) {
        boolean bypass = shop.hasBypass(player);
        double dynamic;
        if (bypass) {
            dynamic = 0;
        } else {
            Double dyn = plugin.getSellShopDatabase().getDynamicValue(shop, player);
            dynamic = dyn == null ? 0.0 : dyn;
        }
        boolean noCut = shop.dynamicValueMaximum == 0 || !shop.dynamicValueCutWhenMaximum;
        double currentPrice = bypass ? shop.priceBase : shop.getPrice(player, dynamic);
        int count = (int) Math.floor(shop.currency.get(player) / currentPrice);
        String currentPriceString = plugin.displayNames().formatMoney(currentPrice, shop.currency);
        String dynamicDisplay = bypass ? "" : shop.getDisplayDynamic(player, dynamic);
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
                    // 购买1个
                    if (noCut || dynamic + shop.dynamicValueAdd <= shop.dynamicValueMaximum) {
                        lore.add(sellOne.replace("%price%", currentPriceString));
                    }
                }
                int stackSize = item.getType().getMaxStackSize();
                if (count >= stackSize) {
                    // 购买1组
                    if (noCut || dynamic + shop.dynamicValueAdd * stackSize <= shop.dynamicValueMaximum) {
                        double showPrice = calculatePrice(shop, player, dynamic, stackSize);
                        lore.add(sellStack.replace("%price%", plugin.displayNames().formatMoney(showPrice, shop.currency))
                                .replace("%count%", String.valueOf(stackSize)));
                    }
                }
                continue;
            }
            lore.add(s.replace("%price%", currentPriceString)
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

    @Override
    public void onClick(CustomGuiManager.Impl gui, Player player, ClickType click) {
        if (click(gui.manager().plugin, click, shop, null, player)) {
            gui.postSubmit();
        }
    }

    public static boolean click(SweetAdaptiveShop plugin, ClickType click, SellShop shop, @Nullable PlayerItem playerItem, Player player) {
        if (playerItem != null && playerItem.isOutdate()) {
            Messages.gui__sell__outdate.tm(player);
            return false;
        }
        if (click.equals(ClickType.LEFT)) { // 购买1个
            Double dyn = plugin.getSellShopDatabase().getDynamicValue(shop, player);
            double dynamic = dyn == null ? 0.0 : dyn;
            if (shop.dynamicValueMaximum > 0 && shop.dynamicValueCutWhenMaximum) {
                double add = shop.dynamicValueAdd;
                if (dynamic + add > shop.dynamicValueMaximum) {
                    Messages.gui__sell__maximum.tm(player);
                    return false;
                }
            }
            double price = calculatePrice(shop, player, dynamic, 1);
            String money = plugin.displayNames().formatMoney(price, shop.currency);
            if (!shop.currency.takeMoney(player, price)) {
                Messages.gui__sell__no_money.tm(player);
                return false;
            }
            shop.give(player, 1);
            Messages.gui__sell__success_message.tm(player,
                    Pair.of("%money%", money),
                    Pair.of("%amount%", 1),
                    Pair.of("%item%", shop.displayName));
            return true;
        }
        if (click.equals(ClickType.RIGHT)) { // 购买1组
            int stackSize = shop.displayItem.getType().getMaxStackSize();
            Double dyn = plugin.getSellShopDatabase().getDynamicValue(shop, player);
            double dynamic = dyn == null ? 0.0 : dyn;
            if (shop.dynamicValueMaximum > 0 && shop.dynamicValueCutWhenMaximum) {
                double add = shop.dynamicValueAdd * stackSize;
                if (shop.hasReachDynamicValueMaximum(dynamic + add)) {
                    Messages.gui__sell__maximum.tm(player);
                    return false;
                }
            }
            double price = calculatePrice(shop, player, dynamic, stackSize);
            String money = plugin.displayNames().formatMoney(price, shop.currency);
            double total = Double.parseDouble(money);
            if (!shop.currency.takeMoney(player, total)) {
                Messages.gui__sell__no_money.tm(player);
                return false;
            }
            shop.give(player, stackSize);
            Messages.gui__sell__success_message.tm(player,
                    Pair.of("%money%", money),
                    Pair.of("%amount%", stackSize),
                    Pair.of("%item%", shop.displayName));
            return true;
        }
        return false;
    }

    /**
     * 预先计算购买多个物品按照动态价格累加的总价
     */
    private static double calculatePrice(SellShop shop, Player player, double dynamic, int stackSize) {
        if (shop.hasBypass(player)) {
            return shop.priceBase * stackSize;
        }
        double price = 0;
        for (int i = 0; i < stackSize; i++) {
            double newDynamic = shop.handleDynamicValueMaximum(dynamic + shop.dynamicValueAdd * i);
            price += shop.getPrice(player, newDynamic);
        }
        return price;
    }

    @Nullable
    public static ShopIconSell load(CustomGuiManager manager, String parentId, ConfigurationSection config, String itemId) {
        SellShop shop = SellShopManager.inst().get(itemId);
        if (shop == null) {
            manager.warn("[shop/custom/" + parentId + "] 找不到出售商品 " + itemId);
            return null;
        }
        return new ShopIconSell(itemId, shop, config);
    }
}
