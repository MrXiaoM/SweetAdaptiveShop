package top.mrxiaom.sweet.adaptiveshop;
        
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.EconomyHolder;

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


    @Override
    protected void beforeEnable() {
        options.registerDatabase(
                // 在这里添加数据库 (如果需要的话)
        );
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetAdaptiveShop 加载完毕");
    }
}
