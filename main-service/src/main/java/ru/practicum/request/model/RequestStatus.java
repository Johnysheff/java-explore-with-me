package ru.practicum.request.model;

public enum RequestStatus {
    PENDING,    //ожидает рассмотрения
    CONFIRMED,  //подтверждено
    REJECTED,   //отклонено
    CANCELED    //отменено пользователем
}