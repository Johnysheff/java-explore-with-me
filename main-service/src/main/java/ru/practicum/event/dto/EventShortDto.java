package ru.practicum.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto {
    private Long id;
    private String annotation;

    @NotNull(message = "Категория не может быть null")
    private CategoryDto category;

    private Long confirmedRequests;

    @NotNull(message = "Дата события обязательна")
    private LocalDateTime eventDate;

    @NotNull(message = "Инициатор обязателен")
    private UserShortDto initiator;

    @NotNull(message = "Поле paid обязательно")
    private Boolean paid;

    @NotBlank(message = "Заголовок события не может быть пустым")
    private String title;

    private Long views;
}