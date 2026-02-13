package ru.practicum.event.mapper;

import org.mapstruct.*;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;
import ru.practicum.user.mapper.UserMapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class})
public interface EventMapper {

    @Mapping(source = "event.eventDate", target = "eventDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
    EventFullDto toEventFullDto(Event event);

    EventShortDto toEventShortDto(Event event);

    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategoryIdToCategory")
    Event fromNewEventDto(NewEventDto dto);

    List<EventShortDto> toEventsShortDto(List<Event> events);

    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategoryIdToCategory")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEventFromUserDto(UpdateEventUserRequest dto, @MappingTarget Event entity);

    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategoryIdToCategory")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEventFromAdminDto(UpdateEventAdminRequest dto, @MappingTarget Event entity);

    List<EventFullDto> toEventsFullDto(List<Event> events);

    @Named("mapCategoryIdToCategory")
    default Category mapCategoryIdToCategory(Long id) {
        if (id == null) return null;
        Category category = new Category();
        category.setId(id);
        return category;
    }
}
