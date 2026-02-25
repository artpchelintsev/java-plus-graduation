package ru.practicum.event.controller;

import feign.FeignException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.RequestFeignClient;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.dto.request.UserEventsQuery;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.ConflictException;

import java.util.Collections;
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
        try {
            eventService.ensureUserIsInitiator(userId, eventId);
            return requestFeignClient.getEventRequests(userId, eventId);
        } catch (FeignException.NotFound e) {
            log.error("Request service not found: {}", e.getMessage());
            return Collections.emptyList();
        } catch (FeignException e) {
            log.error("Feign error when getting event requests: {}", e.getMessage());
            if (e.status() == 409) {
                throw new ConflictException("Conflict when getting event requests");
            }
            return Collections.emptyList();
        }
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateStatuses(@PathVariable Long userId,
                                                         @PathVariable Long eventId,
                                                         @RequestBody EventRequestStatusUpdateRequest request) {
        try {
            eventService.ensureUserIsInitiator(userId, eventId);
            return requestFeignClient.changeRequestStatus(userId, eventId, request);
        } catch (FeignException.NotFound e) {
            log.error("Request service not found: {}", e.getMessage());
            throw new ConflictException("Request service unavailable");
        } catch (FeignException e) {
            log.error("Feign error when updating request status: {}", e.getMessage());
            if (e.status() == 409) {
                throw new ConflictException("Conflict when updating request status");
            }
            throw new ConflictException("Error updating request status: " + e.getMessage());
        }
    }
}