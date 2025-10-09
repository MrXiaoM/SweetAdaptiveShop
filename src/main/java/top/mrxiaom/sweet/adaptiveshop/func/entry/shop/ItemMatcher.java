package top.mrxiaom.sweet.adaptiveshop.func.entry.shop;

import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

public interface ItemMatcher {
    int priority();
    boolean match(ItemStack item);

    static ItemMatcher create(int priority, Function<ItemStack, Boolean> matcher) {
        return new ItemMatcher() {
            @Override
            public int priority() {
                return priority;
            }

            @Override
            public boolean match(ItemStack item) {
                return matcher.apply(item);
            }
        };
    }
}
