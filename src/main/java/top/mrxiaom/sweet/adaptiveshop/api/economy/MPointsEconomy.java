package top.mrxiaom.sweet.adaptiveshop.api.economy;

import com.google.common.collect.Lists;
import me.yic.mpoints.MPointsAPI;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.api.IEconomyResolver;
import top.mrxiaom.sweet.adaptiveshop.func.config.DisplayNames;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MPointsEconomy implements IEconomyWithSign, IEconomy {
    public static class Resolver implements IEconomyResolver {
        private final SweetAdaptiveShop plugin;
        public Resolver(SweetAdaptiveShop plugin) {
            this.plugin = plugin;
        }

        @Override
        public @Nullable IEconomy parse(@NotNull String str) {
            if (str.startsWith("MPoints:") && str.length() > 8) {
                IEconomyWithSign withSign = plugin.getMPoints();
                if (withSign != null) {
                    return withSign.of(str.substring(8));
                }
            }
            return null;
        }
    }
    private static final Map<String, IEconomy> caches = new HashMap<>();
    private final SweetAdaptiveShop plugin;
    private final MPointsAPI api;
    private final String sign;

    public MPointsEconomy(SweetAdaptiveShop plugin, MPointsAPI api, String sign) {
        this.plugin = plugin;
        this.api = api;
        this.sign = sign;
    }

    public String sign() {
        return sign;
    }

    @Override
    public List<String> getSigns() {
        return Lists.newArrayList(api.getpointslist());
    }

    @Override
    public IEconomy of(String sign) {
        IEconomy cache = caches.get(sign);
        if (cache != null) return cache;
        if (!api.getpointslist().contains(sign)) return null;
        IEconomy economy = new MPointsEconomy(plugin, api, sign);
        caches.put(sign, economy);
        return economy;
    }

    @Override
    public String id() {
        return "MPoints:" + sign;
    }

    @Override
    public String getName() {
        return sign == null ? "MPoints" : ("MPoints{" + sign + "}");
    }

    @Override
    public String displayName() {
        return sign == null ? "MPoints" : plugin.displayNames().getCurrencyMPoints(sign);
    }

    @Override
    public double get(OfflinePlayer player) {
        if (sign == null) throw new UnsupportedOperationException("");
        return api.getbalance(sign, player.getUniqueId()).doubleValue();
    }

    @Override
    public boolean has(OfflinePlayer player, double money) {
        return get(player) >= money;
    }

    @Override
    public boolean giveMoney(OfflinePlayer player, double money) {
        if (sign == null) throw new UnsupportedOperationException("");
        return api.changebalance(sign, player.getUniqueId(), player.getName(), BigDecimal.valueOf(money), true) == 0;
    }

    @Override
    public boolean takeMoney(OfflinePlayer player, double money) {
        if (sign == null) throw new UnsupportedOperationException("");
        return api.changebalance(sign, player.getUniqueId(), player.getName(), BigDecimal.valueOf(money), false) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MPointsEconomy)) return false;
        MPointsEconomy that = (MPointsEconomy) o;
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id());
    }
}
