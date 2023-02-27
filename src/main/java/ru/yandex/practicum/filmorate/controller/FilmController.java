package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.FilmAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.FilmDataException;
import ru.yandex.practicum.filmorate.exception.FilmNotExistException;
import ru.yandex.practicum.filmorate.model.Film;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {

    private final Map<Integer, Film> films = new HashMap<>();
    private static final LocalDate FIRST_FILM_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private int id = 0;

    @GetMapping
    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        if (films.containsKey(film.getId())) {
            log.warn("Передан фильм который уже был добавлен " + film);
            throw new FilmAlreadyExistException("Фильм с названием "
                    + film.getName() + " уже есть в списке фильмов.");
        }
        if (film.getReleaseDate().isBefore(FIRST_FILM_RELEASE_DATE)) {
            log.warn("Передан фильм с невозможной датой выхода " + film);
            throw new FilmDataException("Дата релиза фильма не может быть раньше 28 декабря 1895 года.");
        }
        film.setId(++id);
        films.put(film.getId(), film);
        log.debug("Добавлен новый фильм: " + film);
        return film;
    }

    @PutMapping
    public Film put(@Valid @RequestBody Film film) {
        if (films.get(film.getId()) == null) {
            log.warn("Фильма " + film + " не в списке.");
            throw new FilmNotExistException("Данного фильма не существует.");
        }
        films.put(film.getId(), film);
        log.debug("Обновлён фильм: " + film);
        return film;
    }
}
