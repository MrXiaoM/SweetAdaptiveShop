package top.mrxiaom.sweet.adaptiveshop.func;

import org.bukkit.configuration.MemoryConfiguration;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Order;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

@AutoRegister
public class OrderManager extends AbstractModule {
    File folder;
    Map<String, Order> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public OrderManager(SweetAdaptiveShop plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
    }

    protected void realReloadConfig(MemoryConfiguration config) {
        String path = config.getString("path.order", "./order");
        folder = path.startsWith("./") ? new File(plugin.getDataFolder(), path) : new File(path);
        if (!folder.exists()) {
            Utils.mkdirs(folder);
            plugin.saveResource("order/example.yml", new File(folder, "example.yml"));
        }
        map.clear();
        File[] files = folder.listFiles();
        if (files != null) for (File file : files) {
            String name = file.getName();
            if (!name.endsWith(".yml") || name.contains(" ")) continue;
            Order loaded = Order.load(this, file, name.substring(0, name.length() - 4));
            if (loaded == null) continue;
            map.put(loaded.id, loaded);
        }
        info("加载了 " + map.size() + " 个订单");
    }

    public static OrderManager inst() {
        return instanceOf(OrderManager.class);
    }
}
