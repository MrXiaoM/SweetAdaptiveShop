package top.mrxiaom.sweet.adaptiveshop.func.config;

import org.bukkit.Bukkit;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractGuisModule;
import top.mrxiaom.sweet.adaptiveshop.func.config.customgui.CustomGui;
import top.mrxiaom.sweet.adaptiveshop.func.config.customgui.ShopIcon;
import top.mrxiaom.sweet.adaptiveshop.gui.Refreshable;

import java.io.File;

@AutoRegister
public class CustomGuiManager extends AbstractGuisModule<CustomGui> {
    public CustomGuiManager(SweetAdaptiveShop plugin) {
        super(plugin, "[gui/custom]");
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        GuiManager manager = GuiManager.inst();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (manager.getOpeningGui(player) instanceof Impl) {
                player.closeInventory();
            }
        }
        super.reloadConfig(cfg);
        File folder = plugin.resolve("./gui/custom/");
        if (!folder.exists()) {
            plugin.saveResource("gui/custom/example.yml", new File(folder, "example.yml"));
        }
        Util.reloadFolder(folder, false, (id, file) -> loadConfig(this, file, id, CustomGui::load));
        info("加载了 " + menus.size() + " 个自定义商店菜单");
    }

    @Override
    public int priority() {
        return 1010;
    }

    @NotNull
    public Impl create(Player player, CustomGui model) {
        return new Impl(player, model);
    }

    public class Impl extends Gui implements Refreshable {
        private Impl(@NotNull Player player, @NotNull CustomGui model) {
            super(player, model);
        }

        @Override
        public void refreshGui() {
            updateInventory(getInventory());
            Util.submitInvUpdate(player);
        }

        public CustomGuiManager manager() {
            return CustomGuiManager.this;
        }

        @Override
        public void onClick(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                ItemStack currentItem, ItemStack cursor,
                InventoryView view, InventoryClickEvent event
        ) {
            event.setCancelled(true);
            Character clickedId = getClickedId(slot);
            if (clickedId != null) {
                ShopIcon shopIcon = model.mainIcons.get(clickedId);
                if (shopIcon != null) {
                    shopIcon.onClick(this, player, click);
                    return;
                }
                LoadedIcon icon = otherIcons.get(clickedId);
                if (icon != null) {
                    icon.click(player, click);
                }
            }
        }

        public void postSubmit() {
            plugin.getScheduler().runTaskLater(() -> {
                if (model.closeAfterSubmit) {
                    player.closeInventory();
                    Util.submitInvUpdate(player);
                } else {
                    refreshGui();
                }
            }, 1L);
        }
    }

    public static CustomGuiManager inst() {
        return instanceOf(CustomGuiManager.class);
    }
}
