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

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        return filmStorage.create(film);
    }

    public Film put(Film film) {
        return filmStorage.put(film);
    }

    public Film getFilm(int id) {
        return filmStorage.getFilm(id);
    }

    public void setLike(int filmId, int userId) {
        if (getFilm(filmId) == null) {
            throw new FilmNotExistException("Фильм с id: " + filmId + " не найден.");
        }
        if (userStorage.getUser(userId) == null) {
            throw new UserNotExistException("Не найден пользователь с id:" + userId);
        }
        getFilm(filmId).getLikes().add(userId);
    }

    public void deleteLike(int filmId, int userId) {
        if (getFilm(filmId) == null) {
            throw new FilmNotExistException("Фильм с id: " + filmId + " не найден.");
        }
        if (userStorage.getUser(userId) == null) {
            throw new UserNotExistException("Не найден пользователь с id:" + userId);
        }
        getFilm(filmId).getLikes().remove(userId);
    }

    public List<Film> topLikedFilms(int count) {
        // TODO подумать над более быстрой сортировкой
        return filmStorage.findAll().stream()
                .sorted(this::compare)
                .limit(count)
                .collect(Collectors.toList());
    }

    private int compare(Film f0, Film f1) {
        return Integer.compare(f1.getLikes().size(), f0.getLikes().size());
    }
}
