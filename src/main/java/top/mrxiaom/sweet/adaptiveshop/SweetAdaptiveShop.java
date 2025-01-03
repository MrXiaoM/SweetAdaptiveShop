package top.mrxiaom.sweet.adaptiveshop;
        
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.EconomyHolder;
import top.mrxiaom.pluginbase.func.LanguageManager;
import top.mrxiaom.sweet.adaptiveshop.database.BuyShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.OrderDatabase;
import top.mrxiaom.sweet.adaptiveshop.mythic.IMythic;
import top.mrxiaom.sweet.adaptiveshop.mythic.Mythic4;
import top.mrxiaom.sweet.adaptiveshop.mythic.Mythic5;

public class SweetAdaptiveShop extends BukkitPlugin {
    public static SweetAdaptiveShop getInstance() {
        return (SweetAdaptiveShop) BukkitPlugin.getInstance();
    }

    public SweetAdaptiveShop() {
        super(options()
                .bungee(false)
                .adventure(true)
                .database(true)
                .reconnectDatabaseWhenReloadConfig(false)
                .vaultEconomy(true)
                .scanIgnore("top.mrxiaom.sweet.adaptiveshop.libs")
        );
    }
    @NotNull
    public EconomyHolder getEconomy() {
        return options.economy();
    }

    private IMythic mythic;
    private BuyShopDatabase buyShopDatabase;
    private OrderDatabase orderDatabase;

    public BuyShopDatabase getBuyShopDatabase() {
        return buyShopDatabase;
    }

    public OrderDatabase getOrderDatabase() {
        return orderDatabase;
    }

    @Nullable
    public IMythic getMythic() {
        return mythic;
    }

    @Override
    protected void beforeLoad() {
        MinecraftVersion.replaceLogger(getLogger());
        MinecraftVersion.disableUpdateCheck();
        MinecraftVersion.disableBStats();
        MinecraftVersion.getVersion();
    }

    @Override
    protected void beforeEnable() {
        Plugin mythicPlugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (mythicPlugin != null) {
            String ver = mythicPlugin.getDescription().getVersion();
            if (ver.startsWith("5.")) {
                mythic = new Mythic5();
            } else if (ver.startsWith("4.")) {
                mythic = new Mythic4();
            } else {
                mythic = null;
            }
        } else {
            mythic = null;
        }
        LanguageManager.inst().setLangFile("messages.yml")
                        .register(Messages.class, Messages::holder);
        options.registerDatabase(
                this.buyShopDatabase = new BuyShopDatabase(this),
                this.orderDatabase = new OrderDatabase(this)
        );
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetAdaptiveShop 加载完毕");
    }

    public String getDBKey(Player player) {
        return player.getName(); // TODO
    }
}
