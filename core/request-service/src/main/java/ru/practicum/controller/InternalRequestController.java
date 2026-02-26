package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.RequestStatsDto;
import ru.practicum.service.RequestServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final RequestServiceImpl service;

    @PostMapping("/stats")
    public RequestStatsDto getRequestStats(@RequestBody List<Long> eventIds) {
        return service.getRequestStatsByEvents(eventIds);
    }

    @GetMapping("/confirmed")
    public boolean hasConfirmedRequest(
            @RequestParam long userId,
            @RequestParam long eventId) {
        return service.hasConfirmedRequest(userId, eventId);
    }
}
