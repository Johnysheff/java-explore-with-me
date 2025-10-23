package ru.practicum.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.ViewStats;
import ru.practicum.request.ParticipationRequestRepository;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.statsclient.StatsClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventStatisticsService {
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    public Long getConfirmedRequestsCount(Long eventId) {
        try {
            return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        } catch (Exception e) {
            log.error("Ошибка при подсчете подтвержденных запросов для события {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    public Long getViewsCount(Long eventId) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();
            String uri = "/events/" + eventId;

            List<ViewStats> stats = statsClient.getStats(start, end, List.of(uri), true);
            return stats.isEmpty() ? 0L : stats.get(0).getHits();
        } catch (Exception e) {
            log.error("Ошибка при получении статистики просмотров для события {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    public Map<Long, Long> getViewsCounts(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);

            return stats.stream()
                    .collect(Collectors.toMap(
                            stat -> Long.parseLong(stat.getUri().substring("/events/".length())),
                            ViewStats::getHits
                    ));
        } catch (Exception e) {
            log.error("Ошибка при получении статистики просмотров для списка событий: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}