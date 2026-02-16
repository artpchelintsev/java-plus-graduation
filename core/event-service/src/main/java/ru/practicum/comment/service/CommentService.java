package ru.practicum.comment.service;

import ru.practicum.comment.dto.CommentAdminFilter;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;

import java.util.List;

public interface CommentService {

    CommentDto addComment(Long userId, Long eventId, NewCommentDto dto);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto);

    void deleteComment(Long userId, Long commentId);

    CommentDto getCommentById(Long commentId);

    List<CommentDto> getCommentsByEvent(Long eventId, int from, int size);

    List<CommentDto> getAllComments(CommentAdminFilter filter);

    CommentDto adminUpdateComment(Long commentId, String text);

    void adminDeleteComment(Long commentId);

    void restoreComment(Long commentId);
}