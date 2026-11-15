package de.drucken.service;

import de.drucken.model.PrintJob;
import de.drucken.model.PrintJobRequest;
import de.drucken.persistence.PrintJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class PrintJobServiceTest {

    @Autowired
    PrintJobService service;

    @Autowired
    PrintJobRepository repository;

    @MockitoBean
    PdfPrintService printService;

    @BeforeEach
    void leertAuftraege() {
        repository.deleteAll();
    }

    @Test
    void sendetAsynchronUndPersistiertDenAbschluss() throws Exception {
        Path pdf = Path.of("report.pdf");
        when(printService.pdfDateiFinden("report.pdf")).thenReturn(pdf);
        AtomicReference<PrintJobListener> listener = new AtomicReference<>();
        CountDownLatch sendStarted = new CountDownLatch(1);
        CountDownLatch releaseSend = new CountDownLatch(1);
        doAnswer(invocation -> {
            listener.set(invocation.getArgument(2));
            sendStarted.countDown();
            assertTrue(releaseSend.await(5, TimeUnit.SECONDS));
            return null;
        }).when(printService).senden(eq(pdf), eq(2), any());

        PrintJob created = service.erstellen(new PrintJobRequest("report.pdf", 2));
        assertEquals(PrintJob.PrintStatus.WARTEND, created.status());
        assertTrue(sendStarted.await(5, TimeUnit.SECONDS));

        releaseSend.countDown();
        listener.get().printJobCompleted(new PrintJobEvent(mock(DocPrintJob.class), PrintJobEvent.JOB_COMPLETE));

        assertTrue(warteAufStatus(created.id(), PrintJob.PrintStatus.FERTIG));
        assertEquals(PrintJob.PrintStatus.FERTIG, service.finden(created.id()).orElseThrow().status());
    }

    @Test
    void persistiertSynchroneDruckfehlerAmAuftrag() throws Exception {
        Path pdf = Path.of("report.pdf");
        when(printService.pdfDateiFinden("report.pdf")).thenReturn(pdf);
        doThrow(new PrintException("Printer unavailable"))
                .when(printService).senden(eq(pdf), eq(1), any());

        PrintJob job = service.erstellen(new PrintJobRequest("report.pdf", 1));

        assertTrue(warteAufStatus(job.id(), PrintJob.PrintStatus.FEHLER));
        assertEquals("Printer unavailable", service.finden(job.id()).orElseThrow().fehlermeldung());
    }

    private boolean warteAufStatus(long id, PrintJob.PrintStatus status) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (service.finden(id).map(PrintJob::status).orElse(null) == status) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
    }
}
