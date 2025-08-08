package top.mrxiaom.sweet.adaptiveshop.func;

import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.pluginbase.func.gui.IModel;

public abstract class AbstractGuisModule<M extends IModel> extends top.mrxiaom.pluginbase.func.AbstractGuisModule<SweetAdaptiveShop, M> {
    public AbstractGuisModule(SweetAdaptiveShop plugin, String warningPrefix) {
        super(plugin, warningPrefix);
    }
}
