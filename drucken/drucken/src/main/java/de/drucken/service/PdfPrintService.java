package de.drucken.service;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.event.PrintJobListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;

@Service
public class PdfPrintService {

    private final Path pdfDirectory;
    private final String printerName;

    public PdfPrintService(
            @Value("${drucken.pdf-verzeichnis:pdfs}") String pdfDirectory,
            @Value("${drucken.drucker-name}") String printerName) {
        this.pdfDirectory = Path.of(pdfDirectory).toAbsolutePath().normalize();
        this.printerName = printerName;
    }

    public Path pdfDateiFinden(String dokument) {
        try {
            Path requested = Path.of(dokument);
            if (requested.isAbsolute() || requested.normalize().startsWith("..")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Das Dokument muss innerhalb des PDF-Verzeichnisses liegen.");
            }

            Path root = pdfDirectory.toRealPath();
            Path pdf = root.resolve(requested).toRealPath();
            if (!pdf.startsWith(root) || !Files.isRegularFile(pdf) || !Files.isReadable(pdf)
                    || !pdf.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Das Dokument muss eine lesbare PDF-Datei im PDF-Verzeichnis sein.");
            }
            return pdf;
        } catch (InvalidPathException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungültiger Dokumentpfad.", exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Die PDF-Datei oder das PDF-Verzeichnis ist nicht verfügbar.", exception);
        }
    }

    public void senden(Path pdf, int kopien, PrintJobListener listener) throws PrintException {
        PrintService printer = findeDrucker();
        DocFlavor flavor = DocFlavor.INPUT_STREAM.PDF;
        if (!printer.isDocFlavorSupported(flavor)) {
            throw new PrintException("Der Drucker unterstützt keine PDF-Dokumente.");
        }

        HashPrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        Copies copies = new Copies(kopien);
        if (!printer.isAttributeValueSupported(copies, flavor, attributes)) {
            throw new PrintException("Der Drucker unterstützt die angeforderte Anzahl von Kopien nicht.");
        }
        attributes.add(copies);

        DocPrintJob job = printer.createPrintJob();
        job.addPrintJobListener(listener);
        try (var input = Files.newInputStream(pdf)) {
            Doc document = new SimpleDoc(input, flavor, null);
            try {
                job.print(document, attributes);
            } catch (PrintException exception) {
                throw new PrintServiceUnavailableException(
                        "Der Drucker '" + printerName + "' ist nicht erreichbar.", exception);
            }
        } catch (IOException exception) {
            throw new PrintException(exception);
        }
    }

    private PrintService findeDrucker() throws PrintException {
        for (PrintService printer : PrintServiceLookup.lookupPrintServices(null, null)) {
            if (printer.getName().equals(printerName)) {
                return printer;
            }
        }
        throw new PrintServiceUnavailableException(
                "Der konfigurierte Drucker '" + printerName + "' wurde nicht gefunden.");
    }
}
