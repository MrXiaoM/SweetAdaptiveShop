package top.mrxiaom.sweet.adaptiveshop;

import org.bukkit.command.CommandSender;
import top.mrxiaom.pluginbase.func.language.IHolderAccessor;
import top.mrxiaom.pluginbase.func.language.Language;
import top.mrxiaom.pluginbase.func.language.LanguageEnumAutoHolder;
import top.mrxiaom.pluginbase.utils.AdventureUtil;

import java.util.List;

import static top.mrxiaom.pluginbase.func.language.LanguageEnumAutoHolder.wrap;

@Language(prefix = "messages.")
public enum Messages implements IHolderAccessor {
    help("&d&lSweetAdaptiveShop &c&l帮助命令",
            "&f/sashop open buy &8-- &7打开收购商店",
            "&f/sashop open order &8-- &7打开收购订单"
    ),
    help_op("&d&lSweetAdaptiveShop &c&l帮助命令&f &7<&e必选参数&7> [&e可选参数&7]",
            "&f/sashop open buy &7[&e分组&7] [&e玩家&7] &8-- &7为自己或某在线玩家打开收购商店",
            "&f/sashop open order &7[&e玩家&7] &8-- &7为自己或某在线玩家打开收购订单",
            "&f/sashop give &7<&e玩家&7> <&e物品模板&7> <&e数量&7> <&e物品类型&7> &7<&e时间计算操作...>",
            "  &8-- &7以特定的模板和数量，特定的到期时间，给予某人道具",
            "     &7物品模板请见 template.yml，物品类型可使用 buy 或 order。",
            "     &7时间计算操作请参考<click:open_url:https://www.minebbs.com/resources/9883><hover:show_text:'我是链接'>&n&f这个链接</hover></click>&r&7的文档说明。",
            "&f/sashop reload database &8-- &7重载 database.yml 并重新连接数据库",
            "&f/sashop reload &8-- &7重载插件配置文件，但不重新连接数据库"
    ),


    /*------------------------------------------------------------------*/
    ;Messages(String defaultValue) {
        holder = wrap(this, defaultValue);
    }
    Messages(String... defaultValue) {
        holder = wrap(this, defaultValue);
    }
    Messages(List<String> defaultValue) {
        holder = wrap(this, defaultValue);
    }
    private final LanguageEnumAutoHolder<Messages> holder;
    public LanguageEnumAutoHolder<Messages> holder() {
        return holder;
    }
    public void send(CommandSender sender, Object... args) {
        for (String s : holder().list(args)) {
            AdventureUtil.sendMessage(sender, s);
        }
    }
}
