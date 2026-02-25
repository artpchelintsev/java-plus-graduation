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
}
