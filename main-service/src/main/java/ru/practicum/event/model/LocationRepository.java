package ru.practicum.event.model;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.event.location.Location;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByLatAndLon(Float lat, Float lon);
}