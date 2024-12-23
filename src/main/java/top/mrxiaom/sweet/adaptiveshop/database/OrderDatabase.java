package top.mrxiaom.sweet.adaptiveshop.database;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;
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
    public void reload(Connection connection, String prefix) {
        TABLE_ORDERS = (prefix + "player_orders").toUpperCase();
        try (Connection conn = connection) {
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
        } catch (SQLException e) {
            warn(e);
        }
    }

    @Nullable
    public List<PlayerOrder> getPlayerOrders(Player player) {
        String id = plugin.getDBKey(player);
        return getPlayerOrders(id);
    }

    @Nullable
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
                        boolean hasDone = resultSet.getInt("has_done") == 1;
                        Timestamp outdate = resultSet.getTimestamp("outdate");
                        list.add(new PlayerOrder(order, hasDone, outdate.toLocalDateTime()));
                    }
                }
            }
            ordersCache.put(player, list);
            return list;
        } catch (SQLException e) {
            warn(e);
        }
        return null;
    }

    public void setPlayerOrders(Player player, List<PlayerOrder> list) {
        String id = plugin.getDBKey(player);
        setPlayerOrders(id, list);
    }

    public void setPlayerOrders(String player, List<PlayerOrder> list) {
        try (Connection conn = plugin.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM `" + TABLE_ORDERS + "` WHERE `name`=?;"
            )) {
                ps.setString(1, player);
                ps.execute();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `" + TABLE_ORDERS + "`(`name`,`order`,`has_done`,`outdate`) VALUES (?,?,?,?);"
            )) {
                for (PlayerOrder order : list) {
                    ps.setString(1, player);
                    ps.setString(2, order.getOrder());
                    ps.setInt(3, order.isHasDone() ? 1 : 0);
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

    public void markOrderDone(Player player, String order) {
        String id = plugin.getDBKey(player);
        markOrderDone(id, order);
    }

    public void markOrderDone(String player, String order) {
        try (Connection conn = plugin.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE `" + TABLE_ORDERS + "` SET `has_done`=1 WHERE `name`=? AND `order`=?"
            )) {
                ps.setString(1, player);
                ps.setString(2, order);
                ps.execute();
            }
            List<PlayerOrder> orders = ordersCache.get(player);
            for (PlayerOrder playerOrder : orders) {
                if (playerOrder.getOrder().equals(order)) {
                    playerOrder.setHasDone(true);
                }
            }
        } catch (SQLException e) {
            warn(e);
        }
    }
}
