package de.drucken.service;

import de.drucken.model.PrintJob.PrintStatus;
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
                job.aktualisieren(status, Instant.now(), fehlermeldung, null);
                jobRepository.save(job);
            }
        });
    }

    @Transactional
    public int versuchStarten(long id) {
        return jobRepository.findById(id)
                .filter(job -> job.getStatus() == PrintStatus.WARTEND)
                .map(job -> {
                    job.versuchStarten(Instant.now());
                    jobRepository.save(job);
                    return job.getVersuche();
                })
                .orElse(0);
    }

    @Transactional
    public boolean wiederholungPlanen(long id, String fehlermeldung, Instant naechsterVersuchAm) {
        return jobRepository.findById(id)
                .filter(job -> job.getStatus() == PrintStatus.IN_BEARBEITUNG)
                .map(job -> {
                    job.aktualisieren(PrintStatus.WARTEND, Instant.now(), fehlermeldung, naechsterVersuchAm);
                    jobRepository.save(job);
                    return true;
                })
                .orElse(false);
    }
}
