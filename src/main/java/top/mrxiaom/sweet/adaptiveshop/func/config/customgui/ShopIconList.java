package top.mrxiaom.sweet.adaptiveshop.func.config.customgui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.adaptiveshop.func.config.CustomGuiManager;

import java.util.List;

public class ShopIconList extends ShopIcon {
    private final List<ShopIcon> icons;
    protected ShopIconList(String type, List<ShopIcon> icons) {
        super(type, "<list>");
        this.icons = icons;
    }

    @Nullable
    public ShopIcon getIcon(int index) {
        if (index < 0 || index >= icons.size()) return null;
        return icons.get(index);
    }

    @Override
    public ItemStack generateIcon(CustomGuiManager.Impl gui, Player player) {
        throw new UnsupportedOperationException("用法错误，请使用 getIcon(int)");
    }

    @Override
    public void onClick(CustomGuiManager.Impl gui, Player player, ClickType click) {
        throw new UnsupportedOperationException("用法错误，请使用 getIcon(int)");
    }
}
