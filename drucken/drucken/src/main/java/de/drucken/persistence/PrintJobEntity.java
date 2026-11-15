package de.drucken.persistence;

import de.drucken.model.PrintJob;
import de.drucken.model.PrintJob.PrintStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "print_jobs")
public class PrintJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String dokument;

    @Column(nullable = false)
    private int kopien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrintStatus status;

    @Column(nullable = false, updatable = false)
    private Instant erstelltAm;

    @Column(nullable = false)
    private Instant aktualisiertAm;

    @Column(length = 2000)
    private String fehlermeldung;

    protected PrintJobEntity() {
    }

    public PrintJobEntity(String dokument, int kopien, PrintStatus status, Instant erstelltAm) {
        this.dokument = dokument;
        this.kopien = kopien;
        this.status = status;
        this.erstelltAm = erstelltAm;
        this.aktualisiertAm = erstelltAm;
    }

    public Long getId() {
        return id;
    }

    public PrintStatus getStatus() {
        return status;
    }

    public void aktualisieren(PrintStatus status, Instant aktualisiertAm, String fehlermeldung) {
        this.status = status;
        this.aktualisiertAm = aktualisiertAm;
        this.fehlermeldung = fehlermeldung;
    }

    public PrintJob toModel() {
        return new PrintJob(id, dokument, kopien, status, erstelltAm, aktualisiertAm, fehlermeldung);
    }
}
