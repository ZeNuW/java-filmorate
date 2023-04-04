package ru.yandex.practicum.filmorate.exception;

public class FilmInformationNotExistException extends RuntimeException {
    public FilmInformationNotExistException(String s) {
        super(s);
    }
}
