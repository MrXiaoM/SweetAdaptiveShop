package top.mrxiaom.sweet.adaptiveshop.actions;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;

import java.util.List;

public class ActionGive implements IAction {
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.startsWith("[give]")) return parse(s.substring(6).split(" "));
        if (s.startsWith("give:")) return parse(s.substring(5).split(" "));
        return null;
    };
    public final Material material;
    public final int count;
    public ActionGive(Material material, int count) {
        this.material = material;
        this.count = Math.min(material.getMaxStackSize(), count);
    }
    @Override
    public void run(Player player, List<Pair<String, Object>> pairs) {
        ItemStack item = new ItemStack(material, count);
        ItemStackUtil.giveItemToPlayer(player, item);
    }

    public static ActionGive parse(String[] args) {
        if (args.length != 2) return null;
        Material material = Util.valueOr(Material.class, args[0], null);
        int count = Util.parseInt(args[1]).orElse(0);
        if (material == null || material.equals(Material.AIR)|| count <= 0) return null;
        return new ActionGive(material, count);
    }
}
