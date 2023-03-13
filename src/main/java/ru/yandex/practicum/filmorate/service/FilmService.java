package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.FilmNotExistException;
import ru.yandex.practicum.filmorate.exception.UserNotExistException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilmService {

    FilmStorage filmStorage;
    UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film getFilm(int id) {
        Film film = filmStorage.getFilms().get(id);
        if (film == null) {
            throw new FilmNotExistException("Фильма с id " + id + " не существует");
        }
        return film;
    }

    public void setLike(int filmId, int userId) {
        if (filmStorage.getFilms().get(filmId) == null) {
            throw new FilmNotExistException("Фильм с id: " + filmId + " не найден.");
        }
        if (userStorage.getUsers().get(userId) == null) {
            throw new UserNotExistException("Не найден пользователь с id:" + userId);
        }
        filmStorage.getFilms().get(filmId).getLikes().add(userId);
    }

    public void deleteLike(int filmId, int userId) {
        if (filmStorage.getFilms().get(filmId) == null) {
            throw new FilmNotExistException("Фильм с id: " + filmId + " не найден.");
        }
        if (userStorage.getUsers().get(userId) == null) {
            throw new UserNotExistException("Не найден пользователь с id:" + userId);
        }
        filmStorage.getFilms().get(filmId).getLikes().remove(userId);
    }

    public List<Film> topLikedFilms(int count) {
        // TODO подумать над более быстрой сортировкой
        return filmStorage.getFilms().values().stream()
                .sorted(this::compare)
                .limit(count)
                .collect(Collectors.toList());
    }

    private int compare(Film f0, Film f1) {
        return Integer.compare(f1.getLikes().size(), f0.getLikes().size());
    }

}
