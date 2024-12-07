package top.mrxiaom.sweet.adaptiveshop.mythic;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Mythic5 implements IMythic {
    MythicBukkit plugin = MythicBukkit.inst();
    @Nullable
    @Override
    public ItemStack getItem(String id) {
        if (id == null) return null;
        return plugin.getItemManager().getItem(id)
                .map(it -> it.generateItemStack(1))
                .map(BukkitAdapter::adapt)
                .orElse(null);
    }
}
