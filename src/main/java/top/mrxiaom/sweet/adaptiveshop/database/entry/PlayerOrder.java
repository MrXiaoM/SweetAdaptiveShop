package top.mrxiaom.sweet.adaptiveshop.database.entry;

import java.time.LocalDateTime;

public class PlayerOrder {
    String order;
    boolean hasDone;
    LocalDateTime outdate;

    public PlayerOrder(String order, boolean hasDone, LocalDateTime outdate) {
        this.order = order;
        this.hasDone = hasDone;
        this.outdate = outdate;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public boolean isHasDone() {
        return hasDone;
    }

    public void setHasDone(boolean hasDone) {
        this.hasDone = hasDone;
    }

    public LocalDateTime getOutdate() {
        return outdate;
    }

    public void setOutdate(LocalDateTime outdate) {
        this.outdate = outdate;
    }

    public boolean isOutdate() {
        return LocalDateTime.now().isAfter(outdate);
    }
}
