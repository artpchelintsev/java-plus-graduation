package ru.practicum.event.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.event.dto.request.PublicEventFilter;
import ru.practicum.event.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/events")
@Slf4j
@Validated
@AllArgsConstructor
public class PublicEventController {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> searchPublicEvents(
            @Valid PublicEventFilter filter) {
        return eventService.searchPublicEvents(filter);
    }

    @GetMapping("/{id}")
    public EventFullDto findPublicEventById(
            @PathVariable long id,
            @RequestHeader("X-EWM-USER-ID") long userId) {
        return eventService.findPublicEventById(id, userId);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") long userId) {
        return eventService.getRecommendations(userId, 10);
    }

    @PutMapping("/{eventId}/like")
    public ResponseEntity<Void> likeEvent(
            @PathVariable long eventId,
            @RequestHeader("X-EWM-USER-ID") long userId) {
        eventService.likeEvent(userId, eventId);
        return ResponseEntity.ok().build();
    }
}
