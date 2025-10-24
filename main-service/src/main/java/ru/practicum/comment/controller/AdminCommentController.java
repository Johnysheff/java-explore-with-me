package ru.practicum.comment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.CommentService;
import ru.practicum.comment.dto.CommentDto;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class AdminCommentController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getCommentsForModeration(@RequestParam(defaultValue = "0") Integer from,
                                                     @RequestParam(defaultValue = "10") Integer size) {
        log.info("Получение комментариев для модерации: from={}, size={}", from, size);
        return commentService.getCommentsForModeration(from, size);
    }

    @PatchMapping("/{commentId}/publish")
    public CommentDto publishComment(@PathVariable Long commentId) {
        log.info("Публикация комментария {}", commentId);
        return commentService.publishComment(commentId);
    }

    @PatchMapping("/{commentId}/reject")
    public CommentDto rejectComment(@PathVariable Long commentId) {
        log.info("Отклонение комментария {}", commentId);
        return commentService.rejectComment(commentId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        log.info("Удаление комментария {} администратором", commentId);
        commentService.deleteCommentByAdmin(commentId);
    }
}