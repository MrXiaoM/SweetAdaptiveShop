package top.mrxiaom.sweet.adaptiveshop.mythic;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;

public interface IMythic {
    @Nullable
    ItemStack getItem(String id);

    @Nullable
    String getItemId(ItemStack item);

    @Nullable
    @Deprecated
    static String getId(ItemStack item) {
        IMythic mythic = SweetAdaptiveShop.getInstance().getMythic();
        return mythic == null ? null : mythic.getItemId(item);
    }
}
