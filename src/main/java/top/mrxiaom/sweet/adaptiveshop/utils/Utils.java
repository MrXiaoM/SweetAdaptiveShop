package top.mrxiaom.sweet.adaptiveshop.utils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.udojava.evalex.Expression;
import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.Messages;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.mythic.IMythic;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;

import static top.mrxiaom.pluginbase.utils.ItemStackUtil.getItemMeta;

public class Utils {
    @CanIgnoreReturnValue
    public static boolean mkdirs(File file) {
        return file.mkdirs();
    }

    public static long now() {
        return LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    }

    public static LocalDateTime nextOutdate() {
        // TODO: 让用户自定义明天过期时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(LocalDate.now().atTime(4, 0, 0))) {
            return LocalTime.of(4, 0, 0).atDate(LocalDate.now());
        }
        return LocalDate.now().plusDays(1).atTime(4, 0, 0);
    }

    public static int resolveRefreshCount(Player player, String nbtKey) {
        long now = Utils.now();
        int count = 0;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().equals(Material.AIR) || item.getAmount() == 0) continue;
            if (NBT.get(item, nbt -> {
                if (!nbt.hasTag(nbtKey)) return false;
                Long outdate = nbt.getLong(nbtKey);
                return outdate == 0L || now < outdate;
            })) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public static boolean takeFirstRefreshCount(Player player, String nbtKey) {
        long now = Utils.now();
        PlayerInventory inv = player.getInventory();
        boolean flag = false;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().equals(Material.AIR) || item.getAmount() == 0) continue;
            Long outdate = NBT.get(item, nbt -> {
                if (!nbt.hasTag(nbtKey)) return null;
                return nbt.getLong(nbtKey);
            });
            if (outdate == null) continue; // 如果不是刷新券物品，跳过
            if (outdate != 0 && now >= outdate) { // 如果有过期时间，且已过期，清除物品
                String name = ItemStackUtil.getItemDisplayName(item);
                Messages.refresh__outdate.tm(player, name);
                item.setType(Material.AIR);
                item.setAmount(0);
                inv.setItem(i, null);
                continue;
            }
            // 如果没有过期时间，或者刷新券没有过期，且还没有扣过刷新券，扣除一张
            if (!flag) {
                flag = true;
                int amount = item.getAmount();
                if (amount > 1) {
                    item.setAmount(amount - 1);
                } else {
                    item.setType(Material.AIR);
                    item.setAmount(0);
                    item = null;
                }
                inv.setItem(i, item);
            }
        }
        return flag;
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
            return null;
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
