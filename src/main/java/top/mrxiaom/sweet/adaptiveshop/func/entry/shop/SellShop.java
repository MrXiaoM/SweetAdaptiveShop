package top.mrxiaom.sweet.adaptiveshop.func.entry.shop;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.api.economy.IEconomy;
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

import static top.mrxiaom.pluginbase.actions.ActionProviders.loadActions;

public class SellShop implements IShop {
    public final String group, id, permission;
    public final ItemStack displayItem;
    public final String displayName;
    public final int maxCount;
    public final List<IAction> commands;
    public final IEconomy currency;
    public final double priceBase;
    public final DoubleRange scaleRange;
    public final double scaleWhenDynamicLargeThan;
    public final List<ValueFormula> scaleFormula;
    public final String scalePermission;
    public final PermMode scalePermissionMode;
    public final boolean dynamicValuePerPlayer;
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

    SellShop(AbstractModule holder, File file, String id) {
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
        this.permission = config.getString("permission", "sweet.adaptive.shop.sell." + id).replace("%id%", id);

        ItemMatcherHelper matchItem = ItemMatcherHelper.loadForSellShop(holder, config);
        this.displayName = matchItem.displayName;
        this.displayItem = matchItem.displayItem;
        this.maxCount = matchItem.maxCount;

        this.commands = loadActions(config, "commands");

        String currencyId = config.getString("price/currency", "Vault");
        IEconomy currency = holder.plugin.parseEconomy(currencyId);
        if (currency == null) {
            throw new IllegalArgumentException("price.currency 指定的货币类型 " + currencyId + " 无效");
        }
        this.currency = currency;
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
            holder.warn("[sell] 读取 " + id + " 时出错，display-format 格式错误，已设为 '0.00'");
            dynamicValueDisplayFormat = new DecimalFormat("0.00");
        }
        this.dynamicValueDisplayFormat = dynamicValueDisplayFormat;

        this.dynamicValuePlaceholders = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("dynamic-value/placeholders");
        if (section != null) for (String s : section.getKeys(false)) {
            Double value = Util.parseDouble(s).orElse(null);
            if (value == null) {
                holder.warn("[sell] 读取 " + id + " 时出错，dynamic-value.placeholders 的一个键 " + s + " 无法转换为数字");
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
        return "sell";
    }

    @Deprecated
    public double getPrice(double dynamic) {
        return getPrice(null, dynamic);
    }

    @Deprecated
    public String getDisplayDynamic(double dynamic) {
        return getDisplayDynamic(null, dynamic);
    }

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

    public void give(Player player, int count) {
        SweetAdaptiveShop plugin = SweetAdaptiveShop.getInstance();
        double value = dynamicValueAdd * count;
        plugin.getScheduler().runTaskAsync(() -> addDynamicValue(player, value));
        ItemStack sellitem = new ItemStack(displayItem.getType(), count);
        ItemStackUtil.giveItemToPlayer(player,sellitem);
        try {
            for (IAction action : commands) {
                action.run(player);
            }
        } catch (Throwable t) {
            SweetAdaptiveShop.getInstance().warn("为玩家 " + player.getName() + " 的出售商店操作执行命令时出现异常", t);
        }
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
    public static SellShop load(AbstractModule holder, File file, String id) {
        try {
            return new SellShop(holder, file, id);
        } catch (Throwable t) {
            holder.warn("[sell] 读取 " + id + " 时，" + t.getMessage());
        }
        return null;
    }
}
