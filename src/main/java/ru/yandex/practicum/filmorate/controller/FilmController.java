package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.FilmAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.FilmDataException;
import ru.yandex.practicum.filmorate.exception.FilmNotExistException;
import ru.yandex.practicum.filmorate.exception.InvalidFilmNameException;
import ru.yandex.practicum.filmorate.model.Film;

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
    private final LocalDate firstFilmReleaseDate = LocalDate.of(1895, 12, 28);
    private int id = 0;

    @GetMapping
    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        if (films.containsKey(film.getId())) {
            log.warn("Передан который уже был добавлен " + film);
            throw new FilmAlreadyExistException("Фильм с названием "
                    + film.getName() + " уже есть в списке фильмов.");
        }
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Передан фильм с отсутствующим названием " + film);
            throw new InvalidFilmNameException("Название фильма не может быть пустым.");
        }
        if (film.getReleaseDate().isBefore(firstFilmReleaseDate)) {
            log.warn("Передан фильм с невозможной датой выхода " + film);
            throw new FilmDataException("Дата релиза фильма не может быть раньше 28 декабря 1895 года.");
        }
        if (film.getDescription().length() > 200) {
            log.warn("Описание фильма содержит больше 200 символов " + film);
            throw new FilmDataException("Описание фильма должно содержать меньше 200 символов.");
        }
        if (film.getDuration() <= 0) {
            log.warn("Длительность фильма меньше или равно 0" + film);
            throw new FilmDataException("Длительность фильма не может быть отрицательной или равной 0.");
        }
        film.setId(++id);
        films.put(film.getId(), film);
        log.debug("Добавлен новый фильм: " + film);
        return film;
    }

    @PutMapping
    public Film put(@RequestBody Film film) {
        if (films.get(film.getId()) == null) {
            log.warn("Фильма " + film + " не в списке.");
            throw new FilmNotExistException("Данного фильма не существует.");
        }
        if (film.getName() == null || film.getName().isBlank()) {
            throw new InvalidFilmNameException("Название фильма не может быть пустым.");
        }
        films.put(film.getId(), film);
        log.debug("Добавлен/обновлён фильм: " + film);
        return film;
    }
}
