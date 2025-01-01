package top.mrxiaom.sweet.adaptiveshop.func;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.BuyShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.func.entry.BuyShop;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Group;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static top.mrxiaom.sweet.adaptiveshop.utils.Utils.limit;

@AutoRegister
public class BuyShopManager extends AbstractModule {
    File folder;
    Map<String, BuyShop> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    Map<String, Group> groups = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public BuyShopManager(SweetAdaptiveShop plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        Utils.outdateHour = limit(config.getInt("routine-time.hour", 4), 0, 23);
        Utils.outdateMinute = limit(config.getInt("routine-time.minute", 0), 0, 59);
        Utils.outdateSecond = limit(config.getInt("routine-time.second", 0), 0, 59);

        File file = new File(plugin.getDataFolder(), "groups.yml");
        if (!file.exists()) {
            plugin.saveResource("groups.yml", file);
        }
        groups.clear();
        YamlConfiguration configGroups = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = configGroups.getConfigurationSection("groups");
        if (section != null) for (String groupName : section.getKeys(false)) {
            Group loaded = Group.load(section, groupName);
            groups.put(groupName, loaded);
        }

        String path = config.getString("path.buy", "./buy");
        folder = path.startsWith("./") ? new File(plugin.getDataFolder(), path) : new File(path);
        if (!folder.exists()) {
            Utils.mkdirs(folder);
            plugin.saveResource("buy/wheat.yml", new File(folder, "wheat.yml"));
        }
        map.clear();
        reloadConfig(folder);
        info("加载了 " + map.size() + " 个收购商品");
        for (Map.Entry<String, BuyShop> entry : map.entrySet()) {
            BuyShop cfg = entry.getValue();
            Group group = groups.get(cfg.group);
            if (group == null) {
                warn("[收购][" + cfg.id + "] 找不到分组 " + cfg.group);
                continue;
            }
            group.buyShop.put(entry.getKey(), entry.getValue());
        }
        OrderManager.inst().realReloadConfig(config); // 确保加载顺序正确
    }

    private void reloadConfig(File folder) {
        File[] files = folder.listFiles();
        if (files != null) for (File file : files) {
            if (file.isDirectory()) {
                reloadConfig(file);
                continue;
            }
            String name = file.getName();
            if (!name.endsWith(".yml") || name.contains(" ")) continue;
            String id = name.substring(0, name.length() - 4);
            if (map.containsKey(id)) {
                warn("重名的收购配置 " + file.getAbsolutePath());
                continue;
            }
            BuyShop loaded = BuyShop.load(this, file, id);
            if (loaded == null) continue;
            map.put(loaded.id, loaded);
        }
    }

    @Nullable
    public BuyShop get(String id) {
        return map.get(id);
    }

    @Nullable
    public Group getGroup(String group){
        return groups.get(group);
    }

    /**
     * 获取玩家商品列表，并自动刷新已过期商品
     */
    public List<Pair<BuyShop, PlayerItem>> getPlayerItems(Player player, @Nullable String group) {
        BuyShopDatabase db = plugin.getBuyShopDatabase();
        List<PlayerItem> items = db.getPlayerItems(player);
        if (items == null) items = new ArrayList<>();
        List<Pair<BuyShop, PlayerItem>> list = new ArrayList<>();
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        items.removeIf(it -> {
            if (it.isOutdate()) return true;
            BuyShop shop = get(it.getItem());
            if (shop == null) return true;
            list.add(Pair.of(shop, it));
            int count = counts.getOrDefault(shop.group, 0) + 1;
            counts.put(shop.group, count);
            return false;
        });
        LocalDateTime tomorrow = Utils.nextOutdate();
        boolean flag = false;
        for (Group g : groups.values()) {
            int needs = Math.max(0, g.dailyCount - counts.getOrDefault(g.id, 0));
            for (int i = 0; i < needs; i++) {
                BuyShop shop = g.randomNewItem(player, items);
                if (shop == null) break;
                PlayerItem entry = new PlayerItem(shop.id, tomorrow);
                list.add(Pair.of(shop, entry));
                items.add(entry);
                flag = true;
            }
        }
        if (flag) db.setPlayerItems(player, items);
        if (group != null) {
            list.removeIf(it -> !it.getKey().group.equals(group));
        }
        return list;
    }

    public static BuyShopManager inst() {
        return instanceOf(BuyShopManager.class);
    }
}
