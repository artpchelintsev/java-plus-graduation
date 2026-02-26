package ru.practicum.event.service;

import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.client.RequestFeignClient;
import ru.practicum.client.UserFeignClient;
import ru.practicum.common.EntityValidator;
import ru.practicum.controller.RecommendationsClient;
import ru.practicum.controller.UserActionClient;
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
import ru.practicum.stats.proto.ActionTypeProto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserActionClient userActionClient;
    private final RecommendationsClient recommendationsClient;
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

            List<Long> ids = dtos.stream().map(EventShortDto::getId).collect(Collectors.toList());
            Map<Long, Double> ratings = fetchRatings(ids);
            dtos.forEach(dto -> dto.setRating(ratings.getOrDefault(dto.getId(), 0.0)));
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

            Map<Long, Double> ratings = fetchRatings(List.of(dto.getId()));
            dto.setRating(ratings.getOrDefault(dto.getId(), 0.0));
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

            List<Long> ids = eventDtos.stream().map(EventShortDto::getId).collect(Collectors.toList());
            Map<Long, Double> ratings = fetchRatings(ids);
            eventDtos.forEach(dto -> dto.setRating(ratings.getOrDefault(dto.getId(), 0.0)));
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
        } else {
            dtos = Collections.emptyList();
        }

        List<Long> eventIds = dtos.stream().map(EventShortDto::getId).collect(Collectors.toList());
        Map<Long, Double> ratings = fetchRatings(eventIds);
        for (EventShortDto dto : dtos) {
            dto.setRating(ratings.getOrDefault(dto.getId(), 0.0));
        }

        if (filter.getSort() != null && filter.getSort() == EventSort.VIEWS && !dtos.isEmpty()) {
            dtos.sort(Comparator.comparing(EventShortDto::getRating).reversed());
        }

        return dtos;
    }

    public EventFullDto findPublicEventById(long id, long userId) {
        Event event = findByPublicId(id);
        EventFullDto dto = eventMapper.toEventFullDto(event);

        if (dto != null) {
            enrichWithInitiator(dto, event.getInitiatorId());
            enrichWithCategory(dto);
            setConfirmedRequestsForEvents(List.of(dto));

            try {
                userActionClient.sendUserAction(userId, id, ActionTypeProto.ACTION_VIEW, Instant.now());
            } catch (Exception e) {
                log.warn("Failed to send VIEW action: {}", e.getMessage());
            }

            Map<Long, Double> ratings = fetchRatings(List.of(dto.getId()));
            dto.setRating(ratings.getOrDefault(dto.getId(), 0.0));
        }

        return dto;
    }

    public EventFullDto getEventByIdInternal(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return eventMapper.toEventFullDto(event);
    }

    public List<EventFullDto> searchEventsByAdmin(AdminEventFilter filter) {
        List<EventFullDto> dtos = eventMapper.toEventsFullDto(eventRepository.searchEventsByAdmin(filter));

        if (dtos != null && !dtos.isEmpty()) {
            enrichWithInitiatorsForFullDto(dtos);
            enrichWithCategoriesForFullDto(dtos);
            setConfirmedRequestsForEvents(dtos);

            List<Long> ids = dtos.stream().map(EventFullDto::getId).collect(Collectors.toList());
            Map<Long, Double> ratings = fetchRatings(ids);
            dtos.forEach(dto -> dto.setRating(ratings.getOrDefault(dto.getId(), 0.0)));
        } else {
            dtos = Collections.emptyList();
        }

        return dtos;
    }

    public List<EventShortDto> getRecommendations(long userId, int maxResults) {
        List<Long> eventIds = recommendationsClient
                .getRecommendationsForUser(userId, maxResults)
                .map(r -> r.getEventId())
                .collect(Collectors.toList());

        if (eventIds.isEmpty()) return Collections.emptyList();

        List<Event> events = eventRepository.findAllById(eventIds);
        List<EventShortDto> dtos = eventMapper.toEventsShortDto(events);

        if (dtos != null && !dtos.isEmpty()) {
            enrichWithInitiators(dtos);
            enrichWithCategories(dtos);
            enrichWithConfirmedRequests(dtos);

            Map<Long, Double> ratings = fetchRatings(eventIds);
            dtos.forEach(dto -> dto.setRating(ratings.getOrDefault(dto.getId(), 0.0)));
        }

        return dtos != null ? dtos : Collections.emptyList();
    }

    public void likeEvent(long userId, long eventId) {
        boolean visited = requestFeignClient.hasConfirmedRequest(userId, eventId);
        if (!visited) {
            throw new InvalidRequestException("Пользователь не посещал мероприятие " + eventId);
        }
        try {
            userActionClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE, Instant.now());
        } catch (Exception e) {
            log.warn("Failed to send LIKE action: {}", e.getMessage());
        }
    }

    private Map<Long, Double> fetchRatings(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return Map.of();
        try {
            return recommendationsClient.getInteractionsCount(eventIds)
                    .collect(Collectors.toMap(
                            r -> r.getEventId(),
                            r -> r.getScore(),
                            (a, b) -> a
                    ));
        } catch (Exception e) {
            log.error("Failed to fetch ratings: {}", e.getMessage());
            return Map.of();
        }
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
}