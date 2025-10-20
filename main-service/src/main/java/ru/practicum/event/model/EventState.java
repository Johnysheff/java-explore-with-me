package ru.practicum.event.model;

public enum EventState {
    PENDING,    //ожидает модерации
    PUBLISHED,  //опубликовано
    CANCELED    //отменено
}