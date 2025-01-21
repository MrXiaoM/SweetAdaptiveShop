package top.mrxiaom.sweet.adaptiveshop.func;

import org.bukkit.Bukkit;
import org.bukkit.configuration.MemoryConfiguration;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;

@AutoRegister
public class UUIDModeManager extends AbstractModule {
    public UUIDModeManager(SweetAdaptiveShop plugin) {
        super(plugin);
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        if (config.getBoolean("uuid-mode-legacy", true)) {
            plugin.setUuidMode(false);
            return;
        }
        String uuidMode = config.getString("uuid-mode", "auto").toLowerCase();
        switch (uuidMode) {
            case "true": {
                plugin.setUuidMode(true);
                break;
            }
            case "false": {
                plugin.setUuidMode(false);
                break;
            }
            case "auto":
            default: {
                plugin.setUuidMode(Bukkit.getOnlineMode());
                break;
            }
        }
    }
}
