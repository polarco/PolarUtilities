package br.com.polarutilities.feature.home;

import br.com.polarutilities.feature.PluginFeature;
import br.com.polarutilities.model.StoredLocation;
import br.com.polarutilities.storage.PluginStorage;
import br.com.polarutilities.teleport.TeleportService;
import br.com.polarutilities.util.CommandRegistrar;
import br.com.polarutilities.util.Texts;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class HomeFeature implements PluginFeature, Listener {
    private final JavaPlugin plugin;
    private final CommandRegistrar registrar;
    private final HomeService homeService;
    private final TeleportService teleportService;
    private final NamespacedKey homeNameKey;

    public HomeFeature(
        JavaPlugin plugin,
        CommandRegistrar registrar,
        PluginStorage storage,
        TeleportService teleportService
    ) {
        this.plugin = plugin;
        this.registrar = registrar;
        this.homeService = new HomeService(plugin, storage);
        this.teleportService = teleportService;
        this.homeNameKey = new NamespacedKey(plugin, "home_name");
    }

    @Override
    public void enable() {
        registrar.register("sethome", this::setHome);
        registrar.register("home", this::home, this::completeHomes);
        registrar.register("homes", this::homes, this::completeHomesCommand);
        registrar.register("delhome", this::deleteHome, this::completeHomes);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HomeMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !player.getUniqueId().equals(holder.ownerId())) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) {
            return;
        }

        String homeName = clicked.getItemMeta().getPersistentDataContainer()
            .get(homeNameKey, PersistentDataType.STRING);
        if (homeName == null) {
            return;
        }

        if (event.isRightClick() && event.isShiftClick()) {
            if (homeService.deleteHome(player.getUniqueId(), homeName)) {
                Texts.success(player, "Home " + homeName + " removida.");
                openHomesMenu(player);
            }
            return;
        }

        player.closeInventory();
        teleportToHome(player, homeName);
    }

    private boolean setHome(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem salvar homes.");
            return true;
        }

        String homeName = args.length == 0 ? "home" : args[0];
        HomeService.SaveResult result = homeService.setHome(player, homeName);
        switch (result) {
            case CREATED -> Texts.success(player, "Home " + homeService.normalize(homeName) + " criada.");
            case UPDATED -> Texts.success(player, "Home " + homeService.normalize(homeName) + " atualizada.");
            case INVALID_NAME -> Texts.error(player, "Nome invalido. Use letras, numeros, _ ou - com ate 24 caracteres.");
            case LIMIT_REACHED -> Texts.error(player, "Voce atingiu o limite de " + homeService.maxHomes(player) + " homes.");
        }
        return true;
    }

    private boolean home(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem usar homes.");
            return true;
        }

        if (args.length == 0) {
            Map<String, StoredLocation> homes = homeService.homes(player.getUniqueId());
            if (homes.isEmpty()) {
                Texts.error(player, "Voce ainda nao tem homes. Use /sethome.");
                return true;
            }
            if (homes.size() == 1) {
                teleportToHome(player, homes.keySet().iterator().next());
                return true;
            }
            openHomesMenu(player);
            return true;
        }

        teleportToHome(player, args[0]);
        return true;
    }

    private boolean homes(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem usar homes.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            sendHomesList(player);
            return true;
        }

        if (!player.hasPermission("polarutilities.home.gui")) {
            sendHomesList(player);
            return true;
        }

        openHomesMenu(player);
        return true;
    }

    private boolean deleteHome(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            Texts.error(sender, "Apenas jogadores podem remover homes.");
            return true;
        }
        if (args.length != 1) {
            Texts.error(player, "Use /delhome <nome>.");
            return true;
        }

        String name = homeService.normalize(args[0]);
        if (!homeService.deleteHome(player.getUniqueId(), name)) {
            Texts.error(player, "Home " + name + " nao encontrada.");
            return true;
        }
        Texts.success(player, "Home " + name + " removida.");
        return true;
    }

    private void teleportToHome(Player player, String rawName) {
        String name = homeService.normalize(rawName);
        homeService.home(player.getUniqueId(), name)
            .flatMap(StoredLocation::toLocation)
            .ifPresentOrElse(
                location -> teleportService.teleport(player, location, "home " + name),
                () -> Texts.error(player, "Home " + name + " nao encontrada ou mundo nao carregado.")
            );
    }

    private void sendHomesList(Player player) {
        Map<String, StoredLocation> homes = homeService.homes(player.getUniqueId());
        if (homes.isEmpty()) {
            Texts.error(player, "Voce ainda nao tem homes. Use /sethome.");
            return;
        }

        Texts.info(player, "Homes salvas:");
        for (Map.Entry<String, StoredLocation> entry : homes.entrySet()) {
            Component line = Component.text("- ", NamedTextColor.DARK_GRAY)
                .append(Texts.commandLink(entry.getKey(), "/home " + entry.getKey()))
                .append(Component.text(" em " + entry.getValue().shortText(), NamedTextColor.GRAY))
                .append(Texts.separator())
                .append(Texts.button("Remover", "/delhome " + entry.getKey(), NamedTextColor.RED, "Remover esta home"));
            player.sendMessage(line);
        }
    }

    private void openHomesMenu(Player player) {
        Map<String, StoredLocation> homes = homeService.homes(player.getUniqueId());
        if (homes.isEmpty()) {
            Texts.error(player, "Voce ainda nao tem homes. Use /sethome.");
            return;
        }

        int size = inventorySize(homes.size());
        HomeMenuHolder holder = new HomeMenuHolder(player.getUniqueId());
        String title = plugin.getConfig().getString("homes.gui-title", "Suas homes");
        Inventory inventory = Bukkit.createInventory(holder, size, Component.text(title));
        holder.setInventory(inventory);

        ItemStack filler = filler();
        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, filler);
        }

        List<Integer> slots = contentSlots(size);
        int index = 0;
        for (Map.Entry<String, StoredLocation> entry : homes.entrySet()) {
            if (index >= slots.size()) {
                break;
            }
            inventory.setItem(slots.get(index), homeItem(player, entry.getKey(), entry.getValue()));
            index++;
        }

        player.openInventory(inventory);
    }

    private ItemStack homeItem(Player player, String name, StoredLocation location) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(Component.text(name, NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
            Component.text(location.shortText(), NamedTextColor.GRAY),
            Component.text("Clique para teleportar", NamedTextColor.GREEN),
            Component.text("Shift + direito para remover", NamedTextColor.RED)
        ));
        meta.getPersistentDataContainer().set(homeNameKey, PersistentDataType.STRING, name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    private int inventorySize(int items) {
        int rows = Math.max(3, (int) Math.ceil(items / 7.0) + 2);
        return Math.min(54, rows * 9);
    }

    private List<Integer> contentSlots(int size) {
        List<Integer> slots = new ArrayList<>();
        int rows = size / 9;
        for (int row = 1; row < rows - 1; row++) {
            for (int column = 1; column < 8; column++) {
                slots.add(row * 9 + column);
            }
        }
        return slots;
    }

    private List<String> completeHomes(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            return List.of();
        }
        return homeService.homes(player.getUniqueId()).keySet().stream()
            .filter(home -> home.startsWith(args[0].toLowerCase(Locale.ROOT)))
            .toList();
    }

    private List<String> completeHomesCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> modes = List.of("gui", "list");
        return modes.stream()
            .filter(mode -> mode.startsWith(args[0].toLowerCase(Locale.ROOT)))
            .toList();
    }
}
