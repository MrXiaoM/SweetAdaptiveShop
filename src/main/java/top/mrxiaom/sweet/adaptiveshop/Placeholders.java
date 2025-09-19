package top.mrxiaom.sweet.adaptiveshop;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.adaptiveshop.func.config.BuyShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.config.SellShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.BuyShop;
import top.mrxiaom.sweet.adaptiveshop.func.entry.shop.SellShop;

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
        if (params.startsWith("dynamic_")) { // 旧版本变量兼容
            params = "buy_" + params;
        }
        return request(offline, params);
    }
    private String request(OfflinePlayer offline, @NotNull String params) {
        if (params.startsWith("buy_dynamic_")) {
            Player player = offline != null && offline.isOnline() ? offline.getPlayer() : null;
            String buyShopId = params.substring(12);
            BuyShop shop = BuyShopManager.inst().get(buyShopId);
            Double dynamicValue = plugin.getBuyShopDatabase().getDynamicValue(shop, player);
            return dynamicValue == null ? "NaN" : String.format("%.2f", dynamicValue);
        }
        if (params.startsWith("sell_dynamic_")) {
            Player player = offline != null && offline.isOnline() ? offline.getPlayer() : null;
            String sellShopId = params.substring(13);
            SellShop shop = SellShopManager.inst().get(sellShopId);
            Double dynamicValue = plugin.getSellShopDatabase().getDynamicValue(shop, player);
            return dynamicValue == null ? "NaN" : String.format("%.2f", dynamicValue);
        }
        return null;
    }
}
