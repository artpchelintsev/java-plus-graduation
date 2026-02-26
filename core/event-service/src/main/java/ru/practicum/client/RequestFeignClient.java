package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatsDto;

import java.util.List;

@FeignClient(
        name = "request-service",
        configuration = FeignErrorDecoder.class,
        fallback = RequestFeignClientFallback.class
)

public interface RequestFeignClient {

    @PostMapping("/requests/stats")
    RequestStatsDto getRequestStats(@RequestBody List<Long> eventIds);

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    List<ParticipationRequestDto> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId);

    @PostMapping("/users/{userId}/events/{eventId}/requests/status")
    EventRequestStatusUpdateResult changeRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest updateRequest);

    @GetMapping("/internal/requests/confirmed")
    boolean hasConfirmedRequest(@RequestParam("userId") long userId,
                                @RequestParam("eventId") long eventId);
}
