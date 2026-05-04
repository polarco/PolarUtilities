package br.com.polarutilities.feature.update;

import java.time.Instant;

public record UpdateCheckResult(
    String currentVersion,
    UpdateMetadata metadata,
    boolean updateAvailable,
    Instant checkedAt
) {
}
