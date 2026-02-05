package ru.practicum.repository;

import ru.practicum.dto.ViewStatsDto;
import ru.practicum.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository {
    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);

    EndpointHit save(EndpointHit hit);
}

