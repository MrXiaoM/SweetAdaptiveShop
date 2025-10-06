package top.mrxiaom.sweet.adaptiveshop.func.config.customgui;

import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.temporary.TemporaryInteger;
import top.mrxiaom.pluginbase.utils.*;
import top.mrxiaom.sweet.adaptiveshop.Messages;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.func.config.BuyShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.config.CustomGuiManager;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.BuyShop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopIconBuy extends ShopIcon {
    private final BuyShop shop;
    private final LoadedIcon buySlot;
    private final String buyOne, buyStack, buyAll;
    private final List<String> buyBypassLore;
    public ShopIconBuy(String itemId, BuyShop shop, ConfigurationSection config) {
        super("buy", itemId);
        this.shop = shop;
        this.buySlot = LoadedIcon.load(config);
        this.buyOne = config.getString("operations.one");
        this.buyStack = config.getString("operations.stack");
        this.buyAll = config.getString("operations.all");
        this.buyBypassLore = config.getStringList("lore-bypass");
    }

    @Override
    public ItemStack generateIcon(CustomGuiManager.Impl gui, Player player) {
        return generateIcon(gui.manager().plugin, shop, player, buySlot, buyBypassLore, buyOne, buyStack, buyAll);
    }

    public static ItemStack generateIcon(SweetAdaptiveShop plugin, BuyShop shop, Player player, LoadedIcon buySlot, List<String> buyBypassLore, String buyOne, String buyStack, String buyAll) {
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
        double price = bypass ? shop.priceBase : shop.getPrice(player, dynamic);

        String priceString = String.format("%.2f", price).replace(".00", "");
        String dynamicDisplay = bypass ? "" : shop.getDisplayDynamic(player, dynamic);
        String dynamicPlaceholder = bypass ? "" : shop.getDynamicValuePlaceholder(dynamic);
        String limitation;
        int maxLimit;
        if (shop.dynamicValueLimitationPlayer > 0) {
            TemporaryInteger buyCount = plugin.getBuyCountDatabase().getCount(player, shop);
            limitation = Messages.gui__limitation__format.str(
                    Pair.of("%current%", buyCount.getValue()),
                    Pair.of("%max%", shop.dynamicValueLimitationPlayer));
            maxLimit = Math.max(0, shop.dynamicValueLimitationPlayer - buyCount.getValue());
        } else {
            limitation = Messages.gui__limitation__infinite.str();
            maxLimit = Integer.MAX_VALUE;
        }
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
                if (maxLimit == 0) continue;
                if (count >= 1) {
                    if (noCut || dynamic + shop.dynamicValueAdd <= shop.dynamicValueMaximum) {
                        lore.add(buyOne.replace("%price%", priceString));
                    }
                }
                int stackSize = item.getType().getMaxStackSize();
                double showprice;
                if (count >= stackSize && maxLimit >= stackSize) {
                    if (noCut || dynamic + shop.dynamicValueAdd * stackSize <= shop.dynamicValueMaximum) {
                        // 预先计算出售多个物品按照动态价格累加的总价
                        showprice=0;
                        for(int i=0;i<stackSize;i++){
                            showprice += shop.getPrice(player, Math.min(dynamic+shop.dynamicValueAdd*i,shop.dynamicValueMaximum));
                        }
                        String priceStr = String.format("%.2f", showprice).replace(".00", "");
                        lore.add(buyStack.replace("%price%", priceStr)
                                .replace("%count%", String.valueOf(stackSize)));
                    }
                }
                if (count >= 1) {
                    if (noCut || dynamic + shop.dynamicValueAdd * count <= shop.dynamicValueMaximum) {
                        // 个人感觉按动态值上限算可以卖的总数量很麻烦，容易出BUG，就不写了
                        // 预先计算出售多个物品按照动态价格累加的总价
                        showprice=0;
                        for(int i=0;i<count;i++){
                            showprice += shop.getPrice(player, Math.min(dynamic+shop.dynamicValueAdd*i,shop.dynamicValueMaximum));
                        }
                        String priceStr = String.format("%.2f", showprice).replace(".00", "");
                        lore.add(buyAll.replace("%price%", priceStr)
                                .replace("%count%", String.valueOf(Math.min(count, maxLimit))));
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

    @Override
    public void onClick(CustomGuiManager.Impl gui, Player player, ClickType click) {
        if (click(gui.manager().plugin, click, shop, null, player)) {
            gui.postSubmit();
        }
    }

    public static boolean click(SweetAdaptiveShop plugin, ClickType click, BuyShop shop, @Nullable PlayerItem playerItem, Player player) {
        int count = shop.getCount(player);
        int maxLimit;
        if (shop.dynamicValueLimitationPlayer > 0) {
            TemporaryInteger buyCount = plugin.getBuyCountDatabase().getCount(player, shop);
            maxLimit = Math.max(0, shop.dynamicValueLimitationPlayer - buyCount.getValue());
        } else {
            maxLimit = Integer.MAX_VALUE;
        }
        if (click.equals(ClickType.LEFT)) { // 提交1个
            if (maxLimit >= 1) {
                return doBuy(plugin, player, shop, playerItem, count, 1);
            }
            return false;
        }
        if (click.equals(ClickType.RIGHT)) { // 提交1组
            int stackSize = shop.displayItem.getType().getMaxStackSize();
            if (maxLimit >= stackSize) {
                return doBuy(plugin, player, shop, playerItem, count, stackSize);
            }
            return false;
        }
        if (click.equals(ClickType.SHIFT_LEFT)) { // 提交全部
            if (maxLimit > 0) {
                int countToBuy = Math.min(count, maxLimit);
                return doBuy(plugin, player, shop, playerItem, count, countToBuy);
            }
        }
        return false;
    }

    private static boolean doBuy(SweetAdaptiveShop plugin, Player player, BuyShop shop, @Nullable PlayerItem playerItem, int count, int countToBuy) {
        if (playerItem != null && playerItem.isOutdate()) {
            Messages.gui__buy__outdate.tm(player);
            return false;
        }
        if (count <= 0 || count < countToBuy) {
            Messages.gui__buy__not_enough.tm(player);
            return false;
        }
        if (shop.dynamicValueLimitationPlayer > 0) {
            TemporaryInteger buyCount = plugin.getBuyCountDatabase().getCount(player, shop);
            if (buyCount.getValue() + countToBuy > shop.dynamicValueLimitationPlayer) {
                Messages.gui__limitation__reach_tips.tm(player);
                return false;
            }
        }
        Double dyn = plugin.getBuyShopDatabase().getDynamicValue(shop, player);
        double dynamic = dyn == null ? 0.0 : dyn;
        if (shop.dynamicValueMaximum > 0 && shop.dynamicValueCutWhenMaximum) {
            double add = shop.dynamicValueAdd * countToBuy;
            if (dynamic + add > shop.dynamicValueMaximum) {
                Messages.gui__buy__maximum.tm(player);
                return false;
            }
        }
        shop.take(player, countToBuy);
        // 处理多个物品按照动态价格卖出的总价
        double price=0;
        if (shop.hasBypass(player)) {
            price = shop.priceBase * countToBuy;
        } else {
            for(int i=0;i<countToBuy;i++){
                price += shop.getPrice(player, Math.min(dynamic+shop.dynamicValueAdd*i,shop.dynamicValueMaximum));
            }
        }
        String money = String.format("%.2f", price).replace(".00", "");
        plugin.getEconomy().giveMoney(player, Double.parseDouble(money));
        Messages.gui__buy__success.tm(player, countToBuy, shop.displayName, money);
        return true;
    }

    @Nullable
    public static ShopIconBuy load(CustomGuiManager manager, String parentId, ConfigurationSection config, String itemId) {
        BuyShop shop = BuyShopManager.inst().get(itemId);
        if (shop == null) {
            manager.warn("[shop/custom/" + parentId + "] 找不到收购商品 " + itemId);
            return null;
        }
        return new ShopIconBuy(itemId, shop, config);
    }
}
