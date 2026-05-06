package br.com.polarutilities.feature.difficulty;

import br.com.polarutilities.util.Texts;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class GUIService {
    public static final int SLOT_EASY = 11;
    public static final int SLOT_NORMAL = 13;
    public static final int SLOT_HARD = 15;
    public static final int SLOT_KEEP_INVENTORY = 22;
    public static final int SLOT_STATUS = 26;

    private final JavaPlugin plugin;
    private final DifficultyService difficultyService;

    public GUIService(JavaPlugin plugin, DifficultyService difficultyService) {
        this.plugin = plugin;
        this.difficultyService = difficultyService;
    }

    public void open(Player player) {
        if (!difficultyService.isModuleEnabled()) {
            Texts.error(player, "O sistema de dificuldade esta desativado.");
            return;
        }

        PlayerSettings settings = difficultyService.settings(player);
        DifficultyMenuHolder holder = new DifficultyMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("Dificuldade"));
        holder.setInventory(inventory);

        ItemStack filler = filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(SLOT_EASY, difficultyItem(DifficultyMode.EASY, settings));
        inventory.setItem(SLOT_NORMAL, difficultyItem(DifficultyMode.NORMAL, settings));
        inventory.setItem(SLOT_HARD, difficultyItem(DifficultyMode.HARD, settings));
        inventory.setItem(SLOT_KEEP_INVENTORY, keepInventoryItem(settings));
        inventory.setItem(SLOT_STATUS, statusItem(settings));
        player.openInventory(inventory);
    }

    public void sendStatus(Player player) {
        PlayerSettings settings = difficultyService.settings(player);
        Texts.info(player, "Dificuldade: " + settings.difficulty().name()
            + " | Keep Inventory: " + (settings.keepInventory() ? "ON" : "OFF"));
    }

    public void closeOpenMenus() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof DifficultyMenuHolder) {
                player.closeInventory();
            }
        }
    }

    private ItemStack difficultyItem(DifficultyMode mode, PlayerSettings settings) {
        boolean selected = settings.difficulty() == mode;
        List<Component> lore = new ArrayList<>();
        switch (mode) {
            case EASY -> {
                lore.add(Component.text("Menos dano de mobs", NamedTextColor.GRAY));
                lore.add(Component.text("Menos XP", NamedTextColor.GRAY));
                lore.add(Component.text("Ideal para casual", NamedTextColor.DARK_GRAY));
            }
            case NORMAL -> lore.add(Component.text("Dificuldade padrao", NamedTextColor.GRAY));
            case HARD -> {
                lore.add(Component.text("Mais dano de mobs", NamedTextColor.GRAY));
                lore.add(Component.text("Mais XP e recompensas", NamedTextColor.GRAY));
                lore.add(Component.text("Maior risco = maior recompensa", NamedTextColor.DARK_GRAY));
            }
        }
        if (selected) {
            lore.add(Component.empty());
            lore.add(Component.text("Selecionado", NamedTextColor.GREEN));
        }

        return namedItem(
            switch (mode) {
                case EASY -> Material.LIME_WOOL;
                case NORMAL -> Material.WHITE_WOOL;
                case HARD -> Material.RED_WOOL;
            },
            Component.text("Modo " + mode.name(), nameColor(mode)).decoration(TextDecoration.BOLD, true),
            lore,
            selected
        );
    }

    private ItemStack keepInventoryItem(PlayerSettings settings) {
        boolean enabled = settings.keepInventory();
        return namedItem(
            Material.CHEST,
            Component.text("Keep Inventory", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true),
            List.of(
                Component.text("Status: ", NamedTextColor.GRAY)
                    .append(Component.text(enabled ? "Ativado" : "Desativado", enabled ? NamedTextColor.GREEN : NamedTextColor.RED)),
                Component.text("Clique para alternar", NamedTextColor.DARK_GRAY)
            ),
            enabled
        );
    }

    private ItemStack statusItem(PlayerSettings settings) {
        return namedItem(
            Material.BOOK,
            Component.text("Seu Status", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true),
            List.of(
                Component.text("Dificuldade: ", NamedTextColor.GRAY)
                    .append(Component.text(settings.difficulty().name(), nameColor(settings.difficulty()))),
                Component.text("Keep Inventory: ", NamedTextColor.GRAY)
                    .append(Component.text(settings.keepInventory() ? "ON" : "OFF", settings.keepInventory() ? NamedTextColor.GREEN : NamedTextColor.RED))
            ),
            false
        );
    }

    private ItemStack filler() {
        return namedItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of(), false);
    }

    private ItemStack namedItem(Material material, Component name, List<Component> lore, boolean glow) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream()
            .map(line -> line.decoration(TextDecoration.ITALIC, false))
            .toList());
        if (glow) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private NamedTextColor nameColor(DifficultyMode difficulty) {
        return switch (difficulty) {
            case EASY -> NamedTextColor.GREEN;
            case NORMAL -> NamedTextColor.WHITE;
            case HARD -> NamedTextColor.RED;
        };
    }
}
