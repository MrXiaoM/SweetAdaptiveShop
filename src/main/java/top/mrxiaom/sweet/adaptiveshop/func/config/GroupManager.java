package top.mrxiaom.sweet.adaptiveshop.func.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.adaptiveshop.SweetAdaptiveShop;
import top.mrxiaom.sweet.adaptiveshop.func.AbstractModule;
import top.mrxiaom.sweet.adaptiveshop.func.entry.Group;

import java.io.File;
import java.util.*;

@AutoRegister
public class GroupManager extends AbstractModule {
    private final Map<String, Group> groups = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public GroupManager(SweetAdaptiveShop plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        File file = new File(plugin.getDataFolder(), "groups.yml");
        if (!file.exists()) {
            plugin.saveResource("groups.yml", file);
        }
        groups.clear();
        YamlConfiguration configGroups = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = configGroups.getConfigurationSection("groups");
        if (section != null) for (String groupName : section.getKeys(false)) {
            Group loaded = Group.load(section, groupName);
            groups.put(groupName, loaded);
        }
        info("加载了 " + groups.size() + " 个分组");
    }

    @Nullable
    public Group get(String name) {
        return groups.get(name);
    }

    public Collection<Group> groups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    public Set<String> groups(Permissible p) {
        Set<String> set = new HashSet<>();
        for (Group group : groups.values()) {
            if (group.hasPermission(p)) {
                set.add(group.id);
            }
        }
        return set;
    }

    public static GroupManager inst() {
        return instanceOf(GroupManager.class);
    }
}
