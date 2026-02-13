package ru.practicum.model;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompilationEventId implements Serializable {
    private Long compilationId;
    private Long eventId;
}
