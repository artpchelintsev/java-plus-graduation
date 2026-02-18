package ru.practicum.event.mapper;

import org.mapstruct.*;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface EventMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(source = "event.eventDate", target = "eventDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "category", source = "categoryId")
    @Mapping(target = "initiator", source = "initiatorId")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    EventFullDto toEventFullDto(Event event);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "category", source = "categoryId")
    @Mapping(target = "initiator", source = "initiatorId")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    EventShortDto toEventShortDto(Event event);

    @Mapping(target = "categoryId", source = "category")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    Event fromNewEventDto(NewEventDto dto);

    List<EventShortDto> toEventsShortDto(List<Event> events);

    @Mapping(target = "categoryId", source = "category")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEventFromUserDto(UpdateEventUserRequest dto, @MappingTarget Event entity);

    @Mapping(target = "categoryId", source = "category")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEventFromAdminDto(UpdateEventAdminRequest dto, @MappingTarget Event entity);

    List<EventFullDto> toEventsFullDto(List<Event> events);
}