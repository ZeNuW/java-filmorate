package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmGenre;
import ru.yandex.practicum.filmorate.model.FilmMpa;
import ru.yandex.practicum.filmorate.storage.FilmInformation;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;
import java.util.Set;

@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final FilmInformation filmInformation;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       FilmInformation filmInformation) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.filmInformation = filmInformation;
    }

    public List<Film> findAll() {
        List<Film> films = filmStorage.findAll();
        films.forEach(filmStorage::loadGenre);
        return films;
    }

    public Film create(Film film) {
        film = filmStorage.create(film);
        return getFilm(film.getId());
    }

    public Film put(Film film) {
        film = filmStorage.put(film);
        return getFilm(film.getId());
    }

    public Film getFilm(int id) {
        Film film = filmStorage.getFilm(id);
        filmStorage.loadGenre(film);
        return film;
    }

    public void setLike(int filmId, int userId) {
        getFilm(filmId);
        userStorage.getUser(userId);
        filmStorage.setLike(filmId, userId);
    }

    public void deleteLike(int filmId, int userId) {
        getFilm(filmId);
        userStorage.getUser(userId);
        filmStorage.deleteLike(filmId, userId);
    }

    public List<Film> topLikedFilms(int count) {
        List<Film> topFilms = filmStorage.topLikedFilms(count);
        topFilms.forEach(filmStorage::loadGenre);
        return topFilms;
    }

    public FilmGenre getGenre(int genreId) {
        return filmInformation.getGenre(genreId);
    }

    public Set<FilmGenre> getAllGenres() {
        return filmInformation.getAllGenres();
    }

    public FilmMpa getMpa(int mpaId) {
        return filmInformation.getMpa(mpaId);
    }

    public Set<FilmMpa> getAllMpa() {
        return filmInformation.getAllMpa();
    }
}