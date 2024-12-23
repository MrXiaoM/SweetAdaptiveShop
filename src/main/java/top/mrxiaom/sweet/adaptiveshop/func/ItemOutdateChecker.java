package top.mrxiaom.sweet.adaptiveshop.func;

import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.gui.GuiBuyShop;
import top.mrxiaom.sweet.adaptiveshop.gui.GuiOrders;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

@AutoRegister
public class ItemOutdateChecker extends AbstractModule implements Listener {
    public ItemOutdateChecker(SweetAdaptiveShop plugin) {
        super(plugin);
        registerEvents();
    }

    public boolean isOutdate(ItemStack item) {
        if (item == null || item.getType().equals(Material.AIR) || item.getAmount() == 0) return false;
        long outdate = NBT.get(item, nbt -> {
            if (nbt.hasTag(GuiBuyShop.REFRESH_ITEM)) return nbt.getLong(GuiBuyShop.REFRESH_ITEM);
            if (nbt.hasTag(GuiOrders.REFRESH_ITEM)) return nbt.getLong(GuiBuyShop.REFRESH_ITEM);
            return 0L;
        });
        if (outdate == 0) return false;
        boolean result = Utils.now() >= outdate;
        if (result) {
            item.setType(Material.AIR);
            item.setAmount(0);
            return true;
        }
        return false;
    }

    public void checkInventory(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isOutdate(item)) {
                inv.setItem(i, null);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        checkInventory(e.getPlayer().getInventory());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        checkInventory(e.getInventory());
        checkInventory(e.getPlayer().getInventory());
    }

    @EventHandler
    @SuppressWarnings({"deprecation"})
    public void onInventoryClick(InventoryClickEvent e) {
        if (isOutdate(e.getCurrentItem())) e.setCurrentItem(null);
        if (isOutdate(e.getCursor())) e.setCursor(null);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        if (isOutdate(e.getItemDrop().getItemStack())) {
            e.getItemDrop().remove();
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(EntityDropItemEvent e) {
        if (isOutdate(e.getItemDrop().getItemStack())) {
            e.getItemDrop().remove();
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) {
        if (isOutdate(e.getItem().getItemStack())) {
            e.getItem().remove();
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        PlayerInventory inv = player.getInventory();
        if (isOutdate(inv.getItemInMainHand())) inv.setItemInMainHand(null);
        if (isOutdate(inv.getItemInOffHand())) inv.setItemInOffHand(null);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        if (entity instanceof ItemFrame) {
            ItemFrame frame = (ItemFrame) entity;
            if (isOutdate(frame.getItem())) {
                frame.setItem(null);
            }
        }
    }
}
