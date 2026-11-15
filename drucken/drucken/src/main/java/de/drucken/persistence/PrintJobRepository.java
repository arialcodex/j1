package de.drucken.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrintJobRepository extends JpaRepository<PrintJobEntity, Long> {
    List<PrintJobEntity> findAllByOrderByIdAsc();
}
