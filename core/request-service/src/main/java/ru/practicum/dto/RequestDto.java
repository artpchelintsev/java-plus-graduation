package ru.practicum.dto;

import lombok.*;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestDto {
    Long id;
    Long event;
    Long requester;
    LocalDateTime created;
    String status;
}
