package br.com.polarutilities.feature.update;

import br.com.polarutilities.feature.PluginFeature;
import br.com.polarutilities.util.Texts;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    private static final String OFFICIAL_METADATA_URL = "https://raw.githubusercontent.com/polarco/PolarUtilities/main/release/update.json";
    private static final String OFFICIAL_DOWNLOAD_URL = "https://github.com/polarco/PolarUtilities/releases/download/v%s/PolarUtilities-%s.jar";

    private final JavaPlugin plugin;
    private final AtomicBoolean checkInProgress = new AtomicBoolean(false);
    private final AtomicBoolean downloadInProgress = new AtomicBoolean(false);
    private UpdateCheckResult lastResult;
    private UpdateDownloadResult lastDownloadResult;
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

        if (!checkInProgress.compareAndSet(false, true)) {
            if (requester != null) {
                Texts.warning(requester, "Ja existe uma checagem de update em andamento.");
            }
            return;
        }

        if (requester != null) {
            Texts.info(requester, "Checando atualizacoes oficiais...");
        }

        int timeoutSeconds = Math.max(2, plugin.getConfig().getInt("update-checker.timeout-seconds", 6));
        HttpClient client;
        HttpRequest request;
        try {
            client = httpClient(timeoutSeconds);
            request = HttpRequest.newBuilder(URI.create(OFFICIAL_METADATA_URL))
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
        if (downloadInProgress.get()) {
            return "download de update em andamento";
        }
        if (lastDownloadResult != null) {
            return "update baixado: " + lastDownloadResult.version() + " em " + lastDownloadResult.fileName();
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

    private HttpClient httpClient(int timeoutSeconds) {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .build();
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
            if (plugin.getConfig().getBoolean("update-checker.auto-download", true)) {
                downloadUpdate(requester, result);
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

    private void downloadUpdate(CommandSender requester, UpdateCheckResult result) {
        if (!downloadInProgress.compareAndSet(false, true)) {
            if (requester != null) {
                Texts.warning(requester, "Ja existe um download de update em andamento.");
            }
            return;
        }

        String latest = result.metadata().latest();
        int timeoutSeconds = Math.max(30, plugin.getConfig().getInt("update-checker.timeout-seconds", 6) * 5);
        HttpClient client = httpClient(timeoutSeconds);
        File updateFolder = plugin.getServer().getUpdateFolderFile();
        HttpRequest request = HttpRequest.newBuilder(URI.create(officialDownloadUrl(latest)))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Accept", "application/java-archive, application/octet-stream, */*")
            .header("User-Agent", "PolarUtilities/" + plugin.getPluginMeta().getVersion())
            .GET()
            .build();

        if (requester != null) {
            Texts.info(requester, "Baixando update oficial " + latest + " para aplicar no proximo restart...");
        } else if (plugin.getConfig().getBoolean("update-checker.notify-console", true)) {
            plugin.getLogger().info("Baixando update oficial " + latest + " para aplicar no proximo restart.");
        }

        CompletableFuture
            .supplyAsync(() -> download(client, request, latest, updateFolder))
            .whenComplete((downloadResult, throwable) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                downloadInProgress.set(false);
                if (throwable != null) {
                    handleDownloadFailure(requester, throwable);
                    return;
                }
                handleDownloadSuccess(requester, downloadResult);
            }));
    }

    private UpdateDownloadResult download(HttpClient client, HttpRequest request, String latestVersion, File updateFolder) {
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }

            byte[] body = response.body();
            if (body.length < 1024) {
                throw new IllegalStateException("Arquivo baixado parece invalido ou vazio.");
            }
            if (body[0] != 'P' || body[1] != 'K') {
                throw new IllegalStateException("Arquivo baixado nao parece ser um JAR valido.");
            }

            if (!updateFolder.exists() && !updateFolder.mkdirs()) {
                throw new IllegalStateException("Nao foi possivel criar a pasta de update: " + updateFolder.getAbsolutePath());
            }

            String fileName = "PolarUtilities-" + safeVersion(latestVersion) + ".jar";
            Path target = updateFolder.toPath().resolve(fileName);
            Path temporary = updateFolder.toPath().resolve(fileName + ".tmp");
            Files.write(temporary, body);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            return new UpdateDownloadResult(latestVersion, target.toFile().getName(), target.toAbsolutePath().toString(), Instant.now());
        } catch (Exception exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }

    private void handleDownloadSuccess(CommandSender requester, UpdateDownloadResult result) {
        lastDownloadResult = result;
        lastError = null;
        String message = "Update " + result.version() + " baixado em " + result.fileName() + ". Reinicie o servidor para aplicar.";

        if (requester != null) {
            Texts.success(requester, message);
        }
        if (requester == null || requester != plugin.getServer().getConsoleSender()) {
            plugin.getLogger().info(message);
        }
    }

    private void handleDownloadFailure(CommandSender requester, Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        lastError = "download falhou: " + cause.getMessage();

        if (requester != null) {
            Texts.error(requester, "Nao foi possivel baixar o update: " + cause.getMessage());
            return;
        }

        if (plugin.getConfig().getBoolean("update-checker.notify-console", true)) {
            plugin.getLogger().log(Level.WARNING, "Nao foi possivel baixar o update: " + cause.getMessage());
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
        String downloadUrl = officialDownloadUrl(metadata.latest());
        boolean hasDownload = !downloadUrl.isBlank();
        boolean hasChangelog = metadata.changelogUrl() != null && !metadata.changelogUrl().isBlank();

        if (hasDownload) {
            links = links.append(Texts.urlButton("Baixar JAR", downloadUrl, NamedTextColor.GREEN, "Abrir download oficial"));
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

    private String officialDownloadUrl(String version) {
        return OFFICIAL_DOWNLOAD_URL.formatted(version, version);
    }

    private String safeVersion(String version) {
        return version == null ? "unknown" : version.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private record UpdateDownloadResult(String version, String fileName, String absolutePath, Instant downloadedAt) {
    }
}
