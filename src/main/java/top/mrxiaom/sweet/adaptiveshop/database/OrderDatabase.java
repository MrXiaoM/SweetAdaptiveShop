package top.mrxiaom.sweet.adaptiveshop.database;

import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerOrder;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractPluginHolder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDatabase extends AbstractPluginHolder implements IDatabase {
    private String TABLE_ORDERS;
    public OrderDatabase(SweetAdaptiveShop plugin) {
        super(plugin);
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
    public List<PlayerOrder> getPlayerOrders(String player) {
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
            return list;
        } catch (SQLException e) {
            warn(e);
        }
        return null;
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
        } catch (SQLException e) {
            warn(e);
        }
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
        } catch (SQLException e) {
            warn(e);
        }
    }
}
