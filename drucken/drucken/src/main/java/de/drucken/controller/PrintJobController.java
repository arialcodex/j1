package de.drucken.controller;

import de.drucken.model.PrintJob;
import de.drucken.model.PrintJobRequest;
import de.drucken.model.PrintJobStatusResponse;
import de.drucken.service.PrintJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/druckauftraege")
public class PrintJobController {

    private final PrintJobService service;

    public PrintJobController(PrintJobService service) {
        this.service = service;
    }

    @GetMapping
    public List<PrintJob> alle() {
        return service.alle();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrintJob> einzeln(@PathVariable long id) {
        return service.finden(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<PrintJobStatusResponse> status(@PathVariable long id) {
        return service.finden(id)
                .map(job -> ResponseEntity.ok(new PrintJobStatusResponse(
                        job.id(), job.status(), job.aktualisiertAm(), job.fehlermeldung(),
                        job.versuche(), job.naechsterVersuchAm())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrintJob erstellen(@Valid @RequestBody PrintJobRequest request) {
        return service.erstellen(request);
    }
}
