package top.mrxiaom.sweet.adaptiveshop.utils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.udojava.evalex.Expression;
import org.apache.commons.lang.math.DoubleRange;
import org.bukkit.Material;
import org.bukkit.configuration.MemorySection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.IA;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.mythic.IMythic;

import java.io.File;
import java.math.BigDecimal;
import java.util.function.Consumer;

import static top.mrxiaom.pluginbase.utils.ItemStackUtil.getItemMeta;

public class Utils {
    @CanIgnoreReturnValue
    public static boolean mkdirs(File file) {
        return file.mkdirs();
    }
    public static ItemStack getItem(String str) {
        if (str.startsWith("mythic-")) {
            IMythic mythic = SweetAdaptiveShop.getInstance().getMythic();
            if (mythic == null) {
                throw new IllegalStateException("未安装前置插件 MythicMobs");
            }
            ItemStack item = mythic.getItem(str.substring(7));
            if (item == null) {
                throw new IllegalStateException("找不到相应的 MythicMobs 物品");
            }
            return item;
        } else {
            Integer customModelData = null;
            String material = str;
            if (str.contains("#")) {
                String customModel = str.substring(str.indexOf("#") + 1);
                customModelData = Util.parseInt(customModel).orElseThrow(() -> new IllegalStateException("无法解析 " + customModel + " 为整数"));
                material = str.replace("#" + customModelData, "");
            }

            Material m = Util.valueOr(Material.class, material, null);
            if (m == null) {
                throw new IllegalStateException("找不到物品 " + str);
            } else {
                ItemStack item = new ItemStack(m);
                if (customModelData != null) {
                    ItemMeta meta = getItemMeta(item);
                    meta.setCustomModelData(customModelData);
                    item.setItemMeta(meta);
                }

                return item;
            }
        }
    }
    public static DoubleRange getDoubleRange(MemorySection config, String key) {
        String s = config.getString(key, "");
        try {
            String[] split = s.split("-", 2);
            if (split.length != 2) {
                double value = Double.parseDouble(s);
                return new DoubleRange(value);
            }
            double v1 = Double.parseDouble(split[0]);
            double v2 = Double.parseDouble(split[1]);
            return new DoubleRange(v1, v2);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    public static BigDecimal eval(String formula, Consumer<Expression> variables) {
        if (formula == null) {
            SweetAdaptiveShop.getInstance().warn("无法计算空表达式", new RuntimeException());
        }
        try {
            Expression expression = new Expression(formula);
            variables.accept(expression);
            return expression.eval();
        } catch (Expression.ExpressionException e) {
            SweetAdaptiveShop.getInstance().warn("计算表达式 " + formula + " 时出现一个异常", e);
            return null;
        }
    }
}
