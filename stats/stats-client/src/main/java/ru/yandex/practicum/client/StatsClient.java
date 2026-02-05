package ru.yandex.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.yandex.practicum.client.exception.StatsClientException;
import ru.yandex.practicum.dto.EndpointHitDto;
import ru.yandex.practicum.dto.ViewStatsDto;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class StatsClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RestClient restClient;

    public StatsClient(@Value("${STATS_SERVER_URL:http://stats-server:9090}") String serverUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .build();
    }

    public void saveHit(EndpointHitDto hitDto) {
        try {
            handleErrors(restClient.post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hitDto))
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Не удалось отправить хит в StatsService", e);
            throw new StatsClientException("Не удалось отправить хит в StatsService", e);
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {
        try {
            return handleErrors(restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/stats")
                                .queryParam("start", start.format(FORMATTER))
                                .queryParam("end", end.format(FORMATTER))
                                .queryParam("unique", unique);
                        if (uris != null && !uris.isEmpty()) {
                            for (String u : uris) {
                                uriBuilder.queryParam("uris", u);
                            }
                        }
                        return uriBuilder.build();
                    }))
                    .body(new org.springframework.core.ParameterizedTypeReference<List<ViewStatsDto>>() {
                    });
        } catch (RestClientException e) {
            log.error("Не удалось получить статистику из StatsService", e);
            throw new StatsClientException("Не удалось получить статистику из StatsService", e);
        }
    }

    private RestClient.ResponseSpec handleErrors(RestClient.RequestHeadersSpec<?> request) {
        return request.retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    String errorBody = readBodyAsString(res);
                    throw new StatsClientException("Ошибка клиента (4xx) при обращении к StatsService: "
                            + (errorBody.isBlank() ? "сообщение ошибки не предоставлено" : errorBody));
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    String errorBody = readBodyAsString(res);
                    throw new StatsClientException("Ошибка сервера (5xx) в StatsService: "
                            + (errorBody.isBlank() ? "сообщение ошибки не предоставлено" : errorBody));
                });
    }

    private String readBodyAsString(ClientHttpResponse res) {
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(res.getBody(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(java.util.stream.Collectors.joining("\n"));
        } catch (Exception e) {
            return "не удалось прочитать тело ответа";
        }
    }
}