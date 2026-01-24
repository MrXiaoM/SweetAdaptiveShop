package top.mrxiaom.sweet.adaptiveshop.api.economy;

import java.util.List;

public interface IEconomyWithSign {
    String getName();
    List<String> getSigns();
    IEconomy of(String sign);
}
