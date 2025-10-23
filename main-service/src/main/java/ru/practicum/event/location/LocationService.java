package ru.practicum.event.location;

import ru.practicum.event.dto.LocationDto;

public interface LocationService {

    Location getOrSave(LocationDto dto);
}
