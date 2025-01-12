package top.mrxiaom.sweet.adaptiveshop.func;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;

@AutoRegister(requirePlugins = "ItemsAdder")
public class ItemsAdderManager extends AbstractModule implements Listener {
    private boolean scheduleReload = false;
    public ItemsAdderManager(SweetAdaptiveShop plugin) {
        super(plugin);
        registerEvents();
    }

    protected void scheduleReload() {
        scheduleReload = true;
    }

    @EventHandler
    public void load(ItemsAdderLoadDataEvent e) {
        if (scheduleReload) {
            scheduleReload = false;
            BuyShopManager.inst().reloadBuyShops();
        }
    }

    public static ItemsAdderManager inst() {
        return instanceOf(ItemsAdderManager.class);
    }
}
