package ru.practicum.comment.model;

public enum CommentStatus {
    PENDING,   //на модерации
    PUBLISHED, //опубликован
    REJECTED,  //отклонен модератором
    DELETED    //удален автором
}