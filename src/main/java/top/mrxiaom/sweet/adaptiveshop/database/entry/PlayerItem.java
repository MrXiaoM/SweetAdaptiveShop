package top.mrxiaom.sweet.adaptiveshop.database.entry;

import java.time.LocalDateTime;

/**
 * 储存玩家的商品ID及其到期时间
 */
public class PlayerItem {
    String item;
    LocalDateTime outdate;

    public PlayerItem(String item, LocalDateTime outdate) {
        this.item = item;
        this.outdate = outdate;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
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
