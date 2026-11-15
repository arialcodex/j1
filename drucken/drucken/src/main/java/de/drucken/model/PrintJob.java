package de.drucken.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record PrintJob(
        Long id,
        @NotBlank String dokument,
        @Min(1) int kopien,
        PrintStatus status,
        Instant erstelltAm,
        Instant aktualisiertAm,
        String fehlermeldung,
        int versuche,
        Instant naechsterVersuchAm) {

    public enum PrintStatus { WARTEND, IN_BEARBEITUNG, FERTIG, FEHLER }
}
