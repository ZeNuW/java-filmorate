package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmGenre;
import ru.yandex.practicum.filmorate.model.FilmMpa;

import java.util.List;
import java.util.Set;

public interface FilmStorage {

    List<Film> findAll();

    Film create(Film film);

    Film put(Film film);

    Film getFilm(int id);

    FilmGenre getGenre(int genreId);

    Set<FilmGenre> getAllGenres();

    FilmMpa getMpa(int mpaId);

    Set<FilmMpa> getAllMpa();
}
