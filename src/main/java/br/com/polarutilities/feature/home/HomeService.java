package br.com.polarutilities.feature.home;

import br.com.polarutilities.model.StoredLocation;
import br.com.polarutilities.storage.PluginStorage;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HomeService {
    private static final Pattern HOME_NAME = Pattern.compile("[a-zA-Z0-9_-]{1,24}");
    private final JavaPlugin plugin;
    private final PluginStorage storage;

    public HomeService(JavaPlugin plugin, PluginStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public SaveResult setHome(Player player, String rawName) {
        String name = normalize(rawName);
        if (!isValidName(name)) {
            return SaveResult.INVALID_NAME;
        }

        Map<String, StoredLocation> homes = homes(player.getUniqueId());
        boolean isNewHome = !homes.containsKey(name);
        if (isNewHome && homes.size() >= maxHomes(player)) {
            return SaveResult.LIMIT_REACHED;
        }

        ConfigurationSection section = homesSection(player.getUniqueId(), player.getName(), true)
            .createSection(name);
        StoredLocation.from(player.getLocation()).saveTo(section);
        storage.savePlayers();
        return isNewHome ? SaveResult.CREATED : SaveResult.UPDATED;
    }

    public Optional<StoredLocation> home(UUID playerId, String rawName) {
        String name = normalize(rawName);
        ConfigurationSection section = homesSection(playerId, null, false);
        if (section == null) {
            return Optional.empty();
        }
        return StoredLocation.fromSection(section.getConfigurationSection(name));
    }

    public Map<String, StoredLocation> homes(UUID playerId) {
        Map<String, StoredLocation> homes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ConfigurationSection section = homesSection(playerId, null, false);
        if (section == null) {
            return homes;
        }

        for (String key : section.getKeys(false)) {
            StoredLocation.fromSection(section.getConfigurationSection(key))
                .ifPresent(location -> homes.put(key, location));
        }
        return homes;
    }

    public boolean deleteHome(UUID playerId, String rawName) {
        String name = normalize(rawName);
        ConfigurationSection section = homesSection(playerId, null, false);
        if (section == null || !section.contains(name)) {
            return false;
        }
        section.set(name, null);
        storage.savePlayers();
        return true;
    }

    public int maxHomes(Player player) {
        if (player.hasPermission("polarutilities.home.bypass-limit")) {
            return Integer.MAX_VALUE;
        }

        int configured = plugin.getConfig().getInt("homes.default-limit", 5);
        for (int limit = 100; limit > configured; limit--) {
            if (player.hasPermission("polarutilities.home.limit." + limit)) {
                return limit;
            }
        }
        return configured;
    }

    public boolean isValidName(String name) {
        return HOME_NAME.matcher(name).matches();
    }

    public String normalize(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "home";
        }
        return rawName.trim().toLowerCase();
    }

    private ConfigurationSection homesSection(UUID playerId, String playerName, boolean create) {
        ConfigurationSection root = storage.players().getConfigurationSection("players");
        if (root == null) {
            if (!create) {
                return null;
            }
            root = storage.players().createSection("players");
        }

        ConfigurationSection playerSection = root.getConfigurationSection(playerId.toString());
        if (playerSection == null) {
            if (!create) {
                return null;
            }
            playerSection = root.createSection(playerId.toString());
        }
        if (playerName != null) {
            playerSection.set("name", playerName);
        }

        ConfigurationSection homesSection = playerSection.getConfigurationSection("homes");
        if (homesSection == null && create) {
            homesSection = playerSection.createSection("homes");
        }
        return homesSection;
    }

    public enum SaveResult {
        CREATED,
        UPDATED,
        INVALID_NAME,
        LIMIT_REACHED
    }
}
