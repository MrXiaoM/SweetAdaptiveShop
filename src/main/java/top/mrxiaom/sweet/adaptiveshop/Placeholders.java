package top.mrxiaom.sweet.adaptiveshop;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.adaptiveshop.func.config.BuyShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.BuyShop;

public class Placeholders extends PlaceholderExpansion {
    SweetAdaptiveShop plugin;
    public Placeholders(SweetAdaptiveShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean register() {
        try {
            unregister();
        } catch (Throwable ignored) {
        }
        return super.register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "sashop";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offline, @NotNull String params) {
        if (params.startsWith("dynamic_")) {
            Player player = offline != null && offline.isOnline() ? offline.getPlayer() : null;
            String buyShopId = params.substring(8);
            BuyShop shop = BuyShopManager.inst().get(buyShopId);
            Double dynamicValue = plugin.getBuyShopDatabase().getDynamicValue(shop, player);
            return dynamicValue == null ? "NaN" : String.format("%.2f", dynamicValue);
        }
        return null;
    }
}
