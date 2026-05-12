package be.ucll.it.courses.backend.repository;

import be.ucll.it.courses.backend.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    Optional<Device> findByDeviceIdAndDeviceToken(String deviceId, String deviceToken);
}
