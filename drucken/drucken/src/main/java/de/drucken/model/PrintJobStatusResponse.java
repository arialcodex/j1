package de.drucken.model;

import java.time.Instant;

public record PrintJobStatusResponse(
        Long id,
        PrintJob.PrintStatus status,
        Instant aktualisiertAm,
        String fehlermeldung) {
}
