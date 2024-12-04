package top.mrxiaom.sweet.adaptiveshop.func;
        
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;

@SuppressWarnings({"unused"})
public abstract class AbstractPluginHolder extends top.mrxiaom.pluginbase.func.AbstractPluginHolder<SweetAdaptiveShop> {
    public AbstractPluginHolder(SweetAdaptiveShop plugin) {
        super(plugin);
    }

    public AbstractPluginHolder(SweetAdaptiveShop plugin, boolean register) {
        super(plugin, register);
    }
}
