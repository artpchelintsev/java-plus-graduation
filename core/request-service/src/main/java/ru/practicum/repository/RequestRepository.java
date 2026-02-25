package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.dto.ConfirmedRequestCount;
import ru.practicum.model.Request;
import ru.practicum.model.RequestStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByEventId(Long eventId);

    List<Request> findByRequesterId(Long requesterId);

    Optional<Request> findByEventIdAndRequesterId(Long eventId, Long requesterId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<Request> findByEventIdAndIdIn(Long eventId, List<Long> requestIds);

    List<Request> findByEventIdAndStatus(Long eventId, RequestStatus status);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT COUNT(r) FROM Request r WHERE r.eventId = :eventId AND r.status = 'CONFIRMED'")
    long countConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    @Query("""
            SELECT new ru.practicum.dto.ConfirmedRequestCount(r.eventId, COUNT(r))
            FROM Request r
            WHERE r.status = :status
                AND r.eventId IN :eventIds
            GROUP BY r.eventId
            """)
    List<ConfirmedRequestCount> countConfirmedRequestsForEvents(@Param("eventIds") List<Long> eventIds,
                                                                @Param("status") RequestStatus status);

    void deleteByEventId(Long eventId);

    void deleteByRequesterId(Long requesterId);
}