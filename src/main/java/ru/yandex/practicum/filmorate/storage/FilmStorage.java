package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Map;

public interface FilmStorage {

    List<Film> findAll();

    Film create(Film film);

    Film put(Film film);

    Map<Integer, Film> getFilms();

}
