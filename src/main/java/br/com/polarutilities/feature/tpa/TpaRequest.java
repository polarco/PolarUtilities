package br.com.polarutilities.feature.tpa;

import java.time.Instant;
import java.util.UUID;

public record TpaRequest(
    UUID requesterId,
    String requesterName,
    UUID targetId,
    String targetName,
    TpaRequestType type,
    Instant createdAt
) {
}
