package br.com.polarutilities.feature.admin;

import br.com.polarutilities.feature.PluginFeature;
import br.com.polarutilities.feature.update.UpdateCheckerFeature;
import br.com.polarutilities.storage.PluginStorage;
import br.com.polarutilities.util.CommandRegistrar;
import br.com.polarutilities.util.Texts;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class AdminFeature implements PluginFeature, Listener {
    private static final List<String> ROOT_COMMANDS = List.of(
        "help",
        "version",
        "reload",
        "debug",
        "updates",
        "settings",
        "setwarp",
        "delwarp",
        "setspawn"
    );

    private final JavaPlugin plugin;
    private final CommandRegistrar registrar;
    private final PluginStorage storage;
    private final UpdateCheckerFeature updateCheckerFeature;
    private final NamespacedKey settingPathKey;
    private final NamespacedKey actionKey;
    private final List<SettingOption> options;

    public AdminFeature(
        JavaPlugin plugin,
        CommandRegistrar registrar,
        PluginStorage storage,
        UpdateCheckerFeature updateCheckerFeature
    ) {
        this.plugin = plugin;
        this.registrar = registrar;
        this.storage = storage;
        this.updateCheckerFeature = updateCheckerFeature;
        this.settingPathKey = new NamespacedKey(plugin, "setting_path");
        this.actionKey = new NamespacedKey(plugin, "settings_action");
        this.options = List.of(
            SettingOption.integer("teleport.delay-seconds", Material.CLOCK, "Delay do teleporte", "Tempo antes do teleporte acontecer.", 3, 0, 60, 1, 5),
            SettingOption.bool("teleport.cancel-on-move", "Cancelar ao mover", "Cancela teleportes pendentes quando o player se move.", true),
            SettingOption.integer("tpa.request-timeout-seconds", Material.ENDER_PEARL, "Timeout do TPA", "Tempo para aceitar ou negar um pedido.", 60, 5, 600, 5, 30),
            SettingOption.bool("tpa.allow-self-request", "TPA para si proprio", "Permite usar TPA em si mesmo para testar sozinho.", false),
            SettingOption.integer("homes.default-limit", Material.PLAYER_HEAD, "Limite padrao de homes", "Quantidade de homes para players sem permissao extra.", 5, 1, 100, 1, 10),
            SettingOption.text("homes.gui-title", Material.OAK_SIGN, "Titulo da GUI de homes", "Texto exibido no menu de homes.", "Suas homes"),
            SettingOption.text("warps.list-title", Material.COMPASS, "Titulo da lista de warps", "Texto exibido antes da lista de warps.", "Warps disponiveis"),
            SettingOption.bool("spawn.fallback-to-world-spawn", "Spawn fallback", "Usa o spawn do mundo se /setspawn ainda nao foi usado.", true),
            SettingOption.bool("module.enabled", "Modulo dificuldade", "Liga ou desliga a dificuldade individual.", true),
            SettingOption.bool("update-checker.enabled", "Update checker", "Checa se existe versao nova ao ligar o servidor.", true),
            SettingOption.bool("update-checker.auto-download", "Auto update", "Baixa updates oficiais para aplicar no proximo restart.", true),
            SettingOption.bool("update-checker.notify-console", "Avisar console", "Mostra updates no console do servidor.", true),
            SettingOption.bool("update-checker.notify-admins-on-join", "Avisar admins", "Avisa admins quando entrarem se houver update.", true),
            SettingOption.integer("update-checker.startup-delay-seconds", Material.REPEATER, "Delay do update checker", "Tempo apos o boot antes da checagem.", 5, 0, 120, 1, 10),
            SettingOption.integer("update-checker.timeout-seconds", Material.COMPARATOR, "Timeout do update checker", "Tempo maximo aguardando a URL responder.", 6, 2, 30, 1, 5)
        );
    }

    @Override
    public void enable() {
        registrar.register("polarutilities", this::handleAdminCommand, this::completeAdminCommand);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SettingsMenuHolder holder)) {
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

        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            handleMenuAction(player, action);
            return;
        }

        String path = meta.getPersistentDataContainer().get(settingPathKey, PersistentDataType.STRING);
        SettingOption option = optionByPath(path);
        if (option == null) {
            return;
        }

        switch (option.type()) {
            case BOOLEAN -> {
                boolean newValue = !booleanValue(option);
                plugin.getConfig().set(option.path(), newValue);
                saveSettings();
                Texts.success(player, option.name() + ": " + enabledText(newValue) + ".");
                openSettingsMenu(player);
            }
            case INTEGER -> {
                int delta = event.isShiftClick() ? option.shiftStep() : option.step();
                if (event.isRightClick()) {
                    delta = -delta;
                }
                int current = intValue(option);
                int newValue = clamp(current + delta, option.min(), option.max());
                plugin.getConfig().set(option.path(), newValue);
                saveSettings();
                Texts.success(player, option.name() + ": " + newValue + ".");
                openSettingsMenu(player);
            }
            case TEXT -> {
                player.closeInventory();
                String command = "/polarutilities settings set " + option.path() + " " + stringValue(option);
                Texts.send(player, Component.text("Edite por comando: ", NamedTextColor.GRAY)
                    .append(Texts.suggestedCommand(command, command)));
            }
        }
    }

    private boolean handleAdminCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!sender.hasPermission("polarutilities.admin")) {
            Texts.error(sender, "Voce nao tem permissao para usar este comando.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "version" -> {
                Texts.info(sender, "PolarUtilities v" + plugin.getPluginMeta().getVersion() + " para Paper 26.1.2.");
                yield true;
            }
            case "reload" -> {
                plugin.reloadConfig();
                storage.load();
                Texts.success(sender, "Configuracao e dados recarregados.");
                yield true;
            }
            case "debug" -> {
                sendDebug(sender);
                yield true;
            }
            case "updates" -> {
                updateCheckerFeature.checkNow(sender);
                yield true;
            }
            case "settings" -> {
                handleSettings(sender, args);
                yield true;
            }
            case "setwarp", "delwarp", "setspawn" -> {
                forwardAdminCommand(sender, args);
                yield true;
            }
            default -> {
                Texts.error(sender, "Use /" + label + " help.");
                yield true;
            }
        };
    }

    private void handleSettings(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (sender instanceof Player player) {
                openSettingsMenu(player);
            } else {
                sendSettingsSummary(sender);
            }
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("get")) {
            if (args.length != 3) {
                Texts.error(sender, "Use /polarutilities settings get <chave>.");
                return;
            }
            SettingOption option = optionByPath(args[2]);
            if (option == null) {
                sendUnknownSetting(sender, args[2]);
                return;
            }
            Texts.info(sender, option.path() + " = " + valueText(option));
            return;
        }

        if (action.equals("set")) {
            if (args.length < 4) {
                Texts.error(sender, "Use /polarutilities settings set <chave> <valor>.");
                return;
            }
            SettingOption option = optionByPath(args[2]);
            if (option == null) {
                sendUnknownSetting(sender, args[2]);
                return;
            }
            setOptionFromText(sender, option, joinArgs(args, 3));
            return;
        }

        if (action.equals("reset")) {
            if (args.length != 3) {
                Texts.error(sender, "Use /polarutilities settings reset <chave|all>.");
                return;
            }
            resetSetting(sender, args[2]);
            return;
        }

        Texts.error(sender, "Use /polarutilities settings <get|set|reset>.");
    }

    private void openSettingsMenu(Player player) {
        SettingsMenuHolder holder = new SettingsMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text("PolarUtilities Settings"));
        holder.setInventory(inventory);

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        };
        for (int index = 0; index < options.size() && index < slots.length; index++) {
            inventory.setItem(slots[index], settingItem(options.get(index)));
        }

        inventory.setItem(48, actionItem(Material.BOOK, "help", "Ajuda admin", "Mostra os comandos administrativos."));
        inventory.setItem(49, actionItem(Material.REDSTONE_TORCH, "reload", "Recarregar", "Recarrega config e dados YAML."));
        inventory.setItem(50, actionItem(Material.DEBUG_STICK, "debug", "Debug", "Mostra estado rapido do plugin."));
        inventory.setItem(51, actionItem(Material.SPYGLASS, "updates", "Checar updates", "Forca uma checagem agora."));
        player.openInventory(inventory);
    }

    private ItemStack settingItem(SettingOption option) {
        Material material = option.type() == SettingType.BOOLEAN
            ? (booleanValue(option) ? Material.LIME_DYE : Material.RED_DYE)
            : option.material();
        Component name = Component.text(option.name(), NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true);
        List<Component> lore = switch (option.type()) {
            case BOOLEAN -> List.of(
                Component.text(option.description(), NamedTextColor.GRAY),
                Component.text(option.path(), NamedTextColor.DARK_GRAY),
                Component.text("Valor: " + valueText(option), NamedTextColor.YELLOW),
                Component.text("Clique para alternar", NamedTextColor.GREEN)
            );
            case INTEGER -> List.of(
                Component.text(option.description(), NamedTextColor.GRAY),
                Component.text(option.path(), NamedTextColor.DARK_GRAY),
                Component.text("Valor: " + valueText(option), NamedTextColor.YELLOW),
                Component.text("Esquerdo aumenta, direito diminui", NamedTextColor.GREEN),
                Component.text("Shift usa passo maior", NamedTextColor.DARK_GRAY)
            );
            case TEXT -> List.of(
                Component.text(option.description(), NamedTextColor.GRAY),
                Component.text(option.path(), NamedTextColor.DARK_GRAY),
                Component.text("Valor: " + valueText(option), NamedTextColor.YELLOW),
                Component.text("Clique para receber comando de edicao", NamedTextColor.GREEN)
            );
        };

        ItemStack item = namedItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(settingPathKey, PersistentDataType.STRING, option.path());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack actionItem(Material material, String action, String name, String description) {
        ItemStack item = namedItem(
            material,
            Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true),
            List.of(Component.text(description, NamedTextColor.GRAY))
        );
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack namedItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream()
            .map(line -> line.decoration(TextDecoration.ITALIC, false))
            .toList());
        item.setItemMeta(meta);
        return item;
    }

    private void handleMenuAction(Player player, String action) {
        switch (action) {
            case "help" -> {
                player.closeInventory();
                sendHelp(player);
            }
            case "reload" -> {
                plugin.reloadConfig();
                storage.load();
                Texts.success(player, "Configuracao e dados recarregados.");
                openSettingsMenu(player);
            }
            case "debug" -> {
                player.closeInventory();
                sendDebug(player);
            }
            case "updates" -> {
                player.closeInventory();
                updateCheckerFeature.checkNow(player);
            }
            default -> {
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        Texts.info(sender, "Comandos admin do PolarUtilities:");
        sender.sendMessage(Texts.PREFIX
            .append(Texts.commandLink("/polarutilities settings", "/polarutilities settings"))
            .append(Texts.separator())
            .append(Texts.commandLink("/polarutilities debug", "/polarutilities debug"))
            .append(Texts.separator())
            .append(Texts.commandLink("/polarutilities updates", "/polarutilities updates"))
            .append(Texts.separator())
            .append(Texts.commandLink("/polarutilities reload", "/polarutilities reload")));
        sender.sendMessage(Texts.PREFIX
            .append(Texts.commandLink("/polarutilities setspawn", "/polarutilities setspawn"))
            .append(Texts.separator())
            .append(Texts.suggestedCommand("/polarutilities setwarp <nome>", "/polarutilities setwarp "))
            .append(Texts.separator())
            .append(Texts.suggestedCommand("/polarutilities delwarp <nome>", "/polarutilities delwarp ")));
        Texts.info(sender, "Settings por comando: /polarutilities settings <get|set|reset> <chave> [valor].");
    }

    private void sendDebug(CommandSender sender) {
        Texts.info(sender, "Versao: " + plugin.getPluginMeta().getVersion());
        Texts.info(sender, "Servidor: " + plugin.getServer().getVersion());
        Texts.info(sender, "Pasta de dados: " + plugin.getDataFolder().getAbsolutePath());
        Texts.info(sender, "Config: delay=" + plugin.getConfig().getInt("teleport.delay-seconds", 3)
            + "s, cancelOnMove=" + plugin.getConfig().getBoolean("teleport.cancel-on-move", true)
            + ", tpaSelf=" + plugin.getConfig().getBoolean("tpa.allow-self-request", false)
            + ", tpaTimeout=" + plugin.getConfig().getInt("tpa.request-timeout-seconds", 60)
            + "s, homesLimit=" + plugin.getConfig().getInt("homes.default-limit", 5)
            + ", spawnFallback=" + plugin.getConfig().getBoolean("spawn.fallback-to-world-spawn", true)
            + ", difficultyModule=" + plugin.getConfig().getBoolean("module.enabled", true));
        Texts.info(sender, "Updates: " + updateCheckerFeature.statusText());
    }

    private void sendSettingsSummary(CommandSender sender) {
        Texts.info(sender, "Settings atuais:");
        for (SettingOption option : options) {
            Texts.info(sender, option.path() + " = " + valueText(option));
        }
    }

    private void forwardAdminCommand(CommandSender sender, String[] args) {
        plugin.getServer().dispatchCommand(sender, String.join(" ", args));
    }

    private void setOptionFromText(CommandSender sender, SettingOption option, String value) {
        try {
            switch (option.type()) {
                case BOOLEAN -> plugin.getConfig().set(option.path(), parseBoolean(value));
                case INTEGER -> plugin.getConfig().set(option.path(), clamp(Integer.parseInt(value.trim()), option.min(), option.max()));
                case TEXT -> plugin.getConfig().set(option.path(), value);
            }
        } catch (IllegalArgumentException exception) {
            Texts.error(sender, "Valor invalido para " + option.path() + ".");
            return;
        }

        saveSettings();
        Texts.success(sender, option.path() + " atualizado para " + valueText(option) + ".");
    }

    private void resetSetting(CommandSender sender, String target) {
        if (target.equalsIgnoreCase("all")) {
            for (SettingOption option : options) {
                plugin.getConfig().set(option.path(), option.defaultValue());
            }
            saveSettings();
            Texts.success(sender, "Todas as settings foram resetadas para o padrao.");
            return;
        }

        SettingOption option = optionByPath(target);
        if (option == null) {
            sendUnknownSetting(sender, target);
            return;
        }
        plugin.getConfig().set(option.path(), option.defaultValue());
        saveSettings();
        Texts.success(sender, option.path() + " resetado para " + valueText(option) + ".");
    }

    private void sendUnknownSetting(CommandSender sender, String path) {
        Texts.error(sender, "Setting nao encontrada: " + path + ".");
    }

    private void saveSettings() {
        plugin.saveConfig();
    }

    private SettingOption optionByPath(String path) {
        if (path == null) {
            return null;
        }
        return options.stream()
            .filter(option -> option.path().equalsIgnoreCase(path))
            .findFirst()
            .orElse(null);
    }

    private String valueText(SettingOption option) {
        return switch (option.type()) {
            case BOOLEAN -> enabledText(booleanValue(option));
            case INTEGER -> String.valueOf(intValue(option));
            case TEXT -> stringValue(option);
        };
    }

    private boolean booleanValue(SettingOption option) {
        return plugin.getConfig().getBoolean(option.path(), (Boolean) option.defaultValue());
    }

    private int intValue(SettingOption option) {
        return plugin.getConfig().getInt(option.path(), (Integer) option.defaultValue());
    }

    private String stringValue(SettingOption option) {
        return plugin.getConfig().getString(option.path(), (String) option.defaultValue());
    }

    private String enabledText(boolean enabled) {
        return enabled ? "ligado" : "desligado";
    }

    private boolean parseBoolean(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (List.of("true", "on", "sim", "yes", "1", "ligado").contains(normalized)) {
            return true;
        }
        if (List.of("false", "off", "nao", "no", "0", "desligado").contains(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean");
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String joinArgs(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    private List<String> completeAdminCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("polarutilities.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return startsWith(ROOT_COMMANDS, args[0]);
        }
        if (args[0].equalsIgnoreCase("settings")) {
            return completeSettings(args);
        }
        return List.of();
    }

    private List<String> completeSettings(String[] args) {
        if (args.length == 2) {
            return startsWith(List.of("get", "set", "reset"), args[1]);
        }
        if ((args[1].equalsIgnoreCase("get") || args[1].equalsIgnoreCase("set")) && args.length == 3) {
            return startsWith(options.stream().map(SettingOption::path).toList(), args[2]);
        }
        if (args[1].equalsIgnoreCase("reset") && args.length == 3) {
            return startsWith(options.stream().map(SettingOption::path).toList(), args[2], "all");
        }
        return List.of();
    }

    private List<String> startsWith(List<String> values, String prefix, String... extraValues) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return java.util.stream.Stream.concat(values.stream(), Arrays.stream(extraValues))
            .filter(Objects::nonNull)
            .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
            .distinct()
            .toList();
    }

    private enum SettingType {
        BOOLEAN,
        INTEGER,
        TEXT
    }

    private record SettingOption(
        String path,
        SettingType type,
        Material material,
        String name,
        String description,
        Object defaultValue,
        int min,
        int max,
        int step,
        int shiftStep
    ) {
        private static SettingOption bool(String path, String name, String description, boolean defaultValue) {
            return new SettingOption(path, SettingType.BOOLEAN, Material.LEVER, name, description, defaultValue, 0, 1, 1, 1);
        }

        private static SettingOption integer(
            String path,
            Material material,
            String name,
            String description,
            int defaultValue,
            int min,
            int max,
            int step,
            int shiftStep
        ) {
            return new SettingOption(path, SettingType.INTEGER, material, name, description, defaultValue, min, max, step, shiftStep);
        }

        private static SettingOption text(String path, Material material, String name, String description, String defaultValue) {
            return new SettingOption(path, SettingType.TEXT, material, name, description, defaultValue, 0, 0, 0, 0);
        }
    }
}
