package top.mrxiaom.sweet.adaptiveshop.enums;

import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public enum Routine {
    DAILY, WEEKLY, MONTHLY;

    public LocalDateTime nextOutdate() {
        LocalDateTime time = LocalDateTime.now();
        if (this.equals(DAILY)) {
            time = time.plusDays(1);
        }
        if (this.equals(WEEKLY)) {
            if (!time.getDayOfWeek().equals(DayOfWeek.MONDAY)) {
                time = time.minusDays(time.getDayOfWeek().getValue() - 1);
            }
            time = time.plusWeeks(1);
        }
        if (this.equals(MONTHLY)) {
            time = time.plusMonths(1).withDayOfMonth(1);
        }
        return Utils.nextOutdate(time);
    }
}
