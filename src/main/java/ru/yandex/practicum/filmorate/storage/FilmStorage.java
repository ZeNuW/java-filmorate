package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmStorage {

    List<Film> findAll();

    Film create(Film film);

    Film put(Film film);

    Film getFilm(int id);

    void setLike(int filmId, int userId);

    void deleteLike(int filmId, int userId);

    List<Film> topLikedFilms(int count);

    void loadGenre(Film film);
}