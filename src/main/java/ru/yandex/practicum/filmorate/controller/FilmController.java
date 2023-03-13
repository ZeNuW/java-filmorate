package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {

    private final FilmStorage filmStorage;
    private final FilmService filmService;

    @Autowired
    public FilmController(FilmStorage filmStorage, FilmService filmService) {
        this.filmStorage = filmStorage;
        this.filmService = filmService;
    }

    @GetMapping("{id}")
    public Film getFilm(@PathVariable("id") int id) {
        return filmService.getFilm(id);
    }

    @GetMapping
    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        return filmStorage.create(film);
    }

    @PutMapping
    public Film put(@Valid @RequestBody Film film) {
        return filmStorage.put(film);
    }

    @PutMapping("{id}/like/{userId}")
    public void setLike(@PathVariable("id") int id,
                        @PathVariable("userId") int userId) {
        filmService.setLike(id, userId);
    }

    @DeleteMapping("{id}/like/{userId}")
    public void deleteLike(@PathVariable("id") int id,
                           @PathVariable("userId") int userId) {
        filmService.deleteLike(id, userId);
    }

    @GetMapping("popular")
    public List<Film> getTopLikedFilms(@RequestParam(value = "count", defaultValue = "10") int count) {
        return filmService.topLikedFilms(count);
    }

}
