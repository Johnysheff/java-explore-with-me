package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflictException(ConflictException e) {
        log.error("Конфликт: {}", e.getMessage(), e);
        return ErrorResponse.builder()
                .error("Конфликт данных")
                .message(e.getMessage())
                .reason("Нарушение целостности данных")
                .status("CONFLICT")
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException e) {
        log.error("Объект не найден: {}", e.getMessage(), e);
        return ErrorResponse.builder()
                .error("Объект не найден")
                .message(e.getMessage())
                .reason("Требуемый объект не был найден")
                .status("NOT_FOUND")
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequestException(BadRequestException e) {
        log.warn("Некорректный запрос: {}", e.getMessage(), e);
        return ErrorResponse.builder()
                .error("Ошибка валидации")
                .message(e.getMessage())
                .reason("Некорректно составленный запрос")
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(ValidationException e) {
        log.warn("Ошибка валидации: {}", e.getMessage(), e);
        return ErrorResponse.builder()
                .error("Ошибка валидации")
                .message(e.getMessage())
                .reason("Некорректные данные в запросе")
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Ошибка валидации полей: {}", errorMessage, ex);

        return ErrorResponse.builder()
                .error("Ошибка валидации")
                .message(errorMessage)
                .reason("Некорректно составленный запрос")
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParams(MissingServletRequestParameterException ex) {
        String errorMessage = "Отсутствует обязательный параметр: " + ex.getParameterName();

        log.warn("Отсутствует параметр запроса: {}", ex.getParameterName(), ex);

        return ErrorResponse.builder()
                .error("Отсутствует параметр")
                .message(errorMessage)
                .reason("Некорректно составленный запрос")
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllExceptions(Exception e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);
        return ErrorResponse.builder()
                .error("Внутренняя ошибка сервера")
                .message("Произошла непредвиденная ошибка")
                .reason("Внутренняя ошибка сервера")
                .status("INTERNAL_SERVER_ERROR")
                .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .build();
    }
}