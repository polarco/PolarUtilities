package br.com.polarutilities.util;

import java.util.List;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class CommandRegistrar {
    private final JavaPlugin plugin;

    public CommandRegistrar(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(String commandName, CommandExecutor executor) {
        register(commandName, executor, (sender, command, alias, args) -> List.of());
    }

    public void register(String commandName, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = plugin.getCommand(commandName);
        if (command == null) {
            plugin.getLogger().warning("Comando nao encontrado no plugin.yml: " + commandName);
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
    }
}
