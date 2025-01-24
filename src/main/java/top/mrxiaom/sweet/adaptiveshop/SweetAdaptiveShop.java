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
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.actions.ActionGive;
import top.mrxiaom.sweet.adaptiveshop.database.BuyShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.OrderDatabase;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractGuiModule;
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
    private boolean supportTranslatable;
    private boolean supportOffHand;
    private boolean supportItemsAdder;
    private boolean uuidMode;

    public boolean isSupportTranslatable() {
        return supportTranslatable;
    }

    public boolean isSupportOffHand() {
        return supportOffHand;
    }

    public boolean isSupportItemsAdder() {
        return supportItemsAdder;
    }

    public void setUuidMode(boolean uuidMode) {
        this.uuidMode = uuidMode;
    }

    public boolean isUuidMode() {
        return uuidMode;
    }

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

        supportTranslatable = Util.isPresent("org.bukkit.Translatable");
        supportOffHand = MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_9_R1);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
    }

    @Override
    protected void beforeEnable() {
        supportItemsAdder = Util.isPresent("dev.lone.itemsadder.api.CustomStack");
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
        AbstractGuiModule.registerActionProvider(ActionGive.PROVIDER);
    }

    @Override
    protected void afterEnable() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new Placeholders(this).register();
        }
        getLogger().info("SweetAdaptiveShop 加载完毕");
    }

    public String getDBKey(Player player) {
        if (isUuidMode()) {
            return player.getUniqueId().toString();
        } else {
            return player.getName();
        }
    }
}
