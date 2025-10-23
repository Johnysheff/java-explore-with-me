package ru.practicum.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.category.Category;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    boolean existsByCategory(Category category);

    Page<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    boolean existsByIdAndInitiatorId(Long eventId, Long userId);

    Optional<Event> findByIdAndState(Long id, EventState state);

    Set<Event> findAllByIdIn(List<Long> events);

    List<Event> findByCategoryId(Long categoryId);

    @Query("SELECT e FROM Event e WHERE e.eventDate BETWEEN :start AND :end AND e.state = :state")
    List<Event> findEventsByDateRangeAndState(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("state") EventState state);
}