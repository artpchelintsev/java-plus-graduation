package ru.practicum.comment.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentAdminFilter {

    private Long eventId;
    private Long authorId;
    private Boolean includeDeleted = false;
    @Builder.Default
    private Integer from = 0;
    @Builder.Default
    private Integer size = 10;
}