package br.com.polarutilities.feature.menu;

import br.com.polarutilities.feature.PluginFeature;
import br.com.polarutilities.feature.difficulty.DifficultyMode;
import br.com.polarutilities.storage.PluginStorage;
import br.com.polarutilities.util.CommandRegistrar;
import br.com.polarutilities.util.Texts;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class MenuFeature implements PluginFeature, Listener {
    private static final int SIZE = 45;
    private static final String ACTION_HOMES = "homes";
    private static final String ACTION_WARPS = "warps";
    private static final String ACTION_SPAWN = "spawn";
    private static final String ACTION_DIFFICULTY = "difficulty";
    private static final String ACTION_TPA_TOGGLE = "tpa_toggle";
    private static final String ACTION_STATUS = "status";
    private static final String ACTION_ADMIN_SETTINGS = "admin_settings";
    private static final String ACTION_UPDATES = "updates";
    private static final String ACTION_CLOSE = "close";

    private final JavaPlugin plugin;
    private final CommandRegistrar registrar;
    private final PluginStorage storage;
    private final NamespacedKey actionKey;

    public MenuFeature(JavaPlugin plugin, CommandRegistrar registrar, PluginStorage storage) {
        this.plugin = plugin;
        this.registrar = registrar;
        this.storage = storage;
        this.actionKey = new NamespacedKey(plugin, "menu_action");
    }

    @Override
    public void enable() {
        registrar.register("menu", this::menu);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player) || !player.getUniqueId().equals(holder.ownerId())) {
            return;
        }
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) {
            return;
        }

        String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) {
            return;
        }
        handleAction(player, action);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private boolean menu(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem abrir o menu.");
            return true;
        }
        openMenu(player);
        return true;
    }

    private void openMenu(Player player) {
        MenuHolder holder = new MenuHolder(player.getUniqueId());
        String title = plugin.getConfig().getString("menu.gui-title", "Menu PolarUtilities");
        Inventory inventory = Bukkit.createInventory(holder, SIZE, Component.text(title));
        holder.setInventory(inventory);

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of(), null);
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        if (player.hasPermission("polarutilities.home.use")) {
            inventory.setItem(10, actionItem(Material.PLAYER_HEAD, ACTION_HOMES, "Homes", NamedTextColor.AQUA, List.of(
                Component.text("Salvas: " + homesCount(player), NamedTextColor.GRAY),
                Component.text("Clique para abrir suas homes", NamedTextColor.GREEN)
            )));
        }

        if (player.hasPermission("polarutilities.warp.use")) {
            inventory.setItem(12, actionItem(Material.COMPASS, ACTION_WARPS, "Warps", NamedTextColor.GOLD, List.of(
                Component.text("Disponiveis: " + warpsCount(), NamedTextColor.GRAY),
                Component.text("Clique para listar warps", NamedTextColor.GREEN)
            )));
        }

        if (player.hasPermission("polarutilities.spawn.use")) {
            inventory.setItem(14, actionItem(Material.BEACON, ACTION_SPAWN, "Spawn", NamedTextColor.GREEN, List.of(
                Component.text("Voltar para o spawn principal", NamedTextColor.GRAY),
                Component.text("Clique para teleportar", NamedTextColor.GREEN)
            )));
        }

        if (player.hasPermission("polarutilities.difficulty.use")) {
            DifficultyStatus difficulty = difficultyStatus(player);
            inventory.setItem(16, actionItem(Material.DIAMOND_SWORD, ACTION_DIFFICULTY, "Dificuldade", NamedTextColor.RED, List.of(
                Component.text("Modo: " + difficulty.mode().name(), NamedTextColor.GRAY),
                Component.text("Keep Inventory: " + (difficulty.keepInventory() ? "ON" : "OFF"), difficulty.keepInventory() ? NamedTextColor.GREEN : NamedTextColor.RED),
                Component.text("Clique para abrir a GUI", NamedTextColor.GREEN)
            )));
        }

        if (player.hasPermission("polarutilities.tpa.use")) {
            boolean tpaEnabled = isTpaEnabled(player);
            inventory.setItem(20, actionItem(tpaEnabled ? Material.LIME_DYE : Material.RED_DYE, ACTION_TPA_TOGGLE, "Pedidos de TPA", NamedTextColor.LIGHT_PURPLE, List.of(
                Component.text("Status: " + (tpaEnabled ? "Recebendo" : "Bloqueado"), tpaEnabled ? NamedTextColor.GREEN : NamedTextColor.RED),
                Component.text("Clique para alternar", NamedTextColor.GREEN)
            )));
        }

        DifficultyStatus status = difficultyStatus(player);
        inventory.setItem(22, actionItem(Material.BOOK, ACTION_STATUS, "Seu Status", NamedTextColor.BLUE, List.of(
            Component.text("Homes: " + homesCount(player), NamedTextColor.GRAY),
            Component.text("TPA: " + (isTpaEnabled(player) ? "ON" : "OFF"), isTpaEnabled(player) ? NamedTextColor.GREEN : NamedTextColor.RED),
            Component.text("Dificuldade: " + status.mode().name(), NamedTextColor.GRAY),
            Component.text("Keep Inventory: " + (status.keepInventory() ? "ON" : "OFF"), status.keepInventory() ? NamedTextColor.GREEN : NamedTextColor.RED),
            Component.text("Clique para enviar no chat", NamedTextColor.GREEN)
        )));

        if (player.hasPermission("polarutilities.admin")) {
            inventory.setItem(24, actionItem(Material.REDSTONE_TORCH, ACTION_ADMIN_SETTINGS, "Settings", NamedTextColor.YELLOW, List.of(
                Component.text("Painel administrativo", NamedTextColor.GRAY),
                Component.text("Clique para abrir settings", NamedTextColor.GREEN)
            )));
            inventory.setItem(31, actionItem(Material.SPYGLASS, ACTION_UPDATES, "Updates", NamedTextColor.YELLOW, List.of(
                Component.text("Checagem manual de update", NamedTextColor.GRAY),
                Component.text("Clique para verificar agora", NamedTextColor.GREEN)
            )));
        }

        inventory.setItem(40, actionItem(Material.BARRIER, ACTION_CLOSE, "Fechar", NamedTextColor.RED, List.of(
            Component.text("Clique para fechar o menu", NamedTextColor.GRAY)
        )));
        player.openInventory(inventory);
    }

    private void handleAction(Player player, String action) {
        playClick(player);
        switch (action) {
            case ACTION_HOMES -> executeCommand(player, "homes");
            case ACTION_WARPS -> executeCommand(player, "warps");
            case ACTION_SPAWN -> executeCommand(player, "spawn");
            case ACTION_DIFFICULTY -> executeCommand(player, "dificuldade");
            case ACTION_TPA_TOGGLE -> {
                player.closeInventory();
                plugin.getServer().dispatchCommand(player, "tptoggle");
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        openMenu(player);
                    }
                }, 2L);
            }
            case ACTION_STATUS -> {
                sendStatus(player);
                openMenu(player);
            }
            case ACTION_ADMIN_SETTINGS -> executeCommand(player, "polarutilities settings");
            case ACTION_UPDATES -> executeCommand(player, "polarutilities updates");
            case ACTION_CLOSE -> player.closeInventory();
            default -> {
            }
        }
    }

    private void executeCommand(Player player, String command) {
        player.closeInventory();
        plugin.getServer().dispatchCommand(player, command);
    }

    private void sendStatus(Player player) {
        DifficultyStatus difficulty = difficultyStatus(player);
        Texts.info(player, "Menu: homes=" + homesCount(player)
            + ", warps=" + warpsCount()
            + ", TPA=" + (isTpaEnabled(player) ? "ON" : "OFF")
            + ", dificuldade=" + difficulty.mode().name()
            + ", keepInventory=" + (difficulty.keepInventory() ? "ON" : "OFF") + ".");
    }

    private ItemStack actionItem(Material material, String action, String name, NamedTextColor color, List<Component> lore) {
        return namedItem(
            material,
            Component.text(name, color).decoration(TextDecoration.BOLD, true),
            lore,
            action
        );
    }

    private ItemStack namedItem(Material material, Component name, List<Component> lore, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream()
            .map(line -> line.decoration(TextDecoration.ITALIC, false))
            .toList());
        if (action != null) {
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        }
        item.setItemMeta(meta);
        return item;
    }

    private int homesCount(Player player) {
        ConfigurationSection homes = storage.players().getConfigurationSection("players." + player.getUniqueId() + ".homes");
        return homes == null ? 0 : homes.getKeys(false).size();
    }

    private int warpsCount() {
        ConfigurationSection warps = storage.warps().getConfigurationSection("warps");
        return warps == null ? 0 : warps.getKeys(false).size();
    }

    private boolean isTpaEnabled(Player player) {
        return storage.players().getBoolean("players." + player.getUniqueId() + ".tpa-enabled", true);
    }

    private DifficultyStatus difficultyStatus(Player player) {
        String basePath = "players." + player.getUniqueId();
        DifficultyMode defaultMode = DifficultyMode.fromConfig(plugin.getConfig().getString("difficulty.default-mode", DifficultyMode.NORMAL.name()));
        DifficultyMode mode = DifficultyMode.fromConfig(storage.players().getString(basePath + ".difficulty", defaultMode.name()));
        boolean keepInventory = storage.players().getBoolean(
            basePath + ".keepInventory",
            plugin.getConfig().getBoolean("difficulty.default-keep-inventory", false)
        );
        return new DifficultyStatus(mode, keepInventory);
    }

    private void playClick(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.1F);
    }

    private record DifficultyStatus(DifficultyMode mode, boolean keepInventory) {
    }
}
