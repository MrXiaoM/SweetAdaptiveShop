package top.mrxiaom.sweet.adaptiveshop.func.config;

import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.SellShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Group;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.SellShop;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@AutoRegister
public class SellShopManager extends AbstractModule {
    File folder;
    Map<String, SellShop> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public SellShopManager(SweetAdaptiveShop plugin) {
        super(plugin);
    }

    @Override
    public int priority() {
        return 1001;
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        String path = config.getString("path.sell", "./sell");
        folder = path.startsWith("./") ? new File(plugin.getDataFolder(), path) : new File(path);
        if (!folder.exists()) {
            Utils.mkdirs(folder);
            plugin.saveResource("sell/wheat.yml", new File(folder, "wheat.yml"));
        }
        if (!plugin.isSupportItemsAdder()) {
            reloadSellShops();
        } else {
            if (ItemsAdder.areItemsLoaded()) {
                reloadSellShops();
            }
        }
    }

    public void reloadSellShops() {
        map.clear();
        reloadConfig(folder);
        info("加载了 " + map.size() + " 个出售商品");
        for (Map.Entry<String, SellShop> entry : map.entrySet()) {
            SellShop cfg = entry.getValue();
            Group group = GroupManager.inst().get(cfg.group);
            if (group == null) {
                warn("[出售][" + cfg.id + "] 找不到分组 " + cfg.group);
                continue;
            }
            group.sellShop.put(entry.getKey(), entry.getValue());
        }
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
                warn("重名的出售配置 " + file.getAbsolutePath());
                continue;
            }
            SellShop loaded = SellShop.load(this, file, id);
            if (loaded == null) continue;
            map.put(loaded.id, loaded);
        }
    }

    @Nullable
    public SellShop get(String id) {
        return map.get(id);
    }

    /**
     * 获取玩家商品列表，并自动刷新已过期商品
     */
    public List<Pair<SellShop, PlayerItem>> getPlayerItems(Player player, @Nullable String group) {
        SellShopDatabase db = plugin.getSellShopDatabase();
        List<PlayerItem> items = db.getPlayerItems(player);
        if (items == null) items = new ArrayList<>();
        List<Pair<SellShop, PlayerItem>> list = new ArrayList<>();
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        items.removeIf(it -> {
            if (it.isOutdate()) return true;
            SellShop shop = get(it.getItem());
            if (shop == null) return true;
            list.add(Pair.of(shop, it));
            int count = counts.getOrDefault(shop.group, 0) + 1;
            counts.put(shop.group, count);
            return false;
        });
        LocalDateTime tomorrow = Utils.nextOutdate();
        boolean flag = false;
        Collection<Group> groups = GroupManager.inst().groups();
        for (Group g : groups) {
            if (g.dailySellCount <= 0) continue;
            int needs = Math.max(0, g.dailySellCount - counts.getOrDefault(g.id, 0));
            for (int i = 0; i < needs; i++) {
                SellShop shop = g.randomNewSellShop(player, items);
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

    public static SellShopManager inst() {
        return instanceOf(SellShopManager.class);
    }
}
