package ru.practicum.event.mapper;

import org.mapstruct.*;
import ru.practicum.dto.event.CategoryDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(source = "eventDate", target = "eventDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "mapCategoryIdToDto")
    @Mapping(target = "initiator", source = "initiatorId", qualifiedByName = "mapInitiatorIdToDto")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "rating", ignore = true)
    EventFullDto toEventFullDto(Event event);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "mapCategoryIdToDto")
    @Mapping(target = "initiator", source = "initiatorId", qualifiedByName = "mapInitiatorIdToDto")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "eventDate", source = "eventDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
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

    @Named("mapCategoryIdToDto")
    default CategoryDto mapCategoryIdToDto(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return CategoryDto.builder()
                .id(categoryId)
                .name(null)
                .build();
    }

    @Named("mapInitiatorIdToDto")
    default UserShortDto mapInitiatorIdToDto(Long initiatorId) {
        if (initiatorId == null) {
            return null;
        }
        return UserShortDto.builder()
                .id(initiatorId)
                .name(null)
                .build();
    }
}