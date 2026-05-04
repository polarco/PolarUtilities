package br.com.polarutilities.feature.update;

public record UpdateMetadata(
    String latest,
    String downloadUrl,
    String changelogUrl,
    String message,
    boolean critical
) {
}
