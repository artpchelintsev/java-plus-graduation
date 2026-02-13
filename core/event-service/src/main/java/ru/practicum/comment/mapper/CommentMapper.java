package ru.practicum.comment.mapper;

import org.mapstruct.*;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.model.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorName", source = "author.name")
    @Mapping(target = "eventId", source = "event.id")
    CommentDto toDto(Comment comment);
}