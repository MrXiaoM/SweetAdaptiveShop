package top.mrxiaom.sweet.adaptiveshop;
        
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import me.yic.mpoints.MPointsAPI;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.func.LanguageManager;
import top.mrxiaom.pluginbase.paper.PaperFactory;
import top.mrxiaom.pluginbase.resolver.DefaultLibraryResolver;
import top.mrxiaom.pluginbase.utils.ClassLoaderWrapper;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.inventory.InventoryFactory;
import top.mrxiaom.pluginbase.utils.item.ItemEditor;
import top.mrxiaom.pluginbase.utils.scheduler.FoliaLibScheduler;
import top.mrxiaom.sweet.adaptiveshop.actions.ActionGive;
import top.mrxiaom.sweet.adaptiveshop.actions.ActionRefresh;
import top.mrxiaom.sweet.adaptiveshop.api.IEconomyResolver;
import top.mrxiaom.sweet.adaptiveshop.api.economy.*;
import top.mrxiaom.sweet.adaptiveshop.database.BuyCountDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.BuyShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.OrderDatabase;
import top.mrxiaom.sweet.adaptiveshop.database.SellShopDatabase;
import top.mrxiaom.sweet.adaptiveshop.func.config.DisplayNames;
import top.mrxiaom.sweet.adaptiveshop.mythic.IMythic;
import top.mrxiaom.sweet.adaptiveshop.mythic.Mythic4;
import top.mrxiaom.sweet.adaptiveshop.mythic.Mythic5;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SweetAdaptiveShop extends BukkitPlugin {
    public static SweetAdaptiveShop getInstance() {
        return (SweetAdaptiveShop) BukkitPlugin.getInstance();
    }

    public SweetAdaptiveShop() throws Exception {
        super(options()
                .adventure(true)
                .database(true)
                .reconnectDatabaseWhenReloadConfig(false)
                .scanIgnore("top.mrxiaom.sweet.adaptiveshop.libs")
        );
        this.scheduler = new FoliaLibScheduler(this);

        info("正在检查依赖库状态");
        File librariesDir = ClassLoaderWrapper.isSupportLibraryLoader
                ? new File("libraries")
                : new File(this.getDataFolder(), "libraries");
        DefaultLibraryResolver resolver = new DefaultLibraryResolver(getLogger(), librariesDir);

        YamlConfiguration overrideLibraries = ConfigUtils.load(resolve("./.override-libraries.yml"));
        for (String key : overrideLibraries.getKeys(false)) {
            resolver.getStartsReplacer().put(key, overrideLibraries.getString(key));
        }
        resolver.addResolvedLibrary(BuildConstants.RESOLVED_LIBRARIES);

        List<URL> libraries = resolver.doResolve();
        info("正在添加 " + libraries.size() + " 个依赖库到类加载器");
        for (URL library : libraries) {
            this.classLoader.addURL(library);
        }
    }

    @NotNull
    @Deprecated
    public top.mrxiaom.pluginbase.economy.IEconomy getEconomy() {
        return vault;
    }

    @Override
    public @NotNull InventoryFactory initInventoryFactory() {
        return PaperFactory.createInventoryFactory();
    }

    @Override
    public @NotNull ItemEditor initItemEditor() {
        return PaperFactory.createItemEditor();
    }

    private DisplayNames displayNames;
    private final List<IEconomyResolver> economyResolvers = new ArrayList<>();
    private IEconomy vault;
    private IEconomy playerPoints;
    private IEconomyWithSign mPoints;
    private IEconomyWithSign coinsEngine;

    private IMythic mythic;
    private BuyShopDatabase buyShopDatabase;
    private SellShopDatabase sellShopDatabase;
    private OrderDatabase orderDatabase;
    public BuyCountDatabase buyCountDatabase;
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

    public DisplayNames displayNames() {
        return displayNames;
    }

    @Nullable
    public IEconomy getVault() {
        return vault;
    }
    @Nullable
    public IEconomy getPlayerPoints() {
        return playerPoints;
    }
    @Nullable
    public IEconomyWithSign getMPoints() {
        return mPoints;
    }
    @Nullable
    public IEconomyWithSign getCoinsEngine() {
        return coinsEngine;
    }

    @Nullable
    public IEconomy parseEconomy(@Nullable String str) {
        if (str == null) {
            return null;
        }
        for (IEconomyResolver resolver : economyResolvers) {
            IEconomy economy = resolver.parse(str);
            if (economy != null) {
                return economy;
            }
        }
        return null;
    }

    public List<IEconomyResolver> economyResolvers() {
        return Collections.unmodifiableList(economyResolvers);
    }

    public void registerEconomy(IEconomyResolver resolver) {
        economyResolvers.add(resolver);
        economyResolvers.sort(Comparator.comparing(IEconomyResolver::priority));
    }

    public void unregisterEconomy(IEconomyResolver resolver) {
        economyResolvers.remove(resolver);
        economyResolvers.sort(Comparator.comparing(IEconomyResolver::priority));
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

    public SellShopDatabase getSellShopDatabase() {
        return sellShopDatabase;
    }

    public OrderDatabase getOrderDatabase() {
        return orderDatabase;
    }

    public BuyCountDatabase getBuyCountDatabase() {
        return buyCountDatabase;
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
        LanguageManager.inst()
                .setLangFile("messages.yml")
                .register(Messages.class, Messages::holder)
                .reload();

        initEconomy();

        options.registerDatabase(
                this.buyShopDatabase = new BuyShopDatabase(this),
                this.sellShopDatabase = new SellShopDatabase(this),
                this.orderDatabase = new OrderDatabase(this),
                this.buyCountDatabase = new BuyCountDatabase(this)
        );
        ActionProviders.registerActionProvider(ActionGive.PROVIDER);
        ActionProviders.registerActionProvider(ActionRefresh.PROVIDER);
    }

    private void initEconomy() {
        List<String> loadedEconomies = new ArrayList<>();
        try {
            if (Util.isPresent("net.milkbowl.vault.economy.Economy")) {
                RegisteredServiceProvider<Economy> service = Bukkit.getServicesManager().getRegistration(Economy.class);
                Economy provider = service == null ? null : service.getProvider();
                if (provider != null) {
                    vault = new VaultEconomy(this, provider);
                    economyResolvers.add(new VaultEconomy.Resolver(this));
                    loadedEconomies.add(vault.getName());
                } else {
                    warn("已发现 Vault，但经济插件未加载，无法挂钩经济插件");
                }
            }
        } catch (NoClassDefFoundError ignored) {
        }
        try {
            if (Util.isPresent("org.black_ixx.playerpoints.PlayerPointsAPI")) {
                PlayerPointsAPI api = PlayerPoints.getInstance().getAPI();
                playerPoints = new PlayerPointsEconomy(this, api);
                economyResolvers.add(new PlayerPointsEconomy.Resolver(this));
                loadedEconomies.add(playerPoints.getName());
            }
        } catch (NoClassDefFoundError ignored) {
        }
        try {
            if (Util.isPresent("me.yic.mpoints.MPointsAPI")) {
                mPoints = new MPointsEconomy(this, new MPointsAPI(), null);
                economyResolvers.add(new MPointsEconomy.Resolver(this));
                loadedEconomies.add(mPoints.getName());
            }
        } catch (NoClassDefFoundError ignored) {
        }
        try {
            if (Util.isPresent("su.nightexpress.coinsengine.api.CoinsEngineAPI")) {
                coinsEngine = new CoinsEngineEconomy(null);
                economyResolvers.add(new CoinsEngineEconomy.Resolver(this));
                loadedEconomies.add(coinsEngine.getName());
            }
        } catch (LinkageError ignored) {
        }
        for (String name : loadedEconomies) {
            info("已挂钩经济插件 " + name);
        }
    }

    @Override
    protected void afterEnable() {
        this.displayNames = DisplayNames.get(DisplayNames.class).orElseThrow(IllegalStateException::new);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new Placeholders(this).register();
        }
        getLogger().info("SweetAdaptiveShop 加载完毕");
    }

    public String getDBKey(OfflinePlayer player) {
        if (isUuidMode()) {
            return player.getUniqueId().toString();
        } else {
            return player.getName();
        }
    }
}
