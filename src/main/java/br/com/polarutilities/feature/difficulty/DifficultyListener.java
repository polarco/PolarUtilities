package br.com.polarutilities.feature.difficulty;

import br.com.polarutilities.util.Texts;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class DifficultyListener implements Listener {
    private final DifficultyService difficultyService;
    private final XpPenaltyService xpPenaltyService;

    public DifficultyListener(DifficultyService difficultyService, XpPenaltyService xpPenaltyService) {
        this.difficultyService = difficultyService;
        this.xpPenaltyService = xpPenaltyService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!difficultyService.isModuleEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event instanceof EntityDamageByEntityEvent damageByEntityEvent && isPvpDamage(damageByEntityEvent)) {
            return;
        }

        PlayerSettings settings = difficultyService.settings(player.getUniqueId());
        double multiplier = difficultyService.damageMultiplier(settings.difficulty());
        if (multiplier == 1.0D) {
            return;
        }
        event.setDamage(event.getDamage() * multiplier);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!difficultyService.isModuleEnabled()) {
            return;
        }

        PlayerSettings settings = difficultyService.settings(event.getEntity().getUniqueId());
        if (!settings.keepInventory()) {
            return;
        }

        event.setKeepInventory(true);
        event.getDrops().clear();
        XpPenaltyService.XpPenaltyResult result = xpPenaltyService.applyPenalty(event);
        if (result.lostExp() > 0) {
            Texts.error(event.getEntity(), "Voce perdeu " + result.lossPercent() + "% do seu XP (" + result.lostExp() + " XP).");
        } else {
            Texts.warning(event.getEntity(), "Keep Inventory aplicado sem perda de XP.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!difficultyService.isModuleEnabled()) {
            return;
        }
        if (event instanceof PlayerDeathEvent) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        PlayerSettings settings = difficultyService.settings(killer.getUniqueId());
        double multiplier = difficultyService.xpMultiplier(settings.difficulty());
        if (multiplier == 1.0D) {
            return;
        }
        event.setDroppedExp((int) Math.round(event.getDroppedExp() * multiplier));
    }

    private boolean isPvpDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return true;
        }
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Player;
        }
        if (event.getDamager() instanceof Tameable tameable) {
            return tameable.getOwner() instanceof Player;
        }
        return false;
    }
}
