package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.client.exception.StatsClientException;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class StatsClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;

    public StatsClient(DiscoveryClient discoveryClient,
                       RestTemplate restTemplate,
                       @Value("${stats-server.service-id:stats-server}") String statsServiceId) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = restTemplate;
        this.statsServiceId = statsServiceId;
        this.retryTemplate = createRetryTemplate();
    }

    private RetryTemplate createRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new StatsClientException("No instances of " + statsServiceId + " found"));
        } catch (Exception e) {
            throw new StatsClientException("Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId, e);
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    public void saveHit(EndpointHitDto hitDto) {
        try {
            URI uri = makeUri("/hit");
            log.info("Отправка хита на {}: {}", uri, hitDto.getUri());

            HttpEntity<EndpointHitDto> request = new HttpEntity<>(hitDto);
            restTemplate.postForEntity(uri, request, Void.class);

            log.info("Хит успешно сохранен");
        } catch (Exception e) {
            log.error("Не удалось отправить хит в StatsService", e);
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        try {
            String path = String.format("/stats?start=%s&end=%s&unique=%s",
                    start.format(FORMATTER),
                    end.format(FORMATTER),
                    unique);

            if (uris != null && !uris.isEmpty()) {
                path += "&uris=" + String.join(",", uris);
            }

            URI uri = makeUri(path);
            log.info("Запрос статистики: {}", uri);

            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {}
            );

            List<ViewStatsDto> stats = response.getBody() != null ? response.getBody() : Collections.emptyList();
            log.info("Получена статистика: {}", stats);

            return stats;
        } catch (Exception e) {
            log.error("Не удалось получить статистику из StatsService", e);
            return Collections.emptyList();
        }
    }
}