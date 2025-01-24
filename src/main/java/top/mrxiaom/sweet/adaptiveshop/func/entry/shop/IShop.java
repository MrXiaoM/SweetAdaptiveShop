package top.mrxiaom.sweet.adaptiveshop.func.entry.shop;

import org.bukkit.entity.Player;

public interface IShop {
    String getId();
    boolean hasPermission(Player player);
}
