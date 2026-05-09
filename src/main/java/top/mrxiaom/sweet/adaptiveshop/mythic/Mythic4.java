package top.mrxiaom.sweet.adaptiveshop.mythic;

import de.tr7zw.changeme.nbtapi.NBT;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter;
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
        if (item == null || item.getType().equals(Material.AIR)) return null;
        return NBT.get(item, nbt -> {
            if (nbt.hasTag("MYTHIC_TYPE")) {
                return nbt.getString("MYTHIC_TYPE");
            } else {
                return null;
            }
        });
    }
}
