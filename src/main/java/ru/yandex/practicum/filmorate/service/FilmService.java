package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.FilmNotExistException;
import ru.yandex.practicum.filmorate.exception.UserNotExistException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmGenre;
import ru.yandex.practicum.filmorate.model.FilmMpa;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage) {
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
        Film film = getFilm(filmId);
        if (film == null) {
            throw new FilmNotExistException("Фильм с id: " + filmId + " не найден.");
        }
        if (userStorage.getUser(userId) == null) {
            throw new UserNotExistException("Не найден пользователь с id:" + userId);
        }
        film.getLikes().add(userId);
        filmStorage.put(film);
    }

    public void deleteLike(int filmId, int userId) {
        Film film = getFilm(filmId);
        if (film == null) {
            throw new FilmNotExistException("Фильм с id: " + filmId + " не найден.");
        }
        if (userStorage.getUser(userId) == null) {
            throw new UserNotExistException("Не найден пользователь с id:" + userId);
        }
        film.getLikes().remove(userId);
        filmStorage.put(film);
    }

    public List<Film> topLikedFilms(int count) {
        return filmStorage.findAll().stream()
                .sorted(this::compare)
                .limit(count)
                .collect(Collectors.toList());
    }

    private int compare(Film f0, Film f1) {
        return Integer.compare(f1.getLikes().size(), f0.getLikes().size());
    }

    public Set<FilmGenre> getAllGenres() {
        return filmStorage.getAllGenres();
    }

    public FilmGenre getGenre(int genreId) {
        return filmStorage.getGenre(genreId);
    }

    public FilmMpa getMpa(int mpaId) {
        return filmStorage.getMpa(mpaId);
    }

    public Set<FilmMpa> getAllMpa() {
        return filmStorage.getAllMpa();
    }
}