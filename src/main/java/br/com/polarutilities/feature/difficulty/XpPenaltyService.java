package br.com.polarutilities.feature.difficulty;

import java.time.Duration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class XpPenaltyService {
    private static final String BASE_PERCENT_PATH = "keepInventoryXpPenalty.baseXpLossPercent";
    private static final String EXTRA_PERCENT_PATH = "keepInventoryXpPenalty.extraLossPerRecentDeathPercent";
    private static final String MAX_PERCENT_PATH = "keepInventoryXpPenalty.maxXpLossPercent";
    private static final String STACK_WINDOW_PATH = "keepInventoryXpPenalty.stackWindowMinutes";
    private static final String MINIMUM_LOSS_PATH = "keepInventoryXpPenalty.minimumXpLoss";

    private final JavaPlugin plugin;
    private final DeathTracker deathTracker;

    public XpPenaltyService(JavaPlugin plugin, DeathTracker deathTracker) {
        this.plugin = plugin;
        this.deathTracker = deathTracker;
    }

    public XpPenaltyResult applyPenalty(PlayerDeathEvent event) {
        Player player = event.getEntity();
        XpPenaltyConfig config = config();
        int recentDeaths = deathTracker.recordDeath(player.getUniqueId(), Duration.ofMinutes(config.stackWindowMinutes()));
        int lossPercent = Math.min(config.maxLossPercent(), config.baseLossPercent() + config.extraLossPerRecentDeathPercent() * recentDeaths);

        int currentTotal = XpUtil.totalExperience(player);
        int lostExp = 0;
        if (currentTotal > 0 && lossPercent > 0) {
            lostExp = (int) Math.ceil(currentTotal * (lossPercent / 100.0D));
            lostExp = Math.max(config.minimumXpLoss(), lostExp);
            lostExp = Math.min(currentTotal, lostExp);
        }

        int newTotal = Math.max(0, currentTotal - lostExp);
        XpUtil.XpSnapshot snapshot = XpUtil.snapshotFromTotal(newTotal);
        event.setKeepLevel(false);
        event.setDroppedExp(0);
        event.setShouldDropExperience(false);
        event.setNewTotalExp(snapshot.totalExperience());
        event.setNewLevel(snapshot.level());
        event.setNewExp(snapshot.expIntoLevel());
        return new XpPenaltyResult(lossPercent, lostExp, snapshot.totalExperience(), recentDeaths);
    }

    private XpPenaltyConfig config() {
        int base = clampPercent(plugin.getConfig().getInt(BASE_PERCENT_PATH, 10));
        int extra = Math.max(0, plugin.getConfig().getInt(EXTRA_PERCENT_PATH, 7));
        int max = clampPercent(plugin.getConfig().getInt(MAX_PERCENT_PATH, 50));
        int window = Math.max(1, plugin.getConfig().getInt(STACK_WINDOW_PATH, 20));
        int minimum = Math.max(0, plugin.getConfig().getInt(MINIMUM_LOSS_PATH, 5));
        return new XpPenaltyConfig(base, extra, max, window, minimum);
    }

    private int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record XpPenaltyConfig(
        int baseLossPercent,
        int extraLossPerRecentDeathPercent,
        int maxLossPercent,
        int stackWindowMinutes,
        int minimumXpLoss
    ) {
    }

    public record XpPenaltyResult(int lossPercent, int lostExp, int newTotalExp, int recentDeaths) {
    }
}
