package top.mrxiaom.sweet.adaptiveshop;

import top.mrxiaom.pluginbase.func.language.IHolderAccessor;
import top.mrxiaom.pluginbase.func.language.Language;
import top.mrxiaom.pluginbase.func.language.LanguageEnumAutoHolder;

import java.util.List;

import static top.mrxiaom.pluginbase.func.language.LanguageEnumAutoHolder.wrap;

@Language(prefix = "messages.")
public enum Messages implements IHolderAccessor {
    help("", "&d&lSweetAdaptiveShop &c&l帮助命令&r",
            "&f/sashop open buy",
            "  &8-- &7打开收购商店",
            "&f/sashop open order",
            "  &8-- &7打开收购订单", ""
    ),
    help_op("", "&d&lSweetAdaptiveShop &c&l帮助命令&r&f &7<&e必选参数&7> [&e可选参数&7]",
            "&f/sashop open buy &7[&e分组&7] [&e玩家&7]",
            "  &8-- &7为自己或某在线玩家打开收购商店",
            "&f/sashop open order &7[&e玩家&7]",
            "  &8-- &7为自己或某在线玩家打开收购订单",
            "&f/sashop give &7<&e玩家&7> <&e物品模板&7> <&e数量&7> <&e物品类型&7> &7<&e时间…&7>",
            "  &8-- &7以特定的模板和数量，特定的到期时间，给予某人道具",
            "    &7物品模板请见 template.yml，",
            "    &7物品类型可使用 buy 或 order。",
            "    &7时间计算操作请参考<click:open_url:https://www.minebbs.com/resources/9883><hover:show_text:'我是链接'>&b&n这个链接</hover></click>&r&7的文档说明。",
            "    &7特别地，多个计算操作使用空格而不是下划线隔开，",
            "    &7时间填&f 0 &7或者&f infinite &7代表永久。",
            "&f/sashop reload database",
            "  &8-- &7重载 database.yml 并重新连接数据库",
            "&f/sashop reload",
            "  &8-- &7重载插件配置文件，但不重新连接数据库", ""
    ),
    refresh__outdate("&e刷新券 &f%s&r &e已过期"),
    refresh__buy__not_enough("&e你没有足够的刷新券!"),
    refresh__buy__success("&a你成功刷新了%s商品列表!"),
    refresh__buy__success_other("&a你成功刷新了 %s 的%s商品列表!"),
    refresh__sell__not_enough("&e你没有足够的刷新券!"),
    refresh__sell__success("&a你成功刷新了%s商品列表!"),
    refresh__sell__success_other("&a你成功刷新了 %s 的%s商品列表!"),
    refresh__order__not_enough("&e你没有足够的刷新券!"),
    refresh__order__success("&a你成功刷新了订单列表!"),
    refresh__order__success_other("&a你成功刷新了 %s 的订单列表!"),
    refresh__type_invalid("&e无效的刷新类型 %s"),

    custom_gui__not_input("&e你应该输入菜单名"),
    custom_gui__not_found("&e菜单 &f%s&r &e不存在"),
    group__not_input("&e你应该输入分组名"),
    group__not_found("&e分组 &f%s&r &e不存在"),
    player__not_found("&e玩家 &f%s&r &e不存在"),
    player__not_online("&e玩家 &f%s&r &e不在线"),
    player__no_permission("&c你没有执行此操作的权限"),
    player__only("只有玩家才能执行该命令"),
    gui__not_found("&e找不到这个界面!"),
    gui__buy__outdate("&e这个商品已经过期了! 请重新打开菜单以刷新列表!"),
    gui__buy__not_enough("&e你没有足够的物品提交到商店!"),
    gui__buy__maximum("&e这个商店暂时不再接受这种物品了!"),
    gui__buy__success("&a你提交了 &e%d&a 个 &e%s&r&a，获得 &e%s&r &a金币!"),
    gui__sell__outdate("&e这个商品已经过期了! 请重新打开菜单以刷新列表!"),
    gui__sell__no_money("&e你没有足够的物品提交到商店!"),
    gui__sell__maximum("&e这个商品已经售罄了!"),
    gui__sell__success("&a你花费 &e%s&r &a金币，购买了 &e%d&a 个 &e%s&r&a!"),
    gui__order__outdate("&e这个订单已经过期了! 请重新打开菜单以刷新列表!"),
    gui__order__has_done("&e这个订单已经完成过了!"),
    gui__order__not_enough("&e你没有足够的物品提交这个订单!"),
    gui__order__success("&a你成功提交了订单 &e%s&r&a!"),
    gui__limitation__format("&e%current%&7/&e%max%"),
    gui__limitation__infinite("&e无限"),
    gui__limitation__reach_tips("&e已到达购买上限"),
    template__not_found("&e找不到名为 &f%s&r &e的物品模板"),
    int__invalid("&e请输入正确的数量"),
    int__much("&e你输入的数量太多了"),
    give__type_not_found("&e无效的物品类型 &f%s"),
    give__success("&a成功给予 &f%s&r &b%d&r &a个 &f%s"),
    give__player("&a你收到了 &b%d&r &a个 &f%s"),
    give__full("&f你的物品栏已满，剩余物品已掉落到你附近"),
    reload__config("&a配置文件已重载"),
    reload__database("&a数据库已重新连接"),


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
}
