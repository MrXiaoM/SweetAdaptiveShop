package top.mrxiaom.sweet.adaptiveshop.api.economy;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency;
import su.nightexpress.excellenteconomy.api.currency.operation.NotificationTarget;
import su.nightexpress.excellenteconomy.api.currency.operation.OperationContext;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.api.IEconomyResolver;

import java.util.*;

public class ExcellentEconomyEconomy implements IEconomyWithSign, IEconomy {
    private static final String NAME = "ExcellentEconomy";
    private static final String PREFIX = NAME + ":";

    public static class Resolver implements IEconomyResolver {
        private final SweetAdaptiveShop plugin;
        public Resolver(SweetAdaptiveShop plugin) {
            this.plugin = plugin;
        }

        @Override
        public @Nullable IEconomy parse(@NotNull String str) {
            if (str.startsWith(PREFIX) && str.length() > PREFIX.length()) {
                IEconomyWithSign withSign = plugin.getExcellentEconomy();
                if (withSign != null) {
                    return withSign.of(str.substring(PREFIX.length()));
                }
            }
            return null;
        }
    }
    private final Map<String, IEconomy> caches = new HashMap<>();
    private final ExcellentEconomyAPI api;
    private final ExcellentCurrency currency;
    private final OperationContext operationContext;

    public ExcellentEconomyEconomy(ExcellentEconomyAPI api, ExcellentCurrency currency) {
        this.api = api;
        this.currency = currency;
        this.operationContext = OperationContext
                .custom("SweetAdaptiveShop")
                .silentFor(
                        NotificationTarget.USER,
                        NotificationTarget.EXECUTOR,
                        NotificationTarget.CONSOLE_LOGGER
                );
    }

    public String sign() {
        return currency.getId();
    }

    public String name() {
        return currency.getName();
    }

    @Override
    public List<String> getSigns() {
        List<String> signs = new ArrayList<>();
        for (ExcellentCurrency currency : api.getCurrencies()) {
            signs.add(currency.getId());
        }
        return signs;
    }

    @Override
    public IEconomy of(String sign) {
        IEconomy cache = caches.get(sign);
        if (cache != null) return cache;
        ExcellentCurrency currency = api.getCurrency(sign);
        if (currency == null) return null;
        IEconomy economy = new ExcellentEconomyEconomy(api, currency);
        caches.put(sign, economy);
        return economy;
    }

    @Override
    public String id() {
        return currency == null ? NAME : (PREFIX + currency.getId());
    }

    @Override
    public String getName() {
        return currency == null ? NAME : (NAME + "{" + currency.getId() + "}");
    }

    @Override
    public String displayName() {
        return currency == null ? NAME : currency.getName();
    }

    @Override
    public double get(OfflinePlayer player) {
        if (currency == null) throw new UnsupportedOperationException("");
        return api.getBalanceAsync(player.getUniqueId(), currency).join();
    }

    @Override
    public boolean has(OfflinePlayer player, double money) {
        return get(player) >= money;
    }

    @Override
    public boolean giveMoney(OfflinePlayer player, double money) {
        if (currency == null) throw new UnsupportedOperationException("");
        return api.canPerformOperations()
                && api.depositAsync(player.getUniqueId(), currency, money, operationContext).join().success();
    }

    @Override
    public boolean takeMoney(OfflinePlayer player, double money) {
        if (currency == null) throw new UnsupportedOperationException("");
        return api.canPerformOperations()
                && api.withdrawAsync(player.getUniqueId(), currency, money, operationContext).join().success();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ExcellentEconomyEconomy)) return false;
        ExcellentEconomyEconomy that = (ExcellentEconomyEconomy) o;
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id());
    }
}
