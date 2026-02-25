package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventBatchDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.event.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;

    @PostMapping("/batch")
    public EventBatchDto getEventsByIds(@RequestBody List<Long> eventIds) {
        return eventService.getEventsByIds(eventIds);
    }

    @GetMapping("/{eventId}/internal")
    public EventFullDto getEventByIdInternal(@PathVariable Long eventId) {
        return eventService.getEventByIdInternal(eventId);
    }
}
