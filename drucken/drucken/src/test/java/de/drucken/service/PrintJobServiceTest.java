package de.drucken.service;

import de.drucken.model.PrintJob;
import de.drucken.model.PrintJobRequest;
import de.drucken.persistence.PrintJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import(PrintJobService.class)
class PrintJobServiceTest {

    @Autowired
    PrintJobService service;

    @Autowired
    PrintJobRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @MockitoBean
    PdfPrintService printService;

    @BeforeEach
    void leertAuftraege() {
        repository.deleteAll();
    }

    @Test
    void verfolgtUndPersistiertDenAbschlussDurchDenPrintService() throws Exception {
        Path pdf = Path.of("report.pdf");
        when(printService.pdfDateiFinden("report.pdf")).thenReturn(pdf);
        AtomicReference<PrintJobListener> listener = new AtomicReference<>();
        doAnswer(invocation -> {
            listener.set(invocation.getArgument(2));
            return null;
        }).when(printService).senden(eq(pdf), eq(2), any());

        PrintJob created = service.erstellen(new PrintJobRequest("report.pdf", 2));
        listener.get().printJobCompleted(new PrintJobEvent(mock(DocPrintJob.class), PrintJobEvent.JOB_COMPLETE));
        entityManager.flush();
        entityManager.clear();

        assertEquals(PrintJob.PrintStatus.IN_BEARBEITUNG, created.status());
        assertEquals(PrintJob.PrintStatus.FERTIG, service.finden(created.id()).orElseThrow().status());
    }

    @Test
    void persistiertSynchroneDruckfehlerAmAuftrag() throws Exception {
        Path pdf = Path.of("report.pdf");
        when(printService.pdfDateiFinden("report.pdf")).thenReturn(pdf);
        doThrow(new PrintException("Printer unavailable"))
                .when(printService).senden(eq(pdf), eq(1), any());

        PrintJob job = service.erstellen(new PrintJobRequest("report.pdf", 1));

        assertEquals(PrintJob.PrintStatus.FEHLER, job.status());
        assertEquals("Printer unavailable", service.finden(job.id()).orElseThrow().fehlermeldung());
    }
}
