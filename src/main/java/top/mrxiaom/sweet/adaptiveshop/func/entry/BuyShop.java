package top.mrxiaom.sweet.adaptiveshop.func.entry;

import com.udojava.evalex.Expression;
import de.tr7zw.changeme.nbtapi.NBT;
import org.apache.commons.lang.math.DoubleRange;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.enums.PermMode;
import top.mrxiaom.sweet.adaptiveshop.enums.Routine;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.mythic.IMythic;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class BuyShop {
    public final String group, id, permission;
    public final ItemStack displayItem;
    public final String displayName;
    public final int matchPriority;
    public final Function<ItemStack, Boolean> matcher;
    public final double priceBase;
    public final DoubleRange scaleRange;
    public final double scaleWhenDynamicLargeThan;
    public final String scaleFormula;
    public final String scalePermission;
    public final PermMode scalePermissionMode;
    public final double dynamicValueAdd;
    public final Routine routine;
    public final String dynamicValueDisplayFormula;
    public final Map<Double, String> dynamicValuePlaceholders;
    public final String dynamicValuePlaceholderMin;

    BuyShop(String group, String id, String permission, ItemStack displayItem, String displayName, int matchPriority, Function<ItemStack, Boolean> matcher, double priceBase, DoubleRange scaleRange, double scaleWhenDynamicLargeThan, String scaleFormula, String scalePermission, PermMode scalePermissionMode, double dynamicValueAdd, Routine routine, String dynamicValueDisplayFormula, Map<Double, String> dynamicValuePlaceholders) {
        this.group = group;
        this.id = id;
        this.permission = permission;
        this.displayItem = displayItem;
        this.displayName = displayName;
        this.matchPriority = matchPriority;
        this.matcher = matcher;
        this.priceBase = priceBase;
        this.scaleRange = scaleRange;
        this.scaleWhenDynamicLargeThan = scaleWhenDynamicLargeThan;
        this.scaleFormula = scaleFormula;
        this.scalePermission = scalePermission;
        this.scalePermissionMode = scalePermissionMode;
        this.dynamicValueAdd = dynamicValueAdd;
        this.routine = routine;
        this.dynamicValueDisplayFormula = dynamicValueDisplayFormula;
        this.dynamicValuePlaceholders = dynamicValuePlaceholders;
        String minPlaceholder = "";
        Double min = null;
        for (Map.Entry<Double, String> entry : dynamicValuePlaceholders.entrySet()) {
            if (min == null) {
                min = entry.getKey();
                continue;
            }
            if (entry.getKey() < min) {
                min = entry.getKey();
                minPlaceholder = entry.getValue();
            }
        }
        this.dynamicValuePlaceholderMin = minPlaceholder;
    }

    public boolean match(@NotNull ItemStack item) {
        return matcher.apply(item);
    }

    public double getPrice(double dynamic) {
        if (dynamic <= scaleWhenDynamicLargeThan) return priceBase;
        try {
            BigDecimal value = BigDecimal.valueOf(dynamic - scaleWhenDynamicLargeThan);
            BigDecimal scaleValue = new Expression(scaleFormula)
                    .with("value", value)
                    .eval();
            double min = scaleRange.getMinimumDouble() / 100.0;
            double max = scaleRange.getMaximumDouble() / 100.0;
            double scale = Math.max(min, Math.min(max, scaleValue.doubleValue()));
            double price = priceBase * scale;
            return Double.parseDouble(String.format("%.2f", price));
        } catch (Expression.ExpressionException e) {
            SweetAdaptiveShop.getInstance().warn("表达式 " + scaleFormula + " 计算出错", e);
            return priceBase;
        }
    }

    public String getDisplayDynamic(double dynamic) {
        try {
            BigDecimal value = BigDecimal.valueOf(dynamic);
            BigDecimal dynamicValue = new Expression(dynamicValueDisplayFormula)
                    .with("value", value)
                    .eval();
            return String.format("%.2f", dynamicValue.doubleValue());
        } catch (Expression.ExpressionException e) {
            SweetAdaptiveShop.getInstance().warn("表达式 " + dynamicValueDisplayFormula + " 计算出错", e);
            return String.format("%.2f", dynamic);
        }
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
        int j = count;
        for (int i = 0; i < inv.getSize() && j > 0; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().equals(Material.AIR) || item.getAmount() == 0) continue;
            if (match(item)) {
                int amount = item.getAmount();
                if (j >= amount) {
                    j -= amount;
                    item.setType(Material.AIR);
                    item.setAmount(0);
                    item = null;
                } else {
                    amount = amount - j;
                    item.setAmount(amount);
                    j = 0;
                }
                inv.setItem(i, item);
            }
        }
        SweetAdaptiveShop plugin = SweetAdaptiveShop.getInstance();
        double value = dynamicValueAdd * (count - j);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getBuyShopDatabase().addDynamicValue(id, value, routine.nextOutdate());
        });
        if (j > 0) {
            plugin.warn("预料中的错误: 玩家 " + player.getName() + " 向收购商店 " + id + " 提交 " + count + " 个物品时，有 " + j + " 个物品没有提交成功");
        }
    }

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
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String group = config.getString("group", "default");
        String permission = config.getString("permission", "sweet.adaptive.shop.buy." + id).replace("%id%", id);
        String type = config.getString("type");
        String displayName = config.getString("display-name", null);
        ItemStack displayItem;
        int matchPriority;
        Function<ItemStack, Boolean> matcher;
        if ("vanilla".equals(type)) {
            Material material = Util.valueOr(Material.class, config.getString("material"), null);
            if (material == null) {
                holder.warn("[buy] 读取 " + id + " 时，找不到 material 对应物品");
                return null;
            }
            displayItem = new ItemStack(material);
            matchPriority = 1000;
            matcher = item -> item.getType().equals(material);
            if (displayName == null) {
                displayName = "<translate:" + displayItem.getType().getTranslationKey() + ">";
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
            matchPriority = 999;
            matcher = item -> NBT.get(item, nbt -> {
                return mythicId.equals(nbt.getString("MMOITEMS_ITEM_ID"));
            });
            if (displayName == null) {
                displayName = ItemStackUtil.getItemDisplayName(displayItem);
            }
        } else {
            return null;
        }
        double priceBase = config.getDouble("price.base");
        DoubleRange scaleRange = Utils.getDoubleRange(config, "price.scale.range");
        if (scaleRange == null) {
            holder.warn("[buy] 读取 " + id + " 时出错，price.scale.range 输入的范围无效");
            return null;
        }
        double scaleWhenDynamicLargeThan = config.getDouble("price.scale.when-dynamic-value.large-than");
        String scaleFormula = config.getString("price.scale.when-dynamic-value.scale-formula");
        if (testFormulaFail(scaleFormula)) {
            holder.warn("[buy] 读取 " + id + " 时出错，表达式测试出错");
            return null;
        }
        String scalePermission = config.getString("price.scale.when-has-permission.permission");
        PermMode scalePermissionMode = Util.valueOr(PermMode.class, config.getString("price.scale.when-has-permission.mode"), null);
        if (scalePermissionMode == null) {
            holder.warn("[buy] 读取 " + id + " 时出错，price.scale.when-has-permission.mode 的值无效");
            return null;
        }
        double dynamicValueAdd = config.getDouble("dynamic-value.add");
        Routine routine = Util.valueOr(Routine.class, config.getString("dynamic-value.routine"), null);
        if (routine == null) {
            holder.warn("[buy] 读取 " + id + " 时出错，dynamic-value.routine 的值无效");
            return null;
        }
        String dynamicValueDisplayFormula = config.getString("dynamic-value.display-formula");
        if (testFormulaFail(dynamicValueDisplayFormula)) {
            holder.warn("[buy] 读取 " + id + " 时出错，表达式测试出错");
            return null;
        }
        Map<Double, String> dynamicValuePlaceholders = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("dynamic-value.placeholders");
        if (section != null) for (String s : section.getKeys(false)) {
            Double value = Util.parseDouble(s).orElse(null);
            if (value == null) continue;
            String placeholder = section.getString(s);
            dynamicValuePlaceholders.put(value, placeholder);
        }
        return new BuyShop(group, id, permission, displayItem, displayName, matchPriority, matcher, priceBase, scaleRange, scaleWhenDynamicLargeThan, scaleFormula, scalePermission, scalePermissionMode, dynamicValueAdd, routine, dynamicValueDisplayFormula, dynamicValuePlaceholders);
    }
    private static boolean testFormulaFail(String formula) {
        BigDecimal result = Utils.eval(formula, e -> e.and("value", BigDecimal.valueOf(1.23)));
        return result == null;
    }
}
