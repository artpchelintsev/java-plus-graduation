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
import org.springframework.web.util.UriComponentsBuilder;
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
        List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
        if (instances == null || instances.isEmpty()) {
            throw new RuntimeException("No instances of " + statsServiceId + " found in Eureka");
        }
        return instances.get(0);
    }

    private String getBaseUrl() {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        return "http://" + instance.getHost() + ":" + instance.getPort();
    }

    public void saveHit(EndpointHitDto hitDto) {
        try {
            String baseUrl = getBaseUrl();
            String url = baseUrl + "/hit";
            log.info("Отправка хита на {}: uri={}, ip={}", url, hitDto.getUri(), hitDto.getIp());

            HttpEntity<EndpointHitDto> request = new HttpEntity<>(hitDto);
            restTemplate.postForEntity(url, request, Void.class);

            log.info("Хит успешно сохранен");
        } catch (Exception e) {
            log.error("Не удалось отправить хит в StatsService: {}", e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        try {
            String baseUrl = getBaseUrl();

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/stats")
                    .queryParam("start", start.format(FORMATTER))
                    .queryParam("end", end.format(FORMATTER))
                    .queryParam("unique", unique);

            if (uris != null && !uris.isEmpty()) {
                builder.queryParam("uris", String.join(",", uris));
            }

            URI uri = builder.build().encode().toUri();
            log.info("Запрос статистики: {}", uri);

            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {}
            );

            List<ViewStatsDto> stats = response.getBody() != null ? response.getBody() : Collections.emptyList();
            log.info("Получена статистика: {} записей", stats.size());

            return stats;
        } catch (Exception e) {
            log.error("Не удалось получить статистику из StatsService: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}