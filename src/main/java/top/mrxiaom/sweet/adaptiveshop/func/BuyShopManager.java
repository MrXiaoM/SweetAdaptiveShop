package top.mrxiaom.sweet.adaptiveshop.func;

import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.func.entry.BuyShop;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

@AutoRegister
public class BuyShopManager extends AbstractModule {
    File folder;
    Map<String, BuyShop> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    Map<String, Map<String, BuyShop>> byGroup = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public BuyShopManager(SweetAdaptiveShop plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        String path = config.getString("path.buy", "./buy");
        folder = path.startsWith("./") ? new File(plugin.getDataFolder(), path) : new File(path);
        if (!folder.exists()) {
            Utils.mkdirs(folder);
            plugin.saveResource("buy/wheat.yml", new File(folder, "wheat.yml"));
        }
        map.clear();
        reloadConfig(folder);
        info("加载了 " + map.size() + " 个收购商品");
        byGroup.clear();
        for (Map.Entry<String, BuyShop> entry : map.entrySet()) {
            BuyShop cfg = entry.getValue();
            Map<String, BuyShop> shopMap = byGroup.get(cfg.group);
            if (shopMap == null) {
                shopMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            }
            shopMap.put(entry.getKey(), entry.getValue());
            byGroup.put(cfg.group, shopMap);
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
            BuyShop loaded = BuyShop.load(this, file, name.substring(0, name.length() - 4));
            if (loaded == null) continue;
            map.put(loaded.id, loaded);
        }
    }

    @Nullable
    public BuyShop get(String id) {
        return map.get(id);
    }

    public static BuyShopManager inst() {
        return instanceOf(BuyShopManager.class);
    }
}
