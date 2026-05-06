package br.com.polarutilities.feature.difficulty;

import br.com.polarutilities.feature.PluginFeature;
import br.com.polarutilities.storage.PluginStorage;
import br.com.polarutilities.util.CommandRegistrar;
import org.bukkit.plugin.java.JavaPlugin;

public final class DifficultyFeature implements PluginFeature {
    private final JavaPlugin plugin;
    private final CommandRegistrar registrar;
    private final DeathTracker deathTracker;
    private final DifficultyService difficultyService;
    private final GUIService guiService;
    private final XpPenaltyService xpPenaltyService;

    public DifficultyFeature(JavaPlugin plugin, CommandRegistrar registrar, PluginStorage storage) {
        this.plugin = plugin;
        this.registrar = registrar;
        this.deathTracker = new DeathTracker();
        this.difficultyService = new DifficultyService(plugin, storage);
        this.guiService = new GUIService(plugin, difficultyService);
        this.xpPenaltyService = new XpPenaltyService(plugin, deathTracker);
    }

    @Override
    public void enable() {
        DifficultyCommand command = new DifficultyCommand(plugin, difficultyService, guiService, deathTracker);
        registrar.register("dificuldade", command::execute, command::complete);
        plugin.getServer().getPluginManager().registerEvents(new GUIListener(difficultyService, guiService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DifficultyListener(difficultyService, xpPenaltyService), plugin);
    }

    @Override
    public void disable() {
        deathTracker.clear();
    }
}
