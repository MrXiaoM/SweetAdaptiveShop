package top.mrxiaom.sweet.adaptiveshop;
        
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.EconomyHolder;
import top.mrxiaom.sweet.adaptiveshop.database.BuyShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.OrderDatabase;

public class SweetAdaptiveShop extends BukkitPlugin {
    public static SweetAdaptiveShop getInstance() {
        return (SweetAdaptiveShop) BukkitPlugin.getInstance();
    }

    public SweetAdaptiveShop() {
        super(options()
                .bungee(false)
                .adventure(true)
                .database(true)
                .reconnectDatabaseWhenReloadConfig(true)
                .vaultEconomy(true)
                .scanIgnore("top.mrxiaom.sweet.adaptiveshop.libs")
        );
    }
    @NotNull
    public EconomyHolder getEconomy() {
        return options.economy();
    }

    private BuyShopDatabase buyShopDatabase;
    private OrderDatabase orderDatabase;

    public BuyShopDatabase getBuyShopDatabase() {
        return buyShopDatabase;
    }

    public OrderDatabase getOrderDatabase() {
        return orderDatabase;
    }

    @Override
    protected void beforeEnable() {
        options.registerDatabase(
                this.buyShopDatabase = new BuyShopDatabase(this),
                this.orderDatabase = new OrderDatabase(this)
        );
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetAdaptiveShop 加载完毕");
    }
}
