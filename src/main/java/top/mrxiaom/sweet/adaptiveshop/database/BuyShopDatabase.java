package top.mrxiaom.sweet.adaptiveshop.database;

import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractPluginHolder;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BuyShopDatabase extends AbstractPluginHolder implements IDatabase {
    private String TABLE_BUY_SHOP, TABLE_PLAYER_BUY_SHOP;
    public BuyShopDatabase(SweetAdaptiveShop plugin) {
        super(plugin);
    }

    @Override
    public void reload(Connection connection, String prefix) {
        TABLE_BUY_SHOP = (prefix + "buy_shop").toUpperCase();
        TABLE_PLAYER_BUY_SHOP = (prefix + "player_buy_shop").toUpperCase();
        try (Connection conn = connection) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE if NOT EXISTS `" + TABLE_BUY_SHOP + "`(" +
                            "`item` VARCHAR(64) PRIMARY KEY," +
                            "`dynamic_value` DOUBLE," +
                            "`outdate` TIMESTAMP" +
                    ");")) {
                ps.execute();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE if NOT EXISTS `" + TABLE_PLAYER_BUY_SHOP + "`(" +
                            "`name` VARCHAR(64)," +
                            "`item` VARCHAR(64)," +
                            "`outdate` TIMESTAMP," +
                            "PRIMARY KEY(`name`, `item`)" +
                    ");")) {
                ps.execute();
            }
        } catch (SQLException e) {
            warn(e);
        }
    }

    @Nullable
    public Double getDynamicValue(String item) {
        try (Connection conn = plugin.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM `" + TABLE_BUY_SHOP + "` WHERE `item`=?;"
            )) {
                ps.setString(1, item);
                try (ResultSet resultSet = ps.executeQuery()) {
                    if (resultSet.next()) {
                        Timestamp outdate = resultSet.getTimestamp("outdate");
                        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                        if (now.after(outdate)) {
                            return 0.0;
                        }
                        return resultSet.getDouble("dynamic_value");
                    }
                }
                return 0.0;
            }
        } catch (SQLException e) {
            warn(e);
        }
        return null;
    }

    public void addDynamicValue(String item, double value, LocalDateTime nextOutdateTime) {
        try (Connection conn = plugin.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM `" + TABLE_BUY_SHOP + "` WHERE `item`=?;"
            )) {
                ps.setString(1, item);
                try (ResultSet resultSet = ps.executeQuery()) {
                    if (!resultSet.next()) {
                        try (PreparedStatement ps1 = conn.prepareStatement(
                                "INSERT INTO `" + TABLE_BUY_SHOP + "`(`item`,`dynamic_value`,`outdate`) VALUES(?,?,?);"
                        )) {
                            ps1.setString(1, item);
                            ps1.setDouble(2, value);
                            ps1.setTimestamp(3, Timestamp.valueOf(nextOutdateTime));
                            ps1.execute();
                        }
                        return;
                    }
                    Timestamp outdate = resultSet.getTimestamp("outdate");
                    Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                    if (now.after(outdate)) {
                        try (PreparedStatement ps1 = conn.prepareStatement(
                                "UPDATE `" + TABLE_BUY_SHOP + "` SET `dynamic_value`=?, `outdate`=? WHERE `item`=?;"
                        )) {
                            ps1.setDouble(1, value);
                            ps1.setTimestamp(2, Timestamp.valueOf(nextOutdateTime));
                            ps1.setString(3, item);
                        }
                        return;
                    }
                    double dynamicValue = resultSet.getDouble("dynamic_value");
                    try (PreparedStatement ps1 = conn.prepareStatement(
                            "UPDATE `" + TABLE_BUY_SHOP + "` SET `dynamic_value`=? WHERE `item`=?;"
                    )) {
                        ps1.setDouble(1, dynamicValue + value);
                        ps1.setString(2, item);
                    }
                }
            }
        } catch (SQLException e) {
            warn(e);
        }
    }

    @Nullable
    public List<PlayerItem> getPlayerItems(String player) {
        try (Connection conn = plugin.getConnection()) {
            List<PlayerItem> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * from `" + TABLE_PLAYER_BUY_SHOP + "` WHERE `name`=?;"
            )) {
                ps.setString(1, player);
                try (ResultSet resultSet = ps.executeQuery()) {
                    while (resultSet.next()) {
                        String item = resultSet.getString("item");
                        Timestamp outdate = resultSet.getTimestamp("outdate");
                        list.add(new PlayerItem(item, outdate.toLocalDateTime()));
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            warn(e);
        }
        return null;
    }

    public void setPlayerItems(String player, List<PlayerItem> list) {
        try (Connection conn = plugin.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM `" + TABLE_PLAYER_BUY_SHOP + "` WHERE `name`=?;"
            )) {
                ps.setString(1, player);
                ps.execute();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `" + TABLE_PLAYER_BUY_SHOP + "`(`name`,`item`,`outdate`) VALUES (?,?,?);"
            )) {
                for (PlayerItem playerItem : list) {
                    ps.setString(1, player);
                    ps.setString(2, playerItem.getItem());
                    ps.setTimestamp(3, Timestamp.valueOf(playerItem.getOutdate()));
                    ps.addBatch();
                }
                ps.executeBatch();
                ps.clearBatch();
            }
        } catch (SQLException e) {
            warn(e);
        }
    }
}
