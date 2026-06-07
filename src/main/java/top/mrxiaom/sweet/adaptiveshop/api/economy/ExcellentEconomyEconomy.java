package top.mrxiaom.sweet.adaptiveshop.api.economy;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.api.IEconomyResolver;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ExcellentEconomyEconomy implements IEconomyWithSign, IEconomy {
    private static final String NAME = "ExcellentEconomy";
    private static final String PREFIX = NAME + ":";
    private static final String API_CLASS = "su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI";
    private static final String CURRENCY_CLASS = "su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency";
    private static final String OPERATION_CONTEXT_CLASS = "su.nightexpress.excellenteconomy.api.currency.operation.OperationContext";
    private static final String NOTIFICATION_TARGET_CLASS = "su.nightexpress.excellenteconomy.api.currency.operation.NotificationTarget";

    public static Class<?> apiClass() throws ClassNotFoundException {
        return Class.forName(API_CLASS);
    }

    public static class Resolver implements IEconomyResolver {
        private final SweetAdaptiveShop plugin;
        public Resolver(SweetAdaptiveShop plugin) {
            this.plugin = plugin;
        }

        @Override
        public @Nullable IEconomy parse(@NotNull String str) {
            if (str.startsWith(PREFIX) && str.length() > PREFIX.length()) {
                IEconomyWithSign withSign = plugin.getExcellentEconomy();
                if (withSign != null) {
                    return withSign.of(str.substring(PREFIX.length()));
                }
            }
            return null;
        }
    }
    private final Map<String, IEconomy> caches = new HashMap<>();
    private final Object api;
    private final Object currency;
    private final Class<?> apiClass;
    private final Class<?> currencyClass;
    private final Object operationContext;
    private final Class<?> operationContextClass;

    public ExcellentEconomyEconomy(Object api, Object currency) {
        this.api = api;
        this.currency = currency;
        try {
            this.apiClass = apiClass();
            this.currencyClass = Class.forName(CURRENCY_CLASS);
            this.operationContextClass = Class.forName(OPERATION_CONTEXT_CLASS);
            this.operationContext = createOperationContext();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize ExcellentEconomy reflective API bridge", e);
        }
    }

    public String sign() {
        return currencyId(currency);
    }

    public String name() {
        return currencyName(currency);
    }

    @Override
    public List<String> getSigns() {
        List<String> signs = new ArrayList<>();
        for (Object currency : getCurrencies()) {
            signs.add(currencyId(currency));
        }
        return signs;
    }

    @Override
    public IEconomy of(String sign) {
        IEconomy cache = caches.get(sign);
        if (cache != null) return cache;
        Object currency = getCurrency(sign);
        if (currency == null) return null;
        IEconomy economy = new ExcellentEconomyEconomy(api, currency);
        caches.put(sign, economy);
        return economy;
    }

    @Override
    public String id() {
        return currency == null ? NAME : (PREFIX + currencyId(currency));
    }

    @Override
    public String getName() {
        return currency == null ? NAME : (NAME + "{" + currencyId(currency) + "}");
    }

    @Override
    public String displayName() {
        return currency == null ? NAME : currencyName(currency);
    }

    @Override
    public double get(OfflinePlayer player) {
        if (currency == null) throw new UnsupportedOperationException("");
        return getBalanceAsync(player.getUniqueId(), currency).join();
    }

    @Override
    public boolean has(OfflinePlayer player, double money) {
        return get(player) >= money;
    }

    @Override
    public boolean giveMoney(OfflinePlayer player, double money) {
        if (currency == null) throw new UnsupportedOperationException("");
        return canPerformOperations()
                && operationSuccess(depositAsync(player.getUniqueId(), currency, money).join());
    }

    @Override
    public boolean takeMoney(OfflinePlayer player, double money) {
        if (currency == null) throw new UnsupportedOperationException("");
        return canPerformOperations()
                && operationSuccess(withdrawAsync(player.getUniqueId(), currency, money).join());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ExcellentEconomyEconomy)) return false;
        ExcellentEconomyEconomy that = (ExcellentEconomyEconomy) o;
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id());
    }

    private Object createOperationContext() throws ReflectiveOperationException {
        Object context = operationContextClass.getMethod("custom", String.class).invoke(null, "SweetAdaptiveShop");

        Class<?> notificationTargetClass = Class.forName(NOTIFICATION_TARGET_CLASS);
        Object targets = Array.newInstance(notificationTargetClass, 3);
        Array.set(targets, 0, enumValue(notificationTargetClass, "USER"));
        Array.set(targets, 1, enumValue(notificationTargetClass, "EXECUTOR"));
        Array.set(targets, 2, enumValue(notificationTargetClass, "CONSOLE_LOGGER"));

        Method silentFor = operationContextClass.getMethod("silentFor", targets.getClass());
        return silentFor.invoke(context, targets);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> enumClass, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
    }

    @SuppressWarnings("unchecked")
    private Set<Object> getCurrencies() {
        return (Set<Object>) invoke(apiClass, api, "getCurrencies");
    }

    private Object getCurrency(String sign) {
        return invoke(apiClass, api, "getCurrency", new Class<?>[]{String.class}, sign);
    }

    private String currencyId(Object currency) {
        return (String) invoke(currencyClass, currency, "getId");
    }

    private String currencyName(Object currency) {
        return (String) invoke(currencyClass, currency, "getName");
    }

    private boolean canPerformOperations() {
        return Boolean.TRUE.equals(invoke(apiClass, api, "canPerformOperations"));
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Double> getBalanceAsync(UUID playerId, Object currency) {
        return (CompletableFuture<Double>) invoke(apiClass, api, "getBalanceAsync",
                new Class<?>[]{UUID.class, currencyClass}, playerId, currency);
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Object> depositAsync(UUID playerId, Object currency, double amount) {
        return (CompletableFuture<Object>) invoke(apiClass, api, "depositAsync",
                new Class<?>[]{UUID.class, currencyClass, double.class, operationContextClass},
                playerId, currency, amount, operationContext);
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Object> withdrawAsync(UUID playerId, Object currency, double amount) {
        return (CompletableFuture<Object>) invoke(apiClass, api, "withdrawAsync",
                new Class<?>[]{UUID.class, currencyClass, double.class, operationContextClass},
                playerId, currency, amount, operationContext);
    }

    private boolean operationSuccess(Object operationResult) {
        return Boolean.TRUE.equals(invoke(operationResult, "success"));
    }

    private static Object invoke(Object target, String methodName) {
        return invoke(target.getClass(), target, methodName, new Class<?>[0]);
    }

    private static Object invoke(Class<?> owner, Object target, String methodName) {
        return invoke(owner, target, methodName, new Class<?>[0]);
    }

    private static Object invoke(Class<?> owner, Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = owner.getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException("ExcellentEconomy API call failed: " + methodName, cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ExcellentEconomy API method not found: " + methodName, e);
        }
    }
}
