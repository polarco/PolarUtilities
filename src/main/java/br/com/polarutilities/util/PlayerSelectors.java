package br.com.polarutilities.util;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PlayerSelectors {
    private PlayerSelectors() {
    }

    public static Player onlineByName(String name) {
        return Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public static List<String> onlinePlayerNames(CommandSender sender, String prefix) {
        return onlinePlayerNames(sender, prefix, false);
    }

    public static List<String> onlinePlayerNames(CommandSender sender, String prefix, boolean includeSelf) {
        String lowered = prefix.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
            .filter(player -> includeSelf || !(sender instanceof Player current) || !current.getUniqueId().equals(player.getUniqueId()))
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(lowered))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }
}
