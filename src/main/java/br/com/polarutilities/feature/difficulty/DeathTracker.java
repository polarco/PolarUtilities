package br.com.polarutilities.feature.difficulty;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DeathTracker {
    private final Map<UUID, Deque<Instant>> recentDeaths = new HashMap<>();

    public int recordDeath(UUID playerId, Duration stackWindow) {
        Instant now = Instant.now();
        Deque<Instant> deaths = recentDeaths.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        prune(deaths, now.minus(stackWindow));
        int previousDeaths = deaths.size();
        deaths.addLast(now);
        return previousDeaths;
    }

    public void clear() {
        recentDeaths.clear();
    }

    private void prune(Deque<Instant> deaths, Instant oldestAllowed) {
        while (!deaths.isEmpty() && deaths.peekFirst().isBefore(oldestAllowed)) {
            deaths.removeFirst();
        }
    }
}
