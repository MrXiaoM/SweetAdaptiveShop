package top.mrxiaom.sweet.adaptiveshop.func.entry;

import com.google.common.collect.Lists;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;

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
    public BuyShop randomNewItem(List<PlayerItem> items) {
        List<String> alreadyAdded = new ArrayList<>();
        for (PlayerItem item : items) {
            alreadyAdded.add(item.getItem());
        }
        List<BuyShop> list = Lists.newArrayList(buyShop.values());
        list.removeIf(it -> alreadyAdded.contains(it.id));
        return list.isEmpty() ? null : list.get(new Random().nextInt(list.size()));
    }

    public static Group load(ConfigurationSection section, String id) {
        int dailyCount = section.getInt(id + ".daily-count");
        String display = section.getString(id + ".display", id);
        return new Group(id, display, dailyCount);
    }
}
