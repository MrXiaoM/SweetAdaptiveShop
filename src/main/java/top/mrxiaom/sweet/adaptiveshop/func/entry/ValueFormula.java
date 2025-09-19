package top.mrxiaom.sweet.adaptiveshop.func.entry;

import com.google.common.collect.Lists;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ValueFormula {
    private final @NotNull String formula;
    public ValueFormula(@NotNull String formula) {
        this.formula = formula;
    }

    public BigDecimal eval(@Nullable OfflinePlayer player, BigDecimal value) {
        return Utils.eval(player, formula, e -> e.with("value", value));
    }

    public static BigDecimal eval(List<ValueFormula> list, @Nullable OfflinePlayer player, BigDecimal value) {
        int size = list.size();
        if (size == 0) return value;
        if (size == 1) return list.get(0).eval(player, value);
        BigDecimal result = value;
        for (ValueFormula formula : list) {
            result = formula.eval(player, result);
        }
        return result;
    }

    public static List<ValueFormula> load(ConfigurationSection config, String key) {
        if (config.isList(key)) {
            List<String> stringList = config.getStringList(key);
            if (stringList.isEmpty()) return null;
            List<ValueFormula> formulaList = new ArrayList<>();
            for (String s : stringList) {
                if (!testFormulaFail(s)) {
                    return null;
                }
                formulaList.add(new ValueFormula(s));
            }
            return formulaList;
        } else {
            String formula = config.getString(key);
            if (formula == null || testFormulaFail(formula)) {
                return null;
            }
            return Lists.newArrayList(new ValueFormula(formula));
        }
    }
    private static boolean testFormulaFail(String formula) {
        if (formula.split("%").length > 2) {
            // 有至少两个2代表使用了 PAPI 变量，不进行检查
            return false;
        }
        BigDecimal result = Utils.eval(formula, e -> e.and("value", BigDecimal.valueOf(1.23)));
        return result == null;
    }
}
