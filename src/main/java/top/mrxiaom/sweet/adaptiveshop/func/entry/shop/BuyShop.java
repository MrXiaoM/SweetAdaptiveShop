package top.mrxiaom.sweet.adaptiveshop.func.entry.shop;

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
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.enums.PermMode;
import top.mrxiaom.sweet.adaptiveshop.enums.Routine;
import top.mrxiaom.sweet.adaptiveshop.enums.Strategy;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.func.entry.ValueFormula;
import top.mrxiaom.sweet.adaptiveshop.utils.DoubleRange;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class BuyShop implements IShop {
    public final String group, id, permission;
    public final ItemStack displayItem;
    public final String displayName;
    public final List<String> footer;
    private final ItemMatcher itemMatcher;
    private final Map<Enchantment, List<Integer>> enchantments;
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

    BuyShop(AbstractModule holder, File file, String id) {
        ConfigurationSection section;
        YamlConfiguration config = new YamlConfiguration();
        config.options().pathSeparator('/');
        try {
            config.load(file);
        } catch (FileNotFoundException ignored) {
        } catch (IOException | InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, ex);
        }
        this.id = id;
        this.group = config.getString("group", "default");
        this.permission = config.getString("permission", "sweet.adaptive.shop.buy." + id).replace("%id%", id);

        ItemMatcherHelper matchItem = ItemMatcherHelper.loadForBuyShop(holder, config);
        this.displayName = matchItem.displayName;
        this.displayItem = matchItem.displayItem;
        this.itemMatcher = matchItem.matcher;

        this.enchantments = new HashMap<>();
        section = config.getConfigurationSection("enchantments");
        if (section != null) for (String key : section.getKeys(false)) {
            Enchantment enchant = ItemMatcherHelper.matchEnchant(key);
            if (enchant == null) {
                holder.warn("[buy] 读取 " + id + " 时，无法找到附魔类型 " + key);
                continue;
            }
            List<Integer> levels = section.getIntegerList(key);
            this.enchantments.put(enchant, levels);
        }
        List<String> extraDescription = config.getStringList("extra-description");
        if (!extraDescription.isEmpty()) {
            List<String> lore = ItemStackUtil.getItemLore(displayItem);
            lore.addAll(extraDescription);
            ItemStackUtil.setItemLore(displayItem, lore);
        }
        this.footer = config.getStringList("footer");
        this.priceBase = config.getDouble("price/base");
        DoubleRange scaleRange = Utils.getDoubleRange(config, "price/scale/range");
        if (scaleRange == null) {
            throw new IllegalArgumentException("price.scale.range 输入的范围无效");
        }
        this.scaleRange = scaleRange;

        this.scaleWhenDynamicLargeThan = config.getDouble("price/scale/when-dynamic-value/large-than");
        List<ValueFormula> scaleFormula = ValueFormula.load(config, "price/scale/when-dynamic-value/scale-formula");
        if (scaleFormula == null) {
            throw new IllegalArgumentException("scale-formula 表达式测试出错");
        }
        this.scaleFormula = scaleFormula;

        String scalePermission = config.getString("price/scale/when-has-permission/permission");
        PermMode scalePermissionMode = Util.valueOr(PermMode.class, config.getString("price/scale/when-has-permission/mode"), null);
        if (scalePermissionMode == null) {
            throw new IllegalArgumentException("price.scale.when-has-permission.mode 的值无效");
        }
        this.scalePermission = scalePermission;
        this.scalePermissionMode = scalePermissionMode;

        this.dynamicValuePerPlayer = config.getBoolean("dynamic-value/per-player", false);
        this.dynamicValueLimitationPlayer = config.getInt("dynamic-value/limitation/player", 0);
        this.dynamicValueAdd = config.getDouble("dynamic-value/add");
        this.dynamicValueMaximum = config.getDouble("dynamic-value/maximum", 0.0);
        this.dynamicValueCutWhenMaximum = config.getBoolean("dynamic-value/cut-when-maximum", false);

        Strategy dynamicValueStrategy = Util.valueOr(Strategy.class, config.getString("dynamic-value/strategy"), Strategy.reset);
        DoubleRange dynamicValueRecover = Utils.getDoubleRange(config, "dynamic-value/recover");
        if (dynamicValueStrategy.equals(Strategy.recover) && dynamicValueRecover == null) {
            throw new IllegalArgumentException("dynamic-value.strategy 设为 recover 时，未设置 recover 的值");
        }
        this.dynamicValueStrategy = dynamicValueStrategy;
        this.dynamicValueRecover = dynamicValueRecover;

        Routine routine = Util.valueOr(Routine.class, config.getString("dynamic-value/routine"), null);
        if (routine == null) {
            throw new IllegalArgumentException("dynamic-value.routine 的值无效");
        }
        this.routine = routine;

        List<ValueFormula> dynamicValueDisplayFormula = ValueFormula.load(config, "dynamic-value/display-formula");
        if (dynamicValueDisplayFormula == null) {
            throw new IllegalArgumentException("display-formula 表达式测试出错");
        }
        this.dynamicValueDisplayFormula = dynamicValueDisplayFormula;

        DecimalFormat dynamicValueDisplayFormat;
        try {
            dynamicValueDisplayFormat = new DecimalFormat(config.getString("dynamic-value/display-format", "0.00"));
        } catch (Throwable ignored) {
            holder.warn("[buy] 读取 " + id + " 时出错，display-format 格式错误，已设为 '0.00'");
            dynamicValueDisplayFormat = new DecimalFormat("0.00");
        }
        this.dynamicValueDisplayFormat = dynamicValueDisplayFormat;

        this.dynamicValuePlaceholders = new HashMap<>();
        section = config.getConfigurationSection("dynamic-value/placeholders");
        if (section != null) for (String s : section.getKeys(false)) {
            Double value = Util.parseDouble(s).orElse(null);
            if (value == null) {
                holder.warn("[buy] 读取 " + id + " 时出错，dynamic-value.placeholders 的一个键 " + s + " 无法转换为数字");
                continue;
            }
            String placeholder = section.getString(s);
            this.dynamicValuePlaceholders.put(value, placeholder);
        }
        String minPlaceholder = "无";
        Double min = null;
        for (Map.Entry<Double, String> entry : this.dynamicValuePlaceholders.entrySet()) {
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
        boolean match = itemMatcher.match(item);
        if (match) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null && !enchantments.isEmpty()) return false;
            for (Map.Entry<Enchantment, List<Integer>> entry : enchantments.entrySet()) {
                Enchantment enchant = entry.getKey();
                List<Integer> levels = entry.getValue();
                int enchantLevel = meta.getEnchantLevel(enchant);
                if (enchantLevel == 0) return false;
                if (levels.isEmpty()) continue;
                if (!levels.contains(enchantLevel)) return false;
            }
            return true;
        }
        return false;
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
        return handleDynamicValueMaximum(dynamicValue);
    }

    /**
     * 如果有配置，对动态值应用最大值限制
     * @param dynamicValue 输入的动态值
     * @return 限制后的动态值
     */
    public double handleDynamicValueMaximum(double dynamicValue) {
        if (dynamicValue < 0) return 0.0;
        if (dynamicValueMaximum > 0) {
            // 如果限制了最大值，进行限制
            return Math.min(dynamicValueMaximum, dynamicValue);
        } else {
            // 如果未限制最大值，直接返回
            return dynamicValue;
        }
    }

    public boolean hasReachDynamicValueMaximum(double dynamicValue) {
        if (dynamicValueMaximum > 0) {
            return dynamicValue > dynamicValueMaximum;
        }
        return false;
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
    public static BuyShop load(AbstractModule holder, File file, String id) {
        try {
            return new BuyShop(holder, file, id);
        } catch (Throwable t) {
            holder.warn("[buy] 读取 " + id + " 时，" + t.getMessage());
        }
        return null;
    }
}
