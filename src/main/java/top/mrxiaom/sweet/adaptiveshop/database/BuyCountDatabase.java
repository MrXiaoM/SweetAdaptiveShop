package top.mrxiaom.sweet.adaptiveshop.database;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.pluginbase.temporary.TemporaryInteger;
import top.mrxiaom.pluginbase.temporary.period.EveryDay;
import top.mrxiaom.pluginbase.temporary.period.Period;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractPluginHolder;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.BuyShop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class BuyCountDatabase extends AbstractPluginHolder implements IDatabase, Listener {
    private String TABLE_PLAYER_BUY_COUNT;
    private final Map<String, Map<String, TemporaryInteger>> caches = new HashMap<>();
    private Period period = EveryDay.at(LocalTime.of(4,0));
    public BuyCountDatabase(SweetAdaptiveShop plugin) {
        super(plugin);
        registerEvents();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        String key = plugin.getDBKey(player);
        caches.remove(key);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        String key = plugin.getDBKey(player);
        caches.remove(key);
    }

    public void setPeriod(@Nullable String id, @NotNull Period period) {
        this.period = period;
        for (Map.Entry<String, Map<String, TemporaryInteger>> e : caches.entrySet()) {
            for (Map.Entry<String, TemporaryInteger> entry : e.getValue().entrySet()) {
                if (id == null || entry.getKey().equalsIgnoreCase(id)) {
                    entry.getValue().setPeriod(period);
                }
            }
        }
    }

    @Override
    public void reload(Connection conn, String prefix) throws SQLException {
        TABLE_PLAYER_BUY_COUNT = (prefix + "player_buy_count").toUpperCase();
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_PLAYER_BUY_COUNT + "`(" +
                        "`name` VARCHAR(64)," +
                        "`item` VARCHAR(64)," +
                        "`count` VARCHAR(128)," +
                        "PRIMARY KEY(`name`, `item`)" +
                ");")) {
            ps.execute();
        }
    }

    @NotNull
    public TemporaryInteger getCount(@NotNull Player player, @NotNull BuyShop shop) {
        return getCount(plugin.getDBKey(player), shop, false);
    }

    @NotNull
    public TemporaryInteger getCount(@NotNull String player, @NotNull BuyShop shop, boolean update) {
        Map<String, TemporaryInteger> map = Util.getOrPut(caches, player, () -> new HashMap<>());
        TemporaryInteger cache = map.get(shop.id);
        if (cache != null) {
            if (!update) {
                return cache;
            }
        } else {
            cache = new TemporaryInteger(period, () -> 0);
        }
        try (Connection conn = plugin.getConnection()) {
            map.put(shop.id, getCount(conn, player, shop, cache));
            return cache;
        } catch (SQLException e) {
            warn(e);
        }
        return cache;
    }

    @NotNull
    public TemporaryInteger getCount(@NotNull Connection conn, @NotNull String player, @NotNull BuyShop shop, @NotNull TemporaryInteger cache) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM `" + TABLE_PLAYER_BUY_COUNT + "` WHERE `name`=? AND `item`=?;"
             )) {
            ps.setString(1, player);
            ps.setString(2, shop.id);
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    cache.deserialize(result.getString("count"));
                } else {
                    cache.applyDefaultValue();
                }
            }
        }
        return cache;
    }

    public void addCount(@NotNull Player player, @NotNull BuyShop shop, int count) {
        String key = plugin.getDBKey(player);
        TemporaryInteger cache = getCount(player, shop);
        cache.setValue(cache.getValue() + count);
        try (Connection conn = plugin.getConnection()) {
            setCount(conn, key, shop, cache);
        } catch (SQLException e) {
            warn(e);
        }
    }

    public void setCount(@NotNull Connection conn, @NotNull String player, @NotNull BuyShop shop, @NotNull TemporaryInteger cache) throws SQLException {
        String data = cache.serialize();
        boolean mySQL = plugin.options.database().isMySQL();
        String statement = mySQL
                ? ("INSERT INTO `" + TABLE_PLAYER_BUY_COUNT + "`(`name`,`item`,`count`) VALUES(?, ?, ?) on duplicate key update `count`=?;")
                : ("INSERT OR REPLACE INTO `" + TABLE_PLAYER_BUY_COUNT + "`(`name`,`item`,`count`) VALUES(?, ?, ?);");
        try (PreparedStatement ps = conn.prepareStatement(statement)) {
            ps.setString(1, player);
            ps.setString(2, shop.id);
            ps.setString(3, data);
            if (mySQL) {
                ps.setString(4, data);
            }
            ps.execute();
        }
    }
}
