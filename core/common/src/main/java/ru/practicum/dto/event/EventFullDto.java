package ru.practicum.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.dto.user.UserShortDto;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventFullDto {
    private Long id;
    private String annotation;
    private CategoryDto category;

    @Builder.Default
    private Long confirmedRequests = 0L;

    private LocalDateTime createdOn;
    private String description;
    private String eventDate;
    private UserShortDto initiator;
    private LocationDto location;
    private Boolean paid;

    @Builder.Default
    private Integer participantLimit = 0;

    private LocalDateTime publishedOn;

    @Builder.Default
    private Boolean requestModeration = true;

    private String state;
    private String title;

    @Builder.Default
    private Double rating = 0.0;
}

