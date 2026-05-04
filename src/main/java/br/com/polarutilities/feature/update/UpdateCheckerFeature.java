package br.com.polarutilities.feature.update;

import br.com.polarutilities.feature.PluginFeature;
import br.com.polarutilities.util.Texts;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class UpdateCheckerFeature implements PluginFeature, Listener {
    private final JavaPlugin plugin;
    private final AtomicBoolean checkInProgress = new AtomicBoolean(false);
    private UpdateCheckResult lastResult;
    private String lastError;

    public UpdateCheckerFeature(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }

        long delaySeconds = Math.max(0, plugin.getConfig().getLong("update-checker.startup-delay-seconds", 5));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkNow(null), delaySeconds * 20L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("update-checker.notify-admins-on-join", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("polarutilities.admin")) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            UpdateCheckResult result = lastResult;
            if (result != null && result.updateAvailable()) {
                sendUpdateNotice(player, result);
            }
        }, 40L);
    }

    public void checkNow(CommandSender requester) {
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) {
            if (requester != null) {
                Texts.warning(requester, "Update checker esta desligado.");
            }
            return;
        }

        String url = plugin.getConfig().getString("update-checker.url", "");
        if (url == null || url.isBlank()) {
            lastError = "URL do update checker nao configurada.";
            if (requester != null) {
                Texts.error(requester, lastError);
                Texts.info(requester, "Configure com /polarutilities settings set update-checker.url <url>.");
            }
            return;
        }

        if (!checkInProgress.compareAndSet(false, true)) {
            if (requester != null) {
                Texts.warning(requester, "Ja existe uma checagem de update em andamento.");
            }
            return;
        }

        if (requester != null) {
            Texts.info(requester, "Checando atualizacoes...");
        }

        int timeoutSeconds = Math.max(2, plugin.getConfig().getInt("update-checker.timeout-seconds", 6));
        HttpClient client;
        HttpRequest request;
        try {
            URI uri = URI.create(url);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("Use uma URL http ou https.");
            }
            client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
            request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Accept", "application/json, application/x-yaml, text/yaml, text/plain")
                .header("User-Agent", "PolarUtilities/" + plugin.getPluginMeta().getVersion())
                .GET()
                .build();
        } catch (IllegalArgumentException exception) {
            checkInProgress.set(false);
            lastError = "URL invalida: " + exception.getMessage();
            if (requester != null) {
                Texts.error(requester, lastError);
            } else if (plugin.getConfig().getBoolean("update-checker.notify-console", true)) {
                plugin.getLogger().warning(lastError);
            }
            return;
        }

        CompletableFuture
            .supplyAsync(() -> fetch(client, request))
            .whenComplete((result, throwable) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                checkInProgress.set(false);
                if (throwable != null) {
                    handleFailure(requester, throwable);
                    return;
                }
                handleResult(requester, result);
            }));
    }

    public String statusText() {
        if (checkInProgress.get()) {
            return "checagem em andamento";
        }
        if (lastResult != null) {
            return lastResult.updateAvailable()
                ? "update disponivel: " + lastResult.currentVersion() + " -> " + lastResult.metadata().latest()
                : "atualizado: " + lastResult.currentVersion();
        }
        if (lastError != null) {
            return "erro: " + lastError;
        }
        return "ainda nao checado";
    }

    private UpdateCheckResult fetch(HttpClient client, HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }

            UpdateMetadata metadata = parseMetadata(response.body());
            String currentVersion = plugin.getPluginMeta().getVersion();
            boolean updateAvailable = VersionComparator.compare(currentVersion, metadata.latest()) < 0;
            return new UpdateCheckResult(currentVersion, metadata, updateAvailable, Instant.now());
        } catch (Exception exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }

    private UpdateMetadata parseMetadata(String body) throws InvalidConfigurationException {
        YamlConfiguration metadata = new YamlConfiguration();
        metadata.loadFromString(body);

        String latest = metadata.getString("latest", metadata.getString("version", ""));
        if (latest == null || latest.isBlank()) {
            throw new InvalidConfigurationException("Campo latest ausente.");
        }

        return new UpdateMetadata(
            latest.trim(),
            metadata.getString("downloadUrl", ""),
            metadata.getString("changelogUrl", ""),
            metadata.getString("message", ""),
            metadata.getBoolean("critical", false)
        );
    }

    private void handleResult(CommandSender requester, UpdateCheckResult result) {
        lastResult = result;
        lastError = null;

        if (result.updateAvailable()) {
            if (plugin.getConfig().getBoolean("update-checker.notify-console", true)) {
                sendUpdateNotice(plugin.getServer().getConsoleSender(), result);
            }
            if (requester != null && requester != plugin.getServer().getConsoleSender()) {
                sendUpdateNotice(requester, result);
            }
            return;
        }

        if (requester != null) {
            Texts.success(requester, "PolarUtilities esta atualizado (" + result.currentVersion() + ").");
        } else if (plugin.getConfig().getBoolean("update-checker.notify-console", true)) {
            plugin.getLogger().info("Update checker: PolarUtilities esta atualizado (" + result.currentVersion() + ").");
        }
    }

    private void handleFailure(CommandSender requester, Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        lastError = cause.getMessage();

        if (requester != null) {
            Texts.error(requester, "Nao foi possivel checar updates: " + lastError);
            return;
        }

        if (plugin.getConfig().getBoolean("update-checker.notify-console", true)) {
            plugin.getLogger().log(Level.WARNING, "Nao foi possivel checar updates: " + lastError);
        }
    }

    private void sendUpdateNotice(CommandSender sender, UpdateCheckResult result) {
        UpdateMetadata metadata = result.metadata();
        NamedTextColor color = metadata.critical() ? NamedTextColor.RED : NamedTextColor.GOLD;

        Texts.send(sender, Component.text("Atualizacao disponivel: ", color)
            .append(Component.text(result.currentVersion(), NamedTextColor.GRAY))
            .append(Component.text(" -> ", NamedTextColor.DARK_GRAY))
            .append(Component.text(metadata.latest(), NamedTextColor.GREEN)));

        if (metadata.message() != null && !metadata.message().isBlank()) {
            Texts.send(sender, Component.text(metadata.message(), NamedTextColor.GRAY));
        }

        Component links = Component.empty();
        boolean hasDownload = metadata.downloadUrl() != null && !metadata.downloadUrl().isBlank();
        boolean hasChangelog = metadata.changelogUrl() != null && !metadata.changelogUrl().isBlank();

        if (hasDownload) {
            links = links.append(Texts.urlButton("Baixar", metadata.downloadUrl(), NamedTextColor.GREEN, "Abrir pagina de download"));
        }
        if (hasDownload && hasChangelog) {
            links = links.append(Texts.separator());
        }
        if (hasChangelog) {
            links = links.append(Texts.urlButton("Changelog", metadata.changelogUrl(), NamedTextColor.AQUA, "Abrir changelog"));
        }
        if (hasDownload || hasChangelog) {
            Texts.send(sender, links);
        }
    }
}
