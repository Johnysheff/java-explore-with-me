package ru.practicum.statsclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class StatsClientImpl implements StatsClient {
    private final RestTemplate restTemplate;
    private final String serverUrl;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClientImpl(@Value("${stats-server.url:http://localhost:9090}") String serverUrl,
                           RestTemplateBuilder builder) {
        this.serverUrl = serverUrl;
        this.restTemplate = builder.build();
    }

    //отправляем информацию в сервис статистики
    @Override
    public void hit(EndpointHit endpointHit) {
        restTemplate.postForEntity(serverUrl + "/hit", endpointHit, Object.class);
    }

    //получаем статистику посещений за период
    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUrl + "/stats")
                .queryParam("start", encodeDateTime(start))
                .queryParam("end", encodeDateTime(end))
                .queryParam("unique", unique);
        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", String.join(",", uris));
        }
        ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(
                builder.toUriString(), ViewStats[].class);
        return Arrays.asList(response.getBody());
    }

    private String encodeDateTime(LocalDateTime dateTime) {
        return dateTime.format(formatter);
    }
}