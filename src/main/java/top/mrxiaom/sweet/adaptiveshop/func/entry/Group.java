package top.mrxiaom.sweet.adaptiveshop.func.entry;

import com.google.common.collect.Lists;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.BuyShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.SellShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.BuyShop;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.IShop;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.SellShop;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.time.LocalDateTime;
import java.util.*;

public class Group {
    public final String id;
    public final String display;
    public final int dailyBuyCount, dailySellCount;
    public final Map<String, BuyShop> buyShop = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public final Map<String, SellShop> sellShop = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public final boolean enableBuy, enableSell;
    Group(String id, String display, int dailyBuyCount, int dailySellCount, boolean enableBuy, boolean enableSell) {
        this.id = id;
        this.display = display;
        this.dailyBuyCount = dailyBuyCount;
        this.dailySellCount = dailySellCount;
        this.enableBuy = enableBuy;
        this.enableSell = enableSell;
    }

    public boolean hasPermission(Permissible p) {
        return p.hasPermission("sweet.adaptive.shop.group." + id);
    }

    @Nullable
    public BuyShop randomNewBuyShop(Player player, List<PlayerItem> items) {
        return randomNewFrom(buyShop.values(), player, items);
    }

    @Nullable
    public SellShop randomNewSellShop(Player player, List<PlayerItem> items) {
        return randomNewFrom(sellShop.values(), player, items);
    }

    @Nullable
    private <T extends IShop> T randomNewFrom(Collection<T> values, Player player, List<PlayerItem> items) {
        List<String> alreadyAdded = new ArrayList<>();
        for (PlayerItem item : items) {
            alreadyAdded.add(item.getItem());
        }
        List<T> list = Lists.newArrayList(values);
        list.removeIf(it -> alreadyAdded.contains(it.getId()) || !it.hasPermission(player));
        return list.isEmpty() ? null : list.get(new Random().nextInt(list.size()));
    }

    public void refreshBuyShop(Player player) {
        if (dailyBuyCount <= 0) return;
        SweetAdaptiveShop plugin = SweetAdaptiveShop.getInstance();
        BuyShopDatabase db = plugin.getBuyShopDatabase();
        List<PlayerItem> items = db.getPlayerItems(player);
        items.removeIf(it -> it.isOutdate() || buyShop.containsKey(it.getItem()));
        LocalDateTime tomorrow = Utils.nextOutdate();
        for (int i = 0; i < dailyBuyCount; i++) {
            BuyShop shop = randomNewBuyShop(player, items);
            if (shop == null) continue;
            items.add(new PlayerItem(shop.id, tomorrow));
        }
        db.setPlayerItems(player, items);
    }

    public void refreshSellShop(Player player) {
        if (dailySellCount <= 0) return;
        SweetAdaptiveShop plugin = SweetAdaptiveShop.getInstance();
        //复制粘贴代码忘了改成sell
        SellShopDatabase db = plugin.getSellShopDatabase();
        List<PlayerItem> items = db.getPlayerItems(player);
        items.removeIf(it -> it.isOutdate() || sellShop.containsKey(it.getItem()));
        LocalDateTime tomorrow = Utils.nextOutdate();
        for (int i = 0; i < dailySellCount; i++) {
            SellShop shop = randomNewSellShop(player, items);
            if (shop == null) continue;
            items.add(new PlayerItem(shop.id, tomorrow));
        }
        db.setPlayerItems(player, items);
    }

    public static Group load(ConfigurationSection section, String id) {
        int dailyBuyCount = section.getInt(id + (section.contains(id + ".daily-buy-count") ? ".daily-buy-count" : ".daily-count"));
        int dailySellCount = section.getInt(id + (section.contains(id + ".daily-sell-count") ? ".daily-sell-count" : ".daily-count"));
        String display = section.getString(id + ".display", id);
        boolean enableBuy = section.getBoolean(id + ".buy", true);
        boolean enableSell = section.getBoolean(id + ".sell", true);
        return new Group(id, display, dailyBuyCount, dailySellCount, enableBuy, enableSell);
    }
}
