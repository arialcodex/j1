package de.drucken.service;

import de.drucken.model.PrintJob.PrintStatus;
import de.drucken.persistence.PrintJobEntity;
import de.drucken.persistence.PrintJobRepository;
import javax.print.PrintException;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
public class AsyncPrintJobSubmissionService {

    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofSeconds(10), Duration.ofMinutes(1), Duration.ofMinutes(5));

    private final PdfPrintService pdfPrintService;
    private final PrintJobStatusService statusService;
    private final PrintJobRepository jobRepository;
    private final TaskScheduler retryTaskScheduler;
    private final Executor printTaskExecutor;

    public AsyncPrintJobSubmissionService(
            PdfPrintService pdfPrintService,
            PrintJobStatusService statusService,
            PrintJobRepository jobRepository,
            @Qualifier("printRetryTaskScheduler") TaskScheduler retryTaskScheduler,
            @Qualifier("printTaskExecutor") Executor printTaskExecutor) {
        this.pdfPrintService = pdfPrintService;
        this.statusService = statusService;
        this.jobRepository = jobRepository;
        this.retryTaskScheduler = retryTaskScheduler;
        this.printTaskExecutor = printTaskExecutor;
    }

    @Async("printTaskExecutor")
    public void senden(long jobId) {
        versuch(jobId);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ausstehendeWiederholungenWiederherstellen() {
        for (PrintJobEntity job : jobRepository.findAllByStatusOrderByIdAsc(PrintStatus.WARTEND)) {
            if (job.getNaechsterVersuchAm() == null) {
                try {
                    printTaskExecutor.execute(() -> versuch(job.getId()));
                } catch (RejectedExecutionException exception) {
                    statusService.aktualisieren(
                            job.getId(), PrintStatus.FEHLER, "Die Druckwarteschlange ist ausgelastet.");
                }
            } else {
                wiederholungEinplanen(job.getId(), job.getNaechsterVersuchAm());
            }
        }
    }

    private void versuch(long jobId) {
        int attempt = statusService.versuchStarten(jobId);
        if (attempt == 0) {
            return;
        }

        PrintJobEntity job = jobRepository.findById(jobId).orElseThrow();
        try {
            var pdf = pdfPrintService.pdfDateiFinden(job.getDokument());
            pdfPrintService.senden(pdf, job.getKopien(), new StatusListener(jobId, attempt));
        } catch (PrintServiceUnavailableException exception) {
            druckfehlerBehandeln(jobId, attempt, exception.getMessage());
        } catch (PrintException exception) {
            statusService.aktualisieren(jobId, PrintStatus.FEHLER, Objects.requireNonNullElse(
                    exception.getMessage(), "Der Druckauftrag konnte nicht gesendet werden."));
        } catch (ResponseStatusException exception) {
            statusService.aktualisieren(jobId, PrintStatus.FEHLER, Objects.requireNonNullElse(
                    exception.getReason(), "Die PDF-Datei ist nicht verfügbar."));
        }
    }

    private void druckfehlerBehandeln(long jobId, int attempt, String message) {
        if (attempt <= RETRY_DELAYS.size()) {
            Instant retryAt = Instant.now().plus(RETRY_DELAYS.get(attempt - 1));
            if (statusService.wiederholungPlanen(jobId, message, retryAt)) {
                wiederholungEinplanen(jobId, retryAt);
            }
            return;
        }

        statusService.aktualisieren(jobId, PrintStatus.FEHLER,
                "Der Drucker war nach " + attempt + " Versuchen nicht erreichbar: " + message);
    }

    private void wiederholungEinplanen(long jobId, Instant retryAt) {
        try {
            retryTaskScheduler.schedule(() -> {
                try {
                    printTaskExecutor.execute(() -> versuch(jobId));
                } catch (RejectedExecutionException exception) {
                    statusService.aktualisieren(
                            jobId, PrintStatus.FEHLER, "Die Druckwarteschlange ist ausgelastet.");
                }
            }, retryAt);
        } catch (RejectedExecutionException exception) {
            statusService.aktualisieren(
                    jobId, PrintStatus.FEHLER, "Der Wiederholungsversuch konnte nicht eingeplant werden.");
        }
    }

    private final class StatusListener implements PrintJobListener {
        private final long jobId;
        private final int attempt;

        private StatusListener(long jobId, int attempt) {
            this.jobId = jobId;
            this.attempt = attempt;
        }

        @Override
        public void printDataTransferCompleted(PrintJobEvent event) {
            statusService.aktualisieren(jobId, PrintStatus.IN_BEARBEITUNG, null);
        }

        @Override
        public void printJobCompleted(PrintJobEvent event) {
            statusService.aktualisieren(jobId, PrintStatus.FERTIG, null);
        }

        @Override
        public void printJobFailed(PrintJobEvent event) {
            druckfehlerBehandeln(jobId, attempt, "Der PrintService meldet einen Druckfehler.");
        }

        @Override
        public void printJobCanceled(PrintJobEvent event) {
            statusService.aktualisieren(jobId, PrintStatus.FEHLER, "Der Druckauftrag wurde abgebrochen.");
        }

        @Override
        public void printJobNoMoreEvents(PrintJobEvent event) {
        }

        @Override
        public void printJobRequiresAttention(PrintJobEvent event) {
        }
    }
}
