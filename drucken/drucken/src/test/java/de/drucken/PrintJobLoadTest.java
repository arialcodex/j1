package de.drucken;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.drucken.service.AsyncPrintJobSubmissionService;
import de.drucken.service.PdfPrintService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PrintJobLoadTest {

    private static final int PARALLELE_CLIENTS = 10;
    private static final int ANFRAGEN_PRO_CLIENT = 20;

    @LocalServerPort
    private int port;

    @MockitoBean
    private PdfPrintService pdfPrintService;

    @MockitoBean
    private AsyncPrintJobSubmissionService submissionService;

    @Test
    void beantwortetParalleleApiAufrufe() throws Exception {
        URI endpoint = URI.create("http://localhost:" + port + "/api/druckauftraege");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        ExecutorService executor = Executors.newFixedThreadPool(PARALLELE_CLIENTS);
        CountDownLatch bereit = new CountDownLatch(PARALLELE_CLIENTS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> ergebnisse = new ArrayList<>();

        try {
            for (int i = 0; i < PARALLELE_CLIENTS; i++) {
                ergebnisse.add(executor.submit(() -> {
                    bereit.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Startsignal fuer Lasttest wurde nicht empfangen.");
                    }

                    for (int anfrage = 0; anfrage < ANFRAGEN_PRO_CLIENT; anfrage++) {
                        HttpRequest request = HttpRequest.newBuilder(endpoint)
                                .timeout(Duration.ofSeconds(10))
                                .GET()
                                .build();
                        HttpResponse<String> response = client.send(
                                request, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() != 200
                                || !response.body().trim().startsWith("[")
                                || !response.body().trim().endsWith("]")) {
                            throw new IllegalStateException(
                                    "Unerwartete API-Antwort: HTTP " + response.statusCode());
                        }
                    }
                    return ANFRAGEN_PRO_CLIENT;
                }));
            }

            assertTrue(bereit.await(5, TimeUnit.SECONDS), "Clients wurden nicht rechtzeitig bereit.");
            long startzeit = System.nanoTime();
            start.countDown();

            int erfolgreicheAnfragen = 0;
            for (Future<Integer> ergebnis : ergebnisse) {
                erfolgreicheAnfragen += ergebnis.get(30, TimeUnit.SECONDS);
            }

            long dauerMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startzeit);
            assertEquals(PARALLELE_CLIENTS * ANFRAGEN_PRO_CLIENT, erfolgreicheAnfragen);
            System.out.printf(
                    "Lasttest: %d parallele Clients, %d erfolgreiche Requests in %d ms (%.1f Requests/s)%n",
                    PARALLELE_CLIENTS,
                    erfolgreicheAnfragen,
                    dauerMillis,
                    erfolgreicheAnfragen * 1000.0 / dauerMillis);
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
