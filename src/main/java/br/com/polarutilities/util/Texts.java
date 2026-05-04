package br.com.polarutilities.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

public final class Texts {
    public static final Component PREFIX = Component.text("[PolarUtilities] ", NamedTextColor.AQUA)
        .decoration(TextDecoration.BOLD, true);

    private Texts() {
    }

    public static void success(CommandSender sender, String message) {
        send(sender, Component.text(message, NamedTextColor.GREEN));
    }

    public static void info(CommandSender sender, String message) {
        send(sender, Component.text(message, NamedTextColor.GRAY));
    }

    public static void warning(CommandSender sender, String message) {
        send(sender, Component.text(message, NamedTextColor.YELLOW));
    }

    public static void error(CommandSender sender, String message) {
        send(sender, Component.text(message, NamedTextColor.RED));
    }

    public static void send(CommandSender sender, Component message) {
        sender.sendMessage(PREFIX.append(message));
    }

    public static Component button(String label, String command, NamedTextColor color, String hoverText) {
        return Component.text("[" + label + "]", color)
            .decoration(TextDecoration.BOLD, true)
            .clickEvent(ClickEvent.runCommand(command))
            .hoverEvent(HoverEvent.showText(Component.text(hoverText, NamedTextColor.GRAY)));
    }

    public static Component commandLink(String label, String command) {
        return Component.text(label, NamedTextColor.AQUA)
            .clickEvent(ClickEvent.runCommand(command))
            .hoverEvent(HoverEvent.showText(Component.text("Clique para executar " + command, NamedTextColor.GRAY)));
    }

    public static Component suggestedCommand(String label, String command) {
        return Component.text(label, NamedTextColor.AQUA)
            .clickEvent(ClickEvent.suggestCommand(command))
            .hoverEvent(HoverEvent.showText(Component.text("Clique para preparar " + command, NamedTextColor.GRAY)));
    }

    public static Component urlButton(String label, String url, NamedTextColor color, String hoverText) {
        return Component.text("[" + label + "]", color)
            .decoration(TextDecoration.BOLD, true)
            .clickEvent(ClickEvent.openUrl(url))
            .hoverEvent(HoverEvent.showText(Component.text(hoverText, NamedTextColor.GRAY)));
    }

    public static TextComponent separator() {
        return Component.text(" | ", NamedTextColor.DARK_GRAY);
    }
}
