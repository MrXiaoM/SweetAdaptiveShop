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
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.StringJoiner;

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
    public boolean persist() {
        return true;
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
        if (params.equals("next_outdate_remaining_seconds")) {
            LocalDateTime tomorrow = Utils.nextOutdate();
            long totalSeconds = tomorrow.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            return String.valueOf(totalSeconds);
        }
        if (params.equals("next_outdate_remaining")) {
            LocalDateTime tomorrow = Utils.nextOutdate();
            long totalSeconds = tomorrow.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds / 60) % 60;
            long seconds = totalSeconds % 60;
            StringJoiner joiner = new StringJoiner(":");
            if (hours > 0) joiner.add(String.valueOf(hours));
            if (hours > 0 || minutes > 0) joiner.add(String.valueOf(minutes));
            joiner.add(String.valueOf(seconds));
            return joiner.toString();
        }
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
