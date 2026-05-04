package br.com.polarutilities;

import br.com.polarutilities.feature.PluginFeature;
import br.com.polarutilities.feature.admin.AdminFeature;
import br.com.polarutilities.feature.home.HomeFeature;
import br.com.polarutilities.feature.spawn.SpawnFeature;
import br.com.polarutilities.feature.tpa.TpaFeature;
import br.com.polarutilities.feature.update.UpdateCheckerFeature;
import br.com.polarutilities.feature.warp.WarpFeature;
import br.com.polarutilities.storage.PluginStorage;
import br.com.polarutilities.teleport.TeleportService;
import br.com.polarutilities.util.CommandRegistrar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public final class PolarUtilitiesPlugin extends JavaPlugin {
    private final List<PluginFeature> features = new ArrayList<>();
    private PluginStorage storage;
    private TeleportService teleportService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        storage = new PluginStorage(this);
        storage.load();

        teleportService = new TeleportService(this);
        getServer().getPluginManager().registerEvents(teleportService, this);

        CommandRegistrar registrar = new CommandRegistrar(this);
        UpdateCheckerFeature updateCheckerFeature = new UpdateCheckerFeature(this);

        features.add(new AdminFeature(this, registrar, storage, updateCheckerFeature));
        features.add(updateCheckerFeature);
        features.add(new TpaFeature(this, registrar, storage, teleportService));
        features.add(new HomeFeature(this, registrar, storage, teleportService));
        features.add(new WarpFeature(this, registrar, storage, teleportService));
        features.add(new SpawnFeature(this, registrar, storage, teleportService));

        for (PluginFeature feature : features) {
            feature.enable();
        }

        getLogger().info("PolarUtilities " + getPluginMeta().getVersion() + " habilitado.");
    }

    @Override
    public void onDisable() {
        List<PluginFeature> reverse = new ArrayList<>(features);
        Collections.reverse(reverse);
        for (PluginFeature feature : reverse) {
            feature.disable();
        }
        if (teleportService != null) {
            teleportService.cancelAll();
        }
        if (storage != null) {
            storage.saveAll();
        }
    }

    public PluginStorage storage() {
        return storage;
    }

    public TeleportService teleportService() {
        return teleportService;
    }
}
