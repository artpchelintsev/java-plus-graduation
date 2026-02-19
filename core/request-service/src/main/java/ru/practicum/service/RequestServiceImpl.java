package ru.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventFeignClient;
import ru.practicum.client.UserFeignClient;
import ru.practicum.common.EntityValidator;
import ru.practicum.dto.ConfirmedRequestCount;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.RequestDto;
import ru.practicum.dto.request.RequestStatsDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.model.RequestStatus;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserFeignClient userFeignClient;
    private final EventFeignClient eventFeignClient;
    private final RequestMapper mapper;
    private final EntityValidator entityValidator;

    @Override
    public List<RequestDto> getUserRequests(Long userId) {
        try {
            userFeignClient.getUserById(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("User not found: " + userId);
        }

        List<Request> requests = requestRepository.findByRequesterId(userId);
        return mapper.toDtoList(requests);
    }

    @Override
    @Transactional
    public RequestDto createRequest(Long userId, Long eventId) {
        try {
            userFeignClient.getUserById(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("User not found: " + userId);
        }

        EventFullDto event;
        try {
            event = eventFeignClient.getEventById(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Event not found: " + eventId);
        }

        if (Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Инициатор события не может создать заявку на участие в своём же событии");
        }

        if (event.getState() == null || !"PUBLISHED".equals(event.getState())) {
            throw new ConflictException("Нельзя добавить заявку: событие не опубликовано");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Заявка от этого пользователя на это событие уже существует");
        }

        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Integer limit = event.getParticipantLimit();
        if (limit != null && limit > 0 && confirmed >= limit) {
            throw new ConflictException("Достигнут лимит участников события");
        }

        RequestStatus initialStatus = RequestStatus.PENDING;
        if (!event.getRequestModeration() || limit == null || limit == 0) {
            initialStatus = RequestStatus.CONFIRMED;
        }

        Request request = Request.builder()
                .eventId(eventId)
                .requesterId(userId)
                .status(initialStatus)
                .created(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .build();

        Request saved = requestRepository.save(request);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public RequestDto cancelRequest(Long userId, Long requestId) {
        try {
            userFeignClient.getUserById(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("User not found: " + userId);
        }

        Request request = entityValidator.ensureAndGet(requestRepository, requestId, "Заявка");

        if (!Objects.equals(request.getRequesterId(), userId)) {
            throw new ConflictException("Пользователь может отменять только свои заявки");
        }

        if (request.getStatus() == RequestStatus.CONFIRMED) {
            throw new ConflictException("Нельзя отменить уже подтвержденную заявку");
        }

        request.setStatus(RequestStatus.CANCELED);
        Request saved = requestRepository.save(request);
        return mapper.toDto(saved);
    }

    @Override
    public List<RequestDto> getEventRequests(Long userId, Long eventId) {
        try {
            userFeignClient.getUserById(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("User not found: " + userId);
        }

        EventFullDto event;
        try {
            event = eventFeignClient.getEventById(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Event not found: " + eventId);
        }

        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new NotFoundException("Только инициатор может просматривать заявки данного события");
        }

        List<Request> requests = requestRepository.findByEventId(eventId);
        return mapper.toDtoList(requests);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        try {
            userFeignClient.getUserById(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("User not found: " + userId);
        }

        EventFullDto event;
        try {
            event = eventFeignClient.getEventById(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Event not found: " + eventId);
        }

        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Только инициатор может менять статусы заявок");
        }

        String statusStr = Optional.ofNullable(updateRequest.getStatus())
                .orElse("")
                .toUpperCase(Locale.ROOT);
        RequestStatus targetStatus;
        try {
            targetStatus = RequestStatus.valueOf(statusStr);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Недопустимый статус: " + updateRequest.getStatus());
        }

        if (!(targetStatus == RequestStatus.CONFIRMED || targetStatus == RequestStatus.REJECTED)) {
            throw new ValidationException("Можно массово устанавливать только CONFIRMED или REJECTED");
        }

        List<Long> ids = Optional.ofNullable(updateRequest.getRequestIds())
                .orElse(Collections.emptyList());

        if (ids.isEmpty()) {
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(Collections.emptyList())
                    .rejectedRequests(Collections.emptyList())
                    .build();
        }

        List<Request> requests = requestRepository.findByEventIdAndIdIn(eventId, ids);

        for (Request req : requests) {
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Можно обрабатывать только заявки в статусе PENDING");
            }
        }

        long confirmedNow = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Integer limit = event.getParticipantLimit();

        List<Request> confirmed = new ArrayList<>();
        List<Request> rejected = new ArrayList<>();

        if (targetStatus == RequestStatus.CONFIRMED) {
            if (limit != null && limit > 0) {
                long available = limit - confirmedNow;
                if (available <= 0) {
                    throw new ConflictException("Достигнут лимит участников события");
                }
                if (available < requests.size()) {
                    for (int i = 0; i < requests.size(); i++) {
                        Request req = requests.get(i);
                        if (i < available) {
                            req.setStatus(RequestStatus.CONFIRMED);
                            confirmed.add(req);
                        } else {
                            req.setStatus(RequestStatus.REJECTED);
                            rejected.add(req);
                        }
                    }
                } else {
                    for (Request req : requests) {
                        req.setStatus(RequestStatus.CONFIRMED);
                        confirmed.add(req);
                    }
                }
            } else {
                for (Request req : requests) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(req);
                }
            }
        } else if (targetStatus == RequestStatus.REJECTED) {
            for (Request req : requests) {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(req);
            }
        }

        requestRepository.saveAll(confirmed);
        requestRepository.saveAll(rejected);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(mapper.toDtoList(confirmed))
                .rejectedRequests(mapper.toDtoList(rejected))
                .build();
    }

    @Override
    public RequestStatsDto getRequestStatsByEvents(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return RequestStatsDto.builder()
                    .confirmedRequests(new HashMap<>())
                    .build();
        }

        List<ConfirmedRequestCount> counts = requestRepository
                .countConfirmedRequestsForEvents(eventIds, RequestStatus.CONFIRMED);

        Map<Long, Integer> statsMap = counts.stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestCount::getEventId,
                        c -> c.getCount().intValue()
                ));

        return RequestStatsDto.builder()
                .confirmedRequests(statsMap)
                .build();
    }
}