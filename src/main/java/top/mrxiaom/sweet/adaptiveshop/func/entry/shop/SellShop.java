package top.mrxiaom.sweet.adaptiveshop.func.entry.shop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.gui.actions.IAction;
import top.mrxiaom.pluginbase.utils.IA;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.enums.PermMode;
import top.mrxiaom.sweet.adaptiveshop.enums.Routine;
import top.mrxiaom.sweet.adaptiveshop.enums.Strategy;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.mythic.IMythic;
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

import static top.mrxiaom.pluginbase.func.AbstractGuiModule.loadActions;

public class SellShop implements IShop {
    public final String group, id, permission;
    public final ItemStack displayItem;
    public final String displayName;
    public final int maxCount;
    public final List<IAction> commands;
    public final double priceBase;
    public final DoubleRange scaleRange;
    public final double scaleWhenDynamicLargeThan;
    public final String scaleFormula;
    public final String scalePermission;
    public final PermMode scalePermissionMode;
    public final boolean dynamicValuePerPlayer;
    public final double dynamicValueAdd;
    public final double dynamicValueMaximum;
    public final boolean dynamicValueCutWhenMaximum;
    public final Strategy dynamicValueStrategy;
    public final DoubleRange dynamicValueRecover;
    public final Routine routine;
    public final String dynamicValueDisplayFormula;
    public final DecimalFormat dynamicValueDisplayFormat;
    public final Map<Double, String> dynamicValuePlaceholders;
    public final String dynamicValuePlaceholderMin;

    SellShop(String group, String id, String permission, ItemStack displayItem, String displayName,
             int maxCount, List<IAction> commands, double priceBase,
             DoubleRange scaleRange, double scaleWhenDynamicLargeThan, String scaleFormula,
             String scalePermission, PermMode scalePermissionMode, boolean dynamicValuePerPlayer,
             double dynamicValueAdd, double dynamicValueMaximum, boolean dynamicValueCutWhenMaximum,
             Strategy dynamicValueStrategy, DoubleRange dynamicValueRecover, Routine routine,
             String dynamicValueDisplayFormula, DecimalFormat dynamicValueDisplayFormat, Map<Double, String> dynamicValuePlaceholders) {
        this.group = group;
        this.id = id;
        this.permission = permission;
        this.displayItem = displayItem;
        this.displayName = displayName;
        this.maxCount = maxCount;
        this.commands = commands;
        this.priceBase = priceBase;
        this.scaleRange = scaleRange;
        this.scaleWhenDynamicLargeThan = scaleWhenDynamicLargeThan;
        this.scaleFormula = scaleFormula;
        this.scalePermission = scalePermission;
        this.scalePermissionMode = scalePermissionMode;
        this.dynamicValuePerPlayer = dynamicValuePerPlayer;
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

    public double getPrice(double dynamic) {
        if (dynamic <= scaleWhenDynamicLargeThan) return priceBase;
        BigDecimal value = BigDecimal.valueOf(dynamic - scaleWhenDynamicLargeThan);
        BigDecimal scaleValue = Utils.eval(scaleFormula, e -> e.with("value", value));
        if (scaleValue == null) return priceBase;
        double min = scaleRange.minimum() / 100.0;
        double max = scaleRange.maximum() / 100.0;
        double scale = Math.max(min, Math.min(max, scaleValue.doubleValue()));
        double price = priceBase * scale;
        return Double.parseDouble(String.format("%.2f", price));
    }

    public String getDisplayDynamic(double dynamic) {
        BigDecimal value = BigDecimal.valueOf(dynamic);
        BigDecimal dynamicValue = Utils.eval(dynamicValueDisplayFormula, e -> e.with("value", value));
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

    public void give(Player player, int count) {
        // TODO: 给予玩家物品
        SweetAdaptiveShop plugin = SweetAdaptiveShop.getInstance();
        double value = dynamicValueAdd * count;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> addDynamicValue(player, value));
    }

    public void addDynamicValue(Player player, double value) {
        SweetAdaptiveShop plugin = SweetAdaptiveShop.getInstance();
        plugin.getSellShopDatabase().addDynamicValue(this, player, value);
    }

    public double recoverDynamicValue(double old) {
        if (dynamicValueStrategy.equals(Strategy.reset) || dynamicValueRecover == null) {
            return 0;
        }
        double dynamicValue = Math.max(0, old - dynamicValueRecover.random());
        if (dynamicValueMaximum > 0) {
            return Math.min(dynamicValueMaximum, dynamicValue);
        }
        return dynamicValue;
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
    public static SellShop load(AbstractModule holder, File file, String id) {
        YamlConfiguration config = new YamlConfiguration();
        config.options().pathSeparator('/');
        try {
            config.load(file);
        } catch (FileNotFoundException ignored) {
        } catch (IOException | InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, ex);
        }
        String group = config.getString("group", "default");
        String permission = config.getString("permission", "sweet.adaptive.shop.sell." + id).replace("%id%", id);
        String type = config.getString("type");
        String displayName = config.getString("display-name", null);
        ItemStack displayItem;
        Integer maxCount = config.contains("max-count")
                ? config.getInt("max-count")
                : null;
        List<IAction> commands = loadActions(config, "commands");
        if ("vanilla".equals(type)) {
            String raw = config.getString("material", "");
            String[] s = raw.contains(":") ? raw.split(":", 2) : new String[]{raw};
            Material material = Material.matchMaterial(s[0]);
            if (material == null || material.equals(Material.AIR)) {
                material = Util.valueOr(Material.class, s[0], null);
                if (material == null || material.equals(Material.AIR)) {
                    holder.warn("[sell] 读取 " + id + " 时，找不到 material 对应物品");
                    return null;
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
                holder.warn("[sell] 获取 " + id + " 时出错，未安装前置 MythicMobs");
                return null;
            }
            String mythicId = config.getString("mythic");
            displayItem = mythic.getItem(mythicId);
            if (mythicId == null || displayItem == null) {
                holder.warn("[sell] 获取 " + id + " 时出错，找不到相应的 MythicMobs 物品");
                return null;
            }
            if (maxCount == null) {
                maxCount = displayItem.getType().getMaxStackSize();
            }
            if (displayName == null) {
                displayName = ItemStackUtil.getItemDisplayName(displayItem);
            }
        } else if ("itemsadder".equals(type)) {
            if (!holder.plugin.isSupportItemsAdder()) {
                holder.warn("[sell] 获取 " + id + " 时出错，未安装前置 ItemsAdder");
                return null;
            }
            String itemsAdderId = config.getString("itemsadder");
            displayItem = IA.get(itemsAdderId).orElse(null);
            if (itemsAdderId == null || displayItem == null) {
                holder.warn("[sell] 获取 " + id + " 时出错，找不到相应的 ItemsAdder 物品");
                return null;
            }
            if (maxCount == null) {
                maxCount = displayItem.getType().getMaxStackSize();
            }
            if (displayName == null) {
                displayName = ItemStackUtil.getItemDisplayName(displayItem);
            }
        } else {
            return null;
        }
        double priceBase = config.getDouble("price/base");
        DoubleRange scaleRange = Utils.getDoubleRange(config, "price/scale/range");
        if (scaleRange == null) {
            holder.warn("[sell] 读取 " + id + " 时出错，price.scale.range 输入的范围无效");
            return null;
        }
        double scaleWhenDynamicLargeThan = config.getDouble("price/scale/when-dynamic-value/large-than");
        String scaleFormula = config.getString("price/scale/when-dynamic-value/scale-formula");
        if (testFormulaFail(scaleFormula)) {
            holder.warn("[sell] 读取 " + id + " 时出错，scale-formula 表达式测试出错");
            return null;
        }
        String scalePermission = config.getString("price/scale/when-has-permission/permission");
        PermMode scalePermissionMode = Util.valueOr(PermMode.class, config.getString("price/scale/when-has-permission/mode"), null);
        if (scalePermissionMode == null) {
            holder.warn("[sell] 读取 " + id + " 时出错，price.scale.when-has-permission.mode 的值无效");
            return null;
        }
        boolean dynamicValuePerPlayer = config.getBoolean("dynamic-value/per-player", false);
        double dynamicValueAdd = config.getDouble("dynamic-value/add");
        double dynamicValueMaximum = config.getDouble("dynamic-value/maximum", 0.0);
        boolean dynamicValueCutWhenMaximum = config.getBoolean("dynamic-value/cut-when-maximum", false);
        Strategy dynamicValueStrategy = Util.valueOr(Strategy.class, config.getString("dynamic-value/strategy"), Strategy.reset);
        DoubleRange dynamicValueRecover = Utils.getDoubleRange(config, "dynamic-value/recover");
        if (dynamicValueStrategy.equals(Strategy.recover) && dynamicValueRecover == null) {
            holder.warn("[sell] 读取 " + id + " 时出错，dynamic-value.strategy 设为 recover 时，未设置 recover 的值");
            return null;
        }
        Routine routine = Util.valueOr(Routine.class, config.getString("dynamic-value/routine"), null);
        if (routine == null) {
            holder.warn("[sell] 读取 " + id + " 时出错，dynamic-value.routine 的值无效");
            return null;
        }
        String dynamicValueDisplayFormula = config.getString("dynamic-value/display-formula");
        if (testFormulaFail(dynamicValueDisplayFormula)) {
            holder.warn("[sell] 读取 " + id + " 时出错，display-formula 表达式测试出错");
            return null;
        }
        DecimalFormat dynamicValueDisplayFormat;
        try {
            dynamicValueDisplayFormat = new DecimalFormat(config.getString("dynamic-value/display-format", "#.00"));
        } catch (Throwable ignored) {
            holder.warn("[sell] 读取 " + id + " 时出错，display-format 格式错误，已设为 '#.00'");
            dynamicValueDisplayFormat = new DecimalFormat("#.00");
        }
        Map<Double, String> dynamicValuePlaceholders = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("dynamic-value/placeholders");
        if (section != null) for (String s : section.getKeys(false)) {
            Double value = Util.parseDouble(s).orElse(null);
            if (value == null) {
                holder.warn("[sell] 读取 " + id + " 时出错，dynamic-value.placeholders 的一个键 " + s + " 无法转换为数字");
                continue;
            }
            String placeholder = section.getString(s);
            dynamicValuePlaceholders.put(value, placeholder);
        }
        return new SellShop(group, id, permission, displayItem, displayName,
                maxCount, commands, priceBase,
                scaleRange, scaleWhenDynamicLargeThan, scaleFormula,
                scalePermission, scalePermissionMode, dynamicValuePerPlayer,
                dynamicValueAdd, dynamicValueMaximum, dynamicValueCutWhenMaximum,
                dynamicValueStrategy, dynamicValueRecover, routine,
                dynamicValueDisplayFormula, dynamicValueDisplayFormat, dynamicValuePlaceholders);
    }
    private static boolean testFormulaFail(String formula) {
        BigDecimal result = Utils.eval(formula, e -> e.and("value", BigDecimal.valueOf(1.23)));
        return result == null;
    }
}
