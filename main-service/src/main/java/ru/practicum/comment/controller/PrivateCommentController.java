package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.CommentService;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/comments")
public class PrivateCommentController {
    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable Long userId,
                                    @RequestParam Long eventId,
                                    @RequestBody @Valid NewCommentDto newCommentDto) {
        log.info("Создание комментария пользователем {} к событию {}", userId, eventId);
        return commentService.createComment(userId, eventId, newCommentDto);
    }

    @GetMapping
    public List<CommentDto> getUserComments(@PathVariable Long userId) {
        log.info("Получение комментариев пользователя {}", userId);
        return commentService.getUserComments(userId);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId,
                                    @PathVariable Long commentId,
                                    @RequestBody @Valid NewCommentDto updateDto) {
        log.info("Обновление комментария {} пользователем {}", commentId, userId);
        return commentService.updateCommentByUser(userId, commentId, updateDto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId,
                              @PathVariable Long commentId) {
        log.info("Удаление комментария {} пользователем {}", commentId, userId);
        commentService.deleteCommentByUser(userId, commentId);
    }
}