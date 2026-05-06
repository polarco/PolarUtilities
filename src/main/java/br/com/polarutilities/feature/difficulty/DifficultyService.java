package br.com.polarutilities.feature.difficulty;

import br.com.polarutilities.storage.PluginStorage;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class DifficultyService {
    public static final String MODULE_ENABLED_PATH = "module.enabled";
    private static final String DEFAULT_DIFFICULTY_PATH = "difficulty.default-mode";
    private static final String DEFAULT_KEEP_INVENTORY_PATH = "difficulty.default-keep-inventory";
    private static final String DAMAGE_MULTIPLIER_PATH = "difficulty.damage-multipliers.";
    private static final String XP_MULTIPLIER_PATH = "difficulty.xp-multipliers.";

    private final JavaPlugin plugin;
    private final PluginStorage storage;

    public DifficultyService(JavaPlugin plugin, PluginStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public boolean isModuleEnabled() {
        return plugin.getConfig().getBoolean(MODULE_ENABLED_PATH, true);
    }

    public void reloadStorage() {
        storage.load();
    }

    public PlayerSettings settings(Player player) {
        rememberPlayerName(player);
        return settings(player.getUniqueId());
    }

    public PlayerSettings settings(UUID playerId) {
        ConfigurationSection section = playerSection(playerId, null, false);
        DifficultyMode defaultMode = defaultDifficulty();
        boolean defaultKeepInventory = plugin.getConfig().getBoolean(DEFAULT_KEEP_INVENTORY_PATH, false);
        if (section == null) {
            return new PlayerSettings(playerId, defaultMode, defaultKeepInventory);
        }

        DifficultyMode difficulty = DifficultyMode.fromConfig(section.getString("difficulty", defaultMode.name()));
        boolean keepInventory = section.getBoolean("keepInventory", defaultKeepInventory);
        return new PlayerSettings(playerId, difficulty, keepInventory);
    }

    public void setDifficulty(Player player, DifficultyMode difficulty) {
        ConfigurationSection section = playerSection(player.getUniqueId(), player.getName(), true);
        section.set("difficulty", difficulty.name());
        storage.savePlayers();
    }

    public boolean toggleKeepInventory(Player player) {
        PlayerSettings settings = settings(player.getUniqueId());
        boolean enabled = !settings.keepInventory();
        ConfigurationSection section = playerSection(player.getUniqueId(), player.getName(), true);
        section.set("keepInventory", enabled);
        storage.savePlayers();
        return enabled;
    }

    public double damageMultiplier(DifficultyMode difficulty) {
        return configuredMultiplier(DAMAGE_MULTIPLIER_PATH, difficulty, difficulty.defaultDamageMultiplier());
    }

    public double xpMultiplier(DifficultyMode difficulty) {
        return configuredMultiplier(XP_MULTIPLIER_PATH, difficulty, difficulty.defaultXpMultiplier());
    }

    private void rememberPlayerName(Player player) {
        ConfigurationSection section = playerSection(player.getUniqueId(), player.getName(), true);
        section.set("name", player.getName());
    }

    private DifficultyMode defaultDifficulty() {
        return DifficultyMode.fromConfig(plugin.getConfig().getString(DEFAULT_DIFFICULTY_PATH, DifficultyMode.NORMAL.name()));
    }

    private double configuredMultiplier(String rootPath, DifficultyMode difficulty, double fallback) {
        double value = plugin.getConfig().getDouble(rootPath + difficulty.configKey(), fallback);
        return Math.max(0.0D, value);
    }

    private ConfigurationSection playerSection(UUID playerId, String playerName, boolean create) {
        ConfigurationSection root = storage.players().getConfigurationSection("players");
        if (root == null) {
            if (!create) {
                return null;
            }
            root = storage.players().createSection("players");
        }

        ConfigurationSection section = root.getConfigurationSection(playerId.toString());
        if (section == null) {
            if (!create) {
                return null;
            }
            section = root.createSection(playerId.toString());
        }
        if (playerName != null) {
            section.set("name", playerName);
        }
        return section;
    }
}
