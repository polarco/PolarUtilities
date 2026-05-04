package br.com.polarutilities.feature.spawn;

import br.com.polarutilities.model.StoredLocation;
import br.com.polarutilities.storage.PluginStorage;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class SpawnService {
    private final PluginStorage storage;

    public SpawnService(PluginStorage storage) {
        this.storage = storage;
    }

    public void setSpawn(Location location) {
        ConfigurationSection section = storage.spawn().getConfigurationSection("spawn");
        if (section == null) {
            section = storage.spawn().createSection("spawn");
        }
        StoredLocation.from(location).saveTo(section);
        storage.saveSpawn();
    }

    public Optional<Location> spawn(World fallbackWorld, boolean useFallback) {
        Optional<Location> stored = StoredLocation.fromSection(storage.spawn().getConfigurationSection("spawn"))
            .flatMap(StoredLocation::toLocation);
        if (stored.isPresent() || !useFallback || fallbackWorld == null) {
            return stored;
        }
        return Optional.of(fallbackWorld.getSpawnLocation());
    }
}
