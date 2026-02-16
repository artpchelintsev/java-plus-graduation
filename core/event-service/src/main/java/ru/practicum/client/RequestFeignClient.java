package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatsDto;

import java.util.List;

@FeignClient(
        name = "request-service",
        fallback = RequestFeignClientFallback.class
)
public interface RequestFeignClient {

    @PostMapping("/requests/stats")
    RequestStatsDto getRequestStats(@RequestBody List<Long> eventIds);

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    List<ParticipationRequestDto> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId);

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    Object changeRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody Object updateRequest);
}