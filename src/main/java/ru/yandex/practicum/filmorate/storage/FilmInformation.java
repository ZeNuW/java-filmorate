package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.FilmGenre;
import ru.yandex.practicum.filmorate.model.FilmMpa;

import java.util.Set;

public interface FilmInformation {
    FilmGenre getGenre(int genreId);

    Set<FilmGenre> getAllGenres();

    FilmMpa getMpa(int mpaId);

    Set<FilmMpa> getAllMpa();
}
