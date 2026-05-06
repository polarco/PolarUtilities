package br.com.polarutilities.feature.difficulty;

import java.util.Locale;

public enum DifficultyMode {
    EASY(0.80D, 0.80D),
    NORMAL(1.00D, 1.00D),
    HARD(1.35D, 1.50D);

    private final double defaultDamageMultiplier;
    private final double defaultXpMultiplier;

    DifficultyMode(double defaultDamageMultiplier, double defaultXpMultiplier) {
        this.defaultDamageMultiplier = defaultDamageMultiplier;
        this.defaultXpMultiplier = defaultXpMultiplier;
    }

    public double defaultDamageMultiplier() {
        return defaultDamageMultiplier;
    }

    public double defaultXpMultiplier() {
        return defaultXpMultiplier;
    }

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DifficultyMode fromConfig(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return NORMAL;
        }

        try {
            return DifficultyMode.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return NORMAL;
        }
    }
}
