package top.mrxiaom.sweet.adaptiveshop.func.entry.shop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import top.mrxiaom.pluginbase.utils.Util;

public enum EnumPotionVariation {
    NORMAL(Material.POTION, false),
    SPLASH(Util.valueOrNull(Material.class, "SPLASH_POTION"), true),
    LINGERING(Util.valueOrNull(Material.class, "LINGERING_POTION"), null);

    public static final boolean useDataValue = SPLASH.material == null;
    private final Material material;
    private final Boolean isSplash;
    EnumPotionVariation(Material material, Boolean isSplash) {
        this.material = material;
        this.isSplash = isSplash;
    }

    @SuppressWarnings({"deprecation"})
    public boolean isMatch(ItemStack item) {
        if (useDataValue) {
            // 兼容 1.8
            if (isSplash == null || !item.getType().equals(Material.POTION)) {
                return false;
            }
            Potion potion = Potion.fromItemStack(item);
            return potion.isSplash() == isSplash;
        }
        return item.getType().equals(material);
    }

    public Boolean isSplash() {
        return isSplash;
    }

    @SuppressWarnings({"deprecation"})
    public ItemStack createItem() {
        if (material == null) {
            // 兼容 1.8
            return new ItemStack(Material.POTION);
        }
        return new ItemStack(material);
    }
}
