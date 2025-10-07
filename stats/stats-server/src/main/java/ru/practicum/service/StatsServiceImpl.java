package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    public void saveHit(EndpointHit hit) {
        statsRepository.save(hit);
    }

    //проверяем, что дата начала не позже даты окончания
    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }
        return statsRepository.getStats(start, end, uris != null ? uris : null, Boolean.TRUE.equals(unique));
    }
}