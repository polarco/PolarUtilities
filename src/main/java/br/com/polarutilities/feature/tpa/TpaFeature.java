package br.com.polarutilities.feature.tpa;

import br.com.polarutilities.feature.PluginFeature;
import br.com.polarutilities.storage.PluginStorage;
import br.com.polarutilities.teleport.TeleportService;
import br.com.polarutilities.util.CommandRegistrar;
import br.com.polarutilities.util.PlayerSelectors;
import br.com.polarutilities.util.Texts;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class TpaFeature implements PluginFeature, Listener {
    private final JavaPlugin plugin;
    private final CommandRegistrar registrar;
    private final TpaService service;

    public TpaFeature(
        JavaPlugin plugin,
        CommandRegistrar registrar,
        PluginStorage storage,
        TeleportService teleportService
    ) {
        this.plugin = plugin;
        this.registrar = registrar;
        this.service = new TpaService(plugin, storage, teleportService);
    }

    @Override
    public void enable() {
        registrar.register("tpa", (sender, command, label, args) ->
            sendTeleportRequest(sender, args, TpaRequestType.TO_TARGET), this::completePlayers);
        registrar.register("tpahere", (sender, command, label, args) ->
            sendTeleportRequest(sender, args, TpaRequestType.TO_REQUESTER), this::completePlayers);
        registrar.register("tpaccept", this::accept, this::completePlayers);
        registrar.register("tpdeny", this::deny, this::completePlayers);
        registrar.register("tpacancel", this::cancel, this::completePlayers);
        registrar.register("tptoggle", this::toggle);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.removeInvolving(event.getPlayer().getUniqueId());
    }

    private boolean sendTeleportRequest(CommandSender sender, String[] args, TpaRequestType type) {
        if (!(sender instanceof Player requester)) {
            Texts.error(sender, "Apenas jogadores podem usar este comando.");
            return true;
        }
        if (args.length != 1) {
            Texts.error(sender, "Use /" + (type == TpaRequestType.TO_TARGET ? "tpa" : "tpahere") + " <jogador>.");
            return true;
        }

        Player target = PlayerSelectors.onlineByName(args[0]);
        if (target == null) {
            Texts.error(requester, "Jogador nao encontrado.");
            return true;
        }

        service.sendRequest(requester, target, type);
        return true;
    }

    private boolean accept(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem aceitar TPA.");
            return true;
        }
        service.accept(player, args.length > 0 ? args[0] : null);
        return true;
    }

    private boolean deny(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem negar TPA.");
            return true;
        }
        service.deny(player, args.length > 0 ? args[0] : null);
        return true;
    }

    private boolean cancel(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem cancelar TPA.");
            return true;
        }
        service.cancelOutgoing(player, args.length > 0 ? args[0] : null);
        return true;
    }

    private boolean toggle(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem usar /tptoggle.");
            return true;
        }
        boolean enabled = service.toggleReceiving(player);
        if (enabled) {
            Texts.success(player, "Voce voltou a receber pedidos de TPA.");
        } else {
            Texts.warning(player, "Voce nao recebera pedidos de TPA.");
        }
        return true;
    }

    private List<String> completePlayers(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            boolean includeSelf = plugin.getConfig().getBoolean("tpa.allow-self-request", false);
            return PlayerSelectors.onlinePlayerNames(sender, args[0], includeSelf);
        }
        return List.of();
    }
}
