package top.mrxiaom.sweet.adaptiveshop.utils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * <a href="https://github.com/MrXiaoM/TimeOperate-Expansion/blob/main/src/main/java/top/mrxiaom/papi/TimeExpansion.java">来自 TimeOperate-Expansion</a>
 */
public class TimeUtils {

    public static LocalDateTime override(LocalDateTime time, String[] args, int startIndex) {
        for (int i = startIndex; i < args.length; i++) {
            String override = args[i];
            if (override.startsWith("+")) {
                Duration d = parseDuration(override.substring(1));
                if (d != null) time = time.plusSeconds(d.getSeconds());
            }
            else if (override.startsWith("-")) {
                Duration d = parseDuration(override.substring(1));
                if (d != null) time = time.minusSeconds(d.getSeconds());
            }
            else if (override.contains("=")) {
                String[] specific = override.split("=", 2);
                String unit = specific[0];
                try {
                    int value = Integer.parseInt(specific[1]);
                    if (e(unit, "s", "second")) time = time.withSecond(value);
                    if (e(unit, "m", "minute")) time = time.withMinute(value);
                    if (e(unit, "h", "hour")) time = time.withHour(value);
                    if (e(unit, "d", "day")) time = time.withDayOfMonth(value);
                    if (e(unit, "M", "month")) time = time.withMonth(value);
                    if (e(unit, "y", "year")) time = time.withYear(value);
                } catch (NumberFormatException ignored) {
                }
            } else if (override.contains(":")) {
                String[] split = override.split(":", 3);
                if (split.length == 2) {
                    try {
                        if (!split[0].equals("~")) time = time.withHour(Integer.parseInt(split[0]));
                        if (!split[1].equals("~")) time = time.withMinute(Integer.parseInt(split[1]));
                        time = time.withSecond(0);
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (split.length == 3) {
                    try {
                        if (!split[0].equals("~")) time = time.withHour(Integer.parseInt(split[0]));
                        if (!split[1].equals("~")) time = time.withMinute(Integer.parseInt(split[1]));
                        if (!split[2].equals("~")) time = time.withSecond(Integer.parseInt(split[2]));
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if (override.startsWith("w+")) {
                try {
                    int weeks = Integer.parseInt(override.substring(2));
                    if (!time.getDayOfWeek().equals(DayOfWeek.MONDAY)) { // 重置回周一
                        time = time.minusDays(time.getDayOfWeek().getValue() - 1);
                    }
                    if (weeks > 0) time = time.plusWeeks(weeks);
                } catch (NumberFormatException ignored) {
                }
            } else if (override.startsWith("w-")) {
                try {
                    int weeks = Integer.parseInt(override.substring(2));
                    if (!time.getDayOfWeek().equals(DayOfWeek.MONDAY)) { // 重置回周一
                        time = time.minusDays(time.getDayOfWeek().getValue() - 1);
                    }
                    if (weeks > 0) time = time.minusWeeks(weeks);
                } catch (NumberFormatException ignored) {
                }
            } else if (override.startsWith("M+")) {
                try {
                    int months = Integer.parseInt(override.substring(2));
                    time = time.plusMonths(months);
                } catch (NumberFormatException ignored) {
                }
            } else if (override.startsWith("M-")) {
                try {
                    int months = Integer.parseInt(override.substring(2));
                    time = time.minusMonths(months);
                } catch (NumberFormatException ignored) {
                }
            } else if (override.toLowerCase().startsWith("y+")) {
                try {
                    int years = Integer.parseInt(override.substring(2));
                    time = time.plusYears(years);
                } catch (NumberFormatException ignored) {
                }
            } else if (override.toLowerCase().startsWith("y-")) {
                try {
                    int years = Integer.parseInt(override.substring(2));
                    time = time.minusYears(years);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return time;
    }

    public static Duration parseDuration(String s) {
        try {
            s = s.toUpperCase().replace("D","DT");
            if (!s.contains("DT")) s = "T" + s;
            if (s.endsWith("T")) s = s.substring(0, s.length() - 1);
            return Duration.parse("P" + s);
        } catch (DateTimeParseException ignored){
            return null;
        }
    }

    /**
     * is equals one of the strings
     * @param s string
     * @param a another strings
     */
    static boolean e(String s, String... a) {
        return Arrays.asList(a).contains(s);
    }
}
