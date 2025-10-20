package ru.practicum.event;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.event.dto.*;
import ru.practicum.event.location.Location;
import ru.practicum.event.location.LocationService;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.ParticipationRequestRepository;
import ru.practicum.request.RequestMapper;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final LocationService locationService;
    private final EventMapper eventMapper;
    private final RequestMapper requestMapper;
    private final StatsClient statsClient;

    public List<EventFullDto> searchEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                                  LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                  Integer from, Integer size) {
        validatePaginationParams(from, size);
        Pageable pageable = PageRequest.of(from / size, size);

        Specification<Event> spec = buildAdminSpecification(users, states, categories, rangeStart, rangeEnd);
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        return events.stream()
                .map(eventMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));

        validateEventForAdminUpdate(event, updateRequest);
        updateEventFields(event, updateRequest);

        if (updateRequest.getStateAction() != null) {
            processAdminStateAction(event, updateRequest.getStateAction());
        }

        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(updatedEvent);
    }

    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        validatePaginationParams(from, size);
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable).getContent();

        return events.stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с ID " + newEventDto.getCategory() + " не найдена"));

        validateEventDate(newEventDto.getEventDate(), 2);

        Location location = locationService.getOrSave(newEventDto.getLocation());

        Event event = eventMapper.toEvent(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setLocation(location);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);

        setDefaultEventValues(event);

        Event savedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(savedEvent);
    }

    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено у пользователя с ID " + userId));

        return eventMapper.toEventFullDto(event);
    }

    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено у пользователя с ID " + userId));

        validateEventForUserUpdate(event);

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), 2);
        }

        updateEventFields(event, updateRequest);

        if (updateRequest.getStateAction() != null) {
            processUserStateAction(event, updateRequest.getStateAction());
        }

        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(updatedEvent);
    }

    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, Integer from, Integer size,
                                               HttpServletRequest request) {
        validatePaginationParams(from, size);
        sendHitToStats(request);

        LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.now();
        LocalDateTime end = rangeEnd != null ? rangeEnd : null;

        if (end != null && start.isAfter(end)) {
            throw new BadRequestException("Дата начала не может быть позже даты окончания");
        }

        Specification<Event> spec = buildPublicSpecification(text, categories, paid, start, end, onlyAvailable);
        Pageable pageable = buildPageable(sort, from, size);

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();
        return enrichEventsWithStats(events);
    }

    public EventFullDto getPublicEventById(Long id, HttpServletRequest request) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Опубликованное событие с ID " + id + " не найдено"));

        sendHitToStats(request);

        Long currentViews = getEventViews(id);
        Long newViews = currentViews + 1;

        EventFullDto eventDto = eventMapper.toEventFullDto(event);
        eventDto.setViews(newViews); // Возвращаем увеличенное значение

        return eventDto;
    }

    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        if (!eventRepository.existsByIdAndInitiatorId(eventId, userId)) {
            throw new NotFoundException("Событие с ID " + eventId + " не найдено у пользователя с ID " + userId);
        }

        return requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено у пользователя с ID " + userId));

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ConflictException("Модерация запросов для этого события не требуется");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByIdInAndEventId(
                updateRequest.getRequestIds(), eventId);

        if (requests.isEmpty()) {
            throw new NotFoundException("Запросы с указанными ID не найдены");
        }

        return processRequestStatusUpdate(event, requests, updateRequest.getStatus());
    }

    private Specification<Event> buildAdminSpecification(List<Long> users, List<String> states,
                                                         List<Long> categories, LocalDateTime rangeStart,
                                                         LocalDateTime rangeEnd) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (users != null && !users.isEmpty()) {
                predicates.add(root.get("initiator").get("id").in(users));
            }

            if (states != null && !states.isEmpty()) {
                List<EventState> stateEnums = states.stream()
                        .map(EventState::valueOf)
                        .collect(Collectors.toList());
                predicates.add(root.get("state").in(stateEnums));
            }

            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categories));
            }

            if (rangeStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
            }

            if (rangeEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Event> buildPublicSpecification(String text, List<Long> categories, Boolean paid,
                                                          LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                          Boolean onlyAvailable) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

            if (text != null && !text.isBlank()) {
                String searchText = "%" + text.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("annotation")), searchText),
                        cb.like(cb.lower(root.get("description")), searchText)
                ));
            }

            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categories));
            }

            if (paid != null) {
                predicates.add(cb.equal(root.get("paid"), paid));
            }

            predicates.add(cb.greaterThan(root.get("eventDate"), rangeStart));

            if (rangeEnd != null) {
                predicates.add(cb.lessThan(root.get("eventDate"), rangeEnd));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable buildPageable(String sort, Integer from, Integer size) {
        if ("EVENT_DATE".equals(sort)) {
            return PageRequest.of(from / size, size, Sort.by("eventDate").descending());
        }
        return PageRequest.of(from / size, size);
    }

    private List<EventShortDto> enrichEventsWithStats(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());

        Map<Long, Long> confirmedRequests = requestRepository
                .findConfirmedRequestsCount(eventIds, RequestStatus.CONFIRMED)
                .stream()
                .collect(Collectors.toMap(ConfirmedRequestsDto::getEventId, ConfirmedRequestsDto::getCount));

        Map<Long, Long> views = getEventsViews(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .sorted(Comparator.comparing(EventShortDto::getViews).reversed())
                .collect(Collectors.toList());
    }

    private Map<Long, Long> getEventsViews(List<Long> eventIds) {
        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        try {
            List<ViewStats> stats = statsClient.getStats(
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now(),
                    uris,
                    true
            );

            return stats.stream()
                    .collect(Collectors.toMap(
                            stat -> Long.parseLong(stat.getUri().substring("/events/".length())),
                            ViewStats::getHits
                    ));
        } catch (Exception e) {
            log.error("Ошибка при получении статистики просмотров: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Long getEventViews(Long eventId) {
        try {
            List<ViewStats> stats = statsClient.getStats(
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now(),
                    List.of("/events/" + eventId),
                    true
            );
            return stats.isEmpty() ? 0L : stats.get(0).getHits();
        } catch (Exception e) {
            log.error("Ошибка при получении статистики просмотров для события {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private void sendHitToStats(HttpServletRequest request) {
        try {
            EndpointHit hit = EndpointHit.builder()
                    .app("ewm-main-service")
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();

            statsClient.hit(hit);
        } catch (Exception e) {
            log.error("Ошибка при отправке hit в сервис статистики: {}", e.getMessage());
        }
    }

    private void validateEventForAdminUpdate(Event event, UpdateEventAdminRequest updateRequest) {
        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), 1);
        }

        //проверка для публикации
        if ("PUBLISH_EVENT".equals(updateRequest.getStateAction())) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Нельзя опубликовать уже опубликованное событие");
            }
            if (event.getState() == EventState.CANCELED) {
                throw new ConflictException("Нельзя опубликовать отмененное событие");
            }
        }

        //проверка для отмены
        if ("REJECT_EVENT".equals(updateRequest.getStateAction())) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Нельзя отменить опубликованное событие");
            }
        }
    }

    private void validateEventForUserUpdate(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменять опубликованное событие");
        }
    }

    private void processAdminStateAction(Event event, String stateAction) {
        switch (stateAction) {
            case "PUBLISH_EVENT":
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                break;
            case "REJECT_EVENT":
                // Для админа REJECT_EVENT = отмена события
                event.setState(EventState.CANCELED);
                break;
            default:
                throw new BadRequestException("Неизвестное действие: " + stateAction);
        }
    }

    private void processUserStateAction(Event event, String stateAction) {
        switch (stateAction) {
            case "SEND_TO_REVIEW":
                event.setState(EventState.PENDING);
                break;
            case "CANCEL_REVIEW":
                event.setState(EventState.CANCELED);
                break;
            default:
                throw new BadRequestException("Неизвестное действие: " + stateAction);
        }
    }

    private EventRequestStatusUpdateResult processRequestStatusUpdate(Event event,
                                                                      List<ParticipationRequest> requests,
                                                                      String status) {
        long confirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        int availableSlots = event.getParticipantLimit() - (int) confirmedCount;

        if ("CONFIRMED".equals(status) && availableSlots <= 0) {
            throw new ConflictException("Достигнут лимит участников события");
        }

        List<ParticipationRequest> confirmedRequests = new ArrayList<>();
        List<ParticipationRequest> rejectedRequests = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Запрос должен быть в статусе PENDING");
            }

            if ("CONFIRMED".equals(status) && availableSlots > 0) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedRequests.add(request);
                availableSlots--;
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(request);
            }
        }

        requestRepository.saveAll(requests);

        if (availableSlots == 0 && "CONFIRMED".equals(status)) {
            rejectPendingRequests(event.getId());
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests.stream()
                        .map(requestMapper::toParticipationRequestDto)
                        .collect(Collectors.toList()))
                .rejectedRequests(rejectedRequests.stream()
                        .map(requestMapper::toParticipationRequestDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private void rejectPendingRequests(Long eventId) {
        List<ParticipationRequest> pendingRequests = requestRepository
                .findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);

        pendingRequests.forEach(request -> request.setStatus(RequestStatus.REJECTED));
        requestRepository.saveAll(pendingRequests);
    }

    private void validateEventDate(LocalDateTime eventDate, int hoursBefore) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(hoursBefore))) {
            throw new BadRequestException("Дата события должна быть как минимум за " + hoursBefore + " часа от текущего времени");
        }
    }

    private void setDefaultEventValues(Event event) {
        if (event.getPaid() == null) {
            event.setPaid(false);
        }
        if (event.getParticipantLimit() == null) {
            event.setParticipantLimit(0);
        }
        if (event.getRequestModeration() == null) {
            event.setRequestModeration(true);
        }
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            event.setEventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }
        if (updateRequest.getLocation() != null) {
            Location location = locationService.getOrSave(updateRequest.getLocation());
            event.setLocation(location);
        }
    }

    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            event.setEventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }
        if (updateRequest.getLocation() != null) {
            Location location = locationService.getOrSave(updateRequest.getLocation());
            event.setLocation(location);
        }
    }

    private void validatePaginationParams(Integer from, Integer size) {
        if (from == null || from < 0) {
            throw new BadRequestException("Параметр 'from' должен быть неотрицательным");
        }
        if (size == null || size <= 0) {
            throw new BadRequestException("Параметр 'size' должен быть положительным");
        }
    }
}