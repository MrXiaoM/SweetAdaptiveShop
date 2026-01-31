package top.mrxiaom.sweet.adaptiveshop.func.entry.shop;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.depend.IA;
import top.mrxiaom.sweet.adaptiveshop.api.ItemAdapter;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.mythic.IMythic;

import java.util.ArrayList;
import java.util.List;

import static top.mrxiaom.sweet.adaptiveshop.func.entry.shop.ItemMatcher.create;

public class ItemMatcherHelper {
    public final String displayName;
    public final ItemStack displayItem;
    public final ItemMatcher matcher;
    public final Integer maxCount;
    ItemMatcherHelper(String displayName, ItemStack displayItem, ItemMatcher matcher, Integer maxCount) {
        this.displayName = displayName;
        this.displayItem = displayItem;
        this.matcher = matcher;
        this.maxCount = maxCount;
    }

    @NotNull
    @SuppressWarnings({"deprecation"})
    public static ItemMatcherHelper loadForBuyShop(AbstractModule holder, ConfigurationSection config) {
        String type = config.getString("type");
        String displayName = config.getString("display-name", null);
        ItemStack displayItem;
        ItemMatcher matcher;
        if ("vanilla".equals(type)) {
            String raw = config.getString("material", "");
            String[] s = raw.contains(":") ? raw.split(":", 2) : new String[]{raw};
            Material material = Material.matchMaterial(s[0]);
            if (material == null || material.equals(Material.AIR)) {
                material = Util.valueOr(Material.class, s[0], null);
                if (material == null || material.equals(Material.AIR)) {
                    throw new IllegalArgumentException("找不到 material 对应物品");
                }
            }
            Integer data = s.length > 1 ? Util.parseInt(s[1]).orElse(null) : null;
            displayItem = data == null ? new ItemStack(material) : new ItemStack(material, 1, data.shortValue());
            Material finalMaterial = material;
            matcher = create(1000, item -> item.getType().equals(finalMaterial)
                    && (data == null || item.getDurability() == data.shortValue()));
            if (displayName == null) {
                if (holder.plugin.isSupportTranslatable()) {
                    displayName = "<translate:" + displayItem.getType().getTranslationKey() + ">";
                } else {
                    displayName = displayItem.getType().name().toLowerCase().replace("_", "");
                }
            }
        } else if ("potion".equals(type)) {
            String potionType = config.getString("potion.type");
            PotionEffectType potion = null;
            for (PotionEffectType value : PotionEffectType.values()) {
                String key = value.getKey().toString();
                String name = value.getName();
                if (key.equals(potionType) || name.equalsIgnoreCase(potionType)) {
                    potion = value;
                    break;
                }
            }
            if (potion == null) {
                throw new IllegalArgumentException("找不到 potion.type 对应药水效果");
            }
            String levelStr = config.getString("potion.level");
            Integer level;
            if ("*".equals(levelStr)) {
                level = null;
            } else {
                level = Util.parseInt(levelStr).orElse(null);
                if (level == null) {
                    throw new IllegalArgumentException("potion.level 指定的药水等级不正确");
                }
            }
            List<EnumPotionVariation> variations = new ArrayList<>();
            for (String s : config.getStringList("potion.variations")) {
                EnumPotionVariation value = Util.valueOr(EnumPotionVariation.class, s, null);
                if (value != null) {
                    variations.add(value);
                }
            }
            if (variations.isEmpty()) {
                throw new IllegalArgumentException("potion.variations 为空");
            }
            PotionEffectType finalPotion = potion;
            displayItem = variations.get(0).createItem();
            matcher = create(999, item -> {
                boolean firstMatch = false;
                for (EnumPotionVariation variation : variations) {
                    if (variation.isMatch(item)) {
                        firstMatch = true;
                        break;
                    }
                }
                if (!firstMatch) return false;
                if (EnumPotionVariation.useDataValue) {
                    // 兼容 1.8
                    if (!item.getType().equals(Material.POTION)) return false;
                    Potion potionMeta = Potion.fromDamage(item.getDurability());
                    PotionEffectType effectType = potionMeta.getType().getEffectType();
                    if (!finalPotion.equals(effectType)) return false;
                    if (level == null) return true;
                    return level == potionMeta.getLevel();
                } else {
                    ItemMeta meta = item.getItemMeta();
                    if (!(meta instanceof PotionMeta)) return false;
                    PotionMeta potionMeta = (PotionMeta) meta;
                    PotionData data = potionMeta.getBasePotionData();
                    PotionEffectType effectType = data.getType().getEffectType();
                    if (!finalPotion.equals(effectType)) return false;
                    if (level == null) return true;
                    int potionLevel = potionMeta.getBasePotionData().isUpgraded() ? 2 : 1;
                    return level == potionLevel;
                }
            });
            ItemMeta meta = displayItem.getItemMeta();
            if (!(meta instanceof PotionMeta)) {
                throw new IllegalArgumentException("无法生成药水展示图标物品");
            }
            PotionType potionType1 = null;
            for (PotionType value : PotionType.values()) {
                if (finalPotion.equals(value.getEffectType())) {
                    potionType1 = value;
                    break;
                }
            }
            if (potionType1 == null) {
                throw new IllegalArgumentException("无法获取药水类型");
            }
            if (EnumPotionVariation.useDataValue) {
                // 兼容 1.8
                Potion potionMeta = Potion.fromDamage(0);
                potionMeta.setSplash(variations.get(0).isSplash() == Boolean.TRUE);
                potionMeta.setType(potionType1);
                potionMeta.apply(displayItem);
            } else {
                PotionMeta potionMeta = (PotionMeta) meta;
                potionMeta.setBasePotionData(new PotionData(potionType1));
                displayItem.setItemMeta(potionMeta);
            }
            if (displayName == null) {
                if (holder.plugin.isSupportTranslatable()) {
                    displayName = "<translate:" + displayItem.getType().getTranslationKey() + ">";
                } else {
                    displayName = displayItem.getType().name().toLowerCase().replace("_", "");
                }
            }
        } else if ("enchanted-book".equals(type)) {
            String enchantType = config.getString("enchanted-book.type");
            Enchantment enchant = matchEnchant(enchantType);
            if (enchant == null) {
                throw new IllegalArgumentException("找不到 enchanted-book.type 对应药水效果");
            }
            String levelStr = config.getString("enchanted-book.level");
            Integer level;
            if ("*".equals(levelStr)) {
                level = null;
            } else {
                level = Util.parseInt(levelStr).orElse(null);
                if (level == null) {
                    throw new IllegalArgumentException("enchanted-book.level 指定的附魔等级不正确");
                }
            }
            displayItem = new ItemStack(Material.ENCHANTED_BOOK);
            matcher = create(999, item -> {
                ItemMeta meta = item.getItemMeta();
                if (!(meta instanceof EnchantmentStorageMeta)) return false;
                EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) meta;
                if (!enchantmentStorageMeta.hasStoredEnchant(enchant)) return false;
                return level == null || enchantmentStorageMeta.getStoredEnchantLevel(enchant) == level;
            });
            ItemMeta meta = displayItem.getItemMeta();
            if (!(meta instanceof EnchantmentStorageMeta)) {
                throw new IllegalArgumentException("无法生成附魔书展示图标物品");
            }
            EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) meta;
            enchantmentStorageMeta.addStoredEnchant(enchant, level == null ? 1 : level, true);
            displayItem.setItemMeta(enchantmentStorageMeta);
            if (displayName == null) {
                if (holder.plugin.isSupportTranslatable()) {
                    displayName = "<translate:" + displayItem.getType().getTranslationKey() + ">";
                } else {
                    displayName = displayItem.getType().name().toLowerCase().replace("_", "");
                }
            }
        } else if ("mythic".equals(type)) {
            IMythic mythic = holder.plugin.getMythic();
            if (mythic == null) {
                throw new IllegalArgumentException("未安装前置 MythicMobs");
            }
            String mythicId = config.getString("mythic");
            displayItem = mythic.getItem(mythicId);
            if (mythicId == null || displayItem == null) {
                throw new IllegalArgumentException("找不到相应的 MythicMobs 物品");
            }
            matcher = create(999, item -> mythicId.equals(IMythic.getId(item)));
            if (displayName == null) {
                displayName = ItemStackUtil.getItemDisplayName(displayItem);
            }
        } else if ("itemsadder".equals(type)) {
            if (!holder.plugin.isSupportItemsAdder()) {
                throw new IllegalArgumentException("未安装前置 ItemsAdder");
            }
            String itemsAdderId = config.getString("itemsadder");
            displayItem = IA.get(itemsAdderId).orElse(null);
            if (itemsAdderId == null || displayItem == null) {
                throw new IllegalArgumentException("找不到相应的 ItemsAdder 物品");
            }
            matcher = create(999, item -> NBT.get(item, nbt -> {
                ReadableNBT itemsadder = nbt.getCompound("itemsadder");
                if (itemsadder == null) return false;
                String realId = itemsadder.getString("namespace") + ":" + itemsadder.getString("id");
                return realId.equals(itemsAdderId);
            }));
            if (displayName == null) {
                displayName = ItemStackUtil.getItemDisplayName(displayItem);
            }
        } else if ("craftengine".equals(type)) {
            ItemAdapter adapter = holder.plugin.getItemAdapter("CraftEngine");
            if (adapter == null) {
                throw new IllegalArgumentException("未安装前置 CraftEngine");
            }
            String craftEngineId = config.getString("craftengine");
            Pair<Object, ItemStack> pair = adapter.parseItem(craftEngineId);
            Object key = pair.key();
            displayItem = pair.value();
            matcher = create(999, item -> adapter.isTheSameId(item, key));
            if (displayName == null) {
                displayName = ItemStackUtil.getItemDisplayName(displayItem);
            }
        } else {
            throw new IllegalArgumentException("不支持的物品类型 " + type);
        }
        return new ItemMatcherHelper(displayName, displayItem, matcher, displayItem.getMaxStackSize());
    }

    @NotNull
    @SuppressWarnings({"deprecation"})
    public static ItemMatcherHelper loadForSellShop(AbstractModule holder, ConfigurationSection config) {
        String type = config.getString("type");
        String displayName = config.getString("display-name", null);
        ItemStack displayItem;
        Integer maxCount = config.contains("max-count")
                ? config.getInt("max-count")
                : null;
        if ("vanilla".equals(type)) {
            String raw = config.getString("material", "");
            String[] s = raw.contains(":") ? raw.split(":", 2) : new String[]{raw};
            Material material = Material.matchMaterial(s[0]);
            if (material == null || material.equals(Material.AIR)) {
                material = Util.valueOr(Material.class, s[0], null);
                if (material == null || material.equals(Material.AIR)) {
                    throw new IllegalArgumentException("找不到 material 对应物品");
                }
            }
            Integer data = s.length > 1 ? Util.parseInt(s[1]).orElse(null) : null;
            displayItem = data == null ? new ItemStack(material) : new ItemStack(material, 1, data.shortValue());
            if (maxCount == null) {
                maxCount = displayItem.getType().getMaxStackSize();
            }
            if (displayName == null) {
                if (holder.plugin.isSupportTranslatable()) {
                    displayName = "<translate:" + displayItem.getType().getTranslationKey() + ">";
                } else {
                    displayName = displayItem.getType().name().toLowerCase().replace("_", "");
                }
            }
        } else if ("mythic".equals(type)) {
            IMythic mythic = holder.plugin.getMythic();
            if (mythic == null) {
                throw new IllegalArgumentException("未安装前置 MythicMobs");
            }
            String mythicId = config.getString("mythic");
            displayItem = mythic.getItem(mythicId);
            if (mythicId == null || displayItem == null) {
                throw new IllegalArgumentException("找不到相应的 MythicMobs 物品");
            }
            if (maxCount == null) {
                maxCount = displayItem.getType().getMaxStackSize();
            }
            if (displayName == null) {
                displayName = ItemStackUtil.getItemDisplayName(displayItem);
            }
        } else if ("itemsadder".equals(type)) {
            if (!holder.plugin.isSupportItemsAdder()) {
                throw new IllegalArgumentException("未安装前置 ItemsAdder");
            }
            String itemsAdderId = config.getString("itemsadder");
            displayItem = IA.get(itemsAdderId).orElse(null);
            if (itemsAdderId == null || displayItem == null) {
                throw new IllegalArgumentException("找不到相应的 ItemsAdder 物品");
            }
            if (maxCount == null) {
                maxCount = displayItem.getType().getMaxStackSize();
            }
            if (displayName == null) {
                displayName = ItemStackUtil.getItemDisplayName(displayItem);
            }
        } else if ("craftengine".equals(type)) {
            ItemAdapter adapter = holder.plugin.getItemAdapter("CraftEngine");
            if (adapter == null) {
                throw new IllegalArgumentException("未安装前置 CraftEngine");
            }
            String craftEngineId = config.getString("craftengine");
            displayItem = adapter.parseItem(craftEngineId).value();
            if (maxCount == null) {
                maxCount = displayItem.getType().getMaxStackSize();
            }
            if (displayName == null) {
                displayName = ItemStackUtil.getItemDisplayName(displayItem);
            }
        } else {
            throw new IllegalArgumentException("不支持的物品类型 " + type);
        }
        return new ItemMatcherHelper(displayName, displayItem, null, maxCount);
    }

    @Nullable
    @SuppressWarnings({"deprecation"})
    public static Enchantment matchEnchant(@Nullable String keyOrName) {
        if (keyOrName == null) return null;
        for (Enchantment value : Enchantment.values()) {
            String key = value.getKey().toString();
            String name = value.getName();
            if (key.equals(keyOrName) || name.equalsIgnoreCase(keyOrName)) {
                return value;
            }
        }
        return null;
    }

}
