package top.mrxiaom.sweet.adaptiveshop.api.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.api.IEconomyResolver;

import java.util.Objects;

public class VaultEconomy implements IEconomy {
    public static class Resolver implements IEconomyResolver {
        private final SweetAdaptiveShop plugin;
        public Resolver(SweetAdaptiveShop plugin) {
            this.plugin = plugin;
        }

        @Override
        public @Nullable IEconomy parse(@NotNull String str) {
            return str.equals("Vault") ? plugin.getVault() : null;
        }
    }
    private final SweetAdaptiveShop plugin;
    private final Economy economy;
    private final String name;

    public VaultEconomy(SweetAdaptiveShop plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.name = "Vault{" + economy.getName() + "}";
    }

    @Override
    public String id() {
        return "Vault";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String displayName() {
        return plugin.displayNames().getCurrencyVault();
    }

    @Override
    public double get(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double money) {
        return economy.has(player, money);
    }

    @Override
    public boolean giveMoney(OfflinePlayer player, double money) {
        return economy.depositPlayer(player, money).transactionSuccess();
    }

    @Override
    public boolean takeMoney(OfflinePlayer player, double money) {
        return economy.withdrawPlayer(player, money).transactionSuccess();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VaultEconomy)) return false;
        VaultEconomy that = (VaultEconomy) o;
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id());
    }
}
