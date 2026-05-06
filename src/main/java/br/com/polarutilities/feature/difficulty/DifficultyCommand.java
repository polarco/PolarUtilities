package br.com.polarutilities.feature.difficulty;

import br.com.polarutilities.util.Texts;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class DifficultyCommand {
    private static final List<String> PLAYER_SUBCOMMANDS = List.of("status");
    private static final List<String> ADMIN_SUBCOMMANDS = List.of("enable", "disable", "reload");

    private final JavaPlugin plugin;
    private final DifficultyService difficultyService;
    private final GUIService guiService;
    private final DeathTracker deathTracker;

    public DifficultyCommand(
        JavaPlugin plugin,
        DifficultyService difficultyService,
        GUIService guiService,
        DeathTracker deathTracker
    ) {
        this.plugin = plugin;
        this.difficultyService = difficultyService;
        this.guiService = guiService;
        this.deathTracker = deathTracker;
    }

    public boolean execute(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            return handleAdmin(sender, label, args);
        }

        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem abrir a GUI de dificuldade.");
            return true;
        }

        if (!difficultyService.isModuleEnabled()) {
            Texts.error(player, "O sistema de dificuldade esta desativado.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            guiService.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            guiService.sendStatus(player);
            return true;
        }

        Texts.error(player, "Use /" + label + " ou /" + label + " status.");
        return true;
    }

    public List<String> complete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> values = hasAdminPermission(sender)
                ? List.of("status", "admin")
                : PLAYER_SUBCOMMANDS;
            return startsWith(values, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin") && hasAdminPermission(sender)) {
            return startsWith(ADMIN_SUBCOMMANDS, args[1]);
        }
        return List.of();
    }

    private boolean handleAdmin(CommandSender sender, String label, String[] args) {
        if (!hasAdminPermission(sender)) {
            Texts.error(sender, "Voce nao tem permissao para administrar a dificuldade.");
            return true;
        }
        if (args.length != 2) {
            Texts.error(sender, "Use /" + label + " admin <enable|disable|reload>.");
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "enable" -> {
                plugin.getConfig().set(DifficultyService.MODULE_ENABLED_PATH, true);
                plugin.saveConfig();
                Texts.success(sender, "Modulo de dificuldade ativado!");
                yield true;
            }
            case "disable" -> {
                plugin.getConfig().set(DifficultyService.MODULE_ENABLED_PATH, false);
                plugin.saveConfig();
                deathTracker.clear();
                guiService.closeOpenMenus();
                Texts.error(sender, "Modulo de dificuldade desativado!");
                yield true;
            }
            case "reload" -> {
                plugin.reloadConfig();
                difficultyService.reloadStorage();
                deathTracker.clear();
                Texts.warning(sender, "Configuracao de dificuldade recarregada!");
                yield true;
            }
            default -> {
                Texts.error(sender, "Use /" + label + " admin <enable|disable|reload>.");
                yield true;
            }
        };
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return sender.hasPermission("polarutilities.difficulty.admin") || sender.hasPermission("polarutilities.admin");
    }

    private List<String> startsWith(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
            .filter(Objects::nonNull)
            .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
            .toList();
    }
}
