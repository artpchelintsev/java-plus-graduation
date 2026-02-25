package ru.practicum.event.dao;

import ru.practicum.event.dto.request.AdminEventFilter;
import ru.practicum.event.dto.request.PublicEventFilter;
import ru.practicum.event.model.Event;

import java.util.List;

public interface EventRepositoryCustom {
    List<Event> searchEventsByAdmin(AdminEventFilter filter);

    List<Event> searchEventsByPublic(PublicEventFilter filter);
}
