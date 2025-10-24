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
@RequestMapping("/events/{eventId}/comments")
public class PublicCommentController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getEventComments(@PathVariable Long eventId,
                                             @RequestParam(defaultValue = "0") Integer from,
                                             @RequestParam(defaultValue = "10") Integer size) {
        log.info("Получение комментариев для события {}: from={}, size={}", eventId, from, size);
        return commentService.getCommentsForEvent(eventId, from, size);
    }
}