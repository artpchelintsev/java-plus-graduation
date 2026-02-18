package ru.practicum.controller;

import feign.FeignException;
import ru.practicum.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.RequestDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.service.RequestServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Slf4j
public class RequestController {

    private final RequestServiceImpl service;
    private final RequestMapper mapper;

    @GetMapping("/requests")
    public List<RequestDto> getUserRequests(@PathVariable Long userId) {
        return service.getUserRequests(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto createRequest(@PathVariable Long userId,
                                    @RequestParam Long eventId) {
        return service.createRequest(userId, eventId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ResponseEntity<RequestDto> cancelRequest(@PathVariable Long userId, @PathVariable Long requestId) {
        return ResponseEntity.ok(service.cancelRequest(userId, requestId));
    }
    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        List<RequestDto> requests = service.getEventRequests(userId, eventId);
        return mapper.toParticipationDtoList(requests);
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult changeRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest updateRequest) {

        log.info("Changing request status for event {} by user {}", eventId, userId);

        try {
            return service.changeRequestStatus(userId, eventId, updateRequest);
        } catch (ConflictException e) {
            log.error("Conflict when changing request status: {}", e.getMessage());
            throw e;
        } catch (ValidationException e) {
            log.error("Validation error when changing request status: {}", e.getMessage());
            throw e;
        } catch (FeignException.NotFound e) {
            log.error("Event not found: {}", eventId);
            throw new NotFoundException("Event not found: " + eventId);
        } catch (FeignException.Conflict e) {
            log.error("Conflict in event service: {}", e.getMessage());
            throw new ConflictException(e.getMessage());
        } catch (FeignException e) {
            log.error("Feign error when changing request status: {}", e.getMessage());
            if (e.status() == 409) {
                throw new ConflictException(e.getMessage());
            } else if (e.status() == 404) {
                throw new NotFoundException(e.getMessage());
            } else {
                throw new ConflictException("Error updating request status: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error when changing request status", e);
            throw new ConflictException("Internal server error");
        }
    }
}