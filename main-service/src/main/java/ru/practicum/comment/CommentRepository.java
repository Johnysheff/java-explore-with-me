package ru.practicum.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findAllByEventIdAndStatus(Long eventId, CommentStatus status, Pageable pageable);

    List<Comment> findAllByAuthorId(Long authorId);

    Page<Comment> findAllByStatus(CommentStatus status, Pageable pageable);

    Optional<Comment> findByIdAndAuthorId(Long commentId, Long authorId);

    boolean existsByEventIdAndAuthorId(Long eventId, Long authorId);

    List<Comment> findByEventIdAndStatus(Long eventId, CommentStatus status);
}