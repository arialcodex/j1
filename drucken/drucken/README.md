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
- `POST /api/druckauftraege` – neuer Auftrag, Body: `{"dokument":"test.pdf","kopien":2}`
- `GET  /actuator/health` – Health-Check

## Struktur
```
src/main/java/de/drucken/
  DruckenApplication.java
  controller/   REST-Controller
  service/      Geschäftslogik
  model/        Datenmodelle
  config/       Konfiguration
```
