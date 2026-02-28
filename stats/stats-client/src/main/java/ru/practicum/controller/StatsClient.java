package ru.practicum.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class StatsClient {

    public void saveHit(EndpointHitDto hitDto) {
        log.debug("StatsClient.saveHit is a no-op in recommendations branch");
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        return Collections.emptyList();
    }
}
