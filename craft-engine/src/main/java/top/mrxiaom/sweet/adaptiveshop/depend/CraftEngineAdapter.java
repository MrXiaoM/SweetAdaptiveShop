package top.mrxiaom.sweet.adaptiveshop.depend;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.api.ItemAdapter;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;

@AutoRegister(requirePlugins = "CraftEngine")
public class CraftEngineAdapter extends AbstractModule implements ItemAdapter {
    public CraftEngineAdapter(SweetAdaptiveShop plugin) {
        super(plugin);
        plugin.registerItemAdapter("CraftEngine", this);
        info("已挂钩 CraftEngine");
    }

    @Override
    public @NotNull Pair<Object, ItemStack> parseItem(String str) throws IllegalStateException {
        if (str == null) throw new IllegalStateException("未配置 CE 物品");
        CustomItem<ItemStack> customItem = CraftEngineItems.byId(Key.of(str));
        if (customItem == null) throw new IllegalStateException("找不到 CE 物品 " + str);
        return Pair.of(customItem.id(), customItem.buildItemStack());
    }

    @Override
    public boolean isTheSameId(@Nullable ItemStack item, @NotNull Object id) {
        if (item == null) return false;
        return id.equals(CraftEngineItems.getCustomItemId(item));
    }
}
