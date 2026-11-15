package de.drucken.service;

import de.drucken.model.PrintJob;
import de.drucken.model.PrintJob.PrintStatus;
import de.drucken.model.PrintJobRequest;
import de.drucken.persistence.PrintJobEntity;
import de.drucken.persistence.PrintJobRepository;
import javax.print.PrintException;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PrintJobService {

    private final PrintJobRepository jobRepository;
    private final PdfPrintService pdfPrintService;

    public PrintJobService(PrintJobRepository jobRepository, PdfPrintService pdfPrintService) {
        this.jobRepository = jobRepository;
        this.pdfPrintService = pdfPrintService;
    }

    public PrintJob erstellen(PrintJobRequest request) {
        var pdf = pdfPrintService.pdfDateiFinden(request.dokument());
        Instant now = Instant.now();
        PrintJobEntity job = jobRepository.save(
                new PrintJobEntity(request.dokument(), request.kopien(), PrintStatus.WARTEND, now));

        aktualisieren(job.getId(), PrintStatus.IN_BEARBEITUNG, null);
        try {
            pdfPrintService.senden(pdf, request.kopien(), new StatusListener(job.getId()));
        } catch (PrintException exception) {
            aktualisieren(job.getId(), PrintStatus.FEHLER, Objects.requireNonNullElse(
                    exception.getMessage(), "Der Druckauftrag konnte nicht gesendet werden."));
        }

        return jobRepository.findById(job.getId()).orElseThrow().toModel();
    }

    private void aktualisieren(long id, PrintStatus status, String fehlermeldung) {
        jobRepository.findById(id).ifPresent(job -> {
            if (job.getStatus() != PrintStatus.FERTIG && job.getStatus() != PrintStatus.FEHLER) {
                job.aktualisieren(status, Instant.now(), fehlermeldung);
                jobRepository.save(job);
            }
        });
    }

    private final class StatusListener implements PrintJobListener {
        private final long id;

        private StatusListener(long id) {
            this.id = id;
        }

        @Override
        public void printDataTransferCompleted(PrintJobEvent event) {
            aktualisieren(id, PrintStatus.IN_BEARBEITUNG, null);
        }

        @Override
        public void printJobCompleted(PrintJobEvent event) {
            aktualisieren(id, PrintStatus.FERTIG, null);
        }

        @Override
        public void printJobFailed(PrintJobEvent event) {
            aktualisieren(id, PrintStatus.FEHLER, "Der PrintService meldet einen Druckfehler.");
        }

        @Override
        public void printJobCanceled(PrintJobEvent event) {
            aktualisieren(id, PrintStatus.FEHLER, "Der Druckauftrag wurde abgebrochen.");
        }

        @Override
        public void printJobNoMoreEvents(PrintJobEvent event) {
        }

        @Override
        public void printJobRequiresAttention(PrintJobEvent event) {
        }
    }

    public List<PrintJob> alle() {
        return jobRepository.findAllByOrderByIdAsc().stream()
                .map(PrintJobEntity::toModel)
                .toList();
    }

    public Optional<PrintJob> finden(long id) {
        return jobRepository.findById(id).map(PrintJobEntity::toModel);
    }
}
