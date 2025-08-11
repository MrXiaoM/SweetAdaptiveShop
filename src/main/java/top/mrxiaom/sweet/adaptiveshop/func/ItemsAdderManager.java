package top.mrxiaom.sweet.adaptiveshop.func;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.func.config.BuyShopManager;
import top.mrxiaom.sweet.adaptiveshop.func.config.OrderManager;
import top.mrxiaom.sweet.adaptiveshop.func.config.SellShopManager;

@AutoRegister(requirePlugins = "ItemsAdder")
public class ItemsAdderManager extends AbstractModule implements Listener {
    private boolean scheduleReload = false;
    public ItemsAdderManager(SweetAdaptiveShop plugin) {
        super(plugin);
        registerEvents();
    }

    @Override
    public int priority() {
        return 1001;
    }

    @Override
    @SuppressWarnings({"ConstantValue", "deprecation"})
    public void reloadConfig(MemoryConfiguration config) {
        if (plugin.isSupportItemsAdder() && !ItemsAdder.areItemsLoaded()) {
            info("发现服务器已安装 ItemsAdder，但物品尚未加载。将计划在 ItemsAdder 加载物品后再加载商品与订单。");
            ItemsAdderManager.inst().scheduleReload();
        }
    }

    public void scheduleReload() {
        scheduleReload = true;
    }

    @EventHandler
    public void load(ItemsAdderLoadDataEvent e) {
        if (scheduleReload) {
            scheduleReload = false;
            BuyShopManager.inst().reloadBuyShops();
            SellShopManager.inst().reloadSellShops();
            OrderManager.inst().reloadOrders(plugin.getConfig());
        }
    }

    public static ItemsAdderManager inst() {
        return instanceOf(ItemsAdderManager.class);
    }
}
