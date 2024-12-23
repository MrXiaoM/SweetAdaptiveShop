package top.mrxiaom.sweet.adaptiveshop.func.entry;

import com.google.common.collect.Lists;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.BuyShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.time.LocalDateTime;
import java.util.*;

public class Group {
    public final String id;
    public final String display;
    public final int dailyCount;
    public final Map<String, BuyShop> buyShop = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    Group(String id, String display, int dailyCount) {
        this.id = id;
        this.display = display;
        this.dailyCount = dailyCount;
    }

    @Nullable
    public BuyShop randomNewItem(Player player, List<PlayerItem> items) {
        List<String> alreadyAdded = new ArrayList<>();
        for (PlayerItem item : items) {
            alreadyAdded.add(item.getItem());
        }
        List<BuyShop> list = Lists.newArrayList(buyShop.values());
        list.removeIf(it -> alreadyAdded.contains(it.id) || !it.hasPermission(player));
        return list.isEmpty() ? null : list.get(new Random().nextInt(list.size()));
    }

    public void refresh(Player player) {
        SweetAdaptiveShop plugin = SweetAdaptiveShop.getInstance();
        BuyShopDatabase db = plugin.getBuyShopDatabase();
        List<PlayerItem> items = db.getPlayerItems(player);
        if (items == null) items = new ArrayList<>();
        items.removeIf(it -> it.isOutdate() || buyShop.containsKey(it.getItem()));
        LocalDateTime tomorrow = Utils.nextOutdate();
        for (int i = 0; i < dailyCount; i++) {
            BuyShop shop = randomNewItem(player, items);
            if (shop == null) continue;
            items.add(new PlayerItem(shop.id, tomorrow));
        }
        db.setPlayerItems(player, items);
    }

    public static Group load(ConfigurationSection section, String id) {
        int dailyCount = section.getInt(id + ".daily-count");
        String display = section.getString(id + ".display", id);
        return new Group(id, display, dailyCount);
    }
}
