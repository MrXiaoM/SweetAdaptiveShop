package top.mrxiaom.sweet.adaptiveshop.mythic;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface IMythic {
    @Nullable
    ItemStack getItem(String id);

    @Nullable
    static String getId(ItemStack item) {
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4)) {
            return NBT.getComponents(item, nbt -> {
                if (!nbt.hasTag("minecraft:custom_data")) return null;
                ReadableNBT customData = nbt.getCompound("minecraft:custom_data");
                if (customData == null || !customData.hasTag("PublicBukkitValues")) return null;
                ReadableNBT values = customData.getCompound("PublicBukkitValues");
                if (values == null || !values.hasTag("mythicmobs:type")) return null;
                return values.getString("mythicmobs:type");
            });
        } else {
            return NBT.get(item, nbt -> {
                if (!nbt.hasTag("MYTHIC_TYPE")) return null;
                return nbt.getString("MYTHIC_TYPE");
            });
        }
    }
}
