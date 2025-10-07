package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    //сохраняем информацию
    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody EndpointHit hit) {
        statsService.saveHit(hit);
    }

    //возвращаем статистику за указанный период
    @GetMapping("/stats")
    public List<ViewStats> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {
        return statsService.getStats(start, end, uris, unique);
    }
}