package top.mrxiaom.sweet.adaptiveshop.func.entry;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteItemNBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.gui.IModifier;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.PAPI;
import top.mrxiaom.pluginbase.utils.Util;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static top.mrxiaom.pluginbase.func.gui.IModifier.fit;

public class ItemTemplate {
    public final String id;
    public final Material material;
    public final String display;
    public final List<String> lore;
    public final boolean glow;
    public final boolean unique;
    public final Integer customModelData;
    public final Map<String, String> nbtStrings;
    public final Map<String, String> nbtInts;

    public ItemTemplate(String id, Material material, String display, List<String> lore, boolean glow, boolean unique, Integer customModelData, Map<String, String> nbtStrings, Map<String, String> nbtInts) {
        this.id = id;
        this.material = material;
        this.display = display;
        this.lore = lore;
        this.glow = glow;
        this.unique = unique;
        this.customModelData = customModelData;
        this.nbtStrings = nbtStrings;
        this.nbtInts = nbtInts;
    }

    public ItemStack generateIcon(Player player, int amount, @Nullable IModifier<List<String>> loreModifier, Consumer<ReadWriteItemNBT> itemNbt) {
        if (material.equals(Material.AIR) || amount == 0) return new ItemStack(Material.AIR);
        ItemStack item = new ItemStack(material, amount);
        if (!display.isEmpty()) {
            String displayName = PAPI.setPlaceholders(player, display);
            AdventureItemStack.setItemDisplayName(item, displayName);
        }
        if (!lore.isEmpty()) {
            List<String> loreList = PAPI.setPlaceholders(player, fit(loreModifier, lore));
            AdventureItemStack.setItemLoreMiniMessage(item, loreList);
        }
        if (glow) ItemStackUtil.setGlow(item);
        if (customModelData != null) ItemStackUtil.setCustomModelData(item, customModelData);
        if (!nbtStrings.isEmpty() || !nbtInts.isEmpty() || unique || itemNbt != null) {
            NBT.modify(item, nbt -> {
                if (unique) {
                    nbt.setUUID("SWEET_ADAPTIVE_SHOP_UNIQUE", UUID.randomUUID());
                }
                for (Map.Entry<String, String> entry : nbtStrings.entrySet()) {
                    String value = PAPI.setPlaceholders(player, entry.getValue());
                    nbt.setString(entry.getKey(), value);
                }
                for (Map.Entry<String, String> entry : nbtInts.entrySet()) {
                    String value = PAPI.setPlaceholders(player, entry.getValue());
                    Integer i = Util.parseInt(value).orElse(null);
                    if (i == null) continue;
                    nbt.setInteger(entry.getKey(), i);
                }
                if (itemNbt != null) itemNbt.accept(nbt);
            });
        }
        return item;
    }
}
