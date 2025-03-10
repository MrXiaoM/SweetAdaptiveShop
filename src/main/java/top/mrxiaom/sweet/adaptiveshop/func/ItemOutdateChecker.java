package top.mrxiaom.sweet.adaptiveshop.func;

import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
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
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.Messages;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.gui.GuiBuyShop;
import top.mrxiaom.sweet.adaptiveshop.gui.GuiOrders;
import top.mrxiaom.sweet.adaptiveshop.gui.GuiSellShop;
import top.mrxiaom.sweet.adaptiveshop.utils.Utils;

@AutoRegister
public class ItemOutdateChecker extends AbstractModule implements Listener {
    public class NewerVersion implements Listener {
        @EventHandler
        public void onItemDrop(EntityDropItemEvent e) {
            onItemDropOrPickup(e, e.getEntity(), e.getItemDrop());
        }
        @EventHandler
        public void onItemPickup(EntityPickupItemEvent e) {
            onItemDropOrPickup(e, e.getEntity(), e.getItem());
        }
    }

    public class OlderVersion implements Listener {
        @EventHandler
        public void onItemDrop(PlayerDropItemEvent e) {
            onItemDropOrPickup(e, e.getPlayer(), e.getItemDrop());
        }
        @EventHandler
        public void onItemPickup(PlayerPickupItemEvent e) {
            onItemDropOrPickup(e, e.getPlayer(), e.getItem());
        }
    }

    public ItemOutdateChecker(SweetAdaptiveShop plugin) {
        super(plugin);
        registerEvents();
        if (Util.isPresent("org.bukkit.event.entity.EntityPickupItemEvent")
        && Util.isPresent("org.bukkit.event.entity.EntityDropItemEvent")) {
            registerEvents(new NewerVersion());
        } else {
            registerEvents(new OlderVersion());
        }
    }

    public boolean isOutdate(CommandSender sender, ItemStack item) {
        if (item == null || item.getType().equals(Material.AIR) || item.getAmount() == 0) return false;
        long outdate = NBT.get(item, nbt -> {
            if (nbt.hasTag(GuiBuyShop.REFRESH_ITEM)) return nbt.getLong(GuiBuyShop.REFRESH_ITEM);
            if (nbt.hasTag(GuiSellShop.REFRESH_ITEM)) return nbt.getLong(GuiSellShop.REFRESH_ITEM);
            if (nbt.hasTag(GuiOrders.REFRESH_ITEM)) return nbt.getLong(GuiBuyShop.REFRESH_ITEM);
            return 0L;
        });
        if (outdate == 0) return false;
        boolean result = Utils.now() >= outdate;
        if (result) {
            String name = ItemStackUtil.getItemDisplayName(item);
            Messages.refresh__outdate.tm(sender, name);
            item.setType(Material.AIR);
            item.setAmount(0);
            return true;
        }
        return false;
    }

    public void checkInventory(CommandSender sender, Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isOutdate(sender, item)) {
                inv.setItem(i, null);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        checkInventory(player, player.getInventory());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        HumanEntity player = e.getPlayer();
        checkInventory(player, e.getInventory());
        checkInventory(player, player.getInventory());
    }

    @EventHandler
    @SuppressWarnings({"deprecation"})
    public void onInventoryClick(InventoryClickEvent e) {
        HumanEntity player = e.getWhoClicked();
        if (isOutdate(player, e.getCurrentItem())) e.setCurrentItem(null);
        if (isOutdate(player, e.getCursor())) e.setCursor(null);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        if (isOutdate(player, e.getItemDrop().getItemStack())) {
            e.getItemDrop().remove();
            e.setCancelled(true);
        }
    }

    public void onItemDropOrPickup(Cancellable e, Entity entity, Item item) {
        if (isOutdate(entity, item.getItemStack())) {
            item.remove();
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        PlayerInventory inv = player.getInventory();
        if (!plugin.isSupportOffHand()) {
            if (isOutdate(player, inv.getItemInHand())) inv.setItemInHand(null);
            return;
        }
        if (isOutdate(player, inv.getItemInMainHand())) inv.setItemInMainHand(null);
        if (isOutdate(player, inv.getItemInOffHand())) inv.setItemInOffHand(null);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        if (entity instanceof ItemFrame) {
            ItemFrame frame = (ItemFrame) entity;
            if (isOutdate(e.getPlayer(), frame.getItem())) {
                frame.setItem(null);
            }
        }
    }
}
