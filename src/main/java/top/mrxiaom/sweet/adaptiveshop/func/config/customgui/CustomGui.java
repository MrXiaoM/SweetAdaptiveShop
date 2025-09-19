package top.mrxiaom.sweet.adaptiveshop.func.config.customgui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.gui.IModel;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.sweet.adaptiveshop.func.config.CustomGuiManager;

import java.util.HashMap;
import java.util.Map;

import static top.mrxiaom.pluginbase.func.AbstractGuiModule.getInventory;

public class CustomGui implements IModel {
    public final CustomGuiManager manager;
    private final String id;
    private final String title;
    private final char[] inventory;
    private final @Nullable String permission;
    public final boolean closeAfterSubmit;
    public final Map<Character, ShopIcon> mainIcons;
    private final Map<Character, LoadedIcon> otherIcons;

    private CustomGui(CustomGuiManager manager, String id, ConfigurationSection config, String title, char[] inventory) {
        this.manager = manager;
        this.id = id;
        this.title = title;
        this.inventory = inventory;
        String permission = config.getString("permission", "").trim();
        this.permission = permission.isEmpty() ? null : permission;
        this.closeAfterSubmit = config.getBoolean("close-after-submit", false);
        this.mainIcons = new HashMap<>();
        this.otherIcons = new HashMap<>();
        ConfigurationSection section;

        Map<String, ConfigurationSection> defaultIcons = new HashMap<>();
        section = config.getConfigurationSection("default-icons");
        if (section != null) for (String type : section.getKeys(false)) {
            ConfigurationSection icon = section.getConfigurationSection(type);
            if (icon != null) {
                defaultIcons.put(type, icon);
            }
        }

        section = config.getConfigurationSection("main-icons");
        if (section != null) for (String key : section.getKeys(false)) {
            if (key.length() == 1) {
                char iconId = key.charAt(0);
                ConfigurationSection icon = section.getConfigurationSection(key);
                if (icon == null) continue;
                String type = icon.getString("type", "unknown");
                String itemId = icon.getString("item-id");
                if (itemId == null) {
                    manager.warn("[gui/custom/" + id + "] main-icons." + key + " 未输入商品/订单ID");
                    continue;
                }
                ConfigurationSection def = defaultIcons.get(type);
                if (def != null) for (String path : def.getKeys(true)) {
                    if (!icon.contains(path)) {
                        icon.set(path, def.get(path));
                    }
                }
                switch (type) {
                    case "buy": {
                        ShopIconBuy loaded = ShopIconBuy.load(manager, id, icon, itemId);
                        if (loaded != null) {
                            this.mainIcons.put(iconId, loaded);
                        }
                        break;
                    }
                    case "sell": {
                        ShopIconSell loaded = ShopIconSell.load(manager, id, icon, itemId);
                        if (loaded != null) {
                            this.mainIcons.put(iconId, loaded);
                        }
                        break;
                    }
                    case "order": {
                        // TODO: ShopIconOrder
                        manager.warn("[gui/custom/" + id + "] main-icons." + key + " 的商品类型 " + type + " 正在计划加入插件，敬请期待");
                        // ShopIconOrder loaded = ShopIconOrder.load(manager, id, icon, itemId);
                        // if (loaded != null) {
                        //     this.mainIcons.put(iconId, loaded);
                        // }
                        break;
                    }
                    default: {
                        manager.warn("[gui/custom/" + id + "] main-icons." + key + " 的商品类型无效");
                        break;
                    }
                }
            } else {
                manager.warn("[gui/custom/" + id + "] main-icons." + key + " 的图标ID过长");
            }
        }

        section = config.getConfigurationSection("other-icons");
        if (section != null) for (String key : section.getKeys(false)) {
            if (key.length() == 1) {
                char iconId = key.charAt(0);
                this.otherIcons.put(iconId, LoadedIcon.load(section, key));
            } else {
                manager.warn("[gui/custom/" + id + "] other-icons." + key + " 的图标ID过长");
            }
        }
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public char[] inventory() {
        return inventory;
    }

    @Override
    public Map<Character, LoadedIcon> otherIcons() {
        return otherIcons;
    }

    @Override
    public boolean hasPermission(Permissible p) {
        return permission == null || p.hasPermission(permission);
    }

    @Override
    public ItemStack applyMainIcon(IGuiHolder instance, Player player, char id, int index, int appearTimes) {
        CustomGuiManager.Impl gui = (CustomGuiManager.Impl) instance;
        ShopIcon shopIcon = mainIcons.get(id);
        if (shopIcon != null) {
            return shopIcon.generateIcon(gui, player);
        }
        return null;
    }

    public static CustomGui load(CustomGuiManager manager, ConfigurationSection config, String id) {
        String title = config.getString("title");
        char[] inventory = getInventory(config, "inventory");
        return new CustomGui(manager, id, config, title, inventory);
    }
}
