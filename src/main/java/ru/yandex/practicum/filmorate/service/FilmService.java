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
import java.util.stream.Collectors;

@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final FilmInformation filmInformation;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage, FilmInformation filmInformation) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.filmInformation = filmInformation;
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
        getFilm(filmId);
        userStorage.getUser(userId);
        filmStorage.setLike(filmId,userId);
    }

    public void deleteLike(int filmId, int userId) {
        getFilm(filmId);
        userStorage.getUser(userId);
        filmStorage.deleteLike(filmId,userId);
    }

    public List<Film> topLikedFilms(int count) {
        return filmStorage.findAll().stream()
                .sorted(this::compare)
                .limit(count)
                .collect(Collectors.toList());
    }

    private int compare(Film f0, Film f1) {
        return Integer.compare(f1.getLikes(), f0.getLikes());
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