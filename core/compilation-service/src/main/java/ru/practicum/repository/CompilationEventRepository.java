package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.CompilationEvent;
import ru.practicum.model.CompilationEventId;

import java.util.List;

public interface CompilationEventRepository extends JpaRepository<CompilationEvent, CompilationEventId> {

    @Query("SELECT ce.eventId FROM CompilationEvent ce WHERE ce.compilationId = :compilationId")
    List<Long> findEventIdsByCompilationId(@Param("compilationId") Long compilationId);

    void deleteByCompilationId(Long compilationId);
}
