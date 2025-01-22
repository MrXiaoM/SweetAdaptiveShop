package top.mrxiaom.sweet.adaptiveshop.database.entry;

import java.time.LocalDateTime;

public class PlayerOrder {
    String order;
    int doneCount;
    LocalDateTime outdate;

    public PlayerOrder(String order, int doneCount, LocalDateTime outdate) {
        this.order = order;
        this.doneCount = doneCount;
        this.outdate = outdate;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public int getDoneCount() {
        return doneCount;
    }

    public void setDoneCount(int doneCount) {
        this.doneCount = doneCount;
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
