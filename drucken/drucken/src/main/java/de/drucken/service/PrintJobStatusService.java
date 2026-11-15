package de.drucken.service;

import de.drucken.model.PrintJob.PrintStatus;
import de.drucken.persistence.PrintJobEntity;
import de.drucken.persistence.PrintJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PrintJobStatusService {

    private final PrintJobRepository jobRepository;

    public PrintJobStatusService(PrintJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public void aktualisieren(long id, PrintStatus status, String fehlermeldung) {
        jobRepository.findById(id).ifPresent(job -> {
            if (job.getStatus() != PrintStatus.FERTIG && job.getStatus() != PrintStatus.FEHLER) {
                job.aktualisieren(status, Instant.now(), fehlermeldung);
                jobRepository.save(job);
            }
        });
    }
}
