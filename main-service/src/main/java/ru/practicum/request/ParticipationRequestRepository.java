package ru.practicum.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.event.dto.ConfirmedRequestsDto;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByRequesterId(Long userId);

    List<ParticipationRequest> findAllByEventId(Long eventId);


    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<ParticipationRequest> findAllByIdInAndEventId(List<Long> ids, Long eventId);

    List<ParticipationRequest> findAllByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT new ru.practicum.event.dto.ConfirmedRequestsDto(pr.event.id, COUNT(pr.id)) " +
           "FROM ParticipationRequest pr " +
           "WHERE pr.event.id IN :eventIds AND pr.status = :status " +
           "GROUP BY pr.event.id")
    List<ConfirmedRequestsDto> findConfirmedRequestsCount(@Param("eventIds") List<Long> eventIds,
                                                          @Param("status") RequestStatus status);

    Optional<ParticipationRequest> findByEventIdAndRequesterId(Long eventId, Long requesterId);
}