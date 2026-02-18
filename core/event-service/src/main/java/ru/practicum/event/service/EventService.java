package ru.practicum.event.service;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.client.RequestFeignClient;
import ru.practicum.client.StatsClient;
import ru.practicum.client.UserFeignClient;
import ru.practicum.common.EntityValidator;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.dto.event.EventBatchDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.request.RequestStatsDto;
import ru.practicum.dto.user.UserBatchDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.dto.enums.AdminStateAction;
import ru.practicum.event.dto.enums.EventSort;
import ru.practicum.event.dto.enums.UserStateAction;
import ru.practicum.event.dto.request.AdminEventFilter;
import ru.practicum.event.dto.request.PublicEventFilter;
import ru.practicum.event.dto.request.UserEventsQuery;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.exception.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;
    private final HttpServletRequest request;
    private final EntityValidator entityValidator;
    private final UserFeignClient userFeignClient;
    private final RequestFeignClient requestFeignClient;

    public List<EventShortDto> findEvents(UserEventsQuery query) {
        List<EventShortDto> dtos = eventMapper.toEventsShortDto(eventRepository.findByInitiatorId(query.userId(),
                PageRequest.of(query.from() / query.size(), query.size())));

        if (dtos != null && !dtos.isEmpty()) {
            enrichWithInitiators(dtos);
            enrichWithCategories(dtos);
            enrichWithConfirmedRequests(dtos);

            List<String> uris = dtos.stream()
                    .map(d -> "/events/" + d.getId())
                    .collect(Collectors.toList());
            saveHit();
            Map<String, Long> hits = fetchHitsForUris(uris);
            for (EventShortDto dto : dtos) {
                dto.setViews(hits.getOrDefault("/events/" + dto.getId(), 0L));
            }
        }

        return dtos != null ? dtos : Collections.emptyList();
    }

    private void enrichWithCategories(List<EventShortDto> events) {
        if (events == null || events.isEmpty()) return;

        try {
            List<Long> categoryIds = events.stream()
                    .map(e -> e.getCategory() != null ? e.getCategory().getId() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (categoryIds.isEmpty()) return;

            for (EventShortDto event : events) {
                if (event.getCategory() != null && event.getCategory().getId() != null) {
                    event.getCategory().setName("Category " + event.getCategory().getId());
                }
            }

            log.debug("Enriched {} events with categories", events.size());
        } catch (Exception e) {
            log.error("Failed to enrich with categories", e);
        }
    }

    private void enrichWithCategory(EventFullDto event) {
        if (event == null || event.getCategory() == null || event.getCategory().getId() == null) return;

        try {
            event.getCategory().setName("Category " + event.getCategory().getId());
        } catch (Exception e) {
            log.error("Failed to enrich category for event {}: {}", event.getId(), e.getMessage());
        }
    }

    private void enrichWithCategoriesForFullDto(List<EventFullDto> events) {
        if (events == null || events.isEmpty()) return;

        try {
            List<Long> categoryIds = events.stream()
                    .map(e -> e.getCategory() != null ? e.getCategory().getId() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (categoryIds.isEmpty()) return;

            for (EventFullDto event : events) {
                if (event.getCategory() != null && event.getCategory().getId() != null) {
                    event.getCategory().setName("Category " + event.getCategory().getId());
                }
            }

            log.debug("Enriched {} events with categories", events.size());
        } catch (Exception e) {
            log.error("Failed to enrich with categories", e);
        }
    }

    @Transactional
    public EventFullDto createEvent(long userId, NewEventDto eventDto) {
        try {
            userFeignClient.getUserById(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("User not found: " + userId);
        }

        if (eventDto.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (eventDto.getEventDate().isBefore(now.plusHours(2))) {
                throw new InvalidRequestException("Event date must be at least 2 hours in the future");
            }
        }

        Event event = eventMapper.fromNewEventDto(eventDto);
        event.setInitiatorId(userId);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        Event savedItem = eventRepository.save(event);

        EventFullDto dto = eventMapper.toEventFullDto(savedItem);
        enrichWithInitiator(dto, userId);
        enrichWithCategory(dto);

        return dto;
    }

    public EventFullDto findUserEventById(long userId, long eventId) {
        Event event = findByIdAndUser(eventId, userId);
        EventFullDto dto = eventMapper.toEventFullDto(event);

        if (dto != null) {
            enrichWithInitiator(dto, event.getInitiatorId());
            enrichWithCategory(dto);
            setConfirmedRequestsForEvents(List.of(dto));

            String uri = "/events/" + dto.getId();
            Map<String, Long> hits = fetchHitsForUris(List.of(uri));
            dto.setViews(hits.getOrDefault(uri, 0L));
        }
        return dto;
    }

    public EventBatchDto getEventsByIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return EventBatchDto.builder()
                    .events(new HashMap<>())
                    .build();
        }

        List<Event> events = eventRepository.findAllById(eventIds);
        List<EventShortDto> eventDtos = eventMapper.toEventsShortDto(events);

        if (eventDtos != null && !eventDtos.isEmpty()) {
            enrichWithInitiators(eventDtos);
            enrichWithCategories(eventDtos);
            enrichWithConfirmedRequests(eventDtos);

            List<String> uris = eventDtos.stream()
                    .map(d -> "/events/" + d.getId())
                    .collect(Collectors.toList());
            Map<String, Long> hits = fetchHitsForUris(uris);
            for (EventShortDto dto : eventDtos) {
                dto.setViews(hits.getOrDefault("/events/" + dto.getId(), 0L));
            }
        }

        Map<Long, EventShortDto> eventMap = eventDtos != null ?
                eventDtos.stream()
                        .collect(Collectors.toMap(EventShortDto::getId, dto -> dto)) :
                new HashMap<>();

        return EventBatchDto.builder()
                .events(eventMap)
                .build();
    }

    private Event findByPublicId(long eventId) {
        return eventRepository.findByIdAndState(eventId, EventState.PUBLISHED).orElseThrow(() ->
                new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private Event findByIdAndUser(long eventId, long userId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(() ->
                new NotFoundException("Владелец с ID " + userId + " или ивент с ID " + eventId + " не найдены"));
    }

    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = findByIdAndUser(eventId, userId);
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (updateRequest.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (updateRequest.getEventDate().isBefore(now.plusHours(2))) {
                throw new InvalidRequestException("Event date must be at least 2 hours in the future");
            }
        }

        eventMapper.updateEventFromUserDto(updateRequest, event);

        if (updateRequest.getStateAction() != null) {
            if (event.getState().equals(EventState.CANCELED) &&
                    updateRequest.getStateAction().equals(UserStateAction.SEND_TO_REVIEW)) {
                event.setState(EventState.PENDING);
            } else if (event.getState().equals(EventState.PENDING) &&
                    updateRequest.getStateAction().equals(UserStateAction.CANCEL_REVIEW)) {
                event.setState(EventState.CANCELED);
            }
        }

        Event savedEvent = eventRepository.save(event);
        EventFullDto dto = eventMapper.toEventFullDto(savedEvent);
        enrichWithInitiator(dto, userId);
        enrichWithCategory(dto);

        return dto;
    }

    public List<EventShortDto> searchPublicEvents(PublicEventFilter filter) {
        if (filter.getRangeStart() != null && filter.getRangeEnd() != null) {
            if (filter.getRangeStart().isAfter(filter.getRangeEnd())) {
                throw new InvalidDateRangeException("Дата начала не может быть позже даты окончания.");
            }
        }

        List<EventShortDto> dtos = eventMapper.toEventsShortDto(eventRepository.searchEventsByPublic(filter));

        if (dtos != null && !dtos.isEmpty()) {
            enrichWithInitiators(dtos);
            enrichWithCategories(dtos);
            enrichWithConfirmedRequests(dtos);

            List<String> uris = dtos.stream()
                    .map(d -> "/events/" + d.getId())
                    .collect(Collectors.toList());

            Map<String, Long> hits = fetchHitsForUris(uris);
            for (EventShortDto dto : dtos) {
                dto.setViews(hits.getOrDefault("/events/" + dto.getId(), 0L));
            }
        } else {
            dtos = Collections.emptyList();
        }

        saveHit();

        if (filter.getSort() != null && filter.getSort() == EventSort.VIEWS && !dtos.isEmpty()) {
            dtos.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }

        return dtos;
    }

    public EventFullDto findPublicEventById(long id) {
        Event event = findByPublicId(id);
        saveHit();
        EventFullDto dto = eventMapper.toEventFullDto(event);

        if (dto != null) {
            enrichWithInitiator(dto, event.getInitiatorId());
            enrichWithCategory(dto);
            setConfirmedRequestsForEvents(List.of(dto));

            String uri = "/events/" + dto.getId();
            Map<String, Long> hits = fetchHitsForUris(List.of(uri));
            dto.setViews(hits.getOrDefault(uri, 0L));

        }

        return dto;
    }

    public List<EventFullDto> searchEventsByAdmin(AdminEventFilter filter) {
        List<EventFullDto> dtos = eventMapper.toEventsFullDto(eventRepository.searchEventsByAdmin(filter));

        if (dtos != null && !dtos.isEmpty()) {
            enrichWithInitiatorsForFullDto(dtos);
            enrichWithCategoriesForFullDto(dtos);
            setConfirmedRequestsForEvents(dtos);

            List<String> uris = dtos.stream()
                    .map(d -> "/events/" + d.getId())
                    .collect(Collectors.toList());
            Map<String, Long> hits = fetchHitsForUris(uris);
            for (EventFullDto dto : dtos) {
                dto.setViews(hits.getOrDefault("/events/" + dto.getId(), 0L));
            }
        } else {
            dtos = Collections.emptyList();
        }

        return dtos;
    }

    private void setConfirmedRequestsForEvents(List<EventFullDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        try {
            List<Long> eventIds = dtos.stream()
                    .map(EventFullDto::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (eventIds.isEmpty()) return;

            RequestStatsDto stats = requestFeignClient.getRequestStats(eventIds);
            Map<Long, Integer> confirmedRequestsMap = stats.getConfirmedRequests();

            dtos.forEach(dto -> dto.setConfirmedRequests(
                    confirmedRequestsMap.getOrDefault(dto.getId(), 0).longValue()));

            log.debug("Enriched {} events with confirmed requests", dtos.size());
        } catch (FeignException e) {
            log.error("Feign error when getting confirmed requests: {}", e.getMessage());
            dtos.forEach(dto -> dto.setConfirmedRequests(0L));
        } catch (Exception e) {
            log.error("Failed to enrich with confirmed requests", e);
            dtos.forEach(dto -> dto.setConfirmedRequests(0L));
        }
    }

    @Transactional
    public EventFullDto moderateEvent(Long eventId, UpdateEventAdminRequest adminRequest) {
        Event event = entityValidator.ensureAndGet(eventRepository, eventId, "Event");

        if (adminRequest.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (adminRequest.getEventDate().isBefore(now.plusHours(1))) {
                throw new InvalidRequestException("Event date must be at least one hour in the future to publish.");
            }
        }

        eventMapper.updateEventFromAdminDto(adminRequest, event);

        if (adminRequest.getStateAction() != null) {
            if (event.getState().equals(EventState.PENDING)) {
                if (adminRequest.getStateAction().equals(AdminStateAction.PUBLISH_EVENT)) {
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                }
                if (adminRequest.getStateAction().equals(AdminStateAction.REJECT_EVENT)) {
                    event.setState(EventState.CANCELED);
                }
            } else {
                throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
            }
        }

        Event savedEvent = eventRepository.save(event);
        EventFullDto dto = eventMapper.toEventFullDto(savedEvent);
        enrichWithInitiator(dto, event.getInitiatorId());
        enrichWithCategory(dto);

        return dto;
    }

    public void ensureUserIsInitiator(Long userId, Long eventId) {
        Event event = entityValidator.ensureAndGet(eventRepository, eventId, "Событие");
        if (!event.getInitiatorId().equals(userId)) {
            throw new ValidationException("Только инициатор может просматривать заявки");
        }
    }

    private void enrichWithInitiators(List<EventShortDto> events) {
        if (events == null || events.isEmpty()) return;

        try {
            List<Long> initiatorIds = events.stream()
                    .map(e -> e.getInitiator() != null ? e.getInitiator().getId() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (initiatorIds.isEmpty()) return;

            UserBatchDto usersData = userFeignClient.getUsersByIds(initiatorIds);
            Map<Long, UserShortDto> users = usersData.getUsers();

            for (EventShortDto event : events) {
                if (event.getInitiator() != null) {
                    UserShortDto user = users.get(event.getInitiator().getId());
                    if (user != null) {
                        event.setInitiator(user);
                    } else {
                        event.setInitiator(UserShortDto.builder()
                                .id(event.getInitiator().getId())
                                .name("Unknown User")
                                .build());
                    }
                }
            }

            log.debug("Enriched {} events with initiators", events.size());

        } catch (FeignException e) {
            log.error("Feign error when getting users: {}", e.getMessage());
            for (EventShortDto event : events) {
                if (event.getInitiator() != null) {
                    event.setInitiator(UserShortDto.builder()
                            .id(event.getInitiator().getId())
                            .name("Unknown User")
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to enrich with initiators", e);
        }
    }

    private void enrichWithInitiatorsForFullDto(List<EventFullDto> events) {
        if (events == null || events.isEmpty()) return;

        try {
            List<Long> initiatorIds = events.stream()
                    .map(e -> e.getInitiator() != null ? e.getInitiator().getId() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (initiatorIds.isEmpty()) return;

            UserBatchDto usersData = userFeignClient.getUsersByIds(initiatorIds);
            Map<Long, UserShortDto> users = usersData.getUsers();

            for (EventFullDto event : events) {
                if (event.getInitiator() != null) {
                    UserShortDto user = users.get(event.getInitiator().getId());
                    if (user != null) {
                        event.setInitiator(user);
                    } else {
                        event.setInitiator(UserShortDto.builder()
                                .id(event.getInitiator().getId())
                                .name("Unknown User")
                                .build());
                    }
                }
            }

            log.debug("Enriched {} events with initiators", events.size());

        } catch (FeignException e) {
            log.error("Feign error when getting users: {}", e.getMessage());
            for (EventFullDto event : events) {
                if (event.getInitiator() != null) {
                    event.setInitiator(UserShortDto.builder()
                            .id(event.getInitiator().getId())
                            .name("Unknown User")
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to enrich with initiators", e);
        }
    }

    private void enrichWithInitiator(EventFullDto event, Long initiatorId) {
        if (event == null || initiatorId == null) return;

        try {
            UserDto user = userFeignClient.getUserById(initiatorId);
            event.setInitiator(UserShortDto.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .build());
        } catch (FeignException.NotFound e) {
            log.error("User not found: {}", initiatorId);
            event.setInitiator(UserShortDto.builder()
                    .id(initiatorId)
                    .name("Unknown User")
                    .build());
        } catch (Exception e) {
            log.error("Failed to enrich with initiator for event {}: {}",
                    event.getId(), e.getMessage());
            event.setInitiator(UserShortDto.builder()
                    .id(initiatorId)
                    .name("Unknown")
                    .build());
        }
    }

    private void enrichWithConfirmedRequests(List<EventShortDto> events) {
        if (events == null || events.isEmpty()) return;

        try {
            List<Long> eventIds = events.stream()
                    .map(EventShortDto::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (eventIds.isEmpty()) return;

            RequestStatsDto stats = requestFeignClient.getRequestStats(eventIds);
            Map<Long, Integer> confirmedRequests = stats.getConfirmedRequests();

            for (EventShortDto event : events) {
                Integer count = confirmedRequests.getOrDefault(event.getId(), 0);
                event.setConfirmedRequests(count);
            }

            log.debug("Enriched {} events with confirmed requests", events.size());

        } catch (FeignException e) {
            log.error("Feign error when getting confirmed requests: {}", e.getMessage());
            events.forEach(event -> event.setConfirmedRequests(0));
        } catch (Exception ex) {
            log.error("Failed to enrich with confirmed requests", ex);
            events.forEach(event -> event.setConfirmedRequests(0));
        }
    }

    private void saveHit() {
        try {
            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();

            statsClient.saveHit(hitDto);
        } catch (Exception e) {
            log.error("Не удалось отправить информацию о просмотре в сервис статистики: {}", e.getMessage());
        }
    }

    private Map<String, Long> fetchHitsForUris(List<String> uris) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(10);
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);
            if (stats == null || stats.isEmpty()) return Map.of();
            return stats.stream().collect(Collectors.toMap(
                    ViewStatsDto::getUri, v -> v.getHits() == null ? 0L : v.getHits()));
        } catch (Exception e) {
            log.error("Не удалось получить просмотры из сервиса статистики: {}", e.getMessage());
            return Map.of();
        }
    }
}