package top.mrxiaom.sweet.adaptiveshop.func.config;

import com.google.common.collect.Lists;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.OrderDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerOrder;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Order;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@AutoRegister
public class OrderManager extends AbstractModule {
    File folder;
    Map<String, Order> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    int ordersCount;
    public OrderManager(SweetAdaptiveShop plugin) {
        super(plugin);
    }

    @Override
    public int priority() {
        return 1002;
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        ordersCount = config.getInt("orders-count");

        if (!plugin.isSupportItemsAdder()) {
            reloadOrders(config);
        } else {
            if (ItemsAdder.areItemsLoaded()) {
                reloadOrders(config);
            }
        }
    }

    public void reloadOrders(MemoryConfiguration config) {
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


    @Nullable
    public Order randomNewOrder(Player player, List<PlayerOrder> orders) {
        List<String> alreadyAdded = new ArrayList<>();
        for (PlayerOrder order : orders) {
            alreadyAdded.add(order.getOrder());
        }
        List<Order> list = Lists.newArrayList(map.values());
        list.removeIf(it -> alreadyAdded.contains(it.id) || !it.hasPermission(player));
        return list.isEmpty() ? null : list.get(new Random().nextInt(list.size()));
    }

    @Nullable
    public Order get(String id) {
        return map.get(id);
    }

    /**
     * 获取玩家订单列表，并自动刷新已过期商品
     */
    public List<Pair<Order, PlayerOrder>> getPlayerOrders(Player player) {
        OrderDatabase db = plugin.getOrderDatabase();
        List<PlayerOrder> orders = db.getPlayerOrders(player);
        List<Pair<Order, PlayerOrder>> list = new ArrayList<>();
        // 移除已过期订单
        orders.removeIf(it -> {
            if (it.isOutdate()) return true;
            Order order = map.get(it.getOrder());
            if (order == null) return true;
            list.add(Pair.of(order, it));
            return false;
        });
        // 在订单数量不足时，补充订单到列表
        int needed = Math.max(0, ordersCount - orders.size());
        LocalDateTime tomorrow = Utils.nextOutdate();
        boolean flag = false;
        for (int i = 0; i < needed; i++) {
            Order order = randomNewOrder(player, orders);
            if (order == null) break;
            PlayerOrder entry = new PlayerOrder(order.id, 0, tomorrow);
            list.add(Pair.of(order, entry));
            orders.add(entry);
            flag = true;
        }
        if (flag) db.setPlayerOrders(player, orders);
        return list;
    }

    public void refresh(Player player) {
        OrderDatabase db = plugin.getOrderDatabase();
        List<PlayerOrder> orders = db.getPlayerOrdersCacheOrNew(player);
        orders.clear();
        LocalDateTime tomorrow = Utils.nextOutdate();
        for (int i = 0; i < ordersCount; i++) {
            Order order = randomNewOrder(player, orders);
            if (order == null) break;
            PlayerOrder entry = new PlayerOrder(order.id, 0, tomorrow);
            orders.add(entry);
        }
        db.setPlayerOrders(player, orders);
    }

    public static OrderManager inst() {
        return instanceOf(OrderManager.class);
    }
}
