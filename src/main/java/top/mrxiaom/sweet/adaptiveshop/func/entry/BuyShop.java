package top.mrxiaom.sweet.adaptiveshop.func.entry;

import de.tr7zw.changeme.nbtapi.NBT;
import org.apache.commons.lang.math.DoubleRange;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.enums.Routine;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.mythic.IMythic;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.io.File;
import java.math.BigDecimal;
import java.util.function.Function;

public class BuyShop {
    public final String id;
    public final ItemStack displayItem;
    public final Function<ItemStack, Boolean> matcher;
    public final double priceBase;
    public final DoubleRange scaleRange;
    public final double scaleWhenDynamicLargeThan;
    public final String scaleFormula;
    public final double dynamicValueAdd;
    public final Routine routine;
    public final String dynamicValueDisplayFormula;

    BuyShop(String id, ItemStack displayItem, Function<ItemStack, Boolean> matcher, double priceBase, DoubleRange scaleRange, double scaleWhenDynamicLargeThan, String scaleFormula, double dynamicValueAdd, Routine routine, String dynamicValueDisplayFormula) {
        this.id = id;
        this.displayItem = displayItem;
        this.matcher = matcher;
        this.priceBase = priceBase;
        this.scaleRange = scaleRange;
        this.scaleWhenDynamicLargeThan = scaleWhenDynamicLargeThan;
        this.scaleFormula = scaleFormula;
        this.dynamicValueAdd = dynamicValueAdd;
        this.routine = routine;
        this.dynamicValueDisplayFormula = dynamicValueDisplayFormula;
    }

    @Nullable
    public static BuyShop load(AbstractModule holder, File file, String id) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String type = config.getString("type");
        ItemStack displayItem;
        Function<ItemStack, Boolean> matcher;
        if ("vanilla".equals(type)) {
            Material material = Util.valueOr(Material.class, config.getString("material"), null);
            if (material == null) {
                holder.warn("[buy] 读取 " + id + " 时，找不到 material 对应物品");
                return null;
            }
            displayItem = new ItemStack(material);
            matcher = item -> item.getType().equals(material);
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
            matcher = item -> NBT.get(item, nbt -> {
                return mythicId.equals(nbt.getString("MMOITEMS_ITEM_ID"));
            });
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
        return new BuyShop(id, displayItem, matcher, priceBase, scaleRange, scaleWhenDynamicLargeThan, scaleFormula, dynamicValueAdd, routine, dynamicValueDisplayFormula);
    }
    private static boolean testFormulaFail(String formula) {
        BigDecimal result = Utils.eval(formula, e -> e.and("value", BigDecimal.valueOf(1.23)));
        return result == null;
    }
}
