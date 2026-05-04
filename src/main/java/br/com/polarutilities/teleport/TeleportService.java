package br.com.polarutilities.teleport;

import br.com.polarutilities.util.Texts;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class TeleportService implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, PendingTeleport> pendingTeleports = new HashMap<>();

    public TeleportService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void teleport(Player player, Location target, String reason) {
        cancel(player, false);

        int delaySeconds = plugin.getConfig().getInt("teleport.delay-seconds", 3);
        if (delaySeconds <= 0) {
            performTeleport(player, target, reason);
            return;
        }

        Location start = player.getLocation().clone();
        Texts.warning(player, "Teleportando em " + delaySeconds + "s. Nao se mova.");
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingTeleports.remove(player.getUniqueId());
            performTeleport(player, target, reason);
        }, delaySeconds * 20L);
        pendingTeleports.put(player.getUniqueId(), new PendingTeleport(start, task));
    }

    public void cancelAll() {
        for (PendingTeleport pending : pendingTeleports.values()) {
            pending.task().cancel();
        }
        pendingTeleports.clear();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("teleport.cancel-on-move", true)) {
            return;
        }
        PendingTeleport pending = pendingTeleports.get(event.getPlayer().getUniqueId());
        if (pending == null || sameBlock(pending.start(), event.getTo())) {
            return;
        }
        cancel(event.getPlayer(), true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer(), false);
    }

    private void performTeleport(Player player, Location target, String reason) {
        if (!player.isOnline()) {
            return;
        }
        boolean success = player.teleport(target);
        if (success) {
            Texts.success(player, "Teleportado para " + reason + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.1f);
        } else {
            Texts.error(player, "Nao foi possivel teleportar agora.");
        }
    }

    private void cancel(Player player, boolean notify) {
        PendingTeleport pending = pendingTeleports.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }
        pending.task().cancel();
        if (notify) {
            Texts.error(player, "Teleporte cancelado porque voce se moveu.");
        }
    }

    private boolean sameBlock(Location a, Location b) {
        if (a.getWorld() == null || b.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            return false;
        }
        return a.getBlockX() == b.getBlockX()
            && a.getBlockY() == b.getBlockY()
            && a.getBlockZ() == b.getBlockZ();
    }

    private record PendingTeleport(Location start, BukkitTask task) {
    }
}
