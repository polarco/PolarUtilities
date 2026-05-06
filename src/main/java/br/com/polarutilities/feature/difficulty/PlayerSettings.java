package br.com.polarutilities.feature.difficulty;

import java.util.UUID;

public final class PlayerSettings {
    private final UUID uuid;
    private final DifficultyMode difficulty;
    private final boolean keepInventory;

    public PlayerSettings(UUID uuid, DifficultyMode difficulty, boolean keepInventory) {
        this.uuid = uuid;
        this.difficulty = difficulty;
        this.keepInventory = keepInventory;
    }

    public UUID uuid() {
        return uuid;
    }

    public DifficultyMode difficulty() {
        return difficulty;
    }

    public boolean keepInventory() {
        return keepInventory;
    }
}
