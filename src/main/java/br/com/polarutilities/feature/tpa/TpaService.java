package br.com.polarutilities.feature.tpa;

import br.com.polarutilities.storage.PluginStorage;
import br.com.polarutilities.teleport.TeleportService;
import br.com.polarutilities.util.Texts;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TpaService {
    private final JavaPlugin plugin;
    private final PluginStorage storage;
    private final TeleportService teleportService;
    private final Map<UUID, Map<UUID, TpaRequest>> incoming = new HashMap<>();

    public TpaService(JavaPlugin plugin, PluginStorage storage, TeleportService teleportService) {
        this.plugin = plugin;
        this.storage = storage;
        this.teleportService = teleportService;
    }

    public void sendRequest(Player requester, Player target, TpaRequestType type) {
        boolean selfRequest = requester.getUniqueId().equals(target.getUniqueId());
        if (selfRequest && !plugin.getConfig().getBoolean("tpa.allow-self-request", false)) {
            Texts.error(requester, "Voce nao pode enviar TPA para si mesmo.");
            return;
        }
        if (!isReceivingRequests(target)) {
            Texts.error(requester, target.getName() + " desativou pedidos de TPA.");
            return;
        }

        TpaRequest request = new TpaRequest(
            requester.getUniqueId(),
            requester.getName(),
            target.getUniqueId(),
            target.getName(),
            type,
            Instant.now()
        );
        incoming.computeIfAbsent(target.getUniqueId(), ignored -> new HashMap<>())
            .put(requester.getUniqueId(), request);

        long timeout = plugin.getConfig().getLong("tpa.request-timeout-seconds", 60);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> expire(request), timeout * 20L);

        Texts.success(requester, "Pedido enviado para " + target.getName() + ".");
        sendClickableRequest(target, request, timeout);
    }

    public void accept(Player target, String requesterName) {
        Optional<TpaRequest> found = findRequest(target, requesterName);
        if (found.isEmpty()) {
            Texts.error(target, "Nenhum pedido de TPA encontrado.");
            return;
        }

        TpaRequest request = found.get();
        remove(request);
        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null || !requester.isOnline()) {
            Texts.error(target, request.requesterName() + " nao esta mais online.");
            return;
        }

        if (request.type() == TpaRequestType.TO_TARGET) {
            Location location = target.getLocation();
            Texts.success(target, "Voce aceitou o pedido de " + requester.getName() + ".");
            Texts.success(requester, target.getName() + " aceitou seu pedido.");
            teleportService.teleport(requester, location, "TPA para " + target.getName());
        } else {
            Location location = requester.getLocation();
            Texts.success(target, "Voce aceitou ir ate " + requester.getName() + ".");
            Texts.success(requester, target.getName() + " aceitou vir ate voce.");
            teleportService.teleport(target, location, "TPA ate " + requester.getName());
        }
    }

    public void deny(Player target, String requesterName) {
        Optional<TpaRequest> found = findRequest(target, requesterName);
        if (found.isEmpty()) {
            Texts.error(target, "Nenhum pedido de TPA encontrado.");
            return;
        }

        TpaRequest request = found.get();
        remove(request);
        Player requester = Bukkit.getPlayer(request.requesterId());
        Texts.warning(target, "Pedido de " + request.requesterName() + " recusado.");
        if (requester != null && requester.isOnline()) {
            Texts.error(requester, target.getName() + " recusou seu pedido de TPA.");
        }
    }

    public void cancelOutgoing(Player requester, String targetName) {
        int removed = 0;
        for (Map<UUID, TpaRequest> requests : incoming.values()) {
            for (TpaRequest request : Map.copyOf(requests).values()) {
                boolean sameRequester = request.requesterId().equals(requester.getUniqueId());
                boolean sameTarget = targetName == null || request.targetName().equalsIgnoreCase(targetName);
                if (sameRequester && sameTarget) {
                    requests.remove(request.requesterId());
                    removed++;
                    Player target = Bukkit.getPlayer(request.targetId());
                    if (target != null && target.isOnline()) {
                        Texts.warning(target, requester.getName() + " cancelou o pedido de TPA.");
                    }
                }
            }
        }

        if (removed == 0) {
            Texts.error(requester, "Nenhum pedido enviado encontrado.");
            return;
        }
        Texts.success(requester, removed == 1 ? "Pedido cancelado." : removed + " pedidos cancelados.");
    }

    public boolean toggleReceiving(Player player) {
        boolean enabled = !isReceivingRequests(player);
        ConfigurationSection section = playerSection(player.getUniqueId());
        section.set("name", player.getName());
        section.set("tpa-enabled", enabled);
        storage.savePlayers();
        return enabled;
    }

    public boolean isReceivingRequests(Player player) {
        return storage.players().getBoolean("players." + player.getUniqueId() + ".tpa-enabled", true);
    }

    public void removeInvolving(UUID playerId) {
        incoming.remove(playerId);
        for (Map<UUID, TpaRequest> requests : incoming.values()) {
            requests.remove(playerId);
        }
    }

    private void sendClickableRequest(Player target, TpaRequest request, long timeout) {
        String action = request.type() == TpaRequestType.TO_TARGET
            ? "quer teleportar ate voce"
            : "quer que voce va ate ele";

        Component message = Component.text(request.requesterName() + " " + action + ". ", NamedTextColor.YELLOW)
            .append(Texts.button("Aceitar", "/tpaccept " + request.requesterName(), NamedTextColor.GREEN, "Aceitar pedido de TPA"))
            .append(Texts.separator())
            .append(Texts.button("Negar", "/tpdeny " + request.requesterName(), NamedTextColor.RED, "Negar pedido de TPA"))
            .append(Component.text(" Expira em " + timeout + "s.", NamedTextColor.DARK_GRAY));
        Texts.send(target, message);
    }

    private Optional<TpaRequest> findRequest(Player target, String requesterName) {
        Map<UUID, TpaRequest> requests = incoming.get(target.getUniqueId());
        if (requests == null || requests.isEmpty()) {
            return Optional.empty();
        }

        if (requesterName != null && !requesterName.isBlank()) {
            return requests.values().stream()
                .filter(request -> request.requesterName().equalsIgnoreCase(requesterName))
                .findFirst();
        }

        return requests.values().stream()
            .max(Comparator.comparing(TpaRequest::createdAt));
    }

    private void expire(TpaRequest request) {
        Map<UUID, TpaRequest> requests = incoming.get(request.targetId());
        if (requests == null || !request.equals(requests.get(request.requesterId()))) {
            return;
        }
        requests.remove(request.requesterId());
        Player requester = Bukkit.getPlayer(request.requesterId());
        Player target = Bukkit.getPlayer(request.targetId());
        if (requester != null && requester.isOnline()) {
            Texts.error(requester, "Seu pedido de TPA para " + request.targetName() + " expirou.");
        }
        if (target != null && target.isOnline()) {
            Texts.warning(target, "Pedido de TPA de " + request.requesterName() + " expirou.");
        }
    }

    private void remove(TpaRequest request) {
        Map<UUID, TpaRequest> requests = incoming.get(request.targetId());
        if (requests != null) {
            requests.remove(request.requesterId());
        }
    }

    private ConfigurationSection playerSection(UUID playerId) {
        ConfigurationSection root = storage.players().getConfigurationSection("players");
        if (root == null) {
            root = storage.players().createSection("players");
        }
        ConfigurationSection section = root.getConfigurationSection(playerId.toString());
        if (section == null) {
            section = root.createSection(playerId.toString());
        }
        return section;
    }
}
