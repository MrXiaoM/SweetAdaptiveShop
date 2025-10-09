package top.mrxiaom.sweet.adaptiveshop.func.entry.shop;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.IA;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.enums.PermMode;
import top.mrxiaom.sweet.adaptiveshop.enums.Routine;
import top.mrxiaom.sweet.adaptiveshop.enums.Strategy;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.func.entry.ValueFormula;
import top.mrxiaom.sweet.adaptiveshop.mythic.IMythic;
import top.mrxiaom.sweet.adaptiveshop.utils.DoubleRange;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static top.mrxiaom.sweet.adaptiveshop.func.entry.shop.ItemMatcher.create;

public class BuyShop implements IShop {
    public final String group, id, permission;
    public final ItemStack displayItem;
    public final String displayName;
    public final List<String> footer;
    private final ItemMatcher itemMatcher;
    public final double priceBase;
    public final DoubleRange scaleRange;
    public final double scaleWhenDynamicLargeThan;
    public final List<ValueFormula> scaleFormula;
    public final String scalePermission;
    public final PermMode scalePermissionMode;
    public final boolean dynamicValuePerPlayer;
    public final int dynamicValueLimitationPlayer;
    public final double dynamicValueAdd;
    public final double dynamicValueMaximum;
    public final boolean dynamicValueCutWhenMaximum;
    public final Strategy dynamicValueStrategy;
    public final DoubleRange dynamicValueRecover;
    public final Routine routine;
    public final List<ValueFormula> dynamicValueDisplayFormula;
    public final DecimalFormat dynamicValueDisplayFormat;
    public final Map<Double, String> dynamicValuePlaceholders;
    public final String dynamicValuePlaceholderMin;

    BuyShop(String group, String id, String permission, ItemStack displayItem, String displayName,
            List<String> footer, ItemMatcher matcher, double priceBase,
            DoubleRange scaleRange, double scaleWhenDynamicLargeThan, List<ValueFormula> scaleFormula,
            String scalePermission, PermMode scalePermissionMode, boolean dynamicValuePerPlayer,
            int dynamicValueLimitationPlayer,
            double dynamicValueAdd, double dynamicValueMaximum, boolean dynamicValueCutWhenMaximum,
            Strategy dynamicValueStrategy, DoubleRange dynamicValueRecover, Routine routine,
            List<ValueFormula> dynamicValueDisplayFormula, DecimalFormat dynamicValueDisplayFormat, Map<Double, String> dynamicValuePlaceholders) {
        this.group = group;
        this.id = id;
        this.permission = permission;
        this.displayItem = displayItem;
        this.displayName = displayName;
        this.footer = footer;
        this.itemMatcher = matcher;
        this.priceBase = priceBase;
        this.scaleRange = scaleRange;
        this.scaleWhenDynamicLargeThan = scaleWhenDynamicLargeThan;
        this.scaleFormula = scaleFormula;
        this.scalePermission = scalePermission;
        this.scalePermissionMode = scalePermissionMode;
        this.dynamicValuePerPlayer = dynamicValuePerPlayer;
        this.dynamicValueLimitationPlayer = dynamicValueLimitationPlayer;
        this.dynamicValueAdd = dynamicValueAdd;
        this.dynamicValueMaximum = dynamicValueMaximum;
        this.dynamicValueCutWhenMaximum = dynamicValueCutWhenMaximum;
        this.dynamicValueStrategy = dynamicValueStrategy;
        this.dynamicValueRecover = dynamicValueRecover;
        this.routine = routine;
        this.dynamicValueDisplayFormula = dynamicValueDisplayFormula;
        this.dynamicValueDisplayFormat = dynamicValueDisplayFormat;
        this.dynamicValuePlaceholders = dynamicValuePlaceholders;
        String minPlaceholder = "无";
        Double min = null;
        for (Map.Entry<Double, String> entry : dynamicValuePlaceholders.entrySet()) {
            if (min == null || entry.getKey() < min) {
                min = entry.getKey();
                minPlaceholder = entry.getValue();
            }
        }
        this.dynamicValuePlaceholderMin = minPlaceholder;
    }

    @Override
    public String type() {
        return "buy";
    }

    public boolean match(@NotNull ItemStack item) {
        if (item.getType().equals(Material.AIR) || item.getAmount() == 0) return false;
        return itemMatcher.match(item);
    }

    public int getMatcherPriority() {
        return itemMatcher.priority();
    }

    @Deprecated
    public double getPrice(double dynamic) {
        return getPrice(null, dynamic);
    }
    @Deprecated
    public String getDisplayDynamic(double dynamic) {
        return getDisplayDynamic(null, dynamic);
    }
    /**
     * 根据动态值获取收购价格
     */
    public double getPrice(@Nullable OfflinePlayer player, double dynamic) {
        if (dynamic <= scaleWhenDynamicLargeThan) return priceBase;
        BigDecimal value = BigDecimal.valueOf(dynamic - scaleWhenDynamicLargeThan);
        BigDecimal scaleValue = ValueFormula.eval(scaleFormula, player, value);
        if (scaleValue == null) return priceBase;
        double min = scaleRange.minimum() / 100.0;
        double max = scaleRange.maximum() / 100.0;
        double scale = Math.max(min, Math.min(max, scaleValue.doubleValue()));
        double price = priceBase * scale;
        return Double.parseDouble(String.format("%.2f", price));
    }

    /**
     * 获取动态值显示格式
     */
    public String getDisplayDynamic(@Nullable OfflinePlayer player, double dynamic) {
        BigDecimal value = BigDecimal.valueOf(dynamic);
        BigDecimal dynamicValue = ValueFormula.eval(dynamicValueDisplayFormula, player, value);
        double displayValue = dynamicValue == null ? dynamic : dynamicValue.doubleValue();
        return dynamicValueDisplayFormat.format(displayValue);
    }

    @NotNull
    public String getDynamicValuePlaceholder(double dynamic) {
        Double max = null;
        for (Map.Entry<Double, String> entry : dynamicValuePlaceholders.entrySet()) {
            if (entry.getKey() < dynamic) {
                if (max == null || entry.getKey() > max) {
                    max = entry.getKey();
                }
            }
        }
        String min = dynamicValuePlaceholderMin;
        return max == null ? min : dynamicValuePlaceholders.getOrDefault(max, min);
    }

    public int getCount(Player player) {
        int count = 0;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().equals(Material.AIR) || item.getAmount() == 0) continue;
            if (match(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public void take(Player player, int count) {
        PlayerInventory inv = player.getInventory();
        int needToTake = count;
        for (int i = inv.getSize() - 1; i >= 0 && needToTake > 0; i--) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().equals(Material.AIR) || item.getAmount() == 0) continue;
            if (match(item)) {
                int amount = item.getAmount();
                if (needToTake >= amount) {
                    needToTake -= amount;
                    item.setType(Material.AIR);
                    item.setAmount(0);
                    item = null;
                } else {
                    amount = amount - needToTake;
                    item.setAmount(amount);
                    needToTake = 0;
                }
                inv.setItem(i, item);
            }
        }
        SweetAdaptiveShop plugin = SweetAdaptiveShop.getInstance();
        double value = dynamicValueAdd * (count - needToTake);
        plugin.getScheduler().runTaskAsync(() -> addDynamicValue(plugin, player, value, count));
        if (needToTake > 0) {
            plugin.warn("预料中的错误: 玩家 " + player.getName() + " 向收购商店 " + id + " 提交 " + count + " 个物品时，有 " + needToTake + " 个物品没有提交成功");
        }
    }

    public void addDynamicValue(SweetAdaptiveShop plugin, Player player, double value, int count) {
        plugin.getBuyShopDatabase().addDynamicValue(this, player, value);
        if (dynamicValueLimitationPlayer > 0) {
            plugin.getBuyCountDatabase().addCount(player, this, count);
        }
    }

    /**
     * 获取恢复动态值后，新的动态值是多少
     * @param old 旧的动态值
     */
    public double recoverDynamicValue(double old) {
        // 如果恢复策略是 reset，或者未设置恢复范围
        if (dynamicValueStrategy.equals(Strategy.reset) || dynamicValueRecover == null) {
            // 恢复为 0
            return 0;
        }
        // 如果恢复策略是 recover，且设置了恢复范围
        // 动态值减少范围内随机值
        double dynamicValue = Math.max(0, old - dynamicValueRecover.random());
        if (dynamicValueMaximum > 0) {
            // 如果限制了最大值，进行限制
            return Math.min(dynamicValueMaximum, dynamicValue);
        } else {
            // 如果未限制最大值，直接返回
            return dynamicValue;
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.hasPermission(permission);
    }

    public boolean hasBypass(Player player) {
        if (scalePermissionMode.equals(PermMode.ENABLE))
            return !player.hasPermission(scalePermission);
        if (scalePermissionMode.equals(PermMode.DISABLE))
            return player.hasPermission(scalePermission);
        return false;
    }

    @Nullable
    @SuppressWarnings({"deprecation"})
    public static BuyShop load(AbstractModule holder, File file, String id) {
        YamlConfiguration config = new YamlConfiguration();
        config.options().pathSeparator('/');
        try {
            config.load(file);
        } catch (FileNotFoundException ignored) {
        } catch (IOException | InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, ex);
        }
        String group = config.getString("group", "default");
        String permission = config.getString("permission", "sweet.adaptive.shop.buy." + id).replace("%id%", id);
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
                    holder.warn("[buy] 读取 " + id + " 时，找不到 material 对应物品");
                    return null;
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
                holder.warn("[buy] 读取 " + id + " 时，找不到 potion.type 对应药水效果");
                return null;
            }
            String levelStr = config.getString("potion.level");
            Integer level;
            if ("*".equals(levelStr)) {
                level = null;
            } else {
                level = Util.parseInt(levelStr).orElse(null);
                if (level == null) {
                    holder.warn("[buy] 读取 " + id + " 时，potion.level 指定的药水等级不正确");
                    return null;
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
                holder.warn("[buy] 读取 " + id + " 时，potion.variations 为空");
                return null;
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
                holder.warn("[buy] 读取 " + id + " 时，无法生成药水展示图标物品");
                return null;
            }
            PotionType potionType1 = null;
            for (PotionType value : PotionType.values()) {
                if (finalPotion.equals(value.getEffectType())) {
                    potionType1 = value;
                    break;
                }
            }
            if (potionType1 == null) {
                holder.warn("[buy] 读取 " + id + " 时，无法获取药水类型");
                return null;
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
            Enchantment enchant = null;
            for (Enchantment value : Enchantment.values()) {
                String key = value.getKey().toString();
                String name = value.getName();
                if (key.equals(enchantType) || name.equalsIgnoreCase(enchantType)) {
                    enchant = value;
                    break;
                }
            }
            if (enchant == null) {
                holder.warn("[buy] 读取 " + id + " 时，找不到 enchanted-book.type 对应药水效果");
                return null;
            }
            String levelStr = config.getString("enchanted-book.level");
            Integer level;
            if ("*".equals(levelStr)) {
                level = null;
            } else {
                level = Util.parseInt(levelStr).orElse(null);
                if (level == null) {
                    holder.warn("[buy] 读取 " + id + " 时，enchanted-book.level 指定的药水等级不正确");
                    return null;
                }
            }
            Enchantment finalEnchant = enchant;
            displayItem = new ItemStack(Material.ENCHANTED_BOOK);
            matcher = create(999, item -> {
                ItemMeta meta = item.getItemMeta();
                if (!(meta instanceof EnchantmentStorageMeta)) return false;
                EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) meta;
                if (!enchantmentStorageMeta.hasStoredEnchant(finalEnchant)) return false;
                return level == null || enchantmentStorageMeta.getStoredEnchantLevel(finalEnchant) == level;
            });
            ItemMeta meta = displayItem.getItemMeta();
            if (!(meta instanceof EnchantmentStorageMeta)) {
                holder.warn("[buy] 读取 " + id + " 时，无法生成附魔书展示图标物品");
                return null;
            }
            EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) meta;
            enchantmentStorageMeta.addStoredEnchant(finalEnchant, level == null ? 1 : level, true);
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
                holder.warn("[buy] 获取 " + id + " 时出错，未安装前置 MythicMobs");
                return null;
            }
            String mythicId = config.getString("mythic");
            displayItem = mythic.getItem(mythicId);
            if (mythicId == null || displayItem == null) {
                holder.warn("[buy] 获取 " + id + " 时出错，找不到相应的 MythicMobs 物品");
                return null;
            }
            matcher = create(999, item -> mythicId.equals(IMythic.getId(item)));
            if (displayName == null) {
                displayName = ItemStackUtil.getItemDisplayName(displayItem);
            }
        } else if ("itemsadder".equals(type)) {
            if (!holder.plugin.isSupportItemsAdder()) {
                holder.warn("[buy] 获取 " + id + " 时出错，未安装前置 ItemsAdder");
                return null;
            }
            String itemsAdderId = config.getString("itemsadder");
            displayItem = IA.get(itemsAdderId).orElse(null);
            if (itemsAdderId == null || displayItem == null) {
                holder.warn("[buy] 获取 " + id + " 时出错，找不到相应的 ItemsAdder 物品");
                return null;
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
        } else {
            return null;
        }
        List<String> extraDescription = config.getStringList("extra-description");
        if (!extraDescription.isEmpty()) {
            List<String> lore = ItemStackUtil.getItemLore(displayItem);
            lore.addAll(extraDescription);
            ItemStackUtil.setItemLore(displayItem, lore);
        }
        List<String> footer = config.getStringList("footer");
        double priceBase = config.getDouble("price/base");
        DoubleRange scaleRange = Utils.getDoubleRange(config, "price/scale/range");
        if (scaleRange == null) {
            holder.warn("[buy] 读取 " + id + " 时出错，price.scale.range 输入的范围无效");
            return null;
        }
        double scaleWhenDynamicLargeThan = config.getDouble("price/scale/when-dynamic-value/large-than");
        List<ValueFormula> scaleFormula = ValueFormula.load(config, "price/scale/when-dynamic-value/scale-formula");
        if (scaleFormula == null) {
            holder.warn("[buy] 读取 " + id + " 时出错，scale-formula 表达式测试出错");
            return null;
        }
        String scalePermission = config.getString("price/scale/when-has-permission/permission");
        PermMode scalePermissionMode = Util.valueOr(PermMode.class, config.getString("price/scale/when-has-permission/mode"), null);
        if (scalePermissionMode == null) {
            holder.warn("[buy] 读取 " + id + " 时出错，price.scale.when-has-permission.mode 的值无效");
            return null;
        }
        boolean dynamicValuePerPlayer = config.getBoolean("dynamic-value/per-player", false);
        int dynamicValueLimitationPlayer = config.getInt("dynamic-value/limitation/player", 0);
        double dynamicValueAdd = config.getDouble("dynamic-value/add");
        double dynamicValueMaximum = config.getDouble("dynamic-value/maximum", 0.0);
        boolean dynamicValueCutWhenMaximum = config.getBoolean("dynamic-value/cut-when-maximum", false);
        Strategy dynamicValueStrategy = Util.valueOr(Strategy.class, config.getString("dynamic-value/strategy"), Strategy.reset);
        DoubleRange dynamicValueRecover = Utils.getDoubleRange(config, "dynamic-value/recover");
        if (dynamicValueStrategy.equals(Strategy.recover) && dynamicValueRecover == null) {
            holder.warn("[buy] 读取 " + id + " 时出错，dynamic-value.strategy 设为 recover 时，未设置 recover 的值");
            return null;
        }
        Routine routine = Util.valueOr(Routine.class, config.getString("dynamic-value/routine"), null);
        if (routine == null) {
            holder.warn("[buy] 读取 " + id + " 时出错，dynamic-value.routine 的值无效");
            return null;
        }
        List<ValueFormula> dynamicValueDisplayFormula = ValueFormula.load(config, "dynamic-value/display-formula");
        if (dynamicValueDisplayFormula == null) {
            holder.warn("[buy] 读取 " + id + " 时出错，display-formula 表达式测试出错");
            return null;
        }
        DecimalFormat dynamicValueDisplayFormat;
        try {
            dynamicValueDisplayFormat = new DecimalFormat(config.getString("dynamic-value/display-format", "0.00"));
        } catch (Throwable ignored) {
            holder.warn("[buy] 读取 " + id + " 时出错，display-format 格式错误，已设为 '0.00'");
            dynamicValueDisplayFormat = new DecimalFormat("0.00");
        }
        Map<Double, String> dynamicValuePlaceholders = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("dynamic-value/placeholders");
        if (section != null) for (String s : section.getKeys(false)) {
            Double value = Util.parseDouble(s).orElse(null);
            if (value == null) {
                holder.warn("[buy] 读取 " + id + " 时出错，dynamic-value.placeholders 的一个键 " + s + " 无法转换为数字");
                continue;
            }
            String placeholder = section.getString(s);
            dynamicValuePlaceholders.put(value, placeholder);
        }
        return new BuyShop(group, id, permission, displayItem, displayName,
                footer, matcher, priceBase,
                scaleRange, scaleWhenDynamicLargeThan, scaleFormula,
                scalePermission, scalePermissionMode, dynamicValuePerPlayer,
                dynamicValueLimitationPlayer,
                dynamicValueAdd, dynamicValueMaximum, dynamicValueCutWhenMaximum,
                dynamicValueStrategy, dynamicValueRecover, routine,
                dynamicValueDisplayFormula, dynamicValueDisplayFormat, dynamicValuePlaceholders);
    }
}
