package de.drucken.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PrintJobRequest(
        @NotBlank String dokument,
        @Min(1) int kopien) {
}
