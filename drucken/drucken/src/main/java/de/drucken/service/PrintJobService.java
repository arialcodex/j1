package de.drucken.service;

import de.drucken.model.PrintJob;
import de.drucken.model.PrintJob.PrintStatus;
import de.drucken.model.PrintJobRequest;
import de.drucken.persistence.PrintJobEntity;
import de.drucken.persistence.PrintJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.core.task.TaskRejectedException;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

@Service
public class PrintJobService {

    private final PrintJobRepository jobRepository;
    private final PdfPrintService pdfPrintService;
    private final AsyncPrintJobSubmissionService submissionService;
    private final PrintJobStatusService statusService;

    public PrintJobService(
            PrintJobRepository jobRepository,
            PdfPrintService pdfPrintService,
            AsyncPrintJobSubmissionService submissionService,
            PrintJobStatusService statusService) {
        this.jobRepository = jobRepository;
        this.pdfPrintService = pdfPrintService;
        this.submissionService = submissionService;
        this.statusService = statusService;
    }

    public PrintJob erstellen(PrintJobRequest request) {
        pdfPrintService.pdfDateiFinden(request.dokument());
        Instant now = Instant.now();
        PrintJobEntity job = jobRepository.save(
                new PrintJobEntity(request.dokument(), request.kopien(), PrintStatus.WARTEND, now));

        try {
            submissionService.senden(job.getId());
        } catch (TaskRejectedException exception) {
            statusService.aktualisieren(
                    job.getId(), PrintStatus.FEHLER, "Die Druckwarteschlange ist ausgelastet.");
            return jobRepository.findById(job.getId()).orElseThrow().toModel();
        }

        return job.toModel();
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
