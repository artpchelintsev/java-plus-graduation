package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "compilation_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(CompilationEventId.class)
public class CompilationEvent {

    @Id
    @Column(name = "compilation_id")
    private Long compilationId;

    @Id
    @Column(name = "event_id")
    private Long eventId;
}
