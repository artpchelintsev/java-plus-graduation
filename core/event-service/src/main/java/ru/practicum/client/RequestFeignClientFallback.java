package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatsDto;

import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class RequestFeignClientFallback implements RequestFeignClient {

    @Override
    public RequestStatsDto getRequestStats(List<Long> eventIds) {
        log.warn("Request Service unavailable, returning zeros");
        return RequestStatsDto.builder()
                .confirmedRequests(new HashMap<>())
                .build();
    }
    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.error("Request Service unavailable for event: {}", eventId);
        throw new RuntimeException("Request Service is temporarily unavailable");
    }

    @Override
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        log.error("Request Service unavailable for status change");
        throw new RuntimeException("Request Service is temporarily unavailable");
    }
}
