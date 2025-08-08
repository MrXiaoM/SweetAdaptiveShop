package top.mrxiaom.sweet.adaptiveshop.func.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.func.entry.ItemTemplate;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@AutoRegister
public class TemplateManager extends AbstractModule {
    DateTimeFormatter dateTimeFormat;
    String dateTimeInfinite;
    Map<String, ItemTemplate> itemTemplateMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public TemplateManager(SweetAdaptiveShop plugin) {
        super(plugin);
    }

    public Collection<String> itemTemplates() {
        return itemTemplateMap.keySet();
    }

    public String format(@Nullable LocalDateTime time) {
        if (time == null) {
            return dateTimeInfinite;
        }
        return time.format(dateTimeFormat);
    }

    @Nullable
    public ItemTemplate getTemplate(String templateId) {
        return itemTemplateMap.get(templateId);
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        File file = new File(plugin.getDataFolder(), "template.yml");
        if (!file.exists()) {
            plugin.saveResource("template.yml", file);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        try {
            dateTimeFormat = DateTimeFormatter.ofPattern(config.getString("datetime.format", "yyyy/MM/dd HH:mm:ss"));
        } catch (IllegalArgumentException e) {
            dateTimeFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            warn("[template.yml] datetime.format 格式有误", e);
        }
        dateTimeInfinite = config.getString("datetime.infinite", "永久");

        itemTemplateMap.clear();
        ConfigurationSection section = config.getConfigurationSection("templates");
        if (section != null) for (String id : section.getKeys(false)) {
            Material material = Util.valueOr(Material.class, section.getString(id + ".material"), Material.PAPER);
            String display = section.getString(id + ".display", "");
            List<String> lore = section.getStringList(id + ".lore");
            boolean glow = section.getBoolean(id + ".glow");
            boolean unique = section.getBoolean(id + ".unique");
            Integer customModelData = section.contains(id + ".custom-model-data") ? section.getInt(id + ".custom-model-data") : null;
            Map<String, String> nbtStrings = new HashMap<>();
            ConfigurationSection section1 = section.getConfigurationSection(id + ".nbt-strings");
            if (section1 != null) for (String key : section1.getKeys(false)) {
                nbtStrings.put(key, section1.getString(key, ""));
            }
            Map<String, String> nbtInts = new HashMap<>();
            section1 = section.getConfigurationSection(id + ".nbt-ints");
            if (section1 != null) for (String key : section1.getKeys(false)) {
                nbtInts.put(key, section1.getString(key, ""));
            }
            itemTemplateMap.put(id, new ItemTemplate(id, material, display, lore, glow, unique, customModelData, nbtStrings, nbtInts));
        }
        info("加载了 " + itemTemplateMap.size() + " 个物品模板");
    }

    public static TemplateManager inst() {
        return instanceOf(TemplateManager.class);
    }
}
