package br.com.polarutilities.feature.spawn;

import br.com.polarutilities.feature.PluginFeature;
import br.com.polarutilities.storage.PluginStorage;
import br.com.polarutilities.teleport.TeleportService;
import br.com.polarutilities.util.CommandRegistrar;
import br.com.polarutilities.util.Texts;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class SpawnFeature implements PluginFeature {
    private final JavaPlugin plugin;
    private final CommandRegistrar registrar;
    private final SpawnService spawnService;
    private final TeleportService teleportService;

    public SpawnFeature(
        JavaPlugin plugin,
        CommandRegistrar registrar,
        PluginStorage storage,
        TeleportService teleportService
    ) {
        this.plugin = plugin;
        this.registrar = registrar;
        this.spawnService = new SpawnService(storage);
        this.teleportService = teleportService;
    }

    @Override
    public void enable() {
        registrar.register("setspawn", this::setSpawn);
        registrar.register("spawn", this::spawn);
    }

    private boolean setSpawn(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem definir o spawn.");
            return true;
        }

        spawnService.setSpawn(player.getLocation());
        Texts.success(player, "Spawn principal definido na sua posicao atual.");
        return true;
    }

    private boolean spawn(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem usar /spawn.");
            return true;
        }

        boolean useFallback = plugin.getConfig().getBoolean("spawn.fallback-to-world-spawn", true);
        spawnService.spawn(player.getWorld(), useFallback)
            .ifPresentOrElse(
                location -> teleportService.teleport(player, location, "spawn"),
                () -> Texts.error(player, "Spawn ainda nao foi definido.")
            );
        return true;
    }
}
