package top.mrxiaom.sweet.adaptiveshop.func.config.customgui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.sweet.adaptiveshop.func.config.CustomGuiManager;

public abstract class ShopIcon {
    public final String type;
    public final String itemId;

    protected ShopIcon(String type, String itemId) {
        this.type = type;
        this.itemId = itemId;
    }

    public abstract ItemStack generateIcon(CustomGuiManager.Impl gui, Player player);
    public abstract void onClick(CustomGuiManager.Impl gui, Player player, ClickType click);
}
