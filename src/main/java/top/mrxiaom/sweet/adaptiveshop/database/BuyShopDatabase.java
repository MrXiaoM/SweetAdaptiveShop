package top.mrxiaom.sweet.adaptiveshop.database;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractPluginHolder;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuyShopDatabase extends AbstractPluginHolder implements IDatabase, Listener {
    private String TABLE_BUY_SHOP, TABLE_PLAYER_BUY_SHOP;
    public Map<String, List<PlayerItem>> itemsCache = new HashMap<>();
    public BuyShopDatabase(SweetAdaptiveShop plugin) {
        super(plugin);
        registerEvents();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String id = plugin.getDBKey(e.getPlayer());
        itemsCache.remove(id);
    }

    @Override
    public void reload(Connection conn, String prefix) throws SQLException {
        TABLE_BUY_SHOP = (prefix + "buy_shop").toUpperCase();
        TABLE_PLAYER_BUY_SHOP = (prefix + "player_buy_shop").toUpperCase();
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

    private void setDynamicValue(Connection conn, boolean insert, String item, double value, LocalDateTime nextOutdateTime) throws SQLException {
        if (insert) {
            try (PreparedStatement ps1 = conn.prepareStatement(
                    "INSERT INTO `" + TABLE_BUY_SHOP + "`(`item`,`dynamic_value`,`outdate`) VALUES(?,?,?);"
            )) {
                ps1.setString(1, item);
                ps1.setDouble(2, Double.parseDouble(String.format("%.2f", value)));
                ps1.setTimestamp(3, Timestamp.valueOf(nextOutdateTime));
                ps1.execute();
            }
        } else {
            try (PreparedStatement ps1 = conn.prepareStatement(
                    "UPDATE `" + TABLE_BUY_SHOP + "` SET `dynamic_value`=?, `outdate`=? WHERE `item`=?;"
            )) {
                ps1.setDouble(1, Double.parseDouble(String.format("%.2f", value)));
                ps1.setTimestamp(2, Timestamp.valueOf(nextOutdateTime));
                ps1.setString(3, item);
                ps1.execute();
            }
        }
    }

    public void addDynamicValue(String item, double value, LocalDateTime nextOutdateTime) {
        try (Connection conn = plugin.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM `" + TABLE_BUY_SHOP + "` WHERE `item`=?;"
            )) {
                ps.setString(1, item);
                try (ResultSet resultSet = ps.executeQuery()) {
                    if (!resultSet.next()) {
                        setDynamicValue(conn, true, item, value, nextOutdateTime);
                        return;
                    }
                    Timestamp outdate = resultSet.getTimestamp("outdate");
                    Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                    if (now.after(outdate)) {
                        setDynamicValue(conn, false, item, value, nextOutdateTime);
                        return;
                    }
                    double dynamicValue = resultSet.getDouble("dynamic_value");
                    setDynamicValue(conn, false, item, dynamicValue + value, nextOutdateTime);
                }
            }
        } catch (SQLException e) {
            warn(e);
        }
    }

    @Nullable
    public List<PlayerItem> getPlayerItems(Player player) {
        String id = plugin.getDBKey(player);
        return getPlayerItems(id);
    }

    @Nullable
    public List<PlayerItem> getPlayerItems(String player) {
        List<PlayerItem> cache = itemsCache.get(player);
        if (cache != null) return cache;
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
            itemsCache.put(player, list);
            return list;
        } catch (SQLException e) {
            warn(e);
        }
        return null;
    }

    public void setPlayerItems(Player player, List<PlayerItem> list) {
        String id = plugin.getDBKey(player);
        setPlayerItems(id, list);
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
            itemsCache.put(player, list);
        } catch (SQLException e) {
            warn(e);
        }
    }
}
