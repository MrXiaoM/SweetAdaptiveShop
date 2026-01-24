package top.mrxiaom.sweet.adaptiveshop.func.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.api.economy.IEconomy;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;

import java.util.HashMap;
import java.util.Map;

@AutoRegister
public class DisplayNames extends AbstractModule {
    private String currencyVault = "", currencyPlayerPoints = "";
    private final Map<String, String> currencyMPoints = new HashMap<>();
    public DisplayNames(SweetAdaptiveShop plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        currencyVault = config.getString("display-names.currency-types.vault", "&e金币");
        currencyPlayerPoints = config.getString("display-names.currency-types.points", "&d点券");
        currencyMPoints.clear();
        ConfigurationSection section = config.getConfigurationSection("display-names.currency-types.m-points");
        if (section != null) for (String sign : section.getKeys(false)) {
            currencyMPoints.put(sign, section.getString(sign, sign));
        }
    }

    @NotNull
    public String getCurrencyVault() {
        return currencyVault;
    }

    @NotNull
    public String getCurrencyPlayerPoints() {
        return currencyPlayerPoints;
    }

    @NotNull
    public String getCurrencyMPoints(@NotNull String sign) {
        return currencyMPoints.getOrDefault(sign, sign);
    }

    public String formatMoney(double money, IEconomy currency) {
        return formatMoney(money) + currency.displayName();
    }

    @NotNull
    public String formatMoney(double money) {
        return String.format("%.2f", money).replace(".00", "");
    }
}
