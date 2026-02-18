package ru.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.RequestFeignClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.dto.request.UserEventsQuery;
import ru.practicum.event.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/events")
@Slf4j
@Validated
@AllArgsConstructor
public class PrivateEventController {
    private final EventService eventService;
    private final RequestFeignClient requestFeignClient;

    @GetMapping
    public List<EventShortDto> findUserEvents(@PathVariable long userId,
                                              @PositiveOrZero @RequestParam(name = "from", defaultValue = "0") Integer from,
                                              @Positive @RequestParam(name = "size", defaultValue = "10") Integer size) {
        UserEventsQuery query = new UserEventsQuery(userId, from, size);
        return eventService.findEvents(query);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable long userId,
                                    @Valid @RequestBody NewEventDto eventDto) {
        return eventService.createEvent(userId, eventDto);
    }

    @GetMapping("/{eventId}")
    public EventFullDto findUserEventById(@PathVariable long userId,
                                          @PathVariable long eventId) {
        return eventService.findUserEventById(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateUserEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody @Valid UpdateEventUserRequest updateRequest) {
        return eventService.updateUserEvent(userId, eventId, updateRequest);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId,
                                                          @PathVariable Long eventId) {
        eventService.ensureUserIsInitiator(userId, eventId);

        return requestFeignClient.getEventRequests(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public Object updateStatuses(@PathVariable Long userId,
                                 @PathVariable Long eventId,
                                 @RequestBody Object request) {
        eventService.ensureUserIsInitiator(userId, eventId);

        return requestFeignClient.changeRequestStatus(userId, eventId, request);
    }
}