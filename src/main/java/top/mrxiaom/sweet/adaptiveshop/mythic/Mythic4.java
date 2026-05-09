package top.mrxiaom.sweet.adaptiveshop.mythic;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter;
import io.lumine.xikage.mythicmobs.util.jnbt.CompoundTag;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Mythic4 implements IMythic {
    MythicMobs plugin = MythicMobs.inst();
    @Nullable
    @Override
    public ItemStack getItem(String id) {
        if (id == null) return null;
        return plugin.getItemManager().getItem(id)
                .map(it -> it.generateItemStack(1))
                .map(BukkitAdapter::adapt)
                .orElse(null);
    }

    @Nullable
    @Override
    public String getItemId(ItemStack item) {
        if (item == null || item.getType().equals(Material.AIR) || item.getAmount() <= 0) {
            return null;
        }
        CompoundTag data = plugin.getVolatileCodeHandler().getItemHandler().getNBTData(item);
        if (data != null && data.containsKey("MYTHIC_TYPE")) {
            return data.getString("MYTHIC_TYPE");
        }
        return null;
    }
}
