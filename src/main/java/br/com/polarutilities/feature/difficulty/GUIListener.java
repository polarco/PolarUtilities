package br.com.polarutilities.feature.difficulty;

import br.com.polarutilities.util.Texts;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class GUIListener implements Listener {
    private final DifficultyService difficultyService;
    private final GUIService guiService;

    public GUIListener(DifficultyService difficultyService, GUIService guiService) {
        this.difficultyService = difficultyService;
        this.guiService = guiService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DifficultyMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player) || !player.getUniqueId().equals(holder.ownerId())) {
            return;
        }

        if (!difficultyService.isModuleEnabled()) {
            player.closeInventory();
            Texts.error(player, "O sistema de dificuldade esta desativado.");
            return;
        }

        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        switch (event.getRawSlot()) {
            case GUIService.SLOT_EASY -> setDifficulty(player, DifficultyMode.EASY);
            case GUIService.SLOT_NORMAL -> setDifficulty(player, DifficultyMode.NORMAL);
            case GUIService.SLOT_HARD -> setDifficulty(player, DifficultyMode.HARD);
            case GUIService.SLOT_KEEP_INVENTORY -> toggleKeepInventory(player);
            case GUIService.SLOT_STATUS -> {
                playClick(player);
                guiService.sendStatus(player);
                guiService.open(player);
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof DifficultyMenuHolder)) {
            return;
        }
        event.setCancelled(true);

        if (!difficultyService.isModuleEnabled() && event.getWhoClicked() instanceof Player player) {
            player.closeInventory();
            Texts.error(player, "O sistema de dificuldade esta desativado.");
        }
    }

    private void setDifficulty(Player player, DifficultyMode difficulty) {
        difficultyService.setDifficulty(player, difficulty);
        playClick(player);
        Texts.success(player, "Dificuldade alterada para " + difficulty.name() + "!");
        guiService.open(player);
    }

    private void toggleKeepInventory(Player player) {
        boolean enabled = difficultyService.toggleKeepInventory(player);
        playClick(player);
        if (enabled) {
            Texts.warning(player, "Keep Inventory ativado!");
        } else {
            Texts.error(player, "Keep Inventory desativado!");
        }
        guiService.open(player);
    }

    private void playClick(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8F, 1.15F);
    }
}
