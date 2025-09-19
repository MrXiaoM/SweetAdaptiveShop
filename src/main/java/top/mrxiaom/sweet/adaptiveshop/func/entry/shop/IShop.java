package top.mrxiaom.sweet.adaptiveshop.func.entry.shop;

import org.bukkit.entity.Player;

public interface IShop {
    String type();
    String getId();
    boolean hasPermission(Player player);
}
