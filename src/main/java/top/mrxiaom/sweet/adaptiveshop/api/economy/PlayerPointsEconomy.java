package top.mrxiaom.sweet.adaptiveshop.api.economy;

import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.api.IEconomyResolver;

import java.util.Objects;

public class PlayerPointsEconomy implements IEconomy {
    public static class Resolver implements IEconomyResolver {
        private final SweetAdaptiveShop plugin;
        public Resolver(SweetAdaptiveShop plugin) {
            this.plugin = plugin;
        }

        @Override
        public @Nullable IEconomy parse(@NotNull String str) {
            return str.equals("PlayerPoints") ? plugin.getPlayerPoints() : null;
        }
    }
    private final SweetAdaptiveShop plugin;
    private final PlayerPointsAPI api;
    public PlayerPointsEconomy(SweetAdaptiveShop plugin, PlayerPointsAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @Override
    public String id() {
        return "PlayerPoints";
    }

    @Override
    public String getName() {
        return "PlayerPoints";
    }

    @Override
    public String displayName() {
        return plugin.displayNames().getCurrencyPlayerPoints();
    }

    @Override
    public double get(OfflinePlayer player) {
        return api.look(player.getUniqueId());
    }

    @Override
    public boolean has(OfflinePlayer player, double money) {
        return get(player) >= money;
    }

    @Override
    public boolean giveMoney(OfflinePlayer player, double money) {
        return api.give(player.getUniqueId(), (int) money);
    }

    @Override
    public boolean takeMoney(OfflinePlayer player, double money) {
        return api.take(player.getUniqueId(), (int) money);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlayerPointsEconomy)) return false;
        PlayerPointsEconomy that = (PlayerPointsEconomy) o;
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id());
    }
}
