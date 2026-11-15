# Drucken

Spring-Boot-Projekt (Java 21, Maven).

## Starten
```bash
mvn spring-boot:run
```

## Tests
```bash
mvn test
```

## Beispiel-Endpunkte
- `GET  /api/druckauftraege` – alle Aufträge
- `GET  /api/druckauftraege/{id}` – ein Auftrag
- `POST /api/druckauftraege` – PDF-Druckauftrag, Body: `{"dokument":"test.pdf","kopien":2}`
- `GET  /actuator/health` – Health-Check

PDF-Dateien müssen im konfigurierten Verzeichnis `drucken.pdf-verzeichnis` liegen
(Standard: `pdfs`). Der Drucker wird über `drucken.drucker-name` ausgewählt
(Standard: `Printer1`). Der Druckauftrag unterstützt PDF direkt; unterstützt der
lokale Drucker das PDF-Format oder die gewünschte Kopienzahl nicht, wird der
Auftrag mit Status `FEHLER` und einer Fehlermeldung gespeichert.

Der Status kann über `GET /api/druckauftraege/{id}` nachverfolgt werden.
`FERTIG` wird erst gemeldet, wenn der lokale PrintService ein Abschlussereignis
sendet; `aktualisiertAm` enthält den Zeitpunkt der letzten Statusänderung.
Aufträge und Status werden in einer lokalen H2-Dateidatenbank (`drucken-db.mv.db`)
gespeichert und bleiben nach einem Neustart erhalten. Die JDBC-URL kann über
`spring.datasource.url` angepasst werden.

## Struktur
```
src/main/java/de/drucken/
  DruckenApplication.java
  controller/   REST-Controller
  service/      Geschäftslogik
  model/        Datenmodelle
  config/       Konfiguration
```
