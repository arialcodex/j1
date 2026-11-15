package de.drucken.service;

import de.drucken.model.PrintJob;
import de.drucken.model.PrintJob.PrintStatus;
import de.drucken.model.PrintJobRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Platzhalter-Service mit In-Memory-Speicher. Später durch Datenbank/Druckanbindung ersetzen. */
@Service
public class PrintJobService {

    private final Map<Long, PrintJob> jobs = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public PrintJob erstellen(PrintJobRequest request) {
        long id = sequence.incrementAndGet();
        PrintJob job = new PrintJob(id, request.dokument(), request.kopien(),
                PrintStatus.WARTEND, Instant.now());
        jobs.put(id, job);
        return job;
    }

    public List<PrintJob> alle() {
        return jobs.values().stream()
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .toList();
    }

    public Optional<PrintJob> finden(long id) {
        return Optional.ofNullable(jobs.get(id));
    }
}
