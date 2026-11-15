package de.drucken.persistence;

import de.drucken.model.PrintJob.PrintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrintJobRepository extends JpaRepository<PrintJobEntity, Long> {
    List<PrintJobEntity> findAllByOrderByIdAsc();
    List<PrintJobEntity> findAllByStatusOrderByIdAsc(PrintStatus status);
}
