package top.mrxiaom.sweet.adaptiveshop.mythic;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter;
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
}
