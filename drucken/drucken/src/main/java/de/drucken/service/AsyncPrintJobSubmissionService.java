package de.drucken.service;

import de.drucken.model.PrintJob.PrintStatus;
import javax.print.PrintException;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Objects;

@Service
public class AsyncPrintJobSubmissionService {

    private final PdfPrintService pdfPrintService;
    private final PrintJobStatusService statusService;

    public AsyncPrintJobSubmissionService(
            PdfPrintService pdfPrintService,
            PrintJobStatusService statusService) {
        this.pdfPrintService = pdfPrintService;
        this.statusService = statusService;
    }

    @Async("printTaskExecutor")
    public void senden(long jobId, Path pdf, int kopien) {
        statusService.aktualisieren(jobId, PrintStatus.IN_BEARBEITUNG, null);
        try {
            pdfPrintService.senden(pdf, kopien, new StatusListener(jobId));
        } catch (PrintException exception) {
            statusService.aktualisieren(jobId, PrintStatus.FEHLER, Objects.requireNonNullElse(
                    exception.getMessage(), "Der Druckauftrag konnte nicht gesendet werden."));
        }
    }

    private final class StatusListener implements PrintJobListener {
        private final long jobId;

        private StatusListener(long jobId) {
            this.jobId = jobId;
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
            statusService.aktualisieren(jobId, PrintStatus.FEHLER, "Der PrintService meldet einen Druckfehler.");
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
