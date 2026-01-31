package top.mrxiaom.sweet.adaptiveshop.api;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Pair;

public interface ItemAdapter {
    @NotNull Pair<Object, ItemStack> parseItem(@Nullable String str) throws IllegalStateException;
    boolean isTheSameId(@Nullable ItemStack item, @NotNull Object id);
}
