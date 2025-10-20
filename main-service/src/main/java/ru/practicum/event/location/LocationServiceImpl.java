package ru.practicum.event.location;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.model.LocationRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    @Override
    @Transactional
    public Location getOrSave(LocationDto locationDto) {
        log.debug("Поиск или сохранение локации: lat={}, lon={}", locationDto.getLat(), locationDto.getLon());

        Optional<Location> existingLocation = locationRepository.findByLatAndLon(
                locationDto.getLat(), locationDto.getLon());

        if (existingLocation.isPresent()) {
            log.debug("Локация найдена: {}", existingLocation.get().getId());
            return existingLocation.get();
        }

        Location newLocation = locationMapper.toLocation(locationDto);
        Location savedLocation = locationRepository.save(newLocation);

        log.debug("Новая локация сохранена: {}", savedLocation.getId());
        return savedLocation;
    }
}