package top.mrxiaom.sweet.adaptiveshop.database;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.database.entry.PlayerItem;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractPluginHolder;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.SellShop;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static top.mrxiaom.sweet.adaptiveshop.utils.Utils.limit;

public class SellShopDatabase extends AbstractPluginHolder implements IDatabase, Listener {
    private String TABLE_SELL_SHOP, TABLE_SELL_SHOP_PER_PLAYER, TABLE_PLAYER_SELL_SHOP;
    public Map<String, List<PlayerItem>> itemsCache = new HashMap<>();
    public SellShopDatabase(SweetAdaptiveShop plugin) {
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
        TABLE_SELL_SHOP = (prefix + "sell_shop").toUpperCase();
        TABLE_SELL_SHOP_PER_PLAYER = (prefix + "sell_shop_per_player").toUpperCase();
        TABLE_PLAYER_SELL_SHOP = (prefix + "player_sell_shop").toUpperCase();
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_SELL_SHOP + "`(" +
                        "`item` VARCHAR(64) PRIMARY KEY," +
                        "`dynamic_value` DOUBLE," +
                        "`outdate` TIMESTAMP" +
                        ");")) {
            ps.execute();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_SELL_SHOP_PER_PLAYER + "`(" +
                        "`player` VARCHAR(64)," +
                        "`item` VARCHAR(64)," +
                        "`dynamic_value` DOUBLE," +
                        "`outdate` TIMESTAMP," +
                        "PRIMARY KEY(`player`, `item`)" +
                        ");")) {
            ps.execute();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_PLAYER_SELL_SHOP + "`(" +
                        "`name` VARCHAR(64)," +
                        "`item` VARCHAR(64)," +
                        "`outdate` TIMESTAMP," +
                        "PRIMARY KEY(`name`, `item`)" +
                        ");")) {
            ps.execute();
        }
    }

    @Nullable
    public Double getDynamicValue(SellShop item, @Nullable Player player) {
        if (item == null || item.dynamicValuePerPlayer && player == null) {
            return null;
        }
        try (Connection conn = plugin.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(item.dynamicValuePerPlayer ?
                    ("SELECT * FROM `" + TABLE_SELL_SHOP_PER_PLAYER + "` WHERE `player`=? AND `item`=?") :
                    ("SELECT * FROM `" + TABLE_SELL_SHOP + "` WHERE `item`=?;")
            )) {
                if (item.dynamicValuePerPlayer) {
                    ps.setString(1, plugin.getDBKey(player));
                    ps.setString(2, item.id);
                } else {
                    ps.setString(1, item.id);
                }
                try (ResultSet resultSet = ps.executeQuery()) {
                    if (resultSet.next()) {
                        double dynamicValue = resultSet.getDouble("dynamic_value");
                        Timestamp outdate = resultSet.getTimestamp("outdate");
                        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                        if (now.after(outdate)) { // 动态值过期之后
                            // 按配置策略，重置或恢复动态值
                            double reset = item.recoverDynamicValue(dynamicValue);
                            setDynamicValue(conn, false, item, player, reset);
                            return reset;
                        }
                        return dynamicValue;
                    }
                }
                return 0.0;
            }
        } catch (SQLException e) {
            warn(e);
        }
        return null;
    }

    private void setDynamicValue(Connection conn, boolean insert, SellShop item, Player player, double value) throws SQLException {
        double finalValue;
        if (item.dynamicValueMaximum > 0) { // 启用限制时，限制为 [0, maximum]
            finalValue = limit(value, 0, item.dynamicValueMaximum);
        } else { // 未启用限制时，限制为 [0, +∞)
            finalValue = Math.max(0, value);
        }
        LocalDateTime nextOutdateTime = item.routine.nextOutdate();
        if (insert) {
            try (PreparedStatement ps1 = conn.prepareStatement(item.dynamicValuePerPlayer ?
                    ("INSERT INTO `" + TABLE_SELL_SHOP_PER_PLAYER + "`(`player`,`item`,`dynamic_value`,`outdate`) VALUES(?,?,?,?);") :
                    ("INSERT INTO `" + TABLE_SELL_SHOP + "`(`item`,`dynamic_value`,`outdate`) VALUES(?,?,?);")
            )) {
                if (item.dynamicValuePerPlayer) {
                    ps1.setString(1, plugin.getDBKey(player));
                    ps1.setString(2, item.id);
                    ps1.setDouble(3, Double.parseDouble(String.format("%.2f", finalValue)));
                    ps1.setTimestamp(4, Timestamp.valueOf(nextOutdateTime));
                } else {
                    ps1.setString(1, item.id);
                    ps1.setDouble(2, Double.parseDouble(String.format("%.2f", finalValue)));
                    ps1.setTimestamp(3, Timestamp.valueOf(nextOutdateTime));
                }
                ps1.execute();
            }
        } else {
            try (PreparedStatement ps1 = conn.prepareStatement(item.dynamicValuePerPlayer ?
                    ("UPDATE `" + TABLE_SELL_SHOP_PER_PLAYER + "` SET `dynamic_value`=?, `outdate`=? WHERE `player`=? AND `item`=?;") :
                    ("UPDATE `" + TABLE_SELL_SHOP + "` SET `dynamic_value`=?, `outdate`=? WHERE `item`=?;")
            )) {
                if (item.dynamicValuePerPlayer) {
                    ps1.setDouble(1, Double.parseDouble(String.format("%.2f", finalValue)));
                    ps1.setTimestamp(2, Timestamp.valueOf(nextOutdateTime));
                    ps1.setString(3, plugin.getDBKey(player));
                    ps1.setString(4, item.id);
                } else {
                    ps1.setDouble(1, Double.parseDouble(String.format("%.2f", finalValue)));
                    ps1.setTimestamp(2, Timestamp.valueOf(nextOutdateTime));
                    ps1.setString(3, item.id);
                }
                ps1.execute();
            }
        }
    }

    public void addDynamicValue(SellShop item, @NotNull Player player, double value) {
        try (Connection conn = plugin.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(item.dynamicValuePerPlayer ?
                    ("SELECT * FROM `" + TABLE_SELL_SHOP_PER_PLAYER + "` WHERE `player`=? AND `item`=?;") :
                    ("SELECT * FROM `" + TABLE_SELL_SHOP + "` WHERE `item`=?;")
            )) {
                if (item.dynamicValuePerPlayer) {
                    ps.setString(1, plugin.getDBKey(player));
                    ps.setString(2, item.id);
                } else {
                    ps.setString(1, item.id);
                }
                try (ResultSet resultSet = ps.executeQuery()) {
                    if (!resultSet.next()) { // 数据库中没有记录时，记为 0，加上 value
                        setDynamicValue(conn, true, item, player, value);
                        return;
                    }
                    double dynamicValue = resultSet.getDouble("dynamic_value");
                    Timestamp outdate = resultSet.getTimestamp("outdate");
                    Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                    double newValue;
                    if (now.after(outdate)) { // 动态值过期之后
                        // 按配置策略，重置或恢复动态值
                        double reset = item.recoverDynamicValue(dynamicValue);
                        newValue = reset + value;
                    } else {
                        // 未过期则增加动态值
                        newValue = dynamicValue + value;
                    }
                    setDynamicValue(conn, false, item, player, newValue);
                }
            }
        } catch (SQLException e) {
            warn(e);
        }
    }

    @NotNull
    public List<PlayerItem> getPlayerItems(Player player) {
        String id = plugin.getDBKey(player);
        return getPlayerItems(id);
    }

    @NotNull
    public List<PlayerItem> getPlayerItems(String player) {
        List<PlayerItem> cache = itemsCache.get(player);
        if (cache != null) return cache;
        try (Connection conn = plugin.getConnection()) {
            List<PlayerItem> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * from `" + TABLE_PLAYER_SELL_SHOP + "` WHERE `name`=?;"
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
        return new ArrayList<>();
    }

    public void setPlayerItems(Player player, List<PlayerItem> list) {
        String id = plugin.getDBKey(player);
        setPlayerItems(id, list);
    }
    public void setPlayerItems(String player, List<PlayerItem> list) {
        try (Connection conn = plugin.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM `" + TABLE_PLAYER_SELL_SHOP + "` WHERE `name`=?;"
            )) {
                ps.setString(1, player);
                ps.execute();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `" + TABLE_PLAYER_SELL_SHOP + "`(`name`,`item`,`outdate`) VALUES (?,?,?);"
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
