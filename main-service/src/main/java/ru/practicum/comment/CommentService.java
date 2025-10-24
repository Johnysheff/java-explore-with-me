package ru.practicum.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.event.EventRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Создание комментария пользователем {} к событию {}", userId, eventId);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));

        //проверяем, что событие опубликовано
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        //проверяем, не оставлял ли пользователь уже комментарий к этому событию
        if (commentRepository.existsByEventIdAndAuthorId(eventId, userId)) {
            throw new ConflictException("Пользователь уже оставлял комментарий к этому событию");
        }

        Comment comment = commentMapper.toComment(newCommentDto);
        comment.setAuthor(author);
        comment.setEvent(event);

        Comment savedComment = commentRepository.save(comment);
        log.info("Комментарий создан с ID: {}", savedComment.getId());

        return commentMapper.toCommentDto(savedComment);
    }

    public List<CommentDto> getCommentsForEvent(Long eventId, Integer from, Integer size) {
        log.info("Получение комментариев для события {}: from={}, size={}", eventId, from, size);

        validatePaginationParams(from, size);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());

        return commentRepository.findAllByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, pageable)
                .stream()
                .map(commentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    public List<CommentDto> getUserComments(Long userId) {
        log.info("Получение комментариев пользователя {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        return commentRepository.findAllByAuthorId(userId)
                .stream()
                .map(commentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentDto updateCommentByUser(Long userId, Long commentId, NewCommentDto updateDto) {
        log.info("Обновление комментария {} пользователем {}", commentId, userId);

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Комментарий с ID " + commentId + " не найден у пользователя " + userId));

        //проверяем, что комментарий можно редактировать
        if (comment.getStatus() == CommentStatus.DELETED || comment.getStatus() == CommentStatus.REJECTED) {
            throw new ConflictException("Нельзя редактировать удаленный или отклоненный комментарий");
        }

        comment.setText(updateDto.getText());
        comment.setUpdatedOn(LocalDateTime.now());
        comment.setStatus(CommentStatus.PENDING); // При редактировании отправляем на повторную модерацию

        Comment updatedComment = commentRepository.save(comment);
        log.info("Комментарий {} обновлен", commentId);

        return commentMapper.toCommentDto(updatedComment);
    }

    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        log.info("Удаление комментария {} пользователем {}", commentId, userId);

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Комментарий с ID " + commentId + " не найден у пользователя " + userId));

        comment.setStatus(CommentStatus.DELETED);
        comment.setUpdatedOn(LocalDateTime.now());

        commentRepository.save(comment);
        log.info("Комментарий {} помечен как удаленный", commentId);
    }

    public List<CommentDto> getCommentsForModeration(Integer from, Integer size) {
        log.info("Получение комментариев для модерации: from={}, size={}", from, size);

        validatePaginationParams(from, size);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").ascending());

        return commentRepository.findAllByStatus(CommentStatus.PENDING, pageable)
                .stream()
                .map(commentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentDto publishComment(Long commentId) {
        log.info("Публикация комментария {}", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с ID " + commentId + " не найден"));

        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Можно публиковать только комментарии в статусе PENDING");
        }

        comment.setStatus(CommentStatus.PUBLISHED);
        comment.setUpdatedOn(LocalDateTime.now());

        Comment publishedComment = commentRepository.save(comment);
        log.info("Комментарий {} опубликован", commentId);

        return commentMapper.toCommentDto(publishedComment);
    }

    @Transactional
    public CommentDto rejectComment(Long commentId) {
        log.info("Отклонение комментария {}", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с ID " + commentId + " не найден"));

        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Можно отклонять только комментарии в статусе PENDING");
        }

        comment.setStatus(CommentStatus.REJECTED);
        comment.setUpdatedOn(LocalDateTime.now());

        Comment rejectedComment = commentRepository.save(comment);
        log.info("Комментарий {} отклонен", commentId);

        return commentMapper.toCommentDto(rejectedComment);
    }

    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        log.info("Удаление комментария {} администратором", commentId);

        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий с ID " + commentId + " не найден");
        }

        commentRepository.deleteById(commentId);
        log.info("Комментарий {} удален администратором", commentId);
    }

    private void validatePaginationParams(Integer from, Integer size) {
        if (from < 0) {
            throw new IllegalArgumentException("Параметр 'from' не может быть отрицательным");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Параметр 'size' должен быть положительным");
        }
    }
}