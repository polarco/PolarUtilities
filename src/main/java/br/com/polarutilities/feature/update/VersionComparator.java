package br.com.polarutilities.feature.update;

import java.util.ArrayList;
import java.util.List;

public final class VersionComparator {
    private VersionComparator() {
    }

    public static int compare(String currentVersion, String latestVersion) {
        List<Integer> current = numericParts(currentVersion);
        List<Integer> latest = numericParts(latestVersion);
        int max = Math.max(current.size(), latest.size());

        for (int index = 0; index < max; index++) {
            int currentPart = index < current.size() ? current.get(index) : 0;
            int latestPart = index < latest.size() ? latest.get(index) : 0;
            if (currentPart != latestPart) {
                return Integer.compare(currentPart, latestPart);
            }
        }
        return 0;
    }

    private static List<Integer> numericParts(String version) {
        String normalized = version == null ? "" : version.trim().replaceFirst("^[vV]", "");
        String[] parts = normalized.split("[^0-9]+");
        List<Integer> numbers = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            try {
                numbers.add(Integer.parseInt(part));
            } catch (NumberFormatException ignored) {
                numbers.add(0);
            }
        }
        return numbers.isEmpty() ? List.of(0) : numbers;
    }
}
