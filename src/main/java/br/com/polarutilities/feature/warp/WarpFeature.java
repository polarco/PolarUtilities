package br.com.polarutilities.feature.warp;

import br.com.polarutilities.feature.PluginFeature;
import br.com.polarutilities.model.StoredLocation;
import br.com.polarutilities.storage.PluginStorage;
import br.com.polarutilities.teleport.TeleportService;
import br.com.polarutilities.util.CommandRegistrar;
import br.com.polarutilities.util.Texts;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class WarpFeature implements PluginFeature {
    private final JavaPlugin plugin;
    private final CommandRegistrar registrar;
    private final WarpService warpService;
    private final TeleportService teleportService;

    public WarpFeature(
        JavaPlugin plugin,
        CommandRegistrar registrar,
        PluginStorage storage,
        TeleportService teleportService
    ) {
        this.plugin = plugin;
        this.registrar = registrar;
        this.warpService = new WarpService(storage);
        this.teleportService = teleportService;
    }

    @Override
    public void enable() {
        registrar.register("setwarp", this::setWarp);
        registrar.register("delwarp", this::deleteWarp, this::completeWarps);
        registrar.register("warp", this::warp, this::completeWarps);
        registrar.register("warps", this::warps);
    }

    private boolean setWarp(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem criar warps.");
            return true;
        }
        if (args.length != 1) {
            Texts.error(player, "Use /setwarp <nome>.");
            return true;
        }

        WarpService.SaveResult result = warpService.setWarp(player, args[0]);
        switch (result) {
            case CREATED -> Texts.success(player, "Warp " + warpService.normalize(args[0]) + " criada.");
            case UPDATED -> Texts.success(player, "Warp " + warpService.normalize(args[0]) + " atualizada.");
            case INVALID_NAME -> Texts.error(player, "Nome invalido. Use letras, numeros, _ ou - com ate 24 caracteres.");
        }
        return true;
    }

    private boolean deleteWarp(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (args.length != 1) {
            Texts.error(sender, "Use /delwarp <nome>.");
            return true;
        }

        String name = warpService.normalize(args[0]);
        if (!warpService.deleteWarp(name)) {
            Texts.error(sender, "Warp " + name + " nao encontrada.");
            return true;
        }
        Texts.success(sender, "Warp " + name + " removida.");
        return true;
    }

    private boolean warp(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem usar warps.");
            return true;
        }
        if (args.length != 1) {
            Texts.error(player, "Use /warp <nome>.");
            return true;
        }

        String name = warpService.normalize(args[0]);
        warpService.warp(name)
            .flatMap(StoredLocation::toLocation)
            .ifPresentOrElse(
                location -> teleportService.teleport(player, location, "warp " + name),
                () -> Texts.error(player, "Warp " + name + " nao encontrada ou mundo nao carregado.")
            );
        return true;
    }

    private boolean warps(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        Map<String, StoredLocation> warps = warpService.warps();
        if (warps.isEmpty()) {
            Texts.error(sender, "Nenhum warp foi criado ainda.");
            return true;
        }

        String title = plugin.getConfig().getString("warps.list-title", "Warps disponiveis");
        Texts.info(sender, title);
        for (Map.Entry<String, StoredLocation> entry : warps.entrySet()) {
            Component line = Component.text("- ", NamedTextColor.DARK_GRAY)
                .append(Texts.commandLink(entry.getKey(), "/warp " + entry.getKey()))
                .append(Component.text(" em " + entry.getValue().shortText(), NamedTextColor.GRAY));
            sender.sendMessage(line);
        }
        return true;
    }

    private List<String> completeWarps(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return warpService.warps().keySet().stream()
            .filter(warp -> warp.startsWith(prefix))
            .toList();
    }
}
