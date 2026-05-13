package be.ucll.it.courses.backend.repository;

import be.ucll.it.courses.backend.model.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
}
