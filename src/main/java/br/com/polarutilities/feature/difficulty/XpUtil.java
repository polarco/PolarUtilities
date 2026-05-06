package br.com.polarutilities.feature.difficulty;

import org.bukkit.entity.Player;

public final class XpUtil {
    private XpUtil() {
    }

    public static int totalExperience(Player player) {
        int level = Math.max(0, player.getLevel());
        int expInsideLevel = Math.round(player.getExp() * expToNextLevel(level));
        return totalExperienceAtLevel(level) + Math.max(0, expInsideLevel);
    }

    public static XpSnapshot snapshotFromTotal(int totalExperience) {
        int safeTotal = Math.max(0, totalExperience);
        int level = 0;
        while (totalExperienceAtLevel(level + 1) <= safeTotal) {
            level++;
        }
        int expIntoLevel = safeTotal - totalExperienceAtLevel(level);
        return new XpSnapshot(level, expIntoLevel, safeTotal);
    }

    private static int totalExperienceAtLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) Math.floor(2.5D * level * level - 40.5D * level + 360.0D);
        }
        return (int) Math.floor(4.5D * level * level - 162.5D * level + 2220.0D);
    }

    private static int expToNextLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        }
        if (level <= 30) {
            return 5 * level - 38;
        }
        return 9 * level - 158;
    }

    public record XpSnapshot(int level, int expIntoLevel, int totalExperience) {
    }
}
