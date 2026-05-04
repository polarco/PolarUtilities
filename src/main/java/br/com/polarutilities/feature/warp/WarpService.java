package br.com.polarutilities.feature.warp;

import br.com.polarutilities.model.StoredLocation;
import br.com.polarutilities.storage.PluginStorage;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class WarpService {
    private static final Pattern WARP_NAME = Pattern.compile("[a-zA-Z0-9_-]{1,24}");
    private final PluginStorage storage;

    public WarpService(PluginStorage storage) {
        this.storage = storage;
    }

    public SaveResult setWarp(Player player, String rawName) {
        String name = normalize(rawName);
        if (!isValidName(name)) {
            return SaveResult.INVALID_NAME;
        }

        ConfigurationSection root = root(true);
        boolean created = !root.contains(name);
        StoredLocation.from(player.getLocation()).saveTo(root.createSection(name));
        storage.saveWarps();
        return created ? SaveResult.CREATED : SaveResult.UPDATED;
    }

    public boolean deleteWarp(String rawName) {
        String name = normalize(rawName);
        ConfigurationSection root = root(false);
        if (root == null || !root.contains(name)) {
            return false;
        }
        root.set(name, null);
        storage.saveWarps();
        return true;
    }

    public Optional<StoredLocation> warp(String rawName) {
        String name = normalize(rawName);
        ConfigurationSection root = root(false);
        if (root == null) {
            return Optional.empty();
        }
        return StoredLocation.fromSection(root.getConfigurationSection(name));
    }

    public Map<String, StoredLocation> warps() {
        Map<String, StoredLocation> warps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ConfigurationSection root = root(false);
        if (root == null) {
            return warps;
        }

        for (String key : root.getKeys(false)) {
            StoredLocation.fromSection(root.getConfigurationSection(key))
                .ifPresent(location -> warps.put(key, location));
        }
        return warps;
    }

    public String normalize(String rawName) {
        return rawName == null ? "" : rawName.trim().toLowerCase();
    }

    public boolean isValidName(String name) {
        return WARP_NAME.matcher(name).matches();
    }

    private ConfigurationSection root(boolean create) {
        ConfigurationSection root = storage.warps().getConfigurationSection("warps");
        if (root == null && create) {
            root = storage.warps().createSection("warps");
        }
        return root;
    }

    public enum SaveResult {
        CREATED,
        UPDATED,
        INVALID_NAME
    }
}
