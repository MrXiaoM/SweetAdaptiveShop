package top.mrxiaom.sweet.adaptiveshop.database;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerOrder;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractPluginHolder;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderDatabase extends AbstractPluginHolder implements IDatabase, Listener {
    private String TABLE_ORDERS;
    public Map<String, List<PlayerOrder>> ordersCache = new HashMap<>();
    public OrderDatabase(SweetAdaptiveShop plugin) {
        super(plugin);
        registerEvents();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String id = plugin.getDBKey(e.getPlayer());
        ordersCache.remove(id);
    }

    @Override
    public void reload(Connection conn, String prefix) throws SQLException {
        TABLE_ORDERS = (prefix + "player_orders").toUpperCase();
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_ORDERS + "`(" +
                        "`name` VARCHAR(64)," +
                        "`order` VARCHAR(64)," +
                        "`has_done` INT," + // 兼容 SQLite
                        "`outdate` TIMESTAMP," +
                        "PRIMARY KEY(`name`, `order`)" +
                        ");")) {
            ps.execute();
        }
    }

    @NotNull
    public List<PlayerOrder> getPlayerOrdersCacheOrNew(Player player) {
        String id = plugin.getDBKey(player);
        List<PlayerOrder> orders = ordersCache.get(id);
        return orders != null ? orders : new ArrayList<>();
    }

    @NotNull
    public List<PlayerOrder> getPlayerOrders(Player player) {
        String id = plugin.getDBKey(player);
        return getPlayerOrders(id);
    }

    @NotNull
    public List<PlayerOrder> getPlayerOrders(String player) {
        List<PlayerOrder> cache = ordersCache.get(player);
        if (cache != null) return cache;
        try (Connection conn = plugin.getConnection()) {
            List<PlayerOrder> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * from `" + TABLE_ORDERS + "` WHERE `name`=?;"
            )) {
                ps.setString(1, player);
                try (ResultSet resultSet = ps.executeQuery()) {
                    while (resultSet.next()) {
                        String order = resultSet.getString("order");
                        int doneCount = resultSet.getInt("has_done");
                        Timestamp outdate = resultSet.getTimestamp("outdate");
                        list.add(new PlayerOrder(order, doneCount, outdate.toLocalDateTime()));
                    }
                }
            }
            ordersCache.put(player, list);
            return list;
        } catch (SQLException e) {
            warn(e);
        }
        return new ArrayList<>();
    }

    public void setPlayerOrders(Player player, List<PlayerOrder> list) {
        String id = plugin.getDBKey(player);
        setPlayerOrders(id, list);
    }

    public void setPlayerOrders(String player, List<PlayerOrder> list) {
        try (Connection conn = plugin.getConnection()) {
            // 清空玩家的所有订单
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM `" + TABLE_ORDERS + "` WHERE `name`=?;"
            )) {
                ps.setString(1, player);
                ps.execute();
            }
            // 再把订单列表写回去，虽然效率可能低了点，但是一个周期就那么几条订单，
            // 谁家好人会给玩家安排几百条订单啊，影响不大
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `" + TABLE_ORDERS + "`(`name`,`order`,`has_done`,`outdate`) VALUES (?,?,?,?);"
            )) {
                for (PlayerOrder order : list) {
                    ps.setString(1, player);
                    ps.setString(2, order.getOrder());
                    ps.setInt(3, order.getDoneCount());
                    ps.setTimestamp(4, Timestamp.valueOf(order.getOutdate()));
                    ps.addBatch();
                }
                ps.executeBatch();
                ps.clearBatch();
            }
            ordersCache.put(player, list);
        } catch (SQLException e) {
            warn(e);
        }
    }

    public void markOrderDone(Player player, String order, int doneCount) {
        String id = plugin.getDBKey(player);
        markOrderDone(id, order, doneCount);
    }

    public void markOrderDone(String player, String order, int doneCount) {
        try (Connection conn = plugin.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE `" + TABLE_ORDERS + "` SET `has_done`=? WHERE `name`=? AND `order`=?"
            )) {
                ps.setInt(1, doneCount);
                ps.setString(2, player);
                ps.setString(3, order);
                ps.execute();
            }
            List<PlayerOrder> orders = ordersCache.get(player);
            if (orders != null) for (PlayerOrder playerOrder : orders) {
                if (playerOrder.getOrder().equals(order)) {
                    playerOrder.setDoneCount(doneCount);
                }
            }
        } catch (SQLException e) {
            warn(e);
        }
    }
}
