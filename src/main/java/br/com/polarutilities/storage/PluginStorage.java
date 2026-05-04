package br.com.polarutilities.storage;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginStorage {
    private final JavaPlugin plugin;
    private final File playersFile;
    private final File warpsFile;
    private final File spawnFile;
    private FileConfiguration players;
    private FileConfiguration warps;
    private FileConfiguration spawn;

    public PluginStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playersFile = new File(plugin.getDataFolder(), "players.yml");
        this.warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        this.spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
    }

    public void load() {
        ensureDataFolder();
        players = YamlConfiguration.loadConfiguration(playersFile);
        warps = YamlConfiguration.loadConfiguration(warpsFile);
        spawn = YamlConfiguration.loadConfiguration(spawnFile);
    }

    public FileConfiguration players() {
        return players;
    }

    public FileConfiguration warps() {
        return warps;
    }

    public FileConfiguration spawn() {
        return spawn;
    }

    public void savePlayers() {
        save(players, playersFile);
    }

    public void saveWarps() {
        save(warps, warpsFile);
    }

    public void saveSpawn() {
        save(spawn, spawnFile);
    }

    public void saveAll() {
        savePlayers();
        saveWarps();
        saveSpawn();
    }

    private void ensureDataFolder() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Nao foi possivel criar a pasta de dados.");
        }
    }

    private void save(FileConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Nao foi possivel salvar " + file.getName(), exception);
        }
    }
}
