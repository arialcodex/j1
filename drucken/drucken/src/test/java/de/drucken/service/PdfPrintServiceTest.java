package de.drucken.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfPrintServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void findetPdfInnerhalbDesKonfiguriertenVerzeichnisses() throws Exception {
        Path pdfDirectory = Files.createDirectory(tempDir.resolve("pdfs"));
        Path pdf = Files.writeString(pdfDirectory.resolve("report.pdf"), "%PDF-test");
        PdfPrintService service = new PdfPrintService(pdfDirectory.toString(), "Printer1");

        assertEquals(pdf.toRealPath(), service.pdfDateiFinden("report.pdf"));
    }

    @Test
    void lehntPfadeAusserhalbDesPdfVerzeichnissesAb() throws Exception {
        Path pdfDirectory = Files.createDirectory(tempDir.resolve("pdfs"));
        Files.writeString(tempDir.resolve("secret.pdf"), "%PDF-test");
        PdfPrintService service = new PdfPrintService(pdfDirectory.toString(), "Printer1");

        assertThrows(ResponseStatusException.class, () -> service.pdfDateiFinden("../secret.pdf"));
    }

    @Test
    void lehntNichtPdfDateienAb() throws Exception {
        Path pdfDirectory = Files.createDirectory(tempDir.resolve("pdfs"));
        Files.writeString(pdfDirectory.resolve("report.txt"), "not a pdf");
        PdfPrintService service = new PdfPrintService(pdfDirectory.toString(), "Printer1");

        assertThrows(ResponseStatusException.class, () -> service.pdfDateiFinden("report.txt"));
    }
}
