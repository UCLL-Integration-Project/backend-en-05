package be.ucll.it.courses.backend.repository;

import be.ucll.it.courses.backend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {
    Optional<Event> findFirstByOrderByTimestampDesc();
    Optional<Event> findFirstByDeviceIdAndTimestampBetween(String deviceId, OffsetDateTime from, OffsetDateTime to);
    Optional<Event> findFirstByLatitudeIsNotNullOrderByTimestampDesc();
}
